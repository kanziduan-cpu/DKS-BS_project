/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "led/bsp_gpio_led.h"


void LED_GPIO_Config(void)
{
    
    GPIO_InitTypeDef gpio_initstruct = {0};
    
   
#if 1    
    
    
    RCC_APB2PeriphClockCmd(R_LED_GPIO_CLK_PORT,ENABLE);
    
    /* IO输出状态初始化控制 */
    GPIO_SetBits(R_LED_GPIO_PORT,R_LED_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Pin    = R_LED_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(R_LED_GPIO_PORT,&gpio_initstruct);
   
#endif 
    
#if 1    
    
    
    RCC_APB2PeriphClockCmd(G_LED_GPIO_CLK_PORT,ENABLE);
    
    /* IO输出状态初始化控制 */
    GPIO_SetBits(G_LED_GPIO_PORT,G_LED_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Pin    = G_LED_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(G_LED_GPIO_PORT,&gpio_initstruct);
   
#endif 

#if 1    
    
    
    RCC_APB2PeriphClockCmd(B_LED_GPIO_CLK_PORT,ENABLE);
    
    /* IO输出状态初始化控制 */
    GPIO_SetBits(B_LED_GPIO_PORT,B_LED_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Pin    = B_LED_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(B_LED_GPIO_PORT,&gpio_initstruct);
   
#endif 



#if 0    
    
    
    RCC_APB2PeriphClockCmd(LED4_GPIO_CLK_PORT,ENABLE);
    
    /* IO输出状态初始化控制 */
    GPIO_SetBits(LED4_GPIO_PORT,LED4_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Pin    = LED4_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(LED4_GPIO_PORT,&gpio_initstruct);
   
#endif 

#if 0    
    
    
    RCC_APB2PeriphClockCmd(LED5_GPIO_CLK_PORT,ENABLE);
    
    /* IO输出状态初始化控制 */
    GPIO_ResetBits(LED5_GPIO_PORT,LED5_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Pin    = LED5_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(LED5_GPIO_PORT,&gpio_initstruct);
   
#endif 

#if 0    
    
    
    RCC_APB2PeriphClockCmd(LED6_GPIO_CLK_PORT,ENABLE);
    
    /* IO输出状态初始化控制 */
    GPIO_ResetBits(LED6_GPIO_PORT,LED6_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Pin    = LED6_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(LED6_GPIO_PORT,&gpio_initstruct);
   
#endif 

}


void LED_ON(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, LED_TriggerLevel led_brightstatus)
{
    if(led_brightstatus == LED_LOW_TRIGGER)
    {
        GPIO_ResetBits(GPIOx,GPIO_Pin);
    }
    else
    {
        GPIO_SetBits(GPIOx,GPIO_Pin);
    }
    
}


void LED_OFF(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, LED_TriggerLevel led_brightstatus)
{
    if(led_brightstatus == LED_LOW_TRIGGER)
    {
        GPIO_SetBits(GPIOx,GPIO_Pin);
    }
    else
    {
        GPIO_ResetBits(GPIOx,GPIO_Pin);
    }
}


void LED_TOGGLE(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin)
{
    GPIOx->ODR ^= GPIO_Pin;

}
/*****************************END OF FILE***************************************/
