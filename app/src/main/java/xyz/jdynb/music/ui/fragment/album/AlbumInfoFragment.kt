package xyz.jdynb.music.ui.fragment.album

import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.drake.net.Get
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentAlbumInfoBinding
import xyz.jdynb.music.model.AlbumInfoModel
import xyz.jdynb.music.utils.addWithData
import xyz.jdynb.music.utils.onLoad

/**
 * 专辑信息
 */
class AlbumInfoFragment :
  BaseMusicNavFragment<FragmentAlbumInfoBinding>(R.layout.fragment_album_info) {

  private val args by navArgs<AlbumInfoFragmentArgs>()

  init {
    enableMultiMode = true
  }

  override fun getMusicRecyclerView(): RecyclerView {
    return binding.albumRv
  }

  override fun initView() {
    setAppbar(binding.toolbar, binding.collapsingToolbarLayout)
  }

  override fun initData() {
    binding.m = AlbumInfoModel(
      name = args.albumInfo.album,
      artist = args.albumInfo.artist,
      info = args.albumInfo.albumInfo,
      img = args.albumInfo.pic
    )

    binding.page.onLoad(this) { page ->
      val albumInfo = Get<AlbumInfoModel>(Api.ALBUM_INFO) {
        addQuery("id", args.albumInfo.albumId)
      }.await()

      binding.m = albumInfo

      page.addWithData(albumInfo.musicList)
    }.setEnableLoadMore(false)
  }


}