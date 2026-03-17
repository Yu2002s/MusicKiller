package xyz.jdynb.music.model

import kotlinx.serialization.Serializable

@Serializable
data class AlbumModel(
  var album: String = "",
  val artist: String = "",
  val albumInfo: String = "",
  val releaseDate: String = "",
  val albumId: Long = 0,
  val artistId: Long = 0,
  val pic: String = "",
  val lang: String = "",
)