package edu.uga.cs.tradeit;

import android.content.Intent;
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

public class LoginFragment extends Fragment {

    //variables
    private EditText emailInput, passwInput;
    private Button loginB, registerB;
    private FirebaseAuth auth;

    // empty  default constructor
    public LoginFragment() {  }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState ) {
        // Inflate the layout for this fragment
        View view = inflater.inflate( R.layout.fragment_login, container, false );

        // Firebase
        auth = FirebaseAuth.getInstance();

        // UI
        emailInput = view.findViewById(R.id.emailEditText2);
        passwInput = view.findViewById(R.id.passwEditText2);
        loginB = view.findViewById(R.id.loginButton2);
        registerB = view.findViewById(R.id.navSignupButton);

        loginB.setOnClickListener(v -> loginUser());
        registerB.setOnClickListener(v -> switchToRegister());

        return view;
    }

    public void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwInput.getText().toString().trim();

        // check if empty
        if (email.isEmpty()) {
            emailInput.setError("Email Required");
        }
        if (password.isEmpty()) {
            passwInput.setError("Password Required");
        }

        // disable buttons while loading
        loginB.setEnabled(false);
        registerB.setEnabled(false);

        // Firebase Auth
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // enable buttons
                    loginB.setEnabled(true);
                    registerB.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getActivity(), UserActivity.class);
                        // to prevent user from going back to login page
                        intent.putExtra("fragmentType", "Home");
                        startActivity(intent);
                        getActivity().finish();
                    } else {
                        Toast.makeText(getContext(), "Login Unsuccessful" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }

    public void switchToRegister() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new RegisterFragment())
                .commit();
    }
}
