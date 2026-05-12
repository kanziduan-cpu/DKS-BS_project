/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "dht11/app_dht11.h"
#include "dht11/bsp_dht11.h" 

Dht11_TaskInfo dht11_rd_task  = {0};
DHT11_DATA_TYPEDEF dht11_data = {0};

void Dht11_TaskReset(void)
{
    dht11_rd_task.timer = dht11_rd_task.cycle;
    dht11_rd_task.flag  = 0;

}


void Dht11_TaskInit(uint32_t dht11_rd_task_cycle)
{
    dht11_rd_task.cycle = dht11_rd_task_cycle;
    Dht11_TaskReset();
}



void Dht11_Task(void)
{
    if(dht11_rd_task.flag)
    {
        if(DHT11_ReadData(&dht11_data) == SUCCESS)
        {
            dht11_rd_task.read_completed_flag = 1;//璇诲彇鎴愬姛
        }
        else
        {
            dht11_rd_task.read_completed_flag = 2;//读取失败
        }

        Dht11_TaskReset();
    }
}

/*****************************END OF FILE***************************************/
