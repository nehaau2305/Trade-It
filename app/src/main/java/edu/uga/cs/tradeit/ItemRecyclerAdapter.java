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


    private List<Item> itemsList;
    public ItemRecyclerAdapter( List<Item> itemsList )
    {
        this.itemsList = itemsList;
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        TextView name, seller, category, price;
        Button buyB;
        public ItemHolder( View itemView ) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.nameTextView);
            seller = (TextView) itemView.findViewById(R.id.sellerTextView);
            category = (TextView) itemView.findViewById(R.id.categoryTextView);
            price = (TextView) itemView.findViewById(R.id.priceTextView);
            buyB = (Button) itemView.findViewById(R.id.buyButton);
        }
    } // ItemHolder

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType ) {
        View view = LayoutInflater.from( parent.getContext()).inflate( R.layout.item_card, parent, false );
        return new ItemHolder( view );
    }

    @Override
    public void onBindViewHolder( ItemHolder holder, int position ) {
        // get position
        Item item = itemsList.get(position);
        // set values for UI elements
        holder.name.setText(item.getName());
        holder.price.setText("$" + String.format("%.2f", item.getPrice()));
        holder.category.setText(item.getCategory());
        // get seller
        DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference("users");
        usersDbRef.child(item.getSellerId()).child("name")
                .get()
                .addOnSuccessListener(snapshot -> {
                    holder.seller.setText("Seller: " + snapshot.getValue(String.class));
                });
        // render button conditionally on whether seller is viewing their own item
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null && currentUid.equals(item.getSellerId())) {
            holder.buyB.setEnabled(false);
            holder.buyB.setText("Your Item");
        } else {
            holder.buyB.setEnabled(true);
            holder.buyB.setText("Buy Item");
        }
        // button click listener
        DatabaseReference itemsDbRef = FirebaseDatabase.getInstance().getReference("items");
        holder.buyB.setOnClickListener(v -> {
            // disable button
            holder.buyB.setEnabled(false);
            // use update instead of setValue to avoid overwriting whole object
            String buyerId = FirebaseAuth.getInstance().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("buyerId", buyerId);
            updates.put("status", "pending");
            itemsDbRef.child(item.getKey())
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(holder.itemView.getContext(), "Item Requested", Toast.LENGTH_SHORT).show();
                        // remove item from view (frontend change reflected immediately)
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            itemsList.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), "Request Failed", Toast.LENGTH_SHORT).show();
                    });

        });
    } // onBindViewHolder


    @Override
    public int getItemCount() {
        if(itemsList == null)
            return 0;
        return itemsList.size();
    }
} // ItemRecyclerAdapter
