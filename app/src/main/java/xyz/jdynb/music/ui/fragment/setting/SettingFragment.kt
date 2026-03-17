package xyz.jdynb.music.ui.fragment.setting

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.bumptech.glide.Glide
import com.drake.engine.utils.FileUtils
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import org.litepal.LitePal
import org.litepal.extension.findAll
import xyz.jdynb.music.MusicKillerApplication
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicAppbarFragment
import xyz.jdynb.music.databinding.FragmentSettingBinding
import xyz.jdynb.music.model.FavoriteModel
import xyz.jdynb.music.utils.json
import java.io.File

class SettingFragment : BaseMusicAppbarFragment<FragmentSettingBinding>(R.layout.fragment_setting) {

  private var preferenceFragment: SettingPreferenceFragment? = null

  override fun initView() {
    if (preferenceFragment == null) {
      preferenceFragment = SettingPreferenceFragment()
      childFragmentManager.beginTransaction()
        .replace(binding.preferenceContainer.id, preferenceFragment!!)
        .commit()
    }
  }

  override fun initData() {
  }

  class SettingPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
      return when (preference.key) {
        "pref_gitee" -> {
          openUrl("https://gitee.com/jdy2002/music-killer")
          true
        }

        "pref_github" -> {
          openUrl("https://github.com/yu2002s/music-killer")
          true
        }

        "pref_author" -> {
          openUrl("https://www.jdynb.xyz")
          true
        }

        "pref_cache" -> {
          scopeDialog {
            withIO {
              Glide.get(requireContext()).clearDiskCache()
              requireContext().cacheDir.deleteRecursively()
              requireContext().externalCacheDir?.deleteRecursively()
            }
            toast("清理完成")
          }
          true
        }

        "pref_mp" -> {
          MaterialAlertDialogBuilder(requireContext())
            .setTitle("提示")
            .setMessage("微信搜索公众号: 冬日暖雨")
            .setPositiveButton("关闭", null)
            .show()
          true
        }

        "pref_backup" -> {
          XXPermissions.with(this)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .request(null)
          MaterialAlertDialogBuilder(requireContext())
            .setTitle("备份/恢复")
            .setMessage("备份到: /sdcard/Download/MusicKiller/backup")
            .setPositiveButton("备份") { dialog, which ->
              scopeDialog {
                withIO {
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
                }
              }
            }.setNegativeButton("恢复") { dialog, which ->

            }.setNeutralButton("关闭", null)
            .show()
          true
        }

        else -> super.onPreferenceTreeClick(preference)
      }
    }

    private fun openUrl(url: String) {
      val intent = Intent(Intent.ACTION_VIEW, url.toUri())
      startActivity(intent)
    }
  }

}