package com.agrigo.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class MapUtils {

    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final OkHttpClient client = new OkHttpClient();

    public interface RouteCallback {
        void onRouteFetched(List<LatLng> path, String distance, String duration);
        void onError(String message);
    }

    /**
     * Fetch route — tries Google Directions API first, then falls back to OSRM (free).
     */
    public static void fetchRoute(Context context, LatLng origin, LatLng dest, RouteCallback callback) {
        String apiKey = getApiKey(context);
        if (apiKey == null || apiKey.isEmpty()) {
            Timber.w("No Google API key, trying OSRM directly");
            fetchRouteOSRM(origin, dest, callback);
            return;
        }

        String url = DIRECTIONS_URL + "?origin=" + origin.latitude + "," + origin.longitude
                + "&destination=" + dest.latitude + "," + dest.longitude
                + "&mode=driving"
                + "&key=" + apiKey;

        Timber.d("Fetching Google route from: %s", url);

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Timber.e(e, "Google Directions API Network Failure, trying OSRM");
                fetchRouteOSRM(origin, dest, callback);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);
                        
                        String status = jsonObject.getString("status");
                        if (!status.equals("OK")) {
                            String errorMsg = jsonObject.optString("error_message", "Status: " + status);
                            Timber.e("Google Directions API Error: %s — falling back to OSRM", errorMsg);
                            fetchRouteOSRM(origin, dest, callback);
                            return;
                        }

                        JSONArray routes = jsonObject.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            String encodedPolyline = route.getJSONObject("overview_polyline").getString("points");
                            List<LatLng> path = PolyUtil.decode(encodedPolyline);

                            String distance = "";
                            String duration = "";
                            JSONArray legs = route.getJSONArray("legs");
                            if (legs.length() > 0) {
                                JSONObject leg = legs.getJSONObject(0);
                                distance = leg.getJSONObject("distance").getString("text");
                                duration = leg.getJSONObject("duration").getString("text");
                            }

                            final String finalDistance = distance;
                            final String finalDuration = duration;
                            new Handler(Looper.getMainLooper()).post(() -> callback.onRouteFetched(path, finalDistance, finalDuration));
                        } else {
                            Timber.w("Google returned 0 routes, trying OSRM");
                            fetchRouteOSRM(origin, dest, callback);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Google Directions parse error, trying OSRM");
                        fetchRouteOSRM(origin, dest, callback);
                    }
                } else {
                    Timber.e("Google Directions HTTP error %d, trying OSRM", response.code());
                    fetchRouteOSRM(origin, dest, callback);
                }
            }
        });
    }

    /**
     * Free fallback: OSRM public routing service (no API key needed).
     * Returns road-following polylines just like Google Directions.
     */
    private static void fetchRouteOSRM(LatLng origin, LatLng dest, RouteCallback callback) {
        // OSRM uses lng,lat order (opposite of Google)
        String url = OSRM_URL
                + origin.longitude + "," + origin.latitude + ";"
                + dest.longitude + "," + dest.latitude
                + "?overview=full&geometries=polyline";

        Timber.d("Fetching OSRM route: %s", url);

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Timber.e(e, "OSRM also failed");
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Both Google and OSRM routing failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);

                        String code = jsonObject.getString("code");
                        if (!"Ok".equals(code)) {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError("OSRM error: " + code));
                            return;
                        }

                        JSONArray routes = jsonObject.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            String geometry = route.getString("geometry");
                            List<LatLng> path = PolyUtil.decode(geometry);

                            double distMeters = route.getDouble("distance");
                            double durSeconds = route.getDouble("duration");
                            
                            String distance = String.format("%.1f km", distMeters / 1000.0);
                            String duration = String.format("%.0f mins", durSeconds / 60.0);

                            Timber.d("OSRM route fetched: %s, %s, %d points", distance, duration, path.size());
                            new Handler(Looper.getMainLooper()).post(() -> callback.onRouteFetched(path, distance, duration));
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError("OSRM returned no routes"));
                        }
                    } catch (Exception e) {
                        Timber.e(e, "OSRM parse error");
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("OSRM parse: " + e.getMessage()));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("OSRM HTTP error"));
                }
            }
        });
    }

    public static Polyline drawRoute(GoogleMap map, List<LatLng> path, boolean isDotted) {
        PolylineOptions options = new PolylineOptions()
                .addAll(path)
                .width(16f)
                .color(Color.parseColor("#16A34A"))
                .geodesic(true);

        // Always solid — no dotted pattern
        return map.addPolyline(options);
    }

    private static String getApiKey(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            return null;
        }
    }
}
