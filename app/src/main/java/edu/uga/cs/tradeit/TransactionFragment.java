package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment shows the user's transactions in three tabs:
 * - pending: items you requested and are waiting on
 * - confirm: items you are selling that someone requested
 * - completed: finished transactions you were part of
 *
 * It just swaps which list is loaded based on which button is clicked.
 */
public class TransactionFragment extends Fragment {
    // variables
    private Button pendingBtn, confirmBtn, completedBtn;
    private RecyclerView recyclerView;
    private TransactionRecyclerAdapter adapter;
    private List<Item> transactionsList = new ArrayList<>();
    private String currentUId;
    private String currTab = "pending";

    /**
     * Called to create the view for this fragment.
     * Here we:
     * - inflate the layout
     * - hook up buttons and RecyclerView
     * - set default tab to "pending"
     * - load data for each tab when the buttons are clicked
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);

        // find views
        pendingBtn = view.findViewById(R.id.pendingButton);
        confirmBtn = view.findViewById(R.id.confirmSaleButton);
        completedBtn = view.findViewById(R.id.completedButton);
        recyclerView = view.findViewById(R.id.transactionRecyclerView);
        currentUId = FirebaseAuth.getInstance().getUid();
        adapter = new TransactionRecyclerAdapter(transactionsList, currentUId, currTab);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // set default page to pending
        setButtonSelected(pendingBtn);
        loadPending();

        // button click listeners
        pendingBtn.setOnClickListener(v -> {
            currTab = "pending";
            setButtonSelected(pendingBtn);
            adapter.setCurrentTab(currTab);
            loadPending();
        });
        confirmBtn.setOnClickListener(v -> {
            currTab = "confirm";
            setButtonSelected(confirmBtn);
            adapter.setCurrentTab(currTab);
            loadConfirmSale();
        });
        completedBtn.setOnClickListener(v -> {
            currTab = "completed";
            setButtonSelected(completedBtn);
            adapter.setCurrentTab(currTab);
            loadCompleted();
        });

        return view;
    }

    /**
     * Simple helper to visually mark which button/tab is "active".
     * Only the selected one is set to true.
     */
    private void setButtonSelected(Button selected) {
        pendingBtn.setSelected(false);
        confirmBtn.setSelected(false);
        completedBtn.setSelected(false);
        selected.setSelected(true);
    }

    /**
     * Loads "pending" transactions where the current user is the buyer.
     * These are items with status = pending and buyerId = current user.
     */
    private void loadPending() {
        if (currentUId == null) return;
        // remove existing list
        transactionsList.clear();

        DatabaseReference itemsDbRef = FirebaseDatabase.getInstance().getReference("items");
        itemsDbRef
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        Item item = s.getValue(Item.class);
                        if (item != null && "pending".equals(item.getStatus()) && item.getBuyerId().equals(currentUId)) {
                            transactionsList.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Failed to load pending transactions", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads "confirm sale" items where the current user is the seller.
     * These are items with status = pending and sellerId = current user,
     * meaning someone requested your item and you might need to confirm.
     */
    private void loadConfirmSale() {
        if (currentUId == null) return;
        // remove existing list
        transactionsList.clear();

        DatabaseReference itemsDbRef = FirebaseDatabase.getInstance().getReference("items");
        itemsDbRef
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        Item item = s.getValue(Item.class);
                        if (item != null && "pending".equals(item.getStatus()) && item.getSellerId().equals(currentUId)) {
                            transactionsList.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Failed to load transactions to confirm sale", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads completed transactions where the current user was either
     * the buyer or the seller.
     */
    private void loadCompleted() {
        if (currentUId == null) return;
        // remove existing list
        transactionsList.clear();

        DatabaseReference itemsDbRef = FirebaseDatabase.getInstance().getReference("items");
        itemsDbRef
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        Item item = s.getValue(Item.class);
                        if (item != null && "completed".equals(item.getStatus()) && (currentUId.equals(item.getBuyerId()) || currentUId.equals(item.getSellerId()))) {
                            transactionsList.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Failed to load completed transactions", Toast.LENGTH_SHORT).show());
    }
}
