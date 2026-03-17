package xyz.jdynb.music.ui.fragment.play

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.dirror.lyricviewx.OnPlayClickListener
import com.drake.net.scope.AndroidScope
import com.drake.net.utils.scopeNetLife
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.databinding.FragmentLyricsBinding
import xyz.jdynb.music.utils.DownloadHelper
import xyz.jdynb.music.utils.lyric.LyricUtils
import java.io.FileReader

/**
 * 歌词
 */
class LyricsFragment : BaseMusicNavFragment<FragmentLyricsBinding>(R.layout.fragment_lyrics) {

  companion object {

    private const val TAG = "LyricsFragment"

  }

  private var job: AndroidScope? = null

  override fun initView() {
    binding.lyricViewX.setDraggable(true, object : OnPlayClickListener {
      override fun onPlayClick(time: Long): Boolean {
        mainViewModel.updateCurrentPosition(time)
        return true
      }
    })

    binding.lyricViewX.setCurrentTextSize(55f)
  }

  override fun initData() {
    lifecycleScope.launch {
      mainViewModel.musicModel.collect {
        if (it.id == 0L) {
          return@collect
        }
        job?.cancel()

        job = scopeNetLife {

          val lyricFile = DownloadHelper.getLyricFilePath(requireContext(), it)

          val lyricContent = if (lyricFile.exists() && lyricFile.length() > 0) {
            withIO {
              val fileReader = FileReader(lyricFile)
              fileReader.readText().also {
                fileReader.close()
              }
            }
          } else {
            LyricUtils.getLyricContent(it.id)
          }

          binding.lyricViewX.loadLyric(lyricContent)
        }.catch { error ->
          // toast("获取歌词失败")
          Log.e(TAG, error.message.toString())
        }
      }
    }

    lifecycleScope.launch {
      mainViewModel.currentPosition.collect {
        binding.lyricViewX.updateTime(it)
      }
    }

  }
}