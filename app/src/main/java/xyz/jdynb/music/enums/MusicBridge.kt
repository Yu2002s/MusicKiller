package xyz.jdynb.music.enums

/**
 * 音质
 */
enum class MusicBridge(val level: String) {
  MP3_128K("128kmp3"),
  MP3_192K("192kmp3"),
  MP3_320K("320kmp3"),
  FLAC_2000K("2000kflac");

  companion object {

    @JvmStatic
    fun getBridgeForLevel(level: String): MusicBridge {
      for (bridge in entries) {
        if (bridge.level == level) {
          return bridge
        }
      }
      return MP3_128K
    }
  }
}