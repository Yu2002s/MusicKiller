package xyz.jdynb.music.ui.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
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
import com.drake.engine.utils.ScreenUtils
import com.drake.engine.utils.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.config.shouldShowBottomNav
import xyz.jdynb.music.databinding.ActivityMainBinding
import xyz.jdynb.music.download.DownloadService
import xyz.jdynb.music.ui.fragment.play.LyricsFragment
import xyz.jdynb.music.ui.fragment.play.MusicPlayFragment
import xyz.jdynb.music.utils.UpdateUtils
import xyz.jdynb.music.utils.fixNestedScroll

class MainActivity : EngineActivity<ActivityMainBinding>(R.layout.activity_main), Player.Listener {

  private val mainViewModel by viewModels<MainViewModel>()

  private lateinit var bottomBarBehavior: BottomSheetBehavior<LinearLayout>

  private lateinit var windowInsetsController: WindowInsetsControllerCompat

  /**
   * 是否是亮色状态栏
   */
  private var isLightStatusBar = true

  /**
   * 底栏高度
   */
  private val bottomBarHeight = 68.dp

  var _downloadService: DownloadService? = null

  /**
   * 返回监听
   */
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
      // 连接到下载服务
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

    // 添加返回监听
    onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    // 绑定下载服务
    bindService(Intent(this, DownloadService::class.java), serviceConnection, BIND_AUTO_CREATE)

    // 检查权限
    checkPermissions()

    // 检查更新
    UpdateUtils.checkUpdate(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    initWindow()
    initNavController()
  }

  override fun onClick(v: View) {
    super.onClick(v)
    when (v.id) {
      // 播放底栏
      R.id.play_bar -> {
        mainViewModel.changeBottomBarExpand(true)
      }

      // 播放页关闭按钮
      R.id.btn_close -> {
        mainViewModel.changeBottomBarExpand(false)
      }

      // 播放按钮
      R.id.btn_play -> {
        mainViewModel.updateIsPlaying()
      }

      // 收藏按钮
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
      offscreenPageLimit = 1
      adapter = FragmentAdapter(fragments)
      fixNestedScroll()
    }

    // tab 和 vp 联动
    TabLayoutMediator(binding.tab, vpMusic) { tab, position ->
      tab.text = titles[position]
    }.attach()

    val bottomBar = binding.bottomBar
    bottomBarBehavior = BottomSheetBehavior.from(bottomBar)
    // 播放底栏适配导航栏
    ViewCompat.setOnApplyWindowInsetsListener(bottomBar) { _, insets ->
      // 计算导航栏高度
      val navigationBarHeight =
        insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
      // 计算状态栏高度
      val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
      // 折叠peek高度 = 底栏高度 + 导航栏高度，解决导航栏遮挡
      bottomBarBehavior.peekHeight = bottomBarHeight + navigationBarHeight
      // 防止内容区域被底栏遮挡
      binding.navHostFragment.updatePadding(bottom = bottomBarBehavior.peekHeight)
      val lp = binding.mainPlayer.layoutParams as ViewGroup.MarginLayoutParams
      // 顶部 marginTop 避免状态栏遮挡内容
      lp.topMargin = -(bottomBarHeight - statusBarHeight - 10.dp)
      insets
    }

    bottomBarBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
      override fun onSlide(view: View, offset: Float) {
        // 设置透明度
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
          // 折叠状态
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

  private fun initWindow() {
    // 沉浸式
    WindowCompat.setDecorFitsSystemWindows(window, false)

    windowInsetsController = WindowCompat.getInsetsController(window, binding.root)

    // 如果是横屏的话进行适配
    if (ScreenUtils.isLandscape()) {
      // 全屏
      window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
      // 隐藏系统栏
      windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
      // 设置系统栏显示行为
      windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }

  /**
   * 初始化导航控制器
   */
  private fun initNavController() {
    val navHostFragment = supportFragmentManager
      .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

    val navController = navHostFragment.navController

    // 添加导航事件监听
    navController.addOnDestinationChangedListener { controller, destination, arguments ->
      // 判断这个目标导航能否显示底栏
      mainViewModel.changeBottomBarVisible(destination.shouldShowBottomNav())
    }
  }

  /**
   * 检查权限
   */
  private fun checkPermissions() {
    if (!XXPermissions.isGrantedPermissions(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
      MaterialAlertDialogBuilder(this)
        .setTitle("提示")
        .setMessage("需要存储权限，App才能将下载文件保存到外部储存，如果不授权将保存在Android文件夹内")
        .setPositiveButton("授权") { _, _ ->
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
        .setPositiveButton("授权") { _, _ ->
          XXPermissions.with(this).permission(
            Permission.POST_NOTIFICATIONS
          ).request(null)
        }
        .setNegativeButton("取消", null)
        .show()
    }
  }
}
