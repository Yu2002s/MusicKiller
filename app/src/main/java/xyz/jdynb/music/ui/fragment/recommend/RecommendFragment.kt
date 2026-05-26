package xyz.jdynb.music.ui.fragment.recommend

import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.utils.dp
import com.drake.net.Get
import com.drake.net.cache.CacheMode
import com.drake.net.utils.scope
import com.drake.net.utils.scopeNet
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentRecommendBinding
import xyz.jdynb.music.databinding.ItemRankBinding
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.MusicRankMenuItemModel
import xyz.jdynb.music.model.MusicRankModel
import xyz.jdynb.music.model.Page
import xyz.jdynb.music.model.PlayListModel
import xyz.jdynb.music.model.PlayListTagModel
import xyz.jdynb.music.model.QueryPlayListParams
import xyz.jdynb.music.ui.fragment.HomeFragmentDirections
import xyz.jdynb.music.utils.query
import xyz.jdynb.music.utils.removeAllItemDecorator

class RecommendFragment :
  BaseMusicNavFragment<FragmentRecommendBinding>(R.layout.fragment_recommend) {

  private val playListTagModels = mutableListOf<PlayListTagModel>()

  private val viewModel by viewModels<RecommendViewModel>()

  override fun initData() {
    lifecycleScope.launch {
      viewModel.tagsStateFlow.collect {
        playListTagModels.clear()
        playListTagModels.addAll(it)
        binding.tabPlaylistCate.removeAllTabs()
        playListTagModels.forEach { tag ->
          val tab = binding.tabPlaylistCate.newTab()
          tab.text = tag.name
          binding.tabPlaylistCate.addTab(tab, viewModel.currentTagStateFlow.value == tag.id)
        }
      }
    }

    lifecycleScope.launch {
      viewModel.playListStateFlow.collect {
        binding.recommendRv.models = it
      }
    }

    lifecycleScope.launch {
      viewModel.rankMusicStateFlow.collect {
        binding.rankRv.models = it
      }
    }
  }

  override fun onFirstLoad() {
    super.onFirstLoad()
  }

  private suspend fun loadData(cacheModel: CacheMode) = coroutineScope {
    val tags = Get<MutableList<PlayListTagModel>>(Api.PLAYLIST_INDEX_TAGS) {
      setCacheMode(cacheModel)
    }

    val ranks = Get<List<MusicRankModel>>(Api.RANK_INDEX) {
      setCacheMode(cacheModel)
    }

    val playListModelData = Get<Page<PlayListModel>>(Api.PLAYLIST_RECOMMEND) {
      query(QueryPlayListParams(id = "rcm"))
      setCacheMode(cacheModel)
    }

    val playListTagModels = tags.await()
    playListTagModels.add(0, PlayListTagModel(name = "每日推荐"))
    viewModel.updateTagsState(playListTagModels)
    viewModel.updatePlayListState(playListModelData.await().data)
    viewModel.changeRankMusicState(ranks.await().onEach {
      it.leader = it.leader.replace("酷我", "")
    })
  }

  override fun initView() {
    binding.refresh.onRefresh {
      scope {
        loadData(CacheMode.WRITE)
      }.preview {
        loadData(CacheMode.READ)
      }
    }.setEnableLoadMore(false)

    // 如果没有数据就触发重新加载
    if (viewModel.playListStateFlow.value.isEmpty()) {
      binding.refresh.showLoading()
    }

    binding.tvDownloaded.setOnClickListener {
      navController.navigate(HomeFragmentDirections.actionDownload())
    }

    binding.tvFavorite.setOnClickListener {
      navController.navigate(HomeFragmentDirections.actionFavorite())
    }

    binding.tvHistory.setOnClickListener {
      navController.navigate(HomeFragmentDirections.actionHistory())
    }

    binding.recommendRv.removeAllItemDecorator().dividerSpace(10.dp, DividerOrientation.GRID)
      .setup {
        addType<PlayListModel>(R.layout.item_playlist)

        R.id.item_playlist.onClick {
          navController.navigate(HomeFragmentDirections.actionPlaylistInfo(getModel()))
        }
      }

    binding.rankRv.removeAllItemDecorator().dividerSpace(10.dp).setup {
      addType<MusicRankModel>(R.layout.item_rank)

      R.id.tv_leader.onClick {
        val model = getModel<MusicRankModel>()
        navController.navigate(
          HomeFragmentDirections.actionRankInfo(
            MusicRankMenuItemModel(
              name = model.leader,
              pic = model.pic,
              pub = model.pub,
              sourceId = model.id
            )
          )
        )
      }

      onCreate {
        getBinding<ItemRankBinding>().musicRv.setup {
          addType<MusicModel>(R.layout.item_list_rank)

          R.id.item_rank.onClick {
            val model = getModel<MusicModel>()
            addPlay(model)
          }
        }
      }

      onBind {
        getBinding<ItemRankBinding>().musicRv.models = getModel<MusicRankModel>().musicList
      }
    }

    binding.tabPlaylistCate.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        val playListTag = playListTagModels[tab.position]
        if (!viewModel.changeCurrentTagState(playListTag.id)) {
          return
        }
        scopeNet {
          val url = if (tab.position == 0) Api.PLAYLIST_RECOMMEND else Api.PLAYLIST_BY_TAG
          val playListModelData = Get<Page<PlayListModel>>(url) {
            query(QueryPlayListParams(id = playListTag.id))
          }.await()
          viewModel.updatePlayListState(playListModelData.data)
        }
      }

      override fun onTabReselected(p0: TabLayout.Tab?) {
      }

      override fun onTabUnselected(p0: TabLayout.Tab?) {
      }
    })
  }
}