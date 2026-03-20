/**
  ******************************************************************************
  * @file       bsp_servo.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      舵机应用函数接口
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
  
#include "servo/bsp_servo.h"

/**
 * @brief  初始化控制 SERVO1 的IO
 * @param  无
 * @retval 无
 */
void SERVO1_GPIO_Config(void)
{
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef GPIO_InitStructure = {0};
    
    /*开启 SERVO2 相关的GPIO外设/端口时钟*/
    RCC_APB2PeriphClockCmd(SERVO1_GPIO_CLK_PORT,ENABLE);

    /* IO输出状态初始化控制 */
    GPIO_ResetBits(SERVO1_GPIO_PORT,SERVO1_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为通用 复用推挽输出、设置GPIO速率为50MHz*/
    GPIO_InitStructure.GPIO_Mode    =   GPIO_Mode_AF_PP;
    GPIO_InitStructure.GPIO_Pin     =   SERVO1_GPIO_PIN;
    GPIO_InitStructure.GPIO_Speed   =   GPIO_Speed_50MHz;
    GPIO_Init(SERVO1_GPIO_PORT,&GPIO_InitStructure);  
}

/**
 * @brief  初始化控制 SERVO2 的IO
 * @param  无
 * @retval 无
 */
void SERVO2_GPIO_Config(void)
{
    /* 定义一个 GPIO 结构体 */
    GPIO_InitTypeDef GPIO_InitStructure = {0};

    /*开启 SERVO2 相关的GPIO外设/端口时钟*/
    RCC_APB2PeriphClockCmd(SERVO2_GPIO_CLK_PORT,ENABLE);

    /* IO输出状态初始化控制 */
    GPIO_ResetBits(SERVO2_GPIO_PORT,SERVO1_GPIO_PIN);
    
    /*选择要控制的GPIO引脚、设置GPIO模式为通用 复用推挽输出、设置GPIO速率为50MHz*/
    GPIO_InitStructure.GPIO_Mode    =   GPIO_Mode_AF_PP;
    GPIO_InitStructure.GPIO_Pin     =   SERVO2_GPIO_PIN;
    GPIO_InitStructure.GPIO_Speed   =   GPIO_Speed_50MHz;
    GPIO_Init(SERVO2_GPIO_PORT,&GPIO_InitStructure);
}



/**
  * @brief  配置 SERVO_TIM 中断配置
  * @param  无
  * @retval 无
  */
void SERVO_TIM_NVIC_Config(void)
{
    /* 定义一个中断控制器结构体 */
    NVIC_InitTypeDef nvic_initstructure = {0};

    nvic_initstructure.NVIC_IRQChannel                      = SERVO_TIM_IRQ;
    nvic_initstructure.NVIC_IRQChannelPreemptionPriority    = 0;
    nvic_initstructure.NVIC_IRQChannelSubPriority           = 3;
    nvic_initstructure.NVIC_IRQChannelCmd                   = ENABLE;
    
    NVIC_Init(&nvic_initstructure);
    
}

/**
  * @brief  配置 SERVO_TIM 模式配置
  * @param  无
  * @retval 无
  */
void SERVO_TIM_Mode_Config(void)
{
    /* 定义一个 GENERALTIM 结构体 */
	TIM_TimeBaseInitTypeDef  tim_timebasestructure = {0};		
    
    /* 定义一个 PWM输出配置 结构体 */
    TIM_OCInitTypeDef  tim_ocinitstructure = {0};	

    /*开启 GENERALTIM 相关的外设/端口时钟*/
    SERVO_TIM_APBXCLKCMD(SERVO_TIM_CLK_PORT, ENABLE); 	    //使能TIMx时钟
    
    /* 通用定时器配置 */		 
    tim_timebasestructure.TIM_Period            = 1000;       		    //自动重装载寄存器的值，累计TIM_Period个频率后产生一个更新或者中断
    tim_timebasestructure.TIM_Prescaler         = (72-1);	    	    //设置预分频，计数器的时钟频率CK_CNT等于fCK_PSC/(TIM_Prescaler+1)。
    tim_timebasestructure.TIM_ClockDivision     = TIM_CKD_DIV1 ;	    //设置时钟分频系数：不分频(这里用不到)
    tim_timebasestructure.TIM_CounterMode       = TIM_CounterMode_Up;  	//向上计数模式
    tim_timebasestructure.TIM_RepetitionCounter = 0;                    //重复计数器的值，没用到不用管
    TIM_TimeBaseInit(SERVO_TIM, &tim_timebasestructure);            // 初始化定时器
 
/*    
    例如向上计数时:
        PWM模式1下，TIMx_CNT<TIMx_CCRn时，输出有效电平;TIMx_CNT>TIMx_CCRn时，输出无效电平 
        PWM模式2下，TIMx_CNT<TIMx_CCRn时，输出无效电平;TIMx_CNT>TIMx_CCRn时，输出有效电平
*/    
    /* PWM模式配置 */
    tim_ocinitstructure.TIM_OCMode      = TIM_OCMode_PWM1;	    		//配置为PWM模式1
    tim_ocinitstructure.TIM_OutputState = TIM_OutputState_Enable;	    //使能输出
    tim_ocinitstructure.TIM_Pulse       = PWM_SERVO_PULSE;				//设置初始PWM脉冲宽度	
    tim_ocinitstructure.TIM_OCPolarity  = TIM_OCPolarity_High;  	    //当定时器计数值小于CCR_Val时为高电平

    /* 使能通道和预装载 */
    SERVO1_TIM_OCX_INIT(SERVO_TIM, &tim_ocinitstructure);								
    SERVO1_TIM_OCXPRELOAD_CONFIG(SERVO_TIM, TIM_OCPreload_Enable);
    SERVO2_TIM_OCX_INIT(SERVO_TIM, &tim_ocinitstructure);	 							
    SERVO2_TIM_OCXPRELOAD_CONFIG(SERVO_TIM, TIM_OCPreload_Enable);
    
    /* 使能重载寄存器ARR */
    TIM_ARRPreloadConfig(SERVO_TIM, ENABLE);//使能重载寄存器ARR
    
}


/**
 * @brief  SERVO_TIM 初始化
 * @param  无
 * @retval 无
 */
void SERVO_TIM_Init(void)
{
    /* 配置对应的中断 */
	SERVO_TIM_NVIC_Config();
    
    /* 对应的 GPIO 的配置 */
    SERVO1_GPIO_Config();
    SERVO2_GPIO_Config();
    
    /* 配置 GENERALTIM 模式 */
	SERVO_TIM_Mode_Config();
    
    /* 使能 TIM */
    TIM_Cmd(SERVO_TIM,ENABLE);
}

/**
  * @brief  配置舵机输出的PWM信号的脉宽
  * @param  servo_num：舵机接口号
  * @param  pwm_pulse：脉冲长度
  * @retval 无
  */
void SERVO_PulseConfig(uint16_t servo_num,uint16_t pwm_pulse)
{	
    if (servo_num == SERVO_NUM1)
    {
        TIM_SetCompare3(SERVO_TIM, pwm_pulse);
    }
    if (servo_num == SERVO_NUM2)
    {
        TIM_SetCompare4(SERVO_TIM, pwm_pulse);
    }
}

/**
  * @brief  配置舵机输出的PWM信号的周期
  * @param  pwm_cycle（单位：1/SystemCoreClock*TIM_Prescaler S）
  * @retval 无
  */
void SERVO_CycleConfig(uint16_t pwm_cycle)
{
    TIM_SetAutoreload(SERVO_TIM, pwm_cycle);	
}

/**
  * @brief  计算时隔计数个数
  * @param  time：时隔,最小小数点后一位
  * @retval 时隔计数个数
  */
uint16_t SERVO_TimeCalculate(float time)
{	
    return time*1000.0*1000000*TIM_GetPrescaler(SERVO_TIM)/SystemCoreClock;
}
/**
  * @brief  舵机角度转化时隔
  * @param  Angle：舵机角度（0-180）
  * @retval 舵机角度转化时隔（单位ms）
  */
float SERVO_AngleToTime(uint16_t angle)
{	
    return angle*(0.5/45)+0.5;
}

/*********************************************END OF FILE**********************/
