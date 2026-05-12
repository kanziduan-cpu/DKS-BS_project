/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "key/app_key.h"
#include "key/bsp_gpio_key.h"
#include "debug/bsp_debug.h"
#include "usart/usart_com.h"

Key_TaskInfo key_task  = {0};


void Key_TaskReset(void)
{
    key_task.timer = key_task.cycle;
    key_task.flag  = 0;

}


void Key_TaskInit(uint32_t key_task_cycle)
{
    key_task.cycle = key_task_cycle;
    Key_TaskReset();
}



void Key_Task(void)
{
    if(key_task.flag)
    {
        KEY_Event current_event = KEY_SystickScan(&key1_info);

        if(current_event == EVENT_SHORT_RELEASE)
        {
            USARTX_SendString(DEBUG_USARTX, "embedfire hello!\r\n");
            USARTX_SendString(DEBUG_USARTX, "鎴戞槸KEY1,debug鏁版嵁\r\n");
        }
        Key_TaskReset();
    }
}


/*****************************END OF FILE***************************************/
