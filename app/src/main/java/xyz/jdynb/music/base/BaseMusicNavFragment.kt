package xyz.jdynb.music.base

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.BindingAdapter
import com.drake.brv.utils.bindingAdapter
import com.drake.net.Get
import com.drake.net.scope.AndroidScope
import com.drake.net.utils.scope
import com.drake.tooltip.toast
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.config.SPConfig
import xyz.jdynb.music.constants.IntentActions
import xyz.jdynb.music.constants.IntentExtras
import xyz.jdynb.music.enums.MusicBridge
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.PlayInfo
import xyz.jdynb.music.model.download.DownloadModel
import xyz.jdynb.music.model.setupMusicRv
import xyz.jdynb.music.service.MusicService
import xyz.jdynb.music.ui.activity.MainActivity
import xyz.jdynb.music.ui.activity.MainViewModel
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.toBundle

/**
 * 导航基类（不包含Appbar）
 */
abstract class BaseMusicNavFragment<V : ViewDataBinding>(@LayoutRes contentLayoutId: Int = 0) :
  Fragment(contentLayoutId), MenuProvider {

  companion object {
    private const val KEY_FIRST_LOAD = "first_load"
  }

  open val binding get() = DataBindingUtil.bind<V>(requireView())!!

  protected var isFirstLoad = true
    private set

  protected var isFirstResume = true
    private set

  val activity get() = requireActivity() as MainActivity

  var _mediaController: MediaController? = null
  val mediaController get() = _mediaController!!

  val _downloadService get() = activity._downloadService

  val downloadService get() = _downloadService!!

  val mainViewModel by activityViewModels<MainViewModel>()

  protected val musicModel get() = mainViewModel.musicModel.value

  private var playJob: AndroidScope? = null

  protected lateinit var navController: NavController

  private lateinit var musicBroadReceiver: MusicBroadReceiver

  /**
   * 页码
   */
  var pageNo = 1

  var musicList: MutableList<Any>? = null

  /**
   * 是否开启多选模式
   */
  var enableMultiMode: Boolean = false

  /**
   * 获取音乐RecyclerView的BindingAdapter
   */
  private val bindingAdapter get() = getMusicRecyclerView()?.adapter as? BindingAdapter

  abstract fun initView()

  abstract fun initData()

  /**
   * 第一次页面加载时调用
   */
  protected open fun onFirstLoad() {
    isFirstLoad = false
  }

  protected open fun onFirstResume() {
    isFirstResume = false
  }

  /**
   * 是否开启 MediaController
   *
   * @return true 开启，false 不开启
   */
  protected open fun openMediaController(): Boolean {
    return false
  }

  /**
   * 创建 MediaController 时回调
   */
  protected open fun onCreateMediaController(controller: MediaController) {
  }

  @Suppress("UNCHECKED_CAST")
  private fun eachMusicModels(block: (MusicModel) -> Boolean) {
    val model = musicList?.getOrNull(0)
    val models = if (model is MusicModel) {
      musicList as List<MusicModel>
    } else getMusicModels() as? List<MusicModel>
    models?.forEach {
      if (block.invoke(it)) {
        return@forEach
      }
    }
  }

  /**
   * 播放的MusicModel改变时回调
   */
  protected open fun onMusicModelChanged(musicModel: MusicModel) {
    eachMusicModels {
      if (it.id == musicModel.id) {
        it.isSelected = true
      } else if (it.isSelected) {
        it.isSelected = false
        return@eachMusicModels true
      }
      return@eachMusicModels false
    }
  }

  /**
   * 监听音乐收藏状态改变时回调
   */
  protected open fun onMusicFavoriteChanged(musicId: Long, isFavorite: Boolean) {
    if (musicId == -1L) return
    eachMusicModels {
      if (it.id == musicId) {
        it.isFavorite = isFavorite
        return@eachMusicModels true
      }
      return@eachMusicModels false
    }
  }

  /**
   * 获取音乐实体集合，返回此集合将自动处理选中当前播放的音乐
   *
   * 版本迭代重复代码，现保留
   */
  protected open fun getMusicModels(): List<Any?>? {
    return getMusicRecyclerView()?.bindingAdapter?.models
  }

  /**
   * 获取音乐RecyclerView
   *
   * @return 如果不返回 null，则表示将代理此RecyclerView 创建默认的音乐视图
   */
  protected open fun getMusicRecyclerView(): RecyclerView? {
    return null
  }

  protected open fun onMusicRecyclerViewSetup(onMusicItemClick: (BindingAdapter.(Int, MusicModel) -> Unit)? = null) {

    var onCheckChange: (BindingAdapter.(Int, MusicModel, Boolean, Boolean) -> Unit)? = null

    if (enableMultiMode) {
      onCheckChange = { positon, musicModel, checked, allChecked ->
        // 选中改变事件
      }
    }

    getMusicRecyclerView()?.let { rv ->
      rv.setupMusicRv(
        this,
        onItemClick = onMusicItemClick,
        onCheckChange = onCheckChange,
        onToggleChange = { position, toggleMode, end ->
          requireActivity().invalidateMenu()
        })
    }
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    if (!enableMultiMode) {
      return
    }
    menuInflater.inflate(R.menu.menu_playlist, menu)
  }

  override fun onPrepareMenu(menu: Menu) {
    if (!enableMultiMode) {
      return
    }
    menu.findItem(R.id.download)?.isVisible = bindingAdapter?.toggleMode ?: false
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    return when (menuItem.itemId) {
      R.id.select -> {
        bindingAdapter?.toggle()
        true
      }

      R.id.download -> {
        val checkedModels = getMusicModels()?.filter {
          it as MusicModel
          it.checked
        }
        if (!checkedModels.isNullOrEmpty()) {
          val qualities = MusicBridge.entries.map { it.level }.toTypedArray()
          var currentBridge = MusicBridge.FLAC_2000K
          MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择音质")
            .setSingleChoiceItems(qualities, qualities.lastIndex) { dialog, which ->
              currentBridge = MusicBridge.entries[which]
            }
            .setPositiveButton("下载(${checkedModels.size})") { dialog, which ->
              checkedModels.forEach { model ->
                _downloadService?.addDownload(model as MusicModel, currentBridge)
                bindingAdapter?.toggle(false)
              }
            }
            .setNegativeButton("取消", null)
            .show()
        }
        true
      }

      else -> false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    navController = findNavController()
    /*savedInstanceState?.let {
      isFirstLoad = it.getBoolean(KEY_FIRST_LOAD, true)
    }*/

    if (openMediaController()) {
      val sessionToken = SessionToken(
        requireContext(),
        ComponentName(requireContext(), MusicService::class.java)
      )
      val controllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()
      controllerFuture.addListener(
        {
          _mediaController = controllerFuture.get()
          onCreateMediaController(mediaController)
        },
        MoreExecutors.directExecutor()
      )
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    onMusicRecyclerViewSetup()

    initView()

    initData()

    if (enableMultiMode) {
      requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    if (isFirstLoad) {
      onFirstLoad()
    }

    if (openMediaController()) {
      viewLifecycleOwner.lifecycleScope.launch {
        mainViewModel.musicModel.collect {
          this@BaseMusicNavFragment.onMusicModelChanged(it)
        }
      }

      val intentFilter = IntentFilter(IntentActions.FAVORITE)
      musicBroadReceiver = MusicBroadReceiver()
      ContextCompat.registerReceiver(
        requireContext(),
        musicBroadReceiver,
        intentFilter,
        ContextCompat.RECEIVER_NOT_EXPORTED
      )
    }
  }

  override fun onResume() {
    super.onResume()
    if (isFirstResume) {
      onFirstResume()
      return
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    // outState.putBoolean(KEY_FIRST_LOAD, isFirstLoad)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    if (::musicBroadReceiver.isInitialized) {
      requireContext().unregisterReceiver(musicBroadReceiver)
    }
  }

  /**
   * 设置 AppBar
   */
  fun setAppbar(toolbar: Toolbar, toolbarLayout: CollapsingToolbarLayout) {
    (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
    toolbarLayout.setupWithNavController(toolbar, navController)
  }

  fun setToolbar(toolbar: Toolbar) {
    (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
    toolbar.setupWithNavController(navController)
  }

  /**
   * 取消多选
   */
  fun closeMultiMode() {
    if (!enableMultiMode) {
      return
    }
    if (view == null) {
      return
    }
    bindingAdapter?.toggle(false)
  }

  private suspend fun getPlayInfo(musicModel: MusicModel) = coroutineScope {
    // 播放任务前先取消
    var currentBridge = SPConfig.CURRENT_BRIDGE.getRequired(MusicBridge.MP3_128K.level)
    if (currentBridge == MusicBridge.FLAC_2000K.level && !musicModel.hasLossless) {
      // 如果当前选择的是无损音质，但是音频不支持无损，强制使用下一等级音质
      currentBridge = MusicBridge.MP3_320K.level
    }
    Get<PlayInfo>(Api.PLAY_INFO) {
      addQuery("id", musicModel.id) // 歌曲 id
      addQuery("bridge", currentBridge) // 音质
    }.await()
  }

  /**
   * 构建播放地址
   */
  private fun MediaItem.Builder.createPlayUrl(musicModel: MusicModel): MediaItem.Builder {
    if (musicModel.localPath.isNotEmpty()) {
      return setUri("file://" + musicModel.localPath)
    }
    // 播放任务前先取消
    var currentBridge = SPConfig.CURRENT_BRIDGE.getRequired(MusicBridge.MP3_128K.level)
    if (currentBridge == MusicBridge.FLAC_2000K.level && !musicModel.hasLossless) {
      // 如果当前选择的是无损音质，但是音频不支持无损，强制使用下一等级音质
      currentBridge = MusicBridge.MP3_320K.level
    }
    return setUri("http://localhost?id=${musicModel.id}&bridge=$currentBridge")
  }

  /**
   * 构建 MedialItem 对象
   */
  private fun createMediaItem(musicModel: MusicModel): MediaItem {
    val pic = musicModel.pic120.ifEmpty { musicModel.pic }
    return MediaItem.Builder()
      .createPlayUrl(musicModel)
      .setMediaId(musicModel.id.toString())
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setArtworkUri(pic.toUri())
          .setExtras(musicModel.toBundle())
          .build()
      )
      .build()
  }

  /**
   * 重新播放当前音频
   */
  protected fun rePlay() {
    _mediaController ?: return
    val musicModel = mainViewModel.musicModel.value
    if (musicModel.id == 0L) {
      // 判断当前没有添加音播播放
      return
    }

    playJob?.cancel()
    playJob = scope {
      // 当前媒体的索引位置
      val currentMediaItemIndex = mediaController.currentMediaItemIndex
      // 如果当前媒体没有索引，视为还未播放
      if (currentMediaItemIndex == C.INDEX_UNSET) {
        // 当前没有播放，添加到播放队列中
        addPlay(musicModel)
        return@scope
      }
      // 获取到当前的媒体音频
      val mediaItem = mediaController.currentMediaItem ?: return@scope
      // 先停止播放
      mediaController.stop()
      // 重新获取到播放信息
      // 设置新的播放地址
      val newMediaItem = mediaItem.buildUpon().createPlayUrl(musicModel).build()
      // 替换新的播放媒体
      mediaController.replaceMediaItem(currentMediaItemIndex, newMediaItem)
      // 开始进行播放
      Util.handlePlayButtonAction(mediaController)
    }
  }

  protected fun replacePlay(musicModel: MusicModel) {
    _mediaController ?: return
    if (musicModel.id == 0L) {
      return
    }
    playJob?.cancel()
    playJob = scope {
      mainViewModel.updateMusicModel(musicModel)
      val mediaItem = createMediaItem(musicModel)
      mediaController.replaceMediaItem(mediaController.currentMediaItemIndex, mediaItem)
      Util.handlePlayButtonAction(mediaController)
    }
  }

  protected fun addDownloadPlay(
    downloadModel: DownloadModel,
    immediate: Boolean = true,
    onError: ((Throwable) -> Unit)? = null
  ) {
    addPlay(
      MusicModel(
        pic = downloadModel.cover,
        name = downloadModel.name,
        id = downloadModel.musicId,
        artist = downloadModel.artist,
        localPath = downloadModel.localPath
      ), immediate, onError
    )
  }

  /**
   * 添加到播放队列
   *
   * @param musicModel 音乐对象
   * @param immediate 是否切换到播放
   * @param onError 错误回调
   */
  fun addPlay(
    musicModel: MusicModel,
    immediate: Boolean = true,
    onError: ((Throwable) -> Unit)? = null
  ) {
    _mediaController ?: return

    for (i in 0 until mediaController.mediaItemCount) {
      if (mediaController.getMediaItemAt(i).mediaId == musicModel.id.toString()) {
        if (immediate) {
          mediaController.seekTo(i, 0)
          Util.handlePlayButtonAction(mediaController)
        } else {
          // 移动到最后
          mediaController.moveMediaItem(i, mediaController.mediaItemCount - 1)
        }
        return
      }
    }

    if (immediate) {
      // 先更新 UI
      // mainViewModel.updateMusicModel(musicModel)
      // 更新到默认的进度
      // mediaController.seekToDefaultPosition()
      if (mediaController.isPlaying) {
        // 立即停止
        mediaController.stop()
      }
    } else if (mediaController.mediaItemCount == 0) {
      // mainViewModel.updateMusicModel(musicModel)
    }

    playJob?.cancel()
    playJob = scope {

      val mediaItem = createMediaItem(musicModel)
      // val wasEmpty = mediaController.mediaItemCount == 0
      mediaController.addMediaItem(mediaItem)

      if (immediate) {
        val newIndex = mediaController.mediaItemCount - 1
        mediaController.seekTo(newIndex, 0)
        Util.handlePlayButtonAction(mediaController)
      }
    }.catch { error ->
      Log.e("Base", error.toString())
      onError?.invoke(error) ?: toast(error.message)
    }
  }

  /**
   * 添加播放列表到播放队列中
   *
   * @param list 播放列表
   * @param clear 是否清空当前的播放队列
   */
  fun addPlaylist(list: List<Any?>, clear: Boolean = true) {
    _mediaController ?: return
    if (list.isEmpty()) return

    val mediaItems = list.map { createMediaItem(it as MusicModel) }
    if (clear) {
      mediaController.setMediaItems(mediaItems)
      if (mediaController.isPlaying) {
        mediaController.stop()
      }
      Util.handlePlayButtonAction(mediaController)
    } else {
      mediaController.addMediaItems(mediaItems)
    }
  }

  inner class MusicBroadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        IntentActions.FAVORITE -> {
          val musicId = intent.getLongExtra(IntentExtras.MUSIC_ID, -1L)
          val isFavorite = intent.getBooleanExtra(IntentExtras.FAVORITE, false)
          if (musicId == musicModel.id) {
            musicModel.isFavorite = isFavorite
          }
          onMusicFavoriteChanged(musicId, isFavorite)
        }
      }
    }
  }
}