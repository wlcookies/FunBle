package com.wlcookies.library

import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.*
import android.content.Intent
import android.os.*
import androidx.annotation.MainThread
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.wlcookies.library.util.ByteUtil
import java.util.*

const val ACTION_GATT_CONNECTED = "com.wlcookies.library.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.wlcookies.library.ACTION_GATT_DISCONNECTED"

const val ACTION_GATT_SERVICES_DISCOVERED = "com.wlcookies.library.ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_GATT_SERVICES_DISCOVERED_FAIL =
    "com.wlcookies.library.ACTION_GATT_SERVICES_DISCOVERED_FAIL"

const val ACTION_DATA_AVAILABLE_SUCCESS = "com.wlcookies.library.ACTION_DATA_AVAILABLE_SUCCESS"
const val ACTION_DATA_AVAILABLE_FAIL = "com.wlcookies.library.ACTION_DATA_AVAILABLE_FAIL"

const val ACTION_DATA_WRITE_SUCCESS = "com.wlcookies.library.ACTION_DATA_WRITE_SUCCESS"
const val ACTION_DATA_WRITE_FAIL = "com.wlcookies.library.ACTION_DATA_WRITE_FAIL"

const val EXTRA_DATA_UUID = "com.wlcookies.library.EXTRA_DATA_UUID"
const val EXTRA_DATA_CONTENT = "com.wlcookies.library.EXTRA_DATA_CONTENT"

/**
 * 负责连接Ble Gatt服务器，完成数据的交互
 */
class BleOperationService : Service() {

    inner class LocalBinder : Binder() {
        fun getService() = this@BleOperationService
    }

    private val localBinder = LocalBinder()
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var discoverServicesRunnable: Runnable? = null
    private var handle = Handler()

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {// 连接成功
                            broadcastUpdate(ACTION_GATT_CONNECTED)
                            val bondState = bluetoothDevice?.bondState ?: BOND_NONE
                            if (bondState == BOND_NONE || bondState == BOND_BONDED) {
                                var delayWhenBonded = 0L
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                                    delayWhenBonded = 1000L
                                }
                                val delay = if (bondState == BOND_BONDED) delayWhenBonded else 0L
                                discoverServicesRunnable = Runnable {
                                    val result = gatt?.discoverServices() ?: false
                                    if (!result) {
                                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED_FAIL)
                                    }
                                    discoverServicesRunnable = null
                                }
                                discoverServicesRunnable?.let {
                                    handle.postDelayed(it, delay)
                                }
                            } else if (bondState == BOND_BONDING) {
                                "等待绑定完成".log()
                            }
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {// 连接失败
                            bluetoothGatt?.close()
                            broadcastUpdate(ACTION_GATT_DISCONNECTED)
                        }
                    }
                }
                else -> {
                    bluetoothGatt?.close()
                    broadcastUpdate(ACTION_GATT_DISCONNECTED)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
//                    gatt?.services?.forEach {
//                        "服务UUID：${it.uuid}\n服务type：${it.type}".log()
//                        it.characteristics?.forEach { c ->
//                            "特征UUID：${c.uuid}\n特征type：${it.type}".log()
//                            c.descriptors.forEach { d ->
//                                "描述UUID：${d.uuid}".log()
//                            }
//                        }
//                    }
                }
                else -> {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED_FAIL)
                    bluetoothGatt?.disconnect()
                }
            }
        }

        // 特征读取结果
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE_SUCCESS, characteristic)
                }
                else -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE_FAIL)
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_DATA_WRITE_SUCCESS)
                }
                else -> {
                    broadcastUpdate(ACTION_DATA_WRITE_FAIL)
                }
            }
        }

        // 通知
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE_SUCCESS, characteristic)
        }
    }

    /**
     * 读取值
     */
    fun readData(serviceUUID: UUID, characterUUID: UUID): Boolean {
        // 获取蓝牙设备的服务
        var gattService: BluetoothGattService? = null
        if (bluetoothGatt != null) {
            gattService =
                bluetoothGatt?.getService(serviceUUID)
        }
        if (gattService == null) {
            return false
        }

        // 获取蓝牙设备的特征
        val gattCharacteristic =
            gattService.getCharacteristic(characterUUID)
                ?: return false

        // 发送数据
        gattCharacteristic.setValue("")
        return bluetoothGatt?.readCharacteristic(gattCharacteristic) ?: false
    }

    /**
     * 发送数据
     */
    fun sendData(data: ByteArray?, serviceUUID: UUID, characterUUID: UUID): Boolean {
        // 获取蓝牙设备的服务
        var gattService: BluetoothGattService? = null
        if (bluetoothGatt != null) {
            gattService =
                bluetoothGatt?.getService(serviceUUID)
        }
        if (gattService == null) {
            return false
        }

        // 获取蓝牙设备的特征
        val gattCharacteristic =
            gattService.getCharacteristic(characterUUID)
                ?: return false

        // 发送数据
        gattCharacteristic.value = data
        return bluetoothGatt?.writeCharacteristic(gattCharacteristic) ?: false
    }

    /**
     * 开启通知
     */
    fun setBleNotification(
        enable: Boolean,
        serviceUUID: UUID,
        characterUUID: UUID,
        descriptorUUID: UUID
    ) {
        if (bluetoothGatt == null) {
//            sendBleBroadcast(ACTION_CONNECTING_FAIL)
            return
        }
        val gattService = bluetoothGatt?.getService(serviceUUID) /*获取蓝牙设备的服务*/
        if (gattService == null) {
//            sendBleBroadcast(ACTION_CONNECTING_FAIL)
            return
        }
        val gattCharacteristic =
            gattService.getCharacteristic(characterUUID) /*获取蓝牙设备的特征*/
        if (gattCharacteristic == null) {
//            sendBleBroadcast(ACTION_CONNECTING_FAIL)
            return
        }

        bluetoothGatt?.setCharacteristicNotification(gattCharacteristic, enable)
        /*获取蓝牙设备特征的描述符*/
        val descriptor =
            gattCharacteristic.getDescriptor(descriptorUUID).apply {
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
        if ((bluetoothGatt?.writeDescriptor(descriptor) == true)) {
            "蓝牙设备在数据改变时，通知App，App在收到数据后回调onCharacteristicChanged方法: ".log()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    /**
     * 断开连接并释放资源
     */
    fun release() {
        bluetoothGatt?.let {
            it.disconnect()
            it.close()
        }
        bluetoothDevice = null
    }

    /**
     * 连接Ble GATT服务器
     */
    fun connect(bd: BluetoothDevice) {
        release()
        bluetoothDevice = bd
        bluetoothGatt = bd.connectGatt(this, true, bluetoothGattCallback)
    }

    /**
     * 发送广播
     */
    private fun broadcastUpdate(action: String) {
        LocalBroadcastManager.getInstance(this@BleOperationService).sendBroadcast(Intent(action))
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        //  "特征读取的结果：${ByteUtil.byteArrayToHexString(characteristic.value)}".log()
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA_UUID,characteristic.uuid.toString())
        intent.putExtra(EXTRA_DATA_CONTENT,ByteUtil.byteArrayToHexString(characteristic.value))
        LocalBroadcastManager.getInstance(this@BleOperationService).sendBroadcast(intent)
    }
}