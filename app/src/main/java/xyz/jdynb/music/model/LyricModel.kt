package xyz.jdynb.music.model

import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport

data class LyricModel(
  @Column(nullable = false, unique = true)
  val musicId: Long,

  val content: String = "",
): LitePalSupport()