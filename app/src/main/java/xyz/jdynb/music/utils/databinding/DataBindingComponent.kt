package xyz.jdynb.music.utils.databinding

import android.widget.ImageButton
import android.widget.TextView
import androidx.databinding.BindingAdapter
import xyz.jdynb.music.utils.formatDuration

object DataBindingComponent {

  @BindingAdapter("time")
  @JvmStatic
  fun setTime(textView: TextView, ms: Long) {
    textView.text = ms.formatDuration()
  }

}