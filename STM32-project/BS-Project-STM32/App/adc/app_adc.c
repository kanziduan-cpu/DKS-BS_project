/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "adc/app_adc.h"
#include "adc/bsp_adc.h"
#include "rain/bsp_gpio_rain.h"

RainSoil_TaskInfo rain_soil_task = {0};


void RainSoil_TaskReset(void)
{
    rain_soil_task.timer  = rain_soil_task.cycle;
    rain_soil_task.flag   = 0;
}


void RainSoil_TaskInit(uint32_t rain_soil_task_cycle)
{
    rain_soil_task.cycle   = rain_soil_task_cycle;
    RainSoil_TaskReset();
}


void RainSoil_Task(void)
{
    if(rain_soil_task.flag == 1)
    {
        RainSoil_TaskReset();
    
    }
}
 
/*****************************END OF FILE***************************************/
