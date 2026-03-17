package xyz.jdynb.music.model

import org.litepal.crud.LitePalSupport

/**
 * 音乐历史记录
 */
data class MusicHistoryModel(
  /**
   * 标准图
   */
  val pic: String = "",
  /**
   * 大图
   */
  val picLarge: String = pic,
): LitePalSupport() {

  val id: Long = 0

}