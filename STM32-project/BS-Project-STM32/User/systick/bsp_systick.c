/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "systick/bsp_systick.h"

static uint64_t systick_count = 0;







void SysTick_Init(void)
{
    
    if(SysTick_Config(SystemCoreClock/1000))    
    {
        while(1);   //初始化失败后一直死循环在这，也可方便debug排查
    }
    

}


void SysTick_CountPlus(void)
{
    systick_count++;
}


uint64_t SysTick_GetCount(void)
{
    return systick_count;
}


void SysTick_DelayMs(uint64_t time)
{
    uint64_t tick_start = SysTick_GetCount();
    while(SysTick_GetCount()-tick_start < time);
}

/*****************************END OF FILE***************************************/
