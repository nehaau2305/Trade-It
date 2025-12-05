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

/**
 * This adapter connects a list of Category objects
 * to a RecyclerView, so they can be shown on the screen.
 *
 * It also lets the user edit or delete a category, but
 * only if there are no active (not completed) items in it.
 */
public class CategoryRecyclerAdapter
        extends RecyclerView.Adapter<CategoryRecyclerAdapter.CategoryHolder> {

    // list of categories to show
    private List<Category> categories;

    // reference to "categories" in Firebase Realtime Database
    private DatabaseReference categoriesRef =
            FirebaseDatabase.getInstance().getReference("categories");
    // reference to "items" in Firebase, used to check if a category is in use
    private DatabaseReference itemsRef =
            FirebaseDatabase.getInstance().getReference("items");

    /**
     * Simple constructor that just stores the list of categories.
     * @param categories the list of categories to display
     */
    public CategoryRecyclerAdapter(List<Category> categories) {
        this.categories = categories;
    }

    /**
     * Holds the views (widgets) for each row in the list.
     * One CategoryHolder = one row showing one Category.
     */
    class CategoryHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        TextView infoTextView;
        Button editButton;
        Button deleteButton;

        /**
         * Here we connect the Java fields to the views in category_row.xml
         * @param itemView a single row layout
         */
        public CategoryHolder(View itemView) {
            super(itemView);
            titleTextView  = itemView.findViewById(R.id.categoryTitleTextView);
            dateTextView   = itemView.findViewById(R.id.categoryDateTextView);
            infoTextView   = itemView.findViewById(R.id.categoryInfoTextView);
            editButton     = itemView.findViewById(R.id.editCategoryButton);
            deleteButton   = itemView.findViewById(R.id.deleteCategoryButton);
        }
    }

    /**
     * This is called when a new row view needs to be created.
     * It inflates (builds) the layout for one category row.
     */
    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.category_row, parent, false);
        return new CategoryHolder(v);
    }

    /**
     * This is called to fill a row with data from a Category.
     * It sets the title, date, info text, and button actions.
     */
    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, int position) {
        Category category = categories.get(position);

        // set the category title
        holder.titleTextView.setText(category.getTitle());

        // show the date/time if we have it
        long time = category.getDateTime();
        if (time > 0) {
            String formatted = DateFormat.getDateTimeInstance()
                    .format(new Date(time));
            holder.dateTextView.setText("Created: " + formatted);
        } else {
            holder.dateTextView.setText("");
        }

        // info note to the user about when categories can be changed
        holder.infoTextView.setText(
                "Only categories with no active items can be edited or deleted."
        );

        // when edit button is clicked, check first if category has any active items
        holder.editButton.setOnClickListener(v ->
                checkCategoryEmptyThenEdit(holder, category));

        // when delete button is clicked, also check first if category is empty
        holder.deleteButton.setOnClickListener(v ->
                checkCategoryEmptyThenDelete(holder, category));
    }

    /**
     * Returns how many categories there are to show.
     */
    @Override
    public int getItemCount() {
        return (categories == null) ? 0 : categories.size();
    }

    /**
     * Before editing a category, this checks Firebase "items"
     * to see if any items in this category are still active
     * (not completed). If it is empty, then we let the user edit.
     */
    private void checkCategoryEmptyThenEdit(CategoryHolder holder, Category category) {
        String categoryId = category.getKey();
        if (categoryId == null || categoryId.isEmpty()) {
            Toast.makeText(holder.itemView.getContext(),
                    "Invalid category",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // look up items whose "categoryId" field matches this category key
        itemsRef.orderByChild("categoryId").equalTo(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasActive = false;

                        // loop over all items in this category
                        for (DataSnapshot s : snapshot.getChildren()) {
                            Item item = s.getValue(Item.class);
                            if (item == null) continue;
                            String status = item.getStatus();
                            // if status is not "completed", then it is still active
                            if (status == null || !status.equals("completed")) {
                                hasActive = true;
                                break;
                            }
                        }

                        if (hasActive) {
                            // tell the user they cannot edit if category is still in use
                            Toast.makeText(holder.itemView.getContext(),
                                    "Category is in use. Only categories with no active items (available/pending) can be edited.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // safe to edit
                            showEditDialog(holder, category);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // something went wrong while checking Firebase
                        Toast.makeText(holder.itemView.getContext(),
                                "Failed to check category usage",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Shows a simple pop-up dialog with an EditText
     * so the user can type a new category name and save it.
     */
    private void showEditDialog(CategoryHolder holder, Category category) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(holder.itemView.getContext());
        builder.setTitle("Edit Category");

        final EditText input = new EditText(holder.itemView.getContext());
        input.setHint("Category name");
        input.setText(category.getTitle());
        builder.setView(input);

        // when user taps "Save"
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

            // update the title in Firebase "categories" node
            categoriesRef.child(key).child("title").setValue(newTitle)
                    .addOnSuccessListener(aVoid -> {
                        // also update local object and refresh this row
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

        // if user taps "Cancel", just close the dialog
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Before deleting a category, we again check in Firebase if there
     * are any active items in it. If it is empty (or only completed items),
     * then we go ahead and let the user delete the category.
     */
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

                        // check all items for this category
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
                            // cannot delete if there are still active items
                            Toast.makeText(holder.itemView.getContext(),
                                    "Category is in use. Only categories with no active items (available/pending) can be deleted.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // ask for final confirmation to delete
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

    /**
     * Shows a confirmation pop-up asking if the user really wants
     * to delete this category. If yes, it removes it from Firebase
     * and from the local list.
     */
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

            // remove the category from Firebase
            categoriesRef.child(key).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // also remove it from the local list and update the UI
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
