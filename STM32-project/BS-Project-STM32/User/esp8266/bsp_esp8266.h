/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_ESP8266_H
#define __BSP_ESP8266_H

#include "stm32f10x.h"
#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#define ESP8266_USART_NUM 3

#if (ESP8266_USART_NUM == 1)

    #define ESP8266_TX_GPIO_PORT    			        GPIOA			                /* 瀵瑰簲GPIO绔彛 */
    #define ESP8266_TX_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOA			
    #define ESP8266_TX_GPIO_PIN			            GPIO_Pin_9	       				
    #define ESP8266_RX_GPIO_PORT    			        GPIOA			                /* 瀵瑰簲GPIO绔彛 */
    #define ESP8266_RX_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOA			
    #define ESP8266_RX_GPIO_PIN			            GPIO_Pin_10	       				

    #define ESP8266_USARTX   			            USART1                          
    #define ESP8266_USARTX_CLK_PORT 	                RCC_APB2Periph_USART1				
    #define ESP8266_APBXCLKCMD   			        RCC_APB2PeriphClockCmd	        /* 对应串口外设时钟 */
    #define ESP8266_BAUDRATE   			            115200                          
     
    #define ESP8266_IRQ                              USART1_IRQn                      
    #define ESP8266_IRQHANDLER                       USART1_IRQHandler                /* 对应串口中断处理函数 */
    
#elif (ESP8266_USART_NUM == 2)

    #define ESP8266_TX_GPIO_PORT    			        GPIOA			                /* 瀵瑰簲GPIO绔彛 */
    #define ESP8266_TX_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOA			
    #define ESP8266_TX_GPIO_PIN			            GPIO_Pin_2	       				
    #define ESP8266_RX_GPIO_PORT    			        GPIOA			                /* 瀵瑰簲GPIO绔彛 */
    #define ESP8266_RX_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOA			
    #define ESP8266_RX_GPIO_PIN			            GPIO_Pin_3	       				
                                                                                
    #define ESP8266_USARTX   			            USART2                          
    #define ESP8266_USARTX_CLK_PORT 	                RCC_APB1Periph_USART2			
    #define ESP8266_APBXCLKCMD   			        RCC_APB1PeriphClockCmd	        /* 对应串口外设时钟 */
    #define ESP8266_BAUDRATE   			            115200                          
    
    #define ESP8266_IRQ                              USART2_IRQn                      
    #define ESP8266_IRQHANDLER                       USART2_IRQHandler                /* 对应串口中断处理函数 */
    
#elif (ESP8266_USART_NUM == 3)

    #define ESP8266_TX_GPIO_PORT    			        GPIOB			                /* 瀵瑰簲GPIO绔彛 */
    #define ESP8266_TX_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOB			
    #define ESP8266_TX_GPIO_PIN			            GPIO_Pin_10	       				
    #define ESP8266_RX_GPIO_PORT    			        GPIOB			                /* 瀵瑰簲GPIO绔彛 */
    #define ESP8266_RX_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOB			
    #define ESP8266_RX_GPIO_PIN			            GPIO_Pin_11	       				
                                                                                
    #define ESP8266_USARTX   			            USART3                          
    #define ESP8266_USARTX_CLK_PORT 	                RCC_APB1Periph_USART3			
    #define ESP8266_APBXCLKCMD   			        RCC_APB1PeriphClockCmd	        /* 对应串口外设时钟 */
    #define ESP8266_BAUDRATE   			            115200		                    
    
    #define ESP8266_IRQ                              USART3_IRQn                      
    #define ESP8266_IRQHANDLER                       USART3_IRQHandler                /* 对应串口中断处理函数 */
    
#endif

