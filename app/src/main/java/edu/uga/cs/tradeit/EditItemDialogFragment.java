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

public class EditItemDialogFragment extends DialogFragment {

    private static final String ARG_ITEM_KEY = "itemKey";

    public static EditItemDialogFragment newInstance(String itemKey) {
        EditItemDialogFragment f = new EditItemDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ITEM_KEY, itemKey);
        f.setArguments(b);
        return f;
    }

    private String itemKey;
    private Item currentItem;

    private EditText nameEditText, priceEditText, descriptionEditText;
    private AutoCompleteTextView categoryDropdown;
    private CheckBox freeCheckBox;
    private Button addButton, cancelButton, addCategoryButton;

    private DatabaseReference itemsRef;
    private DatabaseReference categoriesRef;
    private String currentUid;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<String> categoryTitles = new ArrayList<>();
    private final Set<String> categoryTitlesLowerSet = new HashSet<>();
    private String selectedCategoryId = null;

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

        nameEditText       = view.findViewById(R.id.itemNameEditText);
        priceEditText      = view.findViewById(R.id.priceEditText);
        descriptionEditText= view.findViewById(R.id.descriptionEditText);
        categoryDropdown   = view.findViewById(R.id.itemCategoryDropdown);
        freeCheckBox       = view.findViewById(R.id.freeCheckBox);
        addButton          = view.findViewById(R.id.addButton);
        cancelButton       = view.findViewById(R.id.cancelButton);
        addCategoryButton  = view.findViewById(R.id.addCategoryButton);

        TextView titleText = view.findViewById(R.id.addItemTitleText);
        if (titleText != null) {
            titleText.setText("Edit Item");
        }
        addButton.setText("Save Changes");

        freeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                priceEditText.setText("0");
                priceEditText.setEnabled(false);
            } else {
                priceEditText.setEnabled(true);
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());

        addCategoryButton.setOnClickListener(v -> showNewCategoryDialog());

        // Show dropdown on click / focus
        categoryDropdown.setThreshold(0);
        categoryDropdown.setOnClickListener(v -> categoryDropdown.showDropDown());
        categoryDropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                categoryDropdown.showDropDown();
            }
        });

        builder.setView(view);
        Dialog dialog = builder.create();

        loadItem();
        loadCategories();

        addButton.setOnClickListener(v -> saveChanges(dialog));

        return dialog;
    }

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

    private void loadCategories() {
        categoriesRef.orderByChild("title")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        categoryList.clear();
                        categoryTitles.clear();
                        categoryTitlesLowerSet.clear();

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

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                categoryTitles
                        );
                        categoryDropdown.setAdapter(adapter);

                        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
                            if (position >= 0 && position < categoryList.size()) {
                                selectedCategoryId = categoryList.get(position).getKey();
                            }
                        });

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

    private void saveChanges(Dialog dialog) {
        if (currentItem == null) return;

        String name     = nameEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String desc     = descriptionEditText.getText().toString().trim();

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

        if (!"available".equals(currentItem.getStatus())) {
            Toast.makeText(getContext(),
                    "Only available items can be edited",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

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
