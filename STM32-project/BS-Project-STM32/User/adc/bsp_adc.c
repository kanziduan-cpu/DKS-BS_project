/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "adc/bsp_adc.h"
#include "mq135/bsp_gpio_mq135.h"
#include "rain/bsp_gpio_rain.h"


uint16_t adc_source_convertedvalue[20] = {0};//转化后的源始值

/**
    * @brief  配置 ADC 中断配置
    * @param  无
    * @retval 无
    */
void ADCX_NVIC_Config(void)
{
    /* 定义一个中断控制器结构体 */
    NVIC_InitTypeDef nvic_initstructure = {0};
#if 1
    // 配置中断优先级
    nvic_initstructure.NVIC_IRQChannel                      = ADCX_INT_DMA_IRQ;
    nvic_initstructure.NVIC_IRQChannelPreemptionPriority    = 1;
    nvic_initstructure.NVIC_IRQChannelSubPriority           = 0;
    nvic_initstructure.NVIC_IRQChannelCmd                   = ENABLE;
    NVIC_Init(&nvic_initstructure);

#endif    
    
}

/**
    * @brief  配置 ADC_DMA 模式
    * @param  无
    * @retval 无
    */
void ADCX_DMA_Config(void)
{
    /* 定义一个DMA结构体 */
    DMA_InitTypeDef dma_initstructure = {0};

    /*开启ADC_DMA相关的DMA外设/端口时钟*/
    RCC_AHBPeriphClockCmd(ADCX_DMA_CLK_PORT,ENABLE);

    /*复位DMA控制器*/
    DMA_DeInit(ADCX_DMA_CHANNEL);
    
    dma_initstructure.DMA_PeripheralBaseAddr = ADC1_DR_ADDRESS;                         //外设基地址
    dma_initstructure.DMA_MemoryBaseAddr = (uint32_t)&adc_source_convertedvalue;        //AD转换值所存放的内存基地址
    dma_initstructure.DMA_DIR = DMA_DIR_PeripheralSRC;                                  //外设作为数据传输的来源
    dma_initstructure.DMA_BufferSize = ADCX_CHANNEL_NUM;                                //定义指定DMA通道 DMA缓存的大小
    dma_initstructure.DMA_PeripheralInc = DMA_PeripheralInc_Disable;                    //外设地址寄存器不变
    dma_initstructure.DMA_MemoryInc = DMA_MemoryInc_Enable;                             //内存地址寄存器递增
    dma_initstructure.DMA_PeripheralDataSize = DMA_PeripheralDataSize_HalfWord;         //数据位宽16
    dma_initstructure.DMA_MemoryDataSize = DMA_MemoryDataSize_HalfWord;                 //数据位宽16
    dma_initstructure.DMA_Mode = DMA_Mode_Normal;                                       //工作模式
    dma_initstructure.DMA_Priority = DMA_Priority_High;                                 //高优先级
    dma_initstructure.DMA_M2M = DMA_M2M_Disable;                                        //禁止内存到内存
    DMA_Init(ADCX_DMA_CHANNEL,&dma_initstructure);                       //初始化ADC  
    
    DMA_ITConfig(ADCX_DMA_CHANNEL,DMA_IT_TC,ENABLE);                 //使能注入转换完成中断，用于读取转换值
}

/**
 * @brief  配置 ADCX 模式
 * @param  无
 * @retval 无
 */
void ADCX_Mode_Config(void)
{
  
    /* 定义一个 ADC 结构体 */
    ADC_InitTypeDef adc_initstruct = {0};
#if 1  
    /* 开启 ADC 相关的GPIO外设/端口时钟 */
    ADCX_APBXCLKCMD(ADCX_CLK_PORT,ENABLE);
    
    /* ADC 模式配置 */
    adc_initstruct.ADC_Mode                 = ADC_Mode_Independent;                     //只有一个ADC,属于独立模式
    adc_initstruct.ADC_ScanConvMode         = ENABLE;                                   //禁止扫描模式，单通道不需要
    adc_initstruct.ADC_ContinuousConvMode   = DISABLE;                                  //使能连续扫描模式
    adc_initstruct.ADC_ExternalTrigConv     = ADC_ExternalTrigConv_None;                //不需要外部触发转换,使用软件开启
    adc_initstruct.ADC_DataAlign            = ADC_DataAlign_Right;                      //转换结构右对齐
    adc_initstruct.ADC_NbrOfChannel         = ADCX_CHANNEL_NUM;
    
    ADC_Init(ADCX,&adc_initstruct); //初始化ADC  
    
#endif 
    
    /* ADC 的转化配置 */
    RCC_ADCCLKConfig(RCC_PCLK2_Div8);   //配置ADC的时钟为PLL2的8分频,9MHz
    
    ADC_RegularChannelConfig(ADCX,MQ135_ADC_CHANNEL,1,ADC_SampleTime_55Cycles5);   //配置 MQ135 通道采样顺序和时间
    ADC_RegularChannelConfig(ADCX,RAIN_ADC_CHANNEL,2,ADC_SampleTime_55Cycles5);    //配置雨量通道采样顺序和时间
    
    ADC_Cmd(ADCX,ENABLE);                             //开启ADC转换
    
    ADC_ResetCalibration(ADCX);                       //选择需要校准的ADC初始化
    while(ADC_GetResetCalibrationStatus(ADCX));       //等待校准初始化完成
    ADC_StartCalibration(ADCX);                       //开始校准
    while(ADC_GetCalibrationStatus(ADCX));            //等待校准完成
    
    ADC_SoftwareStartConvCmd(ADCX,ENABLE);            //由于没有采用外部触发,所以配置软件触发ADC转换
    
}

/** 
 * @brief  ADCX初始化
 * @param  无
 * @retval 无
 */
void ADCX_Init(void)
{
    /* 配置 ADCX 模式 */
    ADCX_Mode_Config();
    ADCX_DMA_Config();

    /* 对应的GPIO的配置 */
    MQ135_GPIO_Config();
    RAIN_GPIO_Config();
    
    /* 配置对应的中断 */
    ADCX_NVIC_Config();
    
}
/*****************************END OF FILE***************************************/
