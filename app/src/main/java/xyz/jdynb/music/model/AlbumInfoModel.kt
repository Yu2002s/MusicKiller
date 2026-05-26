package xyz.jdynb.music.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumInfoModel(
    @SerialName("name")
    var name: String = "",
    @SerialName("artist")
    var artist: String = "",
    @SerialName("releaseDate")
    var releaseDate: String = "",
    @SerialName("info")
    var info: String = "",
    @SerialName("lang")
    var lang: String = "",
    @SerialName("img")
    var img: String = "",
    @SerialName("musicList")
    var musicList: List<MusicModel> = listOf()
)