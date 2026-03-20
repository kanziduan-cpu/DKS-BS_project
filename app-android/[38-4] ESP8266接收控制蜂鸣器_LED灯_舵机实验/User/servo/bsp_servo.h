#ifndef __BSP_SERVO_H
#define __BSP_SERVO_H

#include "stm32f10x.h"

/* 定义 SERVO 连接的GPIO端口, 用户只需要修改下面的代码即可改变控制的 SERVO 引脚 */

#define SERVO_NUM1 1
#define SERVO_NUM2 2

// SERVO_1
#define SERVO1_GPIO_PORT    			GPIOB			                /* GPIO端口 */
#define SERVO1_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			/* GPIO端口时钟 */
#define SERVO1_GPIO_PIN			        GPIO_Pin_0	       				/* 连接到GPIO */

#define SERVO1_TIM_OCX_INIT 	        TIM_OC3Init			    //通道
#define SERVO1_TIM_OCXPRELOAD_CONFIG 	TIM_OC3PreloadConfig    //预装载

// SERVO_2
#define SERVO2_GPIO_PORT    			GPIOB			                /* GPIO端口 */
#define SERVO2_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			/* GPIO端口时钟 */
#define SERVO2_GPIO_PIN			        GPIO_Pin_1	       				/* 连接到GPIO */

#define SERVO2_TIM_OCX_INIT 	        TIM_OC4Init			    //通道
#define SERVO2_TIM_OCXPRELOAD_CONFIG 	TIM_OC4PreloadConfig    //预装载

#define SERVO_TIM   			        TIM3   			
#define SERVO_TIM_CLK_PORT 	            RCC_APB1Periph_TIM3 		
#define SERVO_TIM_APBXCLKCMD   		    RCC_APB1PeriphClockCmd  
                                                  
#define SERVO_TIM_IRQ                   TIM3_IRQn         
#define SERVO_TIM_IRQHANDLER            TIM3_IRQHandler  


#define PWM_SERVO_PERIOD (1000-1)               //初始周期
#define PWM_SERVO_PULSE  0                      //初始脉宽

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
