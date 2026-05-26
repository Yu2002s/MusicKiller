package xyz.jdynb.music.ui.fragment.setting

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.bumptech.glide.Glide
import com.drake.engine.utils.AppUtils
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.launch
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import org.litepal.extension.findAll
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicAppbarFragment
import xyz.jdynb.music.config.SPConfig
import xyz.jdynb.music.databinding.FragmentSettingBinding
import xyz.jdynb.music.model.FavoriteModel
import xyz.jdynb.music.ui.activity.MainViewModel
import xyz.jdynb.music.utils.DownloadHelper
import xyz.jdynb.music.utils.SpUtils.put
import xyz.jdynb.music.utils.UpdateUtils
import xyz.jdynb.music.utils.UriUtils.getFilePath
import xyz.jdynb.music.utils.json
import xyz.jdynb.music.utils.toDate
import java.io.File

/**
 * 设置
 */
class SettingFragment : BaseMusicAppbarFragment<FragmentSettingBinding>(R.layout.fragment_setting) {

  init {
    enableMediaController = false
  }

  private var preferenceFragment: SettingPreferenceFragment? = null

  override fun initView() {
    if (preferenceFragment == null) {
      preferenceFragment = SettingPreferenceFragment(mainViewModel)
      childFragmentManager.beginTransaction()
        .replace(binding.preferenceContainer.id, preferenceFragment!!)
        .commit()
    }
  }

  override fun initData() {
  }

  class SettingPreferenceFragment(private val mainViewModel: MainViewModel) :
    PreferenceFragmentCompat() {

    companion object {

      const val TYPE_SELECT_DOWNLOAD_MUSIC = 0

      const val TYPE_RESTORE_BACKUP = 1

    }

    private lateinit var pathSelectLauncher: ActivityResultLauncher<Uri?>

    private lateinit var fileSelectLauncher: ActivityResultLauncher<String>

    private var currentSelectType = TYPE_SELECT_DOWNLOAD_MUSIC

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      val downloadMusicPathPreference = findPreference<Preference>(SPConfig.DOWNLOAD_MUSIC_PATH)
      // 下载音乐路径
      val downloadMusicPath = DownloadHelper.getDownloadDirectory(requireContext()).path
      downloadMusicPathPreference?.summary = downloadMusicPath
      // 下载歌词路径
      findPreference<Preference>(getString(R.string.download_lyric_path))
        ?.summary = "$downloadMusicPath/lyric"

      val versionPreference = findPreference<Preference>(getString(R.string.app_version))
      versionPreference?.summary =
        "${AppUtils.getAppVersionName()}(${AppUtils.getAppVersionCode()})"

      pathSelectLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
      ) { result ->
        val filePath = result.getFilePath() ?: return@registerForActivityResult
        when (currentSelectType) {
          TYPE_SELECT_DOWNLOAD_MUSIC -> {
            SPConfig.DOWNLOAD_MUSIC_PATH.put(filePath)
            downloadMusicPathPreference?.summary = filePath
            toast("下载音乐路径已更新")
          }
        }
      }

      fileSelectLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
      ) { result ->
        val filePath = result.getFilePath() ?: return@registerForActivityResult
        when (currentSelectType) {
          TYPE_RESTORE_BACKUP -> restoreBackup(filePath)
        }
      }

      val timeClosePreference = findPreference<Preference>(getString(R.string.timer_close))
      viewLifecycleOwner.lifecycleScope.launch {
        mainViewModel.autoCloseTime.collect {
          timeClosePreference?.summary = it?.toDate("mm:ss") ?: "已关闭"
        }
      }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {

      return when (preference.key) {
        getString(R.string.app_cache) -> {
          clearCache()
          true
        }

        getString(R.string.mp) -> {
          openMpDialog()
          true
        }

        getString(R.string.backup_restore) -> {
          backupFavorite()
          true
        }

        getString(R.string.download_music_path) -> {
          selectDownloadMusicPath()
          true
        }

        getString(R.string.app_version) -> {
          UpdateUtils.checkUpdate(requireContext(), true)
          true
        }

        getString(R.string.timer_close) -> {
          showTimerDialog()
          true
        }

        else -> super.onPreferenceTreeClick(preference)
      }
    }

    private fun restoreBackup(filePath: String) {
      scopeDialog {
        withIO {
          val backupContent = File(filePath).readText()
          val favoriteModels = json.decodeFromString<List<FavoriteModel>>(backupContent)
          LitePal.deleteAll<FavoriteModel>()
          LitePal.saveAll(favoriteModels)
        }
        toast("恢复备份完成")
      }
    }

    private fun selectDownloadMusicPath() {
      currentSelectType = TYPE_SELECT_DOWNLOAD_MUSIC
      XXPermissions.with(this)
        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
        .request(object : OnPermissionCallback {
          override fun onGranted(permissions: List<String>, all: Boolean) {
            if (all) {
              pathSelectLauncher.launch(null)
            }
          }

          override fun onDenied(permissions: List<String>, never: Boolean) {
            if (never) {
              toast("被永久拒绝授权，如需使用请在设置界面打开权限")
            }
          }
        })
    }

    private fun openMpDialog() {
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("提示")
        .setMessage("微信搜索公众号: 冬日暖雨")
        .setPositiveButton("关闭", null)
        .show()
    }

    private fun clearCache() {
      scopeDialog {
        withIO {
          Glide.get(requireContext()).clearDiskCache()
          requireContext().cacheDir.deleteRecursively()
          requireContext().externalCacheDir?.deleteRecursively()
        }
        toast("清理完成")
      }
    }

    private fun backupFavorite() {
      XXPermissions.with(this)
        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
        .request(null)
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("备份/恢复")
        .setMessage("备份到: ${getString(R.string.backup_path)}\n\n恢复将会覆盖原有的收藏音乐")
        .setPositiveButton("备份") { dialog, which ->
          scopeDialog {
            val backupFile = withIO {
              val jsonContent = json.encodeToString(LitePal.findAll<FavoriteModel>())
              val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MusicKiller/backup"
              )
              if (!file.exists()) {
                file.mkdirs()
              }
              val backup = File(file, "backup_${System.currentTimeMillis()}.json")
              backup.writeText(jsonContent)
              backup
            }
            toast("已备份到: ${backupFile.path}")
          }
        }.setNegativeButton("恢复") { dialog, which ->
          fileSelectLauncher.launch("application/json")
          currentSelectType = TYPE_RESTORE_BACKUP
        }.setNeutralButton("关闭", null)
        .show()
    }

    private fun showTimerDialog() {
      val items = arrayOf("关闭", /*"10秒",*/ "10分钟", "15分钟", "20分钟", "30分钟", "45分钟", "1小时")
      val timers = arrayOf<Long>(
        0,
        // 10 * 1000,
        10 * 1000 * 60,
        15 * 1000 * 60,
        20 * 1000 * 60,
        30 * 1000 * 60,
        45 * 1000 * 60,
        60 * 1000 * 60
      )
      var selectedTime = 0L
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("选择时间")
        .setSingleChoiceItems(items, 0) { dialog, which ->
          selectedTime = timers[which]
        }
        .setPositiveButton("保存") { dialog, which ->
          mainViewModel.setAutoCloseTime(selectedTime)
        }
        .setNegativeButton("取消", null)
        .show()
    }
  }

}