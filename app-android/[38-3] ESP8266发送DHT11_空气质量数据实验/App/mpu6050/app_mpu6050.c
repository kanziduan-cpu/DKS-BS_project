/**
  ******************************************************************************
  * @file       app_mpu6050.c
  * @author     embedfire
  * @version     V1.0
  * @date        2025
  * @brief      MPU6050姿态传感器 应用层功能接口
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

#include "mpu6050/app_mpu6050.h"
#include "led/bsp_gpio_led.h"
#include "usart/usart_com.h"
#include "systick/bsp_systick.h"
#include "mpu6050/bsp_i2c_mpu6050.h" 
#include "oled/app_oled.h"

MPU6050_TaskInfo mpu6050_task  = {0};


/**
  * @brief  MPU6050姿态传感器 计数复位
  * @param  无
  * @retval 无
  */
void MPU6050_TaskReset(void)
{
    mpu6050_task.timer = mpu6050_task.cycle;
    mpu6050_task.flag  = 0;

}

/**
  * @brief  MPU6050姿态传感器 任务初始化
  * @param  mpu6050_task_cycle: 任务轮询周期 单位ms(可修改系统节拍定时器)
  * @retval 无
  */
void MPU6050_TaskInit(uint32_t mpu6050_task_cycle)
{
    mpu6050_task.cycle = mpu6050_task_cycle;

    MPU6050_TaskReset();
}

/**
  * @brief  MPU6050姿态传感器 任务
  * @param  无
  * @retval 无
  */
void MPU6050_Task(void)
{
    if(mpu6050_task.flag)
    {
        MPU6050_Handle();
        MPU6050_TaskReset();
    }
}

/**
  * @brief  MPU6050姿态传感器反馈信息处理函数
  * @param  无
  * @retval 无
  */
void MPU6050_Handle(void)
{
    MPU6050_ReadAcc(mpu6050_task.Accel);
    MPU6050_ReadGyro(mpu6050_task.Gyro);
    MPU6050_ReturnTemp(&mpu6050_task.Temp);  
    
    printf("\r\n加速度： %8d%8d%8d    ",mpu6050_task.Accel[0],mpu6050_task.Accel[1],mpu6050_task.Accel[2]);
    printf("陀螺仪： %8d%8d%8d    ",mpu6050_task.Gyro[0],mpu6050_task.Gyro[1],mpu6050_task.Gyro[2]);
    printf("温度： %8.2f°C",mpu6050_task.Temp);
                          
    snprintf(mpu6050_task.Acceleration, sizeof(mpu6050_task.Acceleration), "%6d%6d%6d", mpu6050_task.Accel[0],mpu6050_task.Accel[1],mpu6050_task.Accel[2]);
    snprintf(mpu6050_task.Gyroscope, sizeof(mpu6050_task.Gyroscope), "%6d%6d%6d", mpu6050_task.Gyro[0],mpu6050_task.Gyro[1],mpu6050_task.Gyro[2]);
    snprintf(mpu6050_task.Temperature, sizeof(mpu6050_task.Temperature), "%.2f C", mpu6050_task.Temp);
    
    //刷新屏幕显示
    oled_task.refresh_flag=1;

}

/*****************************END OF FILE***************************************/
