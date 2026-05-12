/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "mq135/bsp_gpio_mq135.h"


void MQ135_GPIO_Config(void)
{
    
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 0    
    
    
    RCC_APB2PeriphClockCmd(MQ135_DO_GPIO_CLK_PORT,ENABLE);
    
    
    gpio_initstruct.GPIO_Pin    = MQ135_DO_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_IN_FLOATING;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(MQ135_DO_GPIO_PORT,&gpio_initstruct);
   
#endif 
    
#if 1    
    
    /* 开启MQ135相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(MQ135_AO_GPIO_CLK_PORT,ENABLE);

    
    gpio_initstruct.GPIO_Mode = GPIO_Mode_AIN;
    gpio_initstruct.GPIO_Pin = MQ135_AO_GPIO_PIN;
    gpio_initstruct.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_Init(MQ135_AO_GPIO_PORT,&gpio_initstruct);
    
#endif  
    
}


BitAction MQ135_Scan(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin)
{
    if(GPIO_ReadInputDataBit(GPIOx,GPIO_Pin) == Bit_RESET)
    {
        return Bit_RESET;
    }
    else
    {
        return Bit_SET;
    }
    
//    return GPIO_ReadInputDataBit(GPIOx,GPIO_Pin);
}

/*****************************END OF FILE***************************************/
