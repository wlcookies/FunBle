package com.wlcookies.funble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

/*开启蓝牙，Intent返回码*/
const val REQUEST_ENABLE_BT = 100

class MainActivity : AppCompatActivity() {

    /**
     * 获取蓝牙适配器
     */
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    /**
     * 确保蓝牙是否启用
     */
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    /**
     * 监听蓝牙开启状态广播接收器
     */
    private val bluetoothStateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.takeIf {
                it.action == BluetoothAdapter.ACTION_STATE_CHANGED
            }?.apply {
                this.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1).log()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            when (resultCode) {
                RESULT_OK -> { // 蓝牙开启
                    "用户开启了蓝牙".log()
                }
                RESULT_CANCELED -> {// 蓝牙拒绝开启
                    "用户拒绝开启蓝牙".log()
                }
            }
        }
    }
}


fun Any.log() {
    Log.d("my-log", "$this")
}