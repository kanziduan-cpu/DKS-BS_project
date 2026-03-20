/**
  ******************************************************************************
  * @file       bsp_esp8266.c
  * @author     embedfire
  * @version     V1.0
  * @date        2025
  * @brief      esp8266 函数接口
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
  
#include "esp8266/bsp_esp8266.h"
#include "usart/usart_com.h"
#include "dwt/bsp_dwt.h"
#include "common/common.h"

ESP8266_DataTypeDef esp8266_receive = {0};


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

/*****************************END OF FILE***************************************/

