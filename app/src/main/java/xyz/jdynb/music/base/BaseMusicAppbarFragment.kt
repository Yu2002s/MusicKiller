package xyz.jdynb.music.base

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import xyz.jdynb.music.R
import xyz.jdynb.music.databinding.FragmentBaseBinding
import xyz.jdynb.music.databinding.LayoutScrollviewBinding
import kotlin.math.abs

/**
 * 导航基类（包含Appbar）
 */
abstract class BaseMusicAppbarFragment<V : ViewDataBinding>(@LayoutRes contentLayoutId: Int = 0) :
  BaseMusicNavFragment<V>(contentLayoutId), OnClickListener {

  var contentView: View? = null

  override val binding: V
    get() = DataBindingUtil.bind<V>(contentView!!)!!

  lateinit var baseContentBinding: FragmentBaseBinding

  private var isExpandedAppBar = true

  /**
   * 是否自动添加滚动视图
   */
  protected open fun isAddScrollView() = true

  protected open fun getAppbarContent(inflater: LayoutInflater): View? = null

  override fun getMusicModels(): List<Any?>? {
    return super.getMusicModels()
  }

  override fun onClick(v: View) {}

  fun setTitle(title: CharSequence) {
    if (::baseContentBinding.isInitialized)
      baseContentBinding.collapsingToolbarLayout.title = title
  }

  fun setSubTitle(subTitle: CharSequence) {
    if (::baseContentBinding.isInitialized)
      baseContentBinding.collapsingToolbarLayout.subtitle = subTitle
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    baseContentBinding = FragmentBaseBinding.inflate(inflater, container, false)
    if (isAddScrollView()) {
      val layoutScrollviewBinding =
        LayoutScrollviewBinding.inflate(inflater, baseContentBinding.root, false)
      baseContentBinding.root.addView(layoutScrollviewBinding.root)
      contentView = super.onCreateView(inflater, layoutScrollviewBinding.root, savedInstanceState)
      layoutScrollviewBinding.root.addView(contentView!!)
    } else {
      contentView = super.onCreateView(inflater, baseContentBinding.root, savedInstanceState)
      baseContentBinding.root.addView(contentView!!)
    }
    getAppbarContent(inflater)?.let {
      baseContentBinding.appbar.addView(it)
    }
    return baseContentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setAppbar(baseContentBinding.toolbar, baseContentBinding.collapsingToolbarLayout)
    baseContentBinding.collapsingToolbarLayout.title = navController.currentDestination?.label
      ?: getString(R.string.app_name)

    baseContentBinding.appbar.setExpanded(isExpandedAppBar, false)
    baseContentBinding.appbar.addOnOffsetChangedListener { appBar, offset ->
      if (abs(offset) == appBar.totalScrollRange) {
        isExpandedAppBar = false
      } else if (offset == 0) {
        isExpandedAppBar = true
      }
    }
    super.onViewCreated(view, savedInstanceState)
  }

}