/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "esp8266/bsp_esp8266.h"
#include "usart/usart_com.h"
#include "dwt/bsp_dwt.h"
#include "common/common.h"
#include "systick/bsp_systick.h"

#define ESP8266_FW_MARKER "FW-20260510-MQTT-ATFIX2"

ESP8266_DataTypeDef esp8266_receive = {0};
static uint8_t esp8266_async_message_buffer[ESP8266_BUFFER_SIZE] = {0};
static uint32_t esp8266_async_message_len = 0U;
static uint8_t esp8266_async_message_ready = 0U;

static uint8_t ESP8266_ShouldCaptureAsyncMessage(const char *message)
{
  if ((message == NULL) || (*message == '\0'))
  {
    return 0U;
  }

  return (strstr(message, "+MQTTSUBRECV:") != NULL) ||
         (strstr(message, "+MQTTDISCONNECTED:") != NULL) ||
         (strstr(message, "+MQTTCONNECTED:") != NULL);
}

static void ESP8266_CaptureAsyncMessageFromRx(void)
{
  uint32_t available_len = 0U;
  uint32_t copy_len = 0U;
  uint32_t offset = 0U;

  if (esp8266_receive.len == 0U)
  {
    return;
  }

  if (esp8266_receive.len >= ESP8266_BUFFER_SIZE)
  {
    esp8266_receive.buffer[ESP8266_BUFFER_SIZE - 1U] = '\0';
    copy_len = ESP8266_BUFFER_SIZE - 1U;
  }
  else
  {
    esp8266_receive.buffer[esp8266_receive.len] = '\0';
    copy_len = esp8266_receive.len;
  }

  if (ESP8266_ShouldCaptureAsyncMessage((char *)esp8266_receive.buffer) == 0U)
  {
    return;
  }

  if (esp8266_async_message_ready != 0U)
  {
    offset = esp8266_async_message_len;
    if ((offset > 0U) && (offset < (ESP8266_BUFFER_SIZE - 2U)))
    {
      esp8266_async_message_buffer[offset++] = '\n';
    }
  }
  else
  {
    memset(esp8266_async_message_buffer, 0, sizeof(esp8266_async_message_buffer));
  }

  available_len = (ESP8266_BUFFER_SIZE - 1U) - offset;
  if (copy_len > available_len)
  {
    copy_len = available_len;
  }

  memcpy(&esp8266_async_message_buffer[offset], esp8266_receive.buffer, copy_len);
  esp8266_async_message_buffer[offset + copy_len] = '\0';
  esp8266_async_message_len = offset + copy_len;
  esp8266_async_message_ready = 1U;
}

static uint8_t ESP8266_ResponseIndicatesAlive(void)
{
  if (esp8266_receive.len == 0U)
  {
    return 0U;
  }

  return (strstr((char *)esp8266_receive.buffer, "WIFI ") != NULL) ||
         (strstr((char *)esp8266_receive.buffer, "ready") != NULL) ||
         (strstr((char *)esp8266_receive.buffer, "OK") != NULL) ||
         (strstr((char *)esp8266_receive.buffer, "ERROR") != NULL) ||
         (strstr((char *)esp8266_receive.buffer, "busy") != NULL);
}

static uint8_t ESP8266_ResponseContains(const char *token)
{
  if ((token == NULL) || (esp8266_receive.len == 0U))
  {
    return 0U;
  }

  return (strstr((char *)esp8266_receive.buffer, token) != NULL) ? 1U : 0U;
}

static void ESP8266_ClearReceiveBuffer(void)
{
  ESP8266_CaptureAsyncMessageFromRx();
  memset(esp8266_receive.buffer, 0, sizeof(esp8266_receive.buffer));
  esp8266_receive.len = 0;
  esp8266_receive.read_flag = 0;
  esp8266_receive.Received_data_completed_flag = 0;
}

void ESP8266_PreserveAsyncMessage(void)
{
  ESP8266_CaptureAsyncMessageFromRx();
}

