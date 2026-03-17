package xyz.jdynb.music.utils

import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.BindingAdapter
import com.drake.brv.PageRefreshLayout
import com.drake.brv.utils.bindingAdapter
import com.drake.net.utils.scope
import kotlinx.coroutines.CoroutineScope
import xyz.jdynb.music.base.BaseMusicNavFragment
import xyz.jdynb.music.model.MusicModel

fun <T> PageRefreshLayout.addWithData(
  data: List<T>?,
  adapter: BindingAdapter? = null,
  isEmpty: () -> Boolean = { data.isNullOrEmpty() },
  hasMore: BindingAdapter.() -> Boolean = { true },
): List<T>? {
  addData(data, adapter, isEmpty, hasMore)
  return data
}

inline fun <reified T> PageRefreshLayout.onLoad(
  fragment: BaseMusicNavFragment<*>,
  crossinline block: suspend CoroutineScope.(PageRefreshLayout) -> List<T>?
): PageRefreshLayout {
  onRefresh {
    if (index == 1) {
      // 第一页是进行清理
      if (fragment.musicList == null) {
        fragment.musicList = mutableListOf()
      } else {
        fragment.musicList!!.clear()
      }
    }
    fragment.pageNo = index
    scope {
      val data = block.invoke(this, this@onRefresh) ?: return@scope

      if (T::class == MusicModel::class) {
        data.onEach {
          it as MusicModel
          fragment.mainViewModel.getMusicModelState(it)
        }
      }
      @Suppress("UNCHECKED_CAST")
      fragment.musicList?.addAll(data as Collection<Any>)
    }
  }

  var bindingAdapter = rv?.bindingAdapter

  if (bindingAdapter == null) {
    // 反射方式获取到 RV 对象
    val refreshContentField = PageRefreshLayout::class.java.getDeclaredField("refreshContent")
    refreshContentField.isAccessible = true
    val refreshContent = refreshContentField.get(this)
    if (refreshContent is RecyclerView) {
      bindingAdapter = refreshContent.bindingAdapter
    }
  }

  bindingAdapter?.let { adapter ->
    if (fragment.musicList.isNullOrEmpty()) {
      showLoading()
    } else {
      index = fragment.pageNo
      adapter.models = fragment.musicList?.toMutableList()
    }
  }
  return this
}