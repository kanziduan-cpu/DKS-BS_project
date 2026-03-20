#ifndef __APP_MQ135_H
#define __APP_MQ135_H

#include "stm32f10x.h"//或#include "stdint.h"

#define RL                               1     /* 根据硬件原理图可知：RL = 1k */ 
#define R0                               2     /* MQ135在洁净空气中的阻值，官方数据手册没有给出，这是实验测试得出，想要准确请多次测试 */ 
#define VC                               5.0   /* MQ135供电电压,根据实际供电修改，默认接5V */
#define A                                4.17  /* y=ax^b 的 a */
#define B                                -2.28 /* y=ax^b 的 b */

typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
    uint8_t  read_completed_flag;
    float    ppm;
}MQ135_TaskInfo;

extern MQ135_TaskInfo mq135_task;

float MQ135_Get_PPM(uint16_t adc_value);
uint16_t MQ135_ReadValue(void);

void MQ135_TaskReset(void);
void MQ135_TaskInit(uint32_t mq135_task_cycle);
void MQ135_Task(void);

#endif /* __APP_MQ135_H  */
