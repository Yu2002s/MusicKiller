package xyz.jdynb.music.service;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.ResolvingDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;

import com.drake.net.Net;
import com.drake.net.request.UrlRequest;
import com.drake.tooltip.ToastKt;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.jdynb.music.MusicKillerApplication;
import xyz.jdynb.music.R;
import xyz.jdynb.music.config.Api;
import xyz.jdynb.music.constants.IntentActions;
import xyz.jdynb.music.constants.IntentExtras;
import xyz.jdynb.music.model.FavoriteModel;
import xyz.jdynb.music.model.MusicModel;
import xyz.jdynb.music.model.PlayHistory;
import xyz.jdynb.music.model.PlayInfo;
import xyz.jdynb.music.ui.activity.MainActivity;
import xyz.jdynb.music.utils.PlayUtilsKt;
import xyz.jdynb.music.utils.converter.JavaSerializationConverter;

/**
 * 音乐服务
 */
public class MusicService extends MediaSessionService implements Player.Listener {

    private static final String TAG = "MusicService";

    // 自定义命令
    private static final String CUSTOM_COMMAND_TOGGLE_FAVORITE = "TOGGLE_FAVORITE";
    private static final String CUSTOM_COMMAND_SHOW_LYRICS = "SHOW_LYRICS";

    private ExoPlayer mExoPlayer;

    private MediaSession mediaSession;

    private final EventLogger mEventLogger = new EventLogger();

    // 收藏状态
    private boolean isFavorite = false;

    private MusicBroadcastReceiver musicBroadcastReceiver;

    @SuppressLint("UnsafeOptInUsageError")
    private class MediaSessionCallback implements MediaSession.Callback {

