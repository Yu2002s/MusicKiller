package xyz.jdynb.music.enums

/**
 * 下载状态
 */
enum class DownloadStatus(val value: Int, val displayName: String) {
  PENDING(0, "等待中"),
  DOWNLOADING(1, "下载中"),
  PAUSED(2, "已暂停"),
  COMPLETED(3, "已完成"),
  FAILED(4, "失败");

  companion object {
    @JvmStatic
    fun fromValue(value: Int): DownloadStatus {
      return entries.find { it.value == value } ?: PENDING
    }
  }
}
