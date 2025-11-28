package edu.uga.cs.tradeit;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyItemsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ItemRecyclerAdapter adapter;
    private List<Item> myItems = new ArrayList<>();

    private Button categoriesButton;

    public MyItemsFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_items, container, false);

        recyclerView      = view.findViewById(R.id.myItemsRecyclerView);
        categoriesButton  = view.findViewById(R.id.categoriesButton);

        int orientation = getResources().getConfiguration().orientation;
        int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 1 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));

        adapter = new ItemRecyclerAdapter(myItems);
        recyclerView.setAdapter(adapter);

        loadMyItems();

        categoriesButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView2, new CategoriesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadMyItems() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            Toast.makeText(requireContext(),
                    "User not recognized", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance().getReference("items");

        db.orderByChild("sellerId").equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        myItems.clear();

                        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                            Item item = itemSnapshot.getValue(Item.class);
                            if (item != null) {
                                if (item.getKey() == null || item.getKey().isEmpty()) {
                                    item.setKey(itemSnapshot.getKey());
                                }
                                myItems.add(item);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(),
                                "Failed to load your items",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
