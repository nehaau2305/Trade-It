package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterFragment extends Fragment {

    // UI components
    private EditText nameInput, emailInput, passwInput;
    private Button registerB, loginB;

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference usersDbRef;

    // Empty default constructor
    public RegisterFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate this fragment's layout
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // Firebase setup
        auth = FirebaseAuth.getInstance();
        usersDbRef = FirebaseDatabase.getInstance().getReference("users");

        // Hook up UI
        nameInput   = view.findViewById(R.id.nameEditText);
        emailInput  = view.findViewById(R.id.emailEditText);
        passwInput  = view.findViewById(R.id.passwEditText);
        registerB   = view.findViewById(R.id.registerButton);
        loginB      = view.findViewById(R.id.navLoginButton);

        // Button listeners
        registerB.setOnClickListener(v -> registerUser());
        loginB.setOnClickListener(v -> switchToLogin());

        return view;
    }

    // Handle registration logic
    private void registerUser() {
        String name     = nameInput.getText().toString().trim();
        String email    = emailInput.getText().toString().trim();
        String password = passwInput.getText().toString().trim();

        // --------- validate input ----------
        boolean hasError = false;

        if (name.isEmpty()) {
            nameInput.setError("Name required");
            hasError = true;
        }

        if (email.isEmpty()) {
            emailInput.setError("Email required");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            hasError = true;
        }

        if (password.isEmpty()) {
            passwInput.setError("Password required");
            hasError = true;
        } else if (password.length() < 6) {
            // Firebase minimum
            passwInput.setError("Password must be at least 6 characters");
            hasError = true;
        }

        if (hasError) {
            return; // stop if validation failed
        }

        // -------- call Firebase ---------
        registerB.setEnabled(false);
        loginB.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        // Check if email already used (duplicate)
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            emailInput.setError("Email already in use");
                        }

                        Toast.makeText(getContext(),
                                "Account registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();

                        registerB.setEnabled(true);
                        loginB.setEnabled(true);
                        return;
                    }

                    // Get uid of newly created user
                    String uid = auth.getCurrentUser().getUid();

                    // Create user object with name + email
                    User user = new User(name, email);

                    // Save user under /users/uid in Realtime Database
                    usersDbRef.child(uid).setValue(user)
                            .addOnCompleteListener(saveTask -> {

                                registerB.setEnabled(true);
                                loginB.setEnabled(true);

                                if (saveTask.isSuccessful()) {
                                    Toast.makeText(getContext(),
                                            "Account created. Please log in.",
                                            Toast.LENGTH_SHORT).show();

                                    // Go back to login screen
                                    switchToLogin();
                                } else {
                                    Toast.makeText(getContext(),
                                            "Failed to save user: " +
                                                    saveTask.getException().getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                });
    }

    // Switch back to login fragment
    private void switchToLogin() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new LoginFragment())
                .commit();
    }
}
