/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_SERVO_H
#define __BSP_SERVO_H

#include "stm32f10x.h"



#define SERVO_NUM1 1
#define SERVO_NUM2 2

// SERVO_1
#define SERVO1_GPIO_PORT    			GPIOB			                /* GPIO绔彛 */
#define SERVO1_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			/* GPIO绔彛鏃堕挓 */
#define SERVO1_GPIO_PIN			        GPIO_Pin_0	       				/* 杩炴帴鍒癎PIO */

#define SERVO1_TIM_OCX_INIT 	        TIM_OC3Init			    //閫氶亾
#define SERVO1_TIM_OCXPRELOAD_CONFIG 	TIM_OC3PreloadConfig    

// SERVO_2
#define SERVO2_GPIO_PORT    			GPIOB			                /* GPIO绔彛 */
#define SERVO2_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			/* GPIO绔彛鏃堕挓 */
#define SERVO2_GPIO_PIN			        GPIO_Pin_1	       				/* 杩炴帴鍒癎PIO */

#define SERVO2_TIM_OCX_INIT 	        TIM_OC4Init			    //閫氶亾
#define SERVO2_TIM_OCXPRELOAD_CONFIG 	TIM_OC4PreloadConfig    

#define SERVO_TIM   			        TIM3   			
#define SERVO_TIM_CLK_PORT 	            RCC_APB1Periph_TIM3 		
#define SERVO_TIM_APBXCLKCMD   		    RCC_APB1PeriphClockCmd  
                                                  
#define SERVO_TIM_IRQ                   TIM3_IRQn         
#define SERVO_TIM_IRQHANDLER            TIM3_IRQHandler  


#define PWM_SERVO_PERIOD (1000-1)               //鍒濆鍛ㄦ湡
#define PWM_SERVO_PULSE  0                      //鍒濆鑴夊

void SERVO1_GPIO_Config(void);
void SERVO2_GPIO_Config(void);
void SERVO_TIM_NVIC_Config(void);
void SERVO_TIM_Mode_Config(void);
void SERVO_TIM_Init(void);
void SERVO_PulseConfig(uint16_t servo_num,uint16_t pwm_pulse);
void SERVO_CycleConfig(uint16_t pwm_cycle);
uint16_t SERVO_TimeCalculate(float time);
float SERVO_AngleToTime(uint16_t angle);

#endif /* __BSP_SERVO_H  */
