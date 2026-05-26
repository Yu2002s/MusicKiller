package xyz.jdynb.music.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.drake.engine.utils.AppUtils
import com.drake.net.Get
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.utils.scope
import com.drake.net.utils.scopeNet
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import xyz.jdynb.music.R
import xyz.jdynb.music.config.Api
import xyz.jdynb.music.model.UpdateModel
import java.io.File

/**
 * App更新工具类
 */
object UpdateUtils {

  /**
   * 检查更新
   */
  @JvmStatic
   fun checkUpdate(context: Context, showToast: Boolean = false) {
    scopeNet {
      val updateModel = Get<UpdateModel?>(Api.CHECK_UPDATE) {
        addQuery("channel", "music")
        addQuery("versionCode", AppUtils.getAppVersionCode())
      }.await()
      if (updateModel == null) {
        if (showToast) {
          toast("已是最新版本")
        }
        return@scopeNet
      }

      val updateDialog = MaterialAlertDialogBuilder(context)
        .setTitle("发现新版本")
        .setMessage("发现新版本需要更新，可下载后进行安装更新\n\n更新内容: ${updateModel.content}")
        .setNegativeButton("取消", null)
        .create()

      val downloadBtnListener = DialogInterface.OnClickListener { d, which ->
        toast("正在后台下载...")
        val downloadButton = updateDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        scope {
          val downloadFile = Get<File>(updateModel.url) {
            setDownloadDir(context.externalCacheDir!!)
            setDownloadFileName(context.getString(R.string.app_name) + updateModel.versionName + ".apk")
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

          installUpdate(context, downloadFile)
        }
      }
      updateDialog.setButton(AlertDialog.BUTTON_POSITIVE, "下载", downloadBtnListener)
      // 发现新版本
      updateDialog.show()
    }
  }

  /**
   * 安装更新
   *
   * @param downloadFile 安装包文件
   */
  @JvmStatic
  fun installUpdate(context: Context, downloadFile: File) {
    if (XXPermissions.isGrantedPermissions(
        context,
        Permission.REQUEST_INSTALL_PACKAGES
      )
    ) {
      toast("正在跳转安装...")
      AppUtils.installApp(downloadFile)
    } else {
      toast("请授权安装权限")
      XXPermissions.with(context)
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