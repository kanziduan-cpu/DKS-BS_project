/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_GPIO_RAIN_H
#define __BSP_GPIO_RAIN_H

#include "stm32f10x.h"


/* 定义RAIN连接的GPIO端口, 用户只需要修改下面的代码即可改变控制的IO引脚 */
// RAIN SIG
#define RAIN_SIG_GPIO_PORT    			    GPIOA			                /* GPIO绔彛 */
#define RAIN_SIG_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOA			/* GPIO绔彛鏃堕挓 */
#define RAIN_SIG_GPIO_PIN			        GPIO_Pin_5	       			/* 杩炴帴鍒癎PIO */


void RAIN_GPIO_Config(void);
float Calculate_RainHeight(uint16_t adc_value); 

#endif /* __BSP_GPIO_RAIN_H  */
