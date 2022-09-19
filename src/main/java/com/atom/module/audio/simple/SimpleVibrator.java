package com.atom.module.audio.simple;

import android.app.Service;
import android.os.Vibrator;

public enum SimpleVibrator {

    SINGLE;

    public final String TAG = SimpleVibrator.class.getSimpleName();

    Vibrator vibrator;

    public Vibrator getVibrator() {
        if (vibrator == null) {
            vibrator = (Vibrator) SimpleAudioManager.getApplication().getSystemService(Service.VIBRATOR_SERVICE);
        }
        return vibrator;
    }

    public void Vibrate(long milliseconds) {
        //long [] pattern={100,400,100,400};//停止 开启 停止 开启
        // 重复两次上面的panttern，如果只是震动一次，index的值设定为-1
        //vibrator.vibrate(pattern, 2);
        getVibrator().vibrate(milliseconds);
    }

    public void startVibrate() {
        getVibrator().vibrate(100);
    }

    public void stopVibrate() {
        getVibrator().cancel();
    }

    public void Vibrate(long[] pattern, boolean isRepeat) {
        vibrator.vibrate(pattern, isRepeat ? 1 : -1);
    }

    public void destroy() {
        vibrator.cancel();
    }
}
