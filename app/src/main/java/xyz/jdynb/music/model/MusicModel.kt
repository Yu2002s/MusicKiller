package xyz.jdynb.music.model


import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.BindingAdapter
import com.drake.brv.PageRefreshLayout
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import com.drake.net.utils.scope
import com.drake.tooltip.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import xyz.jdynb.music.BR
import xyz.jdynb.music.R
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.model.download.DownloadModel
import xyz.jdynb.music.ui.activity.MainViewModel
import kotlin.math.min

@Serializable
data class MusicModel(
  @SerialName("barrage")
  var barrage: String = "",
  @SerialName("artist")
  var artist: String = "",
  @SerialName("pic")
  var pic: String = "",
  // @SerialName("isstar")
  // var isstar: Int = 0,
  @SerialName("id")
  var id: Long = 0,
  @SerialName("duration")
  var duration: Long = 0,
  @SerialName("score100")
  var score100: String = "",
  @SerialName("ad_subtype")
  var adSubtype: String = "",
  @SerialName("content_type")
  var contentType: String = "",
  @SerialName("track")
  var track: Int = 0,
  @SerialName("hasLossless")
  var hasLossless: Boolean = true,
  @SerialName("hasMv")
  var hasMv: Int = 0,
  @SerialName("releaseDate")
  var releaseDate: String = "",
  @SerialName("album")
  var album: String = "",
  @SerialName("albumId")
  var albumId: Long = 0,
  @SerialName("artistId")
  var artistId: Long = 0,
  @SerialName("albumPic")
  var albumPic: String = "",
  @SerialName("songTimeMinutes")
  var songTimeMinutes: String = "",
  @SerialName("pic120")
  var pic120: String = "",
  @SerialName("name")
  var name: String = "",
  @SerialName("online")
  var online: Int = 0,
  @SerialName("tme_musician_adtype")
  var tmeMusicianAdtype: String = "",
  val fullName: String = "",
  var localPath: String = "",
) : BaseObservable() {

  @Transient
  var isPlayList = false

  @Transient
  @get:Bindable
  var checked: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.checked)
    }

  @Transient
  @get:Bindable
  var visibility: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.visibility)
    }

  val showPic get() = pic120.ifEmpty { pic.ifEmpty { R.mipmap.ic_launcher } }

  val showLargePic get() = pic.ifEmpty { R.mipmap.ic_launcher }

  val showName get() = fullName.ifEmpty { name }

  @get:Bindable
  var isSelected = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.selected)
    }


  @get:Bindable
  @Transient
  var currentPosition = 0L
    set(value) {
      field = value
      notifyPropertyChanged(BR.currentPosition)
    }

  @get:Bindable("currentPosition")
  val currentPositonInt get() = currentPosition.toInt()

  @get:Bindable
  @Transient
  var isFavorite = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.favorite)
    }

  companion object {

    fun from(downloadModel: DownloadModel): MusicModel {
      return MusicModel(
        pic = downloadModel.cover,
        name = downloadModel.name,
        id = downloadModel.musicId,
        artist = downloadModel.artist,
        localPath = downloadModel.localPath
      )
    }
  }
}

fun RecyclerView.setupMusicRv(
  fragment: BaseMusicNavFragment<*>,
  onCheckChange: (BindingAdapter.(Int, MusicModel, Boolean, Boolean) -> Unit)? = null,
  onToggleChange: (BindingAdapter.(Int, MusicModel, Boolean) -> Unit)? = null,
  onItemClick: (BindingAdapter.(Int, MusicModel) -> Unit)? = null
) = setupMusicRv(
  fragment = fragment,
  mainViewModel = fragment.mainViewModel,
  onCheckChange = onCheckChange,
  onToggleChange = onToggleChange,
  onItemClick = onItemClick
)

