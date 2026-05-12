/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "esp8266/app_esp8266.h"
#include "esp8266/bsp_esp8266.h"
#include "usart/usart_com.h"
#include <string.h>
#include <stdlib.h>
#include "debug/bsp_debug.h"
#include "dht11/app_dht11.h"
#include "mq135/app_mq135.h"
#include "beep/app_beep.h"
#include "servo/app_servo.h"
#include "led/app_led.h"
#include "led/bsp_gpio_led.h"
#include <ctype.h>
#include <math.h>
#include "adc/bsp_adc.h"
#include "dwt/bsp_dwt.h"
#include "mpu6050/app_mpu6050.h"
#include "systick/bsp_systick.h"

uint8_t esp8266_configuration_completed_flag = 0;//ESP8266配置完毕标志

#define CONTROL_CMD_NUMBER 8
#define RUN_LED_DEFAULT_CYCLE 500U
#define WINDOW_SERVO_OPEN_ANGLE 60U
#define WINDOW_SERVO_CLOSE_ANGLE 0U
#define RAIN_DETECTED_THRESHOLD_ADC 0x1FU
#define WATER_HIGH_ALARM_THRESHOLD 60U
#define MQTT_SENSOR_PAYLOAD_BUFFER_SIZE 512U
#define MQTT_AT_COMMAND_BUFFER_SIZE 1024U
#define MQTT_COMMAND_BUFFER_SIZE 256U
#define MQTT_RESPONSE_WAIT_MS 800U
#define MQTT_PREPARE_RESPONSE_WAIT_MS 300U
#define MQTT_PUBLISH_POLL_INTERVAL_MS 10U
#define ESP8266_PENDING_MESSAGE_BATCH_LIMIT 6U
#define ESP8266_DUPLICATE_PAYLOAD_WINDOW_MS 800U
#define ESP8266_RX_QUIET_WINDOW_MS 800U
#define ESP8266_DEBUG_MONITOR_PERIOD_MS 1000U

static const char *control_cmds[CONTROL_CMD_NUMBER] =
{
    "G_LED_ON", "G_LED_OFF", "B_LED_ON", "B_LED_OFF",
    "BEEP_ON", "BEEP_OFF", "SERVO_LEFT", "SERVO_RIGHT"
};

static char esp8266_normalized_command_buffer[ESP8266_BUFFER_SIZE] = {0};
static uint8_t esp8266_received_snapshot[ESP8266_BUFFER_SIZE] = {0};
static char esp8266_last_control_payload[MQTT_COMMAND_BUFFER_SIZE] = {0};
static char esp8266_mqtt_payload_buffer[MQTT_SENSOR_PAYLOAD_BUFFER_SIZE] = {0};
static char esp8266_mqtt_command_buffer[MQTT_AT_COMMAND_BUFFER_SIZE] = {0};
static uint8_t esp8266_mqtt_step = 0;
static uint64_t esp8266_last_publish_tick = 0;
static uint64_t esp8266_last_rx_activity_tick = 0U;
static uint64_t esp8266_last_monitor_print_tick = 0;
static uint64_t esp8266_last_control_payload_tick = 0U;
static uint8_t esp8266_rain_high_alarm_latched = 0;
static uint8_t esp8266_rain_high_alarm_report_pending = 0;

static char *ESP8266_TrimCommand(char *command);
static uint8_t ESP8266_LogControlApplied(const char *command);
static uint8_t ESP8266_LogControlIgnored(const char *reason, const char *command);
static uint8_t ESP8266_LogControlUnsupported(const char *command);
static void ESP8266_ProcessReceivedMessage(const char *message);
static uint8_t ESP8266_HandleSingleIncomingPayload(const char *payload);
static uint8_t ESP8266_ExtractSingleSubPayload(const char *message, char *payload, size_t payload_size, size_t *consumed_length);
static uint8_t ESP8266_IsDuplicateControlPayload(const char *payload);
static uint8_t ESP8266_TakeReceivedSnapshot(uint8_t *buffer, uint32_t buffer_size);
static uint8_t ESP8266_DrainPendingMessages(uint8_t max_batches);
static uint8_t ESP8266_ShouldDeferPublish(uint64_t now_tick, uint8_t handled_batches);
static uint8_t ESP8266_MQTT_WaitPublishResult(uint32_t timeout_ms);
static uint8_t ESP8266_IsAnyAlarmLatched(void);
static uint8_t ESP8266_HasAlarmReportPending(void);
static void ESP8266_ClearAllAlarmState(void);
static void ESP8266_CheckRainHighAlarm(void);

static uint8_t ESP8266_IsLowLevelActive(GPIO_TypeDef* port, uint16_t pin)
{
    return (GPIO_ReadOutputDataBit(port, pin) == 0U) ? 1U : 0U;
}

static float ESP8266_GetDhtTemperature(void)
{
    return (float)dht11_data.temp_int + ((float)dht11_data.temp_deci / 10.0f);
}

static float ESP8266_GetDhtHumidity(void)
{
    return (float)dht11_data.humi_int + ((float)dht11_data.humi_deci / 10.0f);
}

static float ESP8266_GetWaterLevel(void)
{
    return ((float)adc_source_convertedvalue[ADCX_RAIN_BUFFER_INDEX] * 100.0f) / 4095.0f;
}

static uint32_t ESP8266_GetWaterLevelPercent(void)
{
    return (uint32_t)(ESP8266_GetWaterLevel() + 0.5f);
}

static uint32_t ESP8266_GetRainDetected(void)
{
    return (adc_source_convertedvalue[ADCX_RAIN_BUFFER_INDEX] > RAIN_DETECTED_THRESHOLD_ADC) ? 1U : 0U;
}

static int ESP8266_GetAirQuality(void)
{
    if (mq135_task.ppm <= 0.0f)
    {
        return 0;
    }

    return (int)(mq135_task.ppm + 0.5f);
}

static float ESP8266_GetTiltAngle(void)
{
    float tilt_x = fabsf(mpu6050_task.Tilt[0]);
    float tilt_y = fabsf(mpu6050_task.Tilt[1]);

    return (tilt_x > tilt_y) ? tilt_x : tilt_y;
}

static void ESP8266_FormatFixed1(char *buffer, size_t buffer_size, float value)
{
    long scaled_value = 0;
    unsigned long abs_scaled_value = 0UL;

    if ((buffer == NULL) || (buffer_size == 0U))
    {
        return;
    }

    scaled_value = (value >= 0.0f) ? (long)(value * 10.0f + 0.5f) : (long)(value * 10.0f - 0.5f);
    abs_scaled_value = (unsigned long)((scaled_value < 0L) ? -scaled_value : scaled_value);

    snprintf(buffer,
             buffer_size,
             "%s%lu.%01lu",
             (scaled_value < 0L) ? "-" : "",
             abs_scaled_value / 10UL,
             abs_scaled_value % 10UL);
}

static const char *ESP8266_GetServoState(void)
{
    if ((servo1_task.turn_left_flag != 0U) ||
        (servo1_task.turn_right_flag != 0U) ||
        (servo2_task.turn_left_flag != 0U) ||
        (servo2_task.turn_right_flag != 0U))
    {
        return "moving";
    }

    return "idle";
}

static uint8_t ESP8266_IsAnyAlarmLatched(void)
{
    return ((MPU6050_IsAlarmLatched() != 0U) || (esp8266_rain_high_alarm_latched != 0U)) ? 1U : 0U;
}

