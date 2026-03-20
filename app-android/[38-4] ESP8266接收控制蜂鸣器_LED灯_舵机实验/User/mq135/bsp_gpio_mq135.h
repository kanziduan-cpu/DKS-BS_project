#ifndef __BSP_GPIO_MQ135_H
#define __BSP_GPIO_MQ135_H

#include "stm32f10x.h"

/* 定义 MQ135 连接的GPIO端口, 用户只需要修改下面的代码即可改变控制的 MQ135 引脚 */

// MQ135 DO
#define MQ135_DO_GPIO_PORT    			        GPIOA			                /* GPIO端口 */
#define MQ135_DO_GPIO_CLK_PORT 	                RCC_APB2Periph_GPIOA			/* GPIO端口时钟 */
#define MQ135_DO_GPIO_PIN			            GPIO_Pin_11	       				/* 连接到GPIO */

// MQ135 AO
#define MQ135_AO_GPIO_PORT    			        GPIOA			                /* GPIO端口 */
#define MQ135_AO_GPIO_CLK_PORT 	                RCC_APB2Periph_GPIOA			/* GPIO端口时钟 */
#define MQ135_AO_GPIO_PIN			            GPIO_Pin_4	       				/* 连接到GPIO */

void MQ135_GPIO_Config(void);
BitAction MQ135_Scan(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin);

#endif /* __BSP_GPIO_MQ135_H */
