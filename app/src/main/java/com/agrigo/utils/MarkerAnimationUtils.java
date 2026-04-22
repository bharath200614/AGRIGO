package com.agrigo.utils;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.SphericalUtil;

public class MarkerAnimationUtils {

    /**
     * Animates a marker from its current position to the new position smoothly over the given duration.
     * Also rotates the marker to face the direction of movement.
     *
     * @param marker      The map marker to animate
     * @param toPosition  The new LatLng destination
     * @param durationMs  Animation duration in milliseconds
     */
    public static void animateMarkerToGB(final Marker marker, final LatLng toPosition, long durationMs) {
        if (marker == null || toPosition == null) return;
        
        final LatLng startPosition = marker.getPosition();
        if (startPosition.latitude == toPosition.latitude && startPosition.longitude == toPosition.longitude) {
            return;
        }

        final long start = SystemClock.uptimeMillis();
        
        // Calculate bearing
        float currentRotation = marker.getRotation();
        float targetRotation = (float) SphericalUtil.computeHeading(startPosition, toPosition);
        
        // Ensure rotation is shortest path
        final float[] rotationDelta = new float[1];
        float dy = targetRotation - currentRotation;
        if (dy > 180f) {
            rotationDelta[0] = dy - 360f;
        } else if (dy < -180f) {
            rotationDelta[0] = dy + 360f;
        } else {
            rotationDelta[0] = dy;
        }

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(durationMs);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            try {
                float v = animation.getAnimatedFraction();
                double lng = v * toPosition.longitude + (1 - v) * startPosition.longitude;
                double lat = v * toPosition.latitude + (1 - v) * startPosition.latitude;
                
                LatLng newPos = new LatLng(lat, lng);
                marker.setPosition(newPos);
                marker.setRotation(currentRotation + (rotationDelta[0] * v));
                
            } catch (Exception ex) {
                // Ignore safely
            }
        });
        valueAnimator.start();
    }
}
