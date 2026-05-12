/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_GPIO_LED_H
#define __BSP_GPIO_LED_H

#include "stm32f10x.h"

/* 定义 LED 连接的GPIO端口, 用户只需要修改下面的代码即可改变控制的LED引脚 */

//LED1
#define LED1_GPIO_PORT          GPIOA                           /* GPIO绔彛 */
#define LED1_GPIO_CLK_PORT      RCC_APB2Periph_GPIOA            /* GPIO绔彛鏃堕挓 */
#define LED1_GPIO_PIN           GPIO_Pin_1                      

//LED2
#define LED2_GPIO_PORT          GPIOA                           /* GPIO绔彛 */
#define LED2_GPIO_CLK_PORT      RCC_APB2Periph_GPIOA            /* GPIO绔彛鏃堕挓 */
#define LED2_GPIO_PIN           GPIO_Pin_2                      

//LED3
#define LED3_GPIO_PORT          GPIOA                           /* GPIO绔彛 */
#define LED3_GPIO_CLK_PORT      RCC_APB2Periph_GPIOA            /* GPIO绔彛鏃堕挓 */
#define LED3_GPIO_PIN           GPIO_Pin_3                      

//LED4
#define LED4_GPIO_PORT          GPIOB                           /* GPIO绔彛 */
#define LED4_GPIO_CLK_PORT      RCC_APB2Periph_GPIOB            /* GPIO绔彛鏃堕挓 */
#define LED4_GPIO_PIN           GPIO_Pin_5                      

//LED5
#define LED5_GPIO_PORT          GPIOB                           /* GPIO绔彛 */
#define LED5_GPIO_CLK_PORT      RCC_APB2Periph_GPIOB            /* GPIO绔彛鏃堕挓 */
#define LED5_GPIO_PIN           GPIO_Pin_13                      

//LED6
#define LED6_GPIO_PORT          GPIOB                           /* GPIO绔彛 */
#define LED6_GPIO_CLK_PORT      RCC_APB2Periph_GPIOB            /* GPIO绔彛鏃堕挓 */
#define LED6_GPIO_PIN           GPIO_Pin_14                      

   
// R_LED
#define R_LED_GPIO_PORT          LED1_GPIO_PORT                /* GPIO绔彛 */
#define R_LED_GPIO_CLK_PORT      LED1_GPIO_CLK_PORT            /* GPIO绔彛鏃堕挓 */
#define R_LED_GPIO_PIN           LED1_GPIO_PIN                 

// G_LED  
#define G_LED_GPIO_PORT          LED2_GPIO_PORT                /* GPIO绔彛 */
#define G_LED_GPIO_CLK_PORT      LED2_GPIO_CLK_PORT            /* GPIO绔彛鏃堕挓 */
#define G_LED_GPIO_PIN           LED2_GPIO_PIN                 

// B_LED  
#define B_LED_GPIO_PORT          LED3_GPIO_PORT                /* GPIO绔彛 */
#define B_LED_GPIO_CLK_PORT      LED3_GPIO_CLK_PORT            /* GPIO绔彛鏃堕挓 */
#define B_LED_GPIO_PIN           LED3_GPIO_PIN                 

/************************************鐢ㄦ埛鑷畾涔夊簲鐢ㄥ畯*****************************************************/

// R_LED
#define R_LED_ON_ONLY    LED_ON(R_LED_GPIO_PORT,R_LED_GPIO_PIN,LED_LOW_TRIGGER);    \
                        LED_OFF(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);    \
                        LED_OFF(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);
// G_LED                     
#define G_LED_ON_ONLY   LED_OFF(R_LED_GPIO_PORT,R_LED_GPIO_PIN,LED_LOW_TRIGGER);    \
                         LED_ON(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);    \
                        LED_OFF(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);
// B_LED
#define B_LED_ON_ONLY   LED_OFF(R_LED_GPIO_PORT,R_LED_GPIO_PIN,LED_LOW_TRIGGER);    \
                        LED_OFF(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);    \
                         LED_ON(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);
                         
// R_G_B_LED 鍏ㄤ寒
#define RGB_ALL_ON      LED_ON(R_LED_GPIO_PORT,R_LED_GPIO_PIN,LED_LOW_TRIGGER);    \
                        LED_ON(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);    \
                        LED_ON(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);
                         
// R_G_B_LED 鍏ㄧ伃
#define RGB_ALL_OFF     LED_OFF(R_LED_GPIO_PORT,R_LED_GPIO_PIN,LED_LOW_TRIGGER);   \
                        LED_OFF(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);   \
                        LED_OFF(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);

/* LED鐏寒鏃剁殑IO鐢靛钩 */
typedef enum 
{
    LED_LOW_TRIGGER = 0, 
    LED_HIGH_TRIGGER = 1,
}LED_TriggerLevel;

void LED_GPIO_Config(void);
void LED_ON(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, LED_TriggerLevel led_brightstatus);
void LED_OFF(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, LED_TriggerLevel led_brightstatus);
void LED_TOGGLE(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin);
#endif /* __BSP_GPIO_LED_H */
