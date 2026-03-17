package xyz.jdynb.music.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.drake.net.Get
import com.drake.tooltip.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.litepal.LitePal
import org.litepal.extension.find
import org.litepal.extension.findFirst
import xyz.jdynb.music.R
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.enums.MusicBridge
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.PlayInfo
import xyz.jdynb.music.model.download.DownloadModel
import xyz.jdynb.music.ui.activity.MainActivity
import xyz.jdynb.music.utils.DownloadHelper
import xyz.jdynb.music.utils.lyric.LyricUtils
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.LinkedList

/**
 * 下载服务
 */
class DownloadService : Service() {

  companion object {
    private const val TAG = "DownloadService"

    /**
     * 通知 ID
     */
    private const val NOTIFICATION_ID = 2001

    /**
     * 通知渠道 ID
     */
    private const val CHANNEL_ID = "download_channel"

    /**
     * 通知渠道名
     */
    private const val CHANNEL_NAME = "音乐下载"

    /**
     * 自定义下载 ACTION_NAME
     */
    const val ACTION_ADD_DOWNLOAD = "action_add_download"

    /**
     * 传递所需的参数
     */
    const val EXTRA_MUSIC_MODEL = "extra_music_model"
    const val EXTRA_BRIDGE = "extra_bridge"
  }

  /**
   * binder 用于外部通信
   */
  private val binder = DownloadBinder()

  // 下载队列
  private val downloadQueue = LinkedList<DownloadTask>()

  /**
   * 当前下载任务
   */
  private var currentTask: DownloadTask? = null

  // 自定义协成作用域
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  // 协程下载限制，最大同时进行3个任务
  private val downloadSemaphore = Semaphore(3)

  // 通过 StateFlow 更新下载任务
  private val _downloadProgress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
  val downloadProgress: StateFlow<Map<Long, DownloadProgress>> = _downloadProgress.asStateFlow()

  // Notification
  private lateinit var notificationManager: NotificationManager

