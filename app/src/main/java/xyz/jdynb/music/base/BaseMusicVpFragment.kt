package xyz.jdynb.music.base

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.drake.engine.adapter.FragmentAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import xyz.jdynb.music.databinding.LayoutTabBinding
import xyz.jdynb.music.model.PageModel

abstract class BaseMusicVpFragment<V : ViewDataBinding>(@LayoutRes contentLayoutId: Int = 0) :
  BaseMusicAppbarFragment<V>(contentLayoutId) {

  private lateinit var tab: TabLayout

  private val fragments = mutableListOf<BaseMusicNavFragment<*>>()

  protected val currentFragment get() = fragments.getOrNull(tab.selectedTabPosition)

  abstract fun getViewPager(): ViewPager2

  abstract fun getPages(): List<PageModel<*>>

  protected open fun onTabChange(position: Int, pageModel: PageModel<*>) {

  }

  override fun isAddScrollView(): Boolean {
    return false
  }

  override fun getAppbarContent(inflater: LayoutInflater): View? {
    tab = LayoutTabBinding.inflate(inflater).tabLayout
    return tab
  }

  override fun initView() {
    val pagesModels = getPages()
    val viewPager2 = getViewPager()
    val titles = pagesModels.map { it.title }

    viewPager2.offscreenPageLimit = pagesModels.size
    fragments.clear()
    fragments.addAll(pagesModels.mapNotNull {
      it.fragment.java.getDeclaredConstructor().newInstance().also { instance ->
        instance.arguments = it.args
      } as? BaseMusicNavFragment<*>
    })
    viewPager2.adapter = FragmentAdapter(fragments)

    TabLayoutMediator(tab, viewPager2) {tab, position ->
      tab.text = titles[position]
    }.attach()

    tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        onTabChange(tab.position, pagesModels[tab.position])
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {
        fragments.getOrNull(tab.position)?.closeMultiMode()
      }

      override fun onTabReselected(p0: TabLayout.Tab?) {

      }

    })
  }
}