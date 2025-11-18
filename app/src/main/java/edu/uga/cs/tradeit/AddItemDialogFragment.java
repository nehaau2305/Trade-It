package edu.uga.cs.tradeit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddItemDialogFragment extends DialogFragment {
    private EditText itemName, price;
    private Button addB;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_item, null);
        // UI
        itemName = view.findViewById(R.id.itemNameEditText);
        price = view.findViewById(R.id.priceEditText);
        addB = view.findViewById(R.id.addButton);

        builder.setView(view);
        Dialog dialog = builder.create();

        // button click listener
        addB.setOnClickListener(v -> {
            String name = itemName.getText().toString().trim();
            String priceString = price.getText().toString().trim();
            // check empty
            if (name.isEmpty() || priceString.isEmpty()) {
                Toast.makeText(getContext(), "Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceString);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Price invalid", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference itemsDbRef = FirebaseDatabase.getInstance().getReference("items");
            String key = itemsDbRef.push().getKey();
            FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
            long time = System.currentTimeMillis();
            if (currUser != null && key != null) {
                String sellerId = currUser.getUid();
                Item newItem = new Item(sellerId, name, "category1", time, price, "available");
                newItem.setKey(key);
                itemsDbRef.child(key).setValue(newItem)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Successfully added item", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Failed to add item", Toast.LENGTH_SHORT).show()
                        );
            } else {
                Toast.makeText(getContext(), "User not recognized", Toast.LENGTH_SHORT).show();
            }
        });

        return dialog;
    }
}
