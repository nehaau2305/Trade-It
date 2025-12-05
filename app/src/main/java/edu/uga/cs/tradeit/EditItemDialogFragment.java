package edu.uga.cs.tradeit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A dialog window used to edit an existing item.
 *
 * It loads the item from Firebase, shows its info in a form,
 * lets the user change things, and then saves the changes
 * back to Firebase when the user taps "Save".
 */
public class EditItemDialogFragment extends DialogFragment {

    // key name used to pass the item key into this dialog
    private static final String ARG_ITEM_KEY = "itemKey";

    /**
     * Helper function to create a new EditItemDialogFragment
     * with the item key stored in its arguments.
     *
     * @param itemKey the Firebase key of the item we want to edit
     * @return a new EditItemDialogFragment ready to use
     */
    public static EditItemDialogFragment newInstance(String itemKey) {
        EditItemDialogFragment f = new EditItemDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ITEM_KEY, itemKey);
        f.setArguments(b);
        return f;
    }

    // the key of the item we are editing
    private String itemKey;
    // the actual Item object loaded from Firebase
    private Item currentItem;

    // UI fields in the dialog
    private EditText nameEditText, priceEditText, descriptionEditText;
    private AutoCompleteTextView categoryDropdown;
    private CheckBox freeCheckBox;
    private Button addButton, cancelButton, addCategoryButton;

    // Firebase references for items and categories
    private DatabaseReference itemsRef;
    private DatabaseReference categoriesRef;
    private String currentUid;

    // lists and sets for managing categories and their titles
    private final List<Category> categoryList = new ArrayList<>();
    private final List<String> categoryTitles = new ArrayList<>();
    private final Set<String> categoryTitlesLowerSet = new HashSet<>();
    // which category is currently selected (by id)
    private String selectedCategoryId = null;

    /**
     * Called to build and return the dialog box UI.
     * Here we set up all the views, listeners, and load data.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        itemKey = getArguments() != null ? getArguments().getString(ARG_ITEM_KEY) : null;

        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        categoriesRef = FirebaseDatabase.getInstance().getReference("categories");
        currentUid = FirebaseAuth.getInstance().getUid();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_item, null);

        // find all the views in the layout
        nameEditText       = view.findViewById(R.id.itemNameEditText);
        priceEditText      = view.findViewById(R.id.priceEditText);
        descriptionEditText= view.findViewById(R.id.descriptionEditText);
        categoryDropdown   = view.findViewById(R.id.itemCategoryDropdown);
        freeCheckBox       = view.findViewById(R.id.freeCheckBox);
        addButton          = view.findViewById(R.id.addButton);
        cancelButton       = view.findViewById(R.id.cancelButton);
        addCategoryButton  = view.findViewById(R.id.addCategoryButton);

        // change title and button text so it says "Edit" instead of "Add"
        TextView titleText = view.findViewById(R.id.addItemTitleText);
        if (titleText != null) {
            titleText.setText("Edit Item");
        }
        addButton.setText("Save Changes");

        // when item is free, price becomes 0 and user can't type in price
        freeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                priceEditText.setText("0");
                priceEditText.setEnabled(false);
            } else {
                priceEditText.setEnabled(true);
            }
        });

        // cancel just closes the dialog
        cancelButton.setOnClickListener(v -> dismiss());

        // opens a small dialog to create a new category
        addCategoryButton.setOnClickListener(v -> showNewCategoryDialog());

        // set up dropdown so it always shows suggestions when clicked/focused
        categoryDropdown.setThreshold(0);
        categoryDropdown.setOnClickListener(v -> categoryDropdown.showDropDown());
        categoryDropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                categoryDropdown.showDropDown();
            }
        });

        builder.setView(view);
        Dialog dialog = builder.create();

        // load the item being edited, and all categories for the dropdown
        loadItem();
        loadCategories();

        // when user taps "Save Changes", we try to save everything
        addButton.setOnClickListener(v -> saveChanges(dialog));

        return dialog;
    }

    /**
     * Shows a dialog to create a brand new category.
     * If the category name is valid and not a duplicate,
     * it saves it into Firebase and updates the dropdown list.
     */
    private void showNewCategoryDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setTitle("New Category");

        final EditText input = new EditText(requireContext());
        input.setHint("Category name");
        b.setView(input);

        b.setPositiveButton("Add", (dialogInterface, which) -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(getContext(),
                        "Category title required",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String norm = title.toLowerCase(Locale.US);
            if (categoryTitlesLowerSet.contains(norm)) {
                Toast.makeText(getContext(),
                        "Category already exists",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String creatorId = (currentUid != null) ? currentUid : "unknown";
            long now = System.currentTimeMillis();

            String key = categoriesRef.push().getKey();
            if (key == null) {
                Toast.makeText(getContext(),
                        "Failed to create category",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Category newCat = new Category(creatorId, title, now);
            newCat.setKey(key);

            categoriesRef.child(key).setValue(newCat)
                    .addOnSuccessListener(aVoid -> {
                        if (!isAdded()) return;

                        // update our local lists and dropdown
                        categoryList.add(newCat);
                        categoryTitles.add(title);
                        categoryTitlesLowerSet.add(norm);

                        ArrayAdapter<String> adapter =
                                (ArrayAdapter<String>) categoryDropdown.getAdapter();
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }

                        selectedCategoryId = key;
                        categoryDropdown.setText(title, false);

                        Toast.makeText(getContext(),
                                "Category added",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Failed to save category",
                                    Toast.LENGTH_SHORT).show());
        });

        b.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());
        b.show();
    }

    /**
     * Loads the item from Firebase using the itemKey.
     * Once loaded, it fills in the UI fields with the item's data.
     */
    private void loadItem() {
        if (itemKey == null) return;

        itemsRef.child(itemKey)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    Item item = snapshot.getValue(Item.class);
                    if (item == null) {
                        Toast.makeText(getContext(),
                                "Item not found",
                                Toast.LENGTH_SHORT).show();
                        dismiss();
                        return;
                    }
                    item.setKey(itemKey);
                    currentItem = item;

                    // fill in text fields with current values
                    nameEditText.setText(item.getName());
                    descriptionEditText.setText(item.getDescription());

                    double p = item.getPrice();
                    if (p == 0.0) {
                        freeCheckBox.setChecked(true);
                        priceEditText.setText("0");
                        priceEditText.setEnabled(false);
                    } else {
                        freeCheckBox.setChecked(false);
                        priceEditText.setEnabled(true);
                        priceEditText.setText(String.format(Locale.US, "%.2f", p));
                    }

                    // remember its category so we can show it in the dropdown
                    selectedCategoryId = item.getCategoryId();
                    applySelectedCategoryToDropdown();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Failed to load item",
                            Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    /**
     * Loads all categories from Firebase, builds the dropdown list,
     * and connects the selected category to the item (if editing).
     */
    private void loadCategories() {
        categoriesRef.orderByChild("title")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        categoryList.clear();
                        categoryTitles.clear();
                        categoryTitlesLowerSet.clear();

                        // go through each category in the database
                        for (DataSnapshot catSnap : snapshot.getChildren()) {
                            Category c = catSnap.getValue(Category.class);
                            if (c != null) {
                                if (c.getKey() == null || c.getKey().isEmpty()) {
                                    c.setKey(catSnap.getKey());
                                }
                                String title = c.getTitle();
                                if (title == null) continue;

                                String norm = title.trim().toLowerCase(Locale.US);
                                if (!categoryTitlesLowerSet.contains(norm)) {
                                    categoryTitlesLowerSet.add(norm);
                                    categoryList.add(c);
                                    categoryTitles.add(title);
                                }
                            }
                        }

                        // create an adapter for the category dropdown
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                categoryTitles
                        );
                        categoryDropdown.setAdapter(adapter);

                        // when user picks a category, remember its id
                        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
                            if (position >= 0 && position < categoryList.size()) {
                                selectedCategoryId = categoryList.get(position).getKey();
                            }
                        });

                        // if we already had a selectedCategoryId, try to show it now
                        applySelectedCategoryToDropdown();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(),
                                "Failed to load categories",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * After categories are loaded, this tries to set the dropdown text
     * to match the category whose id is selectedCategoryId.
     */
    private void applySelectedCategoryToDropdown() {
        if (selectedCategoryId == null || categoryList.isEmpty() || !isAdded()) return;

        for (int i = 0; i < categoryList.size(); i++) {
            Category c = categoryList.get(i);
            if (selectedCategoryId.equals(c.getKey())) {
                categoryDropdown.setText(c.getTitle(), false);
                break;
            }
        }
    }

    /**
     * Reads all data from the form fields, checks that it's valid,
     * and then updates the item in Firebase. If successful, it also
     * refreshes the item detail screen and closes the dialog.
     */
    private void saveChanges(Dialog dialog) {
        if (currentItem == null) return;

        String name     = nameEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String desc     = descriptionEditText.getText().toString().trim();

        // basic checks to make sure required fields are not empty
        if (name.isEmpty()) {
            nameEditText.setError("Name required");
            return;
        }

        if (desc.isEmpty()) {
            descriptionEditText.setError("Description required");
            return;
        }

        double priceVal;
        if (freeCheckBox.isChecked()) {
            priceVal = 0.0;
        } else {
            if (priceStr.isEmpty()) {
                priceEditText.setError("Price required");
                return;
            }
            try {
                priceVal = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                priceEditText.setError("Invalid price");
                return;
            }
        }

        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
            Toast.makeText(getContext(),
                    "Please select a category",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // only allow editing when item status is "available"
        if (!"available".equals(currentItem.getStatus())) {
            Toast.makeText(getContext(),
                    "Only available items can be edited",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        // build a map with all updates we want to send to Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("price", priceVal);
        updates.put("categoryId", selectedCategoryId);
        updates.put("description", desc);

        itemsRef.child(currentItem.getKey())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;

                    Toast.makeText(getContext(),
                            "Item updated",
                            Toast.LENGTH_SHORT).show();

                    // if we are on the ItemDetailActivity, refresh it
                    if (getActivity() instanceof ItemDetailActivity) {
                        ((ItemDetailActivity) getActivity()).reloadItem();
                    }

                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Failed to update item",
                            Toast.LENGTH_SHORT).show();
                });
    }
}



