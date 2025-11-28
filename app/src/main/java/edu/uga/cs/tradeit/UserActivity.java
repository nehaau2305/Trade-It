package edu.uga.cs.tradeit;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class UserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set toolbar title and make it white
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("TradeIt");
        }
        toolbar.setTitleTextColor(
                ContextCompat.getColor(this, R.color.uga_white)
        );

        // get fragment type from MainActivity
        String fragmentType = getIntent().getStringExtra("fragmentType");

        // initialize fragment
        Fragment fragment = null;
        if (fragmentType == null) {
            fragment = new HomeFragment();
        } else {
            switch (fragmentType) {
                case "Home":
                    fragment = new HomeFragment();
                    break;
                // other cases (add as needed)
            }
        } // if else

        // update fragment container view
        if (fragment != null && savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView2, fragment)
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_home) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView2, new HomeFragment())
                    .commit();
            return true;
        } else if (itemId == R.id.menu_transactions) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView2, new TransactionFragment())
                    .commit();
            return true;
        } else if (itemId == R.id.menu_my_items) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView2, new MyItemsFragment())
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
