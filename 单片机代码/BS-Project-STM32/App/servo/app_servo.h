/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __APP_SERVO_H
#define	__APP_SERVO_H

#include "stm32f10x.h"
#include "servo/bsp_servo.h"

typedef struct
{
    uint32_t cycle;
    uint32_t timer;
    uint8_t  flag;
    uint8_t  id;
    uint8_t  turn_left_flag;
    uint8_t  turn_right_flag;
    uint8_t  current_angle;
}Servo_TaskInfo;

#define SERVO_CHANGE_ANGLE  10      //变化角度

extern Servo_TaskInfo servo1_task;
extern Servo_TaskInfo servo2_task;

void Servo_TaskReset(Servo_TaskInfo* servo_task);
uint16_t Get_ServoAngle(Servo_TaskInfo* servo_task);
void Servo_AngleConfig(uint16_t servo_num,uint16_t angle);

void Servo_TaskInit(uint32_t servo_task_cycle);
void Servo_Task(Servo_TaskInfo* servo_task);


#endif /* __APP_SERVO_H */

