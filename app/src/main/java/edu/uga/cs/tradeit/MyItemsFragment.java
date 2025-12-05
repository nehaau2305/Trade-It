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

/**
 * This fragment shows all items that belong to
 * the currently logged-in user (their own listings).
 *
 * It:
 * - loads items from Firebase where sellerId == current user
 * - hides completed items
 * - shows them in a grid (1 column portrait, 2 in landscape)
 * - has a button to go to the Categories screen
 */
public class MyItemsFragment extends Fragment {

    // list UI on the screen
    private RecyclerView recyclerView;
    // adapter that connects item data to the list UI
    private ItemRecyclerAdapter adapter;
    // list of the user's own items
    private List<Item> myItems = new ArrayList<>();

    // button that opens the categories fragment
    private Button categoriesButton;

    /**
     * Empty constructor (required for fragments).
     */
    public MyItemsFragment() { }

    /**
     * Called when this fragment should create its view.
     * Here we:
     * - inflate the layout
     * - set up RecyclerView and adapter
     * - load the user's items from Firebase
     * - hook up the Categories button
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_items, container, false);

        recyclerView     = view.findViewById(R.id.myItemsRecyclerView);
        categoriesButton = view.findViewById(R.id.categoriesButton);

        // decide how many columns based on screen orientation
        int orientation = getResources().getConfiguration().orientation;
        int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 1 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));

        // set up adapter with the list of items and attach it
        adapter = new ItemRecyclerAdapter(myItems);
        recyclerView.setAdapter(adapter);

        // load all items where the current user is the seller
        loadMyItems();

        // when the button is clicked, open CategoriesFragment
        categoriesButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView2, new CategoriesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    /**
     * Loads the items for the current user from Firebase.
     * Only items that are NOT completed are shown.
     */
    private void loadMyItems() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            Toast.makeText(requireContext(),
                    "User not recognized", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance().getReference("items");

        // find items where sellerId equals this user's id
        db.orderByChild("sellerId").equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        myItems.clear();

                        // loop over each item from Firebase
                        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                            Item item = itemSnapshot.getValue(Item.class);
                            if (item != null) {
                                // make sure item has a key set
                                if (item.getKey() == null || item.getKey().isEmpty()) {
                                    item.setKey(itemSnapshot.getKey());
                                }

                                String status = item.getStatus();
                                // Only add if NOT completed (available or pending)
                                if (status == null || !status.equals("completed")) {
                                    myItems.add(item);
                                }
                            }
                        }

                        // tell adapter that the data changed so the UI updates
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
