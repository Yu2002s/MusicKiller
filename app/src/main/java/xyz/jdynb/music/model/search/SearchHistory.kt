package xyz.jdynb.music.model.search

import org.litepal.crud.LitePalSupport

/**
 * 搜索历史记录
 */
data class SearchHistory(
  /**
   * 搜索关键字
   */
  val name: String,
  /**
   * 更新时间
   */
  val updateAt: Long = System.currentTimeMillis(),
): LitePalSupport() {

  val id: Long = 0

}
