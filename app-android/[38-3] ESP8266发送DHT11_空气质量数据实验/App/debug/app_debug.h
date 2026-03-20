#ifndef __APP_DEBUG_H
#define __APP_DEBUG_H

#include "stm32f10x.h"//或#include "stdint.h"

void Debug_ReadBufferReset(void);
void Debug_TaskInit(void);
void Debug_Task(void);

#endif /* __APP_DEBUG_H */
