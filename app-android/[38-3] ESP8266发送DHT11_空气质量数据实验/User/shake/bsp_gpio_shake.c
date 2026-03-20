/**
  ******************************************************************************
  * @file       bsp_gpio_shake.c
  * @author     embedfire
  * @version     V1.0
  * @date        2025
  * @brief      震动传感器驱动函数
  ******************************************************************************
  * @attention
  *
  * 实验平台  ：野火 STM32F103C8T6-STM32开发板 
  * 论坛      ：http://www.firebbs.cn
  * 官网      ：https://embedfire.com/
  * 淘宝      ：https://yehuosm.tmall.com/
  *
  ******************************************************************************
  */

#include "bsp_gpio_shake.h"

/* 全局变量定义 */
volatile uint8_t shake_gpio_detected_flag = 0;
volatile uint32_t shake_gpio_count = 0;

/**
  * @brief  震动传感器GPIO配置
  * @param  无
  * @retval 无
  */
void SHAKE_GPIO_Config(void)
{
    GPIO_InitTypeDef GPIO_InitStructure;
    
    /* 开启GPIO端口时钟 */
    RCC_APB2PeriphClockCmd(SHAKE_GPIO_CLK_PORT, ENABLE);
    
    /* 配置震动传感器引脚为输入，下拉模式 */
    GPIO_InitStructure.GPIO_Pin = SHAKE_GPIO_PIN;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IPD; /* 下拉输入 */
    GPIO_Init(SHAKE_GPIO_PORT, &GPIO_InitStructure);
}

/**
  * @brief  震动传感器外部中断配置
  * @param  无
  * @retval 无
  */
void SHAKE_EXTI_Config(void)
{
    EXTI_InitTypeDef EXTI_InitStructure;
    NVIC_InitTypeDef NVIC_InitStructure;
    
    /* 开启AFIO时钟 */
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO, ENABLE);
    
    /* 连接EXTI线到GPIO引脚 */
    GPIO_EXTILineConfig(SHAKE_EXTI_PORTSOURCE, SHAKE_EXTI_PINSOURCE);
    
    /* 配置EXTI线 */
    EXTI_InitStructure.EXTI_Line = SHAKE_EXTI_LINE;
    EXTI_InitStructure.EXTI_Mode = EXTI_Mode_Interrupt;
    EXTI_InitStructure.EXTI_Trigger = EXTI_Trigger_Rising; /* 上升沿触发 */
    EXTI_InitStructure.EXTI_LineCmd = ENABLE;
    EXTI_Init(&EXTI_InitStructure);
    
    /* 配置NVIC */
    NVIC_InitStructure.NVIC_IRQChannel = SHAKE_EXTI_IRQ;
    NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 2; /* 优先级 */
    NVIC_InitStructure.NVIC_IRQChannelSubPriority = 0;
    NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;
    NVIC_Init(&NVIC_InitStructure);
}

/**
  * @brief  震动传感器初始化
  * @param  无
  * @retval 无
  */
void SHAKE_Init(void)
{
    SHAKE_GPIO_Config();
    SHAKE_EXTI_Config();
    shake_gpio_detected_flag = 0;
    shake_gpio_count = 0;
}

/**
  * @brief  读取震动传感器状态
  * @param  无
  * @retval 震动传感器状态 (SHAKE_NORMAL或SHAKE_DETECTED)
  */
uint8_t SHAKE_ReadStatus(void)
{
    return (GPIO_ReadInputDataBit(SHAKE_GPIO_PORT, SHAKE_GPIO_PIN) == Bit_SET) ? 
           SHAKE_DETECTED : SHAKE_NORMAL;
}

/**
  * @brief  清除震动检测标志
  * @param  无
  * @retval 无
  */
void SHAKE_ClearFlag(void)
{
    shake_gpio_detected_flag = 0;
}

/**
  * @brief  获取震动次数
  * @param  无
  * @retval 震动次数
  */
uint32_t SHAKE_GetCount(void)
{
    return shake_gpio_count;
}

/**
  * @brief  重置震动计数器
  * @param  无
  * @retval 无
  */
void SHAKE_ResetCount(void)
{
    shake_gpio_count = 0;
}

/**
  * @brief  震动传感器外部中断处理函数
  * @param  无
  * @retval 无
  * @note   需要在stm32f10x_it.c中的EXTI15_10_IRQHandler中调用
  */
void SHAKE_EXTI_IRQHandler(void)
{
    uint32_t i;
    
    /* 检查是否是震动传感器触发的中断 */
    if(EXTI_GetITStatus(SHAKE_EXTI_LINE) != RESET)
    {
        /* 简单的延时去抖动 */
        for(i = 0; i < 1000; i++);
        
        /* 再次读取引脚状态确认 */
        if(GPIO_ReadInputDataBit(SHAKE_GPIO_PORT, SHAKE_GPIO_PIN) == Bit_SET)
        {
            shake_gpio_detected_flag = 1;
            shake_gpio_count++;
        }
        
        /* 清除中断标志 */
        EXTI_ClearITPendingBit(SHAKE_EXTI_LINE);
    }
}
