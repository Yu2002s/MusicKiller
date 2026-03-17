package xyz.jdynb.music.ui.fragment.favorite

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.BindingAdapter
import com.drake.net.utils.withIO
import org.litepal.LitePal
import org.litepal.extension.find
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.databinding.FragmentFavoriteMusicBinding
import xyz.jdynb.music.model.FavoriteModel
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.PlayListModel
import xyz.jdynb.music.ui.fragment.playlist.PlayListInfoFragmentDirections
import xyz.jdynb.music.utils.onLoad

class MusicFavoriteFragment :
  BaseMusicNavFragment<FragmentFavoriteMusicBinding>(R.layout.fragment_favorite_music) {

  private var type = FavoriteModel.TYPE_SONG

  companion object {

    private const val PARAM_TYPE = "type"

    @JvmStatic
    fun newInstance(type: Int = FavoriteModel.TYPE_SONG): MusicFavoriteFragment {
      val fragment = MusicFavoriteFragment()
      fragment.arguments = bundleOf(PARAM_TYPE to type)
      return fragment
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    type = requireArguments().getInt(PARAM_TYPE, FavoriteModel.TYPE_SONG)
    enableMultiMode = type == FavoriteModel.TYPE_SONG
  }

  override fun openMediaController(): Boolean {
    return true
  }

  override fun getMusicRecyclerView(): RecyclerView? {
    return binding.rvFavorite
  }

  override fun onMusicRecyclerViewSetup(onMusicItemClick: (BindingAdapter.(Int, MusicModel) -> Unit)?) {

    if (type == FavoriteModel.TYPE_SONG) {
      return super.onMusicRecyclerViewSetup(onMusicItemClick)
    }

    super.onMusicRecyclerViewSetup { position, model ->
      navController.navigate(FavoriteFragmentDirections.actionPlaylistInfo(
        PlayListModel(id = model.id)
      ))
    }
  }

  override fun initView() {
    binding.page.onLoad(this) { page ->
      withIO {
        LitePal.where("type = ?", type.toString())
          .order("createAt desc")
          .find<FavoriteModel>().map {
            MusicModel(
              name = it.name,
              pic = it.cover,
              hasLossless = it.hasLossless,
              id = it.musicId,
              artist = it.author,
              artistId = it.authorId,
              duration = it.duration
            ).also { model ->
              model.isFavorite = true
              model.isPlayList = it.type == FavoriteModel.TYPE_PLAYLIST
            }
          }
      }.also {
        page.addData(it)
      }
    }.setEnableLoadMore(false)
  }

  override fun initData() {

  }

  override fun onResume() {
    if (!isFirstResume) {
      binding.page.refresh()
    }
    super.onResume()
  }
}