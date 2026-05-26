package xyz.jdynb.music.ui.fragment.playlist

import androidx.core.view.WindowCompat
import androidx.navigation.fragment.navArgs
import com.drake.brv.utils.models
import com.drake.net.Get
import com.drake.net.utils.scope
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentPlaylistInfoBinding
import xyz.jdynb.music.model.PlayListModel
import xyz.jdynb.music.model.QueryPlayListParams
import xyz.jdynb.music.utils.addWithData
import xyz.jdynb.music.utils.onLoad
import xyz.jdynb.music.utils.query

/**
 * 歌单信息
 */
class PlayListInfoFragment :
  BaseMusicNavFragment<FragmentPlaylistInfoBinding>(R.layout.fragment_playlist_info) {

  private val args by navArgs<PlayListInfoFragmentArgs>()

  init {
    enableMultiMode = true
  }

  override fun initData() {
    binding.page.onLoad(this) { page ->
      val playlist = Get<PlayListModel>(Api.PLAYLIST_INFO) {
        query(QueryPlayListParams(pageNo = page.index, pageSize = 30, pid = args.playlist.id))
      }.await()

      binding.m = playlist

      page.addWithData(playlist.musicList) {
        playlist.total > modelCount
      }
    }
  }

  override fun getMusicModels() = binding.playlistRv.models

  override fun getMusicRecyclerView() = binding.playlistRv

  override fun initView() {
    setAppbar(binding.toolbar, binding.collapsingToolbarLayout)

    binding.btnPlay.setOnClickListener {
      addPlaylist((binding.playlistRv.models) ?: emptyList())
    }

    var isFavorite = false
    scope {
      isFavorite = mainViewModel.isFavoritePlaylist(args.playlist)
      setFavoriteButtonIcon(isFavorite)
    }

    binding.btnFavorite.setOnClickListener {
      mainViewModel.addOrRemoveFavorite(args.playlist)
      isFavorite = !isFavorite
      setFavoriteButtonIcon(isFavorite)
    }
  }

  private fun setFavoriteButtonIcon(isFavorite: Boolean) {
    binding.btnFavorite.setIconResource(
      if (isFavorite) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
    )
  }

  override fun onStart() {
    super.onStart()
    lightStatusBar(false)
  }

  override fun onStop() {
    super.onStop()
    lightStatusBar(true)
  }

  private fun lightStatusBar(isLight: Boolean = true) {
    val insetsController = WindowCompat.getInsetsController(requireActivity().window, binding.root)
    insetsController.isAppearanceLightStatusBars = isLight
  }
}