package edu.uga.cs.tradeit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Collections;
import java.util.Comparator;


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
    private AutoCompleteTextView itemCategoryDropdown;
    private EditText itemNameEditText;
    private EditText priceEditText;
    private EditText descriptionEditText;
    private CheckBox freeCheckBox;
    private Button addButton;
    private Button cancelButton;
    private Button addCategoryButton;

    // categories
    private List<Category> categoryList = new ArrayList<>();
    private List<String> categoryTitles = new ArrayList<>();
    private String selectedCategoryKey = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_item, null);

        itemCategoryDropdown = view.findViewById(R.id.itemCategoryDropdown);
        itemNameEditText     = view.findViewById(R.id.itemNameEditText);
        priceEditText        = view.findViewById(R.id.priceEditText);
        descriptionEditText  = view.findViewById(R.id.descriptionEditText);
        freeCheckBox         = view.findViewById(R.id.freeCheckBox);
        addButton            = view.findViewById(R.id.addButton);
        cancelButton         = view.findViewById(R.id.cancelButton);
        addCategoryButton    = view.findViewById(R.id.addCategoryButton);

        // show all options when user taps the category box
        itemCategoryDropdown.setThreshold(0);
        itemCategoryDropdown.setOnClickListener(v -> itemCategoryDropdown.showDropDown());
        itemCategoryDropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                itemCategoryDropdown.showDropDown();
            }
        });

        builder.setView(view);
        Dialog dialog = builder.create();

        // load categories from Firebase
        loadCategories();

        // when user picks an existing category
        itemCategoryDropdown.setOnItemClickListener((parent, v, position, id) -> {
            if (position >= 0 && position < categoryList.size()) {
                Category selected = categoryList.get(position);
                selectedCategoryKey = selected.getKey();
            } else {
                selectedCategoryKey = null;
            }
        });

        // price formatting (max 2 decimal places)
        priceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String txt = s.toString();
                if (txt.contains(".")) {
                    int i = txt.indexOf(".");
                    if (txt.length() - i - 1 > 2) {
                        s.delete(i + 3, txt.length());
                    }
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        // new category button
        addCategoryButton.setOnClickListener(v -> showCreateCategoryDialog());

        // free checkbox
        freeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                priceEditText.setText("");
                priceEditText.setEnabled(false);
            } else {
                priceEditText.setEnabled(true);
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        addButton.setText("Add Item");
        addButton.setOnClickListener(v -> addNewItem(dialog));

        return dialog;
    }

    private void loadCategories() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            if (isAdded()) {
                Toast.makeText(getContext(), "User not recognized", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DatabaseReference catRef = FirebaseDatabase.getInstance()
                .getReference("categories");

        catRef.orderByChild("creatorId").equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        categoryList.clear();
                        categoryTitles.clear();

                        // 1) collect all categories
                        for (DataSnapshot catSnap : snapshot.getChildren()) {
                            Category c = catSnap.getValue(Category.class);
                            if (c != null) {
                                if (c.getKey() == null || c.getKey().isEmpty()) {
                                    c.setKey(catSnap.getKey());
                                }
                                categoryList.add(c);
                            }
                        }

                        // 2) sort by title (case-insensitive)
                        Collections.sort(categoryList, new Comparator<Category>() {
                            @Override
                            public int compare(Category c1, Category c2) {
                                String t1 = (c1.getTitle() == null) ? "" : c1.getTitle();
                                String t2 = (c2.getTitle() == null) ? "" : c2.getTitle();
                                return t1.compareToIgnoreCase(t2);
                            }
                        });

                        // 3) rebuild titles list in sorted order
                        for (Category c : categoryList) {
                            categoryTitles.add(c.getTitle());
                        }

                        // 4) set adapter
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                categoryTitles
                        );
                        itemCategoryDropdown.setAdapter(adapter);
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


    private void showCreateCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("New Category");

        final EditText input = new EditText(requireContext());
        input.setHint("Category title");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(getContext(),
                        "Title cannot be empty",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            createCategoryInFirebase(title);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void createCategoryInFirebase(String title) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            if (isAdded()) {
                Toast.makeText(getContext(), "User not recognized", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // --- DUPLICATE CHECK (case-insensitive) ----
        for (Category c : categoryList) {
            if (c.getTitle() != null &&
                    c.getTitle().trim().equalsIgnoreCase(title.trim())) {
                if (isAdded()) {
                    Toast.makeText(getContext(),
                            "Category already exists",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }

        DatabaseReference catRef = FirebaseDatabase.getInstance()
                .getReference("categories");

        String key = catRef.push().getKey();
        if (key == null) {
            if (isAdded()) {
                Toast.makeText(getContext(),
                        "Failed to create category",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        long time = System.currentTimeMillis();
        Category newCategory = new Category(currentUid, title, time);
        newCategory.setKey(key);

        catRef.child(key).setValue(newCategory)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;

                    selectedCategoryKey = key;
                    itemCategoryDropdown.setText(title, false);
                    itemCategoryDropdown.showDropDown();

                    Toast.makeText(getContext(),
                            "Category created",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Failed to save category",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void addNewItem(Dialog dialog) {
        String name = itemNameEditText.getText().toString().trim();
        String priceString = priceEditText.getText().toString().trim();
        String desc = descriptionEditText.getText().toString().trim();

        if (name.isEmpty() || desc.isEmpty()) {
            Toast.makeText(getContext(), "Fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryKey == null || selectedCategoryKey.isEmpty()) {
            Toast.makeText(getContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = 0.0;
        if (!freeCheckBox.isChecked()) {
            if (priceString.isEmpty()) {
                Toast.makeText(getContext(),
                        "Enter a price or mark as free",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                price = Double.parseDouble(priceString);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(),
                        "Price invalid",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        DatabaseReference itemsRef =
                FirebaseDatabase.getInstance().getReference("items");
        String key = itemsRef.push().getKey();
        FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
        long time = System.currentTimeMillis();

        if (currUser != null && key != null) {
            String sellerId = currUser.getUid();

            Item newItem = new Item(
                    sellerId,
                    name,
                    selectedCategoryKey,
                    time,
                    price,
                    "available",
                    desc
            );
            newItem.setKey(key);

            itemsRef.child(key).setValue(newItem)
                    .addOnSuccessListener(aVoid -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(),
                                "Successfully added item",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(),
                                "Failed to add item",
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            if (isAdded()) {
                Toast.makeText(getContext(),
                        "User not recognized",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
