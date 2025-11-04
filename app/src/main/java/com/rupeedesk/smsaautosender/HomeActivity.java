package com.rupeedesk.smsaautosender;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeActivity extends AppCompatActivity {

    private TextView balanceTextView;
    private EditText bankNameInput, accountNumberInput, withdrawalAmountInput;
    private Button addBankButton, withdrawButton;
    private ListView withdrawalHistoryList;

    private FirebaseFirestore db;
    private String userId;
    private DocumentReference userDocRef;
    private CollectionReference withdrawalsRef;

    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        balanceTextView = findViewById(R.id.balanceTextView);
        bankNameInput = findViewById(R.id.bankNameInput);
        accountNumberInput = findViewById(R.id.accountNumberInput);
        withdrawalAmountInput = findViewById(R.id.withdrawalAmountInput);
        addBankButton = findViewById(R.id.addBankButton);
        withdrawButton = findViewById(R.id.withdrawButton);
        withdrawalHistoryList = findViewById(R.id.withdrawalHistoryList);

        db = FirebaseFirestore.getInstance();

        userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        userDocRef = db.collection("users").document(userId);
        withdrawalsRef = db.collection("withdrawals");

        loadUserData();
        loadWithdrawalHistory();

        addBankButton.setOnClickListener(v -> saveBankDetails());
        withdrawButton.setOnClickListener(v -> makeWithdrawal());
    }

    private void loadUserData() {
        userDocRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Double balance = snapshot.getDouble("balance");
                Map<String, Object> bankDetails = (Map<String, Object>) snapshot.get("bankDetails");

                balanceTextView.setText(String.format(Locale.getDefault(), "Balance: ₹ %.2f", (balance != null ? balance : 0.0)));

                if (bankDetails != null) {
                    bankNameInput.setText((String) bankDetails.get("bankName"));
                    accountNumberInput.setText((String) bankDetails.get("accountNumber"));
                }
            }
        });
    }

    private void saveBankDetails() {
        String bankName = bankNameInput.getText().toString().trim();
        String accountNumber = accountNumberInput.getText().toString().trim();

        if (bankName.isEmpty() || accountNumber.isEmpty()) {
            Toast.makeText(this, "Please fill both bank name and account number", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> bankDetails = new HashMap<>();
        bankDetails.put("bankName", bankName);
        bankDetails.put("accountNumber", accountNumber);

        userDocRef.update("bankDetails", bankDetails)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Bank details saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save bank details", e);
                    Toast.makeText(this, "Failed to save bank details", Toast.LENGTH_SHORT).show();
                });
    }

    private void makeWithdrawal() {
        String amountStr = withdrawalAmountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter withdrawal amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount must be positive", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check balance before allowing withdrawal
        userDocRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Double balance = snapshot.getDouble("balance");
                if (balance == null || balance < amount) {
                    Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create withdrawal record
                Map<String, Object> withdrawal = new HashMap<>();
                withdrawal.put("userId", userId);
                withdrawal.put("amount", amount);
                withdrawal.put("timestamp", FieldValue.serverTimestamp());
                withdrawal.put("status", "Pending");

                withdrawalsRef.add(withdrawal).addOnSuccessListener(docRef -> {
                    // Deduct balance
                    userDocRef.update("balance", FieldValue.increment(-amount));
                    Toast.makeText(this, "Withdrawal requested", Toast.LENGTH_SHORT).show();
                    withdrawalAmountInput.setText("");
                    loadWithdrawalHistory();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to request withdrawal", e);
                    Toast.makeText(this, "Withdrawal request failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadWithdrawalHistory() {
        Query query = withdrawalsRef.whereEqualTo("userId", userId).orderBy("timestamp", Query.Direction.DESCENDING).limit(20);

        query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }

            if (snapshots != null) {
                List<String> historyItems = new ArrayList<>();

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

                for (QueryDocumentSnapshot doc : snapshots) {
                    Double amount = doc.getDouble("amount");
                    Date timestamp = doc.getDate("timestamp");
                    String status = doc.getString("status");
                    String dateStr = timestamp != null ? sdf.format(timestamp) : "Unknown date";

                    historyItems.add(String.format(Locale.getDefault(), "₹%.2f on %s - %s", amount != null ? amount : 0, dateStr, status));
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyItems);
                withdrawalHistoryList.setAdapter(adapter);
            }
        });
    }
}