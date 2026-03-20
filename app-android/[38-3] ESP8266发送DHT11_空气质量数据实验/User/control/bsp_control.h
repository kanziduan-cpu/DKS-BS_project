#ifndef __BSP_CONTROL_H
#define __BSP_CONTROL_H

#include "stm32f10x.h"
#include <stdint.h>

/* 通风口状态 */
typedef enum {
    VENT_CLOSED = 0,      /* 通风口关闭 */
    VENT_OPEN = 1         /* 通风口打开 */
} Vent_State_TypeDef;

/* 控制模式 */
typedef enum {
    CONTROL_MANUAL = 0,   /* 手动控制 */
    CONTROL_AUTO = 1      /* 自动控制 */
} Control_Mode_TypeDef;

/* 自动控制触发条件 */
typedef enum {
    TRIGGER_NONE = 0,         /* 无触发 */
    TRIGGER_SHAKE = 1,        /* 震动触发 */
    TRIGGER_TILT = 2,         /* 倾斜触发 */
    TRIGGER_AIR_QUALITY = 3,  /* 空气质量触发 */
    TRIGGER_TEMPERATURE = 4,  /* 温度触发 */
    TRIGGER_ALL = 0xFF        /* 所有条件 */
} Trigger_Type_TypeDef;

/* 控制系统状态 */
typedef struct {
    uint8_t control_mode;              /* 控制模式 */
    uint8_t vent_state;               /* 通风口状态 */
    uint8_t trigger_type;             /* 当前触发类型 */
    uint8_t auto_control_enabled;     /* 自动控制使能 */
    uint16_t vent_open_angle;         /* 通风口打开角度 */
    uint16_t vent_close_angle;        /* 通风口关闭角度 */
    uint32_t last_action_time;        /* 最后动作时间 */
    uint32_t action_delay;            /* 动作延迟时间(ms) */
} Control_System_TypeDef;

/* 全局变量声明 */
extern volatile Control_System_TypeDef control_system;

/* 函数声明 */
void CONTROL_Init(void);
void CONTROL_SetMode(Control_Mode_TypeDef mode);
void CONTROL_SetAutoEnable(uint8_t enable);
void CONTROL_OpenVent(void);
void CONTROL_CloseVent(void);
void CONTROL_SetVentAngle(uint16_t angle);
void CONTROL_Update(void);
void CONTROL_ProcessTrigger(Trigger_Type_TypeDef trigger_type, uint8_t trigger_value);
void CONTROL_ManualControl(uint8_t open_close);

/* 阈值设置函数 */
void CONTROL_SetAirQualityThreshold(uint16_t threshold);
void CONTROL_SetTemperatureThreshold(float min_temp, float max_temp);
void CONTROL_SetShakeThreshold(uint8_t severity_level);
void CONTROL_SetTiltThreshold(uint8_t tilt_detected);

/* 状态查询函数 */
uint8_t CONTROL_GetVentState(void);
Control_Mode_TypeDef CONTROL_GetMode(void);
uint8_t CONTROL_IsAutoEnabled(void);

#endif /* __BSP_CONTROL_H */
