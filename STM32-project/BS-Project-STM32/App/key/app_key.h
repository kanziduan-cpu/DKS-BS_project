/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __APP_KEY_H
#define __APP_KEY_H

#include "stm32f10x.h"

typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
    char     device_id[10];
}Key_TaskInfo;


extern Key_TaskInfo key_task;

void Key_TaskReset(void);
void Key_TaskInit(uint32_t key_task_cycle);
void Key_Task(void);

#endif /* __APP_KEY_H */

