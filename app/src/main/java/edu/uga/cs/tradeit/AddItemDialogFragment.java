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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddItemDialogFragment extends DialogFragment {

    // UI fields
    private EditText itemNameEditText;
    private EditText priceEditText;
    private CheckBox freeCheckBox;
    private Button addButton;
    private Button cancelButton;
    private Button addCategoryButton;
    private AutoCompleteTextView itemCategoryDropdown;

    // Firebase references
    private DatabaseReference itemsDbRef;
    private DatabaseReference categoriesDbRef;

    // category data
    private List<Category> categoryList = new ArrayList<>();
    private List<String> categoryTitles = new ArrayList<>();
    private String selectedCategoryKey = null; // id of chosen category

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_item, null);

        // hook up UI elements from XML
        itemNameEditText     = view.findViewById(R.id.itemNameEditText);
        priceEditText        = view.findViewById(R.id.priceEditText);
        freeCheckBox         = view.findViewById(R.id.freeCheckBox);
        addButton            = view.findViewById(R.id.addButton);
        cancelButton         = view.findViewById(R.id.cancelButton);
        addCategoryButton    = view.findViewById(R.id.addCategoryButton);
        itemCategoryDropdown = view.findViewById(R.id.itemCategoryDropdown);

        // Firebase references
        itemsDbRef      = FirebaseDatabase.getInstance().getReference("items");
        categoriesDbRef = FirebaseDatabase.getInstance().getReference("categories");

        // load existing categories into dropdown
        loadCategories();

        // when user picks a category from dropdown, remember its key
        itemCategoryDropdown.setOnItemClickListener((parent, v, position, id) -> {
            if (position >= 0 && position < categoryList.size()) {
                Category c = categoryList.get(position);
                selectedCategoryKey = c.getKey();
            } else {
                selectedCategoryKey = null;
            }
        });

        // free checkbox: disable price field when checked
        freeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                priceEditText.setText("");
                priceEditText.setEnabled(false);
            } else {
                priceEditText.setEnabled(true);
            }
        });

        // "+ New" category button
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());

        // "Add Item" button
        addButton.setOnClickListener(v -> addNewItem());

        // "Cancel" button
        cancelButton.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }

    // -------- load categories from /categories into the dropdown  ----------
    private void loadCategories() {
        categoriesDbRef.orderByChild("title")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        categoryList.clear();
                        categoryTitles.clear();

                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            Category c = catSnapshot.getValue(Category.class);
                            if (c != null) {
                                if (c.getKey() == null || c.getKey().isEmpty()) {
                                    c.setKey(catSnapshot.getKey());
                                }
                                categoryList.add(c);
                                categoryTitles.add(c.getTitle());
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                categoryTitles
                        );
                        itemCategoryDropdown.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                getContext(),
                                "Failed to load categories",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    //  ---------- small dialog to create a new category --------
    private void showAddCategoryDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Category name");

        new AlertDialog.Builder(requireContext())
                .setTitle("New Category")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(
                                getContext(),
                                "Category name required",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currUser == null) {
                        Toast.makeText(
                                getContext(),
                                "User not recognized",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String creatorId = currUser.getUid();
                    long now = System.currentTimeMillis();

                    Category newCat = new Category(creatorId, title, now);
                    String newKey = categoriesDbRef.push().getKey();
                    if (newKey == null) {
                        Toast.makeText(
                                getContext(),
                                "Failed to create category",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    newCat.setKey(newKey);

                    categoriesDbRef.child(newKey).setValue(newCat)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(
                                        getContext(),
                                        "Category created",
                                        Toast.LENGTH_SHORT
                                ).show();

                                // remember and show this new category
                                selectedCategoryKey = newKey;
                                itemCategoryDropdown.setText(title, false);
                                // loadCategories() listener will refresh the list
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            getContext(),
                                            "Failed to save category",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // -------- validate fields and save a new item into /items  ----------
    private void addNewItem() {
        String name = itemNameEditText.getText().toString().trim();
        String priceString = priceEditText.getText().toString().trim();
        boolean isFree = freeCheckBox.isChecked();

        // item name required
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Item name required", Toast.LENGTH_SHORT).show();
            return;
        }

        // category must be selected
        if (selectedCategoryKey == null || selectedCategoryKey.isEmpty()) {
            Toast.makeText(getContext(), "Select or create a category", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedCategoryTitle = itemCategoryDropdown.getText().toString().trim();

        double priceValue;
        if (isFree) {
            priceValue = 0.0;
        } else {
            if (priceString.isEmpty()) {
                Toast.makeText(getContext(),
                        "Price required or mark item as free",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                priceValue = Double.parseDouble(priceString);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currUser == null) {
            Toast.makeText(getContext(), "User not recognized", Toast.LENGTH_SHORT).show();
            return;
        }

        String sellerId = currUser.getUid();
        long time = System.currentTimeMillis();

        String key = itemsDbRef.push().getKey();
        if (key == null) {
            Toast.makeText(getContext(), "Failed to create item key", Toast.LENGTH_SHORT).show();
            return;
        }

        // create Item with BOTH categoryId and categoryTitle
        Item newItem = new Item(
                sellerId,
                name,
                selectedCategoryKey,     // categoryId
                selectedCategoryTitle,   // categoryTitle
                time,
                priceValue,
                "available"
        );
        newItem.setKey(key);

        itemsDbRef.child(key).setValue(newItem)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item added", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to add item", Toast.LENGTH_SHORT).show()
                );
    }
}
