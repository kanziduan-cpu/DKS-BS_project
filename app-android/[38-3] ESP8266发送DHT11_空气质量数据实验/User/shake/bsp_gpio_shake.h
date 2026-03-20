#ifndef __BSP_GPIO_SHAKE_H
#define __BSP_GPIO_SHAKE_H

#include "stm32f10x.h"
#include <stdint.h>

/* 定义震动传感器连接的GPIO端口 */
#define SHAKE_GPIO_PORT    			GPIOB			                /* GPIO端口 */
#define SHAKE_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			/* GPIO端口时钟 */
#define SHAKE_GPIO_PIN			        GPIO_Pin_12	       				/* 连接到GPIO */

/* 定义震动传感器外部中断配置 */
#define SHAKE_EXTI_PORTSOURCE          GPIO_PortSourceGPIOB            /* 中断端口源 */
#define SHAKE_EXTI_PINSOURCE           GPIO_PinSource12               /* 中断PIN源 */
#define SHAKE_EXTI_LINE                EXTI_Line12                    /* 中断线 */
#define SHAKE_EXTI_IRQ                 EXTI15_10_IRQn                 /* 外部中断向量号 */
#define SHAKE_EXTI_IRQHANDLER          EXTI15_10_IRQHandler           /* 中断处理函数 */

/* 震动传感器状态 */
typedef enum {
    SHAKE_NORMAL = 0,      /* 正常状态，无震动 */
    SHAKE_DETECTED = 1     /* 检测到震动 */
} Shake_Status_TypeDef;

/* 全局变量声明 */
extern volatile uint8_t shake_gpio_detected_flag;
extern volatile uint32_t shake_gpio_count;

/* 函数声明 */
void SHAKE_GPIO_Config(void);
void SHAKE_EXTI_Config(void);
void SHAKE_Init(void);
uint8_t SHAKE_ReadStatus(void);
void SHAKE_ClearFlag(void);
uint32_t SHAKE_GetCount(void);
void SHAKE_ResetCount(void);

#endif /* __BSP_GPIO_SHAKE_H */
