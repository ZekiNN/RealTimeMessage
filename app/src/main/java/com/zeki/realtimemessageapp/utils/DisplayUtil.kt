package com.zeki.realtimemessageapp.utils

import android.content.Context

/**
 * @Author: ZEKI https://github.com/ZYF99
 * @Date: 2021/5/21 5:10 下午
 */
class DisplayUtil {
    var screenWidthPx //屏幕宽 px
            = 0
    var screenhightPx //屏幕高 px
            = 0
    var density //屏幕密度
            = 0f
    var densityDPI //屏幕密度
            = 0
    var screenWidthDip //  dp单位
            = 0f
    var screenHightDip //  dp单位
            = 0f

}

/**
 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
 */
fun dip2px(context: Context, dpValue: Float): Int {
    val scale = context.resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

/**
 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
 */
fun px2dip(context: Context, pxValue: Float): Int {
    val scale = context.resources.displayMetrics.density
    return (pxValue / scale + 0.5f).toInt()
}