static uint8_t ESP8266_HasAlarmReportPending(void)
{
    return ((MPU6050_IsAlarmReportPending() != 0U) || (esp8266_rain_high_alarm_report_pending != 0U)) ? 1U : 0U;
}

static uint8_t ESP8266_ShouldDebugPrintMessage(const char *message)
{
    if ((message == NULL) || (*message == '\0'))
    {
        return 0;
    }

    return (strstr(message, "WIFI ") != NULL) ||
           (strstr(message, "MQTT") != NULL) ||
           (strstr(message, "+MQTTSUBRECV:") != NULL) ||
           (strstr(message, "ERROR") != NULL) ||
           (strstr(message, "FAIL") != NULL) ||
           (strstr(message, "busy") != NULL) ||
           (strstr(message, "CONNECT") != NULL) ||
           (strstr(message, "CLOSED") != NULL);
}

static void ESP8266_DebugPrintMonitorData(void)
{
    uint64_t now_tick = SysTick_GetCount();
    uint32_t water_level = ESP8266_GetWaterLevelPercent();
    uint32_t rain_detected = ESP8266_GetRainDetected();
    uint32_t rain_alarm_latched = (esp8266_rain_high_alarm_latched != 0U) ? 1U : 0U;
    uint32_t buzzer_active = (beep_task.beep_active || ESP8266_IsAnyAlarmLatched()) ? 1U : 0U;
    uint32_t green_led_on = ESP8266_IsLowLevelActive(G_LED_GPIO_PORT, G_LED_GPIO_PIN);
    uint32_t blue_led_on = ESP8266_IsLowLevelActive(B_LED_GPIO_PORT, B_LED_GPIO_PIN);
    uint32_t air_quality = (uint32_t)ESP8266_GetAirQuality();
    uint32_t tilt_tenths = (uint32_t)(ESP8266_GetTiltAngle() * 10.0f + 0.5f);

    if ((esp8266_last_monitor_print_tick != 0U) &&
        ((now_tick - esp8266_last_monitor_print_tick) < ESP8266_DEBUG_MONITOR_PERIOD_MS))
    {
        return;
    }

    esp8266_last_monitor_print_tick = now_tick;

    printf("\r\n[MON] step=%u cfg=%u temp=%u.%u hum=%u.%u air=%lu raw=%u water=%lu rain=%lu rain_alarm=%lu tilt=%lu.%lu vib=%u servo1=%u servo2=%u state=%s buzzer=%lu ledg=%lu ledb=%lu\r\n",
           (unsigned int)esp8266_mqtt_step,
           (unsigned int)esp8266_configuration_completed_flag,
           (unsigned int)dht11_data.temp_int,
           (unsigned int)dht11_data.temp_deci,
           (unsigned int)dht11_data.humi_int,
           (unsigned int)dht11_data.humi_deci,
           (unsigned long)air_quality,
           (unsigned int)adc_source_convertedvalue[ADCX_MQ135_BUFFER_INDEX],
           (unsigned long)water_level,
           (unsigned long)rain_detected,
           (unsigned long)rain_alarm_latched,
           (unsigned long)(tilt_tenths / 10U),
           (unsigned long)(tilt_tenths % 10U),
           (unsigned int)mpu6050_task.VibrationLevel,
           (unsigned int)servo1_task.current_angle,
           (unsigned int)servo2_task.current_angle,
           ESP8266_GetServoState(),
           (unsigned long)buzzer_active,
           (unsigned long)green_led_on,
           (unsigned long)blue_led_on);
}

static uint8_t ESP8266_ContainsControlCandidate(const char *message)
{
    if (message == NULL)
    {
        return 0;
    }

    return (strstr(message, "\"cmd\"") != NULL) ||
           (strstr(message, "\"action\"") != NULL) ||
           (strstr(message, "\"command\"") != NULL) ||
            (strstr(message, "+MQTTSUBRECV:") != NULL) ||
            (strstr(message, "LED_") != NULL) ||
           (strstr(message, "G_LED_") != NULL) ||
           (strstr(message, "B_LED_") != NULL) ||
           (strstr(message, "BEEP_") != NULL) ||
           (strstr(message, "SERVO_") != NULL) ||
           (strstr(message, "ledr ") != NULL) ||
           (strstr(message, "ledg ") != NULL) ||
           (strstr(message, "ledb ") != NULL) ||
           (strstr(message, "servo1 ") != NULL) ||
           (strstr(message, "servo2 ") != NULL);
}

static uint8_t ESP8266_ContainsDisconnectEvent(const char *message)
{
    if (message == NULL)
    {
        return 0;
    }

    return (strstr(message, "WIFI DISCONNECT") != NULL) ||
           (strstr(message, "CONNECT FAIL") != NULL) ||
            (strstr(message, "DNS Fail") != NULL) ||
            (strstr(message, "+MQTTDISCONNECTED:") != NULL) ||
            (strstr(message, "MQTT DISCONNECTED") != NULL) ||
            (strstr(message, "MQTT CLOSED") != NULL);
}

static uint8_t ESP8266_ExtractJsonStringValue(const char *json, const char *key, char *output, size_t output_size)
{
    char pattern[32] = {0};
    const char *found = NULL;
    size_t index = 0;

    if ((json == NULL) || (key == NULL) || (output == NULL) || (output_size == 0U))
    {
        return 0;
    }

    snprintf(pattern, sizeof(pattern), "\"%s\"", key);
    found = strstr(json, pattern);
    if (found == NULL)
    {
        return 0;
    }

    found += strlen(pattern);
    while ((*found == ' ') || (*found == '\t') || (*found == '\r') || (*found == '\n'))
    {
        found++;
    }

    if (*found != ':')
    {
        return 0;
    }

    found++;
    while ((*found == ' ') || (*found == '\t') || (*found == '\r') || (*found == '\n'))
    {
        found++;
    }

    if (*found != '"')
    {
        return 0;
    }

    found++;
    while ((found[index] != '\0') && (found[index] != '"') && (index < (output_size - 1U)))
    {
        output[index] = found[index];
        index++;
    }

    output[index] = '\0';
    return (index > 0U) ? 1U : 0U;
}

static uint8_t ESP8266_ExtractJsonLongValue(const char *json, const char *key, long *value)
{
    char pattern[32] = {0};
    const char *found = NULL;
    char *end_ptr = NULL;

    if ((json == NULL) || (key == NULL) || (value == NULL))
    {
        return 0;
    }

    snprintf(pattern, sizeof(pattern), "\"%s\"", key);
    found = strstr(json, pattern);
    if (found == NULL)
    {
        return 0;
    }

    found += strlen(pattern);
    while ((*found == ' ') || (*found == '\t') || (*found == '\r') || (*found == '\n'))
    {
        found++;
    }

    if (*found != ':')
    {
        return 0;
    }

    found++;
    while ((*found == ' ') || (*found == '"'))
    {
        found++;
    }

    *value = strtol(found, &end_ptr, 10);
    return (found != end_ptr) ? 1U : 0U;
}

static uint8_t ESP8266_SetNormalSendMode(void)
{
    return ESP8266_Cmd("AT+CIPMODE=0", "OK", "ERROR", 500);
}