  // OkHttpClient
  private val okHttpClient = OkHttpClient()

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "DownloadService onCreate")
    createNotificationChannel()
    // startForeground(NOTIFICATION_ID, createNotification())
    // restoreQueueState()
  }

  override fun onBind(intent: Intent?): IBinder {
    Log.d(TAG, "DownloadService onBind")
    return binder
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "DownloadService onStartCommand: ${intent?.action}")

    when (intent?.action) {
      ACTION_ADD_DOWNLOAD -> {
        val musicModelJson = intent.getStringExtra(EXTRA_MUSIC_MODEL)
        val bridgeLevel = intent.getStringExtra(EXTRA_BRIDGE)

        if (musicModelJson != null && bridgeLevel != null) {
          try {
            val musicModel =
              kotlinx.serialization.json.Json.decodeFromString<MusicModel>(musicModelJson)
            val bridge = MusicBridge.getBridgeForLevel(bridgeLevel)
            addDownload(musicModel, bridge)
          } catch (e: Exception) {
            Log.e(TAG, "Failed to parse music model", e)
            toast("下载失败：数据解析错误")
          }
        }
      }
    }

    return START_STICKY
  }

  override fun onDestroy() {
    Log.d(TAG, "DownloadService onDestroy")
    // 在销毁之前更新下载队列状态
    saveQueueState()
    // 取消任务
    serviceScope.cancel()
    super.onDestroy()
  }

  inner class DownloadBinder : Binder() {
    fun getService(): DownloadService = this@DownloadService
  }

  /**
   * 添加到下载任务
   */
  fun addDownload(musicModel: MusicModel, bridge: MusicBridge) {
    serviceScope.launch {
      try {
        // 查询数据库获取下载文件是否存在
        val existing = LitePal.where("musicId = ?", musicModel.id.toString())
          .findFirst<DownloadModel>()

        if (existing != null) {
          // 如果是存在的
          if (existing.status == DownloadModel.STATUS_COMPLETED) {
            // 如果是已经完成的下载
            withContext(Dispatchers.Main) {
              toast("该歌曲已下载")
            }
            return@launch
          } else {
            // 如果不是，则恢复继续下载
            resumeDownload(existing.musicId)
            withContext(Dispatchers.Main) {
              toast("已添加到下载队列")
            }
            return@launch
          }
        }

        // 准备下载（获取下载地址和大小）
        val downloadModel = prepareDownload(musicModel, bridge)

        // 检查内存是否足够
        if (!DownloadHelper.hasEnoughSpace(this@DownloadService, downloadModel.fileSize)) {
          withContext(Dispatchers.Main) {
            toast("存储空间不足")
          }
          return@launch
        }

        // 保存到数据库中
        downloadModel.save()

        // 添加到下载队列中
        addToQueue(downloadModel)

        withContext(Dispatchers.Main) {
          toast("已添加到下载队列")
        }

        Log.d(TAG, "Added download: ${downloadModel.name}")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to add download", e)
        withContext(Dispatchers.Main) {
          toast("添加下载失败：${e.message}")
        }
      }
    }
  }

  /**
   * 暂停下载
   */
  fun pauseDownload(musicId: Long) {
    serviceScope.launch {
      val task = downloadQueue.find { it.downloadModel.musicId == musicId }
      task?.let {
        it.job?.cancel()
        it.downloadModel.status = DownloadModel.STATUS_PAUSED
        it.downloadModel.updateAt = System.currentTimeMillis()
        it.downloadModel.update(it.downloadModel.id)
        downloadQueue.remove(it)

        updateProgress(
          musicId, DownloadProgress(
            musicId = musicId,
            downloadedSize = it.downloadModel.downloadedSize,
            totalSize = it.downloadModel.fileSize,
            progress = it.downloadModel.progress,
            status = DownloadModel.STATUS_PAUSED
          )
        )

        Log.d(TAG, "Paused download: ${it.downloadModel.name}")
      }
    }
  }

  /**
   * 恢复下载
   */
  fun resumeDownload(musicId: Long) {
    serviceScope.launch {
      val downloadModel = LitePal.where("musicId = ?", musicId.toString())
        .findFirst<DownloadModel>()

      downloadModel?.let {
        it.status = DownloadModel.STATUS_PENDING
        it.errorMessage = ""
        it.updateAt = System.currentTimeMillis()
        it.update(it.id)

        addToQueue(it)
        Log.d(TAG, "Resumed download: ${it.name}")
      }
    }
  }

  /**
   * 取消下载
   */
  fun cancelDownload(musicId: Long) {
    serviceScope.launch {
      val task = downloadQueue.find { it.downloadModel.musicId == musicId }
      task?.let {
        it.job?.cancel()
        downloadQueue.remove(it)

        // 删除已下载的文件
        val file = DownloadHelper.getFilePath(this@DownloadService, it.downloadModel)
        if (file.exists() && it.downloadModel.status != DownloadModel.STATUS_COMPLETED) {
          file.delete()
        }

        // 从数据库中删除
        LitePal.deleteAll(DownloadModel::class.java, "musicId = ?", musicId.toString())

        // 删除任务监听
        _downloadProgress.update { map ->
          map.filterKeys { key -> key != musicId }
        }

        Log.d(TAG, "Cancelled download: ${it.downloadModel.name}")
      }
    }
  }

  /**
   * 重试下载错误的任务
   */
  fun retryDownload(musicId: Long) {
    serviceScope.launch {
      val downloadModel = LitePal.where("musicId = ?", musicId.toString())
        .findFirst<DownloadModel>()

      downloadModel?.let {
        // 检查下载任务是否过期
        if (System.currentTimeMillis() - it.createAt > 3600000) {
          try {
            // 重新获取下载地址
            val playInfo = Get<PlayInfo>(Api.PLAY_INFO) {
              addQuery("id", it.musicId)
              addQuery("bridge", it.musicBridge)
            }.await()

            val updatedModel = it.copy(
              downloadUrl = playInfo.url,
              createAt = System.currentTimeMillis()
            )
            updatedModel.id = it.id
            updatedModel.status = DownloadModel.STATUS_PENDING
            updatedModel.errorMessage = ""
            updatedModel.updateAt = System.currentTimeMillis()
            updatedModel.update(updatedModel.id)

            // 添加到下载队列
            addToQueue(updatedModel)
            Log.d(TAG, "Retried download with new URL: ${it.name}")
          } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh download URL", e)
            withContext(Dispatchers.Main) {
              toast("重试失败：无法获取下载链接")
            }
          }
        } else {
          // 重新进行下载
          it.status = DownloadModel.STATUS_PENDING
          it.errorMessage = ""
          it.updateAt = System.currentTimeMillis()
          it.update(it.id)

          // 添加到下载队列
          addToQueue(it)
          Log.d(TAG, "Retried download: ${it.name}")
        }
      }
    }
  }

  /**
   * 删除下载
   *
   * @param musicId 音乐 id
   */
  fun deleteDownload(musicId: Long) {
    serviceScope.launch {
      // 从数据库中查询下载任务
      val downloadModel = LitePal.where("musicId = ?", musicId.toString())
        .findFirst<DownloadModel>()

      // 如果获取到下载
      downloadModel?.let {
        // 取消指定的下载服务
        val task = downloadQueue.find { task -> task.downloadModel.musicId == musicId }
        task?.job?.cancel()
        // 从下载队列中移除
        downloadQueue.remove(task)

        // 删除已下载文件
        if (it.localPath.isNotEmpty()) {
          val file = File(it.localPath)
          if (file.exists()) {
            file.delete()
          }
        }

        // 从数据库中删除
        LitePal.deleteAll(DownloadModel::class.java, "musicId = ?", musicId.toString())

        // 移除进度监听
        _downloadProgress.update { map ->
          map.filterKeys { key -> key != musicId }
        }

        withContext(Dispatchers.Main) {
          toast("已删除")
        }

        Log.d(TAG, "Deleted download: ${it.name}")
      }
    }
  }

  /**
   * 准备下载文件：获取下载文件地址和文件大小
   *
   * @param musicModel 音乐实体
   * @param bridge 音质
   * @return DownloadModel
   */
  private suspend fun prepareDownload(musicModel: MusicModel, bridge: MusicBridge) =
    coroutineScope {
      // 获取下载地址
      val playInfo = Get<PlayInfo>(Api.PLAY_INFO) {
        addQuery("id", musicModel.id)
        addQuery("bridge", bridge.level)
      }.await()

      val lyricFile = DownloadHelper.getLyricFilePath(this@DownloadService, musicModel)
      if (!lyricFile.exists()) {
        val lyricContent = LyricUtils.getLyricContent(musicModel.id)
        lyricFile.createNewFile()
        val fileWriter = FileWriter(lyricFile)
        fileWriter.write(lyricContent)
        fileWriter.close()
      }

      // 通过 HEAD 请求获取文件大小
      val fileSize = withContext(Dispatchers.IO) {
        try {
          val connection = URL(playInfo.url).openConnection() as HttpURLConnection
          connection.requestMethod = "HEAD"
          connection.connect()
          val size = connection.contentLengthLong
          connection.disconnect()
          size
        } catch (e: Exception) {
          // 请求失败，没有获取到文件大小
          Log.e(TAG, "Failed to get file size", e)
          0L
        }
      }

      DownloadModel.from(
        musicModel = musicModel,
        downloadUrl = playInfo.url,
        bridge = bridge.level,
        format = playInfo.format.ifEmpty {
          val lastIndex = playInfo.url.lastIndexOf('.')
          if (lastIndex == -1) {
            return@ifEmpty "mp3"
          }
          val ext = playInfo.url.substring(lastIndex + 1)
          ext.ifBlank { "mp3" }
        },
        fileSize = fileSize
      )
    }

  /**
   * 添加下载任务到下载队列中
   */
  private fun addToQueue(downloadModel: DownloadModel) {
    val task = DownloadTask(downloadModel)
    downloadQueue.add(task)
    processQueue()
  }

  /**
   * 处理下载队列
   */
  private fun processQueue() {
    if (downloadQueue.isEmpty()) {
      return
    }

    downloadQueue.forEach { task ->
      // 便利所有等待下载的任务
      if (task.job == null && task.downloadModel.status == DownloadModel.STATUS_PENDING) {
        // 如果有等待下载的，就加入到下载队列中
        task.job = serviceScope.launch {
          downloadSemaphore.withPermit {
            try {
              currentTask = task
              downloadFile(task)
            } catch (e: CancellationException) {
              Log.d(TAG, "Download cancelled: ${task.downloadModel.name}")
            } catch (e: Exception) {
              Log.e(TAG, "Download failed: ${task.downloadModel.name}", e)
              handleDownloadError(task.downloadModel, e.message ?: "未知错误")
            } finally {
              downloadQueue.remove(task)
              updateNotification()
              currentTask = null
            }
          }
        }
      }
    }
  }

  /**
   * 开始下载文件
   */
  private suspend fun downloadFile(task: DownloadTask) = withContext(Dispatchers.IO) {
    val downloadModel = task.downloadModel
    val file = DownloadHelper.getFilePath(this@DownloadService, downloadModel)

    // 判断文件是否存在
    val downloadedSize = if (file.exists()) file.length() else 0L
    downloadModel.downloadedSize = downloadedSize

    // 更新下载状态
    downloadModel.status = DownloadModel.STATUS_DOWNLOADING
    downloadModel.startAt = System.currentTimeMillis()
    downloadModel.updateAt = downloadModel.startAt
    // 更新到数据库中
    downloadModel.update(downloadModel.id)

    val request = Request.Builder()
      .url(downloadModel.downloadUrl)
      .apply {
        // 如果之前有下载的，就进行短点续传
        if (downloadedSize > 0) {
          addHeader("Range", "bytes=$downloadedSize-")
        }
      }
      .build()

    val response = okHttpClient.newCall(request).execute()

    if (!response.isSuccessful) {
      handleDownloadError(downloadModel, "HTTP ${response.code}")
      return@withContext
    }

    val inputStream = response.body?.byteStream()
      ?: throw IOException("Response body is null")

    val outputStream = FileOutputStream(file, downloadedSize > 0)

    try {
      val buffer = ByteArray(8192)
      var bytesRead: Int
      var totalBytesRead = downloadedSize
      var lastUpdateTime = System.currentTimeMillis()
      val startTime = System.currentTimeMillis()

      while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        // 检查任务是否取消了
        if (!task.job?.isActive!!) break

        outputStream.write(buffer, 0, bytesRead)
        totalBytesRead += bytesRead

        // 500ms 更新一次
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= 500) {
          val elapsedTime = currentTime - startTime
          val speed = if (elapsedTime > 0) {
            ((totalBytesRead - downloadedSize) * 1000) / elapsedTime
          } else 0

          updateProgress(
            downloadModel.musicId,
            DownloadProgress(
              musicId = downloadModel.musicId,
              downloadedSize = totalBytesRead,
              totalSize = downloadModel.fileSize,
              progress = ((totalBytesRead * 100) / downloadModel.fileSize).toInt(),
              status = DownloadModel.STATUS_DOWNLOADING,
              speed = speed
            )
          )

          // 更新数据到数据库中
          downloadModel.downloadedSize = totalBytesRead
          downloadModel.updateAt = currentTime
          downloadModel.update(downloadModel.id)

          lastUpdateTime = currentTime
          // 更新通知内容
          updateNotification()
        }
      }

      outputStream.flush()

      // 检查是否下载完成
      if (totalBytesRead >= downloadModel.fileSize) {
        downloadModel.status = DownloadModel.STATUS_COMPLETED
        downloadModel.localPath = file.absolutePath
        downloadModel.completeAt = System.currentTimeMillis()
        downloadModel.downloadedSize = totalBytesRead
        downloadModel.updateAt = downloadModel.completeAt
        // 更新数据到数据库中
        downloadModel.update(downloadModel.id)

        // 重新更新进度，界面才会更新
        updateProgress(
          downloadModel.musicId,
          DownloadProgress(
            musicId = downloadModel.musicId,
            downloadedSize = totalBytesRead,
            totalSize = downloadModel.fileSize,
            progress = 100,
            status = DownloadModel.STATUS_COMPLETED
          )
        )

        Log.d(TAG, "Download completed: ${downloadModel.name}")

        withContext(Dispatchers.Main) {
          toast("下载完成：${downloadModel.name}")
        }
      }
    } finally {
      inputStream.close()
      outputStream.close()
    }
  }

  /**
   * 处理下载失败
   */
  private suspend fun handleDownloadError(downloadModel: DownloadModel, errorMessage: String) {
    downloadModel.status = DownloadModel.STATUS_FAILED
    downloadModel.errorMessage = errorMessage
    downloadModel.updateAt = System.currentTimeMillis()
    // 更新状态到数据库中
    downloadModel.update(downloadModel.id)

    // 之后更新进度状态，对应的流发生改变，下载队列中的界面才会触发更新
    updateProgress(
      downloadModel.musicId,
      DownloadProgress(
        musicId = downloadModel.musicId,
        downloadedSize = downloadModel.downloadedSize,
        totalSize = downloadModel.fileSize,
        progress = downloadModel.progress,
        status = DownloadModel.STATUS_FAILED
      )
    )

    withContext(Dispatchers.Main) {
      toast("下载失败：${downloadModel.name} - $errorMessage")
    }

    Log.e(TAG, "Download error: ${downloadModel.name} - $errorMessage")
  }

  /**
   * 更新下载进度
   *
   * @param musicId 音乐 id
   * @param progress 音乐进度实体类
   */
  private fun updateProgress(musicId: Long, progress: DownloadProgress) {
    _downloadProgress.update { currentMap ->
      // 之前的 Map put 新的内容，如果之前有键的更新Map，没有到追加到 Map，然后更新整个Map，状态流才会更新
      currentMap + (musicId to progress)
    }
  }

  /**
   * 创建下载渠道，适配高android版本（只需要执行一次）
   */
  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "显示音乐下载进度"
        setShowBadge(false)
      }

      notificationManager = getSystemService(NotificationManager::class.java)
      notificationManager.createNotificationChannel(channel)
    } else {
      notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
  }

  /**
   * 创建下载通知
   */
  private fun createNotification(): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this, 0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE
    )

    val currentDownload = currentTask?.downloadModel
    val queueSize = downloadQueue.size

    val title = currentDownload?.name
      ?: if (queueSize > 0) {
        "等待下载 ($queueSize)"
      } else getString(R.string.app_name)

    val text = currentDownload?.statusStr ?: "准备就绪"

    val smallIcon = if (currentDownload != null) {
      when (currentDownload.status) {
        DownloadModel.STATUS_PENDING, DownloadModel.STATUS_DOWNLOADING -> android.R.drawable.stat_sys_download
        DownloadModel.STATUS_PAUSED -> android.R.drawable.ic_media_pause
        DownloadModel.STATUS_FAILED -> android.R.drawable.stat_notify_error
        DownloadModel.STATUS_COMPLETED -> android.R.drawable.stat_sys_download_done
        else -> android.R.drawable.stat_sys_download
      }
    } else {
      android.R.drawable.stat_sys_download
    }

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(title)
      .setContentText(text)
      .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round))
      .setSmallIcon(smallIcon)
      .setOngoing(currentDownload?.status == DownloadModel.STATUS_DOWNLOADING)
      .setProgress(
        100,
        currentDownload?.progress ?: 0,
        (currentDownload?.status == DownloadModel.STATUS_PENDING)
      )
      .setContentIntent(pendingIntent)
      .build()
  }

  /**
   * 更新通知
   */
  private fun updateNotification() {
    notificationManager.notify(NOTIFICATION_ID, createNotification())
  }

  /**
   * 当服务创建时，将未下载的任务恢复下载
   */
  private fun restoreQueueState() {
    serviceScope.launch {
      val pendingDownloads = LitePal.where(
        "status = ? OR status = ?",
        DownloadModel.STATUS_PENDING.toString(),
        DownloadModel.STATUS_DOWNLOADING.toString()
      ).find<DownloadModel>()

      pendingDownloads.forEach { downloadModel ->
        downloadModel.status = DownloadModel.STATUS_PENDING
        downloadModel.update(downloadModel.id)
        addToQueue(downloadModel)
      }

      if (pendingDownloads.isNotEmpty()) {
        Log.d(TAG, "Restored ${pendingDownloads.size} downloads from database")
      }
    }
  }

  /**
   * 保存下载任务状态，如果是正在下载任务，设置为准备下载状态
   */
  private fun saveQueueState() {
    downloadQueue.forEach { task ->
      if (task.downloadModel.status == DownloadModel.STATUS_DOWNLOADING) {
        task.downloadModel.status = DownloadModel.STATUS_PENDING
        task.downloadModel.update(task.downloadModel.id)
      }
    }
  }

  /**
   *下载任务实体类
   */
  data class DownloadTask(
    val downloadModel: DownloadModel,
    var job: Job? = null
  )

  /**
   * 下载进度实体类
   */
  data class DownloadProgress(
    val musicId: Long,
    val downloadedSize: Long,
    val totalSize: Long,
    val progress: Int,
    val status: Int,
    val speed: Long = 0
  )
}
