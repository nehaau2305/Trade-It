package edu.uga.cs.tradeit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * MainActivity is the initial view that is displayed that
 * prompts the user to create an account or login.
 */
public class MainActivity extends AppCompatActivity {

    // variables
    Button loginB;
    Button signUpB;

    /**
     * onCreate connects the UI elements & sets up the button listeners.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // connect buttons to the actual layout components
        loginB = findViewById(R.id.loginButton);
        signUpB = findViewById(R.id.signUpButton);

        // Set up button listeners
        View.OnClickListener listener = v -> {
            Intent intent = new Intent(MainActivity.this, AuthenticateActivity.class);
            if (v.getId() == R.id.loginButton) {
                intent.putExtra("fragmentType", "login");
            } else if (v.getId() == R.id.signUpButton) {
                intent.putExtra("fragmentType", "signup");
            }
            startActivity(intent);
        };

        // connect listeners
        loginB.setOnClickListener(listener);
        signUpB.setOnClickListener(listener);

    }
}