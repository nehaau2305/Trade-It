package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * This screen shows all the details about a single item.
 *
 * It:
 * - loads the item data from Firebase
 * - shows name, category, price, status, seller, and description
 * - lets the seller edit or delete the item (if available)
 * - lets a buyer request the item and complete the transaction
 */
public class ItemDetailActivity extends AppCompatActivity {

    // text fields on the screen
    private TextView nameTV, categoryTV, priceTV, statusTV,
            sellerNameTV, sellerEmailTV, descTV;
    // buttons for actions
    private Button actionButton, editButton, deleteButton;

    // references to different parts of the Firebase database
    private DatabaseReference itemsRef;
    private DatabaseReference usersRef;
    private DatabaseReference categoriesRef;

    // key of the item we are showing
    private String itemKey;
    // the current Item object we loaded
    private Item currentItem;
    // id of the currently logged-in user
    private String currentUid;

    /**
     * Called when this screen is created.
     * Sets up the layout, finds all views, connects to Firebase,
     * and starts loading the item details.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // connect Java variables to views in the layout
        nameTV = findViewById(R.id.detailNameTextView);
        categoryTV = findViewById(R.id.detailCategoryTextView);
        priceTV = findViewById(R.id.detailPriceTextView);
        statusTV = findViewById(R.id.detailStatusTextView);
        sellerNameTV = findViewById(R.id.detailSellerNameTextView);
        sellerEmailTV = findViewById(R.id.detailSellerEmailTextView);
        descTV = findViewById(R.id.detailDescriptionTextView);
        actionButton = findViewById(R.id.detailActionButton);
        editButton = findViewById(R.id.detailEditButton);
        deleteButton = findViewById(R.id.detailDeleteButton);

        // get database references
        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        categoriesRef = FirebaseDatabase.getInstance().getReference("categories");
        currentUid = FirebaseAuth.getInstance().getUid();

        // get the itemKey passed from the previous screen
        itemKey = getIntent().getStringExtra("itemKey");
        if (itemKey == null) {
            Toast.makeText(this, "No item specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadItemDetails();
    }

    /**
     * Small helper to reload the item info.
     * This is called after editing so the screen refreshes.
     */
    public void reloadItem() {
        loadItemDetails();
    }

    /**
     * Loads the item from Firebase using itemKey,
     * then binds it to the screen and loads seller info.
     */
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

    /**
     * Takes the currentItem data and shows it in the TextViews.
     * Also sets up buttons based on who the user is and item status.
     */
    private void bindItemToUI() {
        if (currentItem == null) return;

        nameTV.setText(currentItem.getName() == null ? "" : currentItem.getName());

        // show category name (look it up from the category id)
        String catId = currentItem.getCategoryId();
        if (catId == null || catId.isEmpty()) {
            categoryTV.setText("Category: None");
        } else {
            categoriesRef.child(catId).child("title")
                    .get()
                    .addOnSuccessListener(snap -> {
                        String title = snap.getValue(String.class);
                        if (title == null) title = "Unknown";
                        categoryTV.setText("Category: " + title);
                    })
                    .addOnFailureListener(e ->
                            categoryTV.setText("Category: Unknown"));
        }

        // show price or "Free"
        double p = currentItem.getPrice();
        if (p == 0.0) {
            priceTV.setText("Price: Free");
        } else {
            priceTV.setText("Price: $" + String.format("%.2f", p));
        }

        // show status (available / pending / completed)
        String status = currentItem.getStatus();
        if (status == null) status = "unknown";
        statusTV.setText("Status: " + status);

        // show description or default text
        String desc = currentItem.getDescription();
        if (desc == null || desc.isEmpty()) {
            descTV.setText("No description provided.");
        } else {
            descTV.setText(desc);
        }

        // decide if the edit/delete buttons should be visible
        setupOwnerButtons();
        // decide what the main action button should do
        setupActionButton();
    }

