package edu.uga.cs.tradeit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class CategoryRecyclerAdapter
        extends RecyclerView.Adapter<CategoryRecyclerAdapter.CategoryHolder> {

    private List<Category> categories;

    public CategoryRecyclerAdapter(List<Category> categories) {
        this.categories = categories;
    }

    static class CategoryHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;

        public CategoryHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.categoryTitleTextView);
            dateTextView  = itemView.findViewById(R.id.categoryDateTextView);
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
            String formatted = DateFormat.getDateTimeInstance().format(new Date(time));
            holder.dateTextView.setText("Created: " + formatted);
        } else {
            holder.dateTextView.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return (categories == null) ? 0 : categories.size();
    }
}
