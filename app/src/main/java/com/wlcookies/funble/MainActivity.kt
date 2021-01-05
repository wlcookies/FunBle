package com.wlcookies.funble

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.wlcookies.library.BleOperation
import com.wlcookies.library.BleOperationHelp


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scan_bt = findViewById<Button>(R.id.scan_bt)
        scan_bt.setOnClickListener {
            BleOperationHelp.startScanBle(this, object : BleOperation.BleOperationScanCallback {
                override fun onScanStart() {
                    "开始扫描".log()
                    scan_bt.isEnabled = false
                }

                override fun onScanStop() {
                    "停止扫描".log()
                    scan_bt.isEnabled = true
                }

                override fun onScanResult(device: BluetoothDevice?) {
                    "扫描结果name：${device?.name}".log()
                    "扫描结果address：${device?.address}".log()
                }
            })
        }

        val stop_scan = findViewById<Button>(R.id.stop_scan)
        stop_scan.setOnClickListener {
            BleOperationHelp.stopScanBle(this)
        }
    }
}

fun Any.log() {
    Log.d("my-log", "$this")
}