#define ESP8266_RST_GPIO_PORT    			    GPIOB			                /* 瀵瑰簲GPIO绔彛 */
#define ESP8266_RST_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOB			
#define ESP8266_RST_GPIO_PIN			        GPIO_Pin_9	       				
#define ESP8266_IO_GPIO_PORT    			    GPIOB			                /* 瀵瑰簲GPIO绔彛 */
#define ESP8266_IO_GPIO_CLK_PORT 	            RCC_APB2Periph_GPIOB			
#define ESP8266_IO_GPIO_PIN			            GPIO_Pin_8	       				

#define ESP8266_BUFFER_SIZE 1024 

/******************************* ESP8266 数据类型定义 ***************************/
typedef enum{
	STA,
  AP,
  STA_AP  
} ENUM_Net_ModeTypeDef;


typedef enum{
	 enumTCP,
	 enumUDP,
} ENUM_NetPro_TypeDef;
	

typedef enum{
	Multiple_ID_0 = 0,
	Multiple_ID_1 = 1,
	Multiple_ID_2 = 2,
	Multiple_ID_3 = 3,
	Multiple_ID_4 = 4,
	Single_ID_0 = 5,
} ENUM_ID_NO_TypeDef;
	

typedef enum{
	OPEN = 0,
	WEP = 1,
	WPA_PSK = 2,
	WPA2_PSK = 3,
	WPA_WPA2_PSK = 4,
} ENUM_AP_PsdMode_TypeDef;


typedef struct
{
    uint8_t    buffer[ESP8266_BUFFER_SIZE];
    uint32_t   len;
    uint32_t   read_flag;  
    uint8_t    Received_data_completed_flag;//接收数据完毕标志
}ESP8266_DataTypeDef;

extern ESP8266_DataTypeDef esp8266_receive;
uint8_t                  ESP8266_ReadAsyncMessage          ( uint8_t *buffer, uint32_t buffer_size );
void                     ESP8266_PreserveAsyncMessage      ( void );

void ESP8266_GPIO_PinConfig(void);
void ESP8266_NVIC_Config(void);
void ESP8266_USART_PinConfig(void);
void ESP8266_USART_ModeConfig(void);
void ESP8266_USART_Init(void);
void                     ESP8266_Init                        ( void );
void                     ESP8266_Rst                         ( void );
bool                     ESP8266_AT_Test                     ( void );
bool                     ESP8266_Cmd                         ( char * cmd, char * reply1, char * reply2, u32 waittime );
bool                     ESP8266_AT_Test                     ( void );
bool                     ESP8266_DHCP_CUR                    ( void );
bool                     ESP8266_Net_Mode_Choose             ( ENUM_Net_ModeTypeDef enumMode );
bool                     ESP8266_JoinAP                      ( char * pSSID, char * pPassWord );
bool                     ESP8266_BuildAP                     ( char * pSSID, char * pPassWord, ENUM_AP_PsdMode_TypeDef enunPsdMode );
bool                     ESP8266_Enable_MultipleId           ( FunctionalState enumEnUnvarnishTx );
bool                     ESP8266_Link_Server                 ( ENUM_NetPro_TypeDef enumE, char * ip, char * ComNum, ENUM_ID_NO_TypeDef id);
bool                     ESP8266_StartOrShutServer           ( FunctionalState enumMode, char * pPortNum, char * pTimeOver );
uint8_t                  ESP8266_Get_LinkStatus              ( void );
uint8_t                  ESP8266_Get_IdLinkStatus            ( void );
uint8_t                  ESP8266_Inquire_ApIp                ( char * pApIp, uint8_t ucArrayLength );
bool                     ESP8266_UnvarnishSend               ( void );
void                     ESP8266_ExitUnvarnishSend           ( void );
bool                     ESP8266_SendString                  ( FunctionalState enumEnUnvarnishTx, char * pStr, u32 ulStrLength, ENUM_ID_NO_TypeDef ucId );
bool                     ESP8266_SendBuffer                  ( const uint8_t *pBuffer, u32 ulLength, u32 waittime );
char *                   ESP8266_ReceiveString               ( FunctionalState enumEnUnvarnishTx );


#endif /* __BSP_ESP8266_H  */
