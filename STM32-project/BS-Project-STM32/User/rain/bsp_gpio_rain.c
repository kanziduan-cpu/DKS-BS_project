/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "rain/bsp_gpio_rain.h"


void RAIN_GPIO_Config(void)
{
    
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 1    
    
    RCC_APB2PeriphClockCmd(RAIN_SIG_GPIO_CLK_PORT,ENABLE);

    
    gpio_initstruct.GPIO_Mode = GPIO_Mode_AIN;
    gpio_initstruct.GPIO_Pin = RAIN_SIG_GPIO_PIN;
    gpio_initstruct.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_Init(RAIN_SIG_GPIO_PORT,&gpio_initstruct);
    
#endif
    
}


float Calculate_RainHeight(uint16_t adc_value) 
{
    /* 常量定义（需根据实际传感器校准） */ 
    const float quadratic_coefficient   = 8.98e-6f;  
    const float linear_offset           = 17.6f;     
    const float units_bar_height        = 1.25f;     
    const uint16_t adc_max              = 4095;      

    if (adc_value >= adc_max) {
        return -1.0f; // 杈撳叆瓒呴檺鎴栦紶鎰熷櫒鏁呴殰
    }

    /* 鍏紡鍘熷瀷锛歨eight=(quadratic_coefficient脳adc_value^2-linear_offset)* units_bar_height */

    float rain_height = quadratic_coefficient * adc_value * adc_value - linear_offset;
    
    
    if (rain_height < 0) {
        return 0.0f; // 鏃犻洦閲忔垨浣庝簬浼犳劅鍣ㄧ伒鏁忓害
    }

    return rain_height * units_bar_height;
}

/*****************************END OF FILE***************************************/

