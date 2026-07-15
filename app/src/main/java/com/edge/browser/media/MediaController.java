package com.edge.browser.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class MediaController {

    private static MediaController instance;
    private MediaSession mediaSession;
    private boolean isPlaying = false;
    private String currentTitle = "";
    private String currentArtist = "";
    private final Map<String, Float> tabVolumes = new HashMap<>();
    private MediaListener listener;

    public interface MediaListener {
        void onPlay();
        void onPause();
        void onSkipToNext();
        void onSkipToPrevious();
        void onSeekTo(long position);
        void onVolumeChanged(float volume);
    }

    private MediaController() {}

    public static synchronized MediaController getInstance() {
        if (instance == null) {
            instance = new MediaController();
        }
        return instance;
    }

    public void init(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = new MediaSession(context, "EdgeBrowserMedia");

            mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

            PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                            PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP |
                            PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                            PlaybackState.ACTION_SEEK_TO);

            mediaSession.setPlaybackState(stateBuilder.build());

            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    isPlaying = true;
                    updatePlaybackState();
                    if (listener != null) listener.onPlay();
                }

                @Override
                public void onPause() {
                    isPlaying = false;
                    updatePlaybackState();
                    if (listener != null) listener.onPause();
                }

                @Override
                public void onSkipToNext() {
                    if (listener != null) listener.onSkipToNext();
                }

                @Override
                public void onSkipToPrevious() {
                    if (listener != null) listener.onSkipToPrevious();
                }

                @Override
                public void onSeekTo(long pos) {
                    if (listener != null) listener.onSeekTo(pos);
                }
            });

            mediaSession.setActive(true);
        }
    }

    public void setListener(MediaListener listener) {
        this.listener = listener;
    }

    private void updatePlaybackState() {
        if (mediaSession != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int state = isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
            PlaybackState.Builder builder = new PlaybackState.Builder()
                    .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f);
            mediaSession.setPlaybackState(builder.build());
        }
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        updatePlaybackState();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setTabVolume(String tabId, float volume) {
        tabVolumes.put(tabId, volume);
    }

    public float getTabVolume(String tabId) {
        Float vol = tabVolumes.get(tabId);
        return vol != null ? vol : 1.0f;
    }

    public void muteTab(String tabId) {
        tabVolumes.put(tabId, 0.0f);
    }

    public void unmuteTab(String tabId) {
        tabVolumes.put(tabId, 1.0f);
    }

    public void release() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
    }
}