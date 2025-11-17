package edu.uga.cs.tradeit;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class AuthenticateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticate);

        // get fragment type from MainActivity
        String fragmentType = getIntent().getStringExtra("fragmentType");

        // initialize fragment
        Fragment fragment = null;
        switch (fragmentType) {
            case "login":
                fragment = new LoginFragment();
                break;
            case "signup":
                fragment = new RegisterFragment();
                break;
        }

        // update fragment container view
        if (fragment != null && savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, fragment)
                    .commit();
        }
    } // onCreate

    /**
     * onConfigurationChanged handles rotation by forcing layout recreation.
     * This ensures the correct orientation layout (portrait/land) loads immediately.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate(); // reloads layout in the new orientation
    }

}
