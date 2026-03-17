package xyz.jdynb.music.ui.fragment

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.drake.engine.adapter.FragmentAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicAppbarFragment
import xyz.jdynb.music.databinding.FragmentHomeBinding
import xyz.jdynb.music.databinding.LayoutTabBinding
import xyz.jdynb.music.ui.fragment.artist.ArtistFragment
import xyz.jdynb.music.ui.fragment.playlist.PlayListFragment
import xyz.jdynb.music.ui.fragment.rank.RankFragment
import xyz.jdynb.music.ui.fragment.recommend.RecommendFragment

class HomeFragment : BaseMusicAppbarFragment<FragmentHomeBinding>(R.layout.fragment_home),
  MenuProvider {

  private lateinit var tab: TabLayout

  override fun isAddScrollView(): Boolean {
    return false
  }

  override fun getAppbarContent(inflater: LayoutInflater): View? {
    tab = LayoutTabBinding.inflate(inflater).tabLayout
    return tab
  }

  override fun initData() {

  }

  override fun initView() {
    requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

    binding.vp.adapter = FragmentAdapter(listOf(
      RecommendFragment(), PlayListFragment(), RankFragment(), ArtistFragment(),
    ))
    binding.vp.offscreenPageLimit = 5
    binding.vp.isSaveEnabled = true

    val titles = arrayOf("推荐", "歌单", "排行榜", "歌手")
    TabLayoutMediator(tab, binding.vp) { tab, position ->
      tab.text = titles[position]
    }.attach()
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.menu_home, menu)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    when (menuItem.itemId) {
      R.id.search -> {
        navController.navigate(HomeFragmentDirections.actionSearch())
      }

      R.id.setting -> {
        navController.navigate(HomeFragmentDirections.actionSetting())
      }
    }
    return true
  }
}