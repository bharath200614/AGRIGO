package com.agrigo.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.content.pm.ServiceInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.agrigo.R;
import com.agrigo.activities.IncomingRequestActivity;
import com.agrigo.utils.PreferenceManager;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class DriverDispatchService extends Service {

    private static final String TAG = "DriverDispatchService";
    private static final String CHANNEL_ID = "DriverDispatchChannel";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private ListenerRegistration requestListener;
    private ListenerRegistration openRequestListener;
    private Location lastKnownLocation;

    private String uid;
    private String role; // "driver" or "machineryProvider" (or simply implies which collection to use)

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : preferenceManager.getUserId();

        role = preferenceManager.getUserRole();
        if (role == null) role = "";

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AgriGo Dispatch")
                .setContentText("Listening for incoming requests...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }

        startLocationUpdates();
        startListeningForRequests();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(10f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    lastKnownLocation = location;
                    updateLocationInFirestore(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateLocationInFirestore(Location location) {
        if (uid == null || uid.isEmpty()) return;

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentLat", lat);
        updates.put("currentLng", lng);
        updates.put("geoHash", hash);
        updates.put("lastUpdated", System.currentTimeMillis());

        String collection = "drivers";
        if ("machinery_provider".equalsIgnoreCase(role)) {
            collection = "machinery_providers";
        }

        db.collection(collection).document(uid).update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updated in Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
    }

    private String providerMachineryType = "";

    private void startListeningForRequests() {
        if (uid == null || uid.isEmpty()) return;

        String collectionName = "transport_requests";
        String assignedField = "assignedDriverId";
        
        if ("machinery_provider".equalsIgnoreCase(role)) {
            collectionName = "machinery_bookings";
            assignedField = "assignedProviderId";
        }
        
        final String typeExtra = "machinery_provider".equalsIgnoreCase(role) ? "MACHINERY" : "TRANSPORT";

        // Fetch provider's machineryType for open request filtering
        if ("machinery_provider".equalsIgnoreCase(role)) {
            db.collection("machinery_providers").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String type = doc.getString("machineryType");
                            if (type != null) providerMachineryType = type.toLowerCase().trim();
                            Log.d(TAG, "Provider machineryType = " + providerMachineryType);
                        }
                    });
        }

        requestListener = db.collection(collectionName)
                .whereEqualTo(assignedField, uid)
                .whereEqualTo("status", "REQUESTED")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            // Start IncomingRequestActivity
                            Intent popupIntent = new Intent(this, IncomingRequestActivity.class);
                            popupIntent.putExtra("REQUEST_ID", doc.getId());
                            popupIntent.putExtra("SERVICE_TYPE", typeExtra);
                            popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(popupIntent);
                            // We only process one at a time
                            break;
                        }
                    }
                });

        openRequestListener = db.collection(collectionName)
                .whereEqualTo(assignedField, null)
                .whereEqualTo("status", "REQUESTED")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Open Listen failed.", error);
                        return;
                    }
                    if (value != null && !value.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                com.google.firebase.firestore.DocumentSnapshot doc = dc.getDocument();

                                // For machinery providers, filter by machineryType match
                                if ("MACHINERY".equals(typeExtra) && !providerMachineryType.isEmpty()) {
                                    String reqType = doc.getString("machineryType");
                                    if (reqType == null || !reqType.toLowerCase().trim().equals(providerMachineryType)) {
                                        Log.d(TAG, "Skipping open request " + doc.getId() + " — type mismatch: " + reqType + " vs " + providerMachineryType);
                                        continue;
                                    }
                                }
                                
                                // Distance check
                                if (lastKnownLocation != null) {
                                    String latField = "TRANSPORT".equals(typeExtra) ? "sourceLat" : "farmerLat";
                                    String lngField = "TRANSPORT".equals(typeExtra) ? "sourceLng" : "farmerLng";
                                    Double rLat = doc.getDouble(latField);
                                    Double rLng = doc.getDouble(lngField);
                                    
                                    if (rLat != null && rLng != null) {
                                        double dist = GeoFireUtils.getDistanceBetween(
                                            new GeoLocation(rLat, rLng), 
                                            new GeoLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())
                                        );
                                        if (dist <= 25000) { // 25km trigger bubble
                                            Intent popupIntent = new Intent(this, IncomingRequestActivity.class);
                                            popupIntent.putExtra("REQUEST_ID", doc.getId());
                                            popupIntent.putExtra("SERVICE_TYPE", typeExtra);
                                            popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                            startActivity(popupIntent);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Dispatch Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (requestListener != null) {
            requestListener.remove();
        }
        if (openRequestListener != null) {
            openRequestListener.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
