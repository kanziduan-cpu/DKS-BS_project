#ifndef __BSP_ADC_H
#define __BSP_ADC_H

#include "stm32f10x.h"

// MQ135_ADC_DATA
#define ADCX   			                    ADC1                                    /* 外设序号 */
#define ADCX_CLK_PORT 	                    RCC_APB2Periph_ADC1				        /* 外设时钟 */
#define ADCX_APBXCLKCMD   			        RCC_APB2PeriphClockCmd

#define MQ135_ADC_CHANNEL			        ADC_Channel_4	                        /* 采样通道线 */
#define ADCX_CHANNEL_NUM			        1	                                    /* 采样通道数目 */

#define ADCX_DMA_CLK_PORT 	                RCC_AHBPeriph_DMA1				        /* 外设时钟 */
#define ADCX_DMA_CHANNEL	                DMA1_Channel1				            /* 中断线 */

#define ADCX_INT_DMA_IRQ			        DMA1_Channel1_IRQn                      /* 外部中断向量号 */
#define ADCX_INT_DMA_IRQHANDLER			    DMA1_Channel1_IRQHandler                /* 中断处理函数*/

extern  uint16_t adc_source_convertedvalue[20];//转化后的源始值

//ADC1_BASE地址：0x4000 0000+0x2400 即ADC1的基地址，而他的规则数据寄存器的偏移地址是：0x4c
//#define ADC1_DR_ADDRESS    (uint32_t)DR_ADDRESS
#define ADC1_DR_ADDRESS    ((uint32_t)ADC1_BASE+0x4c)

void ADCX_NVIC_Config(void);
void ADCX_DMA_Config(void);
void ADCX_Mode_Config(void);
void ADCX_Init(void);

#endif /* __BSP_ADC_H  */
