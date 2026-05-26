package xyz.jdynb.music.ui.fragment.search

import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.utils.models
import com.drake.net.Get
import com.drake.net.utils.scope
import xyz.jdynb.music.R
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentSearchMusicListBinding
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.Page

/**
 * 搜索音乐列表
 */
class SearchMusicListFragment :
        BaseSearchListFragment<FragmentSearchMusicListBinding>(R.layout.fragment_search_music_list){

  init {
    enableMultiMode = true
  }

  override fun getMusicModels(): List<Any?>? {
    return binding.rvMusic.models
  }

  override fun getMusicRecyclerView(): RecyclerView {
    return binding.rvMusic
  }

  override fun initView() {
    binding.page.onRefresh {
      scope {
        val result = Get<Page<MusicModel>>(Api.SEARCH) {
          addQuery("keyword", keyword)
          addQuery("pageNo", index)
          addQuery("pageSize", 20)
        }.await()
        addData(result.data.onEach {
          mainViewModel.getMusicModelState(it)
        }) {
          result.total > modelCount
        }
        result.page = index
        addPage(result)
      }
    }

    if (mData.isNotEmpty()) {
      binding.page.index = pageNo
      binding.rvMusic.models = mData.toMutableList()
    }
  }

  override fun onSearch() {
    binding.page.showLoading()
  }
}