fun RecyclerView.setupMusicRv(
  fragment: Fragment,
  mainViewModel: MainViewModel,
  onCheckChange: (BindingAdapter.(Int, MusicModel, Boolean, Boolean) -> Unit)? = null,
  onToggleChange: (BindingAdapter.(Int, MusicModel, Boolean) -> Unit)? = null,
  onItemClick: (BindingAdapter.(Int, MusicModel) -> Unit)? = null
): BindingAdapter {
  return linear().setup {
    addType<MusicModel>(R.layout.item_list_music)

    onToggle { position, toggleMode, end ->
      // 刷新列表显示选择按钮
      val model = getModel<MusicModel>(position)
      model.visibility = toggleMode

      if (!toggleMode) {
        checkedAll(false)
      }

      onToggleChange?.invoke(this, position, model, toggleMode)
    }

    onChecked { position, checked, allChecked ->
      val model = getModel<MusicModel>(position)
      model.checked = checked
      onCheckChange?.invoke(this, position, model, checked, allChecked)
    }

    // 长按列表进入编辑模式
    R.id.item_music.onLongClick {
      if (onCheckChange == null) {
        // 没有开启选择模式
        return@onLongClick
      }
      if (!toggleMode) {
        toggle()
        setChecked(layoutPosition, true)
      }
    }

    onFastClick(R.id.cb, R.id.item_music) {
      val model = getModel<MusicModel>()

      // 如果当前未处于选择模式下 点击无效
      if (toggleMode) {
        val checked = model.checked
        setChecked(layoutPosition, !checked)
        return@onFastClick
      }
      if (onItemClick != null) {
        onItemClick(modelPosition, model)
        return@onFastClick
      }
      // 添加到播放器
      if (fragment is BaseMusicNavFragment<*>) {
        val filteredList =
          models!!.subList(modelPosition, modelPosition + min(20, modelCount - modelPosition))
        fragment.addPlaylist(filteredList, true)
      }
    }

    R.id.btn_favorite.onClick {
      val model = getModel<MusicModel>()
      if (model.isPlayList) {
        // 歌单
        scope {
          mainViewModel.removeFavorite(model.id, FavoriteModel.TYPE_PLAYLIST)
          mutable.removeAt(modelPosition)
          notifyItemRemoved(modelPosition)
        }
        return@onClick
      }
      mainViewModel.addOrRemoveFavorite()
    }

    R.id.btn_add.onClick {
      val model = getModel<MusicModel>()
      // 添加到播放器
      if (fragment is BaseMusicNavFragment<*>) {
        if (model.isPlayList) {
          // 歌单
          toast("歌单暂不支持直接添加到播放队列")
          return@onClick
        }
        fragment.addPlay(model, false)
      }
      toast("${model.name}已添加到播放队列")
    }
  }
}

fun PageRefreshLayout.createPage(total: Long, list: List<MusicModel>) =
  Page(total = total, data = list)

fun PageRefreshLayout.addData(
  fragment: BaseMusicNavFragment<*>,
  bindingAdapter: BindingAdapter?,
  loadData: suspend CoroutineScope.(PageRefreshLayout) -> List<MusicModel>
): PageRefreshLayout {
  onRefresh {
    if (index == 1) {
      if (fragment.musicList == null) {
        fragment.musicList = mutableListOf()
      } else {
        fragment.musicList!!.clear()
      }
    }
    scope {
      val data = loadData(this@onRefresh)
      addData(data = data.onEach {
        fragment.mainViewModel.getMusicModelState(it)
      }, adapter = bindingAdapter)

      fragment.musicList?.addAll(data)
    }
  }

  rv?.bindingAdapter ?: bindingAdapter?.let { adapter ->
    if (adapter.modelCount == 0) {
      showLoading()
    } else {
      adapter.models = fragment.musicList?.toMutableList()
    }
  }
  return this
}

fun PageRefreshLayout.addData(
  fragment: BaseMusicNavFragment<*>,
  loadData: suspend CoroutineScope.(PageRefreshLayout) -> List<MusicModel>
): PageRefreshLayout {
  return addData(fragment, null, loadData = loadData)
}

fun PageRefreshLayout.addPageData(
  fragment: BaseMusicNavFragment<*>,
  bindingAdapter: BindingAdapter?,
  loadData: suspend CoroutineScope.(PageRefreshLayout) -> Page<MusicModel>
): PageRefreshLayout {
  onRefresh {
    if (index == 1) {
      if (fragment.musicList == null) {
        fragment.musicList = mutableListOf()
      } else {
        fragment.musicList!!.clear()
      }
    }
    scope {
      val data = loadData(this@onRefresh)
      addData(data = data.data.onEach {
        fragment.mainViewModel.getMusicModelState(it)
      }, adapter = bindingAdapter, hasMore = {
        data.total > modelCount
      })
    }
  }

  rv?.bindingAdapter ?: bindingAdapter?.let { adapter ->
    if (adapter.modelCount == 0) {
      showLoading()
    } else {
      adapter.models = fragment.musicList?.toMutableList()
    }
  }
  return this
}

fun PageRefreshLayout.addPageData(
  fragment: BaseMusicNavFragment<*>,
  loadData: suspend CoroutineScope.(PageRefreshLayout) -> Page<MusicModel>
): PageRefreshLayout {
  return addPageData(fragment, null, loadData = loadData)
}
