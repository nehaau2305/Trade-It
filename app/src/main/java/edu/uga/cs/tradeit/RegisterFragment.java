package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterFragment extends Fragment {

    //variables
    private EditText nameInput, emailInput, passwInput;
    private Button registerB, loginB;
    private FirebaseAuth auth;
    private DatabaseReference usersDbRef;

    // empty  default constructor
    public RegisterFragment() {  }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState ) {
        // Inflate the layout for this fragment
        View view = inflater.inflate( R.layout.fragment_register, container, false );

        // Firebase
        auth = FirebaseAuth.getInstance();
        usersDbRef = FirebaseDatabase.getInstance().getReference("users");

        // UI
        nameInput = view.findViewById(R.id.nameEditText);
        emailInput = view.findViewById(R.id.emailEditText);
        passwInput = view.findViewById(R.id.passwEditText);
        registerB = view.findViewById(R.id.registerButton);
        loginB = view.findViewById(R.id.navLoginButton);

        registerB.setOnClickListener(v -> registerUser());
        loginB.setOnClickListener(v -> switchToLogin());

        return view;
    }

    public void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwInput.getText().toString().trim();

        // check if empty
        if (name.isEmpty()) {
            nameInput.setError("Name Required");
        }
        if (email.isEmpty()) {
            nameInput.setError("Email Required");
        }
        if (password.isEmpty()) {
            nameInput.setError("Password Required");
        }

        // disable buttons while loading
        registerB.setEnabled(false);
        loginB.setEnabled(false);

        // Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(!task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "Account Registration Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();

                        registerB.setEnabled(true);
                        loginB.setEnabled(true);
                        return;
                    } // check if task successful

                    String uid = auth.getCurrentUser().getUid();
                    User user = new User(name);

                    // save user to database
                    usersDbRef.child(uid).setValue(user)
                            .addOnCompleteListener(saveTask -> {
                                registerB.setEnabled(true);
                                loginB.setEnabled(true);
                                if (saveTask.isSuccessful()) {
                                    Toast.makeText(getContext(), "Account created", Toast.LENGTH_SHORT).show();
                                    switchToLogin();
                                } else {
                                    Toast.makeText(getContext(), "Failed to save user to database: " + saveTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                });

    }

    public void switchToLogin() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new LoginFragment())
                .commit();
    }
}
