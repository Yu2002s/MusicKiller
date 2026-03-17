package xyz.jdynb.music.ui.fragment.artist

import androidx.navigation.fragment.navArgs
import com.drake.engine.adapter.FragmentAdapter
import com.drake.engine.base.app
import com.google.android.material.tabs.TabLayoutMediator
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.databinding.FragmentArtistInfoBinding
import kotlin.math.abs

class ArtistInfoFragment: BaseMusicNavFragment<FragmentArtistInfoBinding>(R.layout.fragment_artist_info) {

  val args by navArgs<ArtistInfoFragmentArgs>()

  override fun initView() {
    setAppbar(binding.toolbar, binding.toolbarLayout)

    binding.vp.adapter = FragmentAdapter(listOf(ArtistMusicFragment()))
    TabLayoutMediator(binding.tab.tabLayout, binding.vp) { tab, position ->
      tab.text = "单曲(${args.artist.musicNum})"
    }.attach()

    var isExpanded = true
    binding.appbar.addOnOffsetChangedListener { appbar, offset ->
      if (abs(offset) >= appbar.totalScrollRange - 50) {
        if (isExpanded) {
          // 折叠
          isExpanded = false
          binding.toolbar.title = args.artist.name
        }
      } else {
        if (!isExpanded) {
          isExpanded = true
          binding.toolbar.title = " "
        }
      }
    }
  }

  override fun initData() {
    binding.m = args.artist
    binding.lifecycleOwner = this
  }
}