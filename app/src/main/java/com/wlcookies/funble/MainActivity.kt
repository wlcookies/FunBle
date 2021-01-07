package com.wlcookies.funble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.wlcookies.library.*
import com.wlcookies.library.util.ByteUtil
import java.util.*


class MainActivity : AppCompatActivity() {

    private val bleDeviceList = mutableListOf<BluetoothDevice>()

    private lateinit var derviceAdapter: BaseQuickAdapter<BluetoothDevice, BaseViewHolder>

    private var bleServiceConnection: BleOperationService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bleServiceConnection =
                (service as? BleOperationService.LocalBinder)?.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleServiceConnection = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.let { i ->
                        when (i.action) {
                            ACTION_GATT_CONNECTING -> {
                                "连接中---------------------".log()
                            }
                            ACTION_GATT_CONNECT_TIMEOUT -> {
                                "连接超时---------------------${Thread.currentThread().name}".log()
                            }
                            ACTION_GATT_CONNECTED -> {
                                "连接成功---------------------".log()
                            }
                            ACTION_GATT_DISCONNECTED -> {
                                "连接失败---------------------".log()
                            }
                            ACTION_GATT_SERVICES_DISCOVERED -> {
                                "发现服务---------------------".log()
                            }
                            ACTION_GATT_SERVICES_DISCOVERED_FAIL -> {
                                "发现服务 失败---------------------".log()
                            }
                            ACTION_DATA_AVAILABLE_SUCCESS -> {
                                "获取数据成功---------------------".log()
                                "获取数据成功 UUID---------------------${i.getStringExtra(EXTRA_DATA_UUID)}".log()
                                "获取数据成功 内容---------------------${i.getStringExtra(EXTRA_DATA_CONTENT)}".log()
                            }
                            ACTION_DATA_AVAILABLE_FAIL -> {
                                "获取数据失败---------------------".log()
                            }
                            ACTION_DATA_WRITE_SUCCESS -> {
                                "写入数据成功---------------------".log()
                            }
                            ACTION_DATA_WRITE_FAIL -> {
                                "写入数据失败---------------------".log()
                            }
                            else -> {
                            }
                        }
                    }
                }
            }, IntentFilter().apply {
                this.addAction(ACTION_GATT_CONNECTED)
                this.addAction(ACTION_GATT_CONNECTING)
                this.addAction(ACTION_GATT_CONNECT_TIMEOUT)
                this.addAction(ACTION_GATT_DISCONNECTED)
                this.addAction(ACTION_GATT_SERVICES_DISCOVERED)
                this.addAction(ACTION_GATT_SERVICES_DISCOVERED_FAIL)
                this.addAction(ACTION_DATA_AVAILABLE_SUCCESS)
                this.addAction(ACTION_DATA_AVAILABLE_FAIL)
                this.addAction(ACTION_DATA_WRITE_SUCCESS)
                this.addAction(ACTION_DATA_WRITE_FAIL)
            }
        )

        findViewById<Button>(R.id.read_bt).setOnClickListener {
            bleServiceConnection?.readData(
                UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
            )

            Handler().postDelayed({
                bleServiceConnection?.setBleNotification(
                    true,
                    BLE_SERVICE_UUID,
                    NOTIFY_CHARACTER_UUID,
                    NOTIFY_DESCRIPTOR_UUID
                )
            }, 500)
        }
        findViewById<Button>(R.id.write_bt).setOnClickListener {
            bleServiceConnection?.sendData(
                ByteUtil.hexStr2Byte("18110100002A81"), BLE_SERVICE_UUID,
                WRITE_CHARACTER_UUID
            )
        }


        bindService(
            Intent(this, BleOperationService::class.java),
            serviceConnection, Context.BIND_AUTO_CREATE
        )

        derviceAdapter = object : BaseQuickAdapter<BluetoothDevice, BaseViewHolder>(
            R.layout.main_ble_device_list_item, bleDeviceList
        ) {
            override fun convert(holder: BaseViewHolder, item: BluetoothDevice) {
                holder.setText(R.id.device_name_tv, item.name + ":" + item.address)
            }
        }
        derviceAdapter.addChildClickViewIds(R.id.connection_bt)
        derviceAdapter.setOnItemChildClickListener { adapter, view, position ->
            when (view.id) {
                R.id.connection_bt -> {
                    BleOperationHelp.stopScanBle(this)
                    bleServiceConnection?.connect(bleDeviceList[position])
                }
                else -> {
                }
            }
        }
        val dervice_rl = findViewById<RecyclerView>(R.id.dervice_rl)
        dervice_rl.layoutManager = LinearLayoutManager(this)
        dervice_rl.adapter = derviceAdapter

        val scan_bt = findViewById<Button>(R.id.scan_bt)
        scan_bt.setOnClickListener {
            BleOperationHelp.startScanBle(this, object : BleOperation.BleOperationScanCallback {
                override fun onScanStart() {
                    "开始扫描".log()
                    scan_bt.isEnabled = false
                    bleDeviceList.clear()
                    derviceAdapter.notifyDataSetChanged()
                }

                override fun onScanStop() {
                    "停止扫描".log()
                    scan_bt.isEnabled = true
                }

                override fun onScanResult(device: BluetoothDevice?) {
                    "扫描结果name：${device?.name}".log()
                    "扫描结果address：${device?.address}".log()
                    device?.let {
                        if (!bleDeviceList.contains(it)) {
                            bleDeviceList.add(it)
                            derviceAdapter.notifyDataSetChanged()
                        }
                    }
                }
            })
        }

        val stop_scan = findViewById<Button>(R.id.stop_scan)
        stop_scan.setOnClickListener {
            BleOperationHelp.stopScanBle(this)
        }
        val close_bt = findViewById<Button>(R.id.close_bt)
        close_bt.setOnClickListener {
            bleServiceConnection?.release()
        }
    }
}

fun Any.log() {
    Log.d("my-log", "$this")
}