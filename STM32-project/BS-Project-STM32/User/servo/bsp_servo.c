/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
#include "servo/bsp_servo.h"


void SERVO1_GPIO_Config(void)
{
    
    GPIO_InitTypeDef GPIO_InitStructure = {0};
    
    
    RCC_APB2PeriphClockCmd(SERVO1_GPIO_CLK_PORT,ENABLE);

    /* IO输出状态初始化控制 */
    GPIO_ResetBits(SERVO1_GPIO_PORT,SERVO1_GPIO_PIN);
    
    
    GPIO_InitStructure.GPIO_Mode    =   GPIO_Mode_AF_PP;
    GPIO_InitStructure.GPIO_Pin     =   SERVO1_GPIO_PIN;
    GPIO_InitStructure.GPIO_Speed   =   GPIO_Speed_50MHz;
    GPIO_Init(SERVO1_GPIO_PORT,&GPIO_InitStructure);  
}


void SERVO2_GPIO_Config(void)
{
    
    GPIO_InitTypeDef GPIO_InitStructure = {0};

    
    RCC_APB2PeriphClockCmd(SERVO2_GPIO_CLK_PORT,ENABLE);

    /* IO输出状态初始化控制 */
    GPIO_ResetBits(SERVO2_GPIO_PORT,SERVO1_GPIO_PIN);
    
    
    GPIO_InitStructure.GPIO_Mode    =   GPIO_Mode_AF_PP;
    GPIO_InitStructure.GPIO_Pin     =   SERVO2_GPIO_PIN;
    GPIO_InitStructure.GPIO_Speed   =   GPIO_Speed_50MHz;
    GPIO_Init(SERVO2_GPIO_PORT,&GPIO_InitStructure);
}




void SERVO_TIM_NVIC_Config(void)
{
    
    NVIC_InitTypeDef nvic_initstructure = {0};

    nvic_initstructure.NVIC_IRQChannel                      = SERVO_TIM_IRQ;
    nvic_initstructure.NVIC_IRQChannelPreemptionPriority    = 0;
    nvic_initstructure.NVIC_IRQChannelSubPriority           = 3;
    nvic_initstructure.NVIC_IRQChannelCmd                   = ENABLE;
    
    NVIC_Init(&nvic_initstructure);
    
}


void SERVO_TIM_Mode_Config(void)
{
    
	TIM_TimeBaseInitTypeDef  tim_timebasestructure = {0};		
    
    
    TIM_OCInitTypeDef  tim_ocinitstructure = {0};	

    
    SERVO_TIM_APBXCLKCMD(SERVO_TIM_CLK_PORT, ENABLE); 	    //浣胯兘TIMx鏃堕挓
    
    		 
    tim_timebasestructure.TIM_Period            = 1000;       		    
    tim_timebasestructure.TIM_Prescaler         = (72-1);	    	    
    tim_timebasestructure.TIM_ClockDivision     = TIM_CKD_DIV1 ;	    
    tim_timebasestructure.TIM_CounterMode       = TIM_CounterMode_Up;  	//鍚戜笂璁℃暟妯″紡
    tim_timebasestructure.TIM_RepetitionCounter = 0;                    //閲嶅璁℃暟鍣ㄧ殑鍊硷紝娌＄敤鍒颁笉鐢ㄧ
    TIM_TimeBaseInit(SERVO_TIM, &tim_timebasestructure);            // 鍒濆鍖栧畾鏃跺櫒
 
    
    /* PWM妯″紡閰嶇疆 */
    tim_ocinitstructure.TIM_OCMode      = TIM_OCMode_PWM1;	    		//閰嶇疆涓篜WM妯″紡1
    tim_ocinitstructure.TIM_OutputState = TIM_OutputState_Enable;	    //浣胯兘杈撳嚭
    tim_ocinitstructure.TIM_Pulse       = PWM_SERVO_PULSE;				//设置初始PWM脉冲宽度	
    tim_ocinitstructure.TIM_OCPolarity  = TIM_OCPolarity_High;  	    

    /* 浣胯兘閫氶亾鍜岄瑁呰浇 */
    SERVO1_TIM_OCX_INIT(SERVO_TIM, &tim_ocinitstructure);								
    SERVO1_TIM_OCXPRELOAD_CONFIG(SERVO_TIM, TIM_OCPreload_Enable);
    SERVO2_TIM_OCX_INIT(SERVO_TIM, &tim_ocinitstructure);	 							
    SERVO2_TIM_OCXPRELOAD_CONFIG(SERVO_TIM, TIM_OCPreload_Enable);
    
    /* 浣胯兘閲嶈浇瀵勫瓨鍣ˋRR */
    TIM_ARRPreloadConfig(SERVO_TIM, ENABLE);//浣胯兘閲嶈浇瀵勫瓨鍣ˋRR
    
}



void SERVO_TIM_Init(void)
{
    
	SERVO_TIM_NVIC_Config();
    
    
    SERVO1_GPIO_Config();
    SERVO2_GPIO_Config();
    
    /* 閰嶇疆 GENERALTIM 妯″紡 */
	SERVO_TIM_Mode_Config();
    
    /* 浣胯兘 TIM */
    TIM_Cmd(SERVO_TIM,ENABLE);
}


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


void SERVO_CycleConfig(uint16_t pwm_cycle)
{
    TIM_SetAutoreload(SERVO_TIM, pwm_cycle);	
}


uint16_t SERVO_TimeCalculate(float time)
{	
    return time*1000.0*1000000*TIM_GetPrescaler(SERVO_TIM)/SystemCoreClock;
}

float SERVO_AngleToTime(uint16_t angle)
{	
    return angle*(0.5/45)+0.5;
}

/*********************************************END OF FILE**********************/
