/**
  ******************************************************************************
  * @file       bsp_esp8266.c
  * @brief      esp8266 函数接口
  ******************************************************************************
  */

#include "stm32f10x.h"
#include "stm32f10x_gpio.h"
#include "stm32f10x_rcc.h"
#include "stm32f10x_usart.h"
#include "misc.h"
#include <stdlib.h>
#include "esp8266/bsp_esp8266.h"
#include "usart/usart_com.h"
#include "dwt/bsp_dwt.h"
#include "common/common.h"
#include "mpu6050/bsp_i2c_mpu6050.h"
#include "tilt/bsp_gpio_tilt.h"
#include "dht11/app_dht11.h"
#include "mq135/app_mq135.h"
#include "control/bsp_control.h"
#include "alarm/bsp_alarm.h"
#include "led/bsp_gpio_led.h"
#include "beep/bsp_gpio_beep.h"
#include "servo/bsp_servo.h"

ESP8266_DataTypeDef esp8266_receive = {0};
static uint16_t g_estimated_water_level = 8; /* cm，启动默认值 */
static uint32_t g_water_level_tick = 0;
static uint16_t g_water_limit_threshold = 70;
static uint16_t g_water_recover_threshold = 25;
/* 设备运行状态（用于和安卓端保持状态同步） */
static uint8_t g_fan_running = 0;
static uint8_t g_dehumidifier_running = 0;
static uint8_t g_light_running = 0;
static uint8_t g_pump_running = 0;


uint16_t ESP8266_UpdateWaterLevelByRain(uint8_t rain_detected)
{
    uint32_t now = DWT_GetTick();
    const uint32_t update_interval_ms = 1000;

    if (now - g_water_level_tick < update_interval_ms) {
        return g_estimated_water_level;
    }
    g_water_level_tick = now;

    /*
     * 雨量信号 -> 水位估算：
     * - 检测到雨：水位上升更快
     * - 未检测到雨：水位缓慢回落
     */
    if (rain_detected) {
        g_estimated_water_level = (g_estimated_water_level >= 96) ? 100 : (g_estimated_water_level + 4);
    } else {
        g_estimated_water_level = (g_estimated_water_level <= 1) ? 0 : (g_estimated_water_level - 1);
    }

    return g_estimated_water_level;
}

uint16_t ESP8266_GetEstimatedWaterLevel(void)
{
    return g_estimated_water_level;
}

uint16_t ESP8266_GetWaterLimitThreshold(void)
{
    return g_water_limit_threshold;
}

uint16_t ESP8266_GetWaterRecoverThreshold(void)
{
    return g_water_recover_threshold;
}

/**
 * @brief  初始化控制 ESP8266  的IO
 * @param  无
 * @retval 无
 */
