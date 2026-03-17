package xyz.jdynb.music.ui.activity

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.drake.engine.base.EngineToolbarActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.jdynb.music.MusicKillerApplication
import xyz.jdynb.music.R
import xyz.jdynb.music.databinding.ActivityCrashBinding
import xyz.jdynb.music.utils.startActivity

/**
 * App全局闪退处理
 */
class CrashActivity: EngineToolbarActivity<ActivityCrashBinding>(R.layout.activity_crash) {

  companion object {

    private const val PARAM_LOG = "log"

    private const val PARAM_ANR = "anr"

    fun actionStart(log: String, isAnr: Boolean = false) {
      startActivity<CrashActivity>(PARAM_LOG to log, PARAM_ANR to isAnr)
    }
  }

  @OptIn(UnstableApi::class)
  override fun initView() {
    title = "抱歉，系统崩溃了！"

    binding.restartApp.setOnClickListener {
      finish()
    }

    if (intent.getBooleanExtra(PARAM_ANR, false)) {
      MaterialAlertDialogBuilder(MusicKillerApplication.context)
        .setTitle("提示")
        .setMessage("App长时间没有响应，已触发保护机制，自动闪退。日志已保存至设备！点击重启按钮即可恢复运行！")
        .setPositiveButton("重启App") {dialog, which ->
          finish()
          startActivity<MainActivity>()
        }
        .setPositiveButton("取消", null)
        .show()
    }
  }

  override fun initData() {
    intent?.getStringExtra(PARAM_LOG)?.let { log ->
      binding.tvCrashContent.text = log
    }
  }
}
