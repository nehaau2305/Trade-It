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

public class ItemRecyclerAdapter extends RecyclerView.Adapter<ItemRecyclerAdapter.ItemHolder> {

    private List<Item> itemsList;

    private DatabaseReference usersRef =
            FirebaseDatabase.getInstance().getReference("users");
    private DatabaseReference itemsRef =
            FirebaseDatabase.getInstance().getReference("items");
    private DatabaseReference categoriesRef =
            FirebaseDatabase.getInstance().getReference("categories");

    private Map<String, String> categoryCache = new HashMap<>();

    public ItemRecyclerAdapter(List<Item> itemsList) {
        this.itemsList = itemsList;
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        TextView name, person, category, price;
        Button actionButton, detailsButton;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.nameTextView);
            person = itemView.findViewById(R.id.personTextView);
            category = itemView.findViewById(R.id.categoryTextView);
            price = itemView.findViewById(R.id.priceTextView);
            actionButton = itemView.findViewById(R.id.actionButton);
            detailsButton = itemView.findViewById(R.id.detailsButton);
        }
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
        Item item = itemsList.get(position);

        holder.name.setText(item.getName());

        double p = item.getPrice();
        holder.price.setText(p == 0.0
                ? "Price: Free"
                : "Price: $" + String.format("%.2f", p));

        usersRef.child(item.getSellerId()).child("name")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String seller = snapshot.getValue(String.class);
                    if (seller == null) seller = "Unknown";
                    holder.person.setText("Seller: " + seller);
                });

        String catId = item.getCategoryId();
        if (catId == null || catId.isEmpty()) {
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

        holder.detailsButton.setVisibility(View.VISIBLE);
        holder.detailsButton.setOnClickListener(v -> {
            android.content.Context ctx = holder.itemView.getContext();
            android.content.Intent intent =
                    new android.content.Intent(ctx, ItemDetailActivity.class);
            intent.putExtra("itemKey", item.getKey());
            ctx.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> holder.detailsButton.performClick());

        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null && currentUid.equals(item.getSellerId())) {
            holder.actionButton.setVisibility(View.VISIBLE);
            holder.actionButton.setEnabled(false);
            holder.actionButton.setText("Your Item");
        } else {
            holder.actionButton.setVisibility(View.VISIBLE);
            holder.actionButton.setEnabled(true);
            holder.actionButton.setText("Request");

            holder.actionButton.setOnClickListener(v -> {
                if (currentUid == null) {
                    Toast.makeText(holder.itemView.getContext(),
                            "You must be logged in to request items",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference itemRef = itemsRef.child(item.getKey());
                itemRef.child("buyerId").setValue(currentUid);
                itemRef.child("status").setValue("pending");
                itemRef.child("buyerConfirmed").setValue(false);
                itemRef.child("sellerConfirmed").setValue(false)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(holder.itemView.getContext(),
                                        "Item requested",
                                        Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(holder.itemView.getContext(),
                                        "Failed to request item",
                                        Toast.LENGTH_SHORT).show());
            });
        }
    }

    @Override
    public int getItemCount() {
        return (itemsList == null) ? 0 : itemsList.size();
    }
}
