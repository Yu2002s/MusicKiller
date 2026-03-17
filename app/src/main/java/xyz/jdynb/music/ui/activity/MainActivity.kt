package xyz.jdynb.music.ui.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.navigation.fragment.NavHostFragment
import com.drake.engine.adapter.FragmentAdapter
import com.drake.engine.base.EngineActivity
import com.drake.engine.utils.AppUtils
import com.drake.engine.utils.dp
import com.drake.net.Get
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.utils.scope
import com.drake.net.utils.scopeNet
import com.drake.tooltip.toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.config.shouldShowBottomNav
import xyz.jdynb.music.databinding.ActivityMainBinding
import xyz.jdynb.music.download.DownloadService
import xyz.jdynb.music.model.UpdateModel
import xyz.jdynb.music.ui.fragment.play.LyricsFragment
import xyz.jdynb.music.ui.fragment.play.MusicPlayFragment
import xyz.jdynb.music.utils.fixNestedScroll
import xyz.jdynb.music.utils.json
import java.io.File

class MainActivity : EngineActivity<ActivityMainBinding>(R.layout.activity_main), Player.Listener {

  private val mainViewModel by viewModels<MainViewModel>()

  private lateinit var bottomBarBehavior: BottomSheetBehavior<LinearLayout>

  private lateinit var windowInsetsController: WindowInsetsControllerCompat

  /**
   * 是否是亮色状态栏
   */
  private var isLightStatusBar = true

  private val bottomBarHeight = 68.dp

  var _downloadService: DownloadService? = null

