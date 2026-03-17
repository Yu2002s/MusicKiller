package xyz.jdynb.music.ui.fragment.recommend

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.jdynb.music.model.MusicRankModel
import xyz.jdynb.music.model.PlayListModel
import xyz.jdynb.music.model.PlayListTagModel
import kotlin.collections.emptyList

class RecommendViewModel : ViewModel() {

  private val _tagsStateFlow = MutableStateFlow<List<PlayListTagModel>>(emptyList())

  val tagsStateFlow = _tagsStateFlow.asStateFlow()

  private val _playlistStateFlow = MutableStateFlow(emptyList<PlayListModel>())
  val playListStateFlow = _playlistStateFlow.asStateFlow()

  private val _currentTagStateFlow = MutableStateFlow("")
  val currentTagStateFlow = _currentTagStateFlow.asStateFlow()

  private val _rankMusicStateFlow = MutableStateFlow(emptyList<MusicRankModel>())
  val rankMusicStateFlow = _rankMusicStateFlow.asStateFlow()

  fun updateTagsState(tags: List<PlayListTagModel>) {
    _tagsStateFlow.value = tags
  }

  fun updatePlayListState(playListData: List<PlayListModel>) {
    _playlistStateFlow.value = playListData
  }

  fun changeCurrentTagState(id: String): Boolean {
    if (currentTagStateFlow.value == id) {
      return false
    }
    _currentTagStateFlow.value = id
    return true
  }

  fun changeRankMusicState(rankMusics: List<MusicRankModel>) {
    _rankMusicStateFlow.value = rankMusics
  }
}