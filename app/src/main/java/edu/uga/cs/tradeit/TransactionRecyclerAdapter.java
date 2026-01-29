package edu.uga.cs.tradeit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for the Transactions screen.
 *
 * It shows the user's transactions in three "modes":
 * - pending: items where the user is the buyer and the sale is not finished
 * - confirm: items where the user is the seller and needs to confirm the sale
 * - completed: finished transactions where the user was buyer or seller
 *
 * The UI behavior changes slightly based on which tab is active.
 */
public class TransactionRecyclerAdapter
        extends RecyclerView.Adapter<TransactionRecyclerAdapter.TransactionHolder> {

    /** List of items (transactions) to display. */
    private List<Item> itemsList;

    /** Which tab is currently active: "pending", "confirm", or "completed". */
    private String currentTab = "pending";

    /** ID of the currently logged-in user. */
    private String currentUId = FirebaseAuth.getInstance().getUid();

    /** Firebase reference to the categories node, used to look up category titles. */
    private DatabaseReference categoriesRef =
            FirebaseDatabase.getInstance().getReference("categories");

    /**
     * Cache from categoryId -> category title, so we do not fetch the same
     * category over and over from Firebase.
     */
    private Map<String, String> categoryCache = new HashMap<>();

    /**
     * Creates a new adapter.
     *
     * @param itemsList list of Item objects that represent transactions
     * @param currentUId ID of the current user
     * @param tab which tab is active at the moment: "pending", "confirm", or "completed"
     */
    public TransactionRecyclerAdapter(List<Item> itemsList,
                                      String currentUId,
                                      String tab) {
        this.itemsList = itemsList;
        this.currentUId = currentUId;
        this.currentTab = tab;
    }

    /**
     * Called when the fragment switches tabs.
     * We just update the current tab name and tell the RecyclerView to redraw.
     *
     * @param tab new tab name ("pending", "confirm", or "completed")
     */
    public void setCurrentTab(String tab) {
        currentTab = tab;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for one transaction row (card).
     * It holds references to views inside item_card.xml.
     */
    class TransactionHolder extends RecyclerView.ViewHolder {
        TextView name, person, category, price;
        Button actionButton;
        Button detailsButton;

        public TransactionHolder(@NonNull View itemView) {
            super(itemView);
            name          = itemView.findViewById(R.id.nameTextView);
            person        = itemView.findViewById(R.id.personTextView);
            category      = itemView.findViewById(R.id.categoryTextView);
            price         = itemView.findViewById(R.id.priceTextView);
            actionButton  = itemView.findViewById(R.id.actionButton);
            detailsButton = itemView.findViewById(R.id.detailsButton);
        }
    }

    /**
     * Creates a new row/card view when needed.
     */
    @NonNull
    @Override
    public TransactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new TransactionHolder(v);
    }

    /**
     * Fills a row with data from one Item and sets button behavior
     * based on the current tab (pending, confirm, or completed).
     */
    @Override
    public void onBindViewHolder(@NonNull TransactionHolder holder, int position) {
        Item item = itemsList.get(position);

        // Item name
        holder.name.setText(item.getName());

        // Price: show "Free" if zero, otherwise format to 2 decimals
        double p = item.getPrice();
        if (p == 0.0) {
            holder.price.setText("Price: Free");
        } else {
            holder.price.setText("Price: $" + String.format("%.2f", p));
        }

        // Category display: prefer stored categoryTitle (saved when completed),
        // otherwise fall back to live category node (if it still exists).
        String storedTitle = item.getCategoryTitle();
        String catId = item.getCategoryId();

        if (storedTitle != null && !storedTitle.isEmpty()) {
            // Completed items keep this even if category is deleted
            holder.category.setText("Category: " + storedTitle);
        } else if (catId == null || catId.isEmpty()) {
            holder.category.setText("Category: None");
        } else if (categoryCache.containsKey(catId)) {
            holder.category.setText("Category: " + categoryCache.get(catId));
        } else {
            categoriesRef.child(catId).child("title")
                    .get()
                    .addOnSuccessListener(snap -> {
                        String title = snap.getValue(String.class);
                        if (title == null) title = "Unknown";
                        categoryCache.put(catId, title);
                        holder.category.setText("Category: " + title);
                    })
                    .addOnFailureListener(e ->
                            holder.category.setText("Category: Unknown"));
        }

        // Reference to users node, used to show buyer/seller names
        DatabaseReference usersDbRef =
                FirebaseDatabase.getInstance().getReference("users");

        // By default, hide and disable the action button.
        holder.actionButton.setVisibility(View.GONE);
        holder.actionButton.setEnabled(false);
        holder.actionButton.setOnClickListener(null);

        // Behavior depends on which tab we are on.
        switch (currentTab) {
            case "pending":
                // User is the buyer; show the seller's name.
                usersDbRef.child(item.getSellerId()).child("name")
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            String seller = snapshot.getValue(String.class);
                            if (seller == null) seller = "Unknown";
                            holder.person.setText("Seller: " + seller);
                        });
                // Action is handled via detail screen, so no button here.
                break;

            case "confirm":
                // User is the seller; show the buyer's name.
                usersDbRef.child(item.getBuyerId()).child("name")
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            String buyer = snapshot.getValue(String.class);
                            if (buyer == null) buyer = "Unknown";
                            holder.person.setText("Buyer: " + buyer);
                        });

                holder.actionButton.setVisibility(View.VISIBLE);

                boolean buyerConf = item.isBuyerConfirmed();
                boolean sellerConf = item.isSellerConfirmed();

                if (sellerConf && !buyerConf) {
                    // Seller already confirmed, waiting on buyer.
                    holder.actionButton.setEnabled(false);
                    holder.actionButton.setText("Waiting for Buyer");
                } else if (sellerConf && buyerConf) {
                    // Both sides already confirmed, transaction completed.
                    holder.actionButton.setEnabled(false);
                    holder.actionButton.setText("Completed");
                } else {
                    // Seller can confirm the sale here.
                    holder.actionButton.setEnabled(true);
                    holder.actionButton.setText("Confirm Sale");

                    holder.actionButton.setOnClickListener(v -> {
                        DatabaseReference itemsDbRef =
                                FirebaseDatabase.getInstance().getReference("items");
                        DatabaseReference itemRef = itemsDbRef.child(item.getKey());

                        // Mark sellerConfirmed = true in Firebase.
                        itemRef.child("sellerConfirmed").setValue(true)
                                .addOnSuccessListener(aVoid ->
                                        itemRef.get().addOnSuccessListener(snap -> {
                                            Boolean bConf =
                                                    snap.child("buyerConfirmed").getValue(Boolean.class);
                                            Boolean sConf =
                                                    snap.child("sellerConfirmed").getValue(Boolean.class);

                                            if (bConf == null) bConf = false;
                                            if (sConf == null) sConf = false;

                                            item.setBuyerConfirmed(bConf);
                                            item.setSellerConfirmed(sConf);

                                            // If both sides confirmed, mark status completed
                                            // and store the category title on the Item itself.
                                            if (bConf && sConf) {
                                                String catTitleToSave = item.getCategoryTitle();

                                                // If we don't already have categoryTitle set,
                                                // try to pull it from the cache for this categoryId.
                                                if (catTitleToSave == null || catTitleToSave.isEmpty()) {
                                                    String catIdInner = item.getCategoryId();
                                                    if (catIdInner != null &&
                                                            categoryCache.containsKey(catIdInner)) {
                                                        catTitleToSave = categoryCache.get(catIdInner);
                                                    }
                                                }

                                                Map<String, Object> updates = new HashMap<>();
                                                updates.put("status", "completed");
                                                if (catTitleToSave != null && !catTitleToSave.isEmpty()) {
                                                    // Save the categoryTitle so completed items
                                                    // remember their category even if the category
                                                    // is later deleted.
                                                    updates.put("categoryTitle", catTitleToSave);
                                                }
                                                itemRef.updateChildren(updates);
                                            }

                                            // Update button label after confirm.
                                            holder.actionButton.setEnabled(false);
                                            if (bConf && sConf) {
                                                holder.actionButton.setText("Completed");
                                            } else {
                                                holder.actionButton.setText("Waiting for Buyer");
                                            }

                                            Toast.makeText(holder.itemView.getContext(),
                                                    "Your side confirmed",
                                                    Toast.LENGTH_SHORT).show();
                                        }));
                    });
                }
                break;

            case "completed":
                // Show the *other* party in the transaction.
                if (currentUId.equals(item.getBuyerId())) {
                    // You were the buyer -> show seller.
                    usersDbRef.child(item.getSellerId()).child("name")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                String seller = snapshot.getValue(String.class);
                                if (seller == null) seller = "Unknown";
                                holder.person.setText("Seller: " + seller);
                            });
                } else {
                    // You were the seller -> show buyer.
                    usersDbRef.child(item.getBuyerId()).child("name")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                String buyer = snapshot.getValue(String.class);
                                if (buyer == null) buyer = "Unknown";
                                holder.person.setText("Buyer: " + buyer);
                            });
                }
                break;
        }

        // Tap the card to open full details.
        holder.itemView.setOnClickListener(v -> {
            android.content.Context ctx = holder.itemView.getContext();
            android.content.Intent intent =
                    new android.content.Intent(ctx, ItemDetailActivity.class);
            intent.putExtra("itemKey", item.getKey());
            ctx.startActivity(intent);
        });

        // The "Details" button does the same thing as tapping the card.
        if (holder.detailsButton != null) {
            holder.detailsButton.setVisibility(View.VISIBLE);
            holder.detailsButton.setOnClickListener(v -> {
                android.content.Context ctx = holder.itemView.getContext();
                android.content.Intent intent =
                        new android.content.Intent(ctx, ItemDetailActivity.class);
                intent.putExtra("itemKey", item.getKey());
                ctx.startActivity(intent);
            });
        }
    }

    /**
     * Returns the number of items in the current list.
     */
    @Override
    public int getItemCount() {
        return itemsList.size();
    }
}
