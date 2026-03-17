package xyz.jdynb.music.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateModel(
  val versionName: String = "",
  val versionCode: Long = 0,
  val url: String = "",
  val content: String = "",
  val updateTime: String = "",
)
