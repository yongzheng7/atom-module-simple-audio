package com.atom.module.audio.simple;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.atom.module.audio.simple.exception.SimpleAudioRuntimeException;

public class SimpleAudioManager {

    private static Application application;
    public static boolean defaultOutput = false;
    public final static String TAG = SimpleAudioManager.class.getSimpleName();

    public static void init(Application application) {
        if (application == null) throw new SimpleAudioRuntimeException("application is null");
        if (SimpleAudioManager.application != null) return;
        SimpleAudioManager.application = application;
    }

    public static Application getApplication() {
        checkApplication();
        return application;
    }

    private static void checkApplication() {
        if (application == null)
            throw new SimpleAudioRuntimeException("checkApplication : application is null");
    }


    public static SimpleAudioFocus getSimpleAudioFocus() {
        checkApplication();
        return SimpleAudioFocus.SINGLE;
    }


    /**
     * 音频外放
     */
    public static void changeToSpeaker() {
        AudioManager audioManager = getAudioManager();
        //注意此处，蓝牙未断开时使用MODE_IN_COMMUNICATION而不是MODE_NORMAL
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);
        audioManager.setSpeakerphoneOn(true);
    }

    public static void changeToReceiverOnly() {
        AudioManager audioManager = getAudioManager();
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
    }

    /**
     * 切换到蓝牙音箱
     */
    public static void changeToHeadset() {
        AudioManager audioManager = getAudioManager();
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.startBluetoothSco();
        audioManager.setBluetoothScoOn(true);
        audioManager.setSpeakerphoneOn(false);
    }

    /**
     * 切换到听筒
     */
    public static void changeToReceiver() {
        AudioManager audioManager = getAudioManager();
        audioManager.setSpeakerphoneOn(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        }
    }

    @SuppressLint("WrongConstant")
    public static int getModel() {
        AudioManager audioManager = getAudioManager();
        return audioManager.getMode();
    }

    public static void changeToNormal() {
        AudioManager audioManager = getAudioManager();
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    public static boolean isWiredHeadsetOn() {
        AudioManager audioManager = getAudioManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo deviceInfo : audioDevices) {
                if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                    return true;
                }
            }
            return false;
        } else {
            return audioManager.isWiredHeadsetOn();
        }
    }

    public static boolean isBluetoothA2dpOn() {
        AudioManager audioManager = getAudioManager();
        return audioManager.isBluetoothA2dpOn();
    }

    /**
     * context 传入的是MicroContext.getApplication()
     */
    public static void choiceAudioModel(boolean outputReceiver) {
        Log.e(TAG, "1 " + (outputReceiver ? "to > 41" : "to > 42"));
        if (isWiredHeadsetOn()) {
            Log.e(TAG, "2 ");
            changeToReceiver();
        } else if (isBluetoothA2dpOn()) {
            Log.e(TAG, "3 ");
            changeToHeadset();
        } else {
            if (outputReceiver) {
                Log.e(TAG, "41 ");
                changeToReceiverOnly();
            } else {
                Log.e(TAG, "42 ");
                changeToSpeaker();
            }
        }
    }

    @SuppressLint("WrongConstant")
    public static void dispose(AudioManager.OnAudioFocusChangeListener focusRequest) {
        AudioManager audioManager = getAudioManager();
        if (audioManager.isBluetoothScoOn()) {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMicrophoneMute(false);
        audioManager.unloadSoundEffects();
        if (null != focusRequest) {
            audioManager.abandonAudioFocus(focusRequest);
        }
    }

    public static void release() {
        AudioManager audioManager = getAudioManager();
        if (audioManager.isBluetoothScoOn()) {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMicrophoneMute(false);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.unloadSoundEffects();
        audioManager.abandonAudioFocus(null);
    }

    public static AudioManager getAudioManager() {
        checkApplication();
        return (AudioManager) getApplication().getSystemService(Context.AUDIO_SERVICE);
    }
}
