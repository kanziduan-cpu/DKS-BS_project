/**
  ******************************************************************************
  * @file       main.c
  * @author     embedfire
  * @version     V1.0
  * @date        2025
  * @brief      ESP8266接收控制蜂鸣器_LED灯_舵机实验
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
#include "beep/bsp_gpio_beep.h"
#include "servo/bsp_servo.h"

/**
  * @brief  主函数
  * @param  无
  * @note   无
  * @retval 无
  */
int main(void)
{
    NVIC_PriorityGroupConfig(NVIC_PriorityGroup_4);
    
    LED_GPIO_Config();
    DWT_Init();
    DEBUG_USART_Init();
    ESP8266_Init();
    BEEP_GPIO_Config();
    SERVO_TIM_Init();
    
    printf("\r\n----ESP8266接收控制蜂鸣器_LED灯_舵机实验----\r\n");
    
    ProgramRunLed_TaskInit(500);
    Debug_TaskInit();
    ESP8266_TaskInit();
    BEEP_TaskInit(30);
    Servo_TaskInit(100);
    
    SysTick_Init();

    while(1)
    {
 
        /* 程序正常运行指示灯 */
        ProgramRunLed_Task();
                
        /* DEBUG串口数据处理 */
        Debug_Task();
        
        /* ESP8266串口数据处理 */
        ESP8266_Task();
        
        /* ESP8266连接WIFI与透传测试 */
        ESP8266_StaTcpClient_Unvarnish_ConfigTest();
        
        /* 蜂鸣器任务 */
        BEEP_Task();

        /* 舵机任务 */
        Servo_Task(&servo1_task);
        Servo_Task(&servo2_task);
    }
}

/*****************************END OF FILE***************************************/
