/**
  ******************************************************************************
  * @file       bsp_control.c
  * @brief      自动控制系统驱动函数
  ******************************************************************************
  */

#include "bsp_control.h"
#include "servo/bsp_servo.h"
#include "mpu6050/bsp_i2c_mpu6050.h"
#include "tilt/bsp_gpio_tilt.h"
#include "adc/bsp_adc.h"
#include "dht11/app_dht11.h"
#include "systick/bsp_systick.h"

/* 全局变量定义 */
volatile Control_System_TypeDef control_system = {0};

/* 阈值变量 */
static uint16_t air_quality_threshold = 100;
static float temperature_min_threshold = 25.0f;
static float temperature_max_threshold = 35.0f;
static uint8_t shake_severity_threshold = 2;  /* 中等以上震动触发 */
static uint8_t tilt_detected_threshold = 1;   /* 检测到倾斜触发 */

/* 舵机控制参数 */
#define SERVO_CHANNEL SERVO_NUM1
#define VENT_OPEN_ANGLE 90      /* 打开角度 */
#define VENT_CLOSE_ANGLE 0     /* 关闭角度 */

/**
  * @brief  控制系统初始化
  * @param  无
  * @retval 无
  */
void CONTROL_Init(void)
{
    /* 初始化舵机定时器 */
    SERVO_TIM_Init();
    
    /* 初始化控制系统状态 */
    control_system.control_mode = CONTROL_AUTO;
    control_system.vent_state = VENT_CLOSED;
    control_system.trigger_type = TRIGGER_NONE;
    control_system.auto_control_enabled = 1;
    control_system.vent_open_angle = VENT_OPEN_ANGLE;
    control_system.vent_close_angle = VENT_CLOSE_ANGLE;
    control_system.last_action_time = 0;
    control_system.action_delay = 1000;  /* 默认1秒延迟 */
    
    /* 关闭通风口 */
    CONTROL_CloseVent();
}

/**
  * @brief  设置控制模式
  * @param  mode: 控制模式 (CONTROL_MANUAL 或 CONTROL_AUTO)
  * @retval 无
  */
void CONTROL_SetMode(Control_Mode_TypeDef mode)
{
    control_system.control_mode = mode;
}

/**
  * @brief  设置自动控制使能
  * @param  enable: 使能状态 (1=使能, 0=禁用)
  * @retval 无
  */
void CONTROL_SetAutoEnable(uint8_t enable)
{
    control_system.auto_control_enabled = enable;
}

/**
  * @brief  打开通风口
  * @param  无
  * @retval 无
  */
void CONTROL_OpenVent(void)
{
    if(control_system.vent_state != VENT_OPEN)
    {
        /* 计算PWM脉冲宽度 */
        float time = SERVO_AngleToTime(control_system.vent_open_angle);
        uint16_t pulse = SERVO_TimeCalculate(time);
        
        /* 设置舵机角度 */
        SERVO_PulseConfig(SERVO_CHANNEL, pulse);
        
        control_system.vent_state = VENT_OPEN;
        control_system.last_action_time = (uint32_t)SysTick_GetCount();
        control_system.trigger_type = TRIGGER_NONE;
    }
}

/**
  * @brief  关闭通风口
  * @param  无
  * @retval 无
  */
void CONTROL_CloseVent(void)
{
    if(control_system.vent_state != VENT_CLOSED)
    {
        /* 计算PWM脉冲宽度 */
        float time = SERVO_AngleToTime(control_system.vent_close_angle);
        uint16_t pulse = SERVO_TimeCalculate(time);
        
        /* 设置舵机角度 */
        SERVO_PulseConfig(SERVO_CHANNEL, pulse);
        
        control_system.vent_state = VENT_CLOSED;
        control_system.last_action_time = (uint32_t)SysTick_GetCount();
        control_system.trigger_type = TRIGGER_NONE;
    }
}

/**
  * @brief  设置通风口角度
  * @param  angle: 角度值 (0-180)
  * @retval 无
  */
void CONTROL_SetVentAngle(uint16_t angle)
{
    /* 限制角度范围 */
    if(angle > 180)
    {
        angle = 180;
    }
    
    /* 计算PWM脉冲宽度 */
    float time = SERVO_AngleToTime(angle);
    uint16_t pulse = SERVO_TimeCalculate(time);
    
    /* 设置舵机角度 */
    SERVO_PulseConfig(SERVO_CHANNEL, pulse);
    
    control_system.last_action_time = (uint32_t)SysTick_GetCount();
    
    /* 更新通风口状态 */
    if(angle == 0)
    {
        control_system.vent_state = VENT_CLOSED;
    }
    else
    {
        control_system.vent_state = VENT_OPEN;
    }
}

/**
  * @brief  更新控制系统（需要在主循环中调用）
  * @param  无
  * @retval 无
  */
