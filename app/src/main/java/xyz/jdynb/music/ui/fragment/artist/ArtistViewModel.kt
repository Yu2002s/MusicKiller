package xyz.jdynb.music.ui.fragment.artist

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.jdynb.music.model.ArtistModel
import xyz.jdynb.music.model.Page

class ArtistViewModel: ViewModel() {

  private val _uiState = MutableStateFlow(mutableListOf<ArtistModel>())

  val uiState = _uiState.asStateFlow()

  var pageNo = 1

  fun clearState() {
    _uiState.value.clear()
  }

  fun addData(page: Page<ArtistModel>) {
    pageNo = page.page
    _uiState.value.addAll(page.data)
  }

}