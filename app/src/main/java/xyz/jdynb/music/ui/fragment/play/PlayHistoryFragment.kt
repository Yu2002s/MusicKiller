package xyz.jdynb.music.ui.fragment.play

import androidx.recyclerview.widget.RecyclerView
import com.drake.net.utils.withIO
import org.litepal.LitePal
import org.litepal.extension.find
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicAppbarFragment
import xyz.jdynb.music.databinding.FragmentPlayHistoryBinding
import xyz.jdynb.music.model.PlayHistory
import xyz.jdynb.music.utils.addWithData
import xyz.jdynb.music.utils.onLoad

class PlayHistoryFragment :
  BaseMusicAppbarFragment<FragmentPlayHistoryBinding>(R.layout.fragment_play_history) {

  init {
    enableMultiMode = true
  }

  override fun openMediaController(): Boolean {
    return true
  }

  override fun isAddScrollView(): Boolean {
    return false
  }

  override fun getMusicRecyclerView(): RecyclerView? {
    return binding.rvPlay
  }

  override fun initView() {
    binding.page.onLoad(this) { page ->

      val data = withIO {
        LitePal.order("updateTime desc")
          .offset((page.index - 1) * 20)
          .limit(20)
          .find<PlayHistory>().map { history ->
            history.toMusicModel()
          }
      }

      page.addWithData(data)
    }
  }

  override fun initData() {

  }
}