package xyz.jdynb.music.ui.activity

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.drake.net.utils.scope
import com.drake.net.utils.withIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import org.litepal.extension.find
import org.litepal.extension.findAll
import org.litepal.extension.findFirst
import xyz.jdynb.music.MusicKillerApplication
import xyz.jdynb.music.constants.IntentActions
import xyz.jdynb.music.constants.IntentExtras
import xyz.jdynb.music.model.FavoriteModel
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.PlayHistory
import xyz.jdynb.music.model.PlayListModel
import kotlin.math.min

class MainViewModel : ViewModel() {

  private val _bottomBarUIState = MutableStateFlow(BottomBarState())
  val bottomBarUIState: StateFlow<BottomBarState> = _bottomBarUIState.asStateFlow()

  private val _musicModel =
    MutableStateFlow(MusicModel(pic = "", name = "请选择音乐播放", artist = "MusicKiller"))
  val musicModel = _musicModel.asStateFlow()

  private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_ALL)
  val repeatMode = _repeatMode.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying get() = _isPlaying.asStateFlow()

  private val _currentPosition = MutableStateFlow(0L)
  val currentPosition = _currentPosition.asStateFlow()

  init {
    // 加载最近播放的歌曲
    /*scope {
      withIO {
        val histories = LitePal.order("updateTime desc").limit(20).find<PlayHistory>()
        if (histories.isNotEmpty()) {
          val size = min(20, histories.size)
          val playList = histories.subList(0, size)
        }
      }
    }*/
  }

  fun changeBottomBarVisible(isVisible: Boolean = true) {
    _bottomBarUIState.update {
      BottomBarState(isVisible = isVisible, isExpanded = it.isExpanded)
    }
  }

  fun changeBottomBarExpand(isExpanded: Boolean = false) {
    _bottomBarUIState.update {
      BottomBarState(isVisible = it.isVisible, isExpanded = isExpanded)
    }
  }

  suspend fun updateMusicModel(musicModel: MusicModel) {
    musicModel.isFavorite = isFavoriteMusic(musicModel)
    _musicModel.value = musicModel

    withIO {
      // 加入到播放历史中
      PlayHistory.from(musicModel).saveOrUpdate("musicId = ?", musicModel.id.toString())
    }
  }

  fun updateRepeatMode(@Player.RepeatMode mode: Int) {
    _repeatMode.value = mode
  }

  fun updateIsPlaying(isPlaying: Boolean = !_isPlaying.value) {
    _isPlaying.value = isPlaying
  }

  fun updateCurrentPosition(position: Long) {
    _currentPosition.value = position
  }

  suspend fun getMusicModelState(model: MusicModel) {
    model.isSelected = _musicModel.value.id == model.id
    model.isFavorite = isFavoriteMusic(model)
  }

  suspend fun getMusicFavorite(id: Long, type: Int = FavoriteModel.TYPE_SONG) = withIO {
    if (id == 0L) return@withIO null
    LitePal.where(
      "type = ? and ${IntentExtras.MUSIC_ID} = ?",
      type.toString(),
      id.toString(),
    )
      .findFirst<FavoriteModel>()
  }

  suspend fun getMusicFavorite(musicModel: MusicModel) = withIO {
    getMusicFavorite(musicModel.id)
  }

  suspend fun getPlayListFavorite(playListModel: PlayListModel) = withIO {
    getMusicFavorite(playListModel.id, FavoriteModel.TYPE_PLAYLIST)
  }

  suspend fun isFavoriteMusic(musicModel: MusicModel): Boolean {
    if (musicModel.isPlayList) {
      return isFavorite(musicModel.id, FavoriteModel.TYPE_PLAYLIST)
    }
    return getMusicFavorite(musicModel) != null
  }

  suspend fun isFavorite(id: Long, type: Int = FavoriteModel.TYPE_SONG): Boolean {
    return getMusicFavorite(id, type) != null
  }

  suspend fun isFavoritePlaylist(playlistModel: PlayListModel): Boolean {
    return getPlayListFavorite(playlistModel) != null
  }

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

  data class BottomBarState(
    val isExpanded: Boolean = false,
    val isVisible: Boolean = true,
  )
}