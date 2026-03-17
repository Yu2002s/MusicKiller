package xyz.jdynb.music.model.download

import androidx.databinding.Bindable
import androidx.databinding.PropertyChangeRegistry
import com.drake.engine.databinding.ObservableImpl
import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport
import xyz.jdynb.music.R
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.utils.DownloadHelper

/**
 * 下载对象
 */
data class DownloadModel(
  /**
   * 音乐 ID (唯一标识)
   */
  @Column(nullable = false, unique = true)
  val musicId: Long = 0,

  /**
   * 下载名称
   */
  val name: String = "",

  /**
   * 艺术家
   */
  val artist: String = "",

  /**
   * 专辑
   */
  val album: String = "",

  /**
   * 封面图
   */
  val cover: String = "",

  /**
   * 时长
   */
  val duration: Long = 0,

  /**
   * 下载 URL
   */
  @Column(nullable = false)
  val downloadUrl: String = "",

  /**
   * 音质 (128kmp3, 192kmp3, 320kmp3, 2000kflac)
   */
  val musicBridge: String = "",

  /**
   * 文件格式 (mp3, flac)
   */
  val fileFormat: String = "mp3",

  /**
   * 文件大小 (字节)
   */
  val fileSize: Long = 0,

  /**
   * 已下载大小 (字节)
   */
  var downloadedSize: Long = 0,

  /**
   * 下载状态
   */
  @Column(nullable = false)
  var status: Int = STATUS_PENDING,

  /**
   * 本地文件路径
   */
  var localPath: String = "",

  /**
   * 错误信息
   */
  var errorMessage: String = "",

  /**
   * 创建时间
   */
  val createAt: Long = System.currentTimeMillis(),

  /**
   * 更新时间
   */
  var updateAt: Long = createAt,

  /**
   * 开始下载时间
   */
  var startAt: Long = 0,

  /**
   * 完成时间
   */
  var completeAt: Long = 0,
) : LitePalSupport(), ObservableImpl {

  override val registry: PropertyChangeRegistry = PropertyChangeRegistry()

  /**
   * 唯一 ID
   */
  var id: Long = 0

  /**
   * 下载进度 (0-100)
   */
  @get:Bindable
  val progress: Int
    get() = if (fileSize > 0) {
      ((downloadedSize * 100) / fileSize).toInt()
    } else 0

  /**
   * 是否已完成
   */
  @get:Bindable
  val isCompleted: Boolean
    get() = status == STATUS_COMPLETED

  /**
   * 是否正在下载
   */
  @get:Bindable
  val isDownloading: Boolean
    get() = status == STATUS_DOWNLOADING

  /**
   * 是否已暂停
   */
  @get:Bindable
  val isPaused: Boolean
    get() = status == STATUS_PAUSED

  /**
   * 是否失败
   */
  @get:Bindable
  val isFailed: Boolean
    get() = status == STATUS_FAILED

  /**
   * 是否等待中
   */
  @get:Bindable
  val isPending: Boolean
    get() = status == STATUS_PENDING

  @get:Bindable
  val statusStr: String
    get() = when (status) {
      STATUS_PENDING -> "等待中"
      STATUS_DOWNLOADING -> "下载中(${progress}%)"
      STATUS_PAUSED -> "已暂停(${progress}%)"
      STATUS_COMPLETED -> "已完成"
      STATUS_FAILED -> "失败: $errorMessage"
      else -> ""
    }

  @get:Bindable
  val fileSizeStr: String
    get() = "${DownloadHelper.formatFileSize(downloadedSize)} / ${DownloadHelper.formatFileSize(fileSize)}"

  @get:Bindable
  val playIcon: Int
    get() = if (status == STATUS_PAUSED) R.drawable.baseline_play_arrow_24 else R.drawable.baseline_pause_24

  companion object {
    /**
     * 等待中
     */
    const val STATUS_PENDING = 0

    /**
     * 下载中
     */
    const val STATUS_DOWNLOADING = 1

    /**
     * 已暂停
     */
    const val STATUS_PAUSED = 2

    /**
     * 已完成
     */
    const val STATUS_COMPLETED = 3

    /**
     * 失败
     */
    const val STATUS_FAILED = 4

    /**
     * 从 MusicModel 创建 DownloadModel
     */
    @JvmStatic
    fun from(
      musicModel: MusicModel,
      downloadUrl: String,
      bridge: String,
      format: String,
      fileSize: Long
    ): DownloadModel {
      return DownloadModel(
        musicId = musicModel.id,
        name = musicModel.name,
        artist = musicModel.artist,
        album = musicModel.album,
        cover = musicModel.pic,
        duration = musicModel.duration,
        downloadUrl = downloadUrl,
        musicBridge = bridge,
        fileFormat = format,
        fileSize = fileSize
      )
    }
  }
}