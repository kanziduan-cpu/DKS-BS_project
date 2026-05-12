/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "debug/app_debug.h"
#include "debug/bsp_debug.h"
#include "usart/usart_com.h"
#include <string.h>
#include "esp8266/bsp_esp8266.h"
#include "esp8266/app_esp8266.h"


void Debug_ReadBufferReset(void)
{
    memset(debug_receive.buffer,NULL,debug_receive.len);
    debug_receive.len = 0;
    debug_receive.read_flag = 0;
}


void Debug_TaskInit(void)
{
    Debug_ReadBufferReset();
}


void Debug_Task(void)
{
    if(debug_receive.read_flag)
    {
    char local_command[DEBUG_BUFFER_SIZE] = {0};
    uint8_t handled = 0;

    memcpy(local_command, debug_receive.buffer, debug_receive.len);
    handled = Get_ESP82666_Cmd(local_command);

    if(handled == 0)
    {
      //转发给esp8266
      USARTX_SendArray(ESP8266_USARTX,debug_receive.buffer,debug_receive.len);
    }
        
        Debug_ReadBufferReset();
    }
}


/*****************************END OF FILE***************************************/
