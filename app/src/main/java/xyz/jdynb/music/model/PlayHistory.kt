package xyz.jdynb.music.model

import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport

data class PlayHistory(
  @Column(unique = true, nullable = false)
  val musicId: Long = 0L,
  val name: String = "",
  val cover: String = "",
  val coverLarge: String = "",
  val artist: String = "",
  @Column(defaultValue = "0")
  val artistId: Long = 0L,
  @Column(defaultValue = "0")
  val duration: Long = 0L,
  @Column(defaultValue = "true")
  val hasLossless: Boolean = true,
  val releaseDate: String = "",
  var album: String = "",
  @Column(defaultValue = "0")
  var albumId: Long = 0,
  val createTime: Long = System.currentTimeMillis(),
  val updateTime: Long = System.currentTimeMillis(),
): LitePalSupport() {
  val id: Long = 0L

  fun toMusicModel() = MusicModel(
    id = musicId,
    name = name,
    pic = cover,
    pic120 = coverLarge,
    artist = artist,
    artistId = artistId,
    duration = duration,
    hasLossless = hasLossless,
    releaseDate = releaseDate,
    album = album,
    albumId = albumId,
  )

  companion object {

    @JvmStatic
    fun from(musicModel: MusicModel) = PlayHistory(
      musicId = musicModel.id,
      name = musicModel.name,
      cover = musicModel.pic,
      coverLarge = musicModel.pic120,
      artist = musicModel.artist,
      artistId = musicModel.artistId,
      duration = musicModel.duration,
      hasLossless = musicModel.hasLossless,
      releaseDate = musicModel.releaseDate,
      album = musicModel.album,
      albumId = musicModel.albumId,
      updateTime = System.currentTimeMillis(),
    )
  }
}