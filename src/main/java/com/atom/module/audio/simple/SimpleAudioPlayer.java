package com.atom.module.audio.simple;


import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.POWER_SERVICE;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.Objects;


public class SimpleAudioPlayer implements LifecycleEventObserver {

    public final String TAG = SimpleAudioPlayer.class.getSimpleName();

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event.compareTo(Lifecycle.Event.ON_PAUSE) >= 0) {
            source.getLifecycle().removeObserver(this);
            stopPlay();
        }
    }

    public static class INSTANCE {
        public static final SimpleAudioPlayer sInstance = new SimpleAudioPlayer();
    }

    private static final int SPACE = 100;// 间隔取样时间

    private PowerManager powerManager;
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;

    private PlayCallback mPlayCallback;
    private String mAudioPath;
    private MediaPlayer mPlayer;
    private final Object mPlayerLock = new Object();
    private final Handler mHandler;

    private final Runnable mUpdatePlayStatusTimer = new Runnable() {
        @Override
        public void run() {
            if (mPlayCallback == null) return;
            synchronized (mPlayerLock) {
                if (mPlayer == null) return;
                mPlayCallback.progress(mPlayer.getCurrentPosition(), mPlayer.getDuration());
            }
            mHandler.postDelayed(this, SPACE);
        }
    };

    private SimpleAudioPlayer() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static SimpleAudioPlayer getInstance() {
        return INSTANCE.sInstance;
    }

    public void startPlay(@NonNull AppCompatActivity activity, String filePath, PlayCallback callback, int seekSec) {
        if (isPlaying()) {
            stopPlay();
        }
        boolean isHasFocus = SimpleAudioFocus.SINGLE.requestTheAudioFocus(this::stopPlay);
        if (!isHasFocus) return;
        this.mPlayCallback = callback;
        this.mAudioPath = filePath;
        try {
            synchronized (mPlayerLock) {
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(filePath);
                mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                mPlayer.setOnCompletionListener(mp -> {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            final int currentPosition = mp.getCurrentPosition();
                            final int duration = mp.getDuration();
                            if (mPlayCallback != null) {
                                mPlayCallback.progress(currentPosition, duration);
                            }
                            stopInternalPlay();
                            onPlayCompleted(duration > 0 && currentPosition == duration);
                        }
                    });
                });
                mPlayer.setOnPreparedListener(mp -> {
                    ready(activity);
                    if (mPlayCallback != null) {
                        mPlayCallback.prepare(mp.getDuration());
                    }
                    //如果小于这 毫秒容易出现播放一场,点击播放即返回完成且失败状态
                    if (seekSec > 0 && mp.getDuration() - seekSec > 1000) {
                        mPlayer.setOnSeekCompleteListener(this::realStart);
                        mPlayer.seekTo(seekSec);
                    } else {
                        realStart(mPlayer);
                    }
                });
                mPlayer.prepareAsync();
            }
        } catch (Exception e) {
            Log.e(TAG, "startPlay failed", e);
            stopInternalPlay();
            onPlayCompleted(false);
        }
    }

    public long getAudioLength(String audioPath) {
        MediaPlayer mediaPlayer = null;
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
            return mediaPlayer.getDuration(); //时长
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
            return 0;
        } finally {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    public void audioRecall(String filePath) {
        if (!isPlaying()) return;
        if (TextUtils.isEmpty(filePath)) return;
        if (TextUtils.isEmpty(mAudioPath)) return;
        if (!Objects.equals(mAudioPath, filePath)) return;
        stopPlay();
    }

    public String getAudioPath() {
        return mAudioPath;
    }

    private void ready(AppCompatActivity activity) {
        setScreenOff();
        SimpleAudioManager.choiceAudioModel(SimpleAudioManager.defaultOutput);
        activity.getLifecycle().addObserver(this);
    }

    private void realStart(MediaPlayer player) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.post(mUpdatePlayStatusTimer);
        player.start();
    }

    public void stopPlay() {
        Log.e(TAG, "停止播放语音");
        stopInternalPlay();
        onPlayCompleted(false);
        mPlayCallback = null;
    }

    public void pausePlay() {
        synchronized (mPlayerLock) {
            if (mPlayer == null) return;
            if (!isPlaying()) return;
            mPlayer.pause();
        }
    }

    public void resumePlay() {
        synchronized (mPlayerLock) {
            if (mPlayer == null) return;
            if (isPlaying()) return;
            mPlayer.start();
        }
    }

    public void resumePlayBySeek(int backMSec) {
        synchronized (mPlayerLock) {
            if (mPlayer == null) return;
            if (isPlaying()) return;
            final int c = mPlayer.getCurrentPosition() - backMSec;
            if (c == 0) {
                mPlayer.start();
            } else {
                mPlayer.setOnSeekCompleteListener(mp -> mPlayer.start());
                mPlayer.seekTo(c);
            }
        }
    }

    private void stopInternalPlay() {
        SimpleAudioFocus.SINGLE.releaseAudioFocusListener();
        mHandler.removeCallbacksAndMessages(null);
        synchronized (mPlayerLock) {
            if (mPlayer == null) {
                return;
            }
            mPlayer.release();
            mPlayer = null;
        }
    }

    public boolean isPlaying() {
        synchronized (mPlayerLock) {
            return mPlayer != null && mPlayer.isPlaying();
        }
    }

    private void onPlayCompleted(boolean success) {
        if (mPlayCallback != null) {
            mPlayCallback.onCompletion(success);
        }
        setScreenOn();
        // 停止播放立即将播放置为默认
        SimpleAudioManager.release();
    }

    public interface PlayCallback {

        void onCompletion(Boolean success);

        void progress(int curr, int max);

        void prepare(int duration);

    }

    private void setScreenOn() {
        if (wakeLock != null) {
            wakeLock.setReferenceCounted(false);
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void setScreenOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (wakeLock == null) {
                wakeLock = getPowerManager().newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, this.getClass().getSimpleName());
            }
            wakeLock.acquire(2 * 60 * 1000L /*10 minutes*/);
        }
    }

    private PowerManager getPowerManager() {
        if (powerManager == null) {
            powerManager = (PowerManager) SimpleAudioManager.getApplication().getSystemService(POWER_SERVICE);
        }
        return powerManager;
    }

}
