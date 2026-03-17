package xyz.jdynb.music.ui.fragment.playlist

import android.os.Bundle
import androidx.navigation.fragment.navArgs
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.divider
import com.drake.brv.utils.setup
import com.drake.net.Get
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicAppbarFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentPlaylistTypeBinding
import xyz.jdynb.music.model.Page
import xyz.jdynb.music.model.PlayListModel
import xyz.jdynb.music.model.QueryPlayListParams
import xyz.jdynb.music.ui.fragment.HomeFragmentDirections
import xyz.jdynb.music.utils.addWithData
import xyz.jdynb.music.utils.onLoad
import xyz.jdynb.music.utils.query
import xyz.jdynb.music.utils.removeAllItemDecorator

class PlayListTypeFragment :
  BaseMusicAppbarFragment<FragmentPlaylistTypeBinding>(R.layout.fragment_playlist_type) {

  override fun isAddScrollView(): Boolean {
    return false
  }

  private val args by navArgs<PlayListTypeFragmentArgs>()

  // private val mData = mutableListOf<PlayListModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    navController.currentDestination?.label = args.playListTag.name
  }

  override fun initView() {
    /*binding.page.onRefresh {
      scope {
        val result = Get<Page<PlayListModel>>(Api.TAG_PLAYLIST_PAGE) {
          query(QueryPlayListParams(id = args.playListTag.id, pageNo = index, pageSize = 21, order = "new"))
        }.await()
        if (index == 1) {
          mData.clear()
        }
        pageNo = index
        mData.addAll(result.data)
        addData(result.data) {
          result.total > modelCount
        }
      }
    }*/

    binding.rvPlaylist.removeAllItemDecorator()
      .divider {
        setDivider(16, true)
        includeVisible = true
        orientation = DividerOrientation.GRID
      }
      .setup {
        addType<PlayListModel>(R.layout.item_playlist)

        R.id.item_playlist.onClick {
          navController.navigate(HomeFragmentDirections.actionPlaylistInfo(getModel()))
        }
      }

    binding.page.onLoad(this) { page ->
      val result = Get<Page<PlayListModel>>(Api.TAG_PLAYLIST_PAGE) {
        query(
          QueryPlayListParams(
            id = args.playListTag.id,
            pageNo = page.index,
            pageSize = 21,
            order = "new"
          )
        )
      }.await()

      page.addWithData(result.data) {
        result.total > modelCount
      }
    }
  }

  override fun initData() {
    /*if (mData.isEmpty()) {
      binding.page.showLoading()
    } else {
      binding.page.index = pageNo
      binding.page.addData(mData.toMutableList())
    }*/
  }
}