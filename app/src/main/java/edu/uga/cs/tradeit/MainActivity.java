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

public class MainActivity extends AppCompatActivity {

    // variables
    Button loginB;
    Button signUpB;

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