# STM32 物联网项目 MQTT 协议参数映射文档

## 当前固件对齐说明（2026-05-11）

> 当前仓库已经切回 ESP8266 AT + MQTT 主线，不再是文档开头描述的“待改造 TCP 透传”状态。以下内容是和当前固件一致的设备页对齐建议。

### App 设备页建议映射

| 设备页模块 | 建议下发指令 | 单片机实际行为 | 备注 |
|---|---|---|---|
| 单片机重置 | 不建议保留 | 当前固件未启用 IWDG/WWDG，也没有安全的软件复位命令 | 当前工作区没有 Android/App 源码，无法直接删除页面按钮；但按 MCU 现状，设备页应移除此入口 |
| LED 灯开关 | `LED_ON` / `LED_OFF` | 同时打开或关闭绿灯和蓝灯 | 如果页面只有一个 LED 总开关，优先用这组命令，不要混用单灯命令 |
| 舵机左/右 | `SERVO_LEFT` / `SERVO_RIGHT` | `SERVO_LEFT` 把两个舵机打到 60 度，`SERVO_RIGHT` 回到 0 度 | 当前固件语义已经等价于“窗户开/关” |
| 告警复位 | `ALARM_RESET` | 清除倾斜报警和水位高预警，并关闭蜂鸣器与双 LED | 可作为设备页附加按钮，不建议和“单片机重置”混用 |

### 当前告警上报对齐

| 告警来源 | Topic | `type` | `alarmTitle` | 触发条件 |
|---|---|---|---|---|
| MPU6050 倾斜异常 | `device/STM32_01/alarm` | `TILT` | `倾斜报警` | `TiltDelta >= 10.0` |
| 雨量/水位过高 | `device/STM32_01/alarm` | `WATER_HIGH` | `水位高预警` | `water_level >= 60` |

### 推荐控制指令示例

```json
{"device_id":"STM32_01","cmd":"LED_ON"}
{"device_id":"STM32_01","cmd":"LED_OFF"}
{"device_id":"STM32_01","cmd":"SERVO_LEFT"}
{"device_id":"STM32_01","cmd":"SERVO_RIGHT"}
{"device_id":"STM32_01","cmd":"ALARM_RESET"}
```

## 一、项目概述

此文档基于 `BS-Project-STM32` 项目代码分析，将该系统的传感器数据和设备控制指令通过 **MQTT 协议** 上传至云端，并实现云端/APP 对设备的远程控制。

### 当前通信方式

当前代码使用 **ESP8266 WiFi模块** 通过 **TCP透传** 方式与服务器通信（`app_esp8266.c` 中的 `ESP8266_StaTcpClient_Unvarnish_ConfigTest` 函数）。需要将此通信方式改造为 **MQTT协议**。

---

## 二、传感器上报参数（上传至云端）

以下参数需要从设备端通过 MQTT 上报到云端：

### 2.1 温湿度数据 — DHT11

| MQTT Topic | 数据类型 | 单位 | 描述 | 代码变量来源 |
|---|---|---|---|---|
| `sensor/temperature` | float | ℃ | 温度值 | `dht11_data.temp_int` + `dht11_data.temp_deci`（例如 25.5℃） |
| `sensor/humidity` | float | %RH | 湿度值 | `dht11_data.humi_int` + `dht11_data.humi_deci`（例如 60.5%RH） |

**数据来源文件：** `User/dht11/bsp_dht11.h` — `DHT11_DATA_TYPEDEF` 结构体

**上报时机：** 每 2000ms 一次（`Dht11_TaskInit(2000)`）

### 2.2 雨量数据 — ADC雨滴传感器

| MQTT Topic | 数据类型 | 单位 | 描述 | 代码变量来源 |
|---|---|---|---|---|
| `sensor/rain/adc` | uint16 | — | 雨量传感器ADC原始值（0~4095） | `adc_source_convertedvalue[ADCX_RAIN_BUFFER_INDEX]` |
| `sensor/rain/voltage` | float | V | 雨量传感器电压值 | `(float)adc_source_convertedvalue[ADCX_RAIN_BUFFER_INDEX] * 3.3f / 4095.0f` |
| `sensor/rain/level` | string | — | 雨量等级描述 | 见下方映射表 |
| `sensor/rain/height` | float | mm | 降水量高度（暴雨时有效） | `Calculate_RainHeight(adc_value)` |

