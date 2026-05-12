/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "dwt/bsp_dwt.h"   


void DWT_Init(void)
{
    /* 使能DWT外设 */
    DEMCR |= (uint32_t)DEMCR_TRCENA;
    
    /* DWT CYCCNT瀵勫瓨鍣ㄨ鏁版竻0 */
    DWT_CYCCNT = (uint32_t)0U;      //浣胯兘CYCCNT瀵勫瓨鍣ㄤ箣鍓嶏紝鍏堟竻0
    
    
    DWT_CTRL  |=(uint32_t)DWT_CTRL_CYCCNTENA;
}


uint32_t DWT_GetTick(void)
{ 
    return ((uint32_t)DWT_CYCCNT);
}


uint32_t DWT_TickToMicrosecond(uint32_t tick,uint32_t frequency)
{ 
    return (uint32_t)(1000000.0/frequency*tick);
}


void DWT_DelayUs(uint32_t time)
{
  /* Convert microseconds to the equivalent DWT tick count. */
  uint32_t tick_duration = time * (SystemCoreClock / 1000000);
  uint32_t tick_start = DWT_GetTick();         /* Capture the starting tick. */
    
    while(DWT_GetTick() - tick_start < tick_duration);
}


void DWT_DelayMs(uint32_t time)
{
    for(uint32_t i = 0; i < time; i++)
    {
        DWT_DelayUs(1000);
    }
}



void DWT_DelayS(uint32_t time)
{
    for(uint32_t i = 0; i < time; i++)
    {
        DWT_DelayMs(1000);
    }
}

/*********************************************END OF FILE**********************/

