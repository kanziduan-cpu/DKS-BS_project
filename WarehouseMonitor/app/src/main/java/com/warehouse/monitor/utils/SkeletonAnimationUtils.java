package com.warehouse.monitor.utils;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class SkeletonAnimationUtils {

    private static final long ANIMATION_DURATION = 1000;
    private static final float MIN_ALPHA = 0.3f;
    private static final float MAX_ALPHA = 1.0f;

    public static void startShimmerAnimation(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", MIN_ALPHA, MAX_ALPHA, MIN_ALPHA);
        animator.setDuration(ANIMATION_DURATION);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    public static void stopShimmerAnimation(View view) {
        view.animate().cancel();
        view.setAlpha(1.0f);
    }
}
