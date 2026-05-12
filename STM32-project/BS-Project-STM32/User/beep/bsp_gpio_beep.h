/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_GPIO_BEEP_H
#define __BSP_GPIO_BEEP_H

#include "stm32f10x.h"



//BEEP
#define BEEP_GPIO_PORT          GPIOA                           /* GPIO绔彛 */
#define BEEP_GPIO_CLK_PORT      RCC_APB2Periph_GPIOA            /* GPIO绔彛鏃堕挓 */
#define BEEP_GPIO_PIN           GPIO_Pin_6                      


/* 铚傞福鍣ㄥ搷鏃剁殑IO鐢靛钩 */
typedef enum 
{
    BEEP_LOW_TRIGGER = 0, 
    BEEP_HIGH_TRIGGER = 1,
}BEEP_TriggerLevel;

void BEEP_GPIO_Config(void);
void BEEP_ON(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, BEEP_TriggerLevel beep_soundsstatus);
void BEEP_OFF(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, BEEP_TriggerLevel beep_soundsstatus);
void BEEP_TOGGLE(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin);
#endif /* __BSP_GPIO_BEEP_H */
