package edu.uga.cs.tradeit;

import android.content.Intent;
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

/**
 * LoginFragments prompts the user to enter their email & password to begin
 * using the application.
 */
public class LoginFragment extends Fragment {
    // initialize variables
    private EditText emailInput, passwInput;
    private Button loginB, registerB;
    private FirebaseAuth auth;
    // constructor
    public LoginFragment() { }

    /**
     * onCreateView binds the UI elements & sets up the button click listeners.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate this fragment's layout
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance();

        // Hook up UI
        emailInput   = view.findViewById(R.id.emailEditText2);
        passwInput   = view.findViewById(R.id.passwEditText2);
        loginB       = view.findViewById(R.id.loginButton2);
        registerB    = view.findViewById(R.id.navSignupButton);

        // Button listeners
        loginB.setOnClickListener(v -> loginUser());
        registerB.setOnClickListener(v -> switchToRegister());

        return view;
    }

    /**
     * loginUser checks if the user entered a value for the email field, if the
     * entered email value is in a valid format, & if the user entered a password value.
     * The method calls the database to check is the account exists. If the user
     * successfully logs in, the user is navigated to the Home Fragment.
     */
    private void loginUser() {
        String email    = emailInput.getText().toString().trim();
        String password = passwInput.getText().toString().trim();

        // ---------- validate input ----------
        boolean hasError = false;

        if (email.isEmpty()) {
            emailInput.setError("Email required");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // check email format
            emailInput.setError("Enter a valid email");
            hasError = true;
        }

        if (password.isEmpty()) {
            passwInput.setError("Password required");
            hasError = true;
        }

        // if any validation failed, stop here
        if (hasError) {
            return;
        }

        // ---------- call Firebase ----------
        loginB.setEnabled(false);
        registerB.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    // re-enable buttons when Firebase reply returns
                    loginB.setEnabled(true);
                    registerB.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "Login successful",
                                Toast.LENGTH_SHORT).show();

                        // Go to the user/home activity
                        Intent intent = new Intent(getActivity(), UserActivity.class);
                        intent.putExtra("fragmentType", "Home");
                        startActivity(intent);

                        // prevent going back to login
                        requireActivity().finish();
                    } else {
                        Toast.makeText(getContext(),
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * switchToRegister allows the user to navigate to the Register Fragment
     * if they decide to create an account instead of logging in.
     */
    private void switchToRegister() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new RegisterFragment())
                .commit();
    }
}
