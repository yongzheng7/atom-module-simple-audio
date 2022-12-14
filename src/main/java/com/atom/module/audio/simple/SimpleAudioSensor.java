package com.atom.module.audio.simple;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.content.Context.SENSOR_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;


public class SimpleAudioSensor implements LifecycleEventObserver, SensorEventListener {

    public final String TAG = this.getClass().getSimpleName();

    private AudioManager audioManager;
    private PowerManager powerManager;

    private SensorManager sensorManager;
    private Sensor sensor;
    private Boolean saveType;

    public SimpleAudioSensor(@NonNull AppCompatActivity mActivity) {
        sensorManager = (SensorManager) mActivity.getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            throw new RuntimeException("SensorManager is null Error ");
        }
        powerManager = (PowerManager) mActivity.getSystemService(POWER_SERVICE);
        if (powerManager == null) {
            throw new RuntimeException("PowerManager is null Error ");
        }
        audioManager = (AudioManager) mActivity.getSystemService(AUDIO_SERVICE);
        if (audioManager == null) {
            throw new RuntimeException("AudioManager is null Error ");
        }
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mActivity.getLifecycle().addObserver(this);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (audioManager == null) {
            return;
        }
        if (SimpleAudioManager.isWiredHeadsetOn()) {
            Log.e(TAG, "???????????????");
            // ???????????????????????????????????????????????????
            return;
        }
        if (SimpleAudioPlayer.getInstance().isPlaying()) {
            // ????????????????????????
            boolean currType = event.values[0] >= sensor.getMaximumRange();
            if (saveType != null && saveType == currType) return;
            saveType = currType;
            Log.i(TAG, "???????????? >> " + currType);
            SimpleAudioPlayer.getInstance().pausePlay();
            if (currType) {
                // ??????????????????????????????????????????
                SimpleAudioManager.changeToSpeaker();
            } else {
                // ????????????????????????????????????????????????????????????????????????
                SimpleAudioManager.changeToReceiverOnly();
            }
            SimpleAudioPlayer.getInstance().resumePlayBySeek(1000);
        } else {
            // ??????????????????
            SimpleAudioManager.changeToNormal();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event.compareTo(Lifecycle.Event.ON_DESTROY) >= 0) {
            source.getLifecycle().removeObserver(this);
            if(sensorManager != null){
                sensorManager.unregisterListener(this);
                sensorManager = null;
            }
        }
    }
}
