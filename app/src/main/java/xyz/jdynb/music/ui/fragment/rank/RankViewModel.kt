package xyz.jdynb.music.ui.fragment.rank

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.jdynb.music.model.MusicRankMenuModel
import kotlin.collections.emptyList

class RankViewModel: ViewModel() {

  private val _uiState = MutableStateFlow(emptyList<MusicRankMenuModel>())

  val uiState = _uiState.asStateFlow()

  fun updateState(list: List<MusicRankMenuModel>) {
    _uiState.value = list
  }
}