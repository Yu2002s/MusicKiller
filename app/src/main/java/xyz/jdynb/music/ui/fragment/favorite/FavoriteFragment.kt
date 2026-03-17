package xyz.jdynb.music.ui.fragment.favorite

import androidx.core.os.bundleOf
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicVpFragment
import xyz.jdynb.music.databinding.FragmentFavoriteBinding
import xyz.jdynb.music.model.FavoriteModel
import xyz.jdynb.music.model.PageModel

class FavoriteFragment :
  BaseMusicVpFragment<FragmentFavoriteBinding>(R.layout.fragment_favorite) {
  override fun getViewPager() = binding.vpFavorite

  override fun onTabChange(position: Int, pageModel: PageModel<*>) {
    super.onTabChange(position, pageModel)
  }

  override fun getPages() = listOf(
    PageModel(
      title = "歌曲",
      fragment = MusicFavoriteFragment::class,
      bundleOf("type" to FavoriteModel.TYPE_SONG)
    ),
    PageModel(
      title = "歌单",
      fragment = MusicFavoriteFragment::class,
      bundleOf("type" to FavoriteModel.TYPE_PLAYLIST)
    )
  )

  override fun initData() {

  }
}