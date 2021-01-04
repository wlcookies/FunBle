package com.wlcookies.funble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/*开启蓝牙，Intent返回码*/
const val REQUEST_ENABLE_BT = 100

const val REQUEST_PERMISSIONS = 101

/*蓝牙扫描持续时间*/
const val SCAN_PERIOD = 10 * 1000L

class MainActivity : AppCompatActivity() {

    /**
     * SDK 21 蓝牙扫描结果回调接口实现
     */
    private val scanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            "onScanResult----------------".log()
            result?.device?.address?.log()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            "onBatchScanResults----------------".log()
            results?.log()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }

    private val bluetoothLeScanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            "bluetoothLeScanCallback-------".log()
            device.log()
        }

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

    /*控制蓝牙扫描动作的开关*/
    private var mScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED)
        ) {
            // 判断位置开关是否开启
            if (!isLocationServiceEnable(this)) {
                // 跳转系统设置页面（位置开关）
                goSystemLocationSetting(this)
            } else {
                // 判断蓝牙开关是否开启
                bluetoothAdapter?.takeIf { it.isDisabled }?.apply {

                }

                if (bluetoothAdapter?.isDisabled != false) {
                    // 弹出系统提示框，询问用户是否开启蓝牙
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    scanLeDevice(true)
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_PERMISSIONS
            )
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


    /**
     * 扫描Ble设备
     */
    fun scanLeDevice(isScan: Boolean) {
        when (isScan) {
            true -> {
                Handler().postDelayed({
                    mScanning = false
                    stopScan()
                }, SCAN_PERIOD)
                mScanning = true
                startScan()
            }
            else -> {
                mScanning = false
                stopScan()
            }
        }


    }

    fun startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter?.bluetoothLeScanner?.startScan(
                scanCallback
            )
        } else {
            bluetoothAdapter?.startLeScan(bluetoothLeScanCallback)
        }
    }

    fun stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(
                scanCallback
            )
        } else {
            bluetoothAdapter?.stopLeScan(bluetoothLeScanCallback)
        }
    }
}

/**
 * 判断系统定位服务是否开启
 */

fun isLocationServiceEnable(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    return gps || network
}

/**
 * 跳转至系统位置信息设置界面
 */
fun goSystemLocationSetting(context: Context) {
    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

fun Any.log() {
    Log.d("my-log", "$this")
}