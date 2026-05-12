/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __APP_LED_H
#define __APP_LED_H

#include "stm32f10x.h"

typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
}Led_TaskInfo;


extern Led_TaskInfo program_run_led_task;

void ProgramRunLed_TaskReset(void);
void ProgramRunLed_TaskInit(uint32_t program_run_led_task_cycle);
void ProgramRunLed_Task(void);

#endif /* __APP_LED_H */
