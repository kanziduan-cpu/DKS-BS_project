/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_GPIO_KEY_H
#define __BSP_GPIO_KEY_H

#include "stm32f10x.h"



//KEY1
#define KEY1_GPIO_PORT          GPIOA                           /* GPIO绔彛 */
#define KEY1_GPIO_CLK_PORT      RCC_APB2Periph_GPIOA            /* GPIO绔彛鏃堕挓 */
#define KEY1_GPIO_PIN           GPIO_Pin_0                      


#define KEY1_EXTI_PORTSOURCE    GPIO_PortSourceGPIOA            
#define KEY1_EXTI_PINSOURCE     GPIO_PinSource0                 
#define KEY1_EXTI_LINE          EXTI_Line0                      
#define KEY1_EXTI_IRQ           EXTI0_IRQn                      
#define KEY1_EXTI_IRQHANDLER    EXTI0_IRQHandler                /* 中断处理函数 */

//KEY2
#define KEY2_GPIO_PORT          GPIOC                           /* GPIO绔彛 */
#define KEY2_GPIO_CLK_PORT      RCC_APB2Periph_GPIOC            /* GPIO绔彛鏃堕挓 */
#define KEY2_GPIO_PIN           GPIO_Pin_13                     

#define KEY2_EXTI_PORTSOURCE    GPIO_PortSourceGPIOC            
#define KEY2_EXTI_PINSOURCE     GPIO_PinSource13                
#define KEY2_EXTI_LINE          EXTI_Line13                     
#define KEY2_EXTI_IRQ           EXTI15_10_IRQn                  
#define KEY2_EXTI_IRQHANDLER    EXTI15_10_IRQHandler            /* 中断处理函数 */


//KEY3
#define KEY3_GPIO_PORT          GPIOB                           /* GPIO绔彛 */
#define KEY3_GPIO_CLK_PORT      RCC_APB2Periph_GPIOB            /* GPIO绔彛鏃堕挓 */
#define KEY3_GPIO_PIN           GPIO_Pin_15                     

//KEY4
#define KEY4_GPIO_PORT          GPIOB                           /* GPIO绔彛 */
#define KEY4_GPIO_CLK_PORT      RCC_APB2Periph_GPIOB            /* GPIO绔彛鏃堕挓 */
#define KEY4_GPIO_PIN           GPIO_Pin_6                      

//KEY5
#define KEY5_GPIO_PORT          GPIOB                           /* GPIO绔彛 */
#define KEY5_GPIO_CLK_PORT      RCC_APB2Periph_GPIOB            /* GPIO绔彛鏃堕挓 */
#define KEY5_GPIO_PIN           GPIO_Pin_7                      

//KEY6
#define KEY6_GPIO_PORT          GPIOB                           /* GPIO绔彛 */
#define KEY6_GPIO_CLK_PORT      RCC_APB2Periph_GPIOB            /* GPIO绔彛鏃堕挓 */
#define KEY6_GPIO_PIN           GPIO_Pin_8                      


/* 鎸夐敭鎸変笅鏃剁殑IO鐢靛钩 */
typedef enum 
{
    KEY_LOW_TRIGGER = 0, 
    KEY_HIGH_TRIGGER = 1,
    KEY_GENERAL_TRIGGER = 2,
}KEY_TriggerLevel;


typedef enum 
{
    KEY_UP = 0, 
    KEY_DOWN = 1,
    KEY_INIT = 2,
}KEY_Status;


typedef enum 
{
    EVENT_ATTONITY = 0, 
    EVENT_PRESS = 1,
    EVENT_SHORT,
    EVENT_SHORT_RELEASE,
    EVENT_LONG,
    EVENT_LONG_RELEASE,
}KEY_Event;


typedef enum 
{
    KEY_NONE_CLICK = 0, 
    KEY_SINGLE_CLICK = 1,
    KEY_DOUBLE_CLICK,
    KEY_LONG_CLICK,
}KEY_ClickType;

/* 按键的结构体 */
typedef struct
{
    GPIO_TypeDef*       GPIOx;
    uint16_t            GPIO_Pin;
    KEY_TriggerLevel    triggerlevel;
    KEY_Status          status;
    uint64_t            press_time;
    uint64_t            release_time;
    KEY_Event           event;  
    KEY_ClickType       clicktype;
}KEY_Info;

extern KEY_Info key1_info;
extern KEY_Info key2_info;

void KEY_NVIC_Config(void);
void KEY_GPIO_Config(void);
void KEY_Init(void);
KEY_Status KEY_Scan(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin, KEY_TriggerLevel key_pressstatus);
void KeyLevel_Init(KEY_Info* key_info);
KEY_Event KEY_SystickScan(KEY_Info* key_info);
#endif /* __BSP_GPIO_KEY_H */
