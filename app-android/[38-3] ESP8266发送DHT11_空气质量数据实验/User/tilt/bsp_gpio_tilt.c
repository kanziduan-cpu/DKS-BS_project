/**
  ******************************************************************************
  * @file       bsp_gpio_tilt.c
  * @brief      倾斜传感器驱动函数
  ******************************************************************************
  */

#include "stm32f10x.h"
#include "bsp_gpio_tilt.h"

/* 全局变量定义 */
volatile uint8_t tilt_detected_flag = 0;
volatile uint32_t tilt_count = 0;

/**
  * @brief  倾斜传感器GPIO配置
  * @param  无
  * @retval 无
  */
void TILT_GPIO_Config(void)
{
    GPIO_InitTypeDef GPIO_InitStructure;
    
    /* 开启GPIO端口时钟 */
    RCC_APB2PeriphClockCmd(TILT_GPIO_CLK_PORT, ENABLE);
    
    /* 配置倾斜传感器引脚为输入，下拉模式 */
    GPIO_InitStructure.GPIO_Pin = TILT_GPIO_PIN;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IPD; // 下拉输入
    GPIO_Init(TILT_GPIO_PORT, &GPIO_InitStructure);
}

/**
  * @brief  倾斜传感器外部中断配置
  * @param  无
  * @retval 无
  */
void TILT_EXTI_Config(void)
{
    EXTI_InitTypeDef EXTI_InitStructure;
    NVIC_InitTypeDef NVIC_InitStructure;
    
    /* 开启AFIO时钟 */
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO, ENABLE);
    
    /* 连接EXTI线到GPIO引脚 */
    GPIO_EXTILineConfig(TILT_EXTI_PORTSOURCE, TILT_EXTI_PINSOURCE);
    
    /* 配置EXTI线 */
    EXTI_InitStructure.EXTI_Line = TILT_EXTI_LINE;
    EXTI_InitStructure.EXTI_Mode = EXTI_Mode_Interrupt;
    EXTI_InitStructure.EXTI_Trigger = EXTI_Trigger_Rising; // 上升沿触发
    EXTI_InitStructure.EXTI_LineCmd = ENABLE;
    EXTI_Init(&EXTI_InitStructure);
    
    /* 配置NVIC */
    NVIC_InitStructure.NVIC_IRQChannel = TILT_EXTI_IRQ;
    NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 2; // 优先级
    NVIC_InitStructure.NVIC_IRQChannelSubPriority = 1;
    NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;
    NVIC_Init(&NVIC_InitStructure);
}

/**
  * @brief  倾斜传感器初始化
  * @param  无
  * @retval 无
  */
void TILT_Init(void)
{
    TILT_GPIO_Config();
    TILT_EXTI_Config();
    tilt_detected_flag = 0;
    tilt_count = 0;
}

/**
  * @brief  读取倾斜传感器状态
  * @param  无
  * @retval 倾斜传感器状态 (TILT_NORMAL或TILT_DETECTED)
  */
uint8_t TILT_ReadStatus(void)
{
    return (GPIO_ReadInputDataBit(TILT_GPIO_PORT, TILT_GPIO_PIN) == Bit_SET) ? TILT_DETECTED : TILT_NORMAL;
}

/**
  * @brief  清除倾斜检测标志
  * @param  无
  * @retval 无
  */
void TILT_ClearFlag(void)
{
    tilt_detected_flag = 0;
}

/**
  * @brief  获取倾斜次数
  * @param  无
  * @retval 倾斜次数
  */
uint32_t TILT_GetCount(void)
{
    return tilt_count;
}

/**
  * @brief  重置倾斜计数器
  * @param  无
  * @retval 无
  */
void TILT_ResetCount(void)
{
    tilt_count = 0;
}

/**
  * @brief  倾斜传感器外部中断处理函数
  * @param  无
  * @retval 无
  * @note   需要在stm32f10x_it.c中的EXTI15_10_IRQHandler中调用
  */
void TILT_EXTI_IRQHandler(void)
{
    /* 检查是否是倾斜传感器触发的中断 */
    if(EXTI_GetITStatus(TILT_EXTI_LINE) != RESET)
    {
        /* 简单的延时去抖动 */
        for(uint32_t i = 0; i < 1000; i++);
        
        /* 再次读取引脚状态确认 */
        if(GPIO_ReadInputDataBit(TILT_GPIO_PORT, TILT_GPIO_PIN) == Bit_SET)
        {
            tilt_detected_flag = 1;
            tilt_count++;
        }
        
        /* 清除中断标志 */
        EXTI_ClearITPendingBit(TILT_EXTI_LINE);
    }
}






