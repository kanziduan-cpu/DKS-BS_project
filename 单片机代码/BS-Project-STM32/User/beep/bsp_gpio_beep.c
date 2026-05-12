/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "beep/bsp_gpio_beep.h"


void BEEP_GPIO_Config(void)
{
    
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 1    
    
    
    RCC_APB2PeriphClockCmd(BEEP_GPIO_CLK_PORT,ENABLE);
    
    /* IO输出状态初始化控制 */
    GPIO_ResetBits(BEEP_GPIO_PORT,BEEP_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Pin    = BEEP_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(BEEP_GPIO_PORT,&gpio_initstruct);
   
#endif 
    
}


void BEEP_ON(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, BEEP_TriggerLevel beep_soundsstatus)
{
    if(beep_soundsstatus == BEEP_LOW_TRIGGER)
    {
        GPIO_ResetBits(GPIOx,GPIO_Pin);
    }
    else
    {
        GPIO_SetBits(GPIOx,GPIO_Pin);
    }
    
}


void BEEP_OFF(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, BEEP_TriggerLevel beep_soundsstatus)
{
    if(beep_soundsstatus == BEEP_LOW_TRIGGER)
    {
        GPIO_SetBits(GPIOx,GPIO_Pin);
    }
    else
    {
        GPIO_ResetBits(GPIOx,GPIO_Pin);
    }
}


void BEEP_TOGGLE(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin)
{
    GPIOx->ODR ^= GPIO_Pin;

}
/*****************************END OF FILE***************************************/
