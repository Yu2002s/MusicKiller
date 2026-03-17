package xyz.jdynb.music.ui.fragment.search

import android.content.Context
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.divider
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.adapter.FragmentAdapter
import com.drake.engine.utils.dp
import com.drake.net.utils.scope
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.databinding.FragmentSearchMusicBinding
import xyz.jdynb.music.model.search.SearchHistory
import xyz.jdynb.music.utils.removeAllItemDecorator

/**
 * 搜索音乐
 */
class SearchMusicFragment :
  BaseMusicNavFragment<FragmentSearchMusicBinding>(R.layout.fragment_search_music), MenuProvider {

  companion object {

    private const val TAG = "SearchMusicFragment"
  }

  private val viewModel by viewModels<SearchViewModel>()

  private val fragments = listOf(
    SearchMusicListFragment(), SearchAlbumListFragment(),
    SearchPlaylistFragment(), SearchArtistListFragment()
  )

  private val inputMethodManager by lazy {
    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  }

  override fun initView() {
    setToolbar(binding.toolbar)
    requireActivity().addMenuProvider(this, viewLifecycleOwner)

    val vp = binding.vp
    vp.offscreenPageLimit = 4
    vp.isSaveEnabled = true
    vp.adapter = FragmentAdapter(fragments)

    val titles = arrayOf("单曲", "专辑", "歌单", "歌手")

    TabLayoutMediator(binding.tab.tabLayout, vp) { tab, position -> tab.text = titles[position] }
      .attach()

    binding.editKey.doAfterTextChanged { editable ->
      val keyword = editable.toString()
      val searchAction = viewModel.searchAction.value
      if (searchAction.submit && searchAction.keyword == keyword) {
        // 忽略提交状态
        return@doAfterTextChanged
      }
      viewModel.search(SearchViewModel.SearchAction(keyword = keyword))
    }

    binding.editKey.setOnEditorActionListener { v, actionId, event ->
      val keyword = binding.editKey.text.toString()
      Log.i(TAG, "onSearch: $keyword")
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        submit(keyword)
        return@setOnEditorActionListener true
      }
      false
    }

    binding.rvHistory.removeAllItemDecorator().divider {
      setDivider(10, true)
      includeVisible = true
      orientation = DividerOrientation.GRID
    }.setup {
      addType<SearchHistory>(R.layout.item_list_history)

      R.id.item_history.onClick {
        submit(getModel<SearchHistory>().name)
      }

      R.id.item_history.onLongClick {
        mutable.removeAt(modelPosition)
        notifyItemRemoved(modelPosition)
        scope(Dispatchers.IO) {
          getModel<SearchHistory>().delete()
        }
      }
    }

    binding.rvSuggest.setup {
      addType<String>(R.layout.item_list_suggest)

      R.id.item_suggest.onClick { submit(getModel()) }
    }

    binding.rvKey.setup {
      addType<String>(R.layout.item_list_suggest)

      R.id.item_suggest.onClick { submit(getModel()) }
    }
  }

  /** 提交搜索 */
  private fun submit(keyword: String) {
    Log.i(TAG, "submit: $keyword")
    viewModel.search(SearchViewModel.SearchAction(keyword = keyword, true))
  }

  private fun currentSearchFragment() = fragments[binding.vp.currentItem]

  override fun initData() {
    binding.m = viewModel
    binding.lifecycleOwner = this
    lifecycleScope.launch {
      viewModel.uiState.collect { state ->
        Log.i(TAG, "uiState: $state")
        when (state) {
          is SearchViewModel.UiState.Suggest -> {
            // 建议
            binding.rvSuggest.models = state.suggestList
          }

          is SearchViewModel.UiState.Nothing -> {
            // 历史
            binding.rvHistory.models = state.histories
            binding.rvKey.models = state.suggestList
          }

          is SearchViewModel.UiState.Result -> {
            // 由 Fragment 自行监听 ViewModel 变化
            binding.editKey.post {
              binding.editKey.setSelection(binding.editKey.text.length)
            }
            // binding.editKey.clearFocus()
            inputMethodManager.hideSoftInputFromWindow(
              binding.editKey.windowToken,
              InputMethodManager.HIDE_NOT_ALWAYS
            )
          }
        }
      }
    }
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    val searchItem = menu.add(0, 0, 0, "清除")
    searchItem.setIcon(R.drawable.baseline_close_24)
    searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == 0) {
      viewModel.clearKeyword()
      return true
    }
    return false
  }
}
