/**
  ******************************************************************************
  * @file       app_key.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      按键应用层功能接口
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

#include "key/app_key.h"
#include "key/bsp_gpio_key.h"
#include "debug/bsp_debug.h"
#include "usart/usart_com.h"

Key_TaskInfo key_task  = {0};

/**
  * @brief  按键 计数复位
  * @param  无
  * @retval 无
  */
void Key_TaskReset(void)
{
    key_task.timer = key_task.cycle;
    key_task.flag  = 0;

}

/**
  * @brief  按键 任务初始化
  * @param  key_task_cycle: 任务轮询周期 单位ms(可修改系统节拍定时器)
  * @retval 无
  */
void Key_TaskInit(uint32_t key_task_cycle)
{
    key_task.cycle = key_task_cycle;
    Key_TaskReset();
}


/**
  * @brief  按键 任务
  * @param  无
  * @retval 无
  */
void Key_Task(void)
{
    if(key_task.flag)
    {
        KEY_Event current_event = KEY_SystickScan(&key1_info);

        if(current_event == EVENT_SHORT_RELEASE)
        {
            USARTX_SendString(DEBUG_USARTX, "embedfire hello!\r\n");
            USARTX_SendString(DEBUG_USARTX, "我是KEY1,debug数据\r\n");
        }
        Key_TaskReset();
    }
}


/*****************************END OF FILE***************************************/