uint8_t ESP8266_ReadAsyncMessage(uint8_t *buffer, uint32_t buffer_size)
{
  uint32_t copy_len = 0U;

  if ((buffer == NULL) || (buffer_size == 0U) || (esp8266_async_message_ready == 0U))
  {
    return 0U;
  }

  copy_len = esp8266_async_message_len;
  if (copy_len >= buffer_size)
  {
    copy_len = buffer_size - 1U;
  }

  memcpy(buffer, esp8266_async_message_buffer, copy_len);
  buffer[copy_len] = '\0';

  memset(esp8266_async_message_buffer, 0, sizeof(esp8266_async_message_buffer));
  esp8266_async_message_len = 0U;
  esp8266_async_message_ready = 0U;
  return 1U;
}

static void ESP8266_PrintRawResponse(const char *tag)
{
  uint32_t index = 0;

  printf("\r\n[%s] RX len: %lu\r\n", tag, (unsigned long)esp8266_receive.len);

  if (esp8266_receive.len == 0)
  {
    printf("[%s] no uart data\r\n", tag);
    return;
  }

  printf("[%s] ASCII: ", tag);
  for (index = 0; index < esp8266_receive.len; index++)
  {
    uint8_t data = esp8266_receive.buffer[index];

    if ((data >= 0x20) && (data <= 0x7E))
    {
      printf("%c", data);
    }
    else if (data == '\r')
    {
      printf("<CR>");
    }
    else if (data == '\n')
    {
      printf("<LF>");
    }
    else
    {
      printf("<0x%02X>", data);
    }
  }
  printf("\r\n");

  printf("[%s] HEX:", tag);
  for (index = 0; index < esp8266_receive.len; index++)
  {
    printf(" %02X", esp8266_receive.buffer[index]);
  }
  printf("\r\n");
}



void ESP8266_GPIO_PinConfig(void)
{
    
    
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 1  
    
    
    RCC_APB2PeriphClockCmd(ESP8266_RST_GPIO_CLK_PORT,ENABLE);

    /* IO输出状态初始化控制 */
    GPIO_SetBits(ESP8266_RST_GPIO_PORT,ESP8266_RST_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Pin    = ESP8266_RST_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(ESP8266_RST_GPIO_PORT,&gpio_initstruct); 
    
#endif 
    
#if 1    
    
    
    RCC_APB2PeriphClockCmd(ESP8266_IO_GPIO_CLK_PORT,ENABLE);

    /* IO输出状态初始化控制 */
    GPIO_SetBits(ESP8266_IO_GPIO_PORT,ESP8266_IO_GPIO_PIN);
    
    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_Out_PP;
    gpio_initstruct.GPIO_Pin    = ESP8266_IO_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(ESP8266_IO_GPIO_PORT,&gpio_initstruct); 
    
#endif  
}


void ESP8266_NVIC_Config(void)
{
    
    NVIC_InitTypeDef nvic_initstruct = {0};
    
    
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO,ENABLE); 
      
    
    nvic_initstruct.NVIC_IRQChannel                     = ESP8266_IRQ;
    
    nvic_initstruct.NVIC_IRQChannelPreemptionPriority   =  1;
    /* 閰嶇疆瀛愪紭鍏堢骇 */
    nvic_initstruct.NVIC_IRQChannelSubPriority          =  0;
    /* 浣胯兘閰嶇疆涓柇閫氶亾 */
    nvic_initstruct.NVIC_IRQChannelCmd                  =  ENABLE;

    NVIC_Init(&nvic_initstruct);
    
}


void ESP8266_USART_PinConfig(void)
{
    
    
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 1  
    
    
    RCC_APB2PeriphClockCmd(ESP8266_TX_GPIO_CLK_PORT,ENABLE);

    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_PP;
    gpio_initstruct.GPIO_Pin    = ESP8266_TX_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(ESP8266_TX_GPIO_PORT,&gpio_initstruct); 
    
#endif 
    
#if 1    
    
    
    RCC_APB2PeriphClockCmd(ESP8266_RX_GPIO_CLK_PORT,ENABLE);

    
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_IPU;
    gpio_initstruct.GPIO_Pin    = ESP8266_RX_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(ESP8266_RX_GPIO_PORT,&gpio_initstruct); 
    
#endif  
}