static uint8_t ESP8266_MQTT_UserConfig(void)
{
    snprintf(esp8266_mqtt_command_buffer,
             sizeof(esp8266_mqtt_command_buffer),
             "AT+MQTTUSERCFG=0,1,\"%s\",\"%s\",\"%s\",0,0,\"\"",
             macUser_ESP8266_MqttClientId,
             macUser_ESP8266_MqttUsername,
             macUser_ESP8266_MqttPassword);

    return ESP8266_Cmd(esp8266_mqtt_command_buffer, "OK", NULL, 1000);
}

static uint8_t ESP8266_MQTT_ConnectBroker(void)
{
    snprintf(esp8266_mqtt_command_buffer,
             sizeof(esp8266_mqtt_command_buffer),
             "AT+MQTTCONN=0,\"%s\",%s,0",
             macUser_ESP8266_MqttHost,
             macUser_ESP8266_MqttPort);

    return ESP8266_Cmd(esp8266_mqtt_command_buffer, "OK", NULL, 5000);
}

static uint8_t ESP8266_MQTT_SubscribeControl(void)
{
    snprintf(esp8266_mqtt_command_buffer,
             sizeof(esp8266_mqtt_command_buffer),
             "AT+MQTTSUB=0,\"%s\",%u",
             macUser_ESP8266_MqttControlTopic,
             (unsigned int)ESP8266_MQTT_SUBSCRIBE_QOS);

    return ESP8266_Cmd(esp8266_mqtt_command_buffer, "OK", NULL, 1000);
}

static void ESP8266_MQTT_CleanSession(void)
{
    if ((esp8266_mqtt_step < 6U) && (esp8266_configuration_completed_flag == 0U))
    {
        return;
    }

    ESP8266_Cmd("AT+MQTTCLEAN=0", "OK", "ERROR", 1000);
}

static uint8_t ESP8266_MQTT_Publish(const char *topic, const char *payload)
{
    int payload_length = 0;
    int command_length = 0;

    if ((topic == NULL) || (payload == NULL))
    {
        return 0;
    }

    payload_length = (int)strlen(payload);
    if (payload_length <= 0)
    {
        return 0;
    }

    command_length = snprintf(esp8266_mqtt_command_buffer,
                              sizeof(esp8266_mqtt_command_buffer),
                              "AT+MQTTPUBRAW=0,\"%s\",%d,%u,0",
                              topic,
                              payload_length,
                              (unsigned int)ESP8266_MQTT_PUBLISH_QOS);
    if ((command_length <= 0) || (command_length >= (int)sizeof(esp8266_mqtt_command_buffer)))
    {
        return 0;
    }

    if (ESP8266_Cmd(esp8266_mqtt_command_buffer, "OK", ">", 1000) == 0)
    {
        printf("\r\n[MQTT] publish prepare failed\r\n");
        return 0;
    }

    ESP8266_ReadBufferReset();
    USARTX_SendArray(ESP8266_USARTX, (uint8_t *)payload, (uint32_t)payload_length);

    return ESP8266_MQTT_WaitPublishResult(MQTT_RESPONSE_WAIT_MS);
}

static uint8_t ESP8266_MQTT_BuildStatusPayload(void)
{
    int payload_length = 0;
    char temp_text[16] = {0};
    char humidity_text[16] = {0};
    char tilt_text[16] = {0};
    uint32_t water_level = ESP8266_GetWaterLevelPercent();
    uint32_t rain_detected = ESP8266_GetRainDetected();
    uint32_t water_high_alarm = (esp8266_rain_high_alarm_latched != 0U) ? 1U : 0U;
    uint32_t alarm_active = ESP8266_IsAnyAlarmLatched();
    uint32_t buzzer_active = (beep_task.beep_active || alarm_active) ? 1U : 0U;
    uint32_t green_led_on = ESP8266_IsLowLevelActive(G_LED_GPIO_PORT, G_LED_GPIO_PIN);
    uint32_t blue_led_on = ESP8266_IsLowLevelActive(B_LED_GPIO_PORT, B_LED_GPIO_PIN);
    uint32_t is_running = ((servo1_task.turn_left_flag != 0U) ||
                           (servo1_task.turn_right_flag != 0U) ||
                           (servo2_task.turn_left_flag != 0U) ||
                           (servo2_task.turn_right_flag != 0U) ||
                           (buzzer_active != 0U) ||
                           (green_led_on != 0U) ||
                           (blue_led_on != 0U)) ? 1U : 0U;

    ESP8266_FormatFixed1(temp_text, sizeof(temp_text), ESP8266_GetDhtTemperature());
    ESP8266_FormatFixed1(humidity_text, sizeof(humidity_text), ESP8266_GetDhtHumidity());
    ESP8266_FormatFixed1(tilt_text, sizeof(tilt_text), ESP8266_GetTiltAngle());

    payload_length = snprintf(
        esp8266_mqtt_payload_buffer,
        sizeof(esp8266_mqtt_payload_buffer),
        "{\"device_id\":\"%s\",\"machine_code\":\"%s\",\"temp\":%s,\"hum\":%s,\"air_quality\":%d,\"mq135_raw\":%u,\"water_level\":%lu,\"rain_detected\":%lu,\"water_high_alarm\":%lu,\"alarm_active\":%lu,\"vibration_level\":%u,\"tilt_angle\":%s,\"servo_angle\":%u,\"servo_state\":\"%s\",\"buzzer_enabled\":1,\"buzzer_active\":%lu,\"wifi_connected\":1,\"green_led_on\":%lu,\"blue_led_on\":%lu,\"is_online\":1,\"is_running\":%lu,\"timestamp\":%lu}",
        macUser_ESP8266_DeviceId,
        macUser_ESP8266_MachineCode,
        temp_text,
        humidity_text,
        ESP8266_GetAirQuality(),
        adc_source_convertedvalue[ADCX_MQ135_BUFFER_INDEX],
        (unsigned long)water_level,
        (unsigned long)rain_detected,
        (unsigned long)water_high_alarm,
        (unsigned long)alarm_active,
        mpu6050_task.VibrationLevel,
        tilt_text,
        servo1_task.current_angle,
        ESP8266_GetServoState(),
        (unsigned long)buzzer_active,
        (unsigned long)green_led_on,
        (unsigned long)blue_led_on,
        (unsigned long)is_running,
        (unsigned long)SysTick_GetCount());
    if ((payload_length <= 0) || (payload_length >= (int)sizeof(esp8266_mqtt_payload_buffer)))
    {
        return 0;
    }

    return 1;
}

