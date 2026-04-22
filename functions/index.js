const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNotification = functions.firestore
  .document("notifications/{docId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    if (!data) return null;

    const topic = data.topic; // Ex: 'farmer_123' or 'available_drivers'
    const title = data.title;
    const message = data.message;

    if (!topic || !title || !message) {
      console.log("Missing data, skipping notification.");
      return null;
    }

    const payload = {
      notification: {
        title: title,
        body: message,
      },
      topic: topic,
    };

    try {
      const response = await admin.messaging().send(payload);
      console.log("Successfully sent message:", response);
      
      // Optionally delete the document after sending to reduce storage usage
      await snap.ref.delete();
      return response;
    } catch (error) {
      console.error("Error sending message:", error);
      return null;
    }
  });
