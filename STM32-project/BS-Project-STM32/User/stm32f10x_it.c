/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
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
      DMA_Cmd(ADCX_DMA_CHANNEL,ENABLE);
      ADC_DMACmd(ADCX,ENABLE);
      ADC_SoftwareStartConvCmd(ADCX,ENABLE);
    }
  }

  if(mpu6050_task.timer > 0x00)
    {
        mpu6050_task.timer--;
        if(mpu6050_task.timer == 0)
        {
            mpu6050_task.flag = 1;
        }
    }

  if(beep_task.timer > 0x00)
    {
    beep_task.timer--;
    if(beep_task.timer == 0)
        {
      beep_task.flag = 1;
    }
  }

  if(servo1_task.timer > 0x00)
  {
    servo1_task.timer--;
    if(servo1_task.timer == 0)
    {
      servo1_task.flag = 1;
    }
  }

  if(servo2_task.timer > 0x00)
  {
    servo2_task.timer--;
    if(servo2_task.timer == 0)
    {
      servo2_task.flag = 1;
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
        ADC_SoftwareStartConvCmd(ADCX,DISABLE);
        DMA_Cmd(ADCX_DMA_CHANNEL,DISABLE);
        ADC_DMACmd(ADCX,DISABLE);
        mq135_task.flag = 1;
    rain_soil_task.flag = 1;
        DMA_SetCurrDataCounter(ADCX_DMA_CHANNEL,ADCX_CHANNEL_NUM);
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

