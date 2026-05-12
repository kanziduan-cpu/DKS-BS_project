/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "mpu6050/bsp_i2c_mpu6050.h" 
#include "i2c/bsp_i2c.h"
#include "debug/bsp_debug.h"
#include "usart/usart_com.h"
#include "dwt/bsp_dwt.h" 
#include <string.h>

#define MPU6050_I2C_PORT IIC_I2C1

static uint8_t mpu6050_slave_addr = MPU6050_AD0D_SLAVER_ARRD;

static void MPU6050_RecoverBus(void)
{
    I2C_GenerateSTOP(MPU6050_I2C_PORT, ENABLE);
    I2C_AcknowledgeConfig(MPU6050_I2C_PORT, ENABLE);
    I2C_Cmd(MPU6050_I2C_PORT, DISABLE);
    I2C_SoftwareResetCmd(MPU6050_I2C_PORT, ENABLE);
    IIC_DELAY_US(10);
    I2C_SoftwareResetCmd(MPU6050_I2C_PORT, DISABLE);
    IIC_Init();
    IIC_DELAY_US(1000);
}

static ErrorStatus MPU6050_WriteRegInternal(uint8_t write_addr,uint8_t byte)
{
    ErrorStatus temp = ERROR;

    temp = IIC_Start(MPU6050_I2C_PORT);
    if(temp != SUCCESS)
    {
        return temp;
    }

    temp = IIC_AddressMatching(MPU6050_I2C_PORT,mpu6050_slave_addr,IIC_WRITE);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    temp = IIC_SendData(MPU6050_I2C_PORT,write_addr);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    temp = IIC_SendData(MPU6050_I2C_PORT,byte);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    IIC_Stop(MPU6050_I2C_PORT);
    return SUCCESS;
}

static ErrorStatus MPU6050_ReadRegInternal(uint8_t read_addr, uint8_t *data)
{
    ErrorStatus temp = ERROR;

    temp = IIC_Start(MPU6050_I2C_PORT);
    if(temp != SUCCESS)
    {
        return temp;
    }

    temp = IIC_AddressMatching(MPU6050_I2C_PORT,mpu6050_slave_addr,IIC_WRITE);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    temp = IIC_SendData(MPU6050_I2C_PORT,read_addr);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    temp = IIC_Restart(MPU6050_I2C_PORT);
    if(temp != SUCCESS)
    {
        return temp;
    }

    temp = IIC_AddressMatching(MPU6050_I2C_PORT,mpu6050_slave_addr,IIC_READ);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    I2C_AcknowledgeConfig(MPU6050_I2C_PORT, DISABLE);
    I2C_GenerateSTOP(MPU6050_I2C_PORT, ENABLE);

    temp = IIC_ReceiveData(MPU6050_I2C_PORT,data);
    I2C_AcknowledgeConfig(MPU6050_I2C_PORT,ENABLE);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    return SUCCESS;
}

static ErrorStatus MPU6050_ReadBufferInternal(uint8_t read_addr, uint8_t* buffer, uint32_t size)
{
    ErrorStatus temp = ERROR;

    temp = IIC_Start(MPU6050_I2C_PORT);
    if(temp != SUCCESS)
    {
        return temp;
    }

    temp = IIC_AddressMatching(MPU6050_I2C_PORT,mpu6050_slave_addr,IIC_WRITE);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    I2C_Cmd(MPU6050_I2C_PORT, ENABLE);

    temp = IIC_SendData(MPU6050_I2C_PORT,read_addr);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    temp = IIC_Restart(MPU6050_I2C_PORT);
    if(temp != SUCCESS)
    {
        return temp;
    }

    temp = IIC_AddressMatching(MPU6050_I2C_PORT,mpu6050_slave_addr,IIC_READ);
    if(temp != SUCCESS)
    {
        IIC_Stop(MPU6050_I2C_PORT);
        return temp;
    }

    while(size)
    {
        if(size == 1)
        {
            I2C_AcknowledgeConfig(MPU6050_I2C_PORT, DISABLE);
            I2C_GenerateSTOP(MPU6050_I2C_PORT, ENABLE);
        }

        if(I2C_CheckEvent(MPU6050_I2C_PORT, I2C_EVENT_MASTER_BYTE_RECEIVED))
        {
            *buffer = I2C_ReceiveData(MPU6050_I2C_PORT);
            buffer++;
            size--;
        }
    }

    I2C_AcknowledgeConfig(MPU6050_I2C_PORT,ENABLE);
    return SUCCESS;
}

