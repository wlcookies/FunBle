package com.wlcookies.library.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings

object CommonUtil {

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
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).takeIf { it.resolveActivity(context.packageManager) != null }
            ?.apply {
                context.startActivity(this)
            }
    }
}