/**
  ******************************************************************************
  * @file       app_servo.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      舵机应用层功能接口
  ******************************************************************************
  * @attention
  *
  * 实验平台  ：野火 STM32F103C8T6-STM32开发板 
  * 论坛      ：http://www.firebbs.cn
  * 官网      ：https://embedfire.com/
  * 淘宝      ：https://yehuosm.tmall.com/
  *
  ******************************************************************************
  */
  
#include "servo/app_servo.h"
#include "servo/bsp_servo.h"

Servo_TaskInfo servo1_task = {0};
Servo_TaskInfo servo2_task = {0};

/**
 * @brief  Servo 计数复位
 * @param  无
 * @retval 无
 */
void Servo_TaskReset(Servo_TaskInfo* servo_task)
{
    servo_task->timer = servo1_task.cycle;
    servo_task->flag = 0;
    servo_task->turn_left_flag = 0;
    servo_task->turn_right_flag = 0;
}  

/**
  * @brief  获取舵机角度
  * @param  无
  * @retval 当前舵机角度
  */
uint16_t Get_ServoAngle(Servo_TaskInfo* servo_task)
{
    return servo_task->current_angle;
}

/**
  * @brief  设置舵机角度
  * @param  current_angle：舵机角度（0-180）
  * @retval 无
  */
void Servo_AngleConfig(uint16_t servo_num,uint16_t angle)
{	
    SERVO_PulseConfig(servo_num,SERVO_TimeCalculate(SERVO_AngleToTime(angle)));
    
    if(servo_num == SERVO_NUM1)
    {
        servo1_task.current_angle = angle;
    }
    if(servo_num == SERVO_NUM2)
    {
        servo2_task.current_angle = angle;
    }
}


/**
 * @brief  Servo 任务初始化
 * @param  servo_task_cycle: 任务轮询周期 单位ms(可修改系统节拍定时器)
 * @retval 无
 */
void Servo_TaskInit(uint32_t servo_task_cycle)
{
    servo1_task.cycle   = servo_task_cycle;
    servo2_task.cycle   = servo_task_cycle;
    servo1_task.id      = SERVO_NUM1;
    servo2_task.id      = SERVO_NUM2;
    
    SERVO_CycleConfig(SERVO_TimeCalculate(20));
    
    Servo_AngleConfig(servo1_task.id,0);
    Servo_AngleConfig(servo2_task.id,0);
    
	Servo_TaskReset(&servo1_task);
    Servo_TaskReset(&servo2_task);
}

/**
 * @brief  Servo 任务
 * @param  无
 * @retval 无
 */
void Servo_Task(Servo_TaskInfo* servo_task)
{
    if(servo_task->flag)      
    {
        if(servo_task->turn_right_flag)
        {
            if(Get_ServoAngle(servo_task)-SERVO_CHANGE_ANGLE>=0)
            {
                Servo_AngleConfig(servo_task->id,Get_ServoAngle(servo_task)-SERVO_CHANGE_ANGLE);
            }
        }
        if(servo_task->turn_left_flag)
        {
            if(Get_ServoAngle(servo_task)+SERVO_CHANGE_ANGLE<=180)
            {
                Servo_AngleConfig(servo_task->id,Get_ServoAngle(servo_task)+SERVO_CHANGE_ANGLE);
            }
        }
        Servo_TaskReset(servo_task);        
    }
}

/*********************************************END OF FILE**********************/
