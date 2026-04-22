package com.agrigo.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeocodingUtils {

    public interface GeocodeCallback {
        void onAddressResolved(String formattedAddress);
    }

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String DEFAULT_LOCATION_TAG = "Pinned Location 📍";

    /**
     * Resolves a LatLng into a human-readable address on a background thread.
     */
    public static void getAddressFromLatLng(Context context, LatLng target, GeocodeCallback callback) {
        if (context == null || target == null || callback == null) return;

        executorService.execute(() -> {
            String resultAddress = DEFAULT_LOCATION_TAG;

            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(target.latitude, target.longitude, 1);

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        
                        String fullAddress = address.getAddressLine(0);
                        String subLocality = address.getSubLocality();
                        String subAdminArea = address.getSubAdminArea();
                        String featureName = address.getFeatureName();
                        
                        if (fullAddress != null && !fullAddress.isEmpty()) {
                            resultAddress = fullAddress; // Use full if available natively
                        } else if (subLocality != null && !subLocality.isEmpty()) {
                            resultAddress = subLocality;
                        } else if (featureName != null && !featureName.isEmpty()) {
                            resultAddress = featureName + ", " + (subAdminArea != null ? subAdminArea : "");
                        }
                    }
                }
            } catch (IOException e) {
                // Network unavailable or Geocoder failing
                resultAddress = DEFAULT_LOCATION_TAG;
            } catch (Exception e) {
                // Safe catch for crash prevention
                resultAddress = DEFAULT_LOCATION_TAG;
            }

            final String finalResult = resultAddress;
            mainHandler.post(() -> callback.onAddressResolved(finalResult));
        });
    }
}
