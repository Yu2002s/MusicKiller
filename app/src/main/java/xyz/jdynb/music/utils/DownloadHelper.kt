package xyz.jdynb.music.utils

import android.content.Context
import android.os.Environment
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.download.DownloadModel
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * 下载辅助工具类
 */
object DownloadHelper {

  /**
   * 生成下载文件名
   * 格式: {musicId}_{name}_{artist}.{format}
   */
  @JvmStatic
  fun generateFileName(downloadModel: DownloadModel): String {
    // 移除特殊字符，保留中英文、数字、空格
    val safeName = downloadModel.name.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5\\s]"), "")
    val safeArtist = downloadModel.artist.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5\\s]"), "")
    return "${safeName}-${safeArtist}-${downloadModel.musicId}.${downloadModel.fileFormat}"
  }

  /**
   * 获取下载目录
   * 如果不同意权限路径: /storage/emulated/0/Android/data/xyz.jdynb.music/files/Music/MusicKiller/
   * 如果同意权限：/storage/emulated/0/Download/MusicKiller/
   */
  @JvmStatic
  fun getDownloadDirectory(context: Context): File {
    val isGranted = XXPermissions.isGrantedPermissions(context, Permission.MANAGE_EXTERNAL_STORAGE)
    val musicDir = if (isGranted) Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
     else context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
    val downloadDir = File(musicDir, "MusicKiller")
    if (!downloadDir.exists()) {
      downloadDir.mkdirs()
    }
    return downloadDir
  }

  /**
   * 检查存储空间是否足够
   * @param context Context
   * @param sizeNeeded 需要的空间大小（字节）
   * @return 是否有足够空间
   */
  @JvmStatic
  fun hasEnoughSpace(context: Context, sizeNeeded: Long): Boolean {
    val downloadDir = getDownloadDirectory(context)
    val usableSpace = downloadDir.usableSpace
    // 保留 100MB 缓冲
    return usableSpace > sizeNeeded + 100_000_000
  }

  /**
   * 格式化文件大小显示
   * @param bytes 字节数
   * @return 格式化后的字符串，如 "12.5 MB"
   */
  @JvmStatic
  fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    val df = DecimalFormat("#,##0.#")
    return df.format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
  }

  /**
   * 计算下载速度
   * @param downloadedBytes 已下载字节数
   * @param elapsedMillis 已用时间（毫秒）
   * @return 速度字符串，如 "1.2 MB/s"
   */
  @JvmStatic
  fun formatSpeed(downloadedBytes: Long, elapsedMillis: Long): String {
    if (elapsedMillis <= 0) return "0 B/s"
    val bytesPerSecond = (downloadedBytes * 1000) / elapsedMillis
    return "${formatFileSize(bytesPerSecond)}/s"
  }

  /**
   * 估算剩余时间
   * @param remainingBytes 剩余字节数
   * @param bytesPerSecond 每秒下载字节数
   * @return 剩余时间字符串，如 "2分30秒"
   */
  @JvmStatic
  fun formatRemainingTime(remainingBytes: Long, bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return "未知"
    val remainingSeconds = remainingBytes / bytesPerSecond

    return when {
      remainingSeconds < 60 -> "${remainingSeconds}秒"
      remainingSeconds < 3600 -> {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        "${minutes}分${seconds}秒"
      }
      else -> {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        "${hours}小时${minutes}分"
      }
    }
  }

  /**
   * 从文件名提取音乐 ID
   */
  @JvmStatic
  fun extractMusicIdFromFileName(fileName: String): Long? {
    return fileName.split("-").firstOrNull()?.toLongOrNull()
  }

  /**
   * 获取完整文件路径
   */
  @JvmStatic
  fun getFilePath(context: Context, downloadModel: DownloadModel): File {
    return File(getDownloadDirectory(context), generateFileName(downloadModel))
  }

  @JvmStatic
  fun getLyricFilePath(context: Context, musicModel: MusicModel): File {
    val file = File(getDownloadDirectory(context), "lyric")
    if (!file.exists()) {
      file.mkdirs()
    }
    return File(file, "${musicModel.name}-${musicModel.artist}.lrc")
  }
}
