package xyz.jdynb.music.ui.fragment.rank

import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.utils.models
import com.drake.net.Get
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicAppbarFragment
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.databinding.FragmentRankInfoBinding
import xyz.jdynb.music.model.MusicRankModel
import xyz.jdynb.music.utils.addWithData
import xyz.jdynb.music.utils.onLoad

class RankInfoFragment: BaseMusicAppbarFragment<FragmentRankInfoBinding>(R.layout.fragment_rank_info) {

  init {
    enableMultiMode = true
  }

  private val args by navArgs<RankInfoFragmentArgs>()

  override fun isAddScrollView(): Boolean {
    return false
  }

  override fun getMusicModels(): List<Any?>? {
    return binding.rvRankMusic.models
  }

  override fun getMusicRecyclerView(): RecyclerView? {
    return binding.rvRankMusic
  }

  override fun initView() {
    setTitle(args.rankItem.name)
    setSubTitle(args.rankItem.pub)

    binding.page.onLoad(this) { page ->
      val result = Get<MusicRankModel>(Api.RANK_MUSIC_LIST) {
        addQuery("rankId", args.rankItem.sourceId)
        addQuery("pageNo", page.index)
        addQuery("pageSize", "20")
      }.await()
      page.addWithData(result.musicList) {
        result.num > modelCount
      }
    }
  }

  override fun initData() {

  }
}