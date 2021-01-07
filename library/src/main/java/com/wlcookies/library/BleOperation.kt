package com.wlcookies.library

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.wlcookies.library.util.CommonUtil

/*开启蓝牙，Intent返回码*/
const val REQUEST_ENABLE_BT = 100

/*权限回调*/
const val REQUEST_PERMISSIONS = 101

/*蓝牙扫描持续时间*/
const val SCAN_PERIOD = 10 * 1000L

/**
 * Ble 蓝牙设备扫描封装
 */
class BleOperation : Fragment() {

    /**
     * 扫描结果回调接口
     */
    interface BleOperationScanCallback {
        fun onScanStart()
        fun onScanStop()
        fun onScanResult(device: BluetoothDevice?)
    }

    var bleOperationScanCallback: BleOperationScanCallback? = null

    /*获取蓝牙适配器*/
    private var bluetoothAdapter: BluetoothAdapter? = null

    /*控制蓝牙扫描动作的开关*/
    private var mScanning = false

    /*确保蓝牙是否启用*/
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    /**
     * SDK 21 蓝牙扫描结果回调接口实现
     */
    private val scanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            bleOperationScanCallback?.onScanResult(result?.device)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }

    private val bluetoothLeScanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            bleOperationScanCallback?.onScanResult(device)
        }

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

    companion object {
        fun newInstance() = BleOperation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothAdapter =
            ContextCompat.getSystemService(requireContext(), BluetoothManager::class.java)?.adapter

        requireActivity().registerReceiver(
            bluetoothStateChangedReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    /**
     * 扫描Ble设备
     */
    fun readyScanBle() {
        if (CommonUtil.isLocationServiceEnable(requireContext())) {
            // 判断蓝牙开关是否开启
            if (bluetoothAdapter?.isDisabled != false) {
                // 弹出系统提示框，询问用户是否开启蓝牙
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                // 动态申请地理位置权限
                if ((ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                            &&
                            ContextCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED)
                ) {
                    doScanLe(true)
                } else {
                    this.requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        REQUEST_PERMISSIONS
                    )
                }
            }
        } else {
            AlertDialog.Builder(requireContext()).setTitle("提示")
                .setMessage("蓝牙设备的扫描操作需要开启定位")
                .setPositiveButton("开启", DialogInterface.OnClickListener { _, _ ->
                    CommonUtil.goSystemLocationSetting(requireContext())
                }).create().show()
        }
    }

    /**
     * 扫描Ble设备
     */
    private fun doScanLe(isScan: Boolean) {
        when (isScan) {
            true -> {
                Handler().postDelayed({
                    if (mScanning) {
                        stopScan()
                        mScanning = false
                    }
                }, SCAN_PERIOD)
                startScan()
                mScanning = true
            }
            else -> {
                stopScan()
                mScanning = false
            }
        }
    }

    private fun startScan() {
        if (!mScanning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothAdapter?.bluetoothLeScanner?.startScan(
                    scanCallback
                )
            } else {
                bluetoothAdapter?.startLeScan(bluetoothLeScanCallback)
            }
            bleOperationScanCallback?.onScanStart()
        }
    }

    private fun stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(
                scanCallback
            )
        } else {
            bluetoothAdapter?.stopLeScan(bluetoothLeScanCallback)
        }
        bleOperationScanCallback?.onScanStop()
    }

    fun doStopScan() {
        if (mScanning) {
            stopScan()
            mScanning = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            when (resultCode) {
                AppCompatActivity.RESULT_OK -> { // 蓝牙开启
                    readyScanBle()
                }
                AppCompatActivity.RESULT_CANCELED -> {// 蓝牙拒绝开启
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.filter {
                    it == PackageManager.PERMISSION_GRANTED
                }.size == grantResults.size) {
                readyScanBle()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(bluetoothStateChangedReceiver)
    }
}

fun Any.log() {
    Log.d("my-log", "$this")
}