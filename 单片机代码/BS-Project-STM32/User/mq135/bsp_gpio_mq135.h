/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_GPIO_MQ135_H
#define __BSP_GPIO_MQ135_H

#include "stm32f10x.h"



// MQ135 DO
#define MQ135_DO_GPIO_PORT    			        GPIOA			                /* GPIO绔彛 */
#define MQ135_DO_GPIO_CLK_PORT 	                RCC_APB2Periph_GPIOA			/* GPIO绔彛鏃堕挓 */
#define MQ135_DO_GPIO_PIN			            GPIO_Pin_11	       				/* 杩炴帴鍒癎PIO */

// MQ135 AO
#define MQ135_AO_GPIO_PORT    			        GPIOA			                /* GPIO绔彛 */
#define MQ135_AO_GPIO_CLK_PORT 	                RCC_APB2Periph_GPIOA			/* GPIO绔彛鏃堕挓 */
#define MQ135_AO_GPIO_PIN			            GPIO_Pin_4	       				/* 杩炴帴鍒癎PIO */

void MQ135_GPIO_Config(void);
BitAction MQ135_Scan(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin);

#endif /* __BSP_GPIO_MQ135_H */
