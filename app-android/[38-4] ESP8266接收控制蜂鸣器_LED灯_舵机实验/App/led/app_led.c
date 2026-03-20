/**
  ******************************************************************************
  * @file       app_led.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      LED灯应用层功能接口
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
  
#include "led/app_led.h"
#include "led/bsp_gpio_led.h"

Led_TaskInfo program_run_led_task  = {0};

/**
  * @brief  程序正常运行指示灯 计数复位
  * @param  无
  * @retval 无
  */
void ProgramRunLed_TaskReset(void)
{
    program_run_led_task.timer = program_run_led_task.cycle;
    program_run_led_task.flag  = 0;

}

/**
  * @brief  程序正常运行指示灯 任务初始化
  * @param  program_run_led_task_cycle: 任务轮询周期 单位ms(可修改系统节拍定时器)
  * @retval 无
  */
void ProgramRunLed_TaskInit(uint32_t program_run_led_task_cycle)
{
    program_run_led_task.cycle = program_run_led_task_cycle;
    ProgramRunLed_TaskReset();
}


/**
  * @brief  程序正常运行指示灯 任务
  * @param  无
  * @retval 无
  */
void ProgramRunLed_Task(void)
{
    if(program_run_led_task.flag)
    {
        LED_TOGGLE(R_LED_GPIO_PORT,R_LED_GPIO_PIN);
        ProgramRunLed_TaskReset();
    }
}

/*****************************END OF FILE***************************************/