void ESP8266_USART_ModeConfig(void)
{
  
    
    USART_InitTypeDef usart_initstruct = {0};
    
    
    ESP8266_APBXCLKCMD(ESP8266_USARTX_CLK_PORT,ENABLE);

    
    usart_initstruct.USART_BaudRate                 =  ESP8266_BAUDRATE;                  
    usart_initstruct.USART_HardwareFlowControl      =  USART_HardwareFlowControl_None;  
    usart_initstruct.USART_Mode                     =  USART_Mode_Tx|USART_Mode_Rx;     //閰嶇疆宸ヤ綔妯″紡
    usart_initstruct.USART_Parity                   =  USART_Parity_No;                 
    usart_initstruct.USART_StopBits                 =  USART_StopBits_1;                
    usart_initstruct.USART_WordLength               =  USART_WordLength_8b;             
    
    USART_Init(ESP8266_USARTX, &usart_initstruct);
    
    USART_ITConfig(ESP8266_USARTX,USART_IT_RXNE,ENABLE);
    USART_ITConfig(ESP8266_USARTX,USART_IT_IDLE,ENABLE);
    
}


void ESP8266_USART_Init(void)
{
    /* 閰嶇疆 ESP8266 涓插彛涓柇閰嶇疆 */
    ESP8266_NVIC_Config();
    
    /* 閰嶇疆 USARTX 妯″紡 */
    ESP8266_USART_ModeConfig();

    
    ESP8266_USART_PinConfig();
    
    /* 浣胯兘涓插彛 */
    USART_Cmd(ESP8266_USARTX,ENABLE);

}




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
        if(esp8266_receive.len == ESP8266_BUFFER_SIZE-1)                    
        {
            esp8266_receive.buffer[esp8266_receive.len] = '\0';             
            esp8266_receive.Received_data_completed_flag = 1;                                
        }
        USART_ClearITPendingBit(ESP8266_USARTX,USART_IT_RXNE);            
    }
    if(USART_GetITStatus(ESP8266_USARTX, USART_IT_IDLE) == SET)  
    {
        USART_ReceiveData(ESP8266_USARTX);                                //鐗瑰埆鎻愮ず锛屾牴鎹墜鍐屽疄闄呮弿杩帮紙璇诲彇鑾峰彇USART_IT_IDLE銆佹暟鎹瘎瀛樺櫒鐨勬暟鎹紝璇诲彇鍚庡搴旂殑瀵勫瓨鍣ㄤ細琚浣嶏級锛岀敤杩欐牱鏂瑰紡娓呴櫎IDLE
        esp8266_receive.buffer[esp8266_receive.len] = '\0';                 
        esp8266_receive.read_flag = 1;                                    
    }
    
}


void ESP8266_Init( void )
{
    
    ESP8266_GPIO_PinConfig();
    
    
    ESP8266_USART_Init();

    /* 复位ESP8266 */
    ESP8266_Rst();
}


void ESP8266_Rst( void )
{
  
  GPIO_SetBits(ESP8266_IO_GPIO_PORT,ESP8266_IO_GPIO_PIN);
  GPIO_ResetBits(ESP8266_RST_GPIO_PORT,ESP8266_RST_GPIO_PIN);
  DWT_DelayMs(500);
  GPIO_SetBits(ESP8266_RST_GPIO_PORT,ESP8266_RST_GPIO_PIN);
  DWT_DelayMs(500);
    ESP8266_ClearReceiveBuffer();

}


