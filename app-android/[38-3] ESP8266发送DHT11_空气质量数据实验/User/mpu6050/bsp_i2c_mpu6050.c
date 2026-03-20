/**
  ******************************************************************************
  * @file       bsp_i2c_mpu6050.c
  * @brief      MPU6050姿态传感器 函数接口
  ******************************************************************************
  */ 
#include "stm32f10x.h"
#include "mpu6050/bsp_i2c_mpu6050.h" 
#include "i2c/bsp_i2c.h"
#include "debug/bsp_debug.h"
#include "usart/usart_com.h"
#include "dwt/bsp_dwt.h" 
#include "led/bsp_gpio_led.h"
#include <math.h>
#include <stdint.h>
/**
  * @brief  配置 MPU6050_INT 中断配置
  * @param  无
  * @retval 无
  */
void MPU6050_INT_NVIC_Config(void)
{
    /* 定义一个 NVIC 结构体 */
    NVIC_InitTypeDef nvic_initstruct = {0};
    
    /* 开启 AFIO 相关的时钟 */
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO,ENABLE); 
    
    /* 配置中断源 */
    nvic_initstruct.NVIC_IRQChannel                     = MPU6050_INT_EXTI_IRQ;
    /* 配置抢占优先级 */
    nvic_initstruct.NVIC_IRQChannelPreemptionPriority   =  1;
    /* 配置子优先级 */
    nvic_initstruct.NVIC_IRQChannelSubPriority          =  0;
    /* 使能配置中断通道 */
    nvic_initstruct.NVIC_IRQChannelCmd                  =  ENABLE;

    NVIC_Init(&nvic_initstruct);

}

/**
  * @brief  初始化控制 MPU6050_INT 的IO
  * @param  无
  * @retval 无
  */
void MPU6050_INT_GPIO_Config(void)
{
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef gpio_initstruct = {0};
      
    /* 开启 MPU6050_INT 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(MPU6050_INT_GPIO_CLK_PORT,ENABLE);
    
    /* IO输出状态初始化控制 */
    GPIO_SetBits(MPU6050_INT_GPIO_PORT,MPU6050_INT_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 浮空输入、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Pin    = MPU6050_INT_GPIO_PIN;
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_IN_FLOATING;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(MPU6050_INT_GPIO_PORT,&gpio_initstruct);
   
}

/**
  * @brief  配置 MPU6050_INT 模式
  * @param  无
  * @retval 无
  */
void MPU6050_INT_Mode_Config(void)
{
    /* 定义一个 EXTI 结构体 */
    EXTI_InitTypeDef exti_initstruct = {0};
   
    /* 开启 AFIO 相关的时钟 */
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO,ENABLE); 
    
	/* 选择中断信号源*/
    GPIO_EXTILineConfig(MPU6050_INT_EXTI_PORTSOURCE,MPU6050_INT_EXTI_PINSOURCE);
    
    /* 选择中断LINE */
    exti_initstruct.EXTI_Line       = MPU6050_INT_EXTI_LINE;
    /* 选择中断模式*/
    exti_initstruct.EXTI_Mode       = EXTI_Mode_Interrupt;
    /* 选择触发方式*/
    exti_initstruct.EXTI_Trigger    = EXTI_Trigger_Falling;
    /* 使能中断*/
    exti_initstruct.EXTI_LineCmd    = ENABLE;
    
    EXTI_Init(&exti_initstruct);

}

/**
  * @brief  MPU6050_INT 初始化
  * @param  无
  * @retval 无
  */
void MPU6050_INT_Init(void)
{
    /* 配置 MPU6050_INT 中断配置 */
    MPU6050_INT_NVIC_Config();
    
    /* 对应的 GPIO 的配置 */
    MPU6050_INT_GPIO_Config();
    
    /* 配置 MPU6050_INT 模式 */
    MPU6050_INT_Mode_Config();
    
}

/**
  * @brief  MPU6050_INT 中断函数
  * @param  无
  * @retval 无
  */
