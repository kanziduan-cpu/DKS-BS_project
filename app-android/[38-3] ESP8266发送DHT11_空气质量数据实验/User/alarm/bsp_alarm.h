#ifndef __BSP_ALARM_H
#define __BSP_ALARM_H

#include "stm32f10x.h"
#include <stdint.h>

/* 报警类型定义 */
typedef enum {
    ALARM_NONE = 0,         /* 无报警 */
    ALARM_TEMPERATURE = 1,   /* 温度异常 */
    ALARM_HUMIDITY = 2,     /* 湿度异常 */
    ALARM_AIR_QUALITY = 3,  /* 空气质量异常 */
    ALARM_SHAKE = 4,        /* 震动检测 */
    ALARM_TILT = 5,         /* 倾斜检测 */
    ALARM_ALL = 0xFF        /* 所有异常 */
} Alarm_Type_TypeDef;

/* 报警严重程度 */
typedef enum {
    ALARM_LEVEL_LOW = 0,    /* 低级报警 */
    ALARM_LEVEL_MEDIUM = 1,  /* 中级报警 */
    ALARM_LEVEL_HIGH = 2,    /* 高级报警 */
    ALARM_LEVEL_CRITICAL = 3 /* 严重报警 */
} Alarm_Level_TypeDef;

/* 报警系统状态 */
typedef struct {
    uint8_t alarm_active;            /* 报警激活状态 */
    uint8_t alarm_type;             /* 当前报警类型 */
    uint8_t alarm_level;            /* 当前报警级别 */
    uint32_t alarm_start_time;      /* 报警开始时间 */
    uint32_t alarm_duration;        /* 报警持续时间(ms) */
    uint8_t flash_enabled;          /* 闪烁使能 */
    uint8_t beep_enabled;           /* 蜂鸣器使能 */
} Alarm_System_TypeDef;

/* 全局变量声明 */
extern volatile Alarm_System_TypeDef alarm_system;

/* 函数声明 */
void ALARM_Init(void);
void ALARM_Start(Alarm_Type_TypeDef alarm_type, Alarm_Level_TypeDef alarm_level);
void ALARM_Stop(void);
void ALARM_Update(void);
void ALARM_SetFlashEnable(uint8_t enable);
void ALARM_SetBeepEnable(uint8_t enable);
uint8_t ALARM_IsActive(void);
void ALARM_ClearAll(void);

/* 异常检测函数 */
uint8_t ALARM_CheckTemperature(float temperature, float min_temp, float max_temp);
uint8_t ALARM_CheckHumidity(float humidity, float min_humidity, float max_humidity);
uint8_t ALARM_CheckAirQuality(uint16_t air_quality, uint16_t threshold);
uint8_t ALARM_CheckShake(uint8_t shake_detected);
uint8_t ALARM_CheckTilt(uint8_t tilt_detected);

#endif /* __BSP_ALARM_H */
