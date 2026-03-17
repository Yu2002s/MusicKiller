package xyz.jdynb.music.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayListTagModel(
  /**
   * 歌单标签 id
   */
  val id: String = "",
  /**
   * 歌单标签名称
   */
  val name: String = "",

  /**
   * 子标签
   */
  val data: List<PlayListTagModel> = listOf()
): java.io.Serializable
