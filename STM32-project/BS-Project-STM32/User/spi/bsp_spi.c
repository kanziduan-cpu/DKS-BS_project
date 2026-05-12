/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "spi/bsp_spi.h"
#include "dwt/bsp_dwt.h"  


void SPI_PinConfig(void)
{
    
    GPIO_InitTypeDef gpio_initstruct = {0};
   
#if 1    
    
    
    RCC_APB2PeriphClockCmd(SPI_CS_GPIO_CLK_PORT,ENABLE);
    
    GPIO_SetBits(SPI_CS_GPIO_PORT,SPI_CS_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Pin    = SPI_CS_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(SPI_CS_GPIO_PORT,&gpio_initstruct); 

#endif    
    
#if 1 
    
    
    RCC_APB2PeriphClockCmd(SPI_SCK_GPIO_CLK_PORT,ENABLE);
    
    GPIO_SetBits(SPI_SCK_GPIO_PORT,SPI_SCK_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_PP;
    gpio_initstruct.GPIO_Pin    = SPI_SCK_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(SPI_SCK_GPIO_PORT,&gpio_initstruct); 
    
#endif 

#if 1 

    
    RCC_APB2PeriphClockCmd(SPI_MISO_GPIO_CLK_PORT,ENABLE);
    
    GPIO_SetBits(SPI_MISO_GPIO_PORT,SPI_MISO_GPIO_PIN);
        
    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_IPU;
    gpio_initstruct.GPIO_Pin    = SPI_MISO_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(SPI_MISO_GPIO_PORT,&gpio_initstruct); 
    
#endif 

#if 1 

    
    RCC_APB2PeriphClockCmd(SPI_MOSI_GPIO_CLK_PORT,ENABLE);
    
    GPIO_SetBits(SPI_MOSI_GPIO_PORT,SPI_MOSI_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_PP;
    gpio_initstruct.GPIO_Pin    = SPI_MOSI_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(SPI_MOSI_GPIO_PORT,&gpio_initstruct); 
    
#endif     
}


void SPI_Mode_Config(void)
{
    
    SPI_InitTypeDef spi_initstruct = {0};
    
    
    SPI_APBXCLKCMD(SPI_SPIX_CLK_PORT,ENABLE);
    
    spi_initstruct.SPI_BaudRatePrescaler    = SPI_BaudRatePrescaler_4;          //娉㈢壒鐜囧垎棰戯紝閫夋嫨4鍒嗛
    spi_initstruct.SPI_CPOL                 = SPI_CPOL_High;				    
	spi_initstruct.SPI_CPHA                 = SPI_CPHA_2Edge;			        //SPI相位，选择第一个时钟边沿采样，极性和相位决定选择SPI模式0
	spi_initstruct.SPI_CRCPolynomial        = 7;				                
    spi_initstruct.SPI_DataSize             = SPI_DataSize_8b;		            
    spi_initstruct.SPI_Direction            = SPI_Direction_2Lines_FullDuplex;	//方向，选择2线全双工
    spi_initstruct.SPI_FirstBit             = SPI_FirstBit_MSB;		            //鍏堣浣嶏紝閫夋嫨楂樹綅鍏堣
    spi_initstruct.SPI_Mode                 = SPI_Mode_Master;			        
    spi_initstruct.SPI_NSS                  = SPI_NSS_Soft;				        
    
    SPI_Init(SPI_SPIX,&spi_initstruct);//将结构体变量交给SPI_Init，配置SPI_SPIX
}


void BSP_SPI_Init(void)
{
    
    SPI_PinConfig();
    
    
    SPI_Mode_Config();
    
    /* 浣胯兘 HARD_SPI */
    SPI_Cmd(SPI_SPIX,ENABLE);
}


uint8_t SPI_TransferData(uint8_t data)
{
	while (SPI_I2S_GetFlagStatus(SPI_SPIX, SPI_I2S_FLAG_TXE) == RESET);	
	
	SPI_I2S_SendData(SPI_SPIX, data);
    
    while(SPI_I2S_GetFlagStatus(SPI_SPIX,SPI_I2S_FLAG_TXE) == RESET);
	
	while (SPI_I2S_GetFlagStatus(SPI_SPIX, SPI_I2S_FLAG_RXNE) == RESET);
	
	return SPI_I2S_ReceiveData(SPI_SPIX);
}

/*********************************************END OF FILE**********************/
