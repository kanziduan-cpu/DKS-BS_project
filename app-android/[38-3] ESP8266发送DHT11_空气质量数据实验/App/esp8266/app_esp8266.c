/**
  ******************************************************************************
  * @file       app_esp8266.c
  * @author     embedfire
  * @version     V1.0
  * @date        2025
  * @brief      esp8266 应用层功能接口
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

#include "esp8266/app_esp8266.h"
#include "esp8266/bsp_esp8266.h"
#include "usart/usart_com.h"
#include <string.h>
#include "debug/bsp_debug.h"
#include "dht11/app_dht11.h"
#include "mq135/app_mq135.h"

uint8_t esp8266_configuration_completed_flag = 0;//ESP8266配置完毕标志
/**
  * @brief  ESP8266 接收缓冲区复位
  * @param  无
  * @retval 无
  */
void ESP8266_ReadBufferReset(void)
{
    memset(esp8266_receive.buffer,NULL,esp8266_receive.len);
    esp8266_receive.len = 0;
    esp8266_receive.read_flag = 0;
    esp8266_receive.Received_data_completed_flag = 0;
}

/**
  * @brief  ESP8266 任务初始化
  * @param  无
  * @retval 无
  */
void ESP8266_TaskInit(void)
{
    ESP8266_ReadBufferReset();
}

/**
  * @brief  ESP8266 任务
  * @param  无
  * @retval 无
  */
void ESP8266_Task(void)
{
    if(esp8266_receive.read_flag)
    {
//        USARTX_SendString(DEBUG_USARTX,"MCU已接收ESP8266数据:\r\n");
        USARTX_SendArray(DEBUG_USARTX,esp8266_receive.buffer,esp8266_receive.len);
        
        ESP8266_ReadBufferReset();
    }
}

/**
  * @brief  ESP8266 StaTcpClient Unvarnish 配置测试函数
  * @param  无
  * @retval 无
  */
void ESP8266_StaTcpClient_Unvarnish_ConfigTest(void)
{
    static uint8_t step = 0;

    switch(step)
    {
        case(0):if(ESP8266_AT_Test() == 1)
                {
                    step++;
                }
                break;

        case(1):printf( "\r\n正在启用DHCP ......\r\n" );
                if(ESP8266_DHCP_CUR() == 1)
                {
                    step++;
                }
                break;       

        case(2):printf( "\r\n正在配置工作模式 STA ......\r\n" );
                if(ESP8266_Net_Mode_Choose ( STA ) == 1)
                {
                    step++;
                }
                break;  

        case(3):printf( "\r\n正在连接 WiFi ......\r\n" );
                if(ESP8266_JoinAP ( macUser_ESP8266_ApSsid, macUser_ESP8266_ApPwd ) == 1)
                {
                    step++;
                }
                break;  

        case(4):printf( "\r\n正在连接 Server ......\r\n" );
                if(ESP8266_Link_Server ( enumTCP, macUser_ESP8266_TcpServer_IP, macUser_ESP8266_TcpServer_Port, Single_ID_0 ) == 1)
                {
                    step++;
                }
                break;  

        case(5):printf( "\r\n进入透传发送模式 ......\r\n" );
                if(ESP8266_UnvarnishSend () == 1)
                {
                    printf( "\r\n配置 ESP8266 完毕\r\n" );
                    printf ( "\r\n开始透传......\r\n" );
                    step++;
                    esp8266_configuration_completed_flag = 1;//ESP8266配置完毕标志
                }
                break;   
        default: break;
    }
}

/**
  * @brief  ESP8266 发送DHT11数据测试函数
  * @param  无
  * @retval 无
  */
void ESP8266_SendDHT11DataTest(void)
{
    char cStr [ 100 ] = { 0 };

    if( dht11_rd_task.read_completed_flag == 1 )//读取成功
    {
        sprintf ( cStr, "\r\n湿度为%d.%d ％RH ，温度为 %d.%d℃ \r\n", 
                  dht11_data.humi_int, dht11_data.humi_deci, dht11_data.temp_int, dht11_data.temp_deci );

        ESP8266_SendString ( ENABLE, cStr, 0, Single_ID_0 );               //发送 DHT11 温湿度信息到网络调试助手

        dht11_rd_task.read_completed_flag = 0;
      
    }
    else if( dht11_rd_task.read_completed_flag == 2 )//读取失败
    {

        sprintf ( cStr, "\r\nRead DHT11 ERROR!\r\n" );
        ESP8266_SendString ( ENABLE, cStr, 0, Single_ID_0 );               //发送 DHT11 温湿度信息到网络调试助手

        dht11_rd_task.read_completed_flag = 0;  
    }
}

/**
  * @brief  ESP8266 发送MQ135数据测试函数
  * @param  无
  * @retval 无
  */
void ESP8266_SendMQ135DataTest(void)
{
    char cStr [ 100 ] = { 0 };

    if( mq135_task.read_completed_flag == 1 )//读取成功
    {

        if(mq135_task.ppm<10)
        {
            sprintf( cStr, "\r\n综合污染气体平均浓度低于检测范围\r\n");
        }
        else if(mq135_task.ppm>1000)
        {
            sprintf( cStr, "\r\n综合污染气体平均浓度超过检测范围\r\n");
        }
        else
        {
            sprintf( cStr, "\r\n综合污染气体的平均浓度：%fppm\r\n",mq135_task.ppm);
        }      
  
    ESP8266_SendString ( ENABLE, cStr, 0, Single_ID_0 );               //发送 MQ135信息到网络调试助手

    mq135_task.read_completed_flag = 0;
    
    }
}

/*****************************END OF FILE***************************************/
