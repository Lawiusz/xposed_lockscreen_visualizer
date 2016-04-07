/*
* Copyright (C) 2015 The CyanogenMod Project
* Copyright (C) 2016 Lawiusz
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package pl.lawiusz.lockscreenvisualizerxposed;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;

public class VisualizerView extends View implements Palette.PaletteAsyncListener,
        KeyguardStateMonitor.Listener {

    private final Paint mPaint;
    private Visualizer mVisualizer;
    private ObjectAnimator mVisualizerColorAnimator;

    private final ValueAnimator[] mValueAnimators;
    private final float[] mFFTPoints;

    private boolean mVisible = false;
    private boolean mPlaying = false;
    private boolean mDisplaying = false; // the state we're animating to
    private int mColor;
    private Bitmap mCurrentBitmap;

    private KeyguardStateMonitor mKeyguardMonitor;

    private final Visualizer.OnDataCaptureListener mVisualizerListener =
            new Visualizer.OnDataCaptureListener() {
                byte rfk, ifk;
                int dbValue;
                float magnitude;

                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    for (int i = 0; i < 32; i++) {
                        mValueAnimators[i].cancel();
                        rfk = fft[i * 2 + 2];
                        ifk = fft[i * 2 + 3];
                        magnitude = rfk * rfk + ifk * ifk;
                        dbValue = magnitude > 0 ? (int) (10 * Math.log10(magnitude)) : 0;

                        mValueAnimators[i].setFloatValues(mFFTPoints[i * 4 + 1],
                                mFFTPoints[3] - (dbValue * 16f));
                        //mValueAnimators[i].setInterpolator(new AccelerateDecelerateInterpolator());
                        mValueAnimators[i].start();
                    }
                }
            };

    private final Runnable mLinkVisualizer = new Runnable() {
        @Override
        public void run() {
            try {
                mVisualizer = new Visualizer(0);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            mVisualizer.setEnabled(false);
            mVisualizer.setCaptureSize(66);
            mVisualizer.setDataCaptureListener(mVisualizerListener,Visualizer.getMaxCaptureRate(),
                    false, true);
            mVisualizer.setEnabled(true);
        }
    };

    private final Runnable mUnlinkVisualizer = new Runnable() {
        @Override
        public void run() {
            if (mVisualizer != null) {
                mVisualizer.setEnabled(false);
                mVisualizer.release();
                mVisualizer = null;
            }
        }
    };

    public VisualizerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mColor = Color.TRANSPARENT;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mColor);

        mFFTPoints = new float[128];
        mValueAnimators = new ValueAnimator[32];
        for (int i = 0; i < 32; i++) {
            final int j = i * 4 + 1;
            mValueAnimators[i] = new ValueAnimator();
            mValueAnimators[i].setDuration(128);
            mValueAnimators[i].addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mFFTPoints[j] = (float) animation.getAnimatedValue();
                    postInvalidate();
                }
            });
        }
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VisualizerView(Context context) {
        this(context, null, 0);
    }

    private void updateViewVisibility() {
        setVisibility(mKeyguardMonitor != null && mKeyguardMonitor.isShowing() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mKeyguardMonitor != null) {
            mKeyguardMonitor.registerListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mKeyguardMonitor != null) {
            mKeyguardMonitor.unregisterListener(this);
        }
        mCurrentBitmap = null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float barUnit = w / 32f;
        float barWidth = barUnit * 8f / 9f;
        barUnit = barWidth + (barUnit - barWidth) * 32f / 31f;
        mPaint.setStrokeWidth(barWidth);

        for (int i = 0; i < 32; i++) {
            mFFTPoints[i * 4] = mFFTPoints[i * 4 + 2] = i * barUnit + (barWidth / 2);
            mFFTPoints[i * 4 + 1] = h;
            mFFTPoints[i * 4 + 3] = h;
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return mDisplaying;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mVisualizer != null) {
            canvas.drawLines(mFFTPoints, mPaint);
        }
    }

    public void setKeyguardMonitor(KeyguardStateMonitor kgm) {
        mKeyguardMonitor = kgm;
        if (isAttachedToWindow()) {
            // otherwise we might never register ourselves
            mKeyguardMonitor.registerListener(this);
            updateViewVisibility();
        }
    }

    public void setVisible() {
        if (!mVisible) {
            mVisible = true;
            checkStateChanged();
        }
    }

    public void setPlaying(boolean playing) {
        if (mPlaying != playing) {
            mPlaying = playing;
            checkStateChanged();
        }
    }


    public void setBitmap(Bitmap bitmap) {
        if (mCurrentBitmap == bitmap) {
            return;
        }
        mCurrentBitmap = bitmap;
        if (bitmap != null) {
            Palette.from(bitmap).generate(this);
        } else {
            setColor(Color.TRANSPARENT);
        }
    }


    @Override
    public void onGenerated(Palette palette) {
        int color = Color.TRANSPARENT;

        color = palette.getVibrantColor(color);
        if (color == Color.TRANSPARENT) {
            color = palette.getLightVibrantColor(color);
            if (color == Color.TRANSPARENT) {
                color = palette.getDarkVibrantColor(color);
            }
        }
        setColor(color);
    }

    private void setColor(int color) {
        if (color == Color.TRANSPARENT) {
            color = Color.WHITE;
        }

        color = Color.argb(140, Color.red(color), Color.green(color), Color.blue(color));

        if (mColor != color) {
            mColor = color;

            if (mVisualizer != null) {
                if (mVisualizerColorAnimator != null) {
                    mVisualizerColorAnimator.cancel();
                }

                mVisualizerColorAnimator = ObjectAnimator.ofArgb(mPaint, "color",
                        mPaint.getColor(), mColor);
                mVisualizerColorAnimator.setStartDelay(600);
                mVisualizerColorAnimator.setDuration(1200);
                mVisualizerColorAnimator.start();
            } else {
                mPaint.setColor(mColor);
            }
        }
    }

    private void checkStateChanged() {
        if (mVisible && mPlaying) {
            if (!mDisplaying) {
                mDisplaying = true;
                AsyncTask.execute(mLinkVisualizer);
                animate()
                        .alpha(1f)
                        .withEndAction(null)
                        .setDuration(800);
            }
        } else {
            if (mDisplaying) {
                mDisplaying = false;
                if (mVisible) {
                    animate()
                            .alpha(0f)
                            .withEndAction(mUnlinkVisualizer)
                            .setDuration(600);
                } else {
                    AsyncTask.execute(mUnlinkVisualizer);
                    animate().
                            alpha(0f)
                            .withEndAction(null)
                            .setDuration(0);
                }
            }
        }
    }

    @Override
    public void onKeyguardStateChanged() {
        checkStateChanged();
    }

}