void MPU6050_INT_EXTI_IRQHANDLER(void)
{
    //确保是否产生了EXTI Line中断
	if(EXTI_GetITStatus(MPU6050_INT_EXTI_LINE) != RESET) 
	{		
        //回调函数
        MPU6050_data_ready_cb();
        //翻转绿灯
        LED_TOGGLE(G_LED_GPIO_PORT,G_LED_GPIO_PIN);
        //清除中断标志位
		EXTI_ClearITPendingBit(MPU6050_INT_EXTI_LINE);     
	}  
}

/**
  * @brief  写入 MPU6050 指定寄存器的值（单字节）
  *	@param	write_addr：写入地址
  *	@param	byte：具体数据
  * @retval SUCCESS 写入成功   ERROR：写入失败
  */
ErrorStatus MPU6050_WriteReg(uint8_t write_addr,uint8_t byte)
{  
    ErrorStatus temp = ERROR;

    /* 检测总线是否繁忙和发出开始信号*/
    temp = IIC_Start(IIC_I2C2);
    if(temp != SUCCESS)
    {
        return temp;
    }

    /* 呼叫从机,地址配对*/
    temp = IIC_AddressMatching(IIC_I2C2,MPU6050_AD0D_SLAVER_ARRD,IIC_WRITE);
    if(temp != SUCCESS)
    {
        printf("地址失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
        return temp;
    }
    
    /* 写入地址*/
    temp = IIC_SendData(IIC_I2C2,write_addr);
    if(temp != SUCCESS)
    {
        printf("写入寄存器地址失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
        return temp;
    }
    
    /* 具体数据*/
    temp = IIC_SendData(IIC_I2C2,byte);
    if(temp != SUCCESS)
    {
        printf("具体数据失败");
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
        return temp;
    }
    
    /* 释放总线并发出停止信号 */
    IIC_Stop(IIC_I2C2);
    return SUCCESS;  
}

/**
  * @brief  读取 MPU6050 指定寄存器的值（单字节）
  * @param  read_addr：读取地址 
  * @note   无
  * @retval 寄存器的值
  */
uint8_t  MPU6050_ReadReg(uint8_t read_addr)
{
    uint8_t data;
    ErrorStatus temp = ERROR;

    /* 检测总线是否繁忙和发出开始信号*/
    temp = IIC_Start(IIC_I2C2);
    if(temp != SUCCESS)
    {
        printf("发出开始信号失败");
    }

    /* 呼叫从机,地址配对*/
    temp = IIC_AddressMatching(IIC_I2C2,MPU6050_AD0D_SLAVER_ARRD,IIC_WRITE);
    if(temp != SUCCESS)
    {
        printf("写地址失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
    }
    
    /* 发送要读的地址*/
    temp = IIC_SendData(IIC_I2C2,read_addr);
    if(temp != SUCCESS)
    {
        printf("读指令/数据失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
    }

    /* 再次发送起始信号 */
    temp = IIC_Restart(IIC_I2C2);
    if(temp != SUCCESS)
    {
        printf("再次发送起始信号失败");
    }

    /* 呼叫从机,读操作 */
    temp = IIC_AddressMatching(IIC_I2C2,MPU6050_AD0D_SLAVER_ARRD,IIC_READ);
    if(temp != SUCCESS)
    {
        printf("读地址失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
    }

    /* 发送非应答信号 */
    I2C_AcknowledgeConfig(IIC_I2C2, DISABLE);

    /* 发送停止信号 */
    I2C_GenerateSTOP(IIC_I2C2, ENABLE);

    /* 读一个字节 */
    temp = IIC_ReceiveData(IIC_I2C2,&data);
    if(temp != SUCCESS)
    {
        printf("读一个字节失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
    }

	/* 重新开启应答信号 */
	I2C_AcknowledgeConfig(IIC_I2C2,ENABLE);    
    
    return data;      
}

/**
  * @brief  连续读取 MPU6050 寄存器的值
  * @param  read_addr：读取地址 
  * @param  buffer：存储读出数据的指针 
  * @param  size：读取数据长度 
  * @note   无
  * @retval 无
  */
int MPU6050_ReadBuffer(uint8_t slave_addr, uint8_t read_addr, uint32_t size, uint8_t* buffer)
{
    ErrorStatus temp = ERROR;

    /* 检测总线是否繁忙和发出开始信号*/
    temp = IIC_Start(IIC_I2C2);
    if(temp != SUCCESS)
    {
        printf("发出开始信号失败");
        return 1;
    }

    /* 呼叫从机,地址配对*/
    temp = IIC_AddressMatching(IIC_I2C2,slave_addr,IIC_WRITE);
    if(temp != SUCCESS)
    {
        printf("写地址失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
        return 1;
    }
    /*通过重新设置PE位清除EV6事件 */
    I2C_Cmd(IIC_I2C2, ENABLE);
    
    /* 发送要读的地址*/
    temp = IIC_SendData(IIC_I2C2,read_addr);
    if(temp != SUCCESS)
    {
        printf("读指令/数据失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
        return 1;
    }

    /* 再次发送起始信号 */
    temp = IIC_Restart(IIC_I2C2);
    if(temp != SUCCESS)
    {
        printf("再次发送起始信号失败");
        return 1;
    }

    /* 呼叫从机,读操作 */
    temp = IIC_AddressMatching(IIC_I2C2,slave_addr,IIC_READ);
    if(temp != SUCCESS)
    {
        printf("读地址失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
        return 1;
    }

  /* 循环读取数据 */
  while(size)  
  {
    /* 是否是最后一个字节，若是则发送非应答信号 */
    if(size == 1)
    {
      /* 发送非应答信号 */
      I2C_AcknowledgeConfig(IIC_I2C2, DISABLE);
      
      /* 发送停止信号 */
      I2C_GenerateSTOP(IIC_I2C2, ENABLE);
    }

    /* 对EV7进行测试并清除 */
    if(I2C_CheckEvent(IIC_I2C2, I2C_EVENT_MASTER_BYTE_RECEIVED))  
    {      
      /* 从从属设备读取一个字节 */
      *buffer = I2C_ReceiveData(IIC_I2C2);

      /* 指向将保存读取的字节的下一个位置 */
      buffer++; 
      
      /* 递减读取字节计数器 */
      size--;        
    }   
  }

	/* 重新开启应答信号 */
	I2C_AcknowledgeConfig(IIC_I2C2,ENABLE);    
    return 0;
}

/**
  * @brief  对 MPU6050 写入数据
  * @param  write_addr：写入首地址 
  * @param  buffer：存储写入数据的指针 
  * @param  size：写入数据长度 
  * @retval 无
  */
int MPU6050_WriteBuffer(uint8_t slave_addr, uint8_t write_addr, uint32_t size, uint8_t* buffer)
{
    ErrorStatus temp = ERROR;

    /* 检测总线是否繁忙和发出开始信号*/
    temp = IIC_Start(IIC_I2C2);
    if(temp != SUCCESS)
    {
        printf("发出开始信号失败");
        return 1;
    }

    /* 呼叫从机,地址配对*/
    temp = IIC_AddressMatching(IIC_I2C2,slave_addr,IIC_WRITE);
    if(temp != SUCCESS)
    {
        printf("地址失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
        return 1;
    }
    
    /* 写指令/数据*/
    temp = IIC_SendData(IIC_I2C2,write_addr);
    if(temp != SUCCESS)
    {
        printf("写指令/数据失败");
        
        /* 释放总线并发出停止信号 */
        IIC_Stop(IIC_I2C2);
        return 1;
    }
 
    /* 具体指令/数据 */
    for(uint32_t i = 0;i<size;i++)
    {
        temp = IIC_SendData(IIC_I2C2,*buffer++);
        if(temp != SUCCESS)
        {
            printf("具体指令/数据失败");
            return 1;
        }
    }
    
    /* 释放总线并发出停止信号 */
    IIC_Stop(IIC_I2C2);
    return 0;
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
    IIC_DELAY_US(1000000); // 1s,这里的延时很重要,上电后延时，没有错误的冗余设计
    
    /* 设备检验*/
    while(1)
    {
        /* 检验 ID */
        if (MPU6050_ID == MPU6050_ReadID())
        {
            printf("\r\n 检测到 MPU6050 !\r\n");
            printf("MPU6050_ID：0x%X\r\n",MPU6050_ID);
            break;
        }
    }
    
    MPU6050_INT_Init();//中断引脚配置
    
    MPU6050_WriteReg(MPU6050_RA_PWR_MGMT_1, 0x00);	    //解除休眠状态
	MPU6050_WriteReg(MPU6050_RA_SMPLRT_DIV , 0x07);	    //陀螺仪采样率，1KHz
	MPU6050_WriteReg(MPU6050_RA_CONFIG , 0x06);	        //低通滤波器的设置，截止频率是1K，带宽是5K
	MPU6050_WriteReg(MPU6050_RA_ACCEL_CONFIG , 0x00);	//配置加速度传感器工作在2G模式，不自检
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
    
    MPU6050_ReadBuffer(MPU6050_AD0D_SLAVER_ARRD,MPU6050_ACC_OUT,6,buf);
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
    
    MPU6050_ReadBuffer(MPU6050_AD0D_SLAVER_ARRD,MPU6050_GYRO_OUT,6,buf);
    GyroData[0] = (buf[0] << 8) | buf[1];
    GyroData[1] = (buf[2] << 8) | buf[3];
    GyroData[2] = (buf[4] << 8) | buf[5];

}

/**
  * @brief   读取MPU6050的原始温度数据
  * @param   TempData:存放原始温度数据的指针
  * @retval  无
  */
void MPU6050_ReadTemp(short *TempData)
{
    uint8_t buf[2];
    
    MPU6050_ReadBuffer(MPU6050_AD0D_SLAVER_ARRD,MPU6050_RA_TEMP_OUT_H,2,buf);
    *TempData = (buf[0] << 8) | buf[1];
    
}
/**
  * @brief   读取MPU6050的温度数据，转化成摄氏度
  * @param   Temperature:存放摄氏度的指针
  * @retval  无 
  */
void MPU6050_ReturnTemp(float *Temperature)
{
	short buffer;
	uint8_t buf[2];
	
	MPU6050_ReadBuffer(MPU6050_AD0D_SLAVER_ARRD,MPU6050_RA_TEMP_OUT_H,2,buf);     //读取温度值
    buffer= (buf[0] << 8) | buf[1];	
	*Temperature=((double) buffer/340.0)+36.53;

}

/* 震动检测相关全局变量 */
volatile uint8_t shake_detected_flag = 0;
volatile uint32_t shake_count = 0;

/* 静态变量用于存储基准值 */
static short baseline_acc_x = 0, baseline_acc_y = 0, baseline_acc_z = 0;
static short baseline_gyro_x = 0, baseline_gyro_y = 0, baseline_gyro_z = 0;
static uint8_t baseline_initialized = 0;

/**
  * @brief   读取MPU6050的所有传感器数据
  * @param   sensor_data:存放传感器数据的结构体指针
  * @retval  无
  */
void MPU6050_ReadSensorData(MPU6050_SensorData_TypeDef *sensor_data)
{
    short acc_data[3];
    short gyro_data[3];
    
    /* 读取加速度数据 */
    MPU6050_ReadAcc(acc_data);
    sensor_data->acc_x = acc_data[0];
    sensor_data->acc_y = acc_data[1];
    sensor_data->acc_z = acc_data[2];
    
    /* 读取角速度数据 */
    MPU6050_ReadGyro(gyro_data);
    sensor_data->gyro_x = gyro_data[0];
    sensor_data->gyro_y = gyro_data[1];
    sensor_data->gyro_z = gyro_data[2];
    
    /* 计算加速度幅值 */
    sensor_data->acc_magnitude = (float)sqrt((float)(acc_data[0]*acc_data[0] + 
                                                      acc_data[1]*acc_data[1] + 
                                                      acc_data[2]*acc_data[2]));
    
    /* 计算角速度幅值 */
    sensor_data->gyro_magnitude = (float)sqrt((float)(gyro_data[0]*gyro_data[0] + 
                                                       gyro_data[1]*gyro_data[1] + 
                                                       gyro_data[2]*gyro_data[2]));
}

/**
  * @brief   初始化基准值（用于震动检测）
  * @param   无
  * @retval  无
  */
static void MPU6050_InitBaseline(void)
{
    MPU6050_SensorData_TypeDef sensor_data;
    short acc_x_sum = 0, acc_y_sum = 0, acc_z_sum = 0;
    short gyro_x_sum = 0, gyro_y_sum = 0, gyro_z_sum = 0;
    const uint8_t samples = 100;  /* 采样100次取平均值 */
    
    for(uint8_t i = 0; i < samples; i++)
    {
        MPU6050_ReadSensorData(&sensor_data);
        acc_x_sum += sensor_data.acc_x;
        acc_y_sum += sensor_data.acc_y;
        acc_z_sum += sensor_data.acc_z;
        gyro_x_sum += sensor_data.gyro_x;
        gyro_y_sum += sensor_data.gyro_y;
        gyro_z_sum += sensor_data.gyro_z;
        DWT_DelayUs(1000);  /* 延时1ms */
    }
    
    /* 计算平均值作为基准值 */
    baseline_acc_x = acc_x_sum / samples;
    baseline_acc_y = acc_y_sum / samples;
    baseline_acc_z = acc_z_sum / samples;
    baseline_gyro_x = gyro_x_sum / samples;
    baseline_gyro_y = gyro_y_sum / samples;
    baseline_gyro_z = gyro_z_sum / samples;
    baseline_initialized = 1;
}

/**
  * @brief   检测震动严重程度
  * @param   sensor_data:当前传感器数据
  * @retval  震动严重程度
  */
Shake_Severity_TypeDef MPU6050_DetectShake(MPU6050_SensorData_TypeDef *sensor_data)
{
    /* 如果还没有初始化基准值，先初始化 */
    if(!baseline_initialized)
    {
        MPU6050_InitBaseline();
    }
    
    /* 计算与基准值的差值 */
    short delta_acc_x = abs(sensor_data->acc_x - baseline_acc_x);
    short delta_acc_y = abs(sensor_data->acc_y - baseline_acc_y);
    short delta_acc_z = abs(sensor_data->acc_z - baseline_acc_z);
    short delta_gyro_x = abs(sensor_data->gyro_x - baseline_gyro_x);
    short delta_gyro_y = abs(sensor_data->gyro_y - baseline_gyro_y);
    short delta_gyro_z = abs(sensor_data->gyro_z - baseline_gyro_z);
    
    /* 计算总的加速度和角速度变化量 */
    float acc_delta_total = (float)sqrt((float)(delta_acc_x*delta_acc_x + 
                                                delta_acc_y*delta_acc_y + 
                                                delta_acc_z*delta_acc_z));
    float gyro_delta_total = (float)sqrt((float)(delta_gyro_x*delta_gyro_x + 
                                                 delta_gyro_y*delta_gyro_y + 
                                                 delta_gyro_z*delta_gyro_z));
    
    /* 震动检测阈值 */
    const float shake_threshold_mild = 500.0f;      /* 轻微震动阈值 */
    const float shake_threshold_moderate = 2000.0f; /* 中等震动阈值 */
    const float shake_threshold_severe = 5000.0f;   /* 严重震动阈值 */
    
    /* 判断震动严重程度 */
    if(acc_delta_total > shake_threshold_severe || gyro_delta_total > shake_threshold_severe * 10)
    {
        shake_detected_flag = 1;
        shake_count++;
        return SHAKE_SEVERE;
    }
    else if(acc_delta_total > shake_threshold_moderate || gyro_delta_total > shake_threshold_moderate * 10)
    {
        shake_detected_flag = 1;
        shake_count++;
        return SHAKE_MODERATE;
    }
    else if(acc_delta_total > shake_threshold_mild || gyro_delta_total > shake_threshold_mild * 10)
    {
        shake_detected_flag = 1;
        shake_count++;
        return SHAKE_MILD;
    }
    else
    {
        shake_detected_flag = 0;
        return SHAKE_STABLE;
    }
}

/**
  * @brief   清除震动检测标志
  * @param   无
  * @retval  无
  */
void MPU6050_ClearShakeFlag(void)
{
    shake_detected_flag = 0;
}

/**
  * @brief   获取震动次数
  * @param   无
  * @retval  震动次数
  */
uint32_t MPU6050_GetShakeCount(void)
{
    return shake_count;
}

/**
  * @brief   重置震动计数器
  * @param   无
  * @retval  无
  */
void MPU6050_ResetShakeCount(void)
{
    shake_count = 0;
}

/*********************************************END OF FILE**********************/