static ErrorStatus MPU6050_SelectSlaveAddress(void)
{
    const uint8_t candidate_addrs[] = {MPU6050_AD0D_SLAVER_ARRD, MPU6050_AD0U_SLAVER_ARRD};
    uint32_t index = 0;

    for (index = 0; index < (sizeof(candidate_addrs) / sizeof(candidate_addrs[0])); index++)
    {
        mpu6050_slave_addr = candidate_addrs[index];

        if ((IIC_CheckDevice(MPU6050_I2C_PORT, mpu6050_slave_addr) == SUCCESS) &&
            (MPU6050_ReadReg(MPU6050_WHO_AM_I) == MPU6050_ID))
        {
            return SUCCESS;
        }
    }

    return ERROR;
}


/**
    * @brief  写入 MPU6050 指定寄存器的值（单字节）
    * @param  write_addr：写入地址
    * @param  byte：具体数据
    * @retval SUCCESS 写入成功   ERROR：写入失败
    */
ErrorStatus MPU6050_WriteReg(uint8_t write_addr,uint8_t byte)
{  
    ErrorStatus temp = MPU6050_WriteRegInternal(write_addr, byte);

    if (temp == SUCCESS)
    {
        return SUCCESS;
    }

    MPU6050_RecoverBus();
    temp = MPU6050_WriteRegInternal(write_addr, byte);
    if (temp != SUCCESS)
    {
        printf("\r\nMPU6050 write register 0x%02X failed\r\n", write_addr);
    }

    return temp;
}

/**
    * @brief  读取 MPU6050 指定寄存器的值（单字节）
    * @param  read_addr：读取地址 
    * @note   无
    * @retval 寄存器的值
    */
uint8_t  MPU6050_ReadReg(uint8_t read_addr)
{
    uint8_t data = 0;
    ErrorStatus temp = MPU6050_ReadRegInternal(read_addr, &data);

    if (temp == SUCCESS)
    {
        return data;
    }

    MPU6050_RecoverBus();
    temp = MPU6050_ReadRegInternal(read_addr, &data);
    if (temp != SUCCESS)
    {
        printf("\r\nMPU6050 read register 0x%02X failed\r\n", read_addr);
        return 0;
    }

    return data;
}

/**
    * @brief  连续读取 MPU6050 寄存器的值
    * @param  read_addr：读取地址 
    * @param  buffer：存储读取出数据的指针 
    * @param  size：读取数据长度
    * @note   无
    * @retval 无
    */
void MPU6050_ReadBuffer(uint8_t read_addr, uint8_t* buffer, uint32_t size)
{
    ErrorStatus temp = MPU6050_ReadBufferInternal(read_addr, buffer, size);

    if (temp == SUCCESS)
    {
        return;
    }

    MPU6050_RecoverBus();
    temp = MPU6050_ReadBufferInternal(read_addr, buffer, size);
    if (temp != SUCCESS)
    {
        memset(buffer, 0, size);
            printf("\r\nMPU6050 burst read register 0x%02X failed\r\n", read_addr);
    }
}

/**
    * @brief  对 MPU6050 写入数据
    * @param  write_addr：写入首地址 
    * @param  buffer：存储写入数据的指针 
    * @param  size：写入数据长度
    * @retval 无
    */
void MPU6050_WriteBuffer(uint8_t write_addr, uint8_t* buffer, uint32_t size)
{
    ErrorStatus temp = ERROR;

    /* 检查总线是否空闲并发出开始信号*/
    temp = IIC_Start(MPU6050_I2C_PORT);
    if(temp != SUCCESS)
    {
        printf("I2C start failed");
    }

    /* 调用从机,地址匹配*/
    temp = IIC_AddressMatching(MPU6050_I2C_PORT,mpu6050_slave_addr,IIC_WRITE);
    if(temp != SUCCESS)
    {
        printf("I2C address match failed");
        
        /* 释放总线并发出停止信号*/
        IIC_Stop(MPU6050_I2C_PORT);
    }
    
    /* 写地址/数据*/
    temp = IIC_SendData(MPU6050_I2C_PORT,write_addr);
    if(temp != SUCCESS)
    {
        printf("I2C write address/data failed");
        
        /* 释放总线并发出停止信号*/
        IIC_Stop(MPU6050_I2C_PORT);
    }
 
    /* 具体指令/数据 */
    for(uint32_t i = 0;i<size;i++)
    {
        temp = IIC_SendData(MPU6050_I2C_PORT,*buffer++);
        if(temp != SUCCESS)
        {
            printf("I2C write payload failed");
        }
    }
    
    /* 释放总线并发出停止信号*/
    IIC_Stop(MPU6050_I2C_PORT);
     
}


