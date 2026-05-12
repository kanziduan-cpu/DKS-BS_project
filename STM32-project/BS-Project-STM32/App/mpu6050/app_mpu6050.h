/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __APP_MPU6050_H
#define __APP_MPU6050_H

#include "stm32f10x.h"

#define MPU6050_TASK_PERIOD_MS 100U
#define MPU6050_TILT_DELTA_ALARM_THRESHOLD 6.0f
#define MPU6050_GYRO_MOTION_ALARM_THRESHOLD_DPS 180.0f

typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
    short Accel[3];
    short Gyro[3];
    float Temp;
    float Tilt[3];
    float TiltDelta;
    uint16_t VibrationLevel;
    uint8_t AlarmLatched;
    uint8_t AlarmReportPending;
}MPU6050_TaskInfo;

extern MPU6050_TaskInfo mpu6050_task;

void MPU6050_TaskReset(void);
void MPU6050_TaskInit(uint32_t mpu6050_task_cycle);
void MPU6050_Task(void);
void MPU6050_Handle(void);
uint8_t MPU6050_IsAlarmLatched(void);
uint8_t MPU6050_IsAlarmReportPending(void);
void MPU6050_ClearAlarmReportPending(void);
void MPU6050_ClearAlarm(void);

#endif /* __APP_MPU6050_H */
