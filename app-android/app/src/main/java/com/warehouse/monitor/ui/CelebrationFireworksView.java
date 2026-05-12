/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CelebrationFireworksView extends View {

    private static final int BURST_COUNT = 5;
    private static final int PARTICLES_PER_BURST = 28;
    private static final int TWINKLE_COUNT = 26;
    private static final float ROCKET_PHASE = 0.24f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Particle> particles = new ArrayList<>();
    private final List<Twinkle> twinkles = new ArrayList<>();
    private final Random random = new Random();
    private final int[] palette = new int[]{
            Color.parseColor("#2F6F8F"),
            Color.parseColor("#00B42A"),
            Color.parseColor("#FF7D00"),
            Color.parseColor("#F53F3F"),
        Color.parseColor("#FFD666"),
        Color.parseColor("#FF85C0")
    };

    private ValueAnimator animator;
    private float progress = 0f;

    public CelebrationFireworksView(Context context) {
        super(context);
        init();
    }

    public CelebrationFireworksView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CelebrationFireworksView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
    }

    public void startCelebration() {
        if (getWidth() == 0 || getHeight() == 0) {
            post(this::startCelebration);
            return;
        }

        generateParticles();
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2400L);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {
                generateParticles();
            }
        });
        animator.start();
    }

    private void generateParticles() {
        particles.clear();
        twinkles.clear();
        float width = getWidth();
        float height = getHeight();

        for (int twinkleIndex = 0; twinkleIndex < TWINKLE_COUNT; twinkleIndex++) {
            twinkles.add(new Twinkle(
                    width * (0.06f + (0.88f * random.nextFloat())),
                    height * (0.08f + (0.62f * random.nextFloat())),
                    dp(1.6f) + (random.nextFloat() * dp(2.8f)),
                    palette[random.nextInt(palette.length)],
                    random.nextFloat()
            ));
        }

        for (int burstIndex = 0; burstIndex < BURST_COUNT; burstIndex++) {
            float startX = width * (0.12f + (0.19f * burstIndex)) + ((random.nextFloat() - 0.5f) * dp(22f));
            float startY = height * (0.28f + (random.nextFloat() * 0.28f));
            float delay = burstIndex * 0.10f;
            float launchStartX = startX + ((random.nextFloat() - 0.5f) * dp(30f));
            float launchStartY = height + dp(26f) + (random.nextFloat() * dp(34f));

            for (int particleIndex = 0; particleIndex < PARTICLES_PER_BURST; particleIndex++) {
                float angle = (float) ((Math.PI * 2.0 * particleIndex) / PARTICLES_PER_BURST);
                angle += (random.nextFloat() - 0.5f) * 0.34f;

                float distance = dp(44f) + (random.nextFloat() * dp(42f));
                float velocityX = (float) Math.cos(angle) * distance;
                float velocityY = (float) Math.sin(angle) * distance - dp(26f);

                particles.add(new Particle(
                        launchStartX,
                        launchStartY,
                        startX,
                        startY,
                        velocityX,
                        velocityY,
                        dp(2.8f) + (random.nextFloat() * dp(3.6f)),
                        palette[random.nextInt(palette.length)],
                        delay
                ));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTwinkles(canvas);

        for (Particle particle : particles) {
            float localProgress = (progress - particle.delay) / (1f - particle.delay);
            if (localProgress <= 0f || localProgress >= 1f) {
                continue;
            }

            if (localProgress < ROCKET_PHASE) {
                drawLaunch(canvas, particle, localProgress / ROCKET_PHASE);
                continue;
            }

            float burstProgress = (localProgress - ROCKET_PHASE) / (1f - ROCKET_PHASE);
            float easedProgress = 1f - (float) Math.pow(1f - burstProgress, 1.8f);
            float x = particle.startX + (particle.velocityX * easedProgress);
            float y = particle.startY + (particle.velocityY * easedProgress) + (dp(68f) * burstProgress * burstProgress);
            float trailProgress = Math.max(0f, burstProgress - 0.10f);
            float trailEased = 1f - (float) Math.pow(1f - trailProgress, 1.8f);
            float trailX = particle.startX + (particle.velocityX * trailEased);
            float trailY = particle.startY + (particle.velocityY * trailEased) + (dp(68f) * trailProgress * trailProgress);
            float radius = particle.radius * (1f - (burstProgress * 0.28f));
            int alpha = (int) (255f * (1f - burstProgress));

            paint.setStrokeWidth(radius * 0.9f);
            paint.setColor(withAlpha(particle.color, Math.max(24, alpha / 2)));
            canvas.drawLine(trailX, trailY, x, y, paint);

            paint.setColor(withAlpha(particle.color, Math.max(18, alpha / 4)));
            canvas.drawCircle(x, y, radius * 2.8f, paint);

            paint.setColor(withAlpha(Color.WHITE, Math.max(30, alpha / 2)));
            canvas.drawCircle(x, y, radius * 1.35f, paint);

            paint.setColor(withAlpha(particle.color, alpha));
            canvas.drawCircle(x, y, radius, paint);
        }
    }

    private void drawTwinkles(Canvas canvas) {
        for (Twinkle twinkle : twinkles) {
            float pulse = (float) ((Math.sin((progress + twinkle.phase) * Math.PI * 4.0) + 1.0) * 0.5);
            int alpha = (int) (38 + (pulse * 128));
            float radius = twinkle.radius * (0.72f + (pulse * 0.9f));

            paint.setColor(withAlpha(twinkle.color, alpha / 4));
            canvas.drawCircle(twinkle.x, twinkle.y, radius * 2.4f, paint);

            paint.setColor(withAlpha(twinkle.color, alpha));
            canvas.drawCircle(twinkle.x, twinkle.y, radius, paint);
        }
    }

    private void drawLaunch(Canvas canvas, Particle particle, float rocketProgress) {
        float easedLaunch = 1f - (float) Math.pow(1f - rocketProgress, 2.2f);
        float x = particle.launchStartX + ((particle.startX - particle.launchStartX) * easedLaunch);
        float y = particle.launchStartY + ((particle.startY - particle.launchStartY) * easedLaunch);
        float trailY = particle.launchStartY + ((particle.startY - particle.launchStartY) * Math.max(0f, easedLaunch - 0.16f));

        paint.setStrokeWidth(dp(2.4f));
        paint.setColor(withAlpha(particle.color, 104));
        canvas.drawLine(x, trailY, x, y, paint);

        paint.setColor(withAlpha(Color.WHITE, 168));
        canvas.drawCircle(x, y, dp(2.2f), paint);

        paint.setColor(withAlpha(particle.color, 220));
        canvas.drawCircle(x, y, dp(1.4f), paint);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(alpha, 255)), Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    private static final class Particle {
        private final float launchStartX;
        private final float launchStartY;
        private final float startX;
        private final float startY;
        private final float velocityX;
        private final float velocityY;
        private final float radius;
        private final int color;
        private final float delay;

        private Particle(float launchStartX, float launchStartY, float startX, float startY,
                         float velocityX, float velocityY, float radius, int color, float delay) {
            this.launchStartX = launchStartX;
            this.launchStartY = launchStartY;
            this.startX = startX;
            this.startY = startY;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.radius = radius;
            this.color = color;
            this.delay = delay;
        }
    }

    private static final class Twinkle {
        private final float x;
        private final float y;
        private final float radius;
        private final int color;
        private final float phase;

        private Twinkle(float x, float y, float radius, int color, float phase) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.phase = phase;
        }
    }
}