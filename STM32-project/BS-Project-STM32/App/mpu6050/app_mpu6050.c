/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "mpu6050/app_mpu6050.h"
#include "mpu6050/bsp_i2c_mpu6050.h"
#include <math.h>
#include <stdio.h>
#include "beep/app_beep.h"
#include "led/bsp_gpio_led.h"

MPU6050_TaskInfo mpu6050_task  = {0};
static float mpu6050_last_tilt_x = 0.0f;
static float mpu6050_last_tilt_y = 0.0f;
static uint8_t mpu6050_has_tilt_reference = 0;

static float MPU6050_GetGyroPeakDps(void)
{
  float gyro_x_dps = fabsf((float)mpu6050_task.Gyro[0]) / 16.4f;
  float gyro_y_dps = fabsf((float)mpu6050_task.Gyro[1]) / 16.4f;
  float gyro_z_dps = fabsf((float)mpu6050_task.Gyro[2]) / 16.4f;
  float gyro_peak_dps = gyro_x_dps;

  if (gyro_y_dps > gyro_peak_dps)
  {
    gyro_peak_dps = gyro_y_dps;
  }

  if (gyro_z_dps > gyro_peak_dps)
  {
    gyro_peak_dps = gyro_z_dps;
  }

  return gyro_peak_dps;
}

static uint16_t MPU6050_CalculateVibrationLevel(float tilt_delta, float gyro_peak_dps)
{
  uint16_t tilt_level = (uint16_t)(tilt_delta * 10.0f);
  uint16_t gyro_level = (uint16_t)gyro_peak_dps;

  return (gyro_level > tilt_level) ? gyro_level : tilt_level;
}

static void MPU6050_FormatFixed1(char *buffer, size_t buffer_size, float value)
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

static void MPU6050_CalculateTilt(void)
{
  float accel_x = (float)mpu6050_task.Accel[0] / 16384.0f;
  float accel_y = (float)mpu6050_task.Accel[1] / 16384.0f;
  float accel_z = (float)mpu6050_task.Accel[2] / 16384.0f;
  const float rad_to_deg = 57.2957795f;

  mpu6050_task.Tilt[0] = atan2f(accel_x, sqrtf((accel_y * accel_y) + (accel_z * accel_z))) * rad_to_deg;
  mpu6050_task.Tilt[1] = atan2f(accel_y, sqrtf((accel_x * accel_x) + (accel_z * accel_z))) * rad_to_deg;
  mpu6050_task.Tilt[2] = atan2f(sqrtf((accel_x * accel_x) + (accel_y * accel_y)), accel_z) * rad_to_deg;
}

static void MPU6050_ActivateAlarmOutputs(void)
{
  beep_task.speed = 100;
  beep_task.beep_active = true;
  beep_task.trigger_flag = 0;

  LED_ON(G_LED_GPIO_PORT, G_LED_GPIO_PIN, LED_LOW_TRIGGER);
  LED_ON(B_LED_GPIO_PORT, B_LED_GPIO_PIN, LED_LOW_TRIGGER);
}

void MPU6050_TaskReset(void)
{
    mpu6050_task.timer = mpu6050_task.cycle;
    mpu6050_task.flag  = 0;
}

void MPU6050_TaskInit(uint32_t mpu6050_task_cycle)
{
    mpu6050_task.cycle = mpu6050_task_cycle;
  mpu6050_task.Tilt[0] = 0.0f;
  mpu6050_task.Tilt[1] = 0.0f;
  mpu6050_task.Tilt[2] = 0.0f;
  mpu6050_task.TiltDelta = 0.0f;
  mpu6050_task.VibrationLevel = 0;
  mpu6050_task.AlarmLatched = 0;
  mpu6050_task.AlarmReportPending = 0;
  mpu6050_last_tilt_x = 0.0f;
  mpu6050_last_tilt_y = 0.0f;
  mpu6050_has_tilt_reference = 0;

    MPU6050_TaskReset();
}

