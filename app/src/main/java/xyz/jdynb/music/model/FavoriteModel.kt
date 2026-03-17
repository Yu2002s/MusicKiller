package xyz.jdynb.music.model

import androidx.databinding.PropertyChangeRegistry
import com.drake.engine.databinding.ObservableImpl
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport

/**
 * 我的收藏
 */
@Serializable
data class FavoriteModel(

  /**
   * 音乐 id (歌曲和歌单)
   */
  @Column(nullable = false, unique = true)
  val musicId: Long = 0,

  /**
   * 名称
   */
  val name: String = "",

  /**
   * 作者
   */
  val author: String = "",

  /**
   * 封面
   */
  val cover: String = "",

  /**
   * 时长
   */
  val duration: Long = 0,

  /**
   * 类型
   */
  val type: Int = TYPE_SONG,

  /**
   * 作者 id
   */
  val authorId: Long = 0,

  /**
   * 是否有无损音质
   */
  var hasLossless: Boolean = true,

  /**
   * 创建时间
   */
  val createAt: Long = System.currentTimeMillis(),
) : LitePalSupport(), ObservableImpl {

  @Contextual
  override val registry: PropertyChangeRegistry = PropertyChangeRegistry()

  /**
   * 唯一 id
   */
  val id: Long = 0

  companion object {

    /**
     * 歌曲
     */
    const val TYPE_SONG = 0

    /**
     * 歌单
     */
    const val TYPE_PLAYLIST = 1

  }

  constructor(musicModel: MusicModel) : this(
    name = musicModel.name,
    musicId = musicModel.id,
    author = musicModel.artist,
    cover = musicModel.pic.ifEmpty { musicModel.pic120 },
    duration = musicModel.duration,
    authorId = musicModel.artistId,
    hasLossless = musicModel.hasLossless,
    type = TYPE_SONG // 歌曲
  )

  constructor(playListModel: PlayListModel) : this(
    name = playListModel.name,
    musicId = playListModel.id,
    author = playListModel.uname,
    cover = playListModel.img,
    type = TYPE_PLAYLIST
  )

}
