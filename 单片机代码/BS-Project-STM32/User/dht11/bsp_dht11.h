/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __BSP_DHT11_H
#define __BSP_DHT11_H

#include "stm32f10x.h"



//DHT11_DATA
#define DHT11_DATA_GPIO_PORT          GPIOB                           /* GPIO绔彛 */
#define DHT11_DATA_GPIO_CLK_PORT      RCC_APB2Periph_GPIOB            /* GPIO绔彛鏃堕挓 */
#define DHT11_DATA_GPIO_PIN           GPIO_Pin_12                     


#define DHT11_DATA_IN()         GPIO_ReadInputDataBit(DHT11_DATA_GPIO_PORT,DHT11_DATA_GPIO_PIN)
#define DHT11_DATA_OUT(VALUE)   if(VALUE)   GPIO_SetBits(DHT11_DATA_GPIO_PORT,DHT11_DATA_GPIO_PIN);\
                                else      GPIO_ResetBits(DHT11_DATA_GPIO_PORT,DHT11_DATA_GPIO_PIN)

                                
typedef struct
{                            
   uint8_t humi_int;        
   uint8_t humi_deci;       
   uint8_t temp_int;        
   uint8_t temp_deci;       
   uint8_t check_sum;       
}DHT11_DATA_TYPEDEF;                               
                                
void DHT11_GPIO_Config(void);
void DHT11_DataPinModeConfig(GPIOMode_TypeDef mode);
uint8_t DHT11_ReadByte(void);
ErrorStatus DHT11_ReadData(DHT11_DATA_TYPEDEF *dht11_data);

#endif /* __BSP_DHT11_H */
