package xyz.jdynb.music.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.databinding.ViewDataBinding
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.drake.brv.BindingAdapter
import com.drake.net.Get
import com.drake.net.utils.scopeDialog
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.jdynb.music.R
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.enums.MusicBridge
import xyz.jdynb.music.model.ArtistModel
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.setupMusicRv
import xyz.jdynb.music.ui.fragment.HomeFragmentDirections

/**
 * 导航基类（不包含Appbar）
 */
abstract class BaseMusicNavFragment<V : ViewDataBinding>(@LayoutRes contentLayoutId: Int = 0) :
  BaseMusicFragment<V>(contentLayoutId), MenuProvider {

  /**
   * 导航控制器
   */
  protected lateinit var navController: NavController

  /**
   * 是否开启多选模式
   */
  var enableMultiMode: Boolean = false

  /**
   * 获取音乐RecyclerView的BindingAdapter
   */
  private val bindingAdapter get() = getMusicRecyclerView()?.adapter as? BindingAdapter

  /**
   * 音乐 Item 点击事件
   */
  var onMusicItemClick : (BindingAdapter.(Int, MusicModel) -> Unit)? = null

  /**
   * 音乐勾选改变事件
   */
  var onMusicCheckChange: (BindingAdapter.(Int, MusicModel, Boolean, Boolean) -> Unit)? = null

  /**
   * 音乐多选切换改变事件
   */
  var onMusicToggleChange : (BindingAdapter.(Int, MusicModel, Boolean) -> Unit)? = { _, _, _ ->
    requireActivity().invalidateMenu()
  }

  /**
   * MusicRecyclerView 初始化时调用
   *
   * @param onMusicItemClick music Item 点击事件
   */
  protected open fun onMusicRecyclerViewSetup() {
    getMusicRecyclerView()?.setupMusicRv(
      this,
      onItemClick = onMusicItemClick,
      onCheckChange = onMusicCheckChange,
      onToggleChange = onMusicToggleChange
    )
  }

  override fun onCreateMediaController(controller: MediaController) {
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    if (!enableMultiMode) {
      return
    }
    menuInflater.inflate(R.menu.menu_playlist, menu)
  }

  override fun onPrepareMenu(menu: Menu) {
    if (!enableMultiMode) {
      return
    }
    menu.findItem(R.id.download)?.isVisible = bindingAdapter?.toggleMode ?: false
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    return when (menuItem.itemId) {
      R.id.select -> {
        bindingAdapter?.toggle()
        true
      }

      R.id.download -> {
        val checkedModels = getMusicModels()?.filter {
          it as MusicModel
          it.checked
        }
        if (!checkedModels.isNullOrEmpty()) {
          val qualities = MusicBridge.entries.map { it.level }.toTypedArray()
          var currentBridge = MusicBridge.FLAC_2000K
          MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择音质")
            .setSingleChoiceItems(qualities, qualities.lastIndex) { dialog, which ->
              currentBridge = MusicBridge.entries[which]
            }
            .setPositiveButton("下载(${checkedModels.size})") { dialog, which ->
              checkedModels.forEach { model ->
                downloadService?.addDownload(model as MusicModel, currentBridge)
                bindingAdapter?.toggle(false)
              }
            }
            .setNegativeButton("取消", null)
            .show()
        }
        true
      }

      else -> false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    navController = findNavController()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    onMusicRecyclerViewSetup()

    super.onViewCreated(view, savedInstanceState)

    if (enableMultiMode) {
      requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }
  }

  /**
   * 设置 AppBar
   */
  fun setAppbar(toolbar: Toolbar, toolbarLayout: CollapsingToolbarLayout) {
    (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
    toolbarLayout.setupWithNavController(toolbar, navController)
  }

  /**
   * 设置 Toolbar
   */
  fun setToolbar(toolbar: Toolbar) {
    (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
    toolbar.setupWithNavController(navController)
  }

  /**
   * 取消多选
   */
  fun closeMultiMode() {
    if (!enableMultiMode) {
      return
    }
    if (view == null) {
      return
    }
    bindingAdapter?.toggle(false)
  }

  /**
   * 打开歌单列表
   */
  fun openPlaylist() {
    navController.navigate(HomeFragmentDirections.actionPlayQueue())
  }

  /**
   * 打开歌手信息页
   */
  fun openArtistInfo(artistId: Long = musicModel.artistId) {
    if (artistId == 0L) return
    scopeDialog {
      val artistInfo = Get<ArtistModel>(Api.ARTIST_INFO) {
        addQuery("artistId", artistId)
      }.await()
      mainViewModel.changeBottomBarExpand(false)
      navController.navigate(HomeFragmentDirections.actionArtistInfo(artistInfo))
    }
  }
}