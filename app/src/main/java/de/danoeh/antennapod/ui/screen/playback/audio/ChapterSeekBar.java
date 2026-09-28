package de.danoeh.antennapod.ui.screen.playback.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class ChapterSeekBar extends androidx.appcompat.widget.AppCompatSeekBar {

    private float width;
    private float center;
    private float density;
    private float progressPrimary;
    private float progressSecondary;
    private float[] dividerPos;
    private boolean isHighlighted = false;
    private boolean isPlaying = false;
    private float phase = 0f;
    private float currentAmplitude = 0f;
    private long lastFrameTime = 0;
    private float wavelength;
    private float maxAmplitude;

    private final Paint paintBackground = new Paint();
    private final Paint paintSecondary = new Paint();
    private final Paint paintProgressPrimary = new Paint();
    private final Path wavePath = new Path();

    public ChapterSeekBar(Context context) {
        super(context);
        init(context);
    }

    public ChapterSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChapterSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setBackground(null);
        dividerPos = null;
        density = context.getResources().getDisplayMetrics().density;
        wavelength = density * 28.0f;
        maxAmplitude = density * 3.5f;

        float strokeWidth = density * 4.0f;

        paintBackground.setColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorSurfaceVariant));
        paintBackground.setAlpha(128);
        paintBackground.setStyle(Paint.Style.STROKE);
        paintBackground.setStrokeWidth(strokeWidth);
        paintBackground.setStrokeCap(Paint.Cap.ROUND);
        paintBackground.setStrokeJoin(Paint.Join.ROUND);
        paintBackground.setAntiAlias(true);

        paintSecondary.setColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorSurfaceVariant));
        paintSecondary.setAlpha(200);
        paintSecondary.setStyle(Paint.Style.STROKE);
        paintSecondary.setStrokeWidth(strokeWidth);
        paintSecondary.setStrokeCap(Paint.Cap.ROUND);
        paintSecondary.setStrokeJoin(Paint.Join.ROUND);
        paintSecondary.setAntiAlias(true);

        paintProgressPrimary.setColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorPrimary));
        paintProgressPrimary.setStyle(Paint.Style.STROKE);
        paintProgressPrimary.setStrokeWidth(strokeWidth);
        paintProgressPrimary.setStrokeCap(Paint.Cap.ROUND);
        paintProgressPrimary.setStrokeJoin(Paint.Join.ROUND);
        paintProgressPrimary.setAntiAlias(true);
    }

    public void setIsPlaying(boolean isPlaying) {
        if (this.isPlaying != isPlaying) {
            this.isPlaying = isPlaying;
            lastFrameTime = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isPlaying = false;
    }

    /**
     * Sets the relative positions of the chapter dividers.
     * @param dividerPos of the chapter dividers relative to the duration of the media.
     */
    public void setDividerPos(final float[] dividerPos) {
        if (dividerPos != null) {
            this.dividerPos = new float[dividerPos.length + 2];
            this.dividerPos[0] = 0;
            System.arraycopy(dividerPos, 0, this.dividerPos, 1, dividerPos.length);
            this.dividerPos[this.dividerPos.length - 1] = 1;
        } else {
            this.dividerPos = null;
        }
        invalidate();
    }

    public void highlightCurrentChapter() {
        isHighlighted = true;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                isHighlighted = false;
                invalidate();
            }
        }, 1000);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        center = (getBottom() - getPaddingBottom() - getTop() - getPaddingTop()) / 2.0f;
        width = (float) (getRight() - getPaddingRight() - getLeft() - getPaddingLeft());
        progressSecondary = getSecondaryProgress() / (float) getMax() * width;
        progressPrimary = getProgress() / (float) getMax() * width;

        boolean wavyEnabled = UserPreferences.isWavySeekbarEnabled();
        long now = SystemClock.uptimeMillis();

        if (wavyEnabled) {
            float targetAmp = (isPlaying && !isPressed()) ? maxAmplitude : 0f;
            currentAmplitude += (targetAmp - currentAmplitude) * 0.18f;

            if (isPlaying && !isPressed()) {
                if (lastFrameTime > 0) {
                    long dt = Math.min(now - lastFrameTime, 100);
                    phase += (dt / 1000.0f) * (density * 32.0f);
                    if (phase > wavelength) {
                        phase %= wavelength;
                    }
                }
                lastFrameTime = now;
                postInvalidateOnAnimation();
            } else {
                lastFrameTime = now;
                if (Math.abs(targetAmp - currentAmplitude) > 0.05f) {
                    postInvalidateOnAnimation();
                }
            }
        } else {
            currentAmplitude = 0f;
            lastFrameTime = now;
        }

        if (dividerPos == null) {
            drawProgress(canvas, wavyEnabled);
        } else {
            drawProgressChapters(canvas, wavyEnabled);
        }
        drawThumb(canvas);
    }

    private void drawWavyLine(Canvas canvas, float startX, float endX, float centerY,
                              float amplitude, float phaseOffset, Paint paint) {
        if (endX <= startX) {
            return;
        }
        if (amplitude <= 0.2f) {
            canvas.drawLine(startX, centerY, endX, centerY, paint);
            return;
        }

        wavePath.rewind();
        wavePath.moveTo(startX, centerY);

        float step = density * 2.0f;
        float length = endX - startX;
        float rampDist = Math.min(density * 16.0f, length / 2.0f);

        for (float x = startX; x < endX; x += step) {
            float ampFactor = 1.0f;
            if (rampDist > 0) {
                if (x - startX < rampDist) {
                    ampFactor = (x - startX) / rampDist;
                } else if (endX - x < rampDist) {
                    ampFactor = (endX - x) / rampDist;
                }
            }
            float curAmp = amplitude * ampFactor;
            float y = centerY + (float) (Math.sin((x - startX + phaseOffset) * (2.0 * Math.PI / wavelength)) * curAmp);
            wavePath.lineTo(x, y);
        }
        wavePath.lineTo(endX, centerY);
        canvas.drawPath(wavePath, paint);
    }

    private void drawProgress(Canvas canvas, boolean wavyEnabled) {
        final int saveCount = canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());

        if (width > progressPrimary) {
            canvas.drawLine(progressPrimary, center, width, center, paintBackground);
        }
        if (progressSecondary > progressPrimary) {
            canvas.drawLine(progressPrimary, center, progressSecondary, center, paintSecondary);
        }
        if (progressPrimary > 0) {
            if (wavyEnabled) {
                drawWavyLine(canvas, 0, progressPrimary, center, currentAmplitude, phase, paintProgressPrimary);
            } else {
                canvas.drawLine(0, center, progressPrimary, center, paintProgressPrimary);
            }
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawProgressChapters(Canvas canvas, boolean wavyEnabled) {
        final int saveCount = canvas.save();
        float chapterMargin = density * 1.5f;

        canvas.translate(getPaddingLeft(), getPaddingTop());

        for (int i = 1; i < dividerPos.length; i++) {
            float left = dividerPos[i - 1] * width;
            float right = dividerPos[i] * width - chapterMargin;
            if (right <= left) {
                continue;
            }

            if (right <= progressPrimary) {
                if (wavyEnabled) {
                    drawWavyLine(canvas, left, right, center, currentAmplitude, phase, paintProgressPrimary);
                } else {
                    canvas.drawLine(left, center, right, center, paintProgressPrimary);
                }
            } else if (left < progressPrimary) {
                if (wavyEnabled) {
                    drawWavyLine(canvas, left, progressPrimary, center, currentAmplitude, phase, paintProgressPrimary);
                } else {
                    canvas.drawLine(left, center, progressPrimary, center, paintProgressPrimary);
                }
                if (progressSecondary > progressPrimary) {
                    float secEnd = Math.min(progressSecondary, right);
                    canvas.drawLine(progressPrimary, center, secEnd, center, paintSecondary);
                    if (right > secEnd) {
                        canvas.drawLine(secEnd, center, right, center, paintBackground);
                    }
                } else {
                    canvas.drawLine(progressPrimary, center, right, center, paintBackground);
                }
            } else {
                if (progressSecondary > left) {
                    float secEnd = Math.min(progressSecondary, right);
                    canvas.drawLine(left, center, secEnd, center, paintSecondary);
                    if (right > secEnd) {
                        canvas.drawLine(secEnd, center, right, center, paintBackground);
                    }
                } else {
                    canvas.drawLine(left, center, right, center, paintBackground);
                }
            }
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawThumb(Canvas canvas) {
        if (getThumb() == null) {
            return;
        }
        final int saveCount = canvas.save();
        canvas.translate(getPaddingLeft() - getThumbOffset(), getPaddingTop());
        getThumb().draw(canvas);
        canvas.restoreToCount(saveCount);
    }
}
