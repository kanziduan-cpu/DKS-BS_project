#ifndef __APP_ESP8266_H
#define __APP_ESP8266_H

#include "stm32f10x.h"//或#include "stdint.h"

/********************************** 用户需要设置的参数**********************************/
#define      macUser_ESP8266_ApSsid                       "wifiname"         //要连接的热点的名称
#define      macUser_ESP8266_ApPwd                        "123456789"           //要连接的热点的密钥

#define      macUser_ESP8266_TcpServer_IP                 "192.168.103.19"     //要连接的服务器的 IP
#define      macUser_ESP8266_TcpServer_Port               "8000"               //要连接的服务器的端口

extern uint8_t esp8266_configuration_completed_flag;//ESP8266配置完毕标志

void ESP8266_ReadBufferReset(void);
void ESP8266_TaskInit(void);
void ESP8266_Task(void);
void ESP8266_StaTcpClient_Unvarnish_ConfigTest(void);
void Get_ESP82666_Cmd( char * cmd);
    
#endif /* __APP_ESP8266_H */
