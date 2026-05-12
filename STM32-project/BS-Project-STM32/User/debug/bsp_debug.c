/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "debug/bsp_debug.h"
#include "usart/usart_com.h"

DEBUG_DataTypeDef debug_receive = {0};


void DEBUG_NVIC_Config(void)
{
    
    NVIC_InitTypeDef nvic_initstruct = {0};
    
    
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO,ENABLE); 
      
    
    nvic_initstruct.NVIC_IRQChannel                     = DEBUG_IRQ;
    
    nvic_initstruct.NVIC_IRQChannelPreemptionPriority   =  1;
    /* 閰嶇疆瀛愪紭鍏堢骇 */
    nvic_initstruct.NVIC_IRQChannelSubPriority          =  0;
    /* 浣胯兘閰嶇疆涓柇閫氶亾 */
    nvic_initstruct.NVIC_IRQChannelCmd                  =  ENABLE;

    NVIC_Init(&nvic_initstruct);
    
}


void DEBUG_USART_PinConfig(void)
{
    
    
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 1  
    
    
    RCC_APB2PeriphClockCmd(DEBUG_TX_GPIO_CLK_PORT,ENABLE);

    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_PP;
    gpio_initstruct.GPIO_Pin    = DEBUG_TX_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(DEBUG_TX_GPIO_PORT,&gpio_initstruct); 
    
#endif 
    
#if 1    
    
    
    RCC_APB2PeriphClockCmd(DEBUG_RX_GPIO_CLK_PORT,ENABLE);

    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_IPU;
    gpio_initstruct.GPIO_Pin    = DEBUG_RX_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(DEBUG_RX_GPIO_PORT,&gpio_initstruct); 
    
#endif  
}


void DEBUG_USART_ModeConfig(void)
{
  
    
    USART_InitTypeDef usart_initstruct = {0};
    
    
    DEBUG_APBXCLKCMD(DEBUG_USARTX_CLK_PORT,ENABLE);

    
    usart_initstruct.USART_BaudRate                 =  DEBUG_BAUDRATE;                  
    usart_initstruct.USART_HardwareFlowControl      =  USART_HardwareFlowControl_None;  
    usart_initstruct.USART_Mode                     =  USART_Mode_Tx|USART_Mode_Rx;     //閰嶇疆宸ヤ綔妯″紡
    usart_initstruct.USART_Parity                   =  USART_Parity_No;                 
    usart_initstruct.USART_StopBits                 =  USART_StopBits_1;                
    usart_initstruct.USART_WordLength               =  USART_WordLength_8b;             
    
    USART_Init(DEBUG_USARTX, &usart_initstruct);
    
    USART_ITConfig(DEBUG_USARTX,USART_IT_RXNE,ENABLE);
    USART_ITConfig(DEBUG_USARTX,USART_IT_IDLE,ENABLE);
    
}


void DEBUG_USART_Init(void)
{
    /* 閰嶇疆 DEBUG 涓插彛涓柇閰嶇疆 */
    DEBUG_NVIC_Config();
    
    /* 閰嶇疆 USARTX 妯″紡 */
    DEBUG_USART_ModeConfig();

    
    DEBUG_USART_PinConfig();
    
    /* 浣胯兘涓插彛 */
    USART_Cmd(DEBUG_USARTX,ENABLE);

}




void DEBUG_IRQHANDLER(void)
{
    uint8_t data_temp = NULL;
    if(USART_GetITStatus(DEBUG_USARTX, USART_IT_RXNE) == SET)  
    {
        data_temp = USART_ReceiveData(DEBUG_USARTX);                                    //读取数据寄存器的数据，读取后对应的寄存器会被复位
        
        if((debug_receive.len < DEBUG_BUFFER_SIZE-1) && debug_receive.read_flag == 0)   //未接收满且程序不正在读取缓冲区，才把数据添加进缓冲区
        {
            debug_receive.buffer[debug_receive.len] = data_temp;
            debug_receive.len++;
        }
        if(debug_receive.len == DEBUG_BUFFER_SIZE-1)                    
        {
            debug_receive.buffer[debug_receive.len] = '\0';             
            debug_receive.read_flag = 1;                                
        }
        USART_ClearITPendingBit(DEBUG_USARTX,USART_IT_RXNE);            
    }
    if(USART_GetITStatus(DEBUG_USARTX, USART_IT_IDLE) == SET)  
    {
        USART_ReceiveData(DEBUG_USARTX);                                //鐗瑰埆鎻愮ず锛屾牴鎹墜鍐屽疄闄呮弿杩帮紙璇诲彇鑾峰彇USART_IT_IDLE銆佹暟鎹瘎瀛樺櫒鐨勬暟鎹紝璇诲彇鍚庡搴旂殑瀵勫瓨鍣ㄤ細琚浣嶏級锛岀敤杩欐牱鏂瑰紡娓呴櫎IDLE
        debug_receive.buffer[debug_receive.len] = '\0';                 
        debug_receive.read_flag = 1;                                    
    }
    
}

/*****************************END OF FILE***************************************/

