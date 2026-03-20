#ifndef __BSP_GPIO_TILT_H
#define __BSP_GPIO_TILT_H

#include "stm32f10x.h"
#include <stdint.h>

/* 定义倾斜/雨量数字传感器连接的GPIO端口（通用IO1） */
#define TILT_GPIO_PORT    			GPIOB			                /* GPIO端口 */
#define TILT_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			/* GPIO端口时钟 */
#define TILT_GPIO_PIN			        GPIO_Pin_13	       				/* 连接到GPIO */

/* 定义倾斜传感器外部中断配置 */
#define TILT_EXTI_PORTSOURCE          GPIO_PortSourceGPIOB            /* 中断端口源 */
#define TILT_EXTI_PINSOURCE           GPIO_PinSource13               /* 中断PIN源 */
#define TILT_EXTI_LINE                EXTI_Line13                    /* 中断线 */
#define TILT_EXTI_IRQ                 EXTI15_10_IRQn                 /* 外部中断向量号 */
#define TILT_EXTI_IRQHANDLER          EXTI15_10_IRQHandler           /* 中断处理函数 */

/* 倾斜传感器状态 */
typedef enum {
    TILT_NORMAL = 0,      /* 正常状态，无倾斜 */
    TILT_DETECTED = 1     /* 检测到倾斜 */
} Tilt_Status_TypeDef;

/* 全局变量声明 */
extern volatile uint8_t tilt_detected_flag;
extern volatile uint32_t tilt_count;

/* 函数声明 */
void TILT_GPIO_Config(void);
void TILT_EXTI_Config(void);
void TILT_Init(void);
uint8_t TILT_ReadStatus(void);
void TILT_ClearFlag(void);
uint32_t TILT_GetCount(void);
void TILT_ResetCount(void);

#endif /* __BSP_GPIO_TILT_H */
