/**
  ******************************************************************************
  * @file       app_debug.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      DEBUG串口 应用层功能接口
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

#include "debug/app_debug.h"
#include "debug/bsp_debug.h"
#include "usart/usart_com.h"
#include <string.h>
#include "esp8266/bsp_esp8266.h"
#include "esp8266/app_esp8266.h"

/**
  * @brief  DEBUG串口 接收缓冲区复位
  * @param  无
  * @retval 无
  */
void Debug_ReadBufferReset(void)
{
    memset(debug_receive.buffer,NULL,debug_receive.len);
    debug_receive.len = 0;
    debug_receive.read_flag = 0;
}

/**
  * @brief  DEBUG串口 任务初始化
  * @param  无
  * @retval 无
  */
void Debug_TaskInit(void)
{
    Debug_ReadBufferReset();
}

/**
  * @brief  DEBUG串口 任务
  * @param  无
  * @retval 无
  */
void Debug_Task(void)
{
    if(debug_receive.read_flag)
    {
        //转发给esp8266
        USARTX_SendArray(ESP8266_USARTX,debug_receive.buffer,debug_receive.len);
        //esp8266配置完成才进行任务
        if(esp8266_configuration_completed_flag == 1)
        {
            Get_ESP82666_Cmd((char *)debug_receive.buffer);
        }
        Debug_ReadBufferReset();
    }
}


/*****************************END OF FILE***************************************/
