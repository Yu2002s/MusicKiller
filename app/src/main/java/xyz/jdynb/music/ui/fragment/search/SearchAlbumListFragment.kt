package xyz.jdynb.music.ui.fragment.search

import android.text.Html
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.net.Get
import com.drake.net.utils.scope
import com.drake.tooltip.toast
import xyz.jdynb.music.R
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentSearchAlbumBinding
import xyz.jdynb.music.model.AlbumModel
import xyz.jdynb.music.model.Page
import xyz.jdynb.music.utils.removeAllItemDecorator

class SearchAlbumListFragment: BaseSearchListFragment<FragmentSearchAlbumBinding>(R.layout.fragment_search_album) {

  override fun onSearch() {
    binding.rvAlbum.scrollToPosition(0)
    binding.page.showLoading()
  }

  override fun openMediaController(): Boolean {
    return false
  }

  override fun initView() {
    binding.page.onRefresh {
      scope {
        val result = Get<Page<AlbumModel>>(Api.SEARCH_ALBUM) {
          addQuery("keyword", keyword)
          addQuery("pageNo", index)
          addQuery("pageSize", 20)
        }.await()

        addData(result.data.onEach {
          it.album = Html.fromHtml(it.album, Html.FROM_HTML_MODE_COMPACT).toString()
        }) {
          result.total > modelCount
        }

        result.page = index
        addPage(result)
      }
    }

    binding.rvAlbum.removeAllItemDecorator().divider {
      setDivider(16, true)
      includeVisible = true
      orientation = DividerOrientation.GRID
    }.setup {
      addType<AlbumModel>(R.layout.item_grid_album)

      R.id.item_album.onClick {
        toast("暂时没有开发...")
      }
    }

    if (mData.isNotEmpty()) {
      binding.page.index = pageNo
      binding.rvAlbum.models = mData.toMutableList()
    }
  }
}