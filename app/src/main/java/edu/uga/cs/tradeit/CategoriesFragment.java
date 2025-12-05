package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
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
 * This fragment shows a list of categories that
 * were created by the currently logged-in user.
 *
 * It uses a RecyclerView to show the list and
 * reads the data from Firebase Realtime Database.
 */
public class CategoriesFragment extends Fragment {

    // this is the scrolling list UI on the screen
    private RecyclerView recyclerView;

    // this connects the list of Category objects to the RecyclerView
    private CategoryRecyclerAdapter adapter;

    // this is the list of all categories made by the user
    private List<Category> categories = new ArrayList<>();

    /**
     * Empty constructor required for fragments.
     * Nothing special happens here.
     */
    public CategoriesFragment() { }

    /**
     * This is called when the fragment's view (screen layout)
     * is being created.
     *
     * It sets up the RecyclerView and starts loading
     * the user's categories from Firebase.
     *
     * @param inflater  used to turn XML layout into actual View objects
     * @param container parent view that will hold this fragment
     * @param savedInstanceState  previous state, if any (not used here)
     * @return the root View for this fragment's UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        // load the layout file fragment_categories.xml
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // find the RecyclerView in the layout
        recyclerView = view.findViewById(R.id.categoriesRecyclerView);

        // show list items in a simple vertical list (one under another)
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // make the adapter with the list of categories and attach it
        adapter = new CategoryRecyclerAdapter(categories);
        recyclerView.setAdapter(adapter);

        // load categories created by the current user from Firebase
        loadMyCategories();

        return view;
    }

    /**
     * Loads the categories from Firebase that belong to
     * the currently logged-in user.
     *
     * Steps:
     * 1. Get the current user's id from FirebaseAuth.
     * 2. Go to "categories" part of the database.
     * 3. Only get categories whose "creatorId" matches this user id.
     * 4. When data comes back, update the list and refresh the screen.
     */
    private void loadMyCategories() {
        // get the ID of the current user (null if no one is logged in)
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            // show a short popup message if user is not found
            Toast.makeText(requireContext(),
                    "User not recognized", Toast.LENGTH_SHORT).show();
            return;
        }

        // point to the "categories" node in the Firebase Realtime Database
        DatabaseReference catRef = FirebaseDatabase.getInstance().getReference("categories");

        // ask Firebase: give me categories where creatorId == current user's id
        catRef.orderByChild("creatorId").equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    /**
                     * This runs when Firebase sends us the data or when
                     * the data changes in the database.
                     *
                     * @param snapshot all the matching categories from Firebase
                     */
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // clear old data so we don't duplicate items
                        categories.clear();

                        // go through each category found in the database
                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            // turn the database row into a Category object
                            Category category = catSnapshot.getValue(Category.class);
                            if (category != null) {
                                // if the category doesn't have a key yet, set it
                                // using Firebase's auto-generated key
                                if (category.getKey() == null || category.getKey().isEmpty()) {
                                    category.setKey(catSnapshot.getKey());
                                }
                                // add it to our list so the adapter can show it
                                categories.add(category);
                            }
                        }

                        // tell the adapter that the data changed so the UI refreshes
                        adapter.notifyDataSetChanged();
                    }

                    /**
                     * This runs if something goes wrong when reading from Firebase.
                     *
                     * @param error the error from Firebase
                     */
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // show a short message if loading fails
                        Toast.makeText(requireContext(),
                                "Failed to load categories",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
