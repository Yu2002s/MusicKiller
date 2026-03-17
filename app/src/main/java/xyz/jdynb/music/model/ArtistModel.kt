package xyz.jdynb.music.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistModel(
    @SerialName("artistFans")
    var artistFans: Int = 0,
    @SerialName("albumNum")
    var albumNum: Int = 0,
    @SerialName("mvNum")
    var mvNum: Int = 0,
    @SerialName("pic")
    var pic: String = "",
    @SerialName("musicNum")
    var musicNum: Int = 0,
    @SerialName("pic120")
    var pic120: String = "",
    @SerialName("isStar")
    var isStar: Int = 0,
    // @SerialName("contentType")
    // var contentType: Any = Any(),
    @SerialName("aartist")
    var aartist: String = "",
    @SerialName("name")
    var name: String = "",
    @SerialName("pic70")
    var pic70: String = "",
    @SerialName("id")
    var id: Long = 0,
    @SerialName("pic300")
    var pic300: String = ""
): java.io.Serializable