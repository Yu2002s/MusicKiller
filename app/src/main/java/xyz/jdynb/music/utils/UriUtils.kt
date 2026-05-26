package xyz.jdynb.music.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.net.toUri
import xyz.jdynb.music.MusicKillerApplication

object UriUtils {

  @JvmStatic
  fun Uri?.getFilePath(): String? {
    if (this == null) return null

    val context = MusicKillerApplication.context

    // 处理 file:// 协议的 Uri
    if (scheme == "file") {
      return path
    }

    // 处理 content:// 协议的 Uri
    if (scheme == "content") {
      // 处理树形URI（文件夹路径）
      if (this.toString().contains("/tree/")) {
        return getTreeUriPath(this)
      }
      
      // 处理 DocumentsProvider
      if (DocumentsContract.isDocumentUri(context, this)) {
        val docId = DocumentsContract.getDocumentId(this)
    
        when {
          // 处理 ExternalStorageProvider
          isExternalStorageDocument(this) -> {
            val split = docId.split(":")
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
              return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
          }
          // 处理 DownloadsProvider
          isDownloadsDocument(this) -> {
            val id = docId.replaceFirst("^raw:".toRegex(), "")
            val contentUri = ContentUris.withAppendedId(
              "content://downloads/public_downloads".toUri(),
              id.toLongOrNull() ?: return null
            )
            return getDataColumn(context, contentUri, null, null)
          }
          // 处理 MediaProvider
          isMediaDocument(this) -> {
            val split = docId.split(":")
            val type = split[0]
            var contentUri: Uri? = null
            when (type) {
              "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
              "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
              "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            if (contentUri != null) {
              val selection = "_id=?"
              val selectionArgs = arrayOf(split[1])
              return getDataColumn(context, contentUri, selection, selectionArgs)
            }
          }
              
          else -> return getDataColumn(context, this, null, null)
        }
      } else {
        // 处理一般的 content:// Uri
        return getDataColumn(context, this, null, null)
      }
    }

    return null
  }

  /**
   * 获取树形URI的路径（文件夹路径）
   */
  private fun getTreeUriPath(uri: Uri): String? {
    val treeUriPath = uri.toString()
    // 提取 tree/ 后面的部分
    val treeIndex = treeUriPath.indexOf("/tree/")
    if (treeIndex == -1) return null
    
    // 获取编码后的路径部分
    val encodedPath = treeUriPath.substring(treeIndex + 6) // 跳过 "/tree/"
    
    // URL解码
    val decodedPath = try {
      java.net.URLDecoder.decode(encodedPath, "UTF-8")
    } catch (e: Exception) {
      encodedPath
    }
    
    // 解析 storageId:path 格式
    val colonIndex = decodedPath.indexOf(':')
    if (colonIndex == -1) return null
    
    val storageId = decodedPath.substring(0, colonIndex)
    val path = decodedPath.substring(colonIndex + 1)
    
    // 处理主存储
    return if ("primary".equals(storageId, ignoreCase = true)) {
      Environment.getExternalStorageDirectory().toString() + "/" + path
    } else {
      // 处理其他存储（如SD卡）
      "/storage/$storageId/$path"
    }
  }

  /**
   * 获取数据列（文件路径）
   */
  private fun getDataColumn(
    context: Context,
    uri: Uri,
    selection: String?,
    selectionArgs: Array<String>?
  ): String? {
    val column = "_data"
    val projection = arrayOf(column)

    try {
      context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(columnIndex)
          }
        }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    return null
  }

  /**
   * 判断是否是 ExternalStorageProvider 的 Uri
   */
  private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
  }

  /**
   * 判断是否是 DownloadsProvider 的 Uri
   */
  private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
  }

  /**
   * 判断是否是 MediaProvider 的 Uri
   */
  private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
  }
}