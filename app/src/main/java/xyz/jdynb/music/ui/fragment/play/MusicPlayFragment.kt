package xyz.jdynb.music.ui.fragment.play

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import com.drake.net.Get
import com.drake.net.utils.scope
import com.drake.net.utils.scopeDialog
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.config.SPConfig
import xyz.jdynb.music.databinding.FragmentMusicPlayBinding
import xyz.jdynb.music.enums.MusicBridge
import xyz.jdynb.music.model.ArtistModel
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.ui.fragment.HomeFragmentDirections
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.SpUtils.put
import xyz.jdynb.music.utils.getMusicInfo
import xyz.jdynb.music.utils.startAnimation

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
  }

  private val handle = Handler(Looper.getMainLooper())

  /**
   * 进度是否正在运行
   */
  private var isRunningProgress = false

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

      if (mediaController.isPlaying) {
        // 循环调用更新进度
        handle.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
      } else {
        isRunningProgress = false
      }
    }
  }

  override fun openMediaController() = true

  override fun onCreateMediaController(controller: MediaController) {
    controller.removeListener(this)
    controller.addListener(this)

    lifecycleScope.launch {
      // 播放循环模式修改
      mainViewModel.repeatMode.collect {
        if (it != mediaController.repeatMode) {
          mediaController.repeatMode = it
        }
      }
    }

    lifecycleScope.launch {
      // 修改播放状态
      mainViewModel.isPlaying.collect {
        if (it != mediaController.isPlaying) {
          if (!handlePlayOrPause()) {
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
      if (musicModel.artistId == 0L) return@setOnClickListener
      scopeDialog {
        val artistInfo = Get<ArtistModel>(Api.ARTIST_INFO) {
          addQuery("artistId", musicInfo.artistId)
        }.await()
        mainViewModel.changeBottomBarExpand(false)
        navController.navigate(HomeFragmentDirections.actionArtistInfo(artistInfo))
      }
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
      handlePlayOrPause(it)
    }

    binding.btnPrev.setOnClickListener {
      // 上一首
      if (mediaController.mediaItemCount == 1 && mediaController.repeatMode == Player.REPEAT_MODE_ALL) {
        mediaController.seekToDefaultPosition()
        return@setOnClickListener
      }
      if (mediaController.hasPreviousMediaItem()) {
        mediaController.seekToPreviousMediaItem()
      } else {
        toast("没有上一首了")
      }
    }

    binding.btnNext.setOnClickListener {
      // 下一首
      if (mediaController.mediaItemCount == 1 && mediaController.repeatMode == Player.REPEAT_MODE_ALL) {
        mediaController.seekToDefaultPosition()
        return@setOnClickListener
      }
      if (mediaController.hasNextMediaItem()) {
        mediaController.seekToNextMediaItem()
      } else {
        toast("没有下一首了")
      }
    }

    binding.btnMode.setOnClickListener {
      // 修改播放模式
      val repeatMode = mediaController.repeatMode
      mainViewModel.updateRepeatMode(
        if (repeatMode == Player.REPEAT_MODE_ONE)
          Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_ONE
      )
    }

    binding.btnPlaylist.setOnClickListener {
      // 打开歌单列表
      navController.navigate(HomeFragmentDirections.actionPlayQueue())
    }

    binding.btnFavorite.setOnClickListener {
      // 收藏
      mainViewModel.addOrRemoveFavorite()
    }

    binding.btnQuality.setOnClickListener {
      // 修改音质
      val qualities = MusicBridge.entries.map { it.level }.toTypedArray()
      var currentBridge = SPConfig.CURRENT_BRIDGE.getRequired(MusicBridge.MP3_128K.level)
      val hasLossless = mainViewModel.musicModel.value.hasLossless
      if (currentBridge == MusicBridge.FLAC_2000K.level && !hasLossless) {
        currentBridge = MusicBridge.MP3_320K.level
      }
      val currentItem = qualities.indexOf(currentBridge)
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("选择音质")
        .setSingleChoiceItems(qualities, currentItem) { dialog, which ->
          SPConfig.CURRENT_BRIDGE.put(qualities[which])
          rePlay()
          dialog.dismiss()
        }.setPositiveButton("取消", null)
        .show()
    }

    binding.btnDownload.setOnClickListener {
      val qualities = MusicBridge.entries.map { it.level }.toTypedArray()
      var currentBridgeLevel = SPConfig.CURRENT_BRIDGE.getRequired(MusicBridge.MP3_128K.level)
      val hasLossless = mainViewModel.musicModel.value.hasLossless
      if (currentBridgeLevel == MusicBridge.FLAC_2000K.level && !hasLossless) {
        currentBridgeLevel = MusicBridge.MP3_320K.level
      }
      val currentItem = qualities.indexOf(currentBridgeLevel)
      var currentBridge = MusicBridge.entries[currentItem]
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("选择音质")
        .setSingleChoiceItems(qualities, currentItem) { dialog, which ->
          currentBridge = MusicBridge.entries[which]
        }
        .setPositiveButton("下载") { dialog, which ->
          val musicModel = mainViewModel.musicModel.value
          if (musicModel.id == 0L) {
            toast("请选择有效音乐下载")
            return@setPositiveButton
          }
          Log.i(TAG, "download: $_downloadService, $musicModel, $currentBridge")
          _downloadService?.addDownload(musicModel, currentBridge)
        }
        .setNegativeButton("取消", null)
        .show()
    }
  }

  private fun handlePlayOrPause(v: View? = null): Boolean {
    // 播放状态修改
    if (mediaController.currentMediaItem == null) {
      // 可能网络问题没有播放信息
      val musicModel = mainViewModel.musicModel.value
      if (musicModel.id != 0L) {
        // 尝试重新播放
        rePlay()
        return true
      }
      toast("请先选择歌曲播放")
      return false
    }
    v?.let {
      ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
    }
    if (!Util.handlePlayPauseButtonAction(mediaController)) {
      rePlay()
    }
    return true
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
        if (binding.musicSeekbar.max == 0) {
          binding.musicSeekbar.max = mediaController.duration.toInt()
        }
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
    toast(error.message)
  }

  private fun runProgress() {
    if (isRunningProgress) {
      Log.i(TAG, "isRunningProgress")
      return
    }
    isRunningProgress = true
    progressRunnable.run()
    Log.i(TAG, "runProgress")
    handle.postDelayed(progressRunnable, PROGRESS_UPDATE_INTERVAL)
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
    handle.removeCallbacks(progressRunnable)
    mediaController.removeListener(this)
  }

}