static uint8_t ESP8266_MQTT_BuildAlarmPayload(void)
{
    int payload_length = 0;
    char alarm_value_text[16] = {0};
    char threshold_text[16] = {0};

    if (MPU6050_IsAlarmReportPending() != 0U)
    {
        ESP8266_FormatFixed1(alarm_value_text, sizeof(alarm_value_text), mpu6050_task.TiltDelta);
        ESP8266_FormatFixed1(threshold_text, sizeof(threshold_text), MPU6050_TILT_DELTA_ALARM_THRESHOLD);

        payload_length = snprintf(
            esp8266_mqtt_payload_buffer,
            sizeof(esp8266_mqtt_payload_buffer),
            "{\"id\":\"alarm_%s_%lu\",\"deviceId\":\"%s\",\"type\":\"TILT\",\"level\":\"CRITICAL\",\"alarmTitle\":\"Tilt alarm\",\"alarmMessage\":\"Tilt change exceeded threshold\",\"alarmValue\":\"%s\",\"thresholdValue\":%s,\"timestamp\":%lu}",
            macUser_ESP8266_DeviceId,
            (unsigned long)SysTick_GetCount(),
            macUser_ESP8266_DeviceId,
            alarm_value_text,
            threshold_text,
            (unsigned long)SysTick_GetCount());
    }
    else if (esp8266_rain_high_alarm_report_pending != 0U)
    {
        payload_length = snprintf(
            esp8266_mqtt_payload_buffer,
            sizeof(esp8266_mqtt_payload_buffer),
            "{\"id\":\"alarm_%s_%lu\",\"deviceId\":\"%s\",\"type\":\"WATER_HIGH\",\"level\":\"WARNING\",\"alarmTitle\":\"Water level high\",\"alarmMessage\":\"Water level exceeded threshold\",\"alarmValue\":\"%lu\",\"thresholdValue\":%u,\"timestamp\":%lu}",
            macUser_ESP8266_DeviceId,
            (unsigned long)SysTick_GetCount(),
            macUser_ESP8266_DeviceId,
            (unsigned long)ESP8266_GetWaterLevelPercent(),
            (unsigned int)WATER_HIGH_ALARM_THRESHOLD,
            (unsigned long)SysTick_GetCount());
    }
    else
    {
        return 0;
    }

    if ((payload_length <= 0) || (payload_length >= (int)sizeof(esp8266_mqtt_payload_buffer)))
    {
        return 0;
    }

    return 1;
}

static uint8_t ESP8266_MQTT_PublishSensorData(void)
{
    if (ESP8266_MQTT_BuildStatusPayload() == 0)
    {
        return 0;
    }

    return ESP8266_MQTT_Publish(macUser_ESP8266_MqttStatusTopic, esp8266_mqtt_payload_buffer);
}

static uint8_t ESP8266_MQTT_PublishAlarmIfNeeded(void)
{
    if (ESP8266_HasAlarmReportPending() == 0U)
    {
        return 1;
    }

    if (ESP8266_MQTT_BuildAlarmPayload() == 0)
    {
        return 0;
    }

    if (ESP8266_MQTT_Publish(macUser_ESP8266_MqttAlarmTopic, esp8266_mqtt_payload_buffer) == 0)
    {
        return 0;
    }

    if (MPU6050_IsAlarmReportPending() != 0U)
    {
        MPU6050_ClearAlarmReportPending();
    }
    else
    {
        esp8266_rain_high_alarm_report_pending = 0U;
    }

    return 1;
}

static uint8_t ESP8266_ExtractMqttPayload(const char *message, char *payload, size_t payload_size)
{
    const char *json_start = NULL;
    const char *json_end = NULL;
    const char *payload_start = NULL;
    size_t payload_length = 0;
    uint32_t comma_count = 0;

    if ((message == NULL) || (payload == NULL) || (payload_size == 0U))
    {
        return 0;
    }

    json_start = strchr(message, '{');
    json_end = strrchr(message, '}');
    if ((json_start != NULL) && (json_end != NULL) && (json_end >= json_start))
    {
        payload_length = (size_t)(json_end - json_start + 1U);
        if (payload_length >= payload_size)
        {
            payload_length = payload_size - 1U;
        }

        memcpy(payload, json_start, payload_length);
        payload[payload_length] = '\0';
        return 1;
    }

    payload_start = message;
    while (*payload_start != '\0')
    {
        if (*payload_start == ',')
        {
            comma_count++;
            if (comma_count == 3U)
            {
                payload_start++;
                break;
            }
        }
        payload_start++;
    }

    if (comma_count < 3U)
    {
        return 0;
    }

    strncpy(payload, payload_start, payload_size - 1U);
    payload[payload_size - 1U] = '\0';
    return 1;
}

static uint8_t ESP8266_MQTT_HandleIncomingPayload(const char *message)
{
    const char *cursor = message;
    char payload[MQTT_COMMAND_BUFFER_SIZE] = {0};
    size_t consumed_length = 0U;
    uint8_t handled_count = 0U;

    while ((cursor != NULL) && (*cursor != '\0'))
    {
        const char *subrecv = strstr(cursor, "+MQTTSUBRECV:");

        if (subrecv == NULL)
        {
            break;
        }

        memset(payload, 0, sizeof(payload));
        if (ESP8266_ExtractSingleSubPayload(subrecv, payload, sizeof(payload), &consumed_length) == 0)
        {
            cursor = subrecv + strlen("+MQTTSUBRECV:");
            continue;
        }

        handled_count += ESP8266_HandleSingleIncomingPayload(payload);
        cursor = subrecv + consumed_length;
    }

    if (handled_count != 0U)
    {
        return 1;
    }

    if (ESP8266_ExtractMqttPayload(message, payload, sizeof(payload)) == 0)
    {
        return ESP8266_LogControlIgnored("payload extract failed", NULL);
    }

    return ESP8266_HandleSingleIncomingPayload(payload);
}

static uint8_t ESP8266_ExtractSingleSubPayload(const char *message, char *payload, size_t payload_size, size_t *consumed_length)
{
    size_t available_length = 0U;
    const char *cursor = NULL;
    const char *length_start = NULL;
    const char *payload_start = NULL;
    char *length_end = NULL;
    size_t payload_length = 0U;
    uint32_t comma_count = 0U;

    if ((message == NULL) || (payload == NULL) || (payload_size == 0U) || (consumed_length == NULL))
    {
        return 0;
    }

    cursor = message;
    while ((*cursor != '\0') && (comma_count < 3U))
    {
        if (*cursor == ',')
        {
            comma_count++;
            if (comma_count == 2U)
            {
                length_start = cursor + 1;
            }
            else if (comma_count == 3U)
            {
                payload_start = cursor + 1;
                break;
            }
        }
        cursor++;
    }

    if ((length_start == NULL) || (payload_start == NULL))
    {
        return 0;
    }

    payload_length = (size_t)strtoul(length_start, &length_end, 10);
    if ((length_end == length_start) || (payload_length == 0U))
    {
        return 0;
    }

    available_length = strlen(payload_start);
    if (payload_length > available_length)
    {
        payload_length = available_length;
    }

    if (payload_length >= payload_size)
    {
        payload_length = payload_size - 1U;
    }

    memcpy(payload, payload_start, payload_length);
    payload[payload_length] = '\0';
    *consumed_length = (size_t)(payload_start - message) + payload_length;
    return 1;
}

static uint8_t ESP8266_HandleSingleIncomingPayload(const char *payload)
{
    char command_buffer[MQTT_COMMAND_BUFFER_SIZE] = {0};
    char *command = command_buffer;

    if (payload == NULL)
    {
        return 0;
    }

    strncpy(command_buffer, payload, sizeof(command_buffer) - 1U);
    command_buffer[sizeof(command_buffer) - 1U] = '\0';

    command = ESP8266_TrimCommand(command_buffer);
    printf("\r\n[CTRL] rx payload: %s\r\n", command);

    if ((*command == '\0') ||
        (strcmp(command, "null") == 0) ||
        (strcmp(command, "{}") == 0) ||
        (strcmp(command, "[]") == 0))
    {
        return ESP8266_LogControlIgnored("empty payload", command);
    }

    if (ESP8266_IsDuplicateControlPayload(command) != 0U)
    {
        return ESP8266_LogControlIgnored("duplicate payload", command);
    }

    return Get_ESP82666_Cmd(command);
}

