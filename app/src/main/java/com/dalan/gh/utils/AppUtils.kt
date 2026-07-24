package com.dalan.gh.utils

import android.content.pm.PackageInfo
import android.os.Build
import com.dalan.gh.app.GhApplication.Companion.appContext

/**
 * 获取 APP 包信息 - [PackageInfo]
 */
inline val packageInfo: PackageInfo
    get() = appContext.packageManager.getPackageInfo(appContext.packageName, 0)

/**
 * 获取 APP versionCode
 */
inline val appVersionCode: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }

/**
 * 过去 APP versionName
 */
inline val appVersionName: String
    get() = packageInfo.versionName