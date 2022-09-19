package com.atom.module.audio.simple;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public class SimpleHeadsetReceiver extends BroadcastReceiver {
    private static final String TAG = "SimpleHeadsetReceiver";

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // 有线耳机的插入和拨出
        if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
            int state = intent.getIntExtra("state", 0);
            if (state == 1) { // 耳机插入
                SimpleAudioManager.changeToReceiver();
            } else if (state == 0) {   // 耳机拔出
                SimpleAudioPlayer.getInstance().stopPlay();
                Log.e("SimpleAudioManager", "有线耳机");
                SimpleAudioManager.choiceAudioModel(SimpleAudioManager.defaultOutput);
            }
            Log.i(TAG, state == 0 ? "耳机拔出" : "耳机插入");
        }
        // 蓝牙耳机的连接和断开
        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "链接蓝牙耳机");
                SimpleAudioManager.changeToHeadset();
            } else {
                Log.i(TAG, "断开连接耳机");
                Log.e("SimpleAudioManager", "蓝牙耳机");
                SimpleAudioPlayer.getInstance().stopPlay();
                SimpleAudioManager.choiceAudioModel(SimpleAudioManager.defaultOutput);
            }
        }
        // 输出中断的回调 包括有线耳机和蓝牙耳机的中断
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
            Log.i(TAG, "输出设备中断,重置为外放 播放");
            Log.e("SimpleAudioManager", "任何耳机");
            SimpleAudioPlayer.getInstance().stopPlay();
            SimpleAudioManager.choiceAudioModel(SimpleAudioManager.defaultOutput);
        }
        // 蓝牙开关的状态回调
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.i(TAG, "蓝牙已关闭");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.i(TAG, "蓝牙已打开");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.i(TAG, "正在打开蓝牙");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.i(TAG, "正在关闭蓝牙");
                    break;
                default:
                    Log.i(TAG, "未知状态");
            }
        }
        // 蓝牙链接的状态回调
        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            BluetoothDevice device;
            switch (intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)) {
                case BluetoothA2dp.STATE_CONNECTING:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(TAG, "device: " + device.getName() + " connecting");
                    break;
                case BluetoothA2dp.STATE_CONNECTED:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(TAG, "device: " + device.getName() + " connected");
                    break;
                case BluetoothA2dp.STATE_DISCONNECTING:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(TAG, "device: " + device.getName() + " disconnecting");
                    break;
                case BluetoothA2dp.STATE_DISCONNECTED:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(TAG, "device: " + device.getName() + " disconnected");
                    break;
                default:
                    break;
            }
        }
        // 蓝牙连接广播
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.i(TAG, "蓝牙已连接：" + device.getName());
        }
        // 蓝牙断开广播
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.i(TAG, "蓝牙已断开：" + device.getName());
        }
    }
}