static uint8_t ESP8266_IsDuplicateControlPayload(const char *payload)
{
    uint64_t now_tick = 0U;

    if ((payload == NULL) || (*payload == '\0'))
    {
        return 0U;
    }

    now_tick = SysTick_GetCount();
    if ((strncmp(esp8266_last_control_payload, payload, sizeof(esp8266_last_control_payload) - 1U) == 0) &&
        ((now_tick - esp8266_last_control_payload_tick) <= ESP8266_DUPLICATE_PAYLOAD_WINDOW_MS))
    {
        return 1U;
    }

    strncpy(esp8266_last_control_payload, payload, sizeof(esp8266_last_control_payload) - 1U);
    esp8266_last_control_payload[sizeof(esp8266_last_control_payload) - 1U] = '\0';
    esp8266_last_control_payload_tick = now_tick;
    return 0U;
}

static void ESP8266_NormalizeLegacyCommand(const char *command, char *normalized, size_t normalized_size)
{
    size_t index = 0;

    if ((command == NULL) || (normalized == NULL) || (normalized_size == 0))
    {
        return;
    }

    while ((command[index] != '\0') && (index < (normalized_size - 1U)))
    {
        char current = command[index];

        if (current == '-')
        {
            current = '_';
        }
        else if ((current >= 'a') && (current <= 'z'))
        {
            current = (char)toupper((unsigned char)current);
        }

        normalized[index] = current;
        index++;
    }

    normalized[index] = '\0';
}

static char *ESP8266_TrimCommand(char *command)
{
    size_t length = 0;

    while ((*command == ' ') || (*command == '\t'))
    {
        command++;
    }

    length = strlen(command);
    while ((length > 0) &&
           ((command[length - 1] == '\r') ||
            (command[length - 1] == '\n') ||
            (command[length - 1] == ' ') ||
            (command[length - 1] == '\t')))
    {
        command[length - 1] = '\0';
        length--;
    }

    return command;
}

static void ESP8266_PrintControlHelp(void)
{
    printf("\r\nAvailable control commands:\r\n");
    printf(" LED_ON / LED_OFF\r\n");
    printf(" G_LED_ON / G_LED_OFF\r\n");
    printf(" B_LED_ON / B_LED_OFF\r\n");
    printf(" BEEP_ON / BEEP_OFF\r\n");
    printf(" SERVO_LEFT / SERVO_RIGHT\r\n");
    printf(" help\r\n");
    printf(" runled on|off\r\n");
    printf(" ledr on|off|toggle\r\n");
    printf(" ledg on|off|toggle\r\n");
    printf(" ledb on|off|toggle\r\n");
    printf(" beep on|off|<speed_ms>\r\n");
    printf(" servo1 <0-180>\r\n");
    printf(" servo2 <0-180>\r\n");
}

static uint8_t ESP8266_LogControlApplied(const char *command)
{
    printf("\r\n[CTRL] applied: %s\r\n", command);
    return 1;
}

static uint8_t ESP8266_LogControlIgnored(const char *reason, const char *command)
{
    if ((command == NULL) || (*command == '\0'))
    {
        printf("\r\n[CTRL] ignored: %s\r\n", reason);
    }
    else
    {
        printf("\r\n[CTRL] ignored(%s): %s\r\n", reason, command);
    }

    return 1;
}

static uint8_t ESP8266_LogControlUnsupported(const char *command)
{
    printf("\r\n[CTRL] unsupported command: %s\r\n", command);
    return 0;
}

static uint8_t ESP8266_LedControl(GPIO_TypeDef* port, uint16_t pin, const char *action)
{
    if (strcmp(action, "on") == 0)
    {
        LED_ON(port, pin, LED_LOW_TRIGGER);
        return 1;
    }
    else if (strcmp(action, "off") == 0)
    {
        LED_OFF(port, pin, LED_LOW_TRIGGER);
        return 1;
    }
    else if (strcmp(action, "toggle") == 0)
    {
        LED_TOGGLE(port, pin);
        return 1;
    }

    return 0;
}

static void ESP8266_EnableDualLedOutput(void)
{
    LED_ON(G_LED_GPIO_PORT, G_LED_GPIO_PIN, LED_LOW_TRIGGER);
    LED_ON(B_LED_GPIO_PORT, B_LED_GPIO_PIN, LED_LOW_TRIGGER);
}

static void ESP8266_DisableDualLedOutput(void)
{
    if (ESP8266_IsAnyAlarmLatched())
    {
        return;
    }

    LED_OFF(G_LED_GPIO_PORT, G_LED_GPIO_PIN, LED_LOW_TRIGGER);
    LED_OFF(B_LED_GPIO_PORT, B_LED_GPIO_PIN, LED_LOW_TRIGGER);
}

static void ESP8266_EnableBuzzerOutput(void)
{
    beep_task.speed = 100;
    beep_task.beep_active = true;
    beep_task.trigger_flag = 0;
}

static void ESP8266_DisableBuzzerOutput(void)
{
    if (ESP8266_IsAnyAlarmLatched())
    {
        return;
    }

    beep_task.beep_active = false;
    beep_task.trigger_flag = 0;
}

static void ESP8266_TriggerAlarmOutput(void)
{
    mpu6050_task.AlarmLatched = 1;
    mpu6050_task.AlarmReportPending = 1;

    ESP8266_EnableBuzzerOutput();
    ESP8266_EnableDualLedOutput();
}

static void ESP8266_ClearAllAlarmState(void)
{
    esp8266_rain_high_alarm_latched = 0U;
    esp8266_rain_high_alarm_report_pending = 0U;
    MPU6050_ClearAlarm();
}

static void ESP8266_CheckRainHighAlarm(void)
{
    uint32_t water_level = ESP8266_GetWaterLevelPercent();

    if ((water_level < WATER_HIGH_ALARM_THRESHOLD) ||
        (esp8266_rain_high_alarm_latched != 0U))
    {
        return;
    }

    esp8266_rain_high_alarm_latched = 1U;
    esp8266_rain_high_alarm_report_pending = 1U;

    ESP8266_EnableBuzzerOutput();
    ESP8266_EnableDualLedOutput();

    printf("\r\n[ALARM] water level exceeded: water=%lu threshold=%u\r\n",
           (unsigned long)water_level,
           (unsigned int)WATER_HIGH_ALARM_THRESHOLD);
}

static uint8_t ESP8266_HandleStandardHttpCommand(const char *normalized_command)
{
    char *end_ptr = NULL;
    unsigned long angle = 0;

    if (normalized_command == NULL)
    {
        return 0;
    }

    if (strcmp(normalized_command, "LED_ON") == 0)
    {
        ESP8266_EnableDualLedOutput();
        return 1;
    }

    if (strcmp(normalized_command, "LED_OFF") == 0)
    {
        ESP8266_DisableDualLedOutput();
        return 1;
    }

    if ((strcmp(normalized_command, "BUZZER_ON") == 0) ||
        (strcmp(normalized_command, "BEEP_ON") == 0))
    {
        ESP8266_EnableBuzzerOutput();
        return 1;
    }

    if ((strcmp(normalized_command, "BUZZER_OFF") == 0) ||
        (strcmp(normalized_command, "BEEP_OFF") == 0))
    {
        ESP8266_DisableBuzzerOutput();
        return 1;
    }

    if ((strcmp(normalized_command, "ALARM") == 0) ||
        (strcmp(normalized_command, "ALARM_ON") == 0))
    {
        ESP8266_TriggerAlarmOutput();
        return 1;
    }

    if (strcmp(normalized_command, "ALARM_RESET") == 0)
    {
        ESP8266_ClearAllAlarmState();
        return 1;
    }

    if (strncmp(normalized_command, "SERVO_", 6) == 0)
    {
        angle = strtoul(normalized_command + 6, &end_ptr, 10);
        if ((end_ptr == (normalized_command + 6)) || (*end_ptr != '\0') || (angle > 180U))
        {
            return 0;
        }

        Servo_AngleConfig(SERVO_NUM1, (uint16_t)angle);
        Servo_AngleConfig(SERVO_NUM2, (uint16_t)angle);
        return 1;
    }

    return 0;
}

