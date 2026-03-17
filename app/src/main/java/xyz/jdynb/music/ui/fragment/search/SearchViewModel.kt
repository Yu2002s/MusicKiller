package xyz.jdynb.music.ui.fragment.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drake.net.Get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.find
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.model.search.SearchHistory

class SearchViewModel : ViewModel() {
  private var _uiState = MutableStateFlow(SearchAction())

  val searchAction = _uiState.asStateFlow()

  /**
   * 搜索关键字
   */
  val keyword get() = searchAction.value.keyword

  @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
  val uiState: StateFlow<UiState> = _uiState
    .debounce { searchAction ->
      // 提交或者搜索关键字为空时，不开启防抖
      if (searchAction.submit || searchAction.keyword.isBlank()) 0 else 500
    }
    .distinctUntilChanged()
    .flatMapLatest { searchAction ->
      flow {
        val keyword = searchAction.keyword.trim()
        if (keyword.isEmpty()) {
          val histories = withContext(Dispatchers.IO) {
            LitePal.order("updateAt desc").limit(100).find<SearchHistory>()
          }
          val suggestList = getSearchSuggestList()
          // 历史
          emit(UiState.Nothing(histories = histories, suggestList = suggestList))
        } else if (searchAction.submit) {
          // 结果
          emit(UiState.Result)
          withContext(Dispatchers.IO) {
            SearchHistory(name = keyword).saveOrUpdate("name = ?", keyword)
          }
        } else {
          // 建议
          val result = getSearchSuggestList(keyword)
          emit(UiState.Suggest(result))
        }
      }
    }.stateIn(viewModelScope, SharingStarted.Lazily, UiState.Nothing())

  @OptIn(ExperimentalCoroutinesApi::class)
  val hasHistory: StateFlow<Boolean> = uiState
    .flatMapConcat {
      flowOf(if (it is UiState.Nothing) {
        it.histories.isNotEmpty()
      } else true)
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

  private suspend fun getSearchSuggestList(keyword: String = "") = coroutineScope {
    val result = Get<List<String>>(Api.SEARCH_KEYWORD) {
      addQuery("key", keyword)
    }.await()
    if (!keyword.isEmpty()) {
      result.map {
        val regex = "RELWORD=(.*?)\\r\\n".toRegex()
        regex.find(it)?.destructured?.component1() ?: "错误"
      }
    } else {
      result
    }
  }

  /**
   * 搜索
   *
   * @param searchAction 搜索操作
   */
  fun search(searchAction: SearchAction) {
    _uiState.value = searchAction
  }

  fun keyword(keyword: String) {
    _uiState.value.keyword = keyword
  }

  fun clearKeyword() {
    _uiState.value = SearchAction()
  }

  /**
   * 搜索操作
   */
  data class SearchAction(
    /**
     * 关键字
     */
    var keyword: String = "",
    /**
     * 是否提交
     */
    val submit: Boolean = false,
  )

  /**
   * UI 状态
   */
  sealed class UiState {

    /**
     * 搜索建议
     *
     * @param histories 历史集合
     * @param suggestList 建议集合
     */
    data class Nothing(
      val histories: List<SearchHistory> = emptyList(),
      val suggestList: List<String> = emptyList()
    ) : UiState()

    /**
     * 搜索建议
     *
     * @param suggestList 建议集合
     */
    data class Suggest(val suggestList: List<String> = emptyList()) : UiState()

    /**
     * 搜索结果
     */
    object Result : UiState()

  }
}