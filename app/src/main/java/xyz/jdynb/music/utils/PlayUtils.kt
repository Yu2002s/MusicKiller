package xyz.jdynb.music.utils

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.os.persistableBundleOf
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import xyz.jdynb.music.model.MusicModel
import java.util.Locale

fun Player.playOrPause() {
  if (isPlaying) {
    pause()
  } else {
    play()
  }
}

// 转换为可读格式
fun Long.formatDuration(): String {
  val seconds = (this / 1000) % 60
  val minutes = (this / 1000 / 60) % 60
  val hours = this / 1000 / 3600

  return if (hours > 0) {
    String.format(Locale.CHINA, "%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format(Locale.CHINA, "%02d:%02d", minutes, seconds)
  }
}

fun MusicModel.toBundle(): Bundle {
  val fields = javaClass.declaredFields
  val bundle = Bundle()
  fields.forEach {
    it.isAccessible = true
    when (val value = it.get(this)) {
      is String -> bundle.putString(it.name, value)
      is Int -> bundle.putInt(it.name, value)
      is Long -> bundle.putLong(it.name, value)
      is Boolean -> bundle.putBoolean(it.name, value)
      is Float -> bundle.putFloat(it.name, value)
    }
  }
  return bundle
}

@Suppress("Deprecation")
fun MediaMetadata.getMusicInfo(): MusicModel? {
  extras ?: return null
  val clazz = MusicModel::class.java
  val fields = clazz.declaredFields
  val musicModel = MusicModel()
  fields.forEach {
    it.isAccessible = true
    it.set(musicModel, extras!!.get(it.name))
  }
  return musicModel
}