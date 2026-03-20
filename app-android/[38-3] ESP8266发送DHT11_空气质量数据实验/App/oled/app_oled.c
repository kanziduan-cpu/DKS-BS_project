/**
  ******************************************************************************
  * @file       app_oled.c
  * @author     embedfire
  * @version     V1.0
  * @date        2025
  * @brief      OLED屏 应用层功能接口
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

#include "oled/app_oled.h"
#include "usart/usart_com.h"
#include "systick/bsp_systick.h"
#include "oled/bsp_i2c_oled.h"
#include <stdio.h>
#include <stdlib.h>
#include "mpu6050/app_mpu6050.h"

OLED_TaskInfo oled_task  = {0};


/**
  * @brief  OLED屏 计数复位
  * @param  无
  * @retval 无
  */
void OLED_TaskReset(void)
{
    oled_task.timer = oled_task.cycle;
    oled_task.flag  = 0;

}

/**
  * @brief  OLED屏器 任务初始化
  * @param  oled_task_cycle: 任务轮询周期 单位ms(可修改系统节拍定时器)
  * @retval 无
  */
void OLED_TaskInit(uint32_t oled_task_cycle)
{
    oled_task.cycle = oled_task_cycle;
    oled_task.refresh_flag = 0;

    OLED_TaskReset();
}

/**
  * @brief  OLED屏 任务
  * @param  无
  * @retval 无
  */
void OLED_Task(void)
{
   //刷新屏幕标志置1     
   if(oled_task.refresh_flag == 1)
   {
        oled_task.refresh_flag = 0;
 
        //显示数据
        OLED_ShowString(1,0,(uint8_t*)mpu6050_task.Acceleration,TEXTSIZE_F6X8);
        OLED_ShowString(4,0,(uint8_t*)mpu6050_task.Gyroscope,TEXTSIZE_F6X8);   
        OLED_ShowString(7,6*8,(uint8_t*)mpu6050_task.Temperature,TEXTSIZE_F6X8); 
   }           

}



/*****************************END OF FILE***************************************/
