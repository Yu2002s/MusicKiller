package xyz.jdynb.music.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.drake.net.request.BodyRequest
import com.drake.net.request.MediaConst
import com.drake.net.request.UrlRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.jdynb.music.MusicKillerApplication
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.reflect.KClass

/*
     一些通用的方法
 */

/**
 * 开启 activity
 * @param args 传递的参数
 */
inline fun <reified T> startActivity(vararg args: Pair<String, Any>) {
  val context = MusicKillerApplication.context
  context.startActivity(Intent(context, T::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    args.forEach {
      when (val second = it.second) {
        is String -> putExtra(it.first, second)
        is Int -> putExtra(it.first, second)
        is Float -> putExtra(it.first, second)
        is Parcelable -> putExtra(it.first, second)
      }
    }
  })
}

fun startActivity(clazz: KClass<*>, block: (Intent.() -> Intent)? = null) {
  val context = MusicKillerApplication.context
  val intent = Intent(context, clazz.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  block?.let {
    intent.block()
  }
  context.startActivity(intent)
}

/*fun BodyRequest.gson(vararg body: Pair<String, Any?>) {
  this.body = Gson().toJson(body.toMap()).toRequestBody(MediaConst.JSON)
}*/

/**
 * Bundle 中添加序列化参数
 */
inline fun <reified T : @Serializable Any> Bundle.putSerializable(key: String, value: T) {
  val jsonString = Json.encodeToString(value)
  putString(key, jsonString)
}

inline fun <reified T : @Serializable Any> Intent.putSerializable(key: String, value: T) {
  putExtra(key, Json.encodeToString(value))
}

/**
 * Bundle 中获取序列化参数
 */
inline fun <reified T : @Serializable Any> Bundle.getSerializableForKey(key: String): T? {
  val jsonString = getString(key) ?: return null
  return Json.decodeFromString<T>(jsonString)
}

/**
 * Fragment 中添加序列化参数
 */
inline fun <reified T : @Serializable Any> Fragment.setSerializableArguments(
  key: String,
  value: T
) {
  arguments = (arguments ?: Bundle()).apply {
    putSerializable(key, value)
  }
}

/**
 * Fragment 中获取序列化参数
 */
inline fun <reified T : @Serializable Any> Fragment.getSerializableArguments(key: String): T? {
  return arguments?.getSerializableForKey(key)
}

inline fun <reified T : @Serializable Any> Activity.setSerializableArguments(
  key: String,
  value: T
) {
  intent.extras?.putSerializable(key, value)
}

inline fun <reified T : @Serializable Any> Activity.getSerializableArguments(key: String): T? {
  return intent.extras?.getSerializableForKey(key)
}

val json = Json {
  // 序列化默认值
  encodeDefaults = true
  ignoreUnknownKeys = true
  coerceInputValues = true
  explicitNulls = false
}

/**
 * 扩展支持直接发送对象并转换为 json
 */
inline fun <reified T> BodyRequest.json(body: T): BodyRequest {
  this.body = json.encodeToString(body)
    .toRequestBody(MediaConst.JSON)
  return this
}

/**
 * 扩展支持 query 参数
 */
inline fun <reified T> UrlRequest.query(params: T): UrlRequest {
  val clazz = params::class.java
  clazz.declaredFields.forEach {
    it.isAccessible = true
    when (val value: Any? = it.get(params)) {
      is String -> addQuery(it.name, value)
      is Number -> addQuery(it.name, value)
      is Boolean -> addQuery(it.name, value)
      // else -> addQuery(it.name, "")
    }
  }
  return this
}

/**
 * 字节转大小
 */
fun Long.formatBytes(): String {
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  if (this == 0L) return "0 B"
  var currentBytes = this.toDouble()
  var unitIndex = 0

  while (currentBytes >= 1024 && unitIndex < units.size - 1) {
    currentBytes /= 1024.0
    unitIndex++
  }

  return "%.2f %s".format(currentBytes, units[unitIndex])
}

fun Long.formatCN(): String {
  if (this < 10000) {
    return this.toString()
  }
  val numberFormat = NumberFormat.getInstance()
  numberFormat.minimumFractionDigits = 2
  numberFormat.maximumFractionDigits = 2
  val yi = 10000 * 10000
  if (this < yi) {
    return numberFormat.format(this / 10000F) + "w"
  }
  return numberFormat.format(this / yi.toFloat()) + "亿"
}

/**
 * 时间戳转日期
 */
fun Long.toDate(pattern: String = "yyyy-MM-dd"): String {
  try {
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.CHINA)
    return simpleDateFormat.format(this)
  } catch (_: IllegalArgumentException) {
    return ""
  }
}

/**
 * Context 转 activity
 */
fun Context.activity(): Activity? {
  if (this is Activity) {
    return this
  } else if (this is ContextWrapper) {
    return this.baseContext.activity()
  }
  return null
}

/**
 * vector 开始动画
 */
fun Drawable.startAnimation() {
  when (this) {
    is AnimatedVectorDrawable -> start()
    is AnimatedVectorDrawableCompat -> start()
    else -> throw IllegalArgumentException()
  }
}
