/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __APP_ADC_H
#define __APP_ADC_H

#include "stm32f10x.h"

typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
}RainSoil_TaskInfo;

extern RainSoil_TaskInfo rain_soil_task;

void RainSoil_TaskReset(void);
void RainSoil_TaskInit(uint32_t rain_soil_task_cycle);
void RainSoil_Task(void);

#endif /* __APP_ADC_H  */
