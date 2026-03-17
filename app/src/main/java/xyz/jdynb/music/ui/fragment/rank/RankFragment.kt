package xyz.jdynb.music.ui.fragment.rank

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.utils.dp
import com.drake.net.Get
import com.drake.net.utils.scope
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentRankBinding
import xyz.jdynb.music.databinding.ItemRankMenuBinding
import xyz.jdynb.music.model.MusicRankMenuItemModel
import xyz.jdynb.music.model.MusicRankMenuModel
import xyz.jdynb.music.ui.fragment.HomeFragmentDirections
import xyz.jdynb.music.utils.removeAllItemDecorator

/**
 * 排行榜
 */
class RankFragment : BaseMusicNavFragment<FragmentRankBinding>(R.layout.fragment_rank) {

  private val viewModel by viewModels<RankViewModel>()

  override fun onFirstResume() {
    super.onFirstResume()

    if (viewModel.uiState.value.isEmpty()) {
      binding.page.showLoading()
    }
  }

  override fun initView() {
    binding.page.onRefresh {
      setEnableLoadMore(false)
      scope {
        val list = Get<List<MusicRankMenuModel>>(Api.RANK_MENU).await()
        viewModel.updateState(list)
      }
    }

    binding.rvRank
      .setup {
        addType<MusicRankMenuModel>(R.layout.item_rank_menu)

        onCreate {
          getBinding<ItemRankMenuBinding>().rvMenu
            .setup {
              addType<MusicRankMenuItemModel>(R.layout.item_grid_rank_menu)

              R.id.item_rank.onClick {
                navController.navigate(HomeFragmentDirections.actionRankInfo(getModel()))
              }
            }
        }

        onBind {
          getBinding<ItemRankMenuBinding>().rvMenu.models = getModel<MusicRankMenuModel>().list
        }
      }
  }

  override fun initData() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.uiState.collect {
        binding.page.addData(it)
      }
    }
  }
}