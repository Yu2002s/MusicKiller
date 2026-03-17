package xyz.jdynb.music.model

import kotlinx.serialization.Serializable

/**
 * 查询歌单所需的参数
 */
@Serializable
data class QueryPlayListParams(
  /**
   * 查询标识
   */
  val id: String = "",
  /**
   * 页码
   */
  val pageNo: Int = 1,
  /**
   * pageSize
   */
  val pageSize: Int = 6,
  /**
   * 排序规则
   */
  val order: String = "",
  /**
   * 歌单 id
   */
  val pid: Long = 0,
)