**雨量等级映射表：**

| ADC原始值范围 | 等级中文 | 等级英文（推荐用） |
|---|---|---|
| ≤ 0x1f (31) | 无雨 | `none` |
| 0x20 ~ 0x3e8 (32~1000) | 毛毛细雨 | `light` |
| 0x3e9 ~ 0x5dc (1001~1500) | 微微小雨 | `moderate` |
| 0x5dd ~ 0x7d0 (1501~2000) | 熊熊中雨 | `heavy` |
| > 0x7d0 (2000) | 汹汹暴雨 | `storm` |

**数据来源文件：** `App/adc/app_adc.c` — `RainSoil_Task()`

**上报时机：** 每 1000ms 一次（`RainSoil_TaskInit(1000)`）

### 2.3 空气质量数据 — MQ135

| MQTT Topic | 数据类型 | 单位 | 描述 | 代码变量来源 |
|---|---|---|---|---|
| `sensor/air/ppm` | float | ppm | 综合污染气体平均浓度 | `mq135_task.ppm` |
| `sensor/air/level` | string | — | 空气质量等级 | 见下方映射表 |

**空气质量等级映射表：**

| ppm范围 | 等级中文 | 等级英文（推荐用） |
|---|---|---|
| < 10 | 低于检测范围 | `low` |
| 10 ~ 1000 | 正常范围 | `normal` |
| > 1000 | 超出检测范围 | `high` |

**数据来源文件：** `App/mq135/app_mq135.h` — `MQ135_TaskInfo` 结构体

**上报时机：** 每 1000ms 一次（`MQ135_TaskInit(1000)`）

### 2.4 MPU6050 六轴姿态数据

| MQTT Topic | 数据类型 | 单位 | 描述 | 代码变量来源 |
|---|---|---|---|---|
| `sensor/mpu6050/accel_x` | int16 | — | X轴加速度原始值 | `mpu6050_task.Accel[0]` |
| `sensor/mpu6050/accel_y` | int16 | — | Y轴加速度原始值 | `mpu6050_task.Accel[1]` |
| `sensor/mpu6050/accel_z` | int16 | — | Z轴加速度原始值 | `mpu6050_task.Accel[2]` |
| `sensor/mpu6050/gyro_x` | int16 | — | X轴陀螺仪原始值 | `mpu6050_task.Gyro[0]` |
| `sensor/mpu6050/gyro_y` | int16 | — | Y轴陀螺仪原始值 | `mpu6050_task.Gyro[1]` |
| `sensor/mpu6050/gyro_z` | int16 | — | Z轴陀螺仪原始值 | `mpu6050_task.Gyro[2]` |
| `sensor/mpu6050/temp` | float | ℃ | MPU6050内部温度 | `mpu6050_task.Temp` |

**数据来源文件：** `App/mpu6050/app_mpu6050.h` — `MPU6050_TaskInfo` 结构体

**上报时机：** 每 1000ms 一次（`MPU6050_TaskInit(1000)`）

### 2.5 设备状态数据

| MQTT Topic | 数据类型 | 描述 | 代码变量来源 |
|---|---|---|---|
| `device/led/green` | bool | 绿灯状态（true=开, false=关） | 从GPIO读取 |
| `device/led/blue` | bool | 蓝灯状态（true=开, false=关） | 从GPIO读取 |
| `device/led/run` | bool | 运行指示灯状态 | `program_run_led_task.flag` |
| `device/beep/active` | bool | 蜂鸣器状态（true=开, false=关） | `beep_task.beep_active` |
| `device/beep/speed` | uint16 | 蜂鸣器翻转周期(ms) | `beep_task.speed` |
| `device/servo1/angle` | uint8 | 舵机1角度(0~180°) | 从GPIO读取 |
| `device/servo2/angle` | uint8 | 舵机2角度(0~180°) | 从GPIO读取 |

---

## 三、设备控制指令（从云端/APP下发）

### 3.1 MQTT 控制指令 Topic 定义

**订阅主题（设备端订阅，接收控制指令）：** `device/control`

**发布主题（设备端发布，响应状态）：** `device/control/response`

### 3.2 控制指令 JSON 格式

所有控制指令使用统一的 JSON 格式下发：

```json
{
  "cmd": "指令名称",
  "params": {
    // 指令参数（可选）
  },
  "timestamp": 1234567890
}
```

### 3.3 指令与串口命令映射表

