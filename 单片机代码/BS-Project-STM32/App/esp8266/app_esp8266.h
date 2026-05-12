/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#ifndef __APP_ESP8266_H
#define __APP_ESP8266_H

#include "stm32f10x.h"

/********************************** 用户需要设置的参数**********************************/
#define      macUser_ESP8266_ApSsid                       "dks"                       
#define      macUser_ESP8266_ApPwd                        "88888888"                  

#define      macUser_ESP8266_DeviceId                     "STM32_01"
#define      macUser_ESP8266_MachineCode                  "NODE001"

#define      macUser_ESP8266_MqttHost                     "47.83.152.62"
#define      macUser_ESP8266_MqttPort                     "1883"
#define      macUser_ESP8266_MqttClientId                 "STM32_01"
#define      macUser_ESP8266_MqttUsername                 ""
#define      macUser_ESP8266_MqttPassword                 ""

#define      macUser_ESP8266_MqttStatusTopic              "device/STM32_01/status"
#define      macUser_ESP8266_MqttControlTopic             "device/STM32_01/control"
#define      macUser_ESP8266_MqttControlResponseTopic     "device/STM32_01/control/response"
#define      macUser_ESP8266_MqttAlarmTopic               "device/STM32_01/alarm"

#define      ESP8266_MQTT_PUBLISH_PERIOD_MS               5000U
#define      ESP8266_MQTT_SUBSCRIBE_QOS                   0U
#define      ESP8266_MQTT_PUBLISH_QOS                     0U

extern uint8_t esp8266_configuration_completed_flag;//ESP8266配置完毕标志

void ESP8266_ReadBufferReset(void);
void ESP8266_ResetConnectionState(void);
void ESP8266_TaskInit(void);
void ESP8266_Task(void);
void ESP8266_StaTcpClient_Unvarnish_ConfigTest(void);
void ESP8266_CloudTask(void);
uint8_t Get_ESP82666_Cmd(char * cmd);
void ESP8266_SendDHT11DataTest(void);
void ESP8266_SendMQ135DataTest(void);

#endif /* __APP_ESP8266_H */

