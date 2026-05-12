/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __APP_BEEP_H
#define	__APP_BEEP_H

#include "stm32f10x.h"
#include "beep/bsp_gpio_beep.h"
#include <stdbool.h>

typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
    uint16_t  speed;         //閫熷害
    bool     beep_active;    
    uint32_t trigger_time;
    uint8_t  trigger_flag;
}BEEP_TaskInfo;


extern BEEP_TaskInfo beep_task;

void BEEP_TaskReset(void);
void BEEP_TaskInit(uint32_t beep_task_cycle);
void BEEP_Task(void);
void BEEP_Process(void);

#endif /* __APP_BEEP_H */