static uint8_t ESP8266_HandleLegacyCommand(const char *command)
{
    uint32_t index = 0;

    memset(esp8266_normalized_command_buffer, 0, sizeof(esp8266_normalized_command_buffer));
    ESP8266_NormalizeLegacyCommand(command, esp8266_normalized_command_buffer, sizeof(esp8266_normalized_command_buffer));

    for(index = 0; index < CONTROL_CMD_NUMBER; index++)
    {
        if(strstr(esp8266_normalized_command_buffer, control_cmds[index]) != NULL)
        {
            switch(index)
            {
                case 0:
                    LED_ON(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);
                    return 1;

                case 1:
                    if (ESP8266_IsAnyAlarmLatched())
                    {
                        return 1;
                    }
                    LED_OFF(G_LED_GPIO_PORT,G_LED_GPIO_PIN,LED_LOW_TRIGGER);
                    return 1;

                case 2:
                    LED_ON(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);
                    return 1;

                case 3:
                    if (ESP8266_IsAnyAlarmLatched())
                    {
                        return 1;
                    }
                    LED_OFF(B_LED_GPIO_PORT,B_LED_GPIO_PIN,LED_LOW_TRIGGER);
                    return 1;

                case 4:
                    ESP8266_EnableBuzzerOutput();
                    return 1;

                case 5:
                    ESP8266_DisableBuzzerOutput();
                    return 1;

                case 6:
                    servo1_task.turn_left_flag = 0;
                    servo1_task.turn_right_flag = 0;
                    servo2_task.turn_left_flag = 0;
                    servo2_task.turn_right_flag = 0;
                    Servo_AngleConfig(SERVO_NUM1, WINDOW_SERVO_OPEN_ANGLE);
                    Servo_AngleConfig(SERVO_NUM2, WINDOW_SERVO_OPEN_ANGLE);
                    return 1;

                case 7:
                    servo1_task.turn_left_flag = 0;
                    servo1_task.turn_right_flag = 0;
                    servo2_task.turn_left_flag = 0;
                    servo2_task.turn_right_flag = 0;
                    Servo_AngleConfig(SERVO_NUM1, WINDOW_SERVO_CLOSE_ANGLE);
                    Servo_AngleConfig(SERVO_NUM2, WINDOW_SERVO_CLOSE_ANGLE);
                    return 1;

                default:
                    break;
            }
        }
    }

    return 0;
}

static uint8_t ESP8266_HandleJsonCommand(const char *command)
{
    char detail[64] = {0};
    char json_status[16] = {0};
    char json_command[32] = {0};
    char json_device_id[32] = {0};
    char normalized_json_command[32] = {0};
    long angle = 0;

    if ((ESP8266_ExtractJsonStringValue(command, "status", json_status, sizeof(json_status)) == 1) &&
        (strcmp(json_status, "success") != 0))
    {
        return ESP8266_LogControlIgnored("json status mismatch", command);
    }

    if ((ESP8266_ExtractJsonStringValue(command, "device_id", json_device_id, sizeof(json_device_id)) == 0) &&
        (ESP8266_ExtractJsonStringValue(command, "deviceId", json_device_id, sizeof(json_device_id)) == 0))
    {
        json_device_id[0] = '\0';
    }

    if ((json_device_id[0] != '\0') &&
        (strcmp(json_device_id, macUser_ESP8266_DeviceId) != 0))
    {
        return ESP8266_LogControlIgnored("device id mismatch", command);
    }

    if ((ESP8266_ExtractJsonStringValue(command, "cmd", json_command, sizeof(json_command)) == 0) &&
        (ESP8266_ExtractJsonStringValue(command, "action", json_command, sizeof(json_command)) == 0) &&
        (ESP8266_ExtractJsonStringValue(command, "command", json_command, sizeof(json_command)) == 0))
    {
        return 0;
    }

    ESP8266_NormalizeLegacyCommand(json_command, normalized_json_command, sizeof(normalized_json_command));

    if (ESP8266_HandleStandardHttpCommand(normalized_json_command) == 1)
    {
        return ESP8266_LogControlApplied(normalized_json_command);
    }

    if ((strcmp(normalized_json_command, "SERVO1") == 0) || (strcmp(normalized_json_command, "SERVO2") == 0))
    {
        if ((ESP8266_ExtractJsonLongValue(command, "angle", &angle) == 0) &&
            (ESP8266_ExtractJsonLongValue(command, "servo_angle", &angle) == 0) &&
            (ESP8266_ExtractJsonLongValue(command, "value", &angle) == 0))
        {
            return ESP8266_LogControlIgnored("missing servo angle", command);
        }

        if ((angle < 0) || (angle > 180))
        {
            return ESP8266_LogControlIgnored("invalid servo angle", command);
        }

        if (strcmp(normalized_json_command, "SERVO1") == 0)
        {
            Servo_AngleConfig(SERVO_NUM1, (uint16_t)angle);
        }
        else
        {
            Servo_AngleConfig(SERVO_NUM2, (uint16_t)angle);
        }

        snprintf(detail, sizeof(detail), "%s=%ld", normalized_json_command, angle);
        return ESP8266_LogControlApplied(detail);
    }

    if (ESP8266_HandleLegacyCommand(normalized_json_command) == 1)
    {
        return ESP8266_LogControlApplied(normalized_json_command);
    }

    return 0;
}

void ESP8266_ReadBufferReset(void)
{
    ESP8266_PreserveAsyncMessage();
    memset(esp8266_receive.buffer,NULL,esp8266_receive.len);
    esp8266_receive.len = 0;
    esp8266_receive.read_flag = 0;
    esp8266_receive.Received_data_completed_flag = 0;
}

static uint8_t ESP8266_TakeReceivedSnapshot(uint8_t *buffer, uint32_t buffer_size)
{
    uint32_t received_length = 0U;

    if ((buffer == NULL) || (buffer_size == 0U) || (esp8266_receive.read_flag == 0U))
    {
        return 0U;
    }

    USART_ITConfig(ESP8266_USARTX, USART_IT_RXNE, DISABLE);
    USART_ITConfig(ESP8266_USARTX, USART_IT_IDLE, DISABLE);

    received_length = esp8266_receive.len;
    if (received_length >= ESP8266_BUFFER_SIZE)
    {
        received_length = ESP8266_BUFFER_SIZE - 1U;
    }

    if (received_length >= buffer_size)
    {
        received_length = buffer_size - 1U;
    }

    memset(buffer, 0, buffer_size);
    if (received_length > 0U)
    {
        memcpy(buffer, esp8266_receive.buffer, received_length);
    }
    buffer[received_length] = '\0';

    memset(esp8266_receive.buffer, 0, sizeof(esp8266_receive.buffer));
    esp8266_receive.len = 0U;
    esp8266_receive.read_flag = 0U;
    esp8266_receive.Received_data_completed_flag = 0U;

    USART_ITConfig(ESP8266_USARTX, USART_IT_RXNE, ENABLE);
    USART_ITConfig(ESP8266_USARTX, USART_IT_IDLE, ENABLE);

    return (received_length > 0U) ? 1U : 0U;
}

