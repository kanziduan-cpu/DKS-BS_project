#ifndef __APP_MPU6050_H
#define __APP_MPU6050_H

#include "stm32f10x.h"//或#include "stdint.h"


typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
    char     Acceleration[24];
    char     Gyroscope[24];
    char     Temperature[16];
    short Accel[3];
    short Gyro[3];
    float Temp;
    
}MPU6050_TaskInfo;

extern MPU6050_TaskInfo mpu6050_task;


void MPU6050_TaskReset(void);
void MPU6050_TaskInit(uint32_t mpu6050_task_cycle);
void MPU6050_Task(void);
void MPU6050_Handle(void);

#endif /* __APP_MPU6050_H */
