package xyz.jdynb.music.config

import xyz.jdynb.music.BuildConfig

object Api {
  val BASE_API: String = if (/*BuildConfig.DEBUG*/false) {
    "http://192.168.31.139:8080"
  } else {
    "http://47.103.74.108:8005"
  }
  // const val BASE_API = "http://192.168.1.42"

  const val HOME_DATA = "/home/data"

  const val CHECK_UPDATE = "/update/check"

  /**
   * 歌单标签列表
   */
  const val PLAYLIST_INDEX_TAGS = "/music/playlist/getIndexPlayListTags"

  /**
   * 获取推荐歌单列表
   */
  const val PLAYLIST_RECOMMEND = "/music/playlist/recommend"

  /**
   * 通过标签获取歌单列表
   */
  const val PLAYLIST_BY_TAG = "/music/playlist/getPlayListByTag"

  /**
   * 获取首页的排行榜数据
   */
  const val RANK_INDEX = "/music/rank/index"

  /**
   * 排行榜菜单
   */
  const val RANK_MENU = "/music/rank/menu"

  /**
   * 排行榜音乐列表
   */
  const val RANK_MUSIC_LIST = "/music/rank/getMusicList"

  /**
   * 获取歌单标签列表
   */
  const val PLAYLIST_TAGS = "/music/playlist/getPlayListTags"

  /**
   * 获取歌单分页列表
   */
  const val PLAYLIST_PAGE = "/music/playlist/page"

  /**
   * 获取标签的歌单列表
   */
  const val TAG_PLAYLIST_PAGE = "/music/playlist/getTagPlaylist"

  /**
   * 歌单信息
   */
  const val PLAYLIST_INFO = "/music/playlist/info"

  /**
   * 获取播放信息
   */
  const val PLAY_INFO = "/music/play/info"

  /**
   * 歌手列表
   */
  const val ARTIST_LIST = "/music/artist/list"

  const val ARTIST_MUSIC = "/music/artist/music"

  const val ARTIST_INFO = "/music/artist/info"

  /**
   * 搜索建议
   */
  const val SEARCH_KEYWORD = "/music/search/key"

  /**
   * 搜索歌曲
   */
  const val SEARCH = "/music/search"

  const val SEARCH_ALBUM = "/music/search/album"

  const val SEARCH_PLAYLIST = "/music/search/playlist"

  const val SEARCH_ARTIST = "/music/search/artist"

}