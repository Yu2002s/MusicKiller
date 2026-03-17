package xyz.jdynb.music.ui.fragment.download

import com.drake.brv.utils.models
import com.drake.net.utils.withIO
import org.litepal.LitePal
import org.litepal.extension.find
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.databinding.FragmentDownloadedBinding
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.download.DownloadModel
import xyz.jdynb.music.model.setupMusicRv
import xyz.jdynb.music.utils.addWithData
import xyz.jdynb.music.utils.onLoad

class DownloadedFragment :
  BaseMusicNavFragment<FragmentDownloadedBinding>(R.layout.fragment_downloaded) {

  override fun openMediaController(): Boolean {
    return true
  }

  override fun onResume() {
    if (!isFirstResume) {
      binding.page.refresh()
    }
    super.onResume()
  }

  override fun initView() {
    binding.rvDownloaded.setupMusicRv(this)

    binding.page.onLoad(this) { page ->
      page.addWithData(withIO {
        LitePal.order("updateAt desc")
          .where("status = ?", DownloadModel.STATUS_COMPLETED.toString())
          .find<DownloadModel>()
          .map {
            MusicModel(
              pic = it.cover,
              name = it.name,
              id = it.musicId,
              artist = it.artist,
              localPath = it.localPath,
            )
          }
      })
    }.setEnableLoadMore(false)
  }

  override fun getMusicModels() = binding.rvDownloaded.models

  override fun initData() {

  }

}