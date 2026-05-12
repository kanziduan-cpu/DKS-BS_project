/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "delay/bsp_delay.h"


void Rough_Delay(__IO uint32_t ncount)
{
    for(uint32_t i = 0;i<ncount;i++)
    {
        __NOP();
    }
}


void Rough_Delay_Us(__IO uint32_t time)
{
    Rough_Delay(7*time);
}


void Rough_Delay_Ms(__IO uint32_t time)
{
    Rough_Delay(0x3e8*7*time);
}


void Rough_Delay_S(__IO uint32_t time)
{
    Rough_Delay(0x3e8*0x3e8*7*time);
}

/*****************************END OF FILE***************************************/
