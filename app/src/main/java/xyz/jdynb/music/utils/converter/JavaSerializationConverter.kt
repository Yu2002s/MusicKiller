@file:Suppress("UNCHECKED_CAST")

package xyz.jdynb.music.utils.converter

import com.drake.net.convert.JSONConvert
import kotlinx.serialization.serializer
import org.json.JSONObject
import xyz.jdynb.music.utils.json
import java.lang.reflect.Type

/**
 * Java 特定的序列化转换器
 */
class JavaSerializationConverter : JSONConvert(code = "200") {
  override fun <R> String.parseBody(succeed: Type): R? {
    val jsonObject = JSONObject(this)
    val jsonData = jsonObject.getJSONObject("data").toString()
    return json.decodeFromString(json.serializersModule.serializer(succeed), jsonData) as R?
  }
}