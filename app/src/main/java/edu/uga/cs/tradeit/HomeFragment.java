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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * HomeFragment is the first view the user sees after logging in. It displays
 * all items still available for purchase & the search function.
 */
public class HomeFragment extends Fragment {

    // initialize all UI variables
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

    private List<Item> allItems = new ArrayList<>();
    private List<Item> itemsList = new ArrayList<>();

    private List<Category> categoryList = new ArrayList<>();
    private List<String> categoryTitles = new ArrayList<>();
    private Set<String> categoryTitlesLowerSet = new HashSet<>();
    private String selectedCategoryKey = null;

    private static final int MODE_CATEGORY = 0;
    private static final int MODE_ITEM = 1;
    private int currentSearchMode = MODE_CATEGORY;

    private static final int SORT_NEWEST = 0;
    private static final int SORT_NAME = 1;
    private int currentSortMode = SORT_NEWEST;

    // empty constructor
    public HomeFragment() { }

    /**
     * onCreateView connects all the UI elements & sets up all the click
     * listeners.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

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

        int orientation = getResources().getConfiguration().orientation;
        int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 1 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));

        adapter = new ItemRecyclerAdapter(itemsList);
        recyclerView.setAdapter(adapter);

        loadAvailableItems();
        loadCategories();

        addItemButton.setOnClickListener(v -> {
            AddItemDialogFragment addItemDialogFragment = new AddItemDialogFragment();
            addItemDialogFragment.show(getParentFragmentManager(), "AddItemDialog");
        });

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

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

        searchModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.searchByCategoryRadio) {
                currentSearchMode = MODE_CATEGORY;
                searchInputEditText.setText("");
                searchInputEditText.setHint("Search category");
                selectedCategoryKey = null;
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

        // show dropdown when clicking category search box
        searchInputEditText.setThreshold(0);
        searchInputEditText.setOnClickListener(v -> {
            if (currentSearchMode == MODE_CATEGORY) {
                searchInputEditText.showDropDown();
            }
        });

        sortModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sortByNewestRadio) {
                currentSortMode = SORT_NEWEST;
            } else if (checkedId == R.id.sortByNameRadio) {
                currentSortMode = SORT_NAME;
            }
            applyFilters();
        });

        setCategorySuggestionsAdapter();
        searchInputEditText.setHint("Search category");

        return view;
    }

    /**
     * loadAvailableItems loads all the items from the Firebase that has
     * the status "available" so that only the items that no one has
     * already requested or purchased is displayed.
     */
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
                        Toast.makeText(requireContext(),
                                "Failed to load items",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * loadCategories loads all categories from the Firebase database & updates the
     * local category lists.
     */
    private void loadCategories() {
        DatabaseReference catRef = FirebaseDatabase.getInstance().getReference("categories");

        catRef.orderByChild("title")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        categoryList.clear();
                        categoryTitles.clear();
                        categoryTitlesLowerSet.clear();

                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            Category category = catSnapshot.getValue(Category.class);
                            if (category != null) {
                                if (category.getKey() == null || category.getKey().isEmpty()) {
                                    category.setKey(catSnapshot.getKey());
                                }
                                String title = category.getTitle();
                                if (title == null) continue;

                                String norm = title.trim().toLowerCase(Locale.US);
                                if (!categoryTitlesLowerSet.contains(norm)) {
                                    categoryTitlesLowerSet.add(norm);
                                    categoryList.add(category);
                                    categoryTitles.add(title);
                                }
                            }
                        }

                        if (currentSearchMode == MODE_CATEGORY) {
                            setCategorySuggestionsAdapter();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Failed to load categories",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * setCategorySuggestionsAdapter displays all current category titles
     * in the database to the user.
     */
    private void setCategorySuggestionsAdapter() {
        if (!isAdded()) return;

        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryTitles
        );
        searchInputEditText.setAdapter(dropdownAdapter);
    }

    /**
     * applyFilters filters the items shown depending on what category
     * or item the user queries. After filtering, this method also applies
     * the current sorting mode, either by creation time or alphabetically.
     */
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
            String query = searchInputEditText.getText().toString().trim().toLowerCase(Locale.US);
            for (Item item : allItems) {
                if (query.isEmpty()) {
                    itemsList.add(item);
                } else {
                    String name = (item.getName() == null) ? "" : item.getName().toLowerCase(Locale.US);
                    if (name.contains(query)) {
                        itemsList.add(item);
                    }
                }
            }
        }

        // sort
        if (currentSortMode == SORT_NEWEST) {
            itemsList.sort((a, b) -> Long.compare(b.getCreationTime(), a.getCreationTime()));
        } else if (currentSortMode == SORT_NAME) {
            itemsList.sort((a, b) -> {
                String na = (a.getName() == null) ? "" : a.getName().toLowerCase(Locale.US);
                String nb = (b.getName() == null) ? "" : b.getName().toLowerCase(Locale.US);
                return na.compareTo(nb);
            });
        }

        adapter.notifyDataSetChanged();
    } // applyFilters
}
