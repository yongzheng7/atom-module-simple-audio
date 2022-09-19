package com.atom.module.audio.simple;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public enum SimpleAudioFocus {

    SINGLE ;

    public final String TAG = SimpleAudioFocus.class.getSimpleName();

    private AudioListener mAudioListener;

    private AudioManager mAudioManager;

    private AudioManager getAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) SimpleAudioManager.getApplication().getSystemService(Context.AUDIO_SERVICE);
        }
        return mAudioManager;
    }

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {//监听器
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    //播放操作
                    Log.e(TAG, "restart");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    mAudioManager.abandonAudioFocus(this);
                    //暂停操作
                    if (mAudioListener != null) {
                        mAudioListener.stop();
                    }
                    break;
                default:
                    break;
            }
        }
    };


    //zxzhong 请求音频焦点 设置监听
    public boolean requestTheAudioFocus(final AudioListener audioListener) {
        mAudioListener = audioListener;
        /**
         * AUDIOFOCUS_GAIN指示申请得到的Audio Focus不知道会持续多久，一般是长期占有；
         * AUDIOFOCUS_GAIN_TRANSIENT指示要申请的AudioFocus是暂时性的，会很快用完释放的；
         * AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK不但说要申请的AudioFocus是暂时性的，还指示当前正在使用AudioFocus的可以继续播放，只是要“duck”一下（降低音量）。
         * AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK / AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK 音量的减低和恢复
         * AudioManager.AUDIOFOCUS_GAIN_TRANSIENT / AudioManager.AUDIOFOCUS_LOSS_TRANSIENT 焦点暂时拾取和得到
         * AudioManager.AUDIOFOCUS_LOSS 焦点失去 / AudioManager.AUDIOFOCUS_GAIN 焦点得到
         */
        //下面两个常量参数试过很多 都无效，最终反编译了其他app才搞定，汗~
        return getAudioManager().requestAudioFocus(mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public void releaseAudioFocusListener() {
        getAudioManager().abandonAudioFocus(mAudioFocusChangeListener);
    }

    public interface AudioListener {
        void stop();
    }
}
