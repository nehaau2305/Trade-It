package edu.uga.cs.tradeit;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // ---------- UI ----------
    private RecyclerView recyclerView;
    private ItemRecyclerAdapter adapter;
    private FloatingActionButton addItemButton;
    private Button logoutButton;

    private Button myItemsButton;
    private Button transactionsButton;

    private RadioGroup searchModeGroup;
    private RadioButton searchByCategoryRadio;
    private RadioButton searchByItemRadio;
    private AutoCompleteTextView searchInputEditText;

    // ---------- Data: items ----------
    private List<Item> allItems = new ArrayList<>();
    private List<Item> itemsList = new ArrayList<>();

    // ---------- Data: categories ----------
    private List<Category> categoryList = new ArrayList<>();
    private List<String> categoryTitles = new ArrayList<>();
    private String selectedCategoryKey = null;

    private static final int MODE_CATEGORY = 0;
    private static final int MODE_ITEM = 1;
    private int currentSearchMode = MODE_CATEGORY;

    public HomeFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // hook up views
        recyclerView          = view.findViewById(R.id.recyclerView);
        addItemButton         = view.findViewById(R.id.floatingActionButton);
        logoutButton          = view.findViewById(R.id.logoutButton);
        myItemsButton         = view.findViewById(R.id.myItemsButton);
        transactionsButton    = view.findViewById(R.id.transactionsButton);
        searchModeGroup       = view.findViewById(R.id.searchModeGroup);
        searchByCategoryRadio = view.findViewById(R.id.searchByCategoryRadio);
        searchByItemRadio     = view.findViewById(R.id.searchByItemRadio);
        searchInputEditText   = view.findViewById(R.id.searchInputEditText);

        // RecyclerView layout (1 column portrait, 2 landscape)
        int orientation = getResources().getConfiguration().orientation;
        int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 1 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));

        adapter = new ItemRecyclerAdapter(itemsList);
        recyclerView.setAdapter(adapter);

        // load data
        loadAvailableItems();
        loadCategories();

        // FAB: add new item
        addItemButton.setOnClickListener(v -> {
            AddItemDialogFragment addItemDialogFragment = new AddItemDialogFragment();
            addItemDialogFragment.show(getParentFragmentManager(), "AddItemDialog");
        });

        // logout
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        // ---------- nav buttons ----------

        myItemsButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView2, new MyItemsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        transactionsButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView2, new TransactionFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // ---------- search toggle: category vs item ----------
        searchModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.searchByCategoryRadio) {
                currentSearchMode = MODE_CATEGORY;
                searchInputEditText.setText("");
                searchInputEditText.setHint("Search category");
                setCategorySuggestionsAdapter();
            } else if (checkedId == R.id.searchByItemRadio) {
                currentSearchMode = MODE_ITEM;
                selectedCategoryKey = null;
                searchInputEditText.setText("");
                searchInputEditText.setHint("Search item name");
                searchInputEditText.setAdapter(null);
            }
            applyFilters();
        });

        searchInputEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (currentSearchMode == MODE_ITEM) {
                    applyFilters();
                }
            }
        });

        searchInputEditText.setOnItemClickListener((parent, v, position, id) -> {
            if (currentSearchMode == MODE_CATEGORY) {
                if (position >= 0 && position < categoryList.size()) {
                    Category selected = categoryList.get(position);
                    selectedCategoryKey = selected.getKey();
                } else {
                    selectedCategoryKey = null;
                }
                applyFilters();
            }
        });

        // default: category mode
        setCategorySuggestionsAdapter();
        searchInputEditText.setHint("Search category");

        return view;
    }

    private void loadAvailableItems() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("items");

        db.orderByChild("status").equalTo("available")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allItems.clear();

                        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                            Item item = itemSnapshot.getValue(Item.class);
                            if (item != null) {
                                if (item.getKey() == null || item.getKey().isEmpty()) {
                                    item.setKey(itemSnapshot.getKey());
                                }
                                allItems.add(item);
                            }
                        }

                        applyFilters();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(),
                                "Failed to load items",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadCategories() {
        DatabaseReference catRef = FirebaseDatabase.getInstance().getReference("categories");

        catRef.orderByChild("title")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        categoryList.clear();
                        categoryTitles.clear();

                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            Category category = catSnapshot.getValue(Category.class);
                            if (category != null) {
                                if (category.getKey() == null || category.getKey().isEmpty()) {
                                    category.setKey(catSnapshot.getKey());
                                }
                                categoryList.add(category);
                                categoryTitles.add(category.getTitle());
                            }
                        }

                        if (currentSearchMode == MODE_CATEGORY) {
                            setCategorySuggestionsAdapter();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(),
                                "Failed to load categories",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setCategorySuggestionsAdapter() {
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryTitles
        );
        searchInputEditText.setAdapter(dropdownAdapter);
    }

    private void applyFilters() {
        itemsList.clear();

        if (currentSearchMode == MODE_CATEGORY) {
            for (Item item : allItems) {
                if (selectedCategoryKey == null || selectedCategoryKey.isEmpty()) {
                    itemsList.add(item);
                } else if (selectedCategoryKey.equals(item.getCategoryId())) {
                    itemsList.add(item);
                }
            }
        } else {
            String query = searchInputEditText.getText().toString().trim().toLowerCase();

            for (Item item : allItems) {
                if (query.isEmpty()) {
                    itemsList.add(item);
                } else {
                    String name = (item.getName() == null) ? "" : item.getName().toLowerCase();
                    if (name.contains(query)) {
                        itemsList.add(item);
                    }
                }
            }
        }

        adapter.notifyDataSetChanged();
    }
}