void ESP8266_GPIO_PinConfig(void)
{
    
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 1  
    
    /* 开启 ESP8266 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(ESP8266_RST_GPIO_CLK_PORT,ENABLE);

    /* IO输出状态初始化控制 */
    GPIO_SetBits(ESP8266_RST_GPIO_PORT,ESP8266_RST_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 推挽输出、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Pin    = ESP8266_RST_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(ESP8266_RST_GPIO_PORT,&gpio_initstruct); 
    
#endif 
    
#if 1    
    
    /* 开启 ESP8266 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(ESP8266_IO_GPIO_CLK_PORT,ENABLE);

    /* IO输出状态初始化控制 */
    GPIO_SetBits(ESP8266_IO_GPIO_PORT,ESP8266_IO_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 推挽输出、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Pin    = ESP8266_IO_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(ESP8266_IO_GPIO_PORT,&gpio_initstruct); 
    
#endif  
}

/**
  * @brief  配置 ESP8266 串口中断配置
  * @param  无
  * @retval 无
  */
void ESP8266_NVIC_Config(void)
{
    /* 定义一个 NVIC 结构体 */
    NVIC_InitTypeDef nvic_initstruct = {0};
    
    /* 开启 AFIO 相关的时钟 */
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO,ENABLE); 
      
    /* 配置中断源 */
    nvic_initstruct.NVIC_IRQChannel                     = ESP8266_IRQ;
    /* 配置抢占优先级 */
    nvic_initstruct.NVIC_IRQChannelPreemptionPriority   =  1;
    /* 配置子优先级 */
    nvic_initstruct.NVIC_IRQChannelSubPriority          =  0;
    /* 使能配置中断通道 */
    nvic_initstruct.NVIC_IRQChannelCmd                  =  ENABLE;

    NVIC_Init(&nvic_initstruct);
    
}

/**
 * @brief  初始化控制 ESP8266 串口 的IO
 * @param  无
 * @retval 无
 */
void ESP8266_USART_PinConfig(void)
{
    
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 1  
    
    /* 开启 ESP8266 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(ESP8266_TX_GPIO_CLK_PORT,ENABLE);

    /*选择要控制的GPIO引脚、设置GPIO模式为 推挽复用、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_PP;
    gpio_initstruct.GPIO_Pin    = ESP8266_TX_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(ESP8266_TX_GPIO_PORT,&gpio_initstruct); 
    
#endif 
    
#if 1    
    
    /* 开启 ESP8266 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(ESP8266_RX_GPIO_CLK_PORT,ENABLE);

    /*选择要控制的GPIO引脚、设置GPIO模式为 上拉输入/浮空输入、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_IPU;
    gpio_initstruct.GPIO_Pin    = ESP8266_RX_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(ESP8266_RX_GPIO_PORT,&gpio_initstruct); 
    
#endif  
}

/**
 * @brief  配置 ESP8266 串口 模式
 * @param  无
 * @retval 无
 */
void ESP8266_USART_ModeConfig(void)
{
  
    /* 定义一个 USART 结构体 */
    USART_InitTypeDef usart_initstruct = {0};
    
    /* 开启 ESP8266 相关的GPIO外设/端口时钟 */
    ESP8266_APBXCLKCMD(ESP8266_USARTX_CLK_PORT,ENABLE);

    /* 配置串口的工作参数 */
    usart_initstruct.USART_BaudRate                 =  ESP8266_BAUDRATE;                  //配置波特率
    usart_initstruct.USART_HardwareFlowControl      =  USART_HardwareFlowControl_None;  //配置硬件流控制
    usart_initstruct.USART_Mode                     =  USART_Mode_Tx|USART_Mode_Rx;     //配置工作模式
    usart_initstruct.USART_Parity                   =  USART_Parity_No;                 //配置校验位    
    usart_initstruct.USART_StopBits                 =  USART_StopBits_1;                //配置停止位
    usart_initstruct.USART_WordLength               =  USART_WordLength_8b;             //配置帧数据字长
    
    USART_Init(ESP8266_USARTX, &usart_initstruct);
    
    USART_ITConfig(ESP8266_USARTX,USART_IT_RXNE,ENABLE);//开启串口数据接收中断
    USART_ITConfig(ESP8266_USARTX,USART_IT_IDLE,ENABLE);//开启串口数据空闲中断
    
}

/**
 * @brief  ESP8266 串口 初始化
 * @param  无
 * @retval 无
 */
void ESP8266_USART_Init(void)
{
    /* 配置 ESP8266 串口中断配置 */
    ESP8266_NVIC_Config();
    
    /* 配置 USARTX 模式 */
    ESP8266_USART_ModeConfig();

    /* 对应的 GPIO 的配置 */
    ESP8266_USART_PinConfig();
    
    /* 使能串口 */
    USART_Cmd(ESP8266_USARTX,ENABLE);

}

/*

1.因为数组下标从0开始，定义buffer[ESP8266_BUFFER_SIZE]时，可取的末尾是buffer[ESP8266_BUFFER_SIZE-1] 

  0   1   2   3  4  5  6  ……   ESP8266_BUFFER_SIZE-3   ESP8266_BUFFER_SIZE-2    ESP8266_BUFFER_SIZE-1

2.因为数组下标从0开始，当接收了len个，buffer[len-1]为最近一次保存位置，buffer[len]指向的是下一个准备接收空位，所以用len与ESP8266_BUFFER_SIZE-1比较。

3.此串口驱动需求为收发字符形式，所以为了简化处理作以下规定：
   在BUFFER末尾固定预留'\0'（为了方便直接将BUFFER给string系列函数使用。不会把'\0'算进len计数，其他字符都算进len计数）如果刚好接满时就如下面所示。

*/

/**
  * @brief  ESP8266 串口 中断回调函数
  * @param  无
  * @retval 无
  */
void ESP8266_IRQHANDLER(void)
{
    uint8_t data_temp = NULL;
    if(USART_GetITStatus(ESP8266_USARTX, USART_IT_RXNE) == SET)  
    {
        data_temp = USART_ReceiveData(ESP8266_USARTX);                                    //读取数据寄存器的数据，读取后对应的寄存器会被复位
        
        if((esp8266_receive.len < ESP8266_BUFFER_SIZE-1) && esp8266_receive.Received_data_completed_flag == 0)   //未接收满且程序不正在读取缓冲区，才把数据添加进缓冲区
        {
            esp8266_receive.buffer[esp8266_receive.len] = data_temp;
            esp8266_receive.len++;
        }
        if(esp8266_receive.len == ESP8266_BUFFER_SIZE-1)                    //如果接满数据包结束
        {
            esp8266_receive.buffer[esp8266_receive.len] = '\0';             //插入字符串结尾标志
            esp8266_receive.Received_data_completed_flag = 1;                                //正在读取数据标志置1，暂不接收
        }
        USART_ClearITPendingBit(ESP8266_USARTX,USART_IT_RXNE);            //清除接收标志位
    }
    if(USART_GetITStatus(ESP8266_USARTX, USART_IT_IDLE) == SET)  
    {
        USART_ReceiveData(ESP8266_USARTX);                                //特别提示，根据手册实际描述（读取获取USART_IT_IDLE、数据寄存器的数据，读取后对应的寄存器会被复位），用这样方式清除IDLE
        esp8266_receive.buffer[esp8266_receive.len] = '\0';                 //插入字符串结尾标志
        esp8266_receive.read_flag = 1;                                    //正在读取数据标志置1，暂不接收
    }
    
}

/**
 * @brief  ESP8266 初始化
 * @param  无
 * @retval 无
 */
void ESP8266_Init( void )
{
    /* 初始化ESP8266其它脚 */
    ESP8266_GPIO_PinConfig();
    
    /* 初始化ESP8266串口脚 */
    ESP8266_USART_Init();

    /* 复位ESP8266 */
    ESP8266_Rst();
}

/**
 * @brief  ESP8266 复位
 * @param  无
 * @retval 无
 */
void ESP8266_Rst( void )
{
    /* 复位引脚，低电平有效 */
    GPIO_ResetBits(ESP8266_RST_GPIO_PORT,ESP8266_RST_GPIO_PIN);
    DWT_DelayMs(500); 
    GPIO_SetBits(ESP8266_RST_GPIO_PORT,ESP8266_RST_GPIO_PIN);
    DWT_DelayMs(500);

}

/**
 * @brief  对ESP8266模块发送AT指令
 * @param  cmd，待发送的指令
 *         reply1，reply2，期待的响应，为NULL表不需响应，两者为或逻辑关系
 *         waittime，等待响应的时间
 * @retval 1，指令发送成功
 *         0，指令发送失败
 */
bool ESP8266_Cmd ( char * cmd, char * reply1, char * reply2, u32 waittime )
{    
//    strEsp8266_Fram_Record .InfBit .FramLength = 0;               //从新开始接收新的数据包

    USART_printf ( ESP8266_USARTX,"%s\r\n", cmd );          //向ESP8266发送指令
    
    if ( ( reply1 == 0 ) && ( reply2 == 0 ) )                      //不需要接收数据
        return true;
    
    DWT_DelayMs ( waittime );                 //延时
    
//    strEsp8266_Fram_Record .Data_RX_BUF [ strEsp8266_Fram_Record .InfBit .FramLength ]  = '\0';
//    
//    macPC_Usart ( "%s", strEsp8266_Fram_Record .Data_RX_BUF );
//    strEsp8266_Fram_Record .InfBit .FramLength = 0;                             //清除接收标志
//    strEsp8266_Fram_Record.InfBit.FramFinishFlag = 0;   
    while(esp8266_receive.Received_data_completed_flag == 1);       //等待接收完成
    bool result;
    if ( ( reply1 != 0 ) && ( reply2 != 0 ) )
        result = ( ( bool ) strstr ( (char *)esp8266_receive.buffer, reply1 ) || 
                        ( bool ) strstr ( (char *)esp8266_receive.buffer, reply2 ) ); 
    
    else if ( reply1 != 0 )
        result = ( ( bool ) strstr ( (char *)esp8266_receive.buffer, reply1 ) );
    
    else
        result = ( ( bool ) strstr ( (char *)esp8266_receive.buffer, reply2 ) );
    
    esp8266_receive.read_flag = 1;
    return result;
}

/**
 * @brief  对ESP8266模块进行AT测试启动
 * @param  无
 * @retval 1，测试成功
 *         0，测试失败
 */
bool ESP8266_AT_Test ( void )
{
    char count=0;
    	
    printf("\r\nAT测试.....\r\n");
    while ( count < 10 )
    {
        printf("\r\nAT测试次数 %d......\r\n", count);
        if( ESP8266_Cmd ( "AT", "OK", NULL, 500 ) )
        {
            printf("\r\nAT测试启动成功 %d......\r\n", count);
            return 1;
        }
//        ESP8266_Rst();
        ++count;
    }
  return 0;
}

/**
 * @brief  对ESP8266模块进行DHCP配置
 * @param  无
 * @retval 1，配置成功
 *         0，配置失败
 */
bool ESP8266_DHCP_CUR ( void )
{
	char cCmd [40];

	sprintf ( cCmd, "AT+CWDHCP=1,1");
	
	return ESP8266_Cmd ( cCmd, "OK", NULL, 500 );
	
}

/**
 * @brief  选择ESP8266模块的工作模式
 * @param  enumMode，工作模式
 * @retval 1，选择成功
 *         0，选择失败
 */
bool ESP8266_Net_Mode_Choose ( ENUM_Net_ModeTypeDef enumMode )
{
	switch ( enumMode )
	{
		case STA:
			return ESP8266_Cmd ( "AT+CWMODE=1", "OK", "no change", 2500 ); 
		
	  case AP:
		  return ESP8266_Cmd ( "AT+CWMODE=2", "OK", "no change", 2500 ); 
		
		case STA_AP:
		  return ESP8266_Cmd ( "AT+CWMODE=3", "OK", "no change", 2500 ); 
		
	  default:
		  return false;
  }
	
}

/**
 * @brief  ESP8266模块连接外部WiFi
 * @param  pSSID，WiFi名称字符串
 *         pPassWord，WiFi密码字符串
 * @retval 1，连接成功
 *         0，连接失败
 */
bool ESP8266_JoinAP ( char * pSSID, char * pPassWord )
{
	char cCmd [120];

	sprintf ( cCmd, "AT+CWJAP=\"%s\",\"%s\"", pSSID, pPassWord );
	
	return ESP8266_Cmd ( cCmd, "OK", NULL, 5000 );
	
}

/**
 * @brief  ESP8266模块创建WiFi热点
 * @param  pSSID，WiFi名称字符串
 *         pPassWord，WiFi密码字符串
 *         enunPsdMode，WiFi加密方式代号字符串
 * @retval 1，创建成功
 *         0，创建失败
 */
bool ESP8266_BuildAP ( char * pSSID, char * pPassWord, ENUM_AP_PsdMode_TypeDef enunPsdMode )
{
	char cCmd [120];

	sprintf ( cCmd, "AT+CWSAP=\"%s\",\"%s\",1,%d", pSSID, pPassWord, enunPsdMode );
	
	return ESP8266_Cmd ( cCmd, "OK", 0, 1000 );
	
}

/**
 * @brief  ESP8266模块启动多连接
 * @param  enumEnUnvarnishTx，配置是否多连接
 * @retval 1，配置成功
 *         0，配置失败
 */
bool ESP8266_Enable_MultipleId ( FunctionalState enumEnUnvarnishTx )
{
	char cStr [20];
	
	sprintf ( cStr, "AT+CIPMUX=%d", ( enumEnUnvarnishTx ? 1 : 0 ) );
	
	return ESP8266_Cmd ( cStr, "OK", 0, 500 );
	
}

/**
 * @brief  ESP8266模块连接外部服务器
 * @param  enumE，网络协议
 *         ip，服务器IP字符串
 *         ComNum，服务器端口字符串
 *         id，模块连接服务器的ID
 * @retval 1，连接成功
 *         0，连接失败
 */
bool ESP8266_Link_Server ( ENUM_NetPro_TypeDef enumE, char * ip, char * ComNum, ENUM_ID_NO_TypeDef id)
{
	char cStr [100] = { 0 }, cCmd [120];

  switch (  enumE )
  {
		case enumTCP:
		  sprintf ( cStr, "\"%s\",\"%s\",%s", "TCP", ip, ComNum );
		  break;
		
		case enumUDP:
		  sprintf ( cStr, "\"%s\",\"%s\",%s", "UDP", ip, ComNum );
		  break;
		
		default:
			break;
  }

  if ( id < 5 )
    sprintf ( cCmd, "AT+CIPSTART=%d,%s", id, cStr);

  else
	  sprintf ( cCmd, "AT+CIPSTART=%s", cStr );

	return ESP8266_Cmd ( cCmd, "OK", "ALREAY CONNECT", 4000 );
	
}

/**
 * @brief  ESP8266模块开启或关闭服务器模式
 * @param  enumMode，开启/关闭
 *         pPortNum，服务器端口号字符串
 *         pTimeOver，服务器超时时间字符串，单位：秒
 * @retval 1，操作成功
 *         0，操作失败
 */
bool ESP8266_StartOrShutServer ( FunctionalState enumMode, char * pPortNum, char * pTimeOver )
{
	char cCmd1 [120], cCmd2 [120];

	if ( enumMode )
	{
		sprintf ( cCmd1, "AT+CIPSERVER=%d,%s", 1, pPortNum );
		
		sprintf ( cCmd2, "AT+CIPSTO=%s", pTimeOver );

		return ( ESP8266_Cmd ( cCmd1, "OK", 0, 500 ) &&
						 ESP8266_Cmd ( cCmd2, "OK", 0, 500 ) );
	}
	
	else
	{
		sprintf ( cCmd1, "AT+CIPSERVER=%d,%s", 0, pPortNum );

		return ESP8266_Cmd ( cCmd1, "OK", 0, 500 );
	}
	
}

/**
 * @brief  获取 ESP8266 的连接状态，较适合单端口时使用
 * @param  无
 * @retval 2，获得ip
 *         3，建立连接
 *         4，失去连接
 *         0，获取状态失败
 */
uint8_t ESP8266_Get_LinkStatus ( void )
{
	if ( ESP8266_Cmd ( "AT+CIPSTATUS", "OK", 0, 500 ) )
	{
		if ( strstr ( (char *)esp8266_receive.buffer, "STATUS:2\r\n" ) )
			return 2;
		
		else if ( strstr ( (char *)esp8266_receive.buffer, "STATUS:3\r\n" ) )
			return 3;
		
		else if ( strstr ( (char *)esp8266_receive.buffer, "STATUS:4\r\n" ) )
			return 4;		

	}
	
	return 0;
	
}

/**
 * @brief  获取 ESP8266 的端口（Id）连接状态，较适合多端口时使用
 * @param  无
 * @retval 端口（Id）的连接状态，低5位为有效位，分别对应Id5~0，某位若置1表该Id建立了连接，若被清0表该Id未建立连接
 */
uint8_t ESP8266_Get_IdLinkStatus ( void )
{
	uint8_t ucIdLinkStatus = 0x00;
	
	
	if ( ESP8266_Cmd ( "AT+CIPSTATUS", "OK", 0, 500 ) )
	{
		if ( strstr ( (char *)esp8266_receive.buffer, "+CIPSTATUS:0," ) )
			ucIdLinkStatus |= 0x01;
		else 
			ucIdLinkStatus &= ~ 0x01;
		
		if ( strstr ( (char *)esp8266_receive.buffer, "+CIPSTATUS:1," ) )
			ucIdLinkStatus |= 0x02;
		else 
			ucIdLinkStatus &= ~ 0x02;
		
		if ( strstr ( (char *)esp8266_receive.buffer, "+CIPSTATUS:2," ) )
			ucIdLinkStatus |= 0x04;
		else 
			ucIdLinkStatus &= ~ 0x04;
		
		if ( strstr ( (char *)esp8266_receive.buffer, "+CIPSTATUS:3," ) )
			ucIdLinkStatus |= 0x08;
		else 
			ucIdLinkStatus &= ~ 0x08;
		
		if ( strstr ( (char *)esp8266_receive.buffer, "+CIPSTATUS:4," ) )
			ucIdLinkStatus |= 0x10;
		else 
			ucIdLinkStatus &= ~ 0x10;	

	}
	
	return ucIdLinkStatus;
	
}

/**
 * @brief  获取 ESP8266 的 AP IP
 * @param  pApIp，存放 AP IP 的数组的首地址
 *         ucArrayLength，存放 AP IP 的数组的长度
 * @retval 0，获取失败
 *         1，获取成功
 */
uint8_t ESP8266_Inquire_ApIp ( char * pApIp, uint8_t ucArrayLength )
{
	char uc;
	
	char * pCh;
	
	
  ESP8266_Cmd ( "AT+CIFSR", "OK", 0, 500 );
	
	pCh = strstr ( (char *)esp8266_receive.buffer, "APIP,\"" );
	
	if ( pCh )
		pCh += 6;
	
	else
		return 0;
	
	for ( uc = 0; uc < ucArrayLength; uc ++ )
	{
		pApIp [ uc ] = * ( pCh + uc);
		
		if ( pApIp [ uc ] == '\"' )
		{
			pApIp [ uc ] = '\0';
			break;
		}
		
	}
	
	return 1;
	
}

/**
 * @brief  配置ESP8266模块进入透传发送
 * @param  无
 * @retval 1，配置成功
 *         0，配置失败
 */
bool ESP8266_UnvarnishSend ( void )
{
	if ( ! ESP8266_Cmd ( "AT+CIPMODE=1", "OK", 0, 500 ) )
		return false;
	
	return 
	  ESP8266_Cmd ( "AT+CIPSEND", "OK", ">", 500 );
	
}

/**
 * @brief  配置ESP8266模块退出透传模式
 * @param  无
 * @retval 无
 */
void ESP8266_ExitUnvarnishSend ( void )
{
	DWT_DelayMs ( 1000 );
	
	USART_printf ( ESP8266_USARTX,"+++" );
    
	DWT_DelayMs ( 500 ); 
	
}

/**
 * @brief  ESP8266模块发送字符串
 * @param  enumEnUnvarnishTx，声明是否已使能了透传模式
 *         pStr，要发送的字符串
 *         ulStrLength，要发送的字符串的字节数
 *         ucId，哪个ID发送的字符串
 * @retval 1，发送成功
 *         0，发送失败
 */
bool ESP8266_SendString ( FunctionalState enumEnUnvarnishTx, char * pStr, u32 ulStrLength, ENUM_ID_NO_TypeDef ucId )
{
	char cStr [20];
	bool bRet = false;
	
		
	if ( enumEnUnvarnishTx )
	{
		USART_printf ( ESP8266_USARTX,"%s", pStr );
		
		bRet = true;
		
	}

	else
	{
		if ( ucId < 5 )
			sprintf ( cStr, "AT+CIPSEND=%d,%d", ucId, ulStrLength + 2 );

		else
			sprintf ( cStr, "AT+CIPSEND=%d", ulStrLength + 2 );
		
		ESP8266_Cmd ( cStr, "> ", 0, 100 );

		bRet = ESP8266_Cmd ( pStr, "SEND OK", 0, 500 );
  }
	
	return bRet;

}

/**
 * @brief  ESP8266模块接收字符串
 * @param  enumEnUnvarnishTx，声明是否已使能了透传模式
 * @retval 接收到的字符串首地址
 */
char * ESP8266_ReceiveString ( FunctionalState enumEnUnvarnishTx )
{
	char * pRecStr = 0;
//	strEsp8266_Fram_Record .InfBit .FramLength = 0;
//	strEsp8266_Fram_Record .InfBit .FramFinishFlag = 0;
//	
//	while ( ! strEsp8266_Fram_Record .InfBit .FramFinishFlag );
//	esp8266_receive.buffer [ strEsp8266_Fram_Record .InfBit .FramLength ] = '\0';
    
	while(esp8266_receive.Received_data_completed_flag == 1);       //等待接收完成
    
	if ( enumEnUnvarnishTx )
		pRecStr = (char *)esp8266_receive.buffer;
	
	else 
	{
		if ( strstr ( (char *)esp8266_receive.buffer, "+IPD" ) )
			pRecStr = (char *)esp8266_receive.buffer;

	}

	return pRecStr;
	
}

/**
  * @brief  发送MPU6050姿态数据
  * @param  无
  * @retval 无
  */
void ESP8266_SendMPU6050DataTest(void)
{
    MPU6050_SensorData_TypeDef sensor_data;
    char data_str[200];
    
    /* 读取MPU6050数据 */
    MPU6050_ReadSensorData(&sensor_data);
    
    /* 检测震动 */
    Shake_Severity_TypeDef shake_severity = MPU6050_DetectShake(&sensor_data);
    
    /* 格式化数据字符串 - 将short类型转换为float */
    sprintf(data_str, "{\"sensor\":\"MPU6050\",\"accel\":{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f},\"gyro\":{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f},\"shake\":%d}\r\n",
            (float)sensor_data.acc_x/16384.0f, (float)sensor_data.acc_y/16384.0f, (float)sensor_data.acc_z/16384.0f,
            (float)sensor_data.gyro_x/131.0f, (float)sensor_data.gyro_y/131.0f, (float)sensor_data.gyro_z/131.0f,
            shake_severity);
    
    /* 发送数据 */
    ESP8266_SendString(DISABLE, data_str, strlen(data_str), Multiple_ID_0);
    
    /* 打印调试信息 */
    printf("MPU6050 Data: %s", data_str);
}

/**
  * @brief  发送倾斜传感器数据
  * @param  无
  * @retval 无
  */
void ESP8266_SendTiltDataTest(void)
{
    uint8_t tilt_status;
    char data_str[100];
    
    /* 读取倾斜传感器状态 */
    tilt_status = TILT_ReadStatus();
    
    /* 格式化数据字符串 */
    sprintf(data_str, "{\"sensor\":\"TILT\",\"status\":%d,\"detected\":%s}\r\n",
            tilt_status, tilt_status ? "true" : "false");
    
    /* 发送数据 */
    ESP8266_SendString(DISABLE, data_str, strlen(data_str), Multiple_ID_0);
    
    /* 打印调试信息 */
    printf("Tilt Sensor Data: %s", data_str);
    
    /* 清除标志 */
    TILT_ClearFlag();
}

/**
  * @brief  发送所有传感器数据
  * @param  无
  * @retval 无
  */
void ESP8266_SendAllSensorData(void)
{
    float temperature = 0, humidity = 0;
    uint16_t air_quality = 0;
    MPU6050_SensorData_TypeDef mpu6050_data;
    uint8_t tilt_status = 0;
    uint8_t vent_state = 0;
    uint8_t alarm_active = 0;
    char data_str[500];
    
    /* 读取所有传感器数据 */
    DHT11_Read_Data(&temperature, &humidity);
    air_quality = MQ135_ReadValue();
    MPU6050_ReadSensorData(&mpu6050_data);
    tilt_status = TILT_ReadStatus();
    vent_state = CONTROL_GetVentState();
    alarm_active = ALARM_IsActive();
    
    /* 格式化JSON数据字符串 - 将short类型转换为float */
    sprintf(data_str,
            "{\"system\":\"MULTI_SENSOR\","
            "\"dht11\":{\"temperature\":%.1f,\"humidity\":%.1f},"
            "\"air_quality\":%d,"
            "\"mpu6050\":{\"accel\":{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f},"
            "\"gyro\":{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f}},"
            "\"tilt\":%d,"
            "\"vent\":%d,"
            "\"alarm\":%d}\r\n",
            temperature, humidity, air_quality,
            (float)mpu6050_data.acc_x/16384.0f, (float)mpu6050_data.acc_y/16384.0f, (float)mpu6050_data.acc_z/16384.0f,
            (float)mpu6050_data.gyro_x/131.0f, (float)mpu6050_data.gyro_y/131.0f, (float)mpu6050_data.gyro_z/131.0f,
            tilt_status, vent_state, alarm_active);
    
    /* 发送数据 */
    ESP8266_SendString(DISABLE, data_str, strlen(data_str), Multiple_ID_0);
    
    /* 打印调试信息 */
    printf("All Sensor Data: %s", data_str);
    
    /* 清除标志 */
    TILT_ClearFlag();
}

/* ================= MQTT 功能实现 ================= */

/**
  * @brief  连接WiFi
  * @param  无
  * @retval 1 连接成功，0 连接失败
  */
bool ESP8266_ConnectWiFi(void)
{
    /* 设置为Station模式 */
    if(!ESP8266_Net_Mode_Choose(STA))
    {
        printf("设置Station模式失败\r\n");
        return false;
    }
    
    /* 连接WiFi */
    printf("正在连接WiFi: %s\r\n", WIFI_SSID);
    if(!ESP8266_JoinAP((char*)WIFI_SSID, (char*)WIFI_PASSWORD))
    {
        printf("WiFi连接失败\r\n");
        return false;
    }
    
    printf("WiFi连接成功\r\n");
    DWT_DelayMs(2000);
    
    return true;
}

/**
  * @brief  检查ESP8266是否支持MQTT AT指令
  * @param  无
  * @retval 1 支持，0 不支持
  */
bool ESP8266_CheckMQTTCapability(void)
{
    /* 输出固件版本，便于确认AT固件类型 */
    ESP8266_Cmd("AT+GMR", "OK", NULL, 1000);
    printf("ESP8266固件信息: %s\r\n", (char*)esp8266_receive.buffer);
    esp8266_receive.read_flag = 0;
    esp8266_receive.len = 0;
    esp8266_receive.Received_data_completed_flag = 0;

    /*
     * MQTT能力探测：
     * 若固件支持MQTT AT命令，返回OK；
     * 若不支持，通常返回ERROR或busy。
     */
    if(ESP8266_Cmd("AT+MQTTUSERCFG=?", "OK", NULL, 1500))
    {
        printf("ESP8266支持MQTT直连公网Broker\r\n");
        return true;
    }

    printf("ESP8266当前AT固件不支持MQTT指令，请刷支持MQTT的AT固件\r\n");
    return false;
}

/**
  * @brief  连接MQTT服务器
  * @param  无
  * @retval 1 连接成功，0 连接失败
  */
bool ESP8266_ConnectMQTT(void)
{
    char cmd[200];
    
    printf("正在连接MQTT服务器...\r\n");
    
    /* 设置MQTT参数 - AT+MQTTUSERCFG配置 */
    sprintf(cmd, "AT+MQTTUSERCFG=0,1,\"%s\",\"%s\",\"%s\",0,0,\"\"", 
            MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD);
    if(!ESP8266_Cmd(cmd, "OK", NULL, 2000))
    {
        printf("MQTT用户配置失败\r\n");
        return false;
    }
    
    /* 连接MQTT服务器 - AT+MQTTCONN配置 */
    sprintf(cmd, "AT+MQTTCONN=0,\"%s\",%s,1", MQTT_SERVER_IP, MQTT_SERVER_PORT);
    if(!ESP8266_Cmd(cmd, "CONNECT", NULL, 5000))
    {
        printf("MQTT服务器连接失败\r\n");
        return false;
    }
    
    printf("MQTT服务器连接成功\r\n");
    DWT_DelayMs(1000);
    
    return true;
}

/**
  * @brief  订阅MQTT主题
  * @param  无
  * @retval 1 订阅成功，0 订阅失败
  */
bool ESP8266_MQTTSubscribe(void)
{
    char cmd[200];
    
    printf("正在订阅主题: %s\r\n", TOPIC_SUB_COMMAND);
    
    /* 订阅主题 - AT+MQTTSUB配置 */
    sprintf(cmd, "AT+MQTTSUB=0,\"%s\",0", TOPIC_SUB_COMMAND);
    if(!ESP8266_Cmd(cmd, "OK", NULL, 2000))
    {
        printf("MQTT主题订阅失败\r\n");
        return false;
    }
    
    printf("MQTT主题订阅成功\r\n");
    return true;
}

/**
  * @brief  发布MQTT消息
  * @param  topic 主题字符串
  * @param  payload 消息内容字符串
  * @retval 1 发布成功，0 发布失败
  */
bool ESP8266_MQTTPublish(char *topic, char *payload)
{
    char cmd[500];
    
    /* 发布消息 - AT+MQTTPUB配置 */
    sprintf(cmd, "AT+MQTTPUB=0,\"%s\",\"%s\",0,0", topic, payload);
    if(!ESP8266_Cmd(cmd, "OK", NULL, 2000))
    {
        printf("MQTT消息发布失败\r\n");
        return false;
    }
    
    return true;
}

/**
  * @brief  发布设备状态到MQTT（安卓端设备页使用）
  * @param  device_id 设备ID（FAN_SYS/DH_SYS/LIGHT_SYS/PUMP_SYS/STM32_MAIN）
  * @param  is_running 运行状态
  * @retval 无
  */
void ESP8266_SendDeviceStatusToMQTT(const char *device_id, uint8_t is_running)
{
    char payload[220];
    const char *id = (device_id != NULL && strlen(device_id) > 0) ? device_id : "STM32_MAIN";

    sprintf(payload,
            "{\"deviceId\":\"%s\","
            "\"status\":\"ONLINE\","
            "\"isRunning\":%s,"
            "\"timestamp\":%lu}",
            id,
            is_running ? "true" : "false",
            DWT_GetTick());

    if(!ESP8266_MQTTPublish((char*)TOPIC_PUB_DEVICE_STATUS, payload))
    {
        printf("设备状态发布失败: %s\r\n", id);
    }
}

/**
  * @brief  发送传感器数据到MQTT
  * @param  无
  * @retval 无
  */
void ESP8266_SendSensorDataToMQTT(void)
{
    float temperature = 0, humidity = 0;
    uint16_t air_quality = 0;
    uint16_t water_level = 0;
    MPU6050_SensorData_TypeDef mpu6050_data;
    uint8_t rain_detected = 0;
    uint8_t vent_state = 0;
    uint8_t alarm_active = 0;
    Shake_Severity_TypeDef shake_severity;
    char payload[600];
    
    /* 读取所有传感器数据 */
    DHT11_Read_Data(&temperature, &humidity);
    air_quality = MQ135_ReadValue();
    rain_detected = TILT_ReadStatus(); /* 通用IO1接雨量检测器，复用倾斜IO读取 */
    water_level = ESP8266_UpdateWaterLevelByRain(rain_detected);
    MPU6050_ReadSensorData(&mpu6050_data);
    shake_severity = MPU6050_DetectShake(&mpu6050_data);
    vent_state = CONTROL_GetVentState();
    alarm_active = ALARM_IsActive();
    
    /* 发送扁平化JSON，便于Android端 EnvironmentData 直接反序列化 */
    sprintf(payload,
            "{\"deviceId\":\"STM32_MAIN\","
            "\"timestamp\":%lu,"
            "\"temperature\":%.1f,"
            "\"humidity\":%.1f,"
            "\"aqi\":%d,"
            "\"co\":%d,"
            "\"co2\":%d,"
            "\"waterLevel\":%d,"
            "\"tiltX\":%.2f,"
            "\"tiltY\":%.2f,"
            "\"tiltZ\":%.2f,"
            "\"vibration\":%d,"
            "\"rainDetected\":%d,"
            "\"vent\":%d,"
            "\"alarm\":%d"
            "}",
            DWT_GetTick(),
            temperature, humidity,
            (air_quality > 500 ? 500 : air_quality),
            air_quality,
            400 + air_quality,
            water_level,
            (float)mpu6050_data.acc_x / 16384.0f * 90.0f,
            (float)mpu6050_data.acc_y / 16384.0f * 90.0f,
            (float)mpu6050_data.acc_z / 16384.0f * 90.0f,
            shake_severity * 25,
            rain_detected,
            vent_state,
            alarm_active);
    
    /* 发布MQTT消息 */
    if(ESP8266_MQTTPublish((char*)TOPIC_PUB_STATUS, payload))
    {
        printf("传感器数据已发送到MQTT: %s\r\n", payload);
    }
    else
    {
        printf("传感器数据发送失败\r\n");
    }
    
    /* 清除标志 */
    TILT_ClearFlag();
}

/**
  * @brief  解析MQTT消息
  * @param  msg 接收到的消息字符串
  * @param  mqtt_msg 解析后的MQTT消息结构体
  * @retval 1 解析成功，0 解析失败
  */
bool ESP8266_ParseMQTTMessage(char *msg, MQTT_Message_TypeDef *mqtt_msg)
{
    char *start, *end;
    
    /* 初始化结构体 */
    memset(mqtt_msg, 0, sizeof(MQTT_Message_TypeDef));
    
    /* 解析 deviceId */
    start = strstr(msg, "\"deviceId\":\"");
    if(start)
    {
        start += strlen("\"deviceId\":\"");
        end = strstr(start, "\"");
        if(end)
        {
            strncpy(mqtt_msg->device_id, start, end - start);
            mqtt_msg->device_id[end - start] = '\0';
        }
    }
    
    /* 解析 action */
    start = strstr(msg, "\"action\":\"");
    if(start)
    {
        start += strlen("\"action\":\"");
        end = strstr(start, "\"");
        if(end)
        {
            strncpy(mqtt_msg->action, start, end - start);
            mqtt_msg->action[end - start] = '\0';
        }
    }
    
    /* 解析 targetDeviceId（安卓APP控制多设备使用） */
    start = strstr(msg, "\"targetDeviceId\":\"");
    if(start)
    {
        start += strlen("\"targetDeviceId\":\"");
        end = strstr(start, "\"");
        if(end)
        {
            strncpy(mqtt_msg->target_device_id, start, end - start);
            mqtt_msg->target_device_id[end - start] = '\0';
        }
    }

    /* 解析 value */
    start = strstr(msg, "\"value\":\"");
    if(start)
    {
        start += strlen("\"value\":\"");
        end = strstr(start, "\"");
        if(end)
        {
            strncpy(mqtt_msg->value, start, end - start);
            mqtt_msg->value[end - start] = '\0';
        }
    }

    /* 未携带 targetDeviceId 时，默认使用 deviceId */
    if(mqtt_msg->target_device_id[0] == '\0')
    {
        strncpy(mqtt_msg->target_device_id, mqtt_msg->device_id, sizeof(mqtt_msg->target_device_id) - 1);
        mqtt_msg->target_device_id[sizeof(mqtt_msg->target_device_id) - 1] = '\0';
    }
    
    printf("解析MQTT消息: deviceId=%s, targetDeviceId=%s, action=%s, value=%s\r\n", 
           mqtt_msg->device_id, mqtt_msg->target_device_id, mqtt_msg->action, mqtt_msg->value);
    
    return true;
}

/**
  * @brief  处理MQTT消息
  * @param  mqtt_msg MQTT消息结构体
  * @retval 无
  */
void ESP8266_ProcessMQTTMessage(MQTT_Message_TypeDef *mqtt_msg)
{
    const char *target = mqtt_msg->target_device_id;

    /* 验证设备ID */
    if(strcmp(mqtt_msg->device_id, "STM32_MAIN") != 0)
    {
        printf("设备ID不匹配，忽略指令\r\n");
        return;
    }
    
    /* 处理不同的控制指令 */
    if(strcmp(mqtt_msg->action, "turn_on") == 0)
    {
        printf("收到turn_on指令, target=%s\r\n", target);

        if(strcmp(target, "FAN_SYS") == 0 || strcmp(target, "STM32_MAIN") == 0)
        {
            CONTROL_SetMode(CONTROL_MANUAL);
            CONTROL_OpenVent();
            g_fan_running = 1;
            ESP8266_SendDeviceStatusToMQTT("FAN_SYS", 1);
        }
        else if(strcmp(target, "DH_SYS") == 0)
        {
            g_dehumidifier_running = 1;
            ESP8266_SendDeviceStatusToMQTT("DH_SYS", 1);
        }
        else if(strcmp(target, "LIGHT_SYS") == 0)
        {
            LED_ON(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
            g_light_running = 1;
            ESP8266_SendDeviceStatusToMQTT("LIGHT_SYS", 1);
        }
        else if(strcmp(target, "PUMP_SYS") == 0)
        {
            g_pump_running = 1;
            ESP8266_SendDeviceStatusToMQTT("PUMP_SYS", 1);
        }
        else
        {
            printf("未识别targetDeviceId=%s，忽略\r\n", target);
        }

        ESP8266_SendSensorDataToMQTT();
    }
    else if(strcmp(mqtt_msg->action, "turn_off") == 0)
    {
        printf("收到turn_off指令, target=%s\r\n", target);

        if(strcmp(target, "FAN_SYS") == 0 || strcmp(target, "STM32_MAIN") == 0)
        {
            CONTROL_SetMode(CONTROL_MANUAL);
            CONTROL_CloseVent();
            g_fan_running = 0;
            ESP8266_SendDeviceStatusToMQTT("FAN_SYS", 0);
        }
        else if(strcmp(target, "DH_SYS") == 0)
        {
            g_dehumidifier_running = 0;
            ESP8266_SendDeviceStatusToMQTT("DH_SYS", 0);
        }
        else if(strcmp(target, "LIGHT_SYS") == 0)
        {
            LED_OFF(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
            g_light_running = 0;
            ESP8266_SendDeviceStatusToMQTT("LIGHT_SYS", 0);
        }
        else if(strcmp(target, "PUMP_SYS") == 0)
        {
            g_pump_running = 0;
            ESP8266_SendDeviceStatusToMQTT("PUMP_SYS", 0);
        }
        else
        {
            printf("未识别targetDeviceId=%s，忽略\r\n", target);
        }

        ESP8266_SendSensorDataToMQTT();
    }
    else if(strcmp(mqtt_msg->action, "auto_mode") == 0)
    {
        printf("收到auto_mode指令\r\n");
        /* 切换到自动模式 */
        CONTROL_SetMode(CONTROL_AUTO);
        CONTROL_SetAutoEnable(1);
        printf("已切换到自动模式\r\n");
        
        /* 回复状态 */
        ESP8266_SendSensorDataToMQTT();
    }
    else if(strcmp(mqtt_msg->action, "beep_on") == 0)
    {
        printf("收到beep_on指令\r\n");
        BEEP_ON(BEEP_GPIO_PORT, BEEP_GPIO_PIN, BEEP_HIGH_TRIGGER);
    }
    else if(strcmp(mqtt_msg->action, "beep_off") == 0)
    {
        printf("收到beep_off指令\r\n");
        BEEP_OFF(BEEP_GPIO_PORT, BEEP_GPIO_PIN, BEEP_HIGH_TRIGGER);
    }
    else if(strcmp(mqtt_msg->action, "led_on") == 0)
    {
        printf("收到led_on指令\r\n");
        LED_ON(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
    }
    else if(strcmp(mqtt_msg->action, "led_off") == 0)
    {
        printf("收到led_off指令\r\n");
        LED_OFF(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
    }
    else if(strcmp(mqtt_msg->action, "servo_left") == 0)
    {
        printf("收到servo_left指令\r\n");
        CONTROL_SetMode(CONTROL_MANUAL);
        CONTROL_SetVentAngle(120);
    }
    else if(strcmp(mqtt_msg->action, "servo_right") == 0)
    {
        printf("收到servo_right指令\r\n");
        CONTROL_SetMode(CONTROL_MANUAL);
        CONTROL_SetVentAngle(30);
    }
    else if(strcmp(mqtt_msg->action, "servo_set") == 0)
    {
        uint16_t angle = (uint16_t)atoi(mqtt_msg->value);
        printf("收到servo_set指令 target=%s angle=%d\r\n", target, angle);
        if(strcmp(target, "FAN_SYS") == 0 || strcmp(target, "STM32_MAIN") == 0)
        {
            CONTROL_SetMode(CONTROL_MANUAL);
            CONTROL_SetVentAngle(angle);
            g_fan_running = (angle > 0) ? 1 : 0;
            ESP8266_SendDeviceStatusToMQTT("FAN_SYS", g_fan_running);
        }
    }
    else if(strcmp(mqtt_msg->action, "query_status") == 0)
    {
        printf("收到query_status指令\r\n");
        /* 查询状态 */
        ESP8266_SendSensorDataToMQTT();
    }
    else if(strcmp(mqtt_msg->action, "config_threshold") == 0)
    {
        char *p;
        int value_i = 0;
        printf("收到config_threshold指令: %s\r\n", mqtt_msg->value);

        p = strstr(mqtt_msg->value, "water_limit=");
        if (p) {
            value_i = atoi(p + 12);
            if (value_i < 0) value_i = 0;
            if (value_i > 100) value_i = 100;
            g_water_limit_threshold = (uint16_t)value_i;
        }

        p = strstr(mqtt_msg->value, "water_recover=");
        if (p) {
            value_i = atoi(p + 14);
            if (value_i < 0) value_i = 0;
            if (value_i > 100) value_i = 100;
            g_water_recover_threshold = (uint16_t)value_i;
        }

        p = strstr(mqtt_msg->value, "auto_mode=");
        if (p) {
            value_i = atoi(p + 10);
            if (value_i == 1) {
                CONTROL_SetMode(CONTROL_AUTO);
                CONTROL_SetAutoEnable(1);
            } else {
                CONTROL_SetMode(CONTROL_MANUAL);
                CONTROL_SetAutoEnable(0);
            }
        }

        p = strstr(mqtt_msg->value, "sensitivity=");
        if (p) {
            const char *sens = p + 12;
            if (strstr(sens, "low") == sens) {
                CONTROL_SetShakeThreshold(3);
            } else if (strstr(sens, "high") == sens) {
                CONTROL_SetShakeThreshold(1);
            } else {
                CONTROL_SetShakeThreshold(2);
            }
        }

        printf("阈值更新完成: limit=%d recover=%d\r\n",
               g_water_limit_threshold, g_water_recover_threshold);
        ESP8266_SendSensorDataToMQTT();
    }
    else
    {
        printf("未知的指令: %s\r\n", mqtt_msg->action);
    }
}

/**
  * @brief  检查并处理MQTT消息
  * @param  无
  * @retval 1 收到并处理了MQTT消息，0 没有收到MQTT消息
  */
uint8_t ESP8266_CheckMQTTMessage(void)
{
    MQTT_Message_TypeDef mqtt_msg;
    
    /* 检查是否有数据收到 */
    if(esp8266_receive.read_flag == 0)
    {
        return 0;
    }
    
    /* 检查是否是MQTT订阅消息 */
    if(strstr((char*)esp8266_receive.buffer, "+MQTTSUB") != NULL)
    {
        printf("收到MQTT消息: %s\r\n", (char*)esp8266_receive.buffer);
        
        /* 解析MQTT消息 */
        if(ESP8266_ParseMQTTMessage((char*)esp8266_receive.buffer, &mqtt_msg))
        {
            /* 处理MQTT消息 */
            ESP8266_ProcessMQTTMessage(&mqtt_msg);
        }
        
        /* 清除接收标志 */
        esp8266_receive.read_flag = 0;
        esp8266_receive.len = 0;
        esp8266_receive.Received_data_completed_flag = 0;
        
        return 1;
    }
    
    /* 清除接收标志 */
    esp8266_receive.read_flag = 0;
    esp8266_receive.len = 0;
    esp8266_receive.Received_data_completed_flag = 0;
    
    return 0;
}

/*****************************END OF FILE***************************************/





















