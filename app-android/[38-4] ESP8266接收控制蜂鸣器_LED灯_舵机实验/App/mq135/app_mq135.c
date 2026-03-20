/**
  ******************************************************************************
  * @file       app_mq135.c
  * @author     embedfire
  * @version     V1.0
  * @date        2024
  * @brief      空气质量传感器应用层功能接口
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
#include "mq135/app_mq135.h"
#include "adc/bsp_adc.h"
#include "debug/bsp_debug.h"
#include "mq135/bsp_gpio_mq135.h"
#include <math.h>

MQ135_TaskInfo mq135_task = {0};

/**
 * @brief  MQ135_ADC 计数复位
 * @param  无
 * @retval 无
 */
void MQ135_TaskReset(void)
{
    mq135_task.timer  = mq135_task.cycle;
    mq135_task.flag   = 0;
}

/**
 * @brief  MQ135_ADC 任务初始化
 * @param  mq135_task_cycle: 任务轮询周期 单位ms(可修改系统节拍定时器)
 * @retval 无
 */
void MQ135_TaskInit(uint32_t mq135_task_cycle)
{
    mq135_task.cycle   = mq135_task_cycle;
    MQ135_TaskReset();
}

/**
  * @brief  求ppm
  * @param  adc_value : ADC读取的原始值（0~4095）
  * @retval ppm
  * @note
  * 根据手册提供的各污染气体灵敏度 拟合成幂函数
  * 需要根据Rs/R0推算ppm，所以拟合函数时，x轴为Rs/R0，y轴为ppm，推导出y=ax^b
  * 图表没有每个点对应具体数值只能大致估计，所以测量值存在误差，想要完全精确请根据环境做多次标定
  */
float MQ135_Get_PPM(uint16_t adc_value)
{

    float vrl = 0;   /* AO输出的模拟电压 */
    float Rs;        /* 当前传感器电阻 */
    float ppm = 0;   /* 污染物平均浓度 */    
    
	/* 读取AO输出电压 */
    vrl = (float)adc_value / 4095 * VC;
    /* 换算Rs电阻 */
    Rs = (float)(VC - vrl) * RL / vrl;
    
    float Rs0 = Rs/R0;  /* Rs/R0 */
    
    /* y=ax^b x为Rs/R0，ab的取值根据数据手册图表自行拟合成幂函数 */
    ppm =  A*pow(Rs/R0,B) ;
    
	return ppm;
}

/**
 * @brief  MQ135_ADC 任务  
 * @param  无
 * @retval 无
 */
void MQ135_Task(void)
{
    if(mq135_task.flag == 1)
    {
        float adc_convertedvalue[20] = {0};//转化后的源始值的计算值
        adc_convertedvalue[0] = (float)adc_source_convertedvalue[0]*3.3/4095;
//        printf("\r\n/*");
//        for(uint32_t adc_task_i_temp = 0;adc_task_i_temp<80;adc_task_i_temp++)
//        {
//            printf("*");
//        }
//        printf("*/");

//        printf("\r\n 当前的值:");
        printf("\r\n 空气质量检测模块： %f V(0x%04X)",adc_convertedvalue[0],adc_source_convertedvalue[0]);
        printf("\r\n");
        /* 拟合函数换算出ppm */
        mq135_task.ppm = MQ135_Get_PPM(adc_source_convertedvalue[0]);
        if(mq135_task.ppm<10)
        {
            printf("综合污染气体平均浓度低于检测范围\r\n");
        }
        else if(mq135_task.ppm>1000)
        {
            printf("综合污染气体平均浓度超过检测范围\r\n");
        }
        else
        {
            printf("综合污染气体的平均浓度：%fppm\r\n",mq135_task.ppm);
        }        
        
//        printf("/*");
//        for(uint32_t adc_task_i_temp = 0;adc_task_i_temp<80;adc_task_i_temp++)
//        {
//            printf("*");
//        }
//        printf("*/");
        mq135_task.read_completed_flag = 1;
        MQ135_TaskReset();
    
    }
}
 
/*****************************END OF FILE***************************************/