> 以下指令与当前代码中 `app_esp8266.c` 的 `control_cmds` 数组和 `Get_ESP82666_Cmd()` 函数中的命令一一对应。

| MQTT JSON 指令 (`cmd`) | 对应串口命令 | 参数说明 | 功能描述 | 对应代码处理 |
|---|---|---|---|---|
| `"G_LED_ON"` | `G_LED_ON` | 无 | **打开绿灯** | `case 0`: `LED_ON(G_LED_GPIO_PORT, G_LED_GPIO_PIN, LED_LOW_TRIGGER)` |
| `"G_LED_OFF"` | `G_LED_OFF` | 无 | **关闭绿灯** | `case 1`: `LED_OFF(G_LED_GPIO_PORT, G_LED_GPIO_PIN, LED_LOW_TRIGGER)` |
| `"B_LED_ON"` | `B_LED_ON` | 无 | **打开蓝灯** | `case 2`: `LED_ON(B_LED_GPIO_PORT, B_LED_GPIO_PIN, LED_LOW_TRIGGER)` |
| `"B_LED_OFF"` | `B_LED_OFF` | 无 | **关闭蓝灯** | `case 3`: `LED_OFF(B_LED_GPIO_PORT, B_LED_GPIO_PIN, LED_LOW_TRIGGER)` |
| `"BEEP_ON"` | `BEEP_ON` | 无 | **打开蜂鸣器**（速度100ms） | `case 4`: `beep_task.speed = 100; beep_task.beep_active = true` |
| `"BEEP_OFF"` | `BEEP_OFF` | 无 | **关闭蜂鸣器** | `case 5`: `beep_task.beep_active = false` |
| `"SERVO_LEFT"` | `SERVO_LEFT` | 无 | **舵机向左旋转**（每次转10°） | `case 6`: `servo1_task.turn_left_flag = 1; servo2_task.turn_left_flag = 1` |
| `"SERVO_RIGHT"` | `SERVO_RIGHT` | 无 | **舵机向右旋转**（每次转10°） | `case 7`: `servo1_task.turn_right_flag = 1; servo2_task.turn_right_flag = 1` |

### 3.4 扩展控制指令

以下指令在 `Get_ESP82666_Cmd()` 函数中有实现，建议同时支持：

| MQTT JSON 指令 | 参数 (`params`) | 功能描述 | 对应串口命令 |
|---|---|---|---|
| `"LED_R"` | `{"action": "on\|off\|toggle"}` | **控制红灯**（on/off/toggle） | `ledr on\|off\|toggle` |
| `"LED_G"` | `{"action": "on\|off\|toggle"}` | **控制绿灯**（on/off/toggle） | `ledg on\|off\|toggle` |
| `"LED_B"` | `{"action": "on\|off\|toggle"}` | **控制蓝灯**（on/off/toggle） | `ledb on\|off\|toggle` |
| `"RUN_LED"` | `{"action": "on\|off"}` | **控制运行指示灯** | `runled on\|off` |
| `"BEEP"` | `{"action": "on\|off", "speed": 100}` | **控制蜂鸣器**（可指定翻转周期ms） | `beep on\|off\|<speed_ms>` |
| `"SERVO1"` | `{"angle": 0-180}` | **设置舵机1角度**（0~180°） | `servo1 <0-180>` |
| `"SERVO2"` | `{"angle": 0-180}` | **设置舵机2角度**（0~180°） | `servo2 <0-180>` |

### 3.5 APP 发送的 MQTT 控制指令示例

```json
// 打开绿灯
{"cmd":"G_LED_ON"}

// 关闭绿灯
{"cmd":"G_LED_OFF"}

// 打开蓝灯
{"cmd":"B_LED_ON"}

// 关闭蓝灯
{"cmd":"B_LED_OFF"}

// 打开蜂鸣器
{"cmd":"BEEP_ON"}

// 关闭蜂鸣器
{"cmd":"BEEP_OFF"}

// 舵机向左（每次转10°）
{"cmd":"SERVO_LEFT"}

// 舵机向右（每次转10°）
{"cmd":"SERVO_RIGHT"}

// 设置舵机1到90°
{"cmd":"SERVO1","params":{"angle":90}}

// 设置舵机2到45°
{"cmd":"SERVO2","params":{"angle":45}}

// 蜂鸣器带速度参数
{"cmd":"BEEP","params":{"action":"on","speed":200}}

// 切换绿灯
{"cmd":"LED_G","params":{"action":"toggle"}}
```

