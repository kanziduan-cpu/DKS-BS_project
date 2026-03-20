/**
  ******************************************************************************
  * @file       app_beep.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      蜂鸣器应用层功能接口
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
  
#include "beep/app_beep.h"
#include "usart/usart_com.h"
#include "systick/bsp_systick.h"

BEEP_TaskInfo beep_task = {0};


/**
 * @brief  蜂鸣器 计数复位
 * @param  无
 * @retval 无
 */
void BEEP_TaskReset(void)
{
    beep_task.timer = beep_task.cycle;
    beep_task.flag = 0;
}  

/**
 * @brief  蜂鸣器 任务初始化
 * @param  beep_task_cycle: 任务轮询周期 单位ms(可修改系统节拍定时器)
 * @retval 无
 */
void BEEP_TaskInit(uint32_t beep_task_cycle)
{
    beep_task.cycle           = beep_task_cycle;
    beep_task.speed           = 0;
    beep_task.beep_active     = false;
    BEEP_TaskReset();
}

/**
  * @brief  蜂鸣器控制处理函数
  * @param  无
  * @retval 无
  */
void BEEP_Process(void) 
{
    /* 判断蜂鸣器是否启动 */
    if(beep_task.beep_active == true)
    {
        if(beep_task.trigger_flag == 0)
        {
            beep_task.trigger_time = SysTick_GetCount();
            beep_task.trigger_flag = 1;
        }
        /* 到达延时时间翻转蜂鸣器 */
        if(SysTick_GetCount() - beep_task.trigger_time >= beep_task.speed)
        {
            BEEP_TOGGLE(BEEP_GPIO_PORT,BEEP_GPIO_PIN);
            beep_task.trigger_flag = 0;
        }
    }
    else
    {
        BEEP_OFF(BEEP_GPIO_PORT,BEEP_GPIO_PIN,BEEP_HIGH_TRIGGER);
    }
}

/**
 * @brief  蜂鸣器 任务
 * @param  无
 * @retval 无
 */
void BEEP_Task(void)
{
    if(beep_task.flag)      
    {
        BEEP_Process();  
        BEEP_TaskReset();        
    }
}

/*********************************************END OF FILE**********************/
