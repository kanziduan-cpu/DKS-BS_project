/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "mq135/app_mq135.h"
#include "adc/bsp_adc.h"
#include "mq135/bsp_gpio_mq135.h"
#include <math.h>

MQ135_TaskInfo mq135_task = {0};


void MQ135_TaskReset(void)
{
    mq135_task.timer  = mq135_task.cycle;
    mq135_task.flag   = 0;
}


void MQ135_TaskInit(uint32_t mq135_task_cycle)
{
    mq135_task.cycle   = mq135_task_cycle;
    MQ135_TaskReset();
}


float MQ135_Get_PPM(uint16_t adc_value)
{

    float vrl = 0;   
    float Rs;        
    float ppm = 0;       
    
	/* 璇诲彇AO杈撳嚭鐢靛帇 */
    vrl = (float)adc_value / 4095 * VC;
    /* 鎹㈢畻Rs鐢甸樆 */
    Rs = (float)(VC - vrl) * RL / vrl;
    
    float Rs0 = Rs/R0;  /* Rs/R0 */
    
    
    ppm =  A*pow(Rs/R0,B) ;
    
	return ppm;
}


void MQ135_Task(void)
{
    if(mq135_task.flag == 1)
    {
        /* 鎷熷悎鍑芥暟鎹㈢畻鍑簆pm */
        mq135_task.ppm = MQ135_Get_PPM(adc_source_convertedvalue[ADCX_MQ135_BUFFER_INDEX]);
        mq135_task.read_completed_flag = 1;
        MQ135_TaskReset();
    
    }
}
 
/*****************************END OF FILE***************************************/
