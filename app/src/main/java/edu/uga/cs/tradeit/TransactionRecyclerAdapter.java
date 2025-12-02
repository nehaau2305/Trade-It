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

public class TransactionRecyclerAdapter
        extends RecyclerView.Adapter<TransactionRecyclerAdapter.TransactionHolder> {

    private List<Item> itemsList;
    private String currentTab = "pending";
    private String currentUId = FirebaseAuth.getInstance().getUid();

    private DatabaseReference categoriesRef =
            FirebaseDatabase.getInstance().getReference("categories");
    private Map<String, String> categoryCache = new HashMap<>();

    public TransactionRecyclerAdapter(List<Item> itemsList,
                                      String currentUId,
                                      String tab) {
        this.itemsList = itemsList;
        this.currentUId = currentUId;
        this.currentTab = tab;
    }

    public void setCurrentTab(String tab) {
        currentTab = tab;
        notifyDataSetChanged();
    }

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

    @NonNull
    @Override
    public TransactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new TransactionHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionHolder holder, int position) {
        Item item = itemsList.get(position);

        holder.name.setText(item.getName());

        double p = item.getPrice();
        if (p == 0.0) {
            holder.price.setText("Price: Free");
        } else {
            holder.price.setText("Price: $" + String.format("%.2f", p));
        }

        String storedTitle = item.getCategoryTitle();
        String catId = item.getCategoryId();

        if (storedTitle != null && !storedTitle.isEmpty()) {
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

        DatabaseReference usersDbRef =
                FirebaseDatabase.getInstance().getReference("users");

        // reset action button by default
        holder.actionButton.setVisibility(View.GONE);
        holder.actionButton.setEnabled(false);
        holder.actionButton.setOnClickListener(null);

        switch (currentTab) {
            case "pending":
                // items where current user is buyer & status = pending
                usersDbRef.child(item.getSellerId()).child("name")
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            String seller = snapshot.getValue(String.class);
                            if (seller == null) seller = "Unknown";
                            holder.person.setText("Seller: " + seller);
                        });
                // action handled via detail page
                break;

            case "confirm":
                // seller view – confirm sale
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
                    holder.actionButton.setEnabled(false);
                    holder.actionButton.setText("Waiting for Buyer");
                } else if (sellerConf && buyerConf) {
                    holder.actionButton.setEnabled(false);
                    holder.actionButton.setText("Completed");
                } else {
                    holder.actionButton.setEnabled(true);
                    holder.actionButton.setText("Confirm Sale");

                    holder.actionButton.setOnClickListener(v -> {
                        DatabaseReference itemsDbRef =
                                FirebaseDatabase.getInstance().getReference("items");
                        DatabaseReference itemRef = itemsDbRef.child(item.getKey());

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

                                            if (bConf && sConf) {
                                                itemRef.child("status").setValue("completed");
                                            }

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

                if (currentUId.equals(item.getBuyerId())) {
                    usersDbRef.child(item.getSellerId()).child("name")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                String seller = snapshot.getValue(String.class);
                                if (seller == null) seller = "Unknown";
                                holder.person.setText("Seller: " + seller);
                            });
                } else {
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

        holder.itemView.setOnClickListener(v -> {
            android.content.Context ctx = holder.itemView.getContext();
            android.content.Intent intent =
                    new android.content.Intent(ctx, ItemDetailActivity.class);
            intent.putExtra("itemKey", item.getKey());
            ctx.startActivity(intent);
        });

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

    @Override
    public int getItemCount() {
        return itemsList.size();
    }
}