bool ESP8266_Cmd ( char * cmd, char * reply1, char * reply2, u32 waittime )
{    
//    strEsp8266_Fram_Record .InfBit .FramLength = 0;               //从新开始接收新的数据包

  ESP8266_ClearReceiveBuffer();

    USART_printf ( ESP8266_USARTX,"%s\r\n", cmd );          
    
    if ( ( reply1 == 0 ) && ( reply2 == 0 ) )                      
        return true;
    
    DWT_DelayMs ( waittime );                 //寤舵椂
    
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


bool ESP8266_AT_Test ( void )
{
	static uint8_t count = 0;
	static uint64_t next_retry_tick = 0;
	bool at_test_ok = false;
	uint64_t now_tick = SysTick_GetCount();

    if(now_tick < next_retry_tick)
    {
        return 0;
    }

    if(count == 0)
    {
  	    printf("\r\nAT test at %lu baud... [diag:MQTT-AT-REF][%s]\r\n",
  	           (unsigned long)ESP8266_BAUDRATE,
  	           ESP8266_FW_MARKER);
    }

    printf("\r\nAT try %d at %lu baud...\r\n", count, (unsigned long)ESP8266_BAUDRATE);
    at_test_ok = ESP8266_Cmd ( "AT", "OK", NULL, 800 );
    ESP8266_PrintRawResponse("AT");

    if(at_test_ok)
    {
      printf("\r\nAT ok on try %d at %lu baud\r\n", count, (unsigned long)ESP8266_BAUDRATE);
        count = 0;
        next_retry_tick = 0;
        return 1;
    }

      if ((ESP8266_ResponseContains("ready") != 0U) ||
        (ESP8266_ResponseContains("WIFI CONNECTED") != 0U) ||
        (ESP8266_ResponseContains("WIFI GOT IP") != 0U))
      {
        printf("\r\n[AT] module is still booting or joining Wi-Fi, wait 1500ms before retry\r\n");
        count = 0;
        next_retry_tick = SysTick_GetCount() + 1500;
        return 0;
      }

      if (ESP8266_ResponseContains("busy") != 0U)
      {
        printf("\r\n[AT] module returned busy, retry after 1000ms\r\n");
        next_retry_tick = SysTick_GetCount() + 1000;
        return 0;
      }

    if (ESP8266_ResponseIndicatesAlive() != 0U)
    {
       printf("\r\n[AT] module responded at %lu baud but did not return OK, retry after 500ms\r\n",
         (unsigned long)ESP8266_BAUDRATE);
        next_retry_tick = SysTick_GetCount() + 500;
        count++;
        return 0;
    }

    count++;

    if(count >= 10)
    {
        printf("\r\nAT failed 10 times at %lu baud, use RST-only reset and retry\r\n",
         (unsigned long)ESP8266_BAUDRATE);
        ESP8266_Rst();
        count = 0;
        next_retry_tick = SysTick_GetCount() + 2000;
    }
    else
    {
        next_retry_tick = SysTick_GetCount() + 300;
    }

    return 0;
}


bool ESP8266_DHCP_CUR ( void )
{
	char cCmd [40];

	sprintf ( cCmd, "AT+CWDHCP=1,1");
	
	return ESP8266_Cmd ( cCmd, "OK", NULL, 500 );
	
}


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


bool ESP8266_JoinAP ( char * pSSID, char * pPassWord )
{
	char cCmd [120];

	sprintf ( cCmd, "AT+CWJAP=\"%s\",\"%s\"", pSSID, pPassWord );
	
	return ESP8266_Cmd ( cCmd, "OK", NULL, 5000 );
	
}


bool ESP8266_BuildAP ( char * pSSID, char * pPassWord, ENUM_AP_PsdMode_TypeDef enunPsdMode )
{
	char cCmd [120];

	sprintf ( cCmd, "AT+CWSAP=\"%s\",\"%s\",1,%d", pSSID, pPassWord, enunPsdMode );
	
	return ESP8266_Cmd ( cCmd, "OK", 0, 1000 );
	
}


bool ESP8266_Enable_MultipleId ( FunctionalState enumEnUnvarnishTx )
{
	char cStr [20];
	
	sprintf ( cStr, "AT+CIPMUX=%d", ( enumEnUnvarnishTx ? 1 : 0 ) );
	
	return ESP8266_Cmd ( cStr, "OK", 0, 500 );
	
}


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
 * @brief  Configure the ESP8266 to enter transparent transmission mode.
 * @param  None
 * @retval 1 when configuration succeeds, otherwise 0.
 */
bool ESP8266_UnvarnishSend ( void )
{
	if ( ! ESP8266_Cmd ( "AT+CIPMODE=1", "OK", 0, 500 ) )
		return false;
	
	return 
	  ESP8266_Cmd ( "AT+CIPSEND", "OK", ">", 500 );
	
}

/**
 * @brief  Exit ESP8266 transparent transmission mode.
 * @param  None
 * @retval None
 */
void ESP8266_ExitUnvarnishSend ( void )
{
	DWT_DelayMs ( 1000 );
	
	USART_printf ( ESP8266_USARTX,"+++" );
    
	DWT_DelayMs ( 500 ); 
	
}

/**
 * @brief  Send a string through the ESP8266 module.
 * @param  enumEnUnvarnishTx Whether transparent mode is already enabled.
 * @param  pStr Pointer to the string buffer to send.
 * @param  ulStrLength String length in bytes.
 * @param  ucId Target connection ID.
 * @retval 1 when sending succeeds, otherwise 0.
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
 * @brief  Send a raw byte buffer through the ESP8266 module.
 * @param  pBuffer Pointer to the buffer to send.
 * @param  ulLength Buffer length in bytes.
 * @param  waittime Time to wait for SEND OK or an HTTP response.
 * @retval 1 when sending succeeds, otherwise 0.
 */
bool ESP8266_SendBuffer ( const uint8_t *pBuffer, u32 ulLength, u32 waittime )
{
  char cStr [20];

  if ((pBuffer == NULL) || (ulLength == 0U))
  {
    return false;
  }

  sprintf(cStr, "AT+CIPSEND=%lu", (unsigned long)ulLength);
  if (ESP8266_Cmd(cStr, ">", "> ", 500) == false)
  {
    return false;
  }

  ESP8266_ClearReceiveBuffer();
  USARTX_SendArray(ESP8266_USARTX, (uint8_t *)pBuffer, ulLength);
  DWT_DelayMs(waittime);

  while(esp8266_receive.Received_data_completed_flag == 1);
  if (esp8266_receive.len >= ESP8266_BUFFER_SIZE)
  {
    esp8266_receive.buffer[ESP8266_BUFFER_SIZE - 1U] = '\0';
  }
  else
  {
    esp8266_receive.buffer[esp8266_receive.len] = '\0';
  }

  if (strstr((char *)esp8266_receive.buffer, "SEND OK") != NULL)
  {
    return true;
  }

  if ((strstr((char *)esp8266_receive.buffer, "HTTP/1.1 200") != NULL) ||
    (strstr((char *)esp8266_receive.buffer, "HTTP/1.1 204") != NULL))
  {
    return true;
  }

  return false;
}

/**
 * @brief  Receive a string from the ESP8266 module.
 * @param  enumEnUnvarnishTx Whether transparent mode is already enabled.
 * @retval Pointer to the received string buffer.
 */
char * ESP8266_ReceiveString ( FunctionalState enumEnUnvarnishTx )
{
	char * pRecStr = 0;
//	strEsp8266_Fram_Record .InfBit .FramLength = 0;
//	strEsp8266_Fram_Record .InfBit .FramFinishFlag = 0;
//	
//	while ( ! strEsp8266_Fram_Record .InfBit .FramFinishFlag );
//	esp8266_receive.buffer [ strEsp8266_Fram_Record .InfBit .FramLength ] = '\0';
    
	while(esp8266_receive.Received_data_completed_flag == 1);       //绛夊緟鎺ユ敹瀹屾垚
    
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

