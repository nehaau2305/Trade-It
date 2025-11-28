package edu.uga.cs.tradeit;

import android.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class CategoryRecyclerAdapter
        extends RecyclerView.Adapter<CategoryRecyclerAdapter.CategoryHolder> {

    private List<Category> categories;

    private DatabaseReference categoriesRef =
            FirebaseDatabase.getInstance().getReference("categories");
    private DatabaseReference itemsRef =
            FirebaseDatabase.getInstance().getReference("items");

    public CategoryRecyclerAdapter(List<Category> categories) {
        this.categories = categories;
    }

    class CategoryHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        TextView infoTextView;
        Button editButton;
        Button deleteButton;

        public CategoryHolder(View itemView) {
            super(itemView);
            titleTextView  = itemView.findViewById(R.id.categoryTitleTextView);
            dateTextView   = itemView.findViewById(R.id.categoryDateTextView);
            infoTextView   = itemView.findViewById(R.id.categoryInfoTextView);
            editButton     = itemView.findViewById(R.id.editCategoryButton);
            deleteButton   = itemView.findViewById(R.id.deleteCategoryButton);
        }
    }

    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.category_row, parent, false);
        return new CategoryHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, int position) {
        Category category = categories.get(position);

        holder.titleTextView.setText(category.getTitle());

        long time = category.getDateTime();
        if (time > 0) {
            String formatted = DateFormat.getDateTimeInstance()
                    .format(new Date(time));
            holder.dateTextView.setText("Created: " + formatted);
        } else {
            holder.dateTextView.setText("");
        }

        holder.infoTextView.setText(
                "Only categories with no active items can be edited or deleted."
        );

        holder.editButton.setOnClickListener(v ->
                checkCategoryEmptyThenEdit(holder, category));

        holder.deleteButton.setOnClickListener(v ->
                checkCategoryEmptyThenDelete(holder, category));
    }

    @Override
    public int getItemCount() {
        return (categories == null) ? 0 : categories.size();
    }

    private void checkCategoryEmptyThenEdit(CategoryHolder holder, Category category) {
        String categoryId = category.getKey();
        if (categoryId == null || categoryId.isEmpty()) {
            Toast.makeText(holder.itemView.getContext(),
                    "Invalid category",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        itemsRef.orderByChild("categoryId").equalTo(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasActive = false;

                        for (DataSnapshot s : snapshot.getChildren()) {
                            Item item = s.getValue(Item.class);
                            if (item == null) continue;
                            String status = item.getStatus();
                            if (status == null || !status.equals("completed")) {
                                hasActive = true;
                                break;
                            }
                        }

                        if (hasActive) {
                            Toast.makeText(holder.itemView.getContext(),
                                    "Category is in use. Only categories with no active items (available/pending) can be edited.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            showEditDialog(holder, category);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(holder.itemView.getContext(),
                                "Failed to check category usage",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditDialog(CategoryHolder holder, Category category) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(holder.itemView.getContext());
        builder.setTitle("Edit Category");

        final EditText input = new EditText(holder.itemView.getContext());
        input.setHint("Category name");
        input.setText(category.getTitle());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (TextUtils.isEmpty(newTitle)) {
                Toast.makeText(holder.itemView.getContext(),
                        "Title cannot be empty",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String key = category.getKey();
            if (key == null || key.isEmpty()) {
                Toast.makeText(holder.itemView.getContext(),
                        "Invalid category",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            categoriesRef.child(key).child("title").setValue(newTitle)
                    .addOnSuccessListener(aVoid -> {
                        category.setTitle(newTitle);
                        notifyItemChanged(holder.getAdapterPosition());
                        Toast.makeText(holder.itemView.getContext(),
                                "Category updated",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(holder.itemView.getContext(),
                                    "Failed to update category",
                                    Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void checkCategoryEmptyThenDelete(CategoryHolder holder, Category category) {
        String categoryId = category.getKey();
        if (categoryId == null || categoryId.isEmpty()) {
            Toast.makeText(holder.itemView.getContext(),
                    "Invalid category",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        itemsRef.orderByChild("categoryId").equalTo(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasActive = false;

                        for (DataSnapshot s : snapshot.getChildren()) {
                            Item item = s.getValue(Item.class);
                            if (item == null) continue;
                            String status = item.getStatus();
                            if (status == null || !status.equals("completed")) {
                                hasActive = true;
                                break;
                            }
                        }

                        if (hasActive) {
                            Toast.makeText(holder.itemView.getContext(),
                                    "Category is in use. Only categories with no active items (available/pending) can be deleted.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            confirmDelete(holder, category);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(holder.itemView.getContext(),
                                "Failed to check category usage",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDelete(CategoryHolder holder, Category category) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(holder.itemView.getContext());
        builder.setTitle("Delete Category");
        builder.setMessage("Delete \"" + category.getTitle() + "\"?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            String key = category.getKey();
            if (key == null || key.isEmpty()) {
                Toast.makeText(holder.itemView.getContext(),
                        "Invalid category",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            categoriesRef.child(key).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            categories.remove(pos);
                            notifyItemRemoved(pos);
                        }
                        Toast.makeText(holder.itemView.getContext(),
                                "Category deleted",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(holder.itemView.getContext(),
                                    "Failed to delete category",
                                    Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
