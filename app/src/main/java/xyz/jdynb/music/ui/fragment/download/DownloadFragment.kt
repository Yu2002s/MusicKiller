package xyz.jdynb.music.ui.fragment.download

import androidx.core.os.bundleOf
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicVpFragment
import xyz.jdynb.music.databinding.FragmentDownloadBinding
import xyz.jdynb.music.model.PageModel
import xyz.jdynb.music.utils.DownloadHelper

class DownloadFragment :
  BaseMusicVpFragment<FragmentDownloadBinding>(R.layout.fragment_download) {
  override fun getViewPager() = binding.vp

  override fun getPages() = listOf(
    PageModel(
      title = "全部",
      fragment = DownloadListFragment::class,
      bundleOf("type" to DownloadListFragment.TYPE_ALL)
    ),
    PageModel(
      title = "已下载",
      fragment = DownloadedFragment::class,
    )
  )

  override fun initView() {
    super.initView()
    binding.tvPath.text = "文件保存在: " + DownloadHelper.getDownloadDirectory(requireContext()).path
  }

  override fun initData() {
  }
}