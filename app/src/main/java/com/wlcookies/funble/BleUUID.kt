package com.wlcookies.funble

import java.util.*

val BLE_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
val WRITE_CHARACTER_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
val NOTIFY_CHARACTER_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
val NOTIFY_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


/*
服务UUID：6e400001-b5a3-f393-e0a9-e50e24dcca9e
    特征UUID：6e400002-b5a3-f393-e0a9-e50e24dcca9e   RX Characteristic 写
    特征UUID：6e400003-b5a3-f393-e0a9-e50e24dcca9e   TX Characteristic 通知
    描述UUID：00002902-0000-1000-8000-00805f9b34fb

    服务UUID：00001800-0000-1000-8000-00805f9b34fb
    特征UUID：00002a00-0000-1000-8000-00805f9b34fb       设备名称
 */