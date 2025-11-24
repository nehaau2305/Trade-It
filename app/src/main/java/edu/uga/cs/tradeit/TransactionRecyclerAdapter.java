package edu.uga.cs.tradeit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TransactionRecyclerAdapter extends RecyclerView.Adapter<TransactionRecyclerAdapter.TransactionHolder>{

    // list of items to show
    private List<Item> itemsList;
    private String currentTab = "pending";
    private String currentUId = FirebaseAuth.getInstance().getUid();

    public TransactionRecyclerAdapter(List<Item> itemsList, String currentUId, String tab) {
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
        public TransactionHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.nameTextView);
            person = itemView.findViewById(R.id.personTextView);
            category = itemView.findViewById(R.id.categoryTextView);
            price = itemView.findViewById(R.id.priceTextView);
            actionButton = itemView.findViewById(R.id.actionButton);
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
        holder.category.setText(item.getCategory());
        holder.price.setText("$" + item.getPrice());
        holder.actionButton.setVisibility(View.GONE);

        DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference("users");

        // switch based on button selected
        switch(currentTab) {
            case "pending":
                holder.actionButton.setVisibility(View.GONE);
                usersDbRef.child(item.getSellerId()).child("name")
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            String seller = snapshot.getValue(String.class);
                            if (seller == null) seller = "Unknown";
                            holder.person.setText("Seller: " + seller);
                        });
                break;
            case "confirm":
                holder.actionButton.setVisibility(View.VISIBLE);
                holder.actionButton.setText("Confirm Sale");
                usersDbRef.child(item.getBuyerId()).child("name")
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            String buyer = snapshot.getValue(String.class);
                            if (buyer == null) buyer = "Unknown";
                            holder.person.setText("Buyer: " + buyer);
                        });
                holder.actionButton.setOnClickListener(v -> {
                    DatabaseReference itemsDbRef = FirebaseDatabase.getInstance().getReference("items");
                    itemsDbRef.child(item.getKey()).child("status").setValue("completed");
                });
                break;
            case "completed":
                holder.actionButton.setVisibility(View.GONE);
                if(currentUId.equals((item.getBuyerId()))) {
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
    } // onBindViewHolder

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

}