  private val onBackPressedCallback = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      if (bottomBarBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
        // 按下返回按钮时，如果底栏是展开的状态，就将底栏设置为折叠的状态
        mainViewModel.changeBottomBarExpand(false)
      }
    }
  }

  private var serviceBound = false

  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      val binder = service as DownloadService.DownloadBinder
      _downloadService = binder.getService()
      serviceBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      serviceBound = false
      _downloadService = null
    }
  }

  override fun init() {
    super.init()
    onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    bindService(Intent(this, DownloadService::class.java), serviceConnection, BIND_AUTO_CREATE)

    if (!XXPermissions.isGrantedPermissions(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
      MaterialAlertDialogBuilder(this)
        .setTitle("提示")
        .setMessage("需要存储权限，App才能将下载文件保存到外部储存，如果不授权将保存在Android文件夹内")
        .setPositiveButton("授权") { dialog, which ->
          XXPermissions.with(this).permission(
            Permission.MANAGE_EXTERNAL_STORAGE,
          )
            .request(null)
        }
        .setNegativeButton("取消", null)
        .show()
    }

    if (!XXPermissions.isGrantedPermissions(this, Permission.POST_NOTIFICATIONS)) {
      MaterialAlertDialogBuilder(this)
        .setTitle("展示通知")
        .setMessage("需要通知权限，App才能发送下载时的通知")
        .setPositiveButton("授权") { dialog, which ->
          XXPermissions.with(this).permission(
            Permission.POST_NOTIFICATIONS
          ).request(null)
        }
        .setNegativeButton("取消", null)
        .show()
    }

    checkUpdate()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val navHostFragment = supportFragmentManager
      .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

    val navController = navHostFragment.navController

    windowInsetsController = WindowCompat.getInsetsController(window, binding.root)

    navController.addOnDestinationChangedListener { controller, destination, arguments ->
      mainViewModel.changeBottomBarVisible(destination.shouldShowBottomNav())
    }
  }

  override fun onClick(v: View) {
    super.onClick(v)
    when (v.id) {
      R.id.play_bar -> {
        mainViewModel.changeBottomBarExpand(true)
      }

      R.id.btn_close -> {
        mainViewModel.changeBottomBarExpand(false)
      }

      R.id.btn_play -> {
        mainViewModel.updateIsPlaying()
      }

      R.id.btn_favorite -> {
        mainViewModel.addOrRemoveFavorite()
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun initView() {

    val titles = arrayOf("歌曲", "歌词")
    val fragments = listOf(MusicPlayFragment(), LyricsFragment())
    val vpMusic = binding.vpMusic

    vpMusic.apply {
      offscreenPageLimit = 2
      adapter = FragmentAdapter(fragments)
      fixNestedScroll()
    }

    TabLayoutMediator(binding.tab, vpMusic) { tab, position ->
      tab.text = titles[position]
    }.attach()

    val bottomBar = binding.bottomBar
    bottomBarBehavior = BottomSheetBehavior.from(bottomBar)
    ViewCompat.setOnApplyWindowInsetsListener(bottomBar) { v, insets ->
      val navigationBarHeight =
        insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
      val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
      bottomBarBehavior.peekHeight = bottomBarHeight + navigationBarHeight
      binding.navHostFragment.updatePadding(bottom = bottomBarBehavior.peekHeight)
      val lp = binding.mainPlayer.layoutParams as ViewGroup.MarginLayoutParams
      lp.topMargin = -(bottomBarHeight - statusBarHeight - 10.dp)
      insets
    }

    bottomBarBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
      override fun onSlide(view: View, offset: Float) {
        binding.mainPlayer.translationZ = if (offset == 0f) -10f else 10f
        binding.mainPlayer.alpha = offset
        binding.playBar.alpha = 1 - offset
      }

      override fun onStateChanged(v: View, state: Int) {
        // 如果是展开的状态
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
          // 同步状态给 ViewModel
          mainViewModel.changeBottomBarExpand(true)
          // 开启返回监听
          onBackPressedCallback.isEnabled = true
          // 如果当前不是亮色状态栏
          if (!windowInsetsController.isAppearanceLightStatusBars) {
            // 设置亮色状态栏
            windowInsetsController.isAppearanceLightStatusBars = true
            isLightStatusBar = false
          } else {
            isLightStatusBar = true
          }
        } else if (state == BottomSheetBehavior.STATE_COLLAPSED) {
          mainViewModel.changeBottomBarExpand(false)
          onBackPressedCallback.isEnabled = false
          if (isLightStatusBar != windowInsetsController.isAppearanceLightStatusBars) {
            windowInsetsController.isAppearanceLightStatusBars = isLightStatusBar
          }
        }
      }
    })
  }

  override fun initData() {
    binding.m = mainViewModel
    binding.lifecycleOwner = this

    lifecycleScope.launch {
      // 监听底栏的展开与折叠
      mainViewModel.bottomBarUIState.collect { uIState ->
        // 修改底栏的状态
        bottomBarBehavior.state =
          if (uIState.isExpanded) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
      }
    }
  }

  private lateinit var updateDialog: AlertDialog

  private fun checkUpdate() {
    scopeNet {
      val updateModel = Get<UpdateModel?>(Api.CHECK_UPDATE) {
        addQuery("channel", "music")
        addQuery("versionCode", AppUtils.getAppVersionCode())
      }.await()
      if (updateModel == null) {
        // toast("已是最新版本")
        return@scopeNet
      }
      val downloadBtnListener = DialogInterface.OnClickListener { d, which ->
        toast("正在后台下载...")
        val downloadButton = updateDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        scope {
          val downloadFile = Get<File>(updateModel.url) {
            setDownloadDir(externalCacheDir!!)
            setDownloadFileName(getString(R.string.app_name) + updateModel.versionName + ".apk")
            addDownloadListener(object : ProgressListener() {
              override fun onProgress(p: Progress) {
                if (p.finish) {
                  downloadButton?.text = "安装中"
                } else {
                  downloadButton?.text = "下载(${p.progress()})"
                }
              }
            })
          }.await()

          installUpdate(downloadFile)
        }
      }

      // 发现新版本
      updateDialog = MaterialAlertDialogBuilder(this@MainActivity)
        .setTitle("发现新版本")
        .setMessage("发现新版本需要更新，可下载后进行安装更新\n\n更新内容: ${updateModel.content}")
        .setNegativeButton("取消", null)
        .setPositiveButton("下载", downloadBtnListener)
        .show()
    }
  }

  private fun installUpdate(downloadFile: File) {
    if (XXPermissions.isGrantedPermissions(
        this@MainActivity,
        Permission.REQUEST_INSTALL_PACKAGES
      )
    ) {
      toast("正在跳转安装...")
      AppUtils.installApp(downloadFile)
    } else {
      toast("请授权安装权限")
      XXPermissions.with(this@MainActivity)
        .permission(Permission.REQUEST_INSTALL_PACKAGES)
        .request { permissions, allGranted ->
          if (allGranted) {
            toast("正在跳转安装")
            AppUtils.installApp(downloadFile)
          } else {
            toast("安装权限被拒绝")
          }
        }
    }
  }
}
