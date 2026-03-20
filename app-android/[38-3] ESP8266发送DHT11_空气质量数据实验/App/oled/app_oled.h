#ifndef __APP_OLED_H
#define __APP_OLED_H

#include "stm32f10x.h"//或#include "stdint.h"


typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
    uint8_t  refresh_flag;//刷新标志
}OLED_TaskInfo;

extern OLED_TaskInfo oled_task;


void OLED_TaskReset(void);
void OLED_TaskInit(uint32_t oled_task_cycle);
void OLED_Task(void);

#endif /* __APP_OLED_H */
