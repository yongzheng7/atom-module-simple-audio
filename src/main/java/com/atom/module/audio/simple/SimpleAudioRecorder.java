package com.atom.module.audio.simple;


import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.atom.module.audio.simple.exception.SimpleAudioException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleAudioRecorder implements LifecycleEventObserver {

    public final String TAG = SimpleAudioRecorder.class.getSimpleName();

    public static class INSTANCE {
        public static final SimpleAudioRecorder sInstance = new SimpleAudioRecorder();
    }

    private static final int MIN_RECORD_DURATION = 1000;
    private static final double BASE = 1.0;
    public static final long MAX_LENGTH = 61;
    public static final long RECIPROCAL_LENGTH = 50;
    public static final long MAX_MILLISECOND = MAX_LENGTH * MIN_RECORD_DURATION;
    public static final long RECIPROCAL_MILLISECOND = RECIPROCAL_LENGTH * MIN_RECORD_DURATION;
    private static final int SPACE = 100;// 间隔取样时间

    private int recordTime = 0;
    private RecordCallback mRecordCallback;
    private AppCompatActivity appCompatActivity;

    private String mAudioRecordPath;
    private MediaRecorder mRecorder;
    private Handler mRecordHandler;
    private HandlerThread mThread;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService SINGLE = Executors.newSingleThreadExecutor();
    private final Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            if (mRecorder != null) {
                recordTime += SPACE;
                SINGLE.execute(() -> {
                    mMainHandler.post(() -> {
                        if (mRecordCallback != null) {
                            mRecordCallback.recordingWatch(recordTime);
                        }
                    });
                    double ratio = 0;
                    try {
                        ratio = mRecorder.getMaxAmplitude() / BASE;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    double db = 0;// 分贝
                    if (ratio > 1)
                        db = 20 * Math.log10(ratio);
                    final double finalDB = db;
                    if (mRecordCallback != null) {
                        mRecordCallback.recordingDB(finalDB);
                    }
                });
                final Handler recordHandler = recordHandler();
                if (recordHandler != null) {
                    recordHandler.postDelayed(this, SPACE);
                }
            }
        }
    };

    private SimpleAudioRecorder() {
        recordHandler();
    }

    public void release() {
        if (mRecordHandler != null) {
            mRecordHandler.removeCallbacksAndMessages(null);
            mRecordHandler = null;
        }
        if (mThread != null) {
            mThread.quitSafely();
            mThread = null;
        }
    }

    private Handler recordHandler() {
        if (mThread != null && mThread.isAlive()) {
            if (mRecordHandler != null) return mRecordHandler;
            mRecordHandler = new Handler(mThread.getLooper());
            return mRecordHandler;
        }
        mThread = new HandlerThread("Simple-Audio_Recorder");
        mThread.start();
        mRecordHandler = new Handler(mThread.getLooper());
        return mRecordHandler;
    }

    public static SimpleAudioRecorder getInstance() {
        return INSTANCE.sInstance;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event.compareTo(Lifecycle.Event.ON_PAUSE) >= 0) {
            source.getLifecycle().removeObserver(this);
            cancelRecord();
        }
    }

    public void startRecord(AppCompatActivity activity, @NonNull RecordCallback callback) {
        // TODO 没必要实现 只是为了暂停音频播放
        boolean isHasFocus = SimpleAudioFocus.SINGLE.requestTheAudioFocus(SimpleAudioFocus.SINGLE::releaseAudioFocusListener);
        if (!isHasFocus) return;
        // 提前检查麦克风占用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            AudioManager mAudioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (mAudioManager.getActiveRecordingConfigurations().size() > 0) {
                callback.onError(new SimpleAudioException("其他应用正在使用麦克风,请稍后"));
                return;
            }
        }
        mRecordCallback = callback;
        appCompatActivity = activity;
        try {
            final File file = new File(SimpleAudioManager.getApplication().getExternalCacheDir() ,"auto_" + System.currentTimeMillis() + ".m4a");
            // 文件的空判断
            final File parentFile = file.getParentFile();
            if (parentFile == null) {
                callback.onError(new SimpleAudioException("音频文件创建失败,请检查存储权限"));
                return;
            }
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            mAudioRecordPath = file.getAbsolutePath();
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // 使用mp4容器并且后缀改为.m4a，来兼容小程序的播放
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(mAudioRecordPath);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recordTime = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mRecorder.registerAudioRecordingCallback(mMainHandler::post, new AudioManager.AudioRecordingCallback() {
                    @Override
                    public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                        super.onRecordingConfigChanged(configs);
                        try {
                            // 异常注销会抛出异常
                            mRecorder.unregisterAudioRecordingCallback(this);
                        } catch (Exception e) {
                            Log.e(TAG, e.getLocalizedMessage());
                        }
                        startRecordTime();
                    }
                });
            }
            mRecorder.prepare();
            mRecorder.start();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                startRecordTime();
            }
        } catch (Exception e) {
            Log.e(TAG, "startRecord failed", e);
            stopInternalRecord();
            onRecordCompleted(false);
        }
    }

    private void startRecordTime() {
        // 最大录制时间之后需要停止录制
        appCompatActivity.getLifecycle().addObserver(this);
        final Handler recordHandler = recordHandler();
        if (recordHandler != null) {
            recordHandler.removeCallbacksAndMessages(null);
            recordHandler.post(mUpdateMicStatusTimer);
            recordHandler.postDelayed(() -> mMainHandler.post(this::stopRecord), MAX_MILLISECOND);
        }
        if (mRecordCallback != null) {
            mRecordCallback.onStart();
        }
    }

    public void stopRecord() {
        stopInternalRecord();
        onRecordCompleted(true);
        mRecordCallback = null;
    }

    public void cancelRecord() {
        stopInternalRecord();
        onRecordCompleted(false);
        mRecordCallback = null;
    }

    private void stopInternalRecord() {
        SimpleAudioFocus.SINGLE.releaseAudioFocusListener();
        final Handler recordHandler = recordHandler();
        if (recordHandler != null) {
            recordHandler.removeCallbacksAndMessages(null);
        }
        if (mRecorder == null) {
            return;
        }
        mRecorder.release();
        mRecorder = null;
    }

    private void onRecordCompleted(boolean success) {
        if (appCompatActivity != null) {
            appCompatActivity.getLifecycle().removeObserver(this);
            appCompatActivity = null;
        }

        if (mRecordCallback != null) {
            mRecordCallback.onCompletion(this, success ? mAudioRecordPath : null);
            mRecordCallback = null;
        }
        mRecorder = null;
    }

    public int getRecordLength() {
        if (mAudioRecordPath != null) {
            return recordTime;
        }
        return 0;
    }

    public void getRecordLength(SimpleAudioCallback<Integer> callback) {
        if (mAudioRecordPath != null) {
            MediaPlayer player = new MediaPlayer();
            try {
                player.setDataSource(mAudioRecordPath);
            } catch (IOException e) {
                e.printStackTrace();
                callback.call(recordTime);
                return;
            }
            player.setOnPreparedListener(mp -> {
                callback.call(mp.getDuration());
                mp.release();
            });
            try {
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                callback.call(recordTime);
                player.release();
                return;
            }
            return;
        }
        callback.call(0);
    }

    public interface RecordCallback {

        void onStart();

        void onCompletion(SimpleAudioRecorder audio, String outPath);

        void recordingDB(double db);

        void recordingWatch(int second);

        void onError(Exception e);
    }
}
