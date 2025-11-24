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

public class CategoriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryRecyclerAdapter adapter;
    private List<Category> categories = new ArrayList<>();

    public CategoriesFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        recyclerView = view.findViewById(R.id.categoriesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new CategoryRecyclerAdapter(categories);
        recyclerView.setAdapter(adapter);

        loadMyCategories();

        return view;
    }

    private void loadMyCategories() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            Toast.makeText(requireContext(),
                    "User not recognized", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference catRef = FirebaseDatabase.getInstance().getReference("categories");

        catRef.orderByChild("creatorId").equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        categories.clear();

                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            Category category = catSnapshot.getValue(Category.class);
                            if (category != null) {
                                if (category.getKey() == null || category.getKey().isEmpty()) {
                                    category.setKey(catSnapshot.getKey());
                                }
                                categories.add(category);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(),
                                "Failed to load categories",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
