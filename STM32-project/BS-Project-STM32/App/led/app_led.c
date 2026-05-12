/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "led/app_led.h"
#include "led/bsp_gpio_led.h"

Led_TaskInfo program_run_led_task  = {0};


void ProgramRunLed_TaskReset(void)
{
    program_run_led_task.timer = program_run_led_task.cycle;
    program_run_led_task.flag  = 0;

}


void ProgramRunLed_TaskInit(uint32_t program_run_led_task_cycle)
{
    program_run_led_task.cycle = program_run_led_task_cycle;
    ProgramRunLed_TaskReset();
}



void ProgramRunLed_Task(void)
{
    if(program_run_led_task.flag)
    {
        LED_TOGGLE(R_LED_GPIO_PORT,R_LED_GPIO_PIN);
        ProgramRunLed_TaskReset();
    }
}

/*****************************END OF FILE***************************************/
