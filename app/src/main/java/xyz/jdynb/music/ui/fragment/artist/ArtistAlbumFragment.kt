package xyz.jdynb.music.ui.fragment.artist

import android.text.Html
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.net.Get
import com.drake.net.utils.scope
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentArtistAlbumBinding
import xyz.jdynb.music.model.AlbumModel
import xyz.jdynb.music.model.Page
import xyz.jdynb.music.utils.removeAllItemDecorator

class ArtistAlbumFragment: BaseMusicNavFragment<FragmentArtistAlbumBinding>(R.layout.fragment_artist_album) {

  init {
    enableMediaController = false
  }

  private val args get() = (requireParentFragment() as ArtistInfoFragment).args

  private val data = mutableListOf<AlbumModel>()

  override fun initView() {
    binding.rvArtistAlbum.removeAllItemDecorator()
      .divider {
        setDivider(16, true)
        includeVisible = true
        orientation = DividerOrientation.GRID
      }.setup {
        addType<AlbumModel>(R.layout.item_grid_album)

        R.id.item_album.onClick {
          navController.navigate(ArtistInfoFragmentDirections.actionAlbumInfo(getModel()))
        }
      }
  }

  override fun initData() {
    binding.page.onRefresh {
      scope {
        val result = Get<Page<AlbumModel>>(Api.ARTIST_ALBUM) {
          addQuery("artistId", args.artist.id)
          addQuery("pageNo", index)
          addQuery("pageSize", 20)
        }.await()

        addData(result.data.onEach {
          it.album = Html.fromHtml(it.album, Html.FROM_HTML_MODE_COMPACT).toString()
        }) {
          result.total > modelCount
        }

        pageNo = index

        if (index == 1) {
          data.clear()
        }
        data.addAll(result.data)
      }
    }

    if (data.isEmpty()) {
      binding.page.showLoading()
    } else {
      binding.page.index = pageNo
      binding.rvArtistAlbum.models = data.toMutableList()
    }
  }
}