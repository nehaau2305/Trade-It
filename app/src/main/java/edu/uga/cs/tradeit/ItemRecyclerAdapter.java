package edu.uga.cs.tradeit;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemRecyclerAdapter extends RecyclerView.Adapter<ItemRecyclerAdapter.ItemHolder>{

    // list of items to show
    private List<Item> itemsList;

    public ItemRecyclerAdapter(List<Item> itemsList) {
        this.itemsList = itemsList;
    }

    // holds references to views inside each card
    class ItemHolder extends RecyclerView.ViewHolder {
        TextView name, seller, category, price;
        Button buyB;

        public ItemHolder(View itemView) {
            super(itemView);
            name     = itemView.findViewById(R.id.nameTextView);
            seller   = itemView.findViewById(R.id.sellerTextView);
            category = itemView.findViewById(R.id.categoryTextView);
            price    = itemView.findViewById(R.id.priceTextView);
            buyB     = itemView.findViewById(R.id.buyButton);
        }
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {

        Item item = itemsList.get(position);

        // ---------- name ----------
        holder.name.setText(item.getName());

        // ---------- price (show "Free" when price is 0) ----------
        double p = item.getPrice();
        if (p == 0.0) {
            holder.price.setText("Free");
        } else {
            holder.price.setText("$" + String.format("%.2f", p));
        }

        // ----------category: show human-readable title, not ID ----------
        String categoryText = item.getCategoryTitle();
        if (categoryText == null || categoryText.isEmpty()) {
            categoryText = item.getCategoryId();
        }
        holder.category.setText(categoryText);

        // ---------- seller name ----------
        DatabaseReference usersDbRef = FirebaseDatabase.getInstance()
                .getReference("users");

        usersDbRef.child(item.getSellerId()).child("name")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String sellerName = snapshot.getValue(String.class);
                    if (sellerName == null) sellerName = "Unknown";
                    holder.seller.setText("Seller: " + sellerName);
                });

        // ---------- buy button state ----------
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null && currentUid.equals(item.getSellerId())) {
            // viewing own item -> cannot buy
            holder.buyB.setEnabled(false);
            holder.buyB.setText("Your Item");
        } else {
            holder.buyB.setEnabled(true);
            holder.buyB.setText("Buy Item");
        }

        // ---------- buy button click  ----------
        DatabaseReference itemsDbRef = FirebaseDatabase.getInstance()
                .getReference("items");

        holder.buyB.setOnClickListener(v -> {
            holder.buyB.setEnabled(false);

            String buyerId = FirebaseAuth.getInstance().getUid();
            if (buyerId == null) {
                Toast.makeText(holder.itemView.getContext(),
                        "Please log in again", Toast.LENGTH_SHORT).show();
                holder.buyB.setEnabled(true);
                return;
            }

            // only update buyerId + status so won't overwrite whole object
            Map<String, Object> updates = new HashMap<>();
            updates.put("buyerId", buyerId);
            updates.put("status", "pending");

            itemsDbRef.child(item.getKey())
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(holder.itemView.getContext(),
                                "Item Requested", Toast.LENGTH_SHORT).show();

                        // remove from list immediately
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            itemsList.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(),
                                "Request Failed", Toast.LENGTH_SHORT).show();
                        holder.buyB.setEnabled(true);
                    });
        });
    }

    @Override
    public int getItemCount() {
        return (itemsList == null) ? 0 : itemsList.size();
    }
}
