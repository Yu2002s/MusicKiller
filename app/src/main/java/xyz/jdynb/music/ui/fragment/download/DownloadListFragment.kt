package xyz.jdynb.music.ui.fragment.download

import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.drake.brv.utils.linear
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.net.utils.scope
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch
import org.litepal.LitePal
import org.litepal.extension.find
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.databinding.FragmentDownloadListBinding
import xyz.jdynb.music.model.download.DownloadModel

class DownloadListFragment : BaseMusicNavFragment<FragmentDownloadListBinding>(R.layout.fragment_download_list) {

  companion object {

    private const val TAG= "DownloadListFragment"

    const val TYPE_ALL = 0

    const val TYPE_DOWNLOADED = 1

    /**
     * 准备下载
     */
    const val TYPE_PENDING = 2

    fun newInstance(type: Int): DownloadListFragment {
      val fragment = DownloadListFragment()
      fragment.arguments = bundleOf("type" to type)
      return fragment
    }
  }

  override fun openMediaController(): Boolean {
    return true
  }

  override fun initView() {

    binding.page.onRefresh {
      // Support refresh operation
      scope {
        val data = withIO {
          loadDownloads()
        }
        Log.i(TAG, "download data: $data")
        addData(data)
      }
    }.apply {
      setEnableLoadMore(false)
      showLoading()
    }

    binding.rvDownload.linear().setup {
      addType<DownloadModel>(R.layout.item_list_download)

      // Pause/Resume button click
      R.id.btn_pause_resume.onClick {
        val model = getModel<DownloadModel>()
        when (model.status) {
          DownloadModel.STATUS_DOWNLOADING, DownloadModel.STATUS_PENDING -> {
            downloadService?.pauseDownload(model.musicId)
          }

          DownloadModel.STATUS_PAUSED -> {
            downloadService?.resumeDownload(model.musicId)
          }
        }
      }

      // Cancel button click
      R.id.btn_cancel.onClick {
        val model = getModel<DownloadModel>()
        downloadService?.cancelDownload(model.musicId)
        mutable.removeAt(modelPosition)
        notifyItemRemoved(modelPosition)
      }

      // Retry button click
     R.id.btn_retry.onClick {
        val model = getModel<DownloadModel>()
        downloadService?.retryDownload(model.musicId)
      }

      // Delete button click
      R.id.btn_delete.onClick {
        val model = getModel<DownloadModel>()
        downloadService?.deleteDownload(model.musicId)
        mutable.removeAt(modelPosition)
        notifyItemRemoved(modelPosition)
      }

      R.id.btn_play.onClick {
        val model = getModel<DownloadModel>()
        addDownloadPlay(downloadModel = model)
      }
    }
  }

  override fun initData() {
    observeDownloadProgress()
  }

  /**
   * Load downloads based on type
   */
  private fun loadDownloads(): List<DownloadModel> {
    val type = arguments?.getInt("type") ?: TYPE_ALL
    return when (type) {
      TYPE_ALL -> LitePal.order("updateAt desc").find<DownloadModel>()
      TYPE_DOWNLOADED -> LitePal
        .where("status = ?", DownloadModel.STATUS_COMPLETED.toString())
        .order("completeAt desc")
        .find<DownloadModel>()

      else -> emptyList()
    }
  }

  /**
   * Observe download progress from DownloadService
   */
  private fun observeDownloadProgress() {
    _downloadService?.let { service ->
      viewLifecycleOwner.lifecycleScope.launch {
        service.downloadProgress.collect { progressMap ->
          // Update RecyclerView items based on progress
          binding.rvDownload.models?.forEachIndexed { index, model ->
            if (model is DownloadModel) {
              progressMap[model.musicId]?.let { progress ->
                // Only update if there are changes
                if (model.downloadedSize != progress.downloadedSize || model.status != progress.status) {
                  model.downloadedSize = progress.downloadedSize
                  model.status = progress.status
                  binding.rvDownload.adapter?.notifyItemChanged(index)
                }
              }
            }
          }
        }
      }
    }
  }
}