/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_DEBUG_H
#define __BSP_DEBUG_H

#include "stm32f10x.h"
#include <stdio.h>

#define DEBUG_USART_NUM 1

#if (DEBUG_USART_NUM == 1)

    #define DEBUG_TX_GPIO_PORT    			    GPIOA			                /* 瀵瑰簲GPIO绔彛 */
    #define DEBUG_TX_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOA			
    #define DEBUG_TX_GPIO_PIN			        GPIO_Pin_9	       				
    #define DEBUG_RX_GPIO_PORT    			    GPIOA			                /* 瀵瑰簲GPIO绔彛 */
    #define DEBUG_RX_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOA			
    #define DEBUG_RX_GPIO_PIN			        GPIO_Pin_10	       				

    #define DEBUG_USARTX   			            USART1                          
    #define DEBUG_USARTX_CLK_PORT 	            RCC_APB2Periph_USART1				
    #define DEBUG_APBXCLKCMD   			        RCC_APB2PeriphClockCmd	        /* 对应串口外设时钟 */
    #define DEBUG_BAUDRATE   			        115200                          
     
    #define DEBUG_IRQ                           USART1_IRQn                      
    #define DEBUG_IRQHANDLER                    USART1_IRQHandler                /* 对应串口中断处理函数 */
     
#endif

#define DEBUG_BUFFER_SIZE 1024 


typedef struct
{
    uint8_t    buffer[DEBUG_BUFFER_SIZE];
    uint32_t   len;
    uint32_t   read_flag;  
}DEBUG_DataTypeDef;

extern DEBUG_DataTypeDef debug_receive;

void DEBUG_NVIC_Config(void);
void DEBUG_USART_PinConfig(void);
void DEBUG_USART_ModeConfig(void);
void DEBUG_USART_Init(void);

#endif /* __BSP_DEBUG_H  */
