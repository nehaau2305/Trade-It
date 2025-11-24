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

import java.util.List;

public class ItemRecyclerAdapter extends RecyclerView.Adapter<ItemRecyclerAdapter.ItemHolder> {

    // list of items to show
    private List<Item> itemsList;

    public ItemRecyclerAdapter(List<Item> itemsList) {
        this.itemsList = itemsList;
    }

    // holds references to views inside each card
    class ItemHolder extends RecyclerView.ViewHolder {
        TextView name, seller, category, price;
        Button viewButton;   // use as "View" button

        public ItemHolder(View itemView) {
            super(itemView);
            name       = itemView.findViewById(R.id.nameTextView);
            seller     = itemView.findViewById(R.id.personTextView);
            category   = itemView.findViewById(R.id.categoryTextView);
            price      = itemView.findViewById(R.id.priceTextView);
            viewButton = itemView.findViewById(R.id.actionButton);
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

        // ---------- category: look up title via categories/{id}/title ----------
        DatabaseReference catRef = FirebaseDatabase.getInstance()
                .getReference("categories");

        String catId = item.getCategoryId();
        if (catId == null || catId.isEmpty()) {
            holder.category.setText("No category");
        } else {
            catRef.child(catId).child("title")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String title = snapshot.getValue(String.class);
                        if (title == null || title.isEmpty()) {
                            holder.category.setText("Unknown category");
                        } else {
                            holder.category.setText(title);
                        }
                    })
                    .addOnFailureListener(e ->
                            holder.category.setText("Unknown category"));
        }

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

        // ---------- VIEW button: open detail screen ----------
        holder.viewButton.setEnabled(true);
        holder.viewButton.setText("View");

        holder.viewButton.setOnClickListener(v -> {
            String key = item.getKey();
            if (key == null || key.isEmpty()) {
                Toast.makeText(holder.itemView.getContext(),
                        "Item is missing key", Toast.LENGTH_SHORT).show();
                return;
            }

            android.content.Context ctx = holder.itemView.getContext();
            android.content.Intent intent =
                    new android.content.Intent(ctx, ItemDetailActivity.class);
            intent.putExtra("itemKey", key);
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return (itemsList == null) ? 0 : itemsList.size();
    }
}
