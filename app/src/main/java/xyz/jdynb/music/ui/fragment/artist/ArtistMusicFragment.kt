package xyz.jdynb.music.ui.fragment.artist

import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.utils.models
import com.drake.net.Get
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentArtistMusicBinding
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.Page
import xyz.jdynb.music.utils.addWithData
import xyz.jdynb.music.utils.onLoad

class ArtistMusicFragment :
  BaseMusicNavFragment<FragmentArtistMusicBinding>(R.layout.fragment_artist_music) {

  init {
    enableMultiMode = true
  }

  private val args get() = (requireParentFragment() as ArtistInfoFragment).args

  override fun getMusicModels(): List<Any?>? {
    return binding.rvArtistMusic.models
  }

  override fun getMusicRecyclerView(): RecyclerView {
    return binding.rvArtistMusic
  }

  override fun initView() {
    binding.page.onLoad(this) { page ->
      val result = Get<Page<MusicModel>>(Api.ARTIST_MUSIC) {
        addQuery("artistId", args.artist.id)
        addQuery("pageNo", page.index)
        addQuery("pageSize", 20)
      }.await()
      page.addWithData(result.data) {
        result.total > modelCount
      }
    }
  }

  override fun initData() {

  }
}