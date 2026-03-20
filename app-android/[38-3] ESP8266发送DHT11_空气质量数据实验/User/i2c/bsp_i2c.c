/**
  ******************************************************************************
  * @file       bsp_i2c.c
  * @author     embedfire
  * @version    V1.1
  * @date       2025
  * @brief      硬件IIC函数接口
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
  
#include "i2c/bsp_i2c.h"
#include "dwt/bsp_dwt.h"  

/**
  * @brief  配置 HARD_IIC 用到的I/O口
  * @param  无  
  * @note   无
  * @retval 无
  */
void IIC_PinConfig(void)
{
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef gpio_initstruct = {0};
    
#if 1
    /* 开启 HARD_IIC 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(IIC_I2C1_SCL_GPIO_CLK_PORT,ENABLE);

    GPIO_SetBits(IIC_I2C1_SCL_GPIO_PORT,IIC_I2C1_SCL_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 开漏复用输出、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_OD;
    gpio_initstruct.GPIO_Pin    = IIC_I2C1_SCL_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(IIC_I2C1_SCL_GPIO_PORT,&gpio_initstruct); 
    
    /* 开启 IIC 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(IIC_I2C1_SDA_GPIO_CLK_PORT,ENABLE);

    GPIO_SetBits(IIC_I2C1_SDA_GPIO_PORT,IIC_I2C1_SDA_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 开漏复用输出、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_OD;
    gpio_initstruct.GPIO_Pin    = IIC_I2C1_SDA_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(IIC_I2C1_SDA_GPIO_PORT,&gpio_initstruct); 
#endif

#if 1   
    /* 开启 HARD_IIC 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(IIC_I2C2_SCL_GPIO_CLK_PORT,ENABLE);
    RCC_APB1PeriphClockCmd(IIC_I2C2_CLK_PORT,ENABLE);//实测I2C2的外设时钟需要GPIO初始化前开启，才能使用I2C2
    GPIO_SetBits(IIC_I2C2_SCL_GPIO_PORT,IIC_I2C2_SCL_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 开漏复用输出、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_OD;
    gpio_initstruct.GPIO_Pin    = IIC_I2C2_SCL_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(IIC_I2C2_SCL_GPIO_PORT,&gpio_initstruct); 
    
    /* 开启 IIC 相关的GPIO外设/端口时钟 */
    RCC_APB2PeriphClockCmd(IIC_I2C2_SDA_GPIO_CLK_PORT,ENABLE);

    GPIO_SetBits(IIC_I2C2_SDA_GPIO_PORT,IIC_I2C2_SDA_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为 开漏复用输出、设置GPIO速率为50MHz*/
    gpio_initstruct.GPIO_Mode   = GPIO_Mode_AF_OD;
    gpio_initstruct.GPIO_Pin    = IIC_I2C2_SDA_GPIO_PIN;
    gpio_initstruct.GPIO_Speed  = GPIO_Speed_50MHz;
    GPIO_Init(IIC_I2C2_SDA_GPIO_PORT,&gpio_initstruct); 
#endif

}

/**
  * @brief  HARD_IIC 工作模式配置
  * @param  无
  * @retval 无
  */
void IIC_Mode_Config(void)
{
    /* 定义一个 HARD_IIC 结构体 */
    I2C_InitTypeDef i2c_initstruct = {0};

#if 1     
    /* 开启 HARD_IIC 相关的GPIO外设/端口时钟 */
    RCC_APB1PeriphClockCmd(IIC_I2C1_CLK_PORT,ENABLE);
    
    i2c_initstruct.I2C_Mode                 = I2C_Mode_I2C;
    i2c_initstruct.I2C_DutyCycle            = I2C_DutyCycle_2;                 //高电平数据稳定，低电平数据变化 SCL 时钟线的占空比
    i2c_initstruct.I2C_OwnAddress1          = IIC_I2C1_OWN_ADDRESS7;
    i2c_initstruct.I2C_Ack                  = I2C_Ack_Enable;
    i2c_initstruct.I2C_AcknowledgedAddress  = I2C_AcknowledgedAddress_7bit;      
    i2c_initstruct.I2C_ClockSpeed           = IIC_SPEED;                        //通信速率
    
    I2C_Init(IIC_I2C1, &i2c_initstruct);
#endif

#if 1     
    /* 开启 HARD_IIC 相关的GPIO外设/端口时钟 */
    RCC_APB1PeriphClockCmd(IIC_I2C2_CLK_PORT,ENABLE);
    
    i2c_initstruct.I2C_Mode                 = I2C_Mode_I2C;
    i2c_initstruct.I2C_DutyCycle            = I2C_DutyCycle_2;                 //高电平数据稳定，低电平数据变化 SCL 时钟线的占空比
    i2c_initstruct.I2C_OwnAddress1          = IIC_I2C2_OWN_ADDRESS7;
    i2c_initstruct.I2C_Ack                  = I2C_Ack_Enable;
    i2c_initstruct.I2C_AcknowledgedAddress  = I2C_AcknowledgedAddress_7bit;      
    i2c_initstruct.I2C_ClockSpeed           = IIC_SPEED;                        //通信速率
    
    I2C_Init(IIC_I2C2, &i2c_initstruct);
#endif

}

/**
  * @brief  HARD_IIC 初始化
  * @param  无
  * @retval 无
  */
void IIC_Init(void)
{
    /* 对应的 GPIO 的配置 */
    IIC_PinConfig();
    
    /* 对应的配置模式 */
    IIC_Mode_Config();
#if 1    
    /* 使能 HARD_IIC */
    I2C_Cmd(IIC_I2C1,ENABLE);
#endif

#if 1    
    /* 使能 HARD_IIC */
    I2C_Cmd(IIC_I2C2,ENABLE);
#endif
}

/**
  * @brief  HARD_IIC 延时接口(单位：us)
  * @param  无
  * @retval 无
  */
void IIC_DELAY_US(uint32_t time)
{
    DWT_DelayUs(time);
}

/**
  * @brief  发起 HARD_IIC 总线开始信号
  * @param  I2Cx:I2C外设
  * @retval ERROR:失败 ;   SUCCESS:成功
  */
ErrorStatus IIC_Start(I2C_TypeDef* I2Cx)
{
    uint32_t check_times = CHECK_TIMES;
    
    /* 检测总线是否繁忙 */
    while(I2C_GetFlagStatus(I2Cx,I2C_FLAG_BUSY))
    {
        check_times--;
        IIC_DELAY_US(1);
        if(check_times == 0)
        {
            return ERROR;
        }
    }
    
    /* 产生I2C 起始信号*/
    I2C_GenerateSTART(I2Cx,ENABLE);
    check_times = CHECK_TIMES;
    
    while(I2C_CheckEvent(I2Cx,I2C_EVENT_MASTER_MODE_SELECT) == ERROR)
    {
        check_times--;
        IIC_DELAY_US(1);
        if(check_times == 0)
        {
            /* 发送停止信号方便下次通信使用*/
            IIC_Stop(I2Cx);
            return ERROR;
        }
    }
    return SUCCESS;
}

/**
  * @brief  发起 HARD_IIC 总线开始信号
  * @param  I2Cx:I2C外设
  * @retval ERROR:失败 ;   SUCCESS:成功
  */
ErrorStatus IIC_Restart(I2C_TypeDef* I2Cx)
{
    uint32_t check_times = CHECK_TIMES;
        
    /* 产生I2C 起始信号*/
    I2C_GenerateSTART(I2Cx,ENABLE);
    check_times = CHECK_TIMES;
    
    while(I2C_CheckEvent(I2Cx,I2C_EVENT_MASTER_MODE_SELECT) == ERROR)
    {
        check_times--;
        IIC_DELAY_US(1);
        if(check_times == 0)
        {
            /* 发送停止信号方便下次通信使用*/
            IIC_Stop(I2Cx);
            return ERROR;
        }
    }
    return SUCCESS;
}

/**
  * @brief  发起 IIC 总线停止信号
  * @param  I2Cx:I2C外设
  * @retval 无
  */
void IIC_Stop(I2C_TypeDef* I2Cx) 
{
    I2C_GenerateSTOP(I2Cx,ENABLE);
}

/**
  * @brief  通过 IIC外设 发送一个数据字节.
  * @param  I2Cx:I2C外设
  * @param  data: 要传输的字节
  * @retval ERROR:失败 ;   SUCCESS:成功
  */
ErrorStatus IIC_SendData(I2C_TypeDef* I2Cx,uint8_t data)
{
    uint32_t check_times = CHECK_TIMES;
    I2C_SendData(I2Cx,data);
    while(I2C_CheckEvent(I2Cx,I2C_EVENT_MASTER_BYTE_TRANSMITTED) == ERROR)
    {
        check_times--;
        IIC_DELAY_US(1);
        if(check_times == 0)
        {
            /* 发送停止信号方便下次通信使用*/
            IIC_Stop(I2Cx);
            return ERROR;
        }
    }
    return SUCCESS;
}

/**
  * @brief  通过 IIC外设 地址验证
  * @param  I2Cx:I2C外设
  * @param  slave_addr: 从设备地址
  * @param  direction: 读写操作(传输方向)
  * @retval ERROR:失败 ;   SUCCESS:成功
  */
ErrorStatus IIC_AddressMatching(I2C_TypeDef* I2Cx,uint8_t slave_addr,IIC_Direction_TypeDef direction)
{
    uint32_t check_times    = CHECK_TIMES;
    uint32_t event_temp     = I2C_EVENT_MASTER_TRANSMITTER_MODE_SELECTED;
    uint32_t i2c_dirction     = I2C_EVENT_MASTER_TRANSMITTER_MODE_SELECTED;
    if(direction == IIC_WRITE)
    {
        event_temp = I2C_EVENT_MASTER_TRANSMITTER_MODE_SELECTED;
        i2c_dirction = I2C_Direction_Transmitter;
    }
    else
    {
        event_temp = I2C_EVENT_MASTER_RECEIVER_MODE_SELECTED;
        i2c_dirction = I2C_Direction_Receiver;
    }

    I2C_Send7bitAddress(I2Cx,(slave_addr<<1)|direction,i2c_dirction);  //本质与I2C_SendData(IIC_I2CX, (slave_addr<<1)|读写操作位)一样

    while(I2C_CheckEvent(I2Cx,event_temp) == ERROR)
    {
        check_times--;
        IIC_DELAY_US(1);
        if(check_times == 0)
        {
            /* 发送停止信号方便下次通信使用*/
            IIC_Stop(I2Cx);
            return ERROR;
        }
    }
    return SUCCESS;
}

/**
  * @brief  检测IIC总线从设备
  * @param  I2Cx:I2C外设
  * @param  slave_addr：从设备地址
  * @retval SUCCESS 检测通过   其他：不通过
  */
ErrorStatus IIC_CheckDevice(I2C_TypeDef* I2Cx,uint8_t slave_addr)
{
    ErrorStatus temp = ERROR;
    
    /* 检测总线是否繁忙和发出开始信号*/
    temp = IIC_Start(I2Cx);
    if(temp != SUCCESS)
    {
        return temp;
    }
   
    /* 呼叫从机,地址配对*/
    temp = IIC_AddressMatching(I2Cx,slave_addr,IIC_WRITE);
    if(temp != SUCCESS)
    {
        return temp;
    }
    
    /* 发送停止信号方便下次通信使用*/
    IIC_Stop(I2Cx);
    return SUCCESS;  
}

/**
  * @brief  通过 IIC外设 接收一个数据字节.
  * @param  I2Cx:I2C外设
  * @param  data: 接收到的字节
  * @retval ERROR:失败 ;   SUCCESS:成功
  */
ErrorStatus IIC_ReceiveData(I2C_TypeDef* I2Cx,uint8_t *data)
{
    uint32_t check_times = CHECK_TIMES;
    while(I2C_CheckEvent(I2Cx,I2C_EVENT_MASTER_BYTE_RECEIVED ) == ERROR)
    {
        check_times--;
        IIC_DELAY_US(1);
        if(check_times == 0)
        {
            /* 发送停止信号方便下次通信使用*/
            IIC_Stop(I2Cx);
            return ERROR;
        }
    }
    *data = I2C_ReceiveData(I2Cx);
    return SUCCESS;

}

/*********************************************END OF FILE**********************/
