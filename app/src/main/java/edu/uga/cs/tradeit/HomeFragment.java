package edu.uga.cs.tradeit;

import android.content.Context;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private RadioGroup sortModeGroup;
    private RadioButton sortByNewestRadio;
    private RadioButton sortByNameRadio;

    // ---------- Data: items ----------
    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> itemsList = new ArrayList<>();

    // ---------- Data: categories ----------
    private final List<Category> categoryList = new ArrayList<>();
    private final List<String> categoryTitles = new ArrayList<>();
    // map categoryId -> title for lookup while filtering
    private final Map<String, String> categoryIdToTitle = new HashMap<>();

    private static final int MODE_CATEGORY = 0;
    private static final int MODE_ITEM = 1;
    private int currentSearchMode = MODE_CATEGORY;

    private static final int SORT_NEWEST = 0;
    private static final int SORT_NAME = 1;
    private int currentSortMode = SORT_NEWEST;

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
        sortModeGroup         = view.findViewById(R.id.sortModeGroup);
        sortByNewestRadio     = view.findViewById(R.id.sortByNewestRadio);
        sortByNameRadio       = view.findViewById(R.id.sortByNameRadio);

        // RecyclerView: 1 column portrait, 2 columns landscape
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

        // nav buttons
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

        // search mode toggle
        searchModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.searchByCategoryRadio) {
                currentSearchMode = MODE_CATEGORY;
                searchInputEditText.setText("");
                searchInputEditText.setHint("Search category");
                setCategorySuggestionsAdapter();
            } else if (checkedId == R.id.searchByItemRadio) {
                currentSearchMode = MODE_ITEM;
                searchInputEditText.setText("");
                searchInputEditText.setHint("Search item name");
                searchInputEditText.setAdapter(null);
            }
            applyFilters();
        });

        // live search (both modes)
        searchInputEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });

        // optional: when user taps a suggestion, we just re-apply filter
        searchInputEditText.setOnItemClickListener((parent, v, position, id) -> applyFilters());

        // sort mode toggle
        sortModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sortByNameRadio) {
                currentSortMode = SORT_NAME;
            } else {
                currentSortMode = SORT_NEWEST;
            }
            applyFilters();
        });

        // defaults
        searchByCategoryRadio.setChecked(true);
        sortByNewestRadio.setChecked(true);
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
                        if (!isAdded()) return;

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
                        if (!isAdded()) return;
                        Context ctx = getContext();
                        if (ctx == null) return;
                        Toast.makeText(ctx,
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
                        if (!isAdded()) return;

                        categoryList.clear();
                        categoryTitles.clear();
                        categoryIdToTitle.clear();

                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            Category category = catSnapshot.getValue(Category.class);
                            if (category != null) {
                                if (category.getKey() == null || category.getKey().isEmpty()) {
                                    category.setKey(catSnapshot.getKey());
                                }
                                categoryList.add(category);
                                categoryTitles.add(category.getTitle());
                                categoryIdToTitle.put(category.getKey(), category.getTitle());
                            }
                        }

                        if (currentSearchMode == MODE_CATEGORY) {
                            setCategorySuggestionsAdapter();
                        }

                        // re-apply filters now that we know titles
                        applyFilters();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        Context ctx = getContext();
                        if (ctx == null) return;
                        Toast.makeText(ctx,
                                "Failed to load categories",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setCategorySuggestionsAdapter() {
        if (!isAdded()) return;
        Context ctx = getContext();
        if (ctx == null) return;

        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                ctx,
                android.R.layout.simple_dropdown_item_1line,
                categoryTitles
        );
        searchInputEditText.setAdapter(dropdownAdapter);
    }

    private void applyFilters() {
        itemsList.clear();

        String query = searchInputEditText.getText().toString().trim().toLowerCase();

        if (currentSearchMode == MODE_CATEGORY) {
            // filter by CATEGORY TITLE (partial match)
            for (Item item : allItems) {
                String catId = item.getCategoryId();
                String title = (catId == null) ? "" : categoryIdToTitle.get(catId);
                if (title == null) title = "";
                String t = title.toLowerCase();

                if (query.isEmpty() || t.contains(query)) {
                    itemsList.add(item);
                }
            }
        } else {
            // filter by ITEM NAME (partial match)
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

        // ---- sort ----
        if (currentSortMode == SORT_NAME) {
            Collections.sort(itemsList, new Comparator<Item>() {
                @Override
                public int compare(Item a, Item b) {
                    String na = (a.getName() == null) ? "" : a.getName().toLowerCase();
                    String nb = (b.getName() == null) ? "" : b.getName().toLowerCase();
                    return na.compareTo(nb);
                }
            });
        } else {
            // newest first
            Collections.sort(itemsList, new Comparator<Item>() {
                @Override
                public int compare(Item a, Item b) {
                    return Long.compare(b.getCreationTime(), a.getCreationTime());
                }
            });
        }

        adapter.notifyDataSetChanged();
    }
}

