/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "dht11/bsp_dht11.h" 
#include "dwt/bsp_dwt.h"  

/**
    * @brief  初始化控制 DHT11 的IO
    * @param  无
    * @retval 无
    */
void DHT11_GPIO_Config(void)
{
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef gpio_initstruct = {0};
      
    /* 开启 DHT11 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(DHT11_DATA_GPIO_CLK_PORT,ENABLE);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 上拉输入、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Pin    = DHT11_DATA_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_IPD;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(DHT11_DATA_GPIO_PORT,&gpio_initstruct);
   
}


/**
 * @brief  DHT11_DATA 引脚模式配置
 * @param  mode:引脚模式
 * @retval 无
 */
void DHT11_DataPinModeConfig(GPIOMode_TypeDef mode)
{
    
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef gpio_initstruct = {0};

    /*选择要控制的GPIO引脚、设置GPIO模式为 mode、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Pin    = DHT11_DATA_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = mode;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(DHT11_DATA_GPIO_PORT,&gpio_initstruct);
    
}

/**
 * @brief  DHT11_DATA 引脚模式配置
 * @param  无
 * @retval dht11_readbyte_temp:返回数据（8bit）
 */
uint8_t DHT11_ReadByte(void)
{
    uint8_t dht11_readbyte_temp = 0;
    
    for(uint8_t i = 0;i<8;i++)
    {
        /* 延时x us 这个延时需要大于低电平开始标志持续的时间即可 */
//        DWT_Delay_Us(54); 
        while(DHT11_DATA_IN() == Bit_RESET);
        
        /* 延时x us 这个延时需要大于数据0持续的时间,小于数据0+低电平开始信号持续时间即可 */
        DWT_DelayUs(40);   
        
        if(DHT11_DATA_IN() == Bit_SET) 
        {
            /* 等待数据1的高电平结束 */
            while(DHT11_DATA_IN() == Bit_SET);
            dht11_readbyte_temp |=(uint8_t)(0x1<<(7-i)); //鎶婁綅7-i浣嶇疆1锛孧SB鍏堣 
        }
        else  
        {
            dht11_readbyte_temp &=(uint8_t)(~(0x1<<(7-i))) ; //鎶婁綅7-i浣嶆竻0锛孧SB鍏堣 
        }
    }
    
    return dht11_readbyte_temp;

}


ErrorStatus DHT11_ReadData(DHT11_DATA_TYPEDEF *dht11_data)
{
    uint8_t count_timer_temp = 0;
    
    /*输出模式*/
    DHT11_DataPinModeConfig(GPIO_Mode_Out_OD);//1、配置开漏输出,前提有上拉电阻，且跳变速度符合应用，高电平驱动能力由上拉电阻决定，电阻大小和反应速度成反比，和功耗成正比
        
    /*起始信号*/
    DHT11_DATA_OUT(0);//总线空闲状态为高电平,主机把数据总线（SDA）拉低等待DHT11响应,主机把总线拉低一段时间至少18ms（最大不得超过30ms）,保证DHT11能检测到起始信号。
    DWT_DelayMs(20);
    
    DHT11_DATA_OUT(1);//鍏抽棴NMOS
    
    /*主机设为输入判断从机响应信号*/
    DHT11_DataPinModeConfig(GPIO_Mode_IPU);

    DWT_DelayUs(20);
    
    /*判断从机是否有低电平响应信号 如不响应则跳出，响应则向下运行*/
    if(DHT11_DATA_IN()== Bit_RESET) 
    {
        count_timer_temp = 0;
        /*轮询直到从机发出 的83us 低电平 应答信号结束*/
        while(DHT11_DATA_IN() == Bit_RESET)
        {
            if(count_timer_temp++ >83)//超时计数,传感器等待外部信号低电平结束，延迟后DHT11的DATA引脚处于输出状态，输出83微秒的低电平作为应答信号
            {
                return ERROR;
            }  
            DWT_DelayUs(1);
        }
        
        count_timer_temp = 0;
        /*轮询直到从机发出 的87us 高电平 通知主机接收数据*/
        while(DHT11_DATA_IN() == Bit_SET)
        {
            if(count_timer_temp++ >87)//超时计数,传感器等待外部信号低电平结束，延迟后DHT11的DATA引脚处于输出状态，输出87微秒的高电平作为应答信号
            {
                return ERROR;
            }  
            DWT_DelayUs(1);
        }
        
        /* 开始接收数据 */
        dht11_data->humi_int  = DHT11_ReadByte();        
        dht11_data->humi_deci = DHT11_ReadByte();        
        dht11_data->temp_int  = DHT11_ReadByte();        
        dht11_data->temp_deci = DHT11_ReadByte();        
        dht11_data->check_sum = DHT11_ReadByte();        
        
        /* 读取结束 */
        DWT_DelayUs(54);
        
        /*检查读取的数据是否正确*/
        
        if(dht11_data->check_sum == dht11_data->humi_int+dht11_data->humi_deci+dht11_data->temp_int+dht11_data->temp_deci)
        {
            return SUCCESS;
        }
        else
        {
            return ERROR;
        }
    }
    else
    {
        return ERROR;
    }
    
    
}
/*********************************************END OF FILE**********************/
