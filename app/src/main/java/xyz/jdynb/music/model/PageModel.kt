package xyz.jdynb.music.model

import android.os.Bundle
import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

data class PageModel<F: Fragment>(
  val title: String,
  val fragment: KClass<out F>,
  val args: Bundle? = null
)