void MPU6050_Task(void)
{
    if(mpu6050_task.flag)
    {
        MPU6050_Handle();
        MPU6050_TaskReset();
    }
}

void MPU6050_Handle(void)
{
    short temp_raw = 0;
  char tilt_delta_text[16] = {0};
  char gyro_peak_text[16] = {0};
  float delta_x = 0.0f;
  float delta_y = 0.0f;
  float gyro_peak_dps = 0.0f;

    MPU6050_ReadAcc(mpu6050_task.Accel);
    MPU6050_ReadGyro(mpu6050_task.Gyro);
    MPU6050_ReadTemp(&temp_raw);
    MPU6050_ReturnTemp(&mpu6050_task.Temp);
    gyro_peak_dps = MPU6050_GetGyroPeakDps();

  if ((mpu6050_task.Accel[0] != 0) || (mpu6050_task.Accel[1] != 0) || (mpu6050_task.Accel[2] != 0))
  {
    MPU6050_CalculateTilt();

    if (mpu6050_has_tilt_reference)
    {
      delta_x = fabsf(mpu6050_task.Tilt[0] - mpu6050_last_tilt_x);
      delta_y = fabsf(mpu6050_task.Tilt[1] - mpu6050_last_tilt_y);
      mpu6050_task.TiltDelta = (delta_x > delta_y) ? delta_x : delta_y;
    }
    else
    {
      mpu6050_task.TiltDelta = 0.0f;
      mpu6050_has_tilt_reference = 1;
    }

    mpu6050_last_tilt_x = mpu6050_task.Tilt[0];
    mpu6050_last_tilt_y = mpu6050_task.Tilt[1];
  }
  else
  {
    mpu6050_task.TiltDelta = 0.0f;
  }

  mpu6050_task.VibrationLevel = MPU6050_CalculateVibrationLevel(mpu6050_task.TiltDelta, gyro_peak_dps);

  if (((mpu6050_task.TiltDelta >= MPU6050_TILT_DELTA_ALARM_THRESHOLD) ||
       (gyro_peak_dps >= MPU6050_GYRO_MOTION_ALARM_THRESHOLD_DPS)) &&
      (mpu6050_task.AlarmLatched == 0U))
  {
    mpu6050_task.AlarmLatched = 1;
    mpu6050_task.AlarmReportPending = 1;
    MPU6050_FormatFixed1(tilt_delta_text, sizeof(tilt_delta_text), mpu6050_task.TiltDelta);
    MPU6050_FormatFixed1(gyro_peak_text, sizeof(gyro_peak_text), gyro_peak_dps);
    printf("\r\n[ALARM] tilt triggered: delta=%s gyro=%s\r\n",
           tilt_delta_text,
           gyro_peak_text);
    MPU6050_ActivateAlarmOutputs();
  }
}

  uint8_t MPU6050_IsAlarmLatched(void)
  {
    return mpu6050_task.AlarmLatched;
  }

  uint8_t MPU6050_IsAlarmReportPending(void)
  {
    return mpu6050_task.AlarmReportPending;
  }

  void MPU6050_ClearAlarmReportPending(void)
  {
    mpu6050_task.AlarmReportPending = 0;
  }

  void MPU6050_ClearAlarm(void)
  {
    mpu6050_task.AlarmLatched = 0;
    mpu6050_task.AlarmReportPending = 0;
    mpu6050_task.TiltDelta = 0.0f;
    mpu6050_task.VibrationLevel = 0;
    mpu6050_last_tilt_x = 0.0f;
    mpu6050_last_tilt_y = 0.0f;
    mpu6050_has_tilt_reference = 0;
    beep_task.beep_active = false;
    beep_task.trigger_flag = 0;

    LED_OFF(G_LED_GPIO_PORT, G_LED_GPIO_PIN, LED_LOW_TRIGGER);
    LED_OFF(B_LED_GPIO_PORT, B_LED_GPIO_PIN, LED_LOW_TRIGGER);
  }

/*****************************END OF FILE***************************************/
