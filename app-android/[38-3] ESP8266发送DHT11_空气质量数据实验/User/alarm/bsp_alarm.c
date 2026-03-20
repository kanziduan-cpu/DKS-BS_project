/**
  ******************************************************************************
  * @file       bsp_alarm.c
  * @brief      声光报警系统驱动函数
  ******************************************************************************
  */

#include "bsp_alarm.h"
#include "beep/bsp_gpio_beep.h"
#include "led/bsp_gpio_led.h"
#include "systick/bsp_systick.h"

/* 全局变量定义 */
volatile Alarm_System_TypeDef alarm_system = {0};

/* 静态变量用于闪烁控制 */
static uint32_t flash_interval = 500; /* 闪烁间隔(ms) */
static uint32_t last_flash_time = 0;
static uint8_t flash_state = 0;

/* 静态变量用于蜂鸣器控制 */
static uint32_t beep_interval = 1000; /* 蜂鸣器间隔(ms) */
static uint32_t last_beep_time = 0;
static uint8_t beep_state = 0;

/**
  * @brief  报警系统初始化
  * @param  无
  * @retval 无
  */
void ALARM_Init(void)
{
    alarm_system.alarm_active = 0;
    alarm_system.alarm_type = ALARM_NONE;
    alarm_system.alarm_level = ALARM_LEVEL_LOW;
    alarm_system.alarm_start_time = 0;
    alarm_system.alarm_duration = 0;
    alarm_system.flash_enabled = 1;
    alarm_system.beep_enabled = 1;
    
    flash_state = 0;
    beep_state = 0;
    last_flash_time = 0;
    last_beep_time = 0;
}

/**
  * @brief  启动报警
  * @param  alarm_type: 报警类型
  * @param  alarm_level: 报警级别
  * @retval 无
  */
void ALARM_Start(Alarm_Type_TypeDef alarm_type, Alarm_Level_TypeDef alarm_level)
{
    alarm_system.alarm_active = 1;
    alarm_system.alarm_type = alarm_type;
    alarm_system.alarm_level = alarm_level;
    alarm_system.alarm_start_time = (uint32_t)SysTick_GetCount();
    
    /* 根据报警级别设置闪烁和蜂鸣器频率 */
    switch(alarm_level)
    {
        case ALARM_LEVEL_LOW:
            flash_interval = 1000;  /* 1秒间隔 */
            beep_interval = 2000;  /* 2秒间隔 */
            break;
        case ALARM_LEVEL_MEDIUM:
            flash_interval = 500;  /* 0.5秒间隔 */
            beep_interval = 1000;  /* 1秒间隔 */
            break;
        case ALARM_LEVEL_HIGH:
            flash_interval = 250;  /* 0.25秒间隔 */
            beep_interval = 500;   /* 0.5秒间隔 */
            break;
        case ALARM_LEVEL_CRITICAL:
            flash_interval = 100;  /* 0.1秒间隔 */
            beep_interval = 200;   /* 0.2秒间隔 */
            break;
        default:
            flash_interval = 500;
            beep_interval = 1000;
            break;
    }
}

/**
  * @brief  停止报警
  * @param  无
  * @retval 无
  */