---

## 四、MQTT 完整 Topic 结构总览

### 4.1 设备上报（Publish）

| Topic | 描述 | 上报频率 |
|---|---|---|
| `device/{device_id}/sensor/temperature` | 温度 | 2000ms |
| `device/{device_id}/sensor/humidity` | 湿度 | 2000ms |
| `device/{device_id}/sensor/rain/adc` | 雨量ADC原始值 | 1000ms |
| `device/{device_id}/sensor/rain/voltage` | 雨量电压值 | 1000ms |
| `device/{device_id}/sensor/rain/level` | 雨量等级 | 1000ms |
| `device/{device_id}/sensor/rain/height` | 降水量(mm) | 1000ms |
| `device/{device_id}/sensor/air/ppm` | 空气质量ppm | 1000ms |
| `device/{device_id}/sensor/air/level` | 空气质量等级 | 1000ms |
| `device/{device_id}/sensor/mpu6050/accel_x` | 加速度X轴 | 1000ms |
| `device/{device_id}/sensor/mpu6050/accel_y` | 加速度Y轴 | 1000ms |
| `device/{device_id}/sensor/mpu6050/accel_z` | 加速度Z轴 | 1000ms |
| `device/{device_id}/sensor/mpu6050/gyro_x` | 陀螺仪X轴 | 1000ms |
| `device/{device_id}/sensor/mpu6050/gyro_y` | 陀螺仪Y轴 | 1000ms |
| `device/{device_id}/sensor/mpu6050/gyro_z` | 陀螺仪Z轴 | 1000ms |
| `device/{device_id}/sensor/mpu6050/temp` | MPU6050温度 | 1000ms |
| `device/{device_id}/status` | 设备综合状态（JSON） | 按需上报 |

### 4.2 设备接收控制（Subscribe）

| Topic | 描述 | QoS |
|---|---|---|
| `device/{device_id}/control` | 接收控制指令 | QoS 1 |
| `device/{device_id}/control/response` | 控制指令响应回执 | QoS 0 |

> 注：`{device_id}` 为设备的唯一标识符（如MAC地址或自定义ID），用于区分多个设备。

### 4.3 建议的简化 Topic（单设备场景）

如果只有一个设备，可以简化为：

| Topic | 方向 | 描述 |
|---|---|---|
| `sensor/temperature` | 上报 | 温度 |
| `sensor/humidity` | 上报 | 湿度 |
| `sensor/rain/adc` | 上报 | 雨量ADC |
| `sensor/rain/level` | 上报 | 雨量等级 |
| `sensor/rain/height` | 上报 | 降水量 |
| `sensor/air/ppm` | 上报 | 空气质量 |
| `sensor/air/level` | 上报 | 空气质量等级 |
| `sensor/mpu6050/accel` | 上报 | 加速度(X,Y,Z) |
| `sensor/mpu6050/gyro` | 上报 | 陀螺仪(X,Y,Z) |
| `sensor/mpu6050/temp` | 上报 | MPU6050温度 |
| `device/status/led` | 上报 | LED状态 |
| `device/status/beep` | 上报 | 蜂鸣器状态 |
| `device/status/servo` | 上报 | 舵机状态 |
| `device/control` | 订阅 | 接收控制指令 |

---

## 五、推荐 MQTT 数据上报格式（JSON）

推荐将所有传感器数据合并为一个 JSON 包上报，减少网络开销：

```json
{
  "device_id": "stm32_001",
  "timestamp": 1234567890,
  "sensors": {
    "temperature": 25.5,
    "humidity": 60.5,
    "rain": {
      "adc": 500,
      "voltage": 0.4,
      "level": "light",
      "height": 0.0
    },
    "air": {
      "ppm": 150.5,
      "level": "normal"
    },
    "mpu6050": {
      "accel": [125, 30, 16384],
      "gyro": [-10, 5, 2],
      "temp": 28.5
    }
  },
  "devices": {
    "led_g": true,
    "led_b": false,
    "beep": false,
    "servo1": 90,
    "servo2": 45
  }
}
```

**上报 Topic：** `device/{device_id}/status`

---

## 六、APP 端指令与串口命令对应关系总结

