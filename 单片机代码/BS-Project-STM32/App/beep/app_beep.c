/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "beep/app_beep.h"
#include "usart/usart_com.h"
#include "systick/bsp_systick.h"

BEEP_TaskInfo beep_task = {0};



void BEEP_TaskReset(void)
{
    beep_task.timer = beep_task.cycle;
    beep_task.flag = 0;
}  


void BEEP_TaskInit(uint32_t beep_task_cycle)
{
    beep_task.cycle           = beep_task_cycle;
    beep_task.speed           = 0;
    beep_task.beep_active     = false;
    BEEP_TaskReset();
}


void BEEP_Process(void) 
{
    
    if(beep_task.beep_active == true)
    {
        if(beep_task.trigger_flag == 0)
        {
            beep_task.trigger_time = SysTick_GetCount();
            beep_task.trigger_flag = 1;
        }
        
        if(SysTick_GetCount() - beep_task.trigger_time >= beep_task.speed)
        {
            BEEP_TOGGLE(BEEP_GPIO_PORT,BEEP_GPIO_PIN);
            beep_task.trigger_flag = 0;
        }
    }
    else
    {
        BEEP_OFF(BEEP_GPIO_PORT,BEEP_GPIO_PIN,BEEP_HIGH_TRIGGER);
    }
}


void BEEP_Task(void)
{
    if(beep_task.flag)      
    {
        BEEP_Process();  
        BEEP_TaskReset();        
    }
}

/*********************************************END OF FILE**********************/
