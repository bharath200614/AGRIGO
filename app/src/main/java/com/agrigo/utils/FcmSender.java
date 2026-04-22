package com.agrigo.utils;

/**
 * FcmSender - Push notification helper.
 * 
 * NOTE: Push notifications are DISABLED for the Spark (free) plan.
 * Cloud Functions are required to process the notification queue.
 * This class is kept as a no-op so callers don't need to be restructured.
 * Re-enable when upgrading to Firebase Blaze plan.
 */
public class FcmSender {

    public static void pushNotification(String topicOrToken, String title, String message) {
        // NO-OP: Push notifications disabled on Spark plan.
        // When Cloud Functions are deployed on Blaze plan, uncomment the Firestore write below:
        //
        // if (topicOrToken == null || topicOrToken.isEmpty() || title == null || message == null) return;
        // try {
        //     FirebaseFirestore db = FirebaseFirestore.getInstance();
        //     Map<String, Object> notificationData = new HashMap<>();
        //     notificationData.put("topic", topicOrToken);
        //     notificationData.put("title", title);
        //     notificationData.put("message", message);
        //     notificationData.put("timestamp", System.currentTimeMillis());
        //     db.collection("notifications").add(notificationData);
        // } catch (Exception e) { /* silent */ }
    }
}
