/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "usart/usart_com.h"
#include "debug/bsp_debug.h"


void USARTX_SendByte(USART_TypeDef *pusartx, uint8_t ch)
{
    
    while (USART_GetFlagStatus(pusartx, USART_FLAG_TC) == RESET);
    
    /* 发送一个字节数据到 pusartx */
    USART_SendData(pusartx,ch);
    
    /* 等待发送数据寄存器为空 */
    while (USART_GetFlagStatus(pusartx, USART_FLAG_TXE) == RESET);

}


void USARTX_SendArray(USART_TypeDef *pusartx, uint8_t *array, uint32_t num)
{
    
    while (USART_GetFlagStatus(pusartx, USART_FLAG_TC) == RESET);
    
    /* 发送多个字节数据到 pusartx */
    for (uint32_t i = 0; i < num; i++)
    {
        /* 发送一个字节数据到 pusartx */
        USART_SendData(pusartx,array[i]);
        
        /* 等待发送数据寄存器为空 */
        while (USART_GetFlagStatus(pusartx, USART_FLAG_TXE) == RESET);
    }

}


void USARTX_SendString(USART_TypeDef *pusartx, char *str)
{
    uint32_t k = 0;
    
    
    while (USART_GetFlagStatus(pusartx, USART_FLAG_TC) == RESET);
    
    do
    {
        /* 发送一个字节数据到 pusartx */
        USART_SendData(pusartx,*(str + k));
        k++;
        
        /* 等待发送数据寄存器为空 */
        while (USART_GetFlagStatus(pusartx, USART_FLAG_TXE) == RESET);
        
    } while (*(str + k) != '\0');

}


int fputc(int ch, FILE *f)
{
    
    while (USART_GetFlagStatus(DEBUG_USARTX, USART_FLAG_TC) == RESET);
    
    /* 发送一个字节数据到串口 */
    USART_SendData(DEBUG_USARTX, (uint8_t)ch);
    
    /* 等待发送数据寄存器为空 */
    while (USART_GetFlagStatus(DEBUG_USARTX, USART_FLAG_TXE) == RESET);

    return (ch);
}

/*****************************END OF FILE***************************************/

