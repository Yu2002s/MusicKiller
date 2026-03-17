package xyz.jdynb.music.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicRankMenuModel(
    @SerialName("name")
    var name: String = "",
    @SerialName("list")
    var list: List<MusicRankMenuItemModel> = listOf()
)

@Serializable
data class MusicRankMenuItemModel(
    @SerialName("sourceId")
    var sourceId: Long = 0L,
    @SerialName("intro")
    var intro: String = "",
    @SerialName("name")
    var name: String = "",
    @SerialName("id")
    var id: String = "",
    @SerialName("source")
    var source: String = "",
    @SerialName("pic")
    var pic: String = "",
    @SerialName("pub")
    var pub: String = ""
): java.io.Serializable