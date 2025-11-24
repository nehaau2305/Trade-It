package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ItemDetailActivity extends AppCompatActivity {

    private TextView nameTV, categoryTV, priceTV, statusTV,
            sellerNameTV, sellerEmailTV, descTV;
    private Button actionButton;

    private DatabaseReference itemsRef;
    private DatabaseReference usersRef;

    private String itemKey;
    private Item currentItem;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        nameTV = findViewById(R.id.detailNameTextView);
        categoryTV = findViewById(R.id.detailCategoryTextView);
        priceTV = findViewById(R.id.detailPriceTextView);
        statusTV = findViewById(R.id.detailStatusTextView);
        sellerNameTV = findViewById(R.id.detailSellerNameTextView);
        sellerEmailTV = findViewById(R.id.detailSellerEmailTextView);
        descTV = findViewById(R.id.detailDescriptionTextView);
        actionButton = findViewById(R.id.detailActionButton);

        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        currentUid = FirebaseAuth.getInstance().getUid();

        itemKey = getIntent().getStringExtra("itemKey");
        if (itemKey == null) {
            Toast.makeText(this, "No item specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadItemDetails();
    }

    private void loadItemDetails() {
        itemsRef.child(itemKey)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Item item = snapshot.getValue(Item.class);
                    if (item == null) {
                        Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    item.setKey(itemKey);
                    currentItem = item;
                    bindItemToUI();
                    loadSellerInfo(item.getSellerId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load item", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindItemToUI() {
        nameTV.setText(currentItem.getName());

        String catText = currentItem.getCategoryId();
        if (catText == null) catText = "No category";
        categoryTV.setText("Category: " + catText); // (still ID, but fine – list pages show title)

        double p = currentItem.getPrice();
        if (p == 0.0) {
            priceTV.setText("Price: Free");
        } else {
            priceTV.setText("Price: $" + String.format("%.2f", p));
        }

        String status = currentItem.getStatus();
        if (status == null) status = "unknown";
        statusTV.setText("Status: " + status);

        String desc = currentItem.getDescription();
        if (desc == null || desc.isEmpty()) {
            descTV.setText("No description provided.");
        } else {
            descTV.setText(desc);
        }

        setupActionButton();
    }

    private void loadSellerInfo(String sellerId) {
        if (sellerId == null) {
            sellerNameTV.setText("Seller: Unknown");
            sellerEmailTV.setText("Email: Unknown");
            return;
        }

        usersRef.child(sellerId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.getValue(User.class);
                    if (user == null) {
                        sellerNameTV.setText("Seller: Unknown");
                        sellerEmailTV.setText("Email: Unknown");
                    } else {
                        String name = user.getName() != null ? user.getName() : "Unknown";
                        String email = user.getEmail() != null ? user.getEmail() : "Unknown";
                        sellerNameTV.setText("Seller: " + name);
                        sellerEmailTV.setText("Email: " + email);
                    }
                })
                .addOnFailureListener(e -> {
                    sellerNameTV.setText("Seller: Unknown");
                    sellerEmailTV.setText("Email: Unknown");
                });
    }

    private void setupActionButton() {
        actionButton.setEnabled(false);
        actionButton.setVisibility(Button.GONE);

        if (currentUid == null || currentItem == null) return;

        String status = currentItem.getStatus();
        String sellerId = currentItem.getSellerId();
        String buyerId = currentItem.getBuyerId();

        boolean isSeller = currentUid.equals(sellerId);
        boolean isBuyer = buyerId != null && currentUid.equals(buyerId);

        if ("available".equals(status)) {
            if (!isSeller) {
                actionButton.setVisibility(Button.VISIBLE);
                actionButton.setEnabled(true);
                actionButton.setText("Request Item");
                actionButton.setOnClickListener(v -> requestItem());
            }
        } else if ("pending".equals(status)) {
            if (!(isSeller || isBuyer)) {
                // neither party – no button
                return;
            }

            boolean buyerConf = currentItem.isBuyerConfirmed();
            boolean sellerConf = currentItem.isSellerConfirmed();

            actionButton.setVisibility(Button.VISIBLE);

            if (isSeller) {
                if (sellerConf && !buyerConf) {
                    actionButton.setEnabled(false);
                    actionButton.setText("Waiting for Buyer");
                } else if (sellerConf && buyerConf) {
                    // should already be completed but just in case
                    actionButton.setVisibility(Button.GONE);
                } else {
                    actionButton.setEnabled(true);
                    actionButton.setText("Complete Transaction");
                    actionButton.setOnClickListener(v -> completeTransaction());
                }
            } else { // isBuyer
                if (buyerConf && !sellerConf) {
                    actionButton.setEnabled(false);
                    actionButton.setText("Waiting for Seller");
                } else if (buyerConf && sellerConf) {
                    actionButton.setVisibility(Button.GONE);
                } else {
                    actionButton.setEnabled(true);
                    actionButton.setText("Complete Transaction");
                    actionButton.setOnClickListener(v -> completeTransaction());
                }
            }
        } else if ("completed".equals(status)) {
            actionButton.setVisibility(Button.GONE);
        }
    }

    private void requestItem() {
        if (currentUid == null || currentItem == null) return;

        DatabaseReference itemRef = itemsRef.child(currentItem.getKey());

        itemRef.child("buyerId").setValue(currentUid);
        itemRef.child("status").setValue("pending");
        itemRef.child("buyerConfirmed").setValue(false);
        itemRef.child("sellerConfirmed").setValue(false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Item requested",
                            Toast.LENGTH_SHORT).show();
                    currentItem.setBuyerId(currentUid);
                    currentItem.setStatus("pending");
                    currentItem.setBuyerConfirmed(false);
                    currentItem.setSellerConfirmed(false);
                    bindItemToUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to request item",
                                Toast.LENGTH_SHORT).show());
    }

    private void completeTransaction() {
        if (currentItem == null || currentUid == null) return;

        DatabaseReference itemRef = itemsRef.child(currentItem.getKey());

        boolean isSeller = currentUid.equals(currentItem.getSellerId());
        String flagField = isSeller ? "sellerConfirmed" : "buyerConfirmed";

        itemRef.child(flagField).setValue(true)
                .addOnSuccessListener(aVoid ->
                        itemRef.get().addOnSuccessListener(snap -> {
                            Boolean buyerConf =
                                    snap.child("buyerConfirmed").getValue(Boolean.class);
                            Boolean sellerConf =
                                    snap.child("sellerConfirmed").getValue(Boolean.class);

                            if (buyerConf == null) buyerConf = false;
                            if (sellerConf == null) sellerConf = false;

                            currentItem.setBuyerConfirmed(buyerConf);
                            currentItem.setSellerConfirmed(sellerConf);

                            if (buyerConf && sellerConf) {
                                itemRef.child("status").setValue("completed");
                                currentItem.setStatus("completed");
                                Toast.makeText(this,
                                        "Transaction completed",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                String other = isSeller ? "buyer" : "seller";
                                Toast.makeText(this,
                                        "Your side is confirmed. Waiting for " + other + ".",
                                        Toast.LENGTH_SHORT).show();
                            }

                            bindItemToUI();
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to confirm transaction",
                                Toast.LENGTH_SHORT).show());
    }
}
