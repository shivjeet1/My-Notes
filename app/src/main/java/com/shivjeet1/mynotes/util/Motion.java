package com.shivjeet1.mynotes.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public final class Motion {
    public static final long FAST = 120L;
    public static final long MEDIUM = 220L;
    public static final long SLOW = 320L;

    private Motion() {
    }

    public static boolean isEnabled(Context context) {
        if (context == null) {
            return true;
        }
        if (animatorsAllowed()) {
            try {
                float scale = Settings.Global.getFloat(context.getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
                if (scale == 0.0f) {
                    return false;
                }
            } catch (Exception ignored) {
                // ignore
            }
            try {
                float transitionScale = Settings.Global.getFloat(context.getContentResolver(),
                        Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f);
                if (transitionScale == 0.0f) {
                    return false;
                }
            } catch (Exception ignored) {
                // ignore
            }
            try {
                float windowScale = Settings.Global.getFloat(context.getContentResolver(),
                        Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f);
                if (windowScale == 0.0f) {
                    return false;
                }
            } catch (Exception ignored) {
                // ignore
            }
            return true;
        }
        return false;
    }

    private static boolean animatorsAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
        return true;
    }

    public static long duration(Context context, long normal) {
        return isEnabled(context) ? normal : 90L;
    }
}
