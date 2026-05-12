/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __APP_DEBUG_H
#define __APP_DEBUG_H

#include "stm32f10x.h"

void Debug_ReadBufferReset(void);
void Debug_TaskInit(void);
void Debug_Task(void);

#endif /* __APP_DEBUG_H */
