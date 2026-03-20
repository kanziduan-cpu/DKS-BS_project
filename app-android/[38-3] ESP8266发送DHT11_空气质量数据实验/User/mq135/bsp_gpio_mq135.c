/**
  ******************************************************************************
  * @file       bsp_gpio_mq135.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      空气检测传感器函数接口
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

#include "mq135/bsp_gpio_mq135.h"

/**
  * @brief  初始化控制 空气检测传感器  的IO
  * @param  无
  * @retval 无
  */
void MQ135_GPIO_Config(void)
{
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 0    
    
    /* 开启 MQ135 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(MQ135_DO_GPIO_CLK_PORT,ENABLE);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 浮空输入、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Pin    = MQ135_DO_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_IN_FLOATING;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(MQ135_DO_GPIO_PORT,&gpio_initstruct);
   
#endif 
    
#if 1    
    
    /* 开启MQ135相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(MQ135_AO_GPIO_CLK_PORT,ENABLE);

    /*选择要控制的GPIO引脚、设置GPIO模式为 模拟输入、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode = GPIO_Mode_AIN;
    gpio_initstruct.GPIO_Pin = MQ135_AO_GPIO_PIN;
    gpio_initstruct.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_Init(MQ135_AO_GPIO_PORT,&gpio_initstruct);
    
#endif  
    
}

/**
  * @brief  检测 空气检测传感器 情况
  * @param  GPIOx：x 可以是 A，B，C等
  * @param  GPIO_Pin：待操作的pin脚号
  * @retval Bit_SET(污染气体浓度未超过阈值)、Bit_RESET(污染气体浓度超过阈值)
  */
BitAction MQ135_Scan(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin)
{
    if(GPIO_ReadInputDataBit(GPIOx,GPIO_Pin) == Bit_RESET)
    {
        return Bit_RESET;
    }
    else
    {
        return Bit_SET;
    }
    
//    return GPIO_ReadInputDataBit(GPIOx,GPIO_Pin);
}

/*****************************END OF FILE***************************************/
