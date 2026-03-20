/**
  ******************************************************************************
  * @file       app_dht11.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      读取DHT11温湿度传感器实验应用层函数接口
  ******************************************************************************
  * @attention
  *
  * 实验平台  ：野火 STM32F103C8T6-STM32开发板 
  * 论坛      ：http://www.firebbs.cn
  * 官网      ：https://embedfire.com/
  * 淘宝      ：https://yehuosm.tmall.com/
  *
  ******************************************************************************
  */
  
#include "dht11/app_dht11.h"
#include "dht11/bsp_dht11.h" 
#include "usart/usart_com.h"
#include "debug/bsp_debug.h"

Dht11_TaskInfo dht11_rd_task  = {0};
DHT11_DATA_TYPEDEF dht11_data = {0};
/**
  * @brief  DHT11温湿度传感器数据读取 计数复位
  * @param  无
  * @retval 无
  */
void Dht11_TaskReset(void)
{
    dht11_rd_task.timer = dht11_rd_task.cycle;
    dht11_rd_task.flag  = 0;

}

/**
  * @brief  DHT11温湿度传感器数据读取 任务初始化
  * @param  dht11_rd_task_cycle: 任务轮询周期 单位ms(可修改系统节拍定时器)
  * @retval 无
  */
void Dht11_TaskInit(uint32_t dht11_rd_task_cycle)
{
    dht11_rd_task.cycle = dht11_rd_task_cycle;
    Dht11_TaskReset();
}


/**
  * @brief  DHT11温湿度传感器数据读取 任务
  * @param  无
  * @retval 无
  */
void Dht11_Task(void)
{
    if(dht11_rd_task.flag)
    {
        if(DHT11_ReadData(&dht11_data) == SUCCESS)
        {
//            printf("READ_DHT11_DATA SUCCESS!\r\n");
            if(dht11_data.humi_deci&0x80)//判断是否低于0
            {
                printf("\r\n湿度为 -%d.%d ％RH\r\n",dht11_data.humi_int,dht11_data.humi_deci);
            }
            else
            {
                printf("\r\n湿度为 %d.%d ％RH\r\n",dht11_data.humi_int,dht11_data.humi_deci);
            }
            
            if(dht11_data.temp_deci&0x80)//判断是否低于0
            {
                printf("\r\n温度为 -%d.%d ℃\r\n",dht11_data.temp_int,dht11_data.temp_deci);
            }
            else
            {
                printf("\r\n温度为 %d.%d ℃\r\n",dht11_data.temp_int,dht11_data.temp_deci);
            }
            dht11_rd_task.read_completed_flag = 1;//读取成功
        }
        else
        {
            printf("READ_DHT11_DATA ERROR!\r\n");
            dht11_rd_task.read_completed_flag = 2;//读取失败
        }

        Dht11_TaskReset();
    }
}

/*****************************END OF FILE***************************************/
