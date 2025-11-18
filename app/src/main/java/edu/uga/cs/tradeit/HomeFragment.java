package edu.uga.cs.tradeit;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private ItemRecyclerAdapter adapter;
    private List<Item> itemsList = new ArrayList<>();
    private FloatingActionButton addItemButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        // determine orientation
        int orientation = getResources().getConfiguration().orientation;
        // display 1 item per row in portrait & 2 in landscape
        int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 1 : 2;

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));

        // adapter
        adapter = new ItemRecyclerAdapter(itemsList);
        recyclerView.setAdapter(adapter);
        addItemButton = view.findViewById(R.id.floatingActionButton);
        loadAvailableItems();

        // button
        addItemButton.setOnClickListener(v -> {
            AddItemDialogFragment addItemDialogFragment = new AddItemDialogFragment();
            addItemDialogFragment.show(getParentFragmentManager(), "AddItemDialog");
        });

        return view;
    } // onCreateView

    private void loadAvailableItems() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("items");

        db.orderByChild("status").equalTo("available")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        itemsList.clear();
                        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                            Item item = itemSnapshot.getValue(Item.class);
                            if (item != null) {
                                if (item.getKey() == null || item.getKey().isEmpty()) {
                                    item.setKey(itemSnapshot.getKey());
                                }
                                itemsList.add(item);
                            }
                        }
                        // refresh recycler view
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
