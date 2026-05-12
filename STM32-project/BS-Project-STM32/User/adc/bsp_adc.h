/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_ADC_H
#define __BSP_ADC_H

#include "stm32f10x.h"

// MQ135 + RAIN ADC_DATA
#define ADCX   			                    ADC1                                    /* 外设序号 */
#define ADCX_CLK_PORT 	                    RCC_APB2Periph_ADC1				        /* 外设时钟 */
#define ADCX_APBXCLKCMD   			        RCC_APB2PeriphClockCmd

#define MQ135_ADC_CHANNEL			        ADC_Channel_4	                        /* MQ135 AO: PA4 */
#define RAIN_ADC_CHANNEL			        ADC_Channel_5	                        /* RAIN AO: PA5 */
#define ADCX_CHANNEL_NUM			        2	                                    /* 閲囨牱閫氶亾鏁扮洰 */

#define ADCX_MQ135_BUFFER_INDEX          0
#define ADCX_RAIN_BUFFER_INDEX           1

#define ADCX_DMA_CLK_PORT 	                RCC_AHBPeriph_DMA1				        /* 外设时钟 */
#define ADCX_DMA_CHANNEL	                DMA1_Channel1				            

#define ADCX_INT_DMA_IRQ			        DMA1_Channel1_IRQn                      
#define ADCX_INT_DMA_IRQHANDLER			    DMA1_Channel1_IRQHandler                /* 中断处理函数*/

extern  uint16_t adc_source_convertedvalue[20];


//#define ADC1_DR_ADDRESS    (uint32_t)DR_ADDRESS
#define ADC1_DR_ADDRESS    ((uint32_t)ADC1_BASE+0x4c)

void ADCX_NVIC_Config(void);
void ADCX_DMA_Config(void);
void ADCX_Mode_Config(void);
void ADCX_Init(void);

#endif /* __BSP_ADC_H  */