void CONTROL_Update(void)
{
    /* 只有在自动模式下才检查传感器 */
    if(control_system.control_mode == CONTROL_AUTO && control_system.auto_control_enabled)
    {
        MPU6050_SensorData_TypeDef sensor_data;
        
        /* 读取MPU6050数据 */
        MPU6050_ReadSensorData(&sensor_data);
        
        /* 检测震动 */
        Shake_Severity_TypeDef shake_severity = MPU6050_DetectShake(&sensor_data);
        if(shake_severity >= shake_severity_threshold)
        {
            CONTROL_ProcessTrigger(TRIGGER_SHAKE, 1);
            MPU6050_ClearShakeFlag();
        }
        
        /* 检测倾斜 */
        uint8_t tilt_detected = TILT_ReadStatus();
        if(tilt_detected >= tilt_detected_threshold)
        {
            CONTROL_ProcessTrigger(TRIGGER_TILT, 1);
            TILT_ClearFlag();
        }
        
        /* 检测空气质量 */
        uint16_t air_quality = MQ135_ReadValue();
        if(air_quality > air_quality_threshold)
        {
            CONTROL_ProcessTrigger(TRIGGER_AIR_QUALITY, 1);
        }
        
        /* 检测温度 */
        float temperature;
        DHT11_Read_Temperature(&temperature);
        if(temperature < temperature_min_threshold || temperature > temperature_max_threshold)
        {
            CONTROL_ProcessTrigger(TRIGGER_TEMPERATURE, 1);
        }
    }
}

/**
  * @brief  处理触发条件
  * @param  trigger_type: 触发类型
  * @param  trigger_value: 触发值
  * @retval 无
  */
void CONTROL_ProcessTrigger(Trigger_Type_TypeDef trigger_type, uint8_t trigger_value)
{
    uint32_t current_time;
    float temperature;
    
    current_time = (uint32_t)SysTick_GetCount();
    
    /* 检查动作延迟 */
    if((current_time - control_system.last_action_time) < control_system.action_delay)
    {
        return;
    }
    
    /* 根据触发类型执行相应动作 */
    switch(trigger_type)
    {
        case TRIGGER_SHAKE:
        case TRIGGER_TILT:
            /* 震动或倾斜时，打开通风口以防止进一步损坏 */
            control_system.trigger_type = trigger_type;
            CONTROL_OpenVent();
            break;
            
        case TRIGGER_AIR_QUALITY:
            /* 空气质量差时，打开通风口 */
            control_system.trigger_type = trigger_type;
            CONTROL_OpenVent();
            break;
            
        case TRIGGER_TEMPERATURE:
            /* 温度异常时，根据情况打开或关闭通风口 */
            control_system.trigger_type = trigger_type;
            
            DHT11_Read_Temperature(&temperature);
            
            if(temperature > temperature_max_threshold)
            {
                /* 温度过高，打开通风口降温 */
                CONTROL_OpenVent();
            }
            else if(temperature < temperature_min_threshold)
            {
                /* 温度过低，关闭通风口保温 */
                CONTROL_CloseVent();
            }
            break;
            
        default:
            break;
    }
}

/**
  * @brief  手动控制通风口
  * @param  open_close: 0=关闭, 1=打开
  * @retval 无
  */
void CONTROL_ManualControl(uint8_t open_close)
{
    if(control_system.control_mode == CONTROL_MANUAL)
    {
        if(open_close)
        {
            CONTROL_OpenVent();
        }
        else
        {
            CONTROL_CloseVent();
        }
    }
}

/**
  * @brief  设置空气质量阈值
  * @param  threshold: 阈值
  * @retval 无
  */
void CONTROL_SetAirQualityThreshold(uint16_t threshold)
{
    air_quality_threshold = threshold;
}

/**
  * @brief  设置温度阈值
  * @param  min_temp: 最小温度
  * @param  max_temp: 最大温度
  * @retval 无
  */
void CONTROL_SetTemperatureThreshold(float min_temp, float max_temp)
{
    temperature_min_threshold = min_temp;
    temperature_max_threshold = max_temp;
}

/**
  * @brief  设置震动阈值
  * @param  severity_level: 严重级别
  * @retval 无
  */
void CONTROL_SetShakeThreshold(uint8_t severity_level)
{
    shake_severity_threshold = severity_level;
}

/**
  * @brief  设置倾斜阈值
  * @param  tilt_detected: 检测标志
  * @retval 无
  */
void CONTROL_SetTiltThreshold(uint8_t tilt_detected)
{
    tilt_detected_threshold = tilt_detected;
}

/**
  * @brief  获取通风口状态
  * @param  无
  * @retval 通风口状态 (VENT_CLOSED 或 VENT_OPEN)
  */
uint8_t CONTROL_GetVentState(void)
{
    return control_system.vent_state;
}

/**
  * @brief  获取控制模式
  * @param  无
  * @retval 控制模式 (CONTROL_MANUAL 或 CONTROL_AUTO)
  */
Control_Mode_TypeDef CONTROL_GetMode(void)
{
    return control_system.control_mode;
}

/**
  * @brief  检查自动控制是否使能
  * @param  无
  * @retval 使能状态 (1=使能, 0=禁用)
  */
uint8_t CONTROL_IsAutoEnabled(void)
{
    return control_system.auto_control_enabled;
}





