package xyz.jdynb.music.ui.fragment.play

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.models
import com.drake.engine.base.EngineBottomSheetDialogFragment
import com.drake.engine.utils.ScreenUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.launch
import xyz.jdynb.music.R
import xyz.jdynb.music.constants.IntentActions
import xyz.jdynb.music.constants.IntentExtras
import xyz.jdynb.music.databinding.FragmentPlayQueueBinding
import xyz.jdynb.music.databinding.ItemListMusicBinding
import xyz.jdynb.music.model.MusicModel
import xyz.jdynb.music.model.setupMusicRv
import xyz.jdynb.music.service.MusicService
import xyz.jdynb.music.ui.activity.MainViewModel
import xyz.jdynb.music.utils.getMusicInfo

class PlayQueueDialogFragment : EngineBottomSheetDialogFragment<FragmentPlayQueueBinding>(),
  Player.Listener {

  private lateinit var mediaController: MediaController

  private val mainViewModel by activityViewModels<MainViewModel>()

  private lateinit var musicReceiver: MusicBroadReceiver

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_play_queue, container, false)
  }

  override fun onStart() {
    super.onStart()
    setBackgroundTransparent()
    behavior.peekHeight = ScreenUtils.getScreenHeight() / 2
  }

  override fun initData() {
    val sessionToken = SessionToken(
      requireContext(),
      ComponentName(requireContext(), MusicService::class.java)
    )
    val controllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()
    controllerFuture.addListener(
      {
        mediaController = controllerFuture.get()
        mediaController.removeListener(this)
        mediaController.addListener(this)

        viewLifecycleOwner.lifecycleScope.launch {
          val list = mutableListOf<MusicModel>()
          for (i in 0 until mediaController.mediaItemCount) {
            val mediaItem = mediaController.getMediaItemAt(i)
            val musicModel = mediaItem.mediaMetadata.getMusicInfo() ?: MusicModel(name = "未知音乐")
            musicModel.isSelected = false
            list.add(musicModel)
          }
          list.onEach {
            it.isFavorite = mainViewModel.isFavoriteMusic(it)
          }

          binding.playListRv.apply {
            models = list
            val currentMediaItemIndex = mediaController.currentMediaItemIndex
            if (bindingAdapter.modelCount == 0 || currentMediaItemIndex < 0
              || currentMediaItemIndex >= bindingAdapter.modelCount
            ) {
              return@apply
            }
            bindingAdapter.setChecked(currentMediaItemIndex, true)
            scrollToPosition(currentMediaItemIndex)
          }
        }
      },
      MoreExecutors.directExecutor()
    )

    binding.playListRv
      .setupMusicRv(this, mainViewModel, onItemClick = { position, model ->
        if (model.isSelected) {
          return@setupMusicRv
        }
        mediaController.seekToDefaultPosition(position)
      })
      .apply {
        singleMode = true

        onCreate {
          getBinding<ItemListMusicBinding>().btnAdd.isVisible = false
        }

        onChecked { position, checked, allChecked ->
          val model = getModel<MusicModel>(position)
          model.isSelected = checked
        }
      }
  }

  override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    binding.playListRv.apply {
      bindingAdapter.setChecked(mediaController.currentMediaItemIndex, true)
      scrollToPosition(mediaController.currentMediaItemIndex)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    if (::mediaController.isInitialized)
      mediaController.removeListener(this)

    if (::musicReceiver.isInitialized)
      requireContext().unregisterReceiver(musicReceiver)
  }

  override fun initView() {
    binding.btnClose.setOnClickListener {
      findNavController().navigateUp()
    }

    binding.btnExpand.setOnClickListener {
      behavior.state = if (behavior.state != BottomSheetBehavior.STATE_EXPANDED)
        BottomSheetBehavior.STATE_EXPANDED
      else BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    musicReceiver = MusicBroadReceiver()
    ContextCompat.registerReceiver(
      requireContext(),
      musicReceiver,
      IntentFilter(IntentActions.FAVORITE),
      ContextCompat.RECEIVER_NOT_EXPORTED
    )
  }


  private inner class MusicBroadReceiver : BroadcastReceiver() {

    @Suppress("UNCHECKED_CAST")
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        IntentActions.FAVORITE -> {
          val musicId = intent.getLongExtra(IntentExtras.MUSIC_ID, -1L)
          val isFavorite = intent.getBooleanExtra(IntentExtras.FAVORITE, false)
          val models = binding.playListRv.models as? List<MusicModel> ?: return
          models.forEach {
            if (it.id == musicId) {
              it.isFavorite = isFavorite
              return@forEach
            }
          }
        }
      }
    }

  }
}