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
#include "led/bsp_gpio_led.h"
#include "beep/app_beep.h"
#include "servo/app_servo.h"

uint8_t esp8266_configuration_completed_flag = 0;//ESP8266配置完毕标志
#define CMD_NUMBER   8 //命令个数
char *Cmd[CMD_NUMBER] = { "G_LED_ON", "G_LED_OFF", "B_LED_ON", "B_LED_OFF", "BEEP_ON", "BEEP_OFF", "SERVO_LEFT", "SERVO_RIGHT" };//具体命令

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
        //esp8266配置完成才进行任务
        if(esp8266_configuration_completed_flag == 1)
        {
            Get_ESP82666_Cmd((char *)esp8266_receive.buffer);
        }
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
  * @brief  获取网络调试助手和串口调试助手发来的信息
  * @param  无
  * @retval 无
  */
void Get_ESP82666_Cmd( char * cmd)
{
	uint8_t i;
	for(i = 0;i < CMD_NUMBER; i++)
	{
     if(( bool ) strstr ( cmd, Cmd[i] ))
		 break;
	}
    
    printf ( "\r\n在网络调试助手或者串口调试助手上发送以下命令可以控制板载LED灯、蜂鸣器和舵机" );    //打印测试例程提示信息
    printf ( "\r\nG_LED_ON\r\nG_LED_OFF\r\nB_LED_ON\r\nB_LED_OFF\r\nBEEP_ON\r\nBEEP_OFF\r\nSERVO_LEFT\r\nSERVO_RIGHT\r\n" );
    
	switch(i)
    {
        case 0:
            printf("\r\n绿灯打开\r\n");
            LED_ON(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);
            break;
      
        case 1:
            printf("\r\n绿灯关闭\r\n");  
            LED_OFF(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);
            break;
        
        case 2:
            printf("\r\n蓝灯打开\r\n");
            LED_ON(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);
            break;
        
        case 3:
            printf("\r\n蓝灯关闭\r\n");
            LED_OFF(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);
            break;
    
        case 4:
            printf("\r\n蜂鸣器打开\r\n");
            beep_task.speed = 100;
            beep_task.beep_active = true;
            break;
    
        case 5:
            printf("\r\n蜂鸣器关闭\r\n");
            beep_task.beep_active = false;
            break;

        case 6:
            printf("\r\n舵机向左\r\n");
            servo1_task.turn_left_flag = 1;
            servo2_task.turn_left_flag = 1;
            break;
        
        case 7:
            printf("\r\n舵机向右\r\n");
            servo1_task.turn_right_flag = 1;
            servo2_task.turn_right_flag = 1;
            break;        
        default:
            break;      
    }   
}

/*****************************END OF FILE***************************************/
