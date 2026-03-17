package xyz.jdynb.music.utils.lyric

import com.drake.net.Get
import kotlinx.coroutines.coroutineScope
import okio.ByteString

object LyricUtils {

  private const val LYRICS_URL = "http://newlyric.kuwo.cn/newlyric.lrc"

  @JvmStatic
  suspend fun getLyricContent(musicId: Long) = coroutineScope {
    // 1. 构建请求参数
    val params = KuwoLyricDecryptor.buildParams(musicId, false)

    // 2. 发起HTTP请求获取响应数据
    val responseData = Get<ByteString>("${LYRICS_URL}?$params").await()

    // 3. 解密歌词
    val rawLyric = KuwoLyricDecryptor.decodeLyrics(responseData.toByteArray(), false)

    // 4. 转换格式
    return@coroutineScope KuwoLyricDecryptor.convertKuwoLrc(rawLyric)
  }

}