void ALARM_Stop(void)
{
    alarm_system.alarm_active = 0;
    alarm_system.alarm_type = ALARM_NONE;
    
    /* 关闭LED和蜂鸣器 */
    LED_OFF(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
    BEEP_OFF(BEEP_GPIO_PORT, BEEP_GPIO_PIN, BEEP_HIGH_TRIGGER);
}

/**
  * @brief  更新报警系统状态（需要在主循环中调用）
  * @param  无
  * @retval 无
  */
void ALARM_Update(void)
{
    if(!alarm_system.alarm_active)
    {
        return;
    }
    
    uint32_t current_time = (uint32_t)SysTick_GetCount();
    
    /* LED闪烁控制 */
    if(alarm_system.flash_enabled)
    {
        if((current_time - last_flash_time) >= flash_interval)
        {
            last_flash_time = current_time;
            flash_state = !flash_state;
            
            if(flash_state)
            {
                LED_ON(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
            }
            else
            {
                LED_OFF(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
            }
        }
    }
    
    /* 蜂鸣器控制 */
    if(alarm_system.beep_enabled)
    {
        if((current_time - last_beep_time) >= beep_interval)
        {
            last_beep_time = current_time;
            beep_state = !beep_state;
            
            if(beep_state)
            {
                BEEP_ON(BEEP_GPIO_PORT, BEEP_GPIO_PIN, BEEP_HIGH_TRIGGER);
            }
            else
            {
                BEEP_OFF(BEEP_GPIO_PORT, BEEP_GPIO_PIN, BEEP_HIGH_TRIGGER);
            }
        }
    }
}

/**
  * @brief  设置闪烁使能
  * @param  enable: 使能状态 (1=使能, 0=禁用)
  * @retval 无
  */
void ALARM_SetFlashEnable(uint8_t enable)
{
    alarm_system.flash_enabled = enable;
    if(!enable)
    {
        LED_OFF(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
    }
}

/**
  * @brief  设置蜂鸣器使能
  * @param  enable: 使能状态 (1=使能, 0=禁用)
  * @retval 无
  */
void ALARM_SetBeepEnable(uint8_t enable)
{
    alarm_system.beep_enabled = enable;
    if(!enable)
    {
        BEEP_OFF(BEEP_GPIO_PORT, BEEP_GPIO_PIN, BEEP_HIGH_TRIGGER);
    }
}

/**
  * @brief  检查报警是否激活
  * @param  无
  * @retval 报警激活状态 (1=激活, 0=未激活)
  */
uint8_t ALARM_IsActive(void)
{
    return alarm_system.alarm_active;
}

/**
  * @brief  清除所有报警
  * @param  无
  * @retval 无
  */
void ALARM_ClearAll(void)
{
    ALARM_Stop();
    alarm_system.alarm_type = ALARM_NONE;
    alarm_system.alarm_level = ALARM_LEVEL_LOW;
    alarm_system.alarm_duration = 0;
}

/**
  * @brief  检查温度是否异常
  * @param  temperature: 当前温度值
  * @param  min_temp: 最小温度阈值
  * @param  max_temp: 最大温度阈值
  * @retval 异常状态 (1=异常, 0=正常)
  */
uint8_t ALARM_CheckTemperature(float temperature, float min_temp, float max_temp)
{
    if(temperature < min_temp || temperature > max_temp)
    {
        /* 判断异常级别 */
        if(temperature < min_temp - 10.0f || temperature > max_temp + 10.0f)
        {
            ALARM_Start(ALARM_TEMPERATURE, ALARM_LEVEL_CRITICAL);
        }
        else if(temperature < min_temp - 5.0f || temperature > max_temp + 5.0f)
        {
            ALARM_Start(ALARM_TEMPERATURE, ALARM_LEVEL_HIGH);
        }
        else
        {
            ALARM_Start(ALARM_TEMPERATURE, ALARM_LEVEL_MEDIUM);
        }
        return 1;
    }
    return 0;
}

/**
  * @brief  检查湿度是否异常
  * @param  humidity: 当前湿度值
  * @param  min_humidity: 最小湿度阈值
  * @param  max_humidity: 最大湿度阈值
  * @retval 异常状态 (1=异常, 0=正常)
  */
uint8_t ALARM_CheckHumidity(float humidity, float min_humidity, float max_humidity)
{
    if(humidity < min_humidity || humidity > max_humidity)
    {
        /* 判断异常级别 */
        if(humidity < min_humidity - 20.0f || humidity > max_humidity + 20.0f)
        {
            ALARM_Start(ALARM_HUMIDITY, ALARM_LEVEL_CRITICAL);
        }
        else if(humidity < min_humidity - 10.0f || humidity > max_humidity + 10.0f)
        {
            ALARM_Start(ALARM_HUMIDITY, ALARM_LEVEL_HIGH);
        }
        else
        {
            ALARM_Start(ALARM_HUMIDITY, ALARM_LEVEL_MEDIUM);
        }
        return 1;
    }
    return 0;
}

/**
  * @brief  检查空气质量是否异常
  * @param  air_quality: 当前空气质量值
  * @param  threshold: 阈值
  * @retval 异常状态 (1=异常, 0=正常)
  */
uint8_t ALARM_CheckAirQuality(uint16_t air_quality, uint16_t threshold)
{
    if(air_quality > threshold)
    {
        /* 判断异常级别 */
        if(air_quality > threshold * 2)
        {
            ALARM_Start(ALARM_AIR_QUALITY, ALARM_LEVEL_CRITICAL);
        }
        else if(air_quality > threshold * 1.5)
        {
            ALARM_Start(ALARM_AIR_QUALITY, ALARM_LEVEL_HIGH);
        }
        else
        {
            ALARM_Start(ALARM_AIR_QUALITY, ALARM_LEVEL_MEDIUM);
        }
        return 1;
    }
    return 0;
}

/**
  * @brief  检查震动是否检测到
  * @param  shake_detected: 震动检测标志
  * @retval 异常状态 (1=异常, 0=正常)
  */
uint8_t ALARM_CheckShake(uint8_t shake_detected)
{
    if(shake_detected)
    {
        ALARM_Start(ALARM_SHAKE, ALARM_LEVEL_HIGH);
        return 1;
    }
    return 0;
}

/**
  * @brief  检查倾斜是否检测到
  * @param  tilt_detected: 倾斜检测标志
  * @retval 异常状态 (1=异常, 0=正常)
  */
uint8_t ALARM_CheckTilt(uint8_t tilt_detected)
{
    if(tilt_detected)
    {
        ALARM_Start(ALARM_TILT, ALARM_LEVEL_HIGH);
        return 1;
    }
    return 0;
}