/**
    * @brief  读取 MPU6050 ID
    * @param  无  
    * @note   无
    * @retval MPU6050 ID
    */
uint8_t MPU6050_ReadID(void)
{
    return MPU6050_ReadReg(MPU6050_WHO_AM_I);
}

/**
    * @brief  MPU6050 初始化 
    * @param  无
    * @retval 无
    */
void MPU6050_Init(void)
{
    IIC_DELAY_US(10000);
    IIC_DELAY_US(1000000); // 1s,这里的延时很重要,上电后延时，没有错误的冗余设计
    
    /* 设备检验*/
    while(1)
    {
        if (MPU6050_SelectSlaveAddress() == SUCCESS)
        {
            printf("\r\nMPU6050 detected\r\n");
            printf("MPU6050_ADDR: 0x%X\r\n", mpu6050_slave_addr);
            printf("MPU6050_ID: 0x%X\r\n", MPU6050_ReadID());
            break;
        }

        printf("\r\nMPU6050 not detected, check wiring: SCL->PB6 SDA->PB7 AD0->GND(0x68) or 3V3(0x69), INT optional.\r\n");
        IIC_DELAY_US(500000);
    }

    MPU6050_WriteReg(MPU6050_RA_PWR_MGMT_1, 0x00);     //解除休眠状态
	MPU6050_WriteReg(MPU6050_RA_SMPLRT_DIV , 0x07);     //陀螺仪采样率，1KHz
    MPU6050_WriteReg(MPU6050_RA_CONFIG , 0x03);        //提高低通滤波带宽，便于动作检测
	MPU6050_WriteReg(MPU6050_RA_ACCEL_CONFIG , 0x00);   //配置加速度传感器工作在2G模式，不自检
	MPU6050_WriteReg(MPU6050_RA_GYRO_CONFIG, 0x18);     //陀螺仪自检及测量范围，典型值：0x18(不自检，2000deg/s)
}

/**
  * @brief   读取MPU6050的加速度数据
  * @param   AccData:存放加速度数据的指针
  * @retval  无
  */
void MPU6050_ReadAcc(short *AccData)
{
    uint8_t buf[6];
    
    MPU6050_ReadBuffer(MPU6050_ACC_OUT,buf,6);
    AccData[0] = (buf[0] << 8) | buf[1];
    AccData[1] = (buf[2] << 8) | buf[3];
    AccData[2] = (buf[4] << 8) | buf[5];

}

/**
    * @brief   读取MPU6050的角速度数据
    * @param   GyroData:存放角速度数据的指针
    * @retval  无 
    */
void MPU6050_ReadGyro(short *GyroData)
{
    uint8_t buf[6];
    
    MPU6050_ReadBuffer(MPU6050_GYRO_OUT,buf,6);
    GyroData[0] = (buf[0] << 8) | buf[1];
    GyroData[1] = (buf[2] << 8) | buf[3];
    GyroData[2] = (buf[4] << 8) | buf[5];

}

/**
    * @brief   读取MPU6050的温度数据（原始值）
    * @param   TempData:存放温度数据的指针
    * @retval  无 
    */
void MPU6050_ReadTemp(short *TempData)
{
    uint8_t buf[2];
    
    MPU6050_ReadBuffer(MPU6050_RA_TEMP_OUT_H,buf,2);
    *TempData = (buf[0] << 8) | buf[1];
    
}
/**
    * @brief   读取MPU6050的温度值（摄氏度）
    * @param   Temperature:存放温度值的指针
    * @retval  无 
    */
void MPU6050_ReturnTemp(float *Temperature)
{
	short buffer;
	uint8_t buf[2];
	
    MPU6050_ReadBuffer(MPU6050_RA_TEMP_OUT_H,buf,2);     // 读取温度原始值
    buffer= (buf[0] << 8) | buf[1];	
	*Temperature=((double) buffer/340.0)+36.53;

}
/*********************************************END OF FILE**********************/