static uint8_t ESP8266_DrainPendingMessages(uint8_t max_batches)
{
    uint8_t handled_batches = 0U;

    if (max_batches == 0U)
    {
        max_batches = 1U;
    }

    while (handled_batches < max_batches)
    {
        if (ESP8266_TakeReceivedSnapshot(esp8266_received_snapshot, sizeof(esp8266_received_snapshot)) == 1U)
        {
            ESP8266_ProcessReceivedMessage((char *)esp8266_received_snapshot);
            handled_batches++;
            continue;
        }

        if (ESP8266_ReadAsyncMessage(esp8266_received_snapshot, sizeof(esp8266_received_snapshot)) == 1U)
        {
            ESP8266_ProcessReceivedMessage((char *)esp8266_received_snapshot);
            handled_batches++;
            continue;
        }

        break;
    }

    return handled_batches;
}

static uint8_t ESP8266_ShouldDeferPublish(uint64_t now_tick, uint8_t handled_batches)
{
    if (handled_batches != 0U)
    {
        return 1U;
    }

    if ((esp8266_last_rx_activity_tick != 0U) &&
        ((now_tick - esp8266_last_rx_activity_tick) < ESP8266_RX_QUIET_WINDOW_MS))
    {
        return 1U;
    }

    return 0U;
}

static uint8_t ESP8266_MQTT_WaitPublishResult(uint32_t timeout_ms)
{
    uint64_t start_tick = SysTick_GetCount();

    while ((SysTick_GetCount() - start_tick) < timeout_ms)
    {
        if (esp8266_receive.read_flag != 0U)
        {
            if (esp8266_receive.len >= ESP8266_BUFFER_SIZE)
            {
                esp8266_receive.buffer[ESP8266_BUFFER_SIZE - 1U] = '\0';
            }
            else
            {
                esp8266_receive.buffer[esp8266_receive.len] = '\0';
            }

            if (strstr((char *)esp8266_receive.buffer, "+MQTTPUB:OK") != NULL)
            {
                ESP8266_ReadBufferReset();
                ESP8266_DrainPendingMessages(ESP8266_PENDING_MESSAGE_BATCH_LIMIT);
                return 1U;
            }

            if ((strstr((char *)esp8266_receive.buffer, "ERROR") != NULL) ||
                (strstr((char *)esp8266_receive.buffer, "FAIL") != NULL))
            {
                printf("\r\n[MQTT] publish failed: %s\r\n", (char *)esp8266_receive.buffer);
                ESP8266_ReadBufferReset();
                ESP8266_DrainPendingMessages(ESP8266_PENDING_MESSAGE_BATCH_LIMIT);
                return 0U;
            }

            ESP8266_ReadBufferReset();
            ESP8266_DrainPendingMessages(ESP8266_PENDING_MESSAGE_BATCH_LIMIT);
        }

        DWT_DelayMs(MQTT_PUBLISH_POLL_INTERVAL_MS);
    }

    if (esp8266_receive.read_flag != 0U)
    {
        if (esp8266_receive.len >= ESP8266_BUFFER_SIZE)
        {
            esp8266_receive.buffer[ESP8266_BUFFER_SIZE - 1U] = '\0';
        }
        else
        {
            esp8266_receive.buffer[esp8266_receive.len] = '\0';
        }

        printf("\r\n[MQTT] publish timeout: %s\r\n", (char *)esp8266_receive.buffer);
    }
    else
    {
        printf("\r\n[MQTT] publish timeout\r\n");
    }

    ESP8266_ReadBufferReset();
    return 0U;
}


void ESP8266_TaskInit(void)
{
    esp8266_rain_high_alarm_latched = 0U;
    esp8266_rain_high_alarm_report_pending = 0U;
    ESP8266_ReadBufferReset();
    ESP8266_ResetConnectionState();
}

void ESP8266_ResetConnectionState(void)
{
    ESP8266_MQTT_CleanSession();
    esp8266_mqtt_step = 0;
    esp8266_last_publish_tick = 0;
    esp8266_last_rx_activity_tick = 0U;
    esp8266_last_monitor_print_tick = 0;
    esp8266_configuration_completed_flag = 0;
    ESP8266_ReadBufferReset();
}


void ESP8266_Task(void)
{
    ESP8266_DrainPendingMessages(ESP8266_PENDING_MESSAGE_BATCH_LIMIT);
}

static void ESP8266_ProcessReceivedMessage(const char *message)
{
    if ((message == NULL) || (*message == '\0'))
    {
        return;
    }

    esp8266_last_rx_activity_tick = SysTick_GetCount();

    if (ESP8266_ShouldDebugPrintMessage(message) != 0U)
    {
        printf("\r\n[ESP8266] %s\r\n", message);
    }

    if ((esp8266_configuration_completed_flag == 1U) &&
        (ESP8266_ContainsDisconnectEvent(message) != 0U))
    {
        ESP8266_ResetConnectionState();
    }
    else if ((esp8266_configuration_completed_flag == 1U) &&
             (ESP8266_ContainsControlCandidate(message) != 0U))
    {
        ESP8266_MQTT_HandleIncomingPayload(message);
    }
}