| APP 按钮/功能 | 串口原始指令 | MQTT 指令 (cmd) | MQTT Topic |
|---|---|---|---|
| 打开绿灯 | `G_LED_ON` | `"G_LED_ON"` | `device/control` |
| 关闭绿灯 | `G_LED_OFF` | `"G_LED_OFF"` | `device/control` |
| 打开蓝灯 | `B_LED_ON` | `"B_LED_ON"` | `device/control` |
| 关闭蓝灯 | `B_LED_OFF` | `"B_LED_OFF"` | `device/control` |
| 打开蜂鸣器 | `BEEP_ON` | `"BEEP_ON"` | `device/control` |
| 关闭蜂鸣器 | `BEEP_OFF` | `"BEEP_OFF"` | `device/control` |
| 舵机向左 | `SERVO_LEFT` | `"SERVO_LEFT"` | `device/control` |
| 舵机向右 | `SERVO_RIGHT` | `"SERVO_RIGHT"` | `device/control` |

---

## 七、代码改造要点（供参考，不修改代码）

如果需要将当前 TCP 透传方式改为 MQTT，需要：

1. **在 `app_esp8266.c` 中**：
   - 将 `ESP8266_StaTcpClient_Unvarnish_ConfigTest()` 改为 MQTT 连接配置（ESP8266 需刷 MQTT AT 固件）
   - 将 `ESP8266_SendDHT11DataTest()` 改为 MQTT Publish 发送
   - 将 `ESP8266_SendMQ135DataTest()` 改为 MQTT Publish 发送
   - 在 `Get_ESP82666_Cmd()` 基础上改为 MQTT Subscribe 回调解析
   - 增加雨量、MPU6050、设备状态的 MQTT 上报函数

2. **需要 ESP8266 AT 固件支持 MQTT 指令**：
   - `AT+MQTTUSERCFG=0,1,"clientID","","",0,0,""` — 配置MQTT
   - `AT+MQTTCONN=0,"broker_address",1883,0` — 连接MQTT Broker
   - `AT+MQTTSUB=0,"topic",1` — 订阅Topic
   - `AT+MQTTPUB=0,"topic","data",1,0` — 发布消息

3. **备选方案**：使用 PubSubClient 库直接在 MCU 代码中实现 MQTT 协议（使用 ESP8266 作为 WiFi 透传模块，MCU 负责协议层处理）。

---

## 八、MQTT 参数一览表（快速参考）

| 参数类别 | 参数名称 | 数据类型 | 单位 | 上报/控制 | 对应代码文件 |
|---|---|---|---|---|---|
| 传感器 | 温度 | float | ℃ | 上报 | `User/dht11/bsp_dht11.h` |
| 传感器 | 湿度 | float | %RH | 上报 | `User/dht11/bsp_dht11.h` |
| 传感器 | 雨量ADC值 | uint16 | — | 上报 | `App/adc/app_adc.c` |
| 传感器 | 雨量等级 | string | — | 上报 | `App/adc/app_adc.c` |
| 传感器 | 降水量 | float | mm | 上报 | `User/rain/bsp_gpio_rain.c` |
| 传感器 | 空气质量ppm | float | ppm | 上报 | `App/mq135/app_mq135.h` |
| 传感器 | 空气等级 | string | — | 上报 | `App/mq135/app_mq135.c` |
| 传感器 | MPU6050加速度 | int16[3] | — | 上报 | `App/mpu6050/app_mpu6050.h` |
| 传感器 | MPU6050陀螺仪 | int16[3] | — | 上报 | `App/mpu6050/app_mpu6050.h` |
| 传感器 | MPU6050温度 | float | ℃ | 上报 | `App/mpu6050/app_mpu6050.h` |
| 执行器 | 绿灯 | bool | — | 控制+上报 | `App/led/app_led.h` |
| 执行器 | 蓝灯 | bool | — | 控制+上报 | `App/led/app_led.h` |
| 执行器 | 红灯(运行灯) | bool | — | 控制+上报 | `App/led/app_led.h` |
| 执行器 | 蜂鸣器 | bool | — | 控制+上报 | `App/beep/app_beep.h` |
| 执行器 | 蜂鸣器速度 | uint16 | ms | 控制(参数) | `App/beep/app_beep.h` |
| 执行器 | 舵机1角度 | uint8 | ° | 控制+上报 | `App/servo/app_servo.h` |
| 执行器 | 舵机2角度 | uint8 | ° | 控制+上报 | `App/servo/app_servo.h` |
