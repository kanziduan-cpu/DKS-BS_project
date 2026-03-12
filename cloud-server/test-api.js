#!/usr/bin/env node

// 测试HTTP API的脚本
const http = require('http');

const BASE_URL = 'http://localhost:3000/api';
const DEVICE_ID = 'warehouse_device_001';

// 颜色输出
const colors = {
    reset: '\x1b[0m',
    green: '\x1b[32m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m'
};

function log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
}

// HTTP请求函数
function request(method, path, data = null) {
    return new Promise((resolve, reject) => {
        const url = new URL(path, BASE_URL);
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            }
        };

        const req = http.request(url, options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                try {
                    resolve({
                        statusCode: res.statusCode,
                        body: JSON.parse(body)
                    });
                } catch (e) {
                    resolve({
                        statusCode: res.statusCode,
                        body: body
                    });
                }
            });
        });

        req.on('error', reject);

        if (data) {
            req.write(JSON.stringify(data));
        }

        req.end();
    });
}

// 测试函数
async function testAPI() {
    log('\n=== 开始测试HTTP API ===\n', 'blue');
    
    let passedTests = 0;
    let totalTests = 0;

    // 测试1: 获取最新传感器数据
    totalTests++;
    log(`测试${totalTests}: 获取最新传感器数据`, 'yellow');
    try {
        const result = await request('GET', `/sensor/latest/${DEVICE_ID}`);
        if (result.statusCode === 200 && result.body) {
            log(`  ✓ 成功 - 温度: ${result.body.temperature}°C`, 'green');
            passedTests++;
        } else {
            log(`  ✗ 失败 - 状态码: ${result.statusCode}`, 'red');
        }
    } catch (error) {
        log(`  ✗ 失败 - ${error.message}`, 'red');
    }

    // 测试2: 获取历史数据
    totalTests++;
    log(`\n测试${totalTests}: 获取历史传感器数据`, 'yellow');
    try {
        const result = await request('GET', `/sensor/history/${DEVICE_ID}?limit=10`);
        if (result.statusCode === 200 && Array.isArray(result.body)) {
            log(`  ✓ 成功 - 获取到 ${result.body.length} 条记录`, 'green');
            passedTests++;
        } else {
            log(`  ✗ 失败 - 状态码: ${result.statusCode}`, 'red');
        }
    } catch (error) {
        log(`  ✗ 失败 - ${error.message}`, 'red');
    }

    // 测试3: 获取设备状态
    totalTests++;
    log(`\n测试${totalTests}: 获取设备状态`, 'yellow');
    try {
        const result = await request('GET', `/device/status/${DEVICE_ID}`);
        if (result.statusCode === 200 && result.body) {
            log(`  ✓ 成功 - 通风: ${result.body.ventilation}, 舵机角度: ${result.body.servo_angle}°`, 'green');
            passedTests++;
        } else {
            log(`  ✗ 失败 - 状态码: ${result.statusCode}`, 'red');
        }
    } catch (error) {
        log(`  ✗ 失败 - ${error.message}`, 'red');
    }

    // 测试4: 发送控制指令
    totalTests++;
    log(`\n测试${totalTests}: 发送控制指令`, 'yellow');
    try {
        const commandData = {
            device_id: DEVICE_ID,
            command: 'control_servo',
            params: { angle: 90 }
        };
        const result = await request('POST', '/control/command', commandData);
        if (result.statusCode === 200 && result.body.success) {
            log(`  ✓ 成功 - 指令ID: ${result.body.command_id}`, 'green');
            passedTests++;
        } else {
            log(`  ✗ 失败 - 状态码: ${result.statusCode}`, 'red');
        }
    } catch (error) {
        log(`  ✗ 失败 - ${error.message}`, 'red');
    }

    // 测试5: 获取报警记录
    totalTests++;
    log(`\n测试${totalTests}: 获取报警记录`, 'yellow');
    try {
        const result = await request('GET', `/alarms/${DEVICE_ID}?limit=5`);
        if (result.statusCode === 200 && Array.isArray(result.body)) {
            log(`  ✓ 成功 - 获取到 ${result.body.length} 条报警`, 'green');
            passedTests++;
        } else {
            log(`  ✗ 失败 - 状态码: ${result.statusCode}`, 'red');
        }
    } catch (error) {
        log(`  ✗ 失败 - ${error.message}`, 'red');
    }

    // 输出测试结果
    log('\n=== 测试结果 ===', 'blue');
    log(`通过: ${passedTests}/${totalTests}`, passedTests === totalTests ? 'green' : 'yellow');
    
    if (passedTests === totalTests) {
        log('\n✓ 所有测试通过!', 'green');
    } else {
        log('\n✗ 部分测试失败', 'red');
    }

    process.exit(0);
}

// 运行测试
testAPI().catch(error => {
    log(`\n测试出错: ${error.message}`, 'red');
    process.exit(1);
});
