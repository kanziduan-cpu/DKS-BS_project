/**
  ******************************************************************************
  * @file    GPIO/IOToggle/stm32f10x_it.c 
  * @author  MCD Application Team
  * @version V3.6.0
  * @date    20-September-2021
  * @brief   Main Interrupt Service Routines.
  *          This file provides template for all exceptions handler and peripherals
  *          interrupt service routine.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2011 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */

/* Includes ------------------------------------------------------------------*/
#include "stm32f10x_it.h"
#include "systick/bsp_systick.h"
#include "main.h"
#include "adc/bsp_adc.h"

/** @addtogroup STM32F10x_StdPeriph_Examples
  * @{
  */

/** @addtogroup GPIO_IOToggle
  * @{
  */

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/******************************************************************************/
/*            Cortex-M3 Processor Exceptions Handlers                         */
/******************************************************************************/

/**
  * @brief  This function handles NMI exception.
  * @param  None
  * @retval None
  */
void NMI_Handler(void)
{
}

/**
  * @brief  This function handles Hard Fault exception.
  * @param  None
  * @retval None
  */
void HardFault_Handler(void)
{
  /* Go to infinite loop when Hard Fault exception occurs */
  while (1)
  {
  }
}

/**
  * @brief  This function handles Memory Manage exception.
  * @param  None
  * @retval None
  */
void MemManage_Handler(void)
{
  /* Go to infinite loop when Memory Manage exception occurs */
  while (1)
  {
  }
}

/**
  * @brief  This function handles Bus Fault exception.
  * @param  None
  * @retval None
  */
void BusFault_Handler(void)
{
  /* Go to infinite loop when Bus Fault exception occurs */
  while (1)
  {
  }
}

/**
  * @brief  This function handles Usage Fault exception.
  * @param  None
  * @retval None
  */
void UsageFault_Handler(void)
{
  /* Go to infinite loop when Usage Fault exception occurs */
  while (1)
  {
  }
}

/**
  * @brief  This function handles SVCall exception.
  * @param  None
  * @retval None
  */
void SVC_Handler(void)
{
}

/**
  * @brief  This function handles Debug Monitor exception.
  * @param  None
  * @retval None
  */
void DebugMon_Handler(void)
{
}

/**
  * @brief  This function handles PendSV_Handler exception.
  * @param  None
  * @retval None
  */
void PendSV_Handler(void)
{
}

/**
  * @brief  This function handles SysTick Handler.
  * @param  None
  * @retval None
  */
void SysTick_Handler(void)
{
    SysTick_CountPlus();
    if(program_run_led_task.timer > 0x00)
    {
        program_run_led_task.timer--;
        if(program_run_led_task.timer == 0)
        {
            program_run_led_task.flag = 1;
        }
    }
    //esp8266配置完成才进行任务
    if(esp8266_configuration_completed_flag == 1)
    {
        if(dht11_rd_task.timer > 0x00)
        {
            dht11_rd_task.timer--;
            if(dht11_rd_task.timer == 0)
            {
                dht11_rd_task.flag = 1;
            }
        }

        if(mq135_task.timer > 0x00)
        {
            mq135_task.timer--;
            if(mq135_task.timer == 0)
            {
                DMA_Cmd(ADCX_DMA_CHANNEL,ENABLE);                   //使能DMA
                ADC_DMACmd(ADCX,ENABLE);                            //使能DMA请求
                ADC_SoftwareStartConvCmd(ADCX,ENABLE);              //由于没有采用外部触发,所以配置软件触发ADC转换
            }
        }
    }
}

/******************************************************************************/
/*                 STM32F10x Peripherals Interrupt Handlers                   */
/*  Add here the Interrupt Handler for the used peripheral(s) (PPP), for the  */
/*  available peripheral interrupt handler's name please refer to the startup */
/*  file (startup_stm32f10x_xx.s).                                            */
/******************************************************************************/

/**
  * @brief  This function handles ADC1_DMA interrupt request.
  * @param  None
  * @retval None
  */
void ADCX_INT_DMA_IRQHANDLER(void)
{
    if(DMA_GetITStatus(DMA1_IT_TC1) == SET)
    {
        ADC_SoftwareStartConvCmd(ADCX,DISABLE);             //由于没有采用外部触发,所以配置软件触发ADC转换
        DMA_Cmd(ADCX_DMA_CHANNEL,DISABLE);                   //失能DMA
        ADC_DMACmd(ADCX,DISABLE);                            //失能DMA请求
        mq135_task.flag = 1;
        DMA_SetCurrDataCounter(ADCX_DMA_CHANNEL,ADCX_CHANNEL_NUM);//重新设置DMA传输计数值,必须在DMA失能下进行
        DMA_ClearITPendingBit(DMA1_IT_TC1); 
    }
}

/**
  * @brief  This function handles PPP interrupt request.
  * @param  None
  * @retval None
  */
/*void PPP_IRQHandler(void)
{
}*/

/**
  * @}
  */

/**
  * @}
  */