    /**
     * Loads the seller's name and email from Firebase
     * and shows them under the item details.
     */
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

    /**
     * Shows or hides the Edit and Delete buttons depending on:
     * - if the current user is the seller
     * - if the item is still available
     */
    private void setupOwnerButtons() {
        if (editButton == null || deleteButton == null || currentItem == null) return;

        String status = currentItem.getStatus();
        String sellerId = currentItem.getSellerId();
        boolean isSeller = currentUid != null && currentUid.equals(sellerId);
        boolean isAvailable = "available".equals(status);

        if (isSeller && isAvailable) {
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);

            // open the edit dialog when seller taps edit
            editButton.setOnClickListener(v -> {
                EditItemDialogFragment dialog =
                        EditItemDialogFragment.newInstance(currentItem.getKey());
                dialog.show(getSupportFragmentManager(), "EditItemDialog");
            });

            // let seller delete the item
            deleteButton.setOnClickListener(v -> deleteItem());
        } else {
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up the big action button at the bottom.
     * What it does depends on:
     * - item status (available/pending/completed)
     * - if user is seller, buyer, or someone else
     */
    private void setupActionButton() {
        actionButton.setEnabled(false);
        actionButton.setVisibility(View.GONE);

        if (currentUid == null || currentItem == null) return;

        String status = currentItem.getStatus();
        String sellerId = currentItem.getSellerId();
        String buyerId = currentItem.getBuyerId();

        boolean isSeller = currentUid.equals(sellerId);
        boolean isBuyer = buyerId != null && currentUid.equals(buyerId);

        // if item is free to request
        if ("available".equals(status)) {
            if (!isSeller) {
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setEnabled(true);
                actionButton.setText("Request Item");
                actionButton.setOnClickListener(v -> requestItem());
            }
        } else if ("pending".equals(status)) {
            // only seller or buyer should see controls in pending state
            if (!(isSeller || isBuyer)) {
                return;
            }

            boolean buyerConf = currentItem.isBuyerConfirmed();
            boolean sellerConf = currentItem.isSellerConfirmed();

            actionButton.setVisibility(View.VISIBLE);

            if (isSeller) {
                if (sellerConf && !buyerConf) {
                    actionButton.setEnabled(false);
                    actionButton.setText("Waiting for Buyer");
                } else if (sellerConf && buyerConf) {
                    actionButton.setVisibility(View.GONE);
                } else {
                    actionButton.setEnabled(true);
                    actionButton.setText("Complete Transaction");
                    actionButton.setOnClickListener(v -> completeTransaction());
                }
            } else {
                if (buyerConf && !sellerConf) {
                    actionButton.setEnabled(false);
                    actionButton.setText("Waiting for Seller");
                } else if (buyerConf && sellerConf) {
                    actionButton.setVisibility(View.GONE);
                } else {
                    actionButton.setEnabled(true);
                    actionButton.setText("Complete Transaction");
                    actionButton.setOnClickListener(v -> completeTransaction());
                }
            }
        } else if ("completed".equals(status)) {
            // nothing to do if already completed
            actionButton.setVisibility(View.GONE);
        }
    }

    /**
     * Called when a buyer taps "Request Item".
     * It marks the item as pending and sets the buyer in Firebase.
     */
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
                    // update local copy and refresh UI
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

    /**
     * Called when buyer or seller taps "Complete Transaction".
     * It sets their confirmed flag. If both sides are confirmed,
     * the item becomes "completed".
     */
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

    /**
     * Lets the seller delete the item, but only if it's still "available".
     * If delete works, the activity closes and goes back to previous screen.
     */
    private void deleteItem() {
        if (currentItem == null) return;

        if (!"available".equals(currentItem.getStatus())) {
            Toast.makeText(this,
                    "Only available items can be deleted",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        itemsRef.child(currentItem.getKey())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Item deleted",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to delete item",
                                Toast.LENGTH_SHORT).show());
    }
}