        @Override
        public MediaSession.ConnectionResult onConnect(MediaSession session, MediaSession.ControllerInfo controller) {
            // 创建可用的自定义命令
            SessionCommands sessionCommands = new SessionCommands.Builder()
                    .add(new SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
                    .add(new SessionCommand(CUSTOM_COMMAND_SHOW_LYRICS, Bundle.EMPTY))
                    .build();

            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .build();
        }

        @NonNull
        @Override
        public ListenableFuture<SessionResult> onCustomCommand(
                @NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull SessionCommand customCommand,
                @NonNull Bundle args) {

            // 处理自定义命令
            switch (customCommand.customAction) {
                case CUSTOM_COMMAND_TOGGLE_FAVORITE:
                    handleToggleFavorite();
                    break;
                case CUSTOM_COMMAND_SHOW_LYRICS:
                    handleShowLyrics();
                    break;
            }

            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initEXOPlayer();

        musicBroadcastReceiver = new MusicBroadcastReceiver();
        ContextCompat.registerReceiver(this, musicBroadcastReceiver,
                new IntentFilter("xyz.jdynb.music.favorite"), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        if (mediaSession == null) {
            initEXOPlayer();
        }
        return mediaSession;
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        if (mediaItem == null) {
            return;
        }

        MediaMetadata mediaMetadata = mediaItem.mediaMetadata;
        MusicModel model = PlayUtilsKt.getMusicInfo(mediaMetadata);

        if (model == null) {
            return;
        }

        if (model.isFavorite() != isFavorite) {
            isFavorite = model.isFavorite();
            setCustomLayout();
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void initEXOPlayer() {
        if (mExoPlayer != null) {
            destroyPlayer();
        }

        // 配置 HTTP 数据源工厂
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true);

        // 配置缓存数据源工厂，将 HTTP 数据源作为上游
        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(MusicKillerApplication.simpleCache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        // 自定义 DataSource.Factory，使用 DefaultDataSource 包装 CacheDataSource (作为 baseDataSource 用于 HTTP)
        DefaultMediaSourceFactory mediaSourceFactory = getDefaultMediaSourceFactory(cacheDataSourceFactory);

        mExoPlayer = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true) // 耳机拔出时停止播�?
                .setWakeMode(C.WAKE_MODE_NETWORK) // 保持唤醒
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

        mExoPlayer.removeListener(this);
        mExoPlayer.addListener(this);

        // mExoPlayer.setPlayWhenReady(true); // 设置准备后自动播放

        mediaSession = new MediaSession.Builder(this, mExoPlayer)
                .setCallback(new MediaSessionCallback())
                .setSessionActivity(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .build();

        // 设置自定义布局（通知栏按钮）
        setCustomLayout();

        mExoPlayer.removeAnalyticsListener(mEventLogger);
        mExoPlayer.addAnalyticsListener(mEventLogger);

       /* List<PlayHistory> histories = LitePal.order("updateTime desc").limit(20).find(PlayHistory.class);
        if (!histories.isEmpty()) {
            int size = Math.min(20, histories.size());
            List<PlayHistory> playList = histories.subList(0, size);
            List<MediaItem> mediaItems = playList.stream().map(new Function<PlayHistory, MediaItem>() {
                @Override
                public MediaItem apply(PlayHistory playHistory) {
                    playHistory.toMusicModel();
                    return null;
                }
            }).collect(Collectors.toList());
            mExoPlayer.setMediaItems();
        }*/

    }

    @OptIn(markerClass = UnstableApi.class)
    @NonNull
    private DefaultMediaSourceFactory getDefaultMediaSourceFactory(CacheDataSource.Factory cacheDataSourceFactory) {
        DataSource.Factory dataSourceFactory = new DataSource.Factory() {
            @NonNull
            @Override
            public DataSource createDataSource() {
                // DefaultDataSource 会自动处理 file, asset, content 等协议
                // 对于 http/https，它会使用我们传入的 baseDataSource (即 cacheDataSource)
                return new DefaultDataSource(MusicService.this, cacheDataSourceFactory.createDataSource());
            }
        };

        // 在上面包装 ResolvingDataSource.Factory，用于自定义 HTTP 解析
        ResolvingDataSource.Factory resolvingDataSourceFactory = new ResolvingDataSource.Factory(dataSourceFactory, new ApiUriResolve());
        return new DefaultMediaSourceFactory(resolvingDataSourceFactory);
    }

    /**
     * 设置通知栏自定义按钮布局
     */
    @SuppressWarnings("deprecation")
    private void setCustomLayout() {
        // 创建收藏按钮
        CommandButton favoriteButton = new CommandButton.Builder()
                .setIconResId(isFavorite ? R.drawable.baseline_favorite_24 : R.drawable.baseline_favorite_border_24)
                .setDisplayName(getString(R.string.favorite))
                .setSessionCommand(new SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
                .build();

        // 创建歌词按钮
        CommandButton lyricsButton = new CommandButton.Builder()
                .setIconResId(R.drawable.baseline_lyrics_24)
                .setDisplayName(getString(R.string.lyrics))
                .setSessionCommand(new SessionCommand(CUSTOM_COMMAND_SHOW_LYRICS, Bundle.EMPTY))
                .build();

        // 设置到MediaSession
        ImmutableList<CommandButton> customLayout = ImmutableList.of(
                favoriteButton,
                lyricsButton
        );

        mediaSession.setCustomLayout(customLayout);
    }

    /**
     * 处理收藏/取消收藏
     */
    private void handleToggleFavorite() {
        MediaItem currentMediaItem = mExoPlayer.getCurrentMediaItem();
        if (currentMediaItem == null) {
            return;
        }

        String musicId = currentMediaItem.mediaId;

        new Thread(() -> {
            FavoriteModel favoriteModel = LitePal.where("musicId = ?", musicId)
                    .findFirst(FavoriteModel.class);

            MediaMetadata mediaMetadata = currentMediaItem.mediaMetadata;

            if (favoriteModel == null) {
                MusicModel model = PlayUtilsKt.getMusicInfo(mediaMetadata);
                new FavoriteModel(model).save();
                isFavorite = true;
            } else {
                LitePal.deleteAll(FavoriteModel.class, "musicId = ?", musicId);
                isFavorite = false;
            }

            Log.d(TAG, "Toggle favorite: " + isFavorite);

            sendBroadcast(new Intent("xyz.jdynb.music.favorite")
                    .setPackage(getPackageName())
                    .putExtra("musicId", Long.parseLong(musicId))
                    .putExtra("isFavorite", isFavorite));
            // 更新通知栏按钮状态
            // new Handler(Looper.getMainLooper()).post(this::setCustomLayout);

        }).start();
    }

    /**
     * 处理显示歌词
     */
    private void handleShowLyrics() {
        Log.d(TAG, "Show lyrics clicked");

        // 创建Intent打开MainActivity并显示歌词
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("ACTION_SHOW_LYRICS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        ToastKt.toast("打开歌词");

        // TODO: 可以通过广播或其他方式通知Activity显示歌词界面
    }

    /**
     * 销毁播放器
     */
    private void destroyPlayer() {
        if (mExoPlayer != null && mExoPlayer.isCommandAvailable(Player.COMMAND_RELEASE)) {
            mExoPlayer.release();
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        mExoPlayer = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyPlayer();
        unregisterReceiver(musicBroadcastReceiver);
    }

    @UnstableApi
    private static class ApiUriResolve implements ResolvingDataSource.Resolver {

        @NonNull
        @Override
        public DataSpec resolveDataSpec(DataSpec dataSpec) throws IOException {

            if (!"http".equals(dataSpec.uri.getScheme())) {
                return dataSpec;
            }

            String id = dataSpec.uri.getQueryParameter("id");
            String bridge = dataSpec.uri.getQueryParameter("bridge");

            UrlRequest urlRequest = Net.get(Api.PLAY_INFO);
            urlRequest.setConverter(new JavaSerializationConverter());
            urlRequest.addQuery("id", id, false);
            urlRequest.addQuery("bridge", bridge, false);
            PlayInfo playInfo = urlRequest.execute(PlayInfo.class);

            Log.i(TAG, "playInfo: " + playInfo);

            return new DataSpec.Builder()
                    .setUri(playInfo.getUrl())
                    .setKey(id) // 使用 ID 作为缓存 Key
                    .build();
        }
    }

    public class MusicBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case IntentActions.FAVORITE:
                    MediaItem currentMediaItem = mExoPlayer.getCurrentMediaItem();
                    if (currentMediaItem == null) {
                        return;
                    }
                    long musicId = intent.getLongExtra(IntentExtras.MUSIC_ID, -1L);
                    boolean favorite = intent.getBooleanExtra(IntentExtras.FAVORITE, false);
                    long currentMusicId = Long.parseLong(currentMediaItem.mediaId);
                    Log.i(TAG, "onReceive: favorite: " + favorite + ", musicId: " + musicId);
                    if (currentMusicId == musicId) {
                        isFavorite = favorite;
                        setCustomLayout();
                    }
                    break;
            }
        }
    }
}
