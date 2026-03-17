package xyz.jdynb.music.ui.fragment.search

import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.model.Page

abstract class BaseSearchListFragment<V : ViewDataBinding>(@LayoutRes contentLayoutId: Int = 0) :
  BaseMusicNavFragment<V>(contentLayoutId) {

  private val viewModel by viewModels<SearchViewModel>({ requireParentFragment() })

  private var _keyword = ""

  protected val keyword get() = viewModel.keyword

  protected val mData = mutableListOf<Any>()

  override fun openMediaController(): Boolean {
    return true
  }

  fun <T> addPage(page: Page<T>) {
    pageNo = page.page
    if (pageNo == 1) {
      mData.clear()
    }
    @Suppress("UNCHECKED_CAST")
    mData.addAll(page.data as List<Any>)
  }

  abstract fun onSearch()

  override fun initData() {
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.uiState.collect { state ->
          if (state is SearchViewModel.UiState.Result) {
            if (_keyword != keyword) {
              _keyword = keyword
              mData.clear()
              pageNo = 1
              onSearch()
            }
          }
        }
      }
    }
  }

}