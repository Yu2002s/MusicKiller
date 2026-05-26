package xyz.jdynb.music.ui.fragment.play

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.drake.net.utils.scope
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import org.litepal.LitePal
import org.litepal.extension.find
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.databinding.FragmentMusicPlayBinding
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.PlayHistory
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.getMusicInfo
import xyz.jdynb.music.utils.startAnimation
import kotlin.math.min

/**
 * 播放音乐页面
 */
class MusicPlayFragment :
  BaseMusicNavFragment<FragmentMusicPlayBinding>(R.layout.fragment_music_play),
  Player.Listener {

  companion object {

    private const val TAG = "MusicPlayFragment"

    /**
     * 播放状态 TAG
     */
    private const val TAG_PLAY = 1

    /**
     * 暂停状态 TAG
     */
    private const val TAG_PAUSE = 2

    /**
     * 进度更新间隔
     */
    private const val PROGRESS_UPDATE_INTERVAL = 200L

    /**
     * 默认最大的播放数
     */
    const val DEFAULT_MAX_PLAY_COUNT = 20
  }

  private val handler = Handler(Looper.getMainLooper())

  /**
   * 进度是否正在运行
   */
  private var isRunningProgress = false

  /**
   * 用户是否手动修改进度
   */
  private var isUserTrackProgress = false

  /**
   * 音乐信息
   */
  private val musicInfo get() = binding.m!!

  /**
   * 用于进度更新
   */
  private val progressRunnable = object : Runnable {
    override fun run() {
      if (!isRunningProgress) {
        // 已停止进度
        return
      }
      val currentPosition = mediaController.currentPosition
      // 更新进度
      musicInfo.currentPosition = currentPosition
      mainViewModel.updateCurrentPosition(currentPosition)
      // 修正进度条最大值

      if (mediaController.isPlaying) {
        // 循环调用更新进度
        handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
      } else {
        isRunningProgress = false
      }
    }
  }

  override fun onCreateMediaController(controller: MediaController) {
    controller.removeListener(this)
    controller.addListener(this)

    lifecycleScope.launch {
      // 修改播放状态
      mainViewModel.isPlaying.collect {
        if (it != mediaController.isPlaying) {
          if (!playOrPause()) {
            mainViewModel.updateIsPlaying(false)
          }
        }
      }
    }

    lifecycleScope.launch {
      // 更新音乐进度
      mainViewModel.currentPosition.collect {
        if (it != musicInfo.currentPosition) {
          mediaController.seekTo(it)
        }
      }
    }

    if (getString(R.string.open_auto_play).getRequired<Boolean>(false)) {
      // 加载最近播放的歌曲
      scope {
        val playList = withIO {
          val defaultMaxPlayCount = resources.getInteger(R.integer.default_max_play_count)
          val histories = LitePal.order("updateTime desc").limit(defaultMaxPlayCount).find<PlayHistory>()
          if (histories.isEmpty()) return@withIO emptyList()
          val size = min(getString(R.string.max_play_count).getRequired<Int>(defaultMaxPlayCount), histories.size)
          histories.subList(0, size).map { it.toMusicModel() }
        }
        if (playList.isNotEmpty()) {
          mainViewModel.updateMusicModel(playList.first())
          addPlaylist(playList)
        }
      }
    }
  }

  override fun initData() {
    lifecycleScope.launch {
      // 监听歌曲信息
      mainViewModel.musicModel.collect { musicInfo ->
        binding.m = musicInfo
      }
    }
  }

  override fun initView() {
    binding.vm = mainViewModel
    binding.lifecycleOwner = this

    binding.musicArtist.setOnClickListener {
      openArtistInfo()
    }

    // 更新播放进度
    binding.musicSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        // 进度更新
        if (fromUser) {
          // 如果是用户自己拖动的
          musicInfo.currentPosition = progress.toLong()
        }
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // 用户开始拖动时
        isUserTrackProgress = true
        isRunningProgress = false
        Log.i(TAG, "onStartTrackingTouch")
      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {
        Log.i(TAG, "onStopTrackingTouch")
        isUserTrackProgress = false
        // 用户取消拖动时
        mediaController.seekTo(seekBar?.progress?.toLong() ?: 0L)
      }
    })

    binding.btnPlay.setOnClickListener {
      // 播放/暂停
      playOrPause(it)
    }

    binding.btnPrev.setOnClickListener {
      // 上一首
      prevMusic()
    }

    binding.btnNext.setOnClickListener {
      // 下一首
      nextMusic()
    }

    binding.btnMode.setOnClickListener {
      // 修改播放模式
      switchRepeatMode()
    }

    binding.btnPlaylist.setOnClickListener {
      // 打开歌单列表
      openPlaylist()
    }

    binding.btnFavorite.setOnClickListener {
      // 收藏
      addOrRemoveFavorite()
    }

    binding.btnQuality.setOnClickListener {
      // 音质
      setMusicBridge()
    }

    binding.btnDownload.setOnClickListener {
      // 下载
      addDownload()
    }
  }

  override fun onMusicFavoriteChanged(musicId: Long, isFavorite: Boolean) {
    Log.i(TAG, "onMusicFavoriteChanged: $musicId, isFavorite: $isFavorite")
    super.onMusicFavoriteChanged(musicId, isFavorite)
  }

  /**
   * 当 MetaData 修改时回调
   */
  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
    Log.i(TAG, "onMediaMetadataChanged")
  }

  /**
   * 当播放媒体切换时调用，或开始重复播放某个媒体项目时调用
   */
  override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    Log.i(TAG, "onMediaItemTransition")
    binding.musicSeekbar.max = mediaItem?.mediaMetadata?.extras?.getInt("duration") ?: 0
    // Log.i(TAG, "metaData: ${mediaController.duration}") // C.TIME_UNSET 媒体未准备返回这个
    binding.musicSeekbar.setProgress(0, true)
    binding.m =
      mediaItem?.mediaMetadata?.getMusicInfo() ?: MusicModel(name = getString(R.string.app_name))
    scope {
      mainViewModel.updateMusicModel(binding.m!!)
    }
  }

  /**
   * 随机模式启用或关闭时调用
   */
  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
    super.onShuffleModeEnabledChanged(shuffleModeEnabled)
  }

  /**
   * 循环模式改变时调用
   */
  override fun onRepeatModeChanged(repeatMode: Int) {
    mainViewModel.updateRepeatMode(repeatMode)
  }

  /**
   * 是否播放状态改变
   *
   * 当isPlaying（）的值更改时调用。
   *
   * 在缓冲中时，这里的 isPlaying 为 false 触发
   */
  override fun onIsPlayingChanged(isPlaying: Boolean) {
    Log.i(TAG, "onIsPlayingChanged: $isPlaying")
    mainViewModel.updateIsPlaying(isPlaying)
    updatePlayBtn(isPlaying)
  }

  /**
   * 播放状态改变
   */
  override fun onPlaybackStateChanged(playbackState: Int) {
    if (mediaController.isPlaying) {
      // 正在播放中
    } else if (playbackState != Player.STATE_BUFFERING) {
      // 未播放的状态，缓存时状态是停止的，所以需要排除
    }
    when (playbackState) {
      Player.STATE_IDLE -> {
        // 空闲状态
        Log.i(TAG, "onPlaybackStateChanged: 空闲状态")
      }

      Player.STATE_READY -> {
        // 准备播放
        Log.i(TAG, "onPlaybackStateChanged: 已准备, duration: ${mediaController.duration}")
        binding.musicSeekbar.max = mediaController.duration.toInt()
      }

      Player.STATE_BUFFERING -> {
        // 缓存中
        Log.i(TAG, "onPlaybackStateChanged: 缓存中")
      }

      Player.STATE_ENDED -> {
        Log.i(TAG, "onPlaybackStateChanged: 已结束")
      }
    }
  }

  /**
   * 播放错误回调
   */
  override fun onPlayerError(error: PlaybackException) {
    toast("播放失败，请稍后重试：" + error.message)
  }

  private fun runProgress() {
    if (isRunningProgress) {
      Log.i(TAG, "isRunningProgress")
      return
    }
    isRunningProgress = true
    progressRunnable.run()
    Log.i(TAG, "runProgress")
    handler.postDelayed(progressRunnable, PROGRESS_UPDATE_INTERVAL)
  }

  /**
   * 更新播放按钮
   */
  private fun updatePlayBtn(isPlaying: Boolean) {
    if (mediaController.playbackState == Player.STATE_BUFFERING) {
      // 正在缓冲时
      isRunningProgress = false
      return
    }
    binding.btnPlay.apply {
      if (isPlaying) {
        // 设置为播放的状态
        if (tag != TAG_PLAY) {
          tag = TAG_PLAY
          icon = AppCompatResources.getDrawable(requireContext(), R.drawable.play_anim)
          background = AppCompatResources.getDrawable(requireContext(), R.drawable.bg_play_anim)
        }
        runProgress()
      } else {
        // 设置为暂停的状态
        if (tag != TAG_PAUSE) {
          tag = TAG_PAUSE
          icon = AppCompatResources.getDrawable(requireContext(), R.drawable.pause_anim)
          background = AppCompatResources.getDrawable(requireContext(), R.drawable.bg_pause_anim)
        }
        isRunningProgress = false
      }
      // 开始动画
      icon.startAnimation()
      background.startAnimation()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.removeCallbacks(progressRunnable)
    mediaController.removeListener(this)
  }

}