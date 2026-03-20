/**
  ******************************************************************************
  * @file       main.c
  * @author     embedfire
  * @version     V1.0
  * @date        2025
  * @brief      ESP8266发送DHT11_空气质量数据实验
  ******************************************************************************
  * @attention
  *
  * 实验平台  ：野火 STM32F103C8T6-STM32开发板 
  * 论坛      ：http://www.firebbs.cn
  * 官网      ：https://embedfire.com/
  * 淘宝      ：https://yehuosm.tmall.com/
  *
  ******************************************************************************
  */
#include "main.h"
#include "stm32f10x.h"
#include "led/bsp_gpio_led.h"
#include "systick/bsp_systick.h"
#include "usart/usart_com.h"
#include "debug/bsp_debug.h"
#include "esp8266/bsp_esp8266.h"
#include "dwt/bsp_dwt.h"
#include "dht11/bsp_dht11.h"
#include "adc/bsp_adc.h"
#include "mpu6050/bsp_i2c_mpu6050.h"
#include "tilt/bsp_gpio_tilt.h"
#include "alarm/bsp_alarm.h"
#include "control/bsp_control.h"
#include "beep/bsp_gpio_beep.h"

/**
  * @brief  主函数
  * @param  无
  * @note   无
  * @retval 无
  */
int main(void)
{
    float temperature = 0, humidity = 0;
    uint16_t air_quality = 0;
    uint16_t water_level = 0;
    uint8_t shake_detected = 0;
    uint8_t tilt_detected = 0;
    uint32_t mqtt_publish_timer = 0;
    uint32_t mqtt_publish_interval = 5000;  /* MQTT发布间隔5秒 */
    uint8_t mqtt_connected = 0;
    
    NVIC_PriorityGroupConfig(NVIC_PriorityGroup_4);
    
    LED_GPIO_Config();
    DWT_Init();
    DEBUG_USART_Init();
    ESP8266_Init();
    DHT11_GPIO_Config();
    ADCX_Init();
    
    /* 初始化MPU6050姿态传感器 */
    MPU6050_Init();
    printf("MPU6050 initialized\r\n");
    
    /* 初始化倾斜传感器 */
    TILT_Init();
    printf("Tilt sensor initialized\r\n");
    
    /* 初始化报警系统 */
    ALARM_Init();
    printf("Alarm system initialized\r\n");
    
    /* 初始化控制系统 */
    CONTROL_Init();
    printf("Control system initialized\r\n");
    
    /* 初始化蜂鸣器 */
    BEEP_GPIO_Config();
    printf("Buzzer initialized\r\n");
    
    printf("\r\n----ESP8266多传感器监控系统----\r\n");
    printf("DHT11温湿度、空气质量、MPU6050姿态、倾斜传感器、声光报警、舵机控制\r\n\r\n");
    printf("引脚映射(基于野火原理图): ESP8266->PB10/PB11, MQ135_AO->PA4, DHT11->PB12, 舵机1->PB0, 蜂鸣器->PA6, 雨量(IO1)->PB13\r\n");
    
    ProgramRunLed_TaskInit(500);
    Debug_TaskInit();
    ESP8266_TaskInit();
    Dht11_TaskInit(2000);
    MQ135_TaskInit(1000);
    
    SysTick_Init();
    
    /* 连接WiFi */
    printf("开始连接WiFi...\r\n");
    if(ESP8266_ConnectWiFi())
    {
        printf("WiFi连接成功\r\n");
        
        /* 连接MQTT服务器 */
        printf("开始连接MQTT服务器...\r\n");
        if(ESP8266_CheckMQTTCapability() && ESP8266_ConnectMQTT())
        {
            printf("MQTT服务器连接成功\r\n");
            
            /* 订阅MQTT主题 */
            if(ESP8266_MQTTSubscribe())
            {
                printf("MQTT主题订阅成功\r\n");
                mqtt_connected = 1;
                mqtt_publish_timer = DWT_GetTick();
                /* 上线后先回报一次设备在线状态，便于安卓端初始化设备列表 */
                ESP8266_SendDeviceStatusToMQTT("STM32_MAIN", 1);
                ESP8266_SendDeviceStatusToMQTT("FAN_SYS", CONTROL_GetVentState() ? 1 : 0);
                ESP8266_SendDeviceStatusToMQTT("DH_SYS", 0);
                ESP8266_SendDeviceStatusToMQTT("LIGHT_SYS", 0);
                ESP8266_SendDeviceStatusToMQTT("PUMP_SYS", 0);
            }
            else
            {
                printf("MQTT主题订阅失败\r\n");
            }
        }
        else
        {
            printf("MQTT服务器连接失败\r\n");
        }
    }
    else
    {
        printf("WiFi连接失败\r\n");
    }
    
    printf("\r\n系统初始化完成，开始运行...\r\n\r\n");

    while(1)
    {
        /* 程序正常运行指示灯 */
        ProgramRunLed_Task();
                
        /* DEBUG串口数据处理 */
        Debug_Task();
        
        /* ESP8266串口数据处理 */
        ESP8266_Task();
        
        /* DHT11温湿度传感器任务 */
        Dht11_Task();
        
        /* MQ135_ADC 任务 */
        MQ135_Task();
        
        /* 读取传感器数据 */
        DHT11_Read_Data(&temperature, &humidity);
        air_quality = MQ135_ReadValue();
        shake_detected = shake_detected_flag;
        tilt_detected = TILT_ReadStatus();
        water_level = ESP8266_UpdateWaterLevelByRain(tilt_detected);
        
        /* 更新报警系统 - 检查各种异常情况（雨量模块复用倾斜IO） */
        ALARM_CheckTemperature(temperature, 15.0f, 35.0f);  /* 温度范围15-35度 */
        ALARM_CheckHumidity(humidity, 30.0f, 80.0f);      /* 湿度范围30-80% */
        ALARM_CheckAirQuality(air_quality, 150);          /* 空气质量阈值150 */
        ALARM_CheckShake(shake_detected);
        ALARM_CheckTilt(water_level >= ESP8266_GetWaterLimitThreshold() ? 1 : 0);
        
        /* 更新报警显示和蜂鸣器 */
        ALARM_Update();
        
        /* 更新控制系统 - 检查触发条件并控制舵机 */
        CONTROL_Update();
        
        /* 检查并处理MQTT消息（接收控制指令） */
        if(mqtt_connected)
        {
            ESP8266_CheckMQTTMessage();
            
            /* 定时发送传感器数据到MQTT */
            if(DWT_GetTick() - mqtt_publish_timer >= mqtt_publish_interval)
            {
                ESP8266_SendSensorDataToMQTT();
                mqtt_publish_timer = DWT_GetTick();
                printf("已发送传感器数据到MQTT服务器, water_level=%dcm\r\n", water_level);
            }
        }
        
        /* 如果有报警，清除标志 */
        if(ALARM_IsActive())
        {
            /* 可以在这里添加延时，避免频繁报警 */
            DWT_DelayUs(100000);  /* 延时100ms */
        }
    }
}

/*****************************END OF FILE***************************************/






