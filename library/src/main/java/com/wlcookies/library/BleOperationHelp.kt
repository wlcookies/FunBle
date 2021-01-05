package com.wlcookies.library

import androidx.fragment.app.FragmentActivity

object BleOperationHelp {

    private const val tag = "ble_fragment"

    /**
     * 扫描Ble设备
     * @see BleOperation
     */
    fun startScanBle(
        activity: FragmentActivity,
        bleOperationScanCallback: BleOperation.BleOperationScanCallback
    ) {
        val fm = activity.supportFragmentManager
        var bleOperation = fm.findFragmentByTag(tag) as? BleOperation
        if (bleOperation == null) {
            bleOperation = BleOperation.newInstance()
            fm.beginTransaction().add(bleOperation, tag).commitAllowingStateLoss()
        }
        fm.executePendingTransactions()
        if (bleOperation.bleOperationScanCallback == null) {
            bleOperation.bleOperationScanCallback = bleOperationScanCallback
        }
        bleOperation.readyScanBle()
    }

    /**
     * 停止扫描
     */
    fun stopScanBle(activity: FragmentActivity) {
        (activity.supportFragmentManager.findFragmentByTag(tag) as? BleOperation)?.doStopScan()
    }
}