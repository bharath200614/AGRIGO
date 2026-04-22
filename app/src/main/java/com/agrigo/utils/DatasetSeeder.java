package com.agrigo.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
// Removed Timber import
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetSeeder {

    private static final String COLLECTION_NAME = "crop_vehicle_dataset";

    public interface SeedCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void seedData(Context context, SeedCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        List<Map<String, Object>> dataset = getDataset();

        for (Map<String, Object> data : dataset) {
            batch.set(db.collection(COLLECTION_NAME).document(), data);
        }

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Firestore seeding successful
                    if (callback != null) callback.onSuccess();
                } else {
                    // Firestore seeding failed
                    if (callback != null) callback.onFailure(task.getException());
                }
            }
        });
    }

    private static List<Map<String, Object>> getDataset() {
        List<Map<String, Object>> list = new ArrayList<>();

        // Rice
        list.add(createEntry("Rice", 220, "Auto"));
        list.add(createEntry("Rice", 780, "Mini Truck"));
        list.add(createEntry("Rice", 1450, "Pickup Truck"));
        list.add(createEntry("Rice", 3200, "Large Truck"));

        // Wheat
        list.add(createEntry("Wheat", 300, "Auto"));
        list.add(createEntry("Wheat", 850, "Pickup Truck"));
        list.add(createEntry("Wheat", 1600, "Truck"));
        list.add(createEntry("Wheat", 4100, "Large Truck"));

        // Maize
        list.add(createEntry("Maize", 250, "Auto"));
        list.add(createEntry("Maize", 900, "Pickup Truck"));
        list.add(createEntry("Maize", 1700, "Truck"));
        list.add(createEntry("Maize", 3500, "Large Truck"));

        // Mosambi
        list.add(createEntry("Mosambi", 50, "Auto / Small Van"));
        list.add(createEntry("Mosambi", 200, "Mini Truck"));
        list.add(createEntry("Mosambi", 500, "Pickup Truck"));
        list.add(createEntry("Mosambi", 1100, "Truck"));
        list.add(createEntry("Mosambi", 3250, "Large Truck"));

        // Barley
        list.add(createEntry("Barley", 180, "Auto"));
        list.add(createEntry("Barley", 620, "Mini Truck"));
        list.add(createEntry("Barley", 1300, "Pickup Truck"));
        list.add(createEntry("Barley", 2800, "Truck"));

        // Jowar
        list.add(createEntry("Jowar", 270, "Auto"));
        list.add(createEntry("Jowar", 700, "Mini Truck"));
        list.add(createEntry("Jowar", 1500, "Truck"));
        list.add(createEntry("Jowar", 3000, "Large Truck"));

        // Bajra
        list.add(createEntry("Bajra", 200, "Auto"));
        list.add(createEntry("Bajra", 750, "Mini Truck"));
        list.add(createEntry("Bajra", 1200, "Pickup Truck"));
        list.add(createEntry("Bajra", 2600, "Truck"));

        // Ragi
        list.add(createEntry("Ragi", 240, "Auto"));
        list.add(createEntry("Ragi", 800, "Mini Truck"));
        list.add(createEntry("Ragi", 1400, "Pickup Truck"));
        list.add(createEntry("Ragi", 3100, "Large Truck"));

        // Foxtail Millet
        list.add(createEntry("Foxtail Millet", 260, "Auto"));
        list.add(createEntry("Foxtail Millet", 720, "Mini Truck"));
        list.add(createEntry("Foxtail Millet", 1350, "Pickup Truck"));
        list.add(createEntry("Foxtail Millet", 2900, "Truck"));

        // Little Millet
        list.add(createEntry("Little Millet", 210, "Auto"));
        list.add(createEntry("Little Millet", 680, "Mini Truck"));
        list.add(createEntry("Little Millet", 1250, "Pickup Truck"));
        list.add(createEntry("Little Millet", 2700, "Truck"));

        return list;
    }

    private static Map<String, Object> createEntry(String cropType, double weight, String vehicleType) {
        Map<String, Object> map = new HashMap<>();
        map.put("CropType", cropType);
        map.put("Weight", weight);
        map.put("VehicleType", vehicleType);
        return map;
    }
}
