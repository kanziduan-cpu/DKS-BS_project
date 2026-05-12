/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
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
#include "i2c/bsp_i2c.h"
#include "mpu6050/bsp_i2c_mpu6050.h"
#include "beep/bsp_gpio_beep.h"
#include "servo/bsp_servo.h"

#define FW_BUILD_MARKER "FW-20260510-MQTT-ATFIX2"



int main(void)
{
    LED_GPIO_Config();
    DWT_Init();
  DEBUG_USART_Init();
  ESP8266_Init();
  DHT11_GPIO_Config();
  ADCX_Init();
    IIC_Init();
    MPU6050_Init();
  BEEP_GPIO_Config();
  SERVO_TIM_Init();

  printf("\r\n----ESP8266 multi-sensor MQTT demo----\r\n");
  printf("[build]%s\r\n", FW_BUILD_MARKER);

  ProgramRunLed_TaskInit(500);
  ESP8266_TaskInit();
  RainSoil_TaskInit(1000);
  Dht11_TaskInit(2000);
  MQ135_TaskInit(1000);
  MPU6050_TaskInit(MPU6050_TASK_PERIOD_MS);
  BEEP_TaskInit(1);
  Servo_TaskInit(20);

    SysTick_Init();

    while(1)
    {
    ProgramRunLed_Task();
    ESP8266_Task();
    ESP8266_StaTcpClient_Unvarnish_ConfigTest();
    RainSoil_Task();
    Dht11_Task();
    MQ135_Task();
        MPU6050_Task();
    BEEP_Task();
    Servo_Task(&servo1_task);
    Servo_Task(&servo2_task);

    ESP8266_CloudTask();

    }
}

/*****************************END OF FILE***************************************/
