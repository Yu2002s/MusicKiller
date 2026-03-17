package xyz.jdynb.music.ui.fragment.search

import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.net.Get
import com.drake.net.utils.scope
import xyz.jdynb.music.R
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentSearchPlaylistBinding
import xyz.jdynb.music.model.Page
import xyz.jdynb.music.model.PlayListModel
import xyz.jdynb.music.utils.removeAllItemDecorator

class SearchPlaylistFragment: BaseSearchListFragment<FragmentSearchPlaylistBinding>(R.layout.fragment_search_playlist) {

  override fun onSearch() {
    binding.page.showLoading()
  }

  override fun openMediaController(): Boolean {
    return false
  }

  override fun initView() {
    binding.page.onRefresh {
      scope {
        val result = Get<Page<PlayListModel>>(Api.SEARCH_PLAYLIST) {
          addQuery("keyword", keyword)
          addQuery("pageNo", index)
          addQuery("pageSize", 20)
        }.await()

        addData(result.data) {
          result.total > modelCount
        }
        result.page = index
        addPage(result)
      }
    }

    binding.rvPlaylist.removeAllItemDecorator()
      .divider {
        setDivider(16, true)
        includeVisible = true
        orientation = DividerOrientation.GRID
      }.setup {
      addType<PlayListModel>(R.layout.item_playlist)

      R.id.item_playlist.onClick {
        navController.navigate(SearchMusicFragmentDirections.actionPlaylistInfo(getModel()))
      }
    }

    if (mData.isNotEmpty()) {
      binding.page.index = pageNo
      binding.rvPlaylist.models = mData.toMutableList()
    }
  }


}