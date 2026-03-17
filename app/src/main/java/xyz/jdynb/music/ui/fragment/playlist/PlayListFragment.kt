package xyz.jdynb.music.ui.fragment.playlist

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.utils.dp
import com.drake.net.Get
import com.drake.net.utils.scope
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentPlaylistBinding
import xyz.jdynb.music.model.Page
import xyz.jdynb.music.model.PlayListModel
import xyz.jdynb.music.model.PlayListTagModel
import xyz.jdynb.music.model.QueryPlayListParams
import xyz.jdynb.music.ui.fragment.HomeFragmentDirections
import xyz.jdynb.music.utils.query
import xyz.jdynb.music.utils.removeAllItemDecorator

/**
 * 歌单
 */
class PlayListFragment : BaseMusicNavFragment<FragmentPlaylistBinding>(R.layout.fragment_playlist) {

  private val viewModel by viewModels<PlayListViewModel>()

  private var currentOrder = "new"

  override fun onFirstResume() {
    super.onFirstResume()
    val models = viewModel.playlistPageStateFlow.value

    if (models.isEmpty()) {
      binding.refresh.showLoading()
    }
  }

  override fun initData() {
    val models = viewModel.playlistPageStateFlow.value

    if (models.isNotEmpty()) {
      // 不是首次加载，设置页码，并且设置已缓存的数据
      binding.refresh.index = viewModel.pageNo
      binding.rvPlaylist.models = models.toMutableList()
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.currentTagPositionStateFlow.collect {
        binding.rvTag.models = viewModel.tagsStateFlow.value.getOrNull(it)?.data
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.tagsStateFlow.collect { tags ->
        binding.tabPlaylistCate.removeAllTabs()
        tags.forEachIndexed { index, model ->
          val tab = binding.tabPlaylistCate.newTab()
          tab.text = model.name
          binding.tabPlaylistCate.addTab(tab, index == viewModel.currentTagPositionStateFlow.value)
        }
      }
    }
  }

  override fun initView() {
    binding.refresh.onRefresh {
      scope {
        if (index == 1) {
          viewModel.clearPlaylist()

          val tags = Get<List<PlayListTagModel>>(Api.PLAYLIST_TAGS).await()
          viewModel.updateTagsState(tags)
          viewModel.updateTagPositionState(0)
        }

        val playListData = async {
          Get<Page<PlayListModel>>(Api.PLAYLIST_PAGE) {
            query(QueryPlayListParams(pageNo = index, pageSize = 21, order = currentOrder))
          }.await()
        }.await()
        addData(playListData.data, binding.rvPlaylist.bindingAdapter) {
          modelCount < playListData.total
        }
        playListData.page = index
        viewModel.addPlaylist(playListData)
      }
    }

    binding.rvTag.removeAllItemDecorator().dividerSpace(8.dp, DividerOrientation.GRID).setup {
      addType<PlayListTagModel>(R.layout.item_list_tag)

      R.id.item_tag.onClick {
        // HomeFragmentDirections
        navController.navigate(HomeFragmentDirections.actionPlaylistType(getModel()))
      }
    }

    binding.rvPlaylist.removeAllItemDecorator().dividerSpace(10.dp, DividerOrientation.GRID).setup {
      addType<PlayListModel>(R.layout.item_playlist)

      R.id.item_playlist.onClick {
        navController.navigate(HomeFragmentDirections.actionPlaylistInfo(getModel()))
      }
    }

    binding.tabPlaylistCate.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        viewModel.updateTagPositionState(tab.position)
      }

      override fun onTabReselected(p0: TabLayout.Tab?) {

      }

      override fun onTabUnselected(p0: TabLayout.Tab?) {

      }
    })

    binding.tabOrder.getTabAt(if (currentOrder == "new") 0 else 1)?.select()
    binding.tabOrder.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        val order = if (tab.position == 0) "new" else "hot"
        if (currentOrder == order) {
          return
        }
        currentOrder = order
        binding.refresh.refresh()
      }

      override fun onTabReselected(p0: TabLayout.Tab?) {

      }

      override fun onTabUnselected(p0: TabLayout.Tab?) {

      }
    })
  }
}