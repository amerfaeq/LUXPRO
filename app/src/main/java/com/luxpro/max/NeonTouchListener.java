package com.luxpro.max;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class NeonTouchListener implements View.OnTouchListener {

    private final Vibrator vibrator;

    public NeonTouchListener(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Haptic Shockwave
                triggerHaptic();

                // Neon Pulse Shrink
                ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(v, "scaleX", 0.95f);
                ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(v, "scaleY", 0.95f);
                scaleDownX.setDuration(100);
                scaleDownY.setDuration(100);

                AnimatorSet scaleDown = new AnimatorSet();
                scaleDown.play(scaleDownX).with(scaleDownY);
                scaleDown.start();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Neon Pulse Expand with Overshoot (Bounce effect)
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(v, "scaleX", 1.0f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(v, "scaleY", 1.0f);
                scaleUpX.setDuration(300);
                scaleUpY.setDuration(300);

                AnimatorSet scaleUp = new AnimatorSet();
                scaleUp.setInterpolator(new OvershootInterpolator(2.5f));
                scaleUp.play(scaleUpX).with(scaleUpY);
                scaleUp.start();

                // Trigger click visually only on UP if we want it, but return false to not
                // consume the actual click listener
                break;
        }
        return false; // Very Important so onClickListeners still fire
    }

    private void triggerHaptic() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(30);
            }
        }
    }
}