uint8_t Get_ESP82666_Cmd(char * cmd)
{
    char *command = ESP8266_TrimCommand(cmd);
    char *argument = NULL;
    unsigned long angle = 0;

    if (*command == '\0')
    {
        return ESP8266_LogControlIgnored("empty command", command);
    }

    if ((strstr(command, "\"cmd\"") != NULL) ||
        (strstr(command, "\"action\"") != NULL) ||
        (strstr(command, "\"command\"") != NULL))
    {
        if (ESP8266_HandleJsonCommand(command) == 1)
        {
            return 1;
        }

        return ESP8266_LogControlUnsupported(command);
    }

    if (strcmp(command, "help") == 0)
    {
        ESP8266_PrintControlHelp();
        return ESP8266_LogControlApplied(command);
    }

    if (strncmp(command, "runled ", 7) == 0)
    {
        argument = command + 7;

        if (strcmp(argument, "on") == 0)
        {
            ProgramRunLed_TaskInit(RUN_LED_DEFAULT_CYCLE);
            return ESP8266_LogControlApplied(command);
        }

        if (strcmp(argument, "off") == 0)
        {
            program_run_led_task.cycle = 0;
            program_run_led_task.timer = 0;
            program_run_led_task.flag = 0;
            LED_OFF(R_LED_GPIO_PORT, R_LED_GPIO_PIN, LED_LOW_TRIGGER);
            return ESP8266_LogControlApplied(command);
        }

        return ESP8266_LogControlIgnored("invalid runled argument", command);
    }

    if (strncmp(command, "ledr ", 5) == 0)
    {
        if (ESP8266_LedControl(R_LED_GPIO_PORT, R_LED_GPIO_PIN, command + 5) == 1)
        {
            return ESP8266_LogControlApplied(command);
        }

        return ESP8266_LogControlIgnored("invalid led action", command);
    }

    if (strncmp(command, "ledg ", 5) == 0)
    {
        if (ESP8266_LedControl(G_LED_GPIO_PORT, G_LED_GPIO_PIN, command + 5) == 1)
        {
            return ESP8266_LogControlApplied(command);
        }

        return ESP8266_LogControlIgnored("invalid led action", command);
    }

    if (strncmp(command, "ledb ", 5) == 0)
    {
        if (ESP8266_LedControl(B_LED_GPIO_PORT, B_LED_GPIO_PIN, command + 5) == 1)
        {
            return ESP8266_LogControlApplied(command);
        }

        return ESP8266_LogControlIgnored("invalid led action", command);
    }

    if (strncmp(command, "beep ", 5) == 0)
    {
        argument = command + 5;

        if (strcmp(argument, "on") == 0)
        {
            ESP8266_EnableBuzzerOutput();
            return ESP8266_LogControlApplied(command);
        }

        if (strcmp(argument, "off") == 0)
        {
            ESP8266_DisableBuzzerOutput();
            return ESP8266_LogControlApplied(command);
        }

        beep_task.speed = (uint16_t)strtoul(argument, NULL, 10);
        if (beep_task.speed == 0)
        {
            return ESP8266_LogControlIgnored("invalid buzzer argument", command);
        }

        beep_task.beep_active = true;
        beep_task.trigger_flag = 0;
        return ESP8266_LogControlApplied(command);
    }

    if (strncmp(command, "servo1 ", 7) == 0)
    {
        angle = strtoul(command + 7, NULL, 10);
        if (angle > 180)
        {
            return ESP8266_LogControlIgnored("invalid servo angle", command);
        }

        Servo_AngleConfig(SERVO_NUM1, (uint16_t)angle);
        return ESP8266_LogControlApplied(command);
    }

    if (strncmp(command, "servo2 ", 7) == 0)
    {
        angle = strtoul(command + 7, NULL, 10);
        if (angle > 180)
        {
            return ESP8266_LogControlIgnored("invalid servo angle", command);
        }

        Servo_AngleConfig(SERVO_NUM2, (uint16_t)angle);
        return ESP8266_LogControlApplied(command);
    }

    if (ESP8266_HandleLegacyCommand(command) == 1)
    {
        return ESP8266_LogControlApplied(command);
    }

    return ESP8266_LogControlUnsupported(command);
}


void ESP8266_StaTcpClient_Unvarnish_ConfigTest(void)
{
    switch(esp8266_mqtt_step)
    {
        case(0):if(ESP8266_AT_Test() == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;

    case(1):if(ESP8266_Enable_MultipleId(DISABLE) == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;       

    case(2):if(ESP8266_SetNormalSendMode() == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;  

    case(3):if(ESP8266_Net_Mode_Choose ( STA ) == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;

    case(4):if(ESP8266_DHCP_CUR() == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;  

    case(5):if(ESP8266_JoinAP ( macUser_ESP8266_ApSsid, macUser_ESP8266_ApPwd ) == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;  

    case(6):if(ESP8266_MQTT_UserConfig() == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;

    case(7):if(ESP8266_MQTT_ConnectBroker() == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;

    case(8):if(ESP8266_MQTT_SubscribeControl() == 1)
                {
                    esp8266_mqtt_step++;
                }
                break;

    case(9):esp8266_configuration_completed_flag = 1;
                esp8266_mqtt_step++;
                break;

        default: break;
    }
}

void ESP8266_CloudTask(void)
{
    uint64_t now_tick = SysTick_GetCount();
    uint8_t handled_batches = 0U;

    ESP8266_CheckRainHighAlarm();
    ESP8266_DebugPrintMonitorData();

    if (esp8266_configuration_completed_flag == 0)
    {
        return;
    }

    handled_batches = ESP8266_DrainPendingMessages(ESP8266_PENDING_MESSAGE_BATCH_LIMIT);

    if ((esp8266_last_publish_tick == 0U) ||
        ((now_tick - esp8266_last_publish_tick) >= ESP8266_MQTT_PUBLISH_PERIOD_MS))
    {
        if (ESP8266_ShouldDeferPublish(now_tick, handled_batches) != 0U)
        {
            return;
        }

        handled_batches += ESP8266_DrainPendingMessages(1U);
        now_tick = SysTick_GetCount();
        if (ESP8266_ShouldDeferPublish(now_tick, handled_batches) != 0U)
        {
            return;
        }

        if (ESP8266_MQTT_PublishSensorData() == 1)
        {
            esp8266_last_publish_tick = SysTick_GetCount();
        }
        else
        {
            printf("\r\n[MQTT] status publish failed, reset MQTT state\r\n");
            ESP8266_ResetConnectionState();
            return;
        }
    }

    handled_batches = ESP8266_DrainPendingMessages(ESP8266_PENDING_MESSAGE_BATCH_LIMIT);
    now_tick = SysTick_GetCount();
    if (ESP8266_ShouldDeferPublish(now_tick, handled_batches) != 0U)
    {
        return;
    }

    if (ESP8266_MQTT_PublishAlarmIfNeeded() == 0)
    {
        printf("\r\n[MQTT] alarm publish failed, reset MQTT state\r\n");
        ESP8266_ResetConnectionState();
    }
}


void ESP8266_SendDHT11DataTest(void)
{
    char cStr [ 100 ] = { 0 };

    if( dht11_rd_task.read_completed_flag == 1 )//璇诲彇鎴愬姛
    {
        sprintf ( cStr, "\r\nHumidity %d.%d %%RH, Temperature %d.%d C\r\n", 
                  dht11_data.humi_int, dht11_data.humi_deci, dht11_data.temp_int, dht11_data.temp_deci );

        ESP8266_SendString ( ENABLE, cStr, 0, Single_ID_0 );               

        dht11_rd_task.read_completed_flag = 0;
      
    }
    else if( dht11_rd_task.read_completed_flag == 2 )//读取失败
    {

        sprintf ( cStr, "\r\nRead DHT11 ERROR!\r\n" );
        ESP8266_SendString ( ENABLE, cStr, 0, Single_ID_0 );               

        dht11_rd_task.read_completed_flag = 0;  
    }
}


void ESP8266_SendMQ135DataTest(void)
{
    char cStr [ 100 ] = { 0 };
    char ppm_text[16] = { 0 };

    if( mq135_task.read_completed_flag == 1 )//璇诲彇鎴愬姛
    {

        if(mq135_task.ppm<10)
        {
            sprintf( cStr, "\r\nMQ135 avg ppm below measurable range\r\n");
        }
        else if(mq135_task.ppm>1000)
        {
            sprintf( cStr, "\r\nMQ135 avg ppm above measurable range\r\n");
        }
        else
        {
            ESP8266_FormatFixed1(ppm_text, sizeof(ppm_text), mq135_task.ppm);
            sprintf( cStr, "\r\nMQ135 avg ppm: %s\r\n", ppm_text);
        }      
  
    ESP8266_SendString ( ENABLE, cStr, 0, Single_ID_0 );               

    mq135_task.read_completed_flag = 0;
    
    }
}

/*****************************END OF FILE***************************************/
