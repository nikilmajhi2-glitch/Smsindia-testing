package com.rupeedesk.smsaautosender;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";

    public static void checkAndSendMessages(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference smsCollection = db.collection("smsInventory");

        smsCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String recipient = document.getString("number");
                    String message = document.getString("message");
                    String userId = document.getString("userId");

                    if (recipient != null && message != null && userId != null &&
                        !recipient.isEmpty() && !message.isEmpty() && !userId.isEmpty()) {

                        Log.d(TAG, "📩 Sending SMS to: " + recipient + " -> " + message);
                        boolean sent = SmsUtils.sendSms(context, recipient, message);

                        if (sent) {
                            deductCredit(userId);
                            document.getReference().delete();
                            Log.d(TAG, "✅ SMS sent and credit deducted from user " + userId);
                        } else {
                            Log.w(TAG, "⚠️ Failed to send SMS to: " + recipient);
                        }
                    } else {
                        Log.w(TAG, "⚠️ Invalid document fields: " + document.getId());
                    }
                }
            } else {
                Log.e(TAG, "❌ Error fetching SMS documents: ", task.getException());
            }
        });
    }

    private static void deductCredit(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
          .document(userId)
          .update("balance", com.google.firebase.firestore.FieldValue.increment(-0.20))
          .addOnSuccessListener(aVoid -> Log.d(TAG, "💰 Credit deducted for user: " + userId))
          .addOnFailureListener(e -> Log.e(TAG, "❌ Credit deduction failed for user: " + userId, e));
    }
}