package xyz.jdynb.music.ui.fragment.playlist

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.jdynb.music.model.Page
import xyz.jdynb.music.model.PlayListModel
import xyz.jdynb.music.model.PlayListTagModel

class PlayListViewModel: ViewModel() {

  /**
   * 每页数据
   */
  private val _playlistPageStateFlow = MutableStateFlow(mutableListOf<PlayListModel>())

  val playlistPageStateFlow = _playlistPageStateFlow.asStateFlow()

  private val _tagsStateFlow = MutableStateFlow(listOf<PlayListTagModel>())

  val tagsStateFlow = _tagsStateFlow.asStateFlow()

  private val _currentTagPositionStateFlow = MutableStateFlow(-1)

  val currentTagPositionStateFlow = _currentTagPositionStateFlow.asStateFlow()

  var pageNo = 1

  fun clearPlaylist() {
    _playlistPageStateFlow.value.clear()
  }

  fun addPlaylist(page: Page<PlayListModel>) {
    pageNo = page.page
    _playlistPageStateFlow.value.addAll(page.data)
  }

  fun updateTagsState(tags: List<PlayListTagModel>) {
    _tagsStateFlow.value = tags
  }

  fun updateTagPositionState(position: Int) {
    _currentTagPositionStateFlow.value = position
  }
}