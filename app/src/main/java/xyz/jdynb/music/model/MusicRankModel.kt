package xyz.jdynb.music.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicRankModel(
    @SerialName("leader")
    var leader: String = "",
    @SerialName("num")
    var num: Int = 0,
    @SerialName("name")
    var name: String = "",
    @SerialName("pic")
    var pic: String = "",
    @SerialName("id")
    var id: Long = 0,
    @SerialName("pub")
    var pub: String = "",
    @SerialName("musicList")
    var musicList: List<MusicModel> = listOf()
)