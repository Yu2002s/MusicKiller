package xyz.jdynb.music.ui.activity

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.drake.net.utils.scope
import com.drake.net.utils.withIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import org.litepal.extension.findFirst
import xyz.jdynb.music.MusicKillerApplication
import xyz.jdynb.music.constants.IntentActions
import xyz.jdynb.music.constants.IntentExtras
import xyz.jdynb.music.model.FavoriteModel
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.PlayHistory
import xyz.jdynb.music.model.PlayListModel
import kotlin.system.exitProcess

class MainViewModel : ViewModel() {

  private val _bottomBarUIState = MutableStateFlow(BottomBarState())

  /**
   * 底栏状态
   */
  val bottomBarUIState: StateFlow<BottomBarState> = _bottomBarUIState.asStateFlow()

  private val _musicModel =
    MutableStateFlow(MusicModel(pic = "", name = "请选择音乐播放", artist = "MusicKiller"))

  /**
   * 当前播放音乐
   */
  val musicModel = _musicModel.asStateFlow()

  private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_ALL)

  /**
   * 当前的循环模式
   */
  val repeatMode = _repeatMode.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)

  /**
   * 是否正在播放
   */
  val isPlaying get() = _isPlaying.asStateFlow()

  private val _currentPosition = MutableStateFlow(0L)

  /**
   * 当前播放进度
   */
  val currentPosition = _currentPosition.asStateFlow()

  private val _autoCloseTime = MutableStateFlow<Long?>(null)
  val autoCloseTime = _autoCloseTime.asStateFlow()

  init {

  }

  /**
   * 修改底栏的显示状态
   *
   * @param isVisible 是否显示
   */
  fun changeBottomBarVisible(isVisible: Boolean = true) {
    _bottomBarUIState.update {
      BottomBarState(isVisible = isVisible, isExpanded = it.isExpanded)
    }
  }

  /**
   * 修改底栏展开状态
   *
   * @param isExpanded 是否展开
   */
  fun changeBottomBarExpand(isExpanded: Boolean = false) {
    _bottomBarUIState.update {
      BottomBarState(isVisible = it.isVisible, isExpanded = isExpanded)
    }
  }

  /**
   * 更新当前播放的音乐
   *
   * @param musicModel 音乐
   */
  suspend fun updateMusicModel(musicModel: MusicModel) {
    // 获取到收藏状态
    musicModel.isFavorite = isFavoriteMusic(musicModel)
    _musicModel.value = musicModel

    withIO {
      // 加入到播放历史中
      PlayHistory.from(musicModel).saveOrUpdate("musicId = ?", musicModel.id.toString())
    }
  }

  /**
   * 更新循环模式
   *
   * @param mode 模式
   * @see Player.RepeatMode
   */
  fun updateRepeatMode(@Player.RepeatMode mode: Int) {
    _repeatMode.value = mode
  }

  /**
   * 更新播放状态
   *
   * @param isPlaying 是否播放
   */
  fun updateIsPlaying(isPlaying: Boolean = !_isPlaying.value) {
    _isPlaying.value = isPlaying
  }

  /**
   * 更新播放进度
   *
   * @param position 进度(秒)
   */
  fun updateCurrentPosition(position: Long) {
    _currentPosition.value = position
  }

  /**
   * 获取音乐状态
   *
   * @param model 音乐
   */
  suspend fun getMusicModelState(model: MusicModel) {
    // 是否是当前播放的音乐
    model.isSelected = _musicModel.value.id == model.id
    // 是否收藏
    model.isFavorite = isFavoriteMusic(model)
  }

  /**
   * 获取音乐收藏状态
   *
   * @param id 音乐id
   * @param type 类型
   * @see FavoriteModel
   *
   * @return FavoriteModel 对象，为null表示没有收藏
   */
  suspend fun getMusicFavorite(id: Long, type: Int = FavoriteModel.TYPE_SONG) = withIO {
    if (id == 0L) return@withIO null
    LitePal.where(
      "type = ? and ${IntentExtras.MUSIC_ID} = ?",
      type.toString(),
      id.toString(),
    )
      .findFirst<FavoriteModel>()
  }

  /**
   * 获取音乐收藏
   *
   * @param musicModel 音乐
   */
  suspend fun getMusicFavorite(musicModel: MusicModel) = withIO {
    getMusicFavorite(musicModel.id)
  }

  /**
   * 获取歌单收藏
   */
  suspend fun getPlayListFavorite(playListModel: PlayListModel) = withIO {
    getMusicFavorite(playListModel.id, FavoriteModel.TYPE_PLAYLIST)
  }

  /**
   * 是否收藏音乐
   */
  suspend fun isFavoriteMusic(musicModel: MusicModel): Boolean {
    if (musicModel.isPlayList) {
      return isFavorite(musicModel.id, FavoriteModel.TYPE_PLAYLIST)
    }
    return getMusicFavorite(musicModel) != null
  }

  /**
   * 是否收藏
   */
  suspend fun isFavorite(id: Long, type: Int = FavoriteModel.TYPE_SONG): Boolean {
    return getMusicFavorite(id, type) != null
  }

  /**
   * 是否收藏歌单
   *
   * @param playlistModel 歌单
   * @see PlayListModel
   */
  suspend fun isFavoritePlaylist(playlistModel: PlayListModel): Boolean {
    return getPlayListFavorite(playlistModel) != null
  }

  /**
   * 添加或删除收藏
   *
   * @param model 音乐
   */
  fun addOrRemoveFavorite(model: MusicModel = musicModel.value) {
    if (model.id <= 0L) {
      return
    }
    scope(Dispatchers.IO) {
      if (!isFavoriteMusic(model)) {
        model.isFavorite = true
        FavoriteModel(model).save()
      } else {
        model.isFavorite = false
        removeFavorite(model.id, FavoriteModel.TYPE_SONG)
      }
      MusicKillerApplication.context.sendBroadcast(
        Intent(IntentActions.FAVORITE)
          .setPackage(MusicKillerApplication.context.packageName)
          .putExtra(IntentExtras.MUSIC_ID, model.id)
          .putExtra(IntentExtras.FAVORITE, model.isFavorite)
      )
    }
  }

  /**
   * 删除收藏
   *
   * @param id 音乐id
   * @param type 收藏类型
   * @see FavoriteModel
   */
  suspend fun removeFavorite(id: Long, type: Int = FavoriteModel.TYPE_SONG) = withIO {
    if (id <= 0) {
      return@withIO
    }
    LitePal.deleteAll<FavoriteModel>(
      "type = ? and ${IntentExtras.MUSIC_ID} = ?",
      type.toString(),
      id.toString()
    )
  }

  /**
   * 添加或删除收藏（歌单）
   */
  fun addOrRemoveFavorite(playListModel: PlayListModel) {
    if (playListModel.id <= 0L) {
      return
    }
    scope(Dispatchers.IO) {
      if (!isFavoritePlaylist(playListModel)) {
        FavoriteModel(playListModel).save()
      } else {
        removeFavorite(playListModel.id, FavoriteModel.TYPE_PLAYLIST)
      }
    }
  }

  private var timeCloseJob: Job? = null

  fun setAutoCloseTime(time: Long) {
    _autoCloseTime.value = if (time <= 0) null else time
    timeCloseJob?.cancel(null)
    if (time <= 0) {
      return
    }
    timeCloseJob = viewModelScope.launch {
      while (_autoCloseTime.value!! > 0) {
        delay(1000L)
        _autoCloseTime.value = _autoCloseTime.value!! - 1000L
      }
      exitProcess(0)
    }
  }

  /**
   * 底栏状态
   */
  data class BottomBarState(
    /**
     * 是否展开
     */
    val isExpanded: Boolean = false,
    /**
     * 是否显示
     */
    val isVisible: Boolean = true,
  )
}