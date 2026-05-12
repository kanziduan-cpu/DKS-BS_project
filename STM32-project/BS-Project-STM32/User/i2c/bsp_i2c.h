/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_I2C_H
#define	__BSP_I2C_H

#include "stm32f10x.h"

/* 定义 IIC 连接的GPIO端口, 用户只需要修改下面的代码即可改变控制的 IIC 引脚 */

//I2C1
#define IIC_I2C1                            I2C1
#define IIC_I2C1_CLK_PORT                   RCC_APB1Periph_I2C1

#define IIC_I2C1_SCL_GPIO_PORT    			GPIOB			                /* 瀵瑰簲GPIO绔彛 */
#define IIC_I2C1_SCL_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			
#define IIC_I2C1_SCL_GPIO_PIN			    GPIO_Pin_6	       				

#define IIC_I2C1_SDA_GPIO_PORT    			GPIOB			                /* 瀵瑰簲GPIO绔彛 */
#define IIC_I2C1_SDA_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			
#define IIC_I2C1_SDA_GPIO_PIN			    GPIO_Pin_7	       				

//I2C2
#define IIC_I2C2                            I2C2
#define IIC_I2C2_CLK_PORT                   RCC_APB1Periph_I2C2

#define IIC_I2C2_SCL_GPIO_PORT    			GPIOB			                /* 瀵瑰簲GPIO绔彛 */
#define IIC_I2C2_SCL_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			
#define IIC_I2C2_SCL_GPIO_PIN			    GPIO_Pin_10	       				

#define IIC_I2C2_SDA_GPIO_PORT    			GPIOB			                /* 瀵瑰簲GPIO绔彛 */
#define IIC_I2C2_SDA_GPIO_CLK_PORT 	        RCC_APB2Periph_GPIOB			
#define IIC_I2C2_SDA_GPIO_PIN			    GPIO_Pin_11	       				



#define IIC_SDA_IN()           GPIO_ReadInputDataBit(IIC_SDA_GPIO_PORT,IIC_SDA_GPIO_PIN)  

#define IIC_SCL_OUT(VALUE)     if(VALUE)    GPIO_SetBits(IIC_SCL_GPIO_PORT,IIC_SCL_GPIO_PIN);\
                               else       GPIO_ResetBits(IIC_SCL_GPIO_PORT,IIC_SCL_GPIO_PIN);

#define IIC_SDA_OUT(VALUE)     if(VALUE)    GPIO_SetBits(IIC_SDA_GPIO_PORT,IIC_SDA_GPIO_PIN);\
                               else       GPIO_ResetBits(IIC_SDA_GPIO_PORT,IIC_SDA_GPIO_PIN); 

#define IIC_DATA_READ          IIC_SDA_IN() 





/* 这个地址只要与外挂的I2C器件地址不一样即可 */
#define IIC_I2C1_OWN_ADDRESS7      0x0A  
#define IIC_I2C2_OWN_ADDRESS7      0x0B

/* IIC 速度模式 */  
#define IIC_SPEED              100000

/* 当前综合工程中 MPU6050 仅使用 I2C1(PB6/PB7)。
 * 禁用 I2C2，避免 PB10/PB11 与 ESP8266 的 USART3 冲突。 */
#define IIC_ENABLE_I2C1        1
#define IIC_ENABLE_I2C2        0

/* 等待超时时间，配合 1us 轮询延时使用 */
#define CHECK_TIMES         5000

typedef enum
{
    IIC_WRITE = 0,
    IIC_READ = 1
}IIC_Direction_TypeDef;

void IIC_PinConfig(void);
void IIC_Init(void);
void IIC_DELAY_US(uint32_t time);
ErrorStatus IIC_Start(I2C_TypeDef* I2Cx);
ErrorStatus IIC_Restart(I2C_TypeDef* I2Cx);
void IIC_Stop(I2C_TypeDef* I2Cx);
ErrorStatus IIC_SendData(I2C_TypeDef* I2Cx, uint8_t data);
ErrorStatus IIC_AddressMatching(I2C_TypeDef* I2Cx,uint8_t slave_addr,IIC_Direction_TypeDef direction);
ErrorStatus IIC_CheckDevice(I2C_TypeDef* I2Cx,uint8_t slave_addr);
ErrorStatus IIC_ReceiveData(I2C_TypeDef* I2Cx,uint8_t *data);

#endif /* __BSP_I2C_H  */
                                        
