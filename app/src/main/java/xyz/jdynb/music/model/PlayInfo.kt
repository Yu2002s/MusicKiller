package xyz.jdynb.music.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayInfo(
    @SerialName("bitrate")
    var bitrate: Int = 0,
    @SerialName("duration")
    var duration: Int = 0,
    @SerialName("format")
    var format: String = "",
    @SerialName("musicId")
    var musicId: Long = 0,
    @SerialName("sig")
    var sig: String = "",
    @SerialName("source")
    var source: String = "",
    @SerialName("type")
    var type: Int = 0,
    @SerialName("url")
    var url: String = "",
    @SerialName("user")
    var user: String = ""
)