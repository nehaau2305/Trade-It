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

/**
 * UserActivity hosts all fragments available to a logged in user.
 * After logging in, the home fragment is created allowing the user to view
 * all items posted. From the toolbar, the user can additionally
 * navigate to the My Items or Transactions fragments allowing them to
 * view all the items & categories the current user created &
 * all the transactions the user is a part of.
 */
public class UserActivity extends AppCompatActivity {

    /**
     * onCreate sets up the toolbar & hosts the Home fragment after the user logs in.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
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

    /**
     * onCreateOptionsMenu inflates the menu.
     * @param menu The options menu in which you place your items.
     *
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_menu, menu);
        return true;
    }

    /**
     * onOptionsItemSelected initializes the appropriate fragment
     * depending on the user's selection. The user can navigate to the
     * home fragment, to view all available items, transaction fragment,
     * to view all the items the user has requested to buy, items the
     * current user has posted that others have requested to buy, & all the
     * completed transactions, & finally the my items fragments that displays
     * all the items & categories the current user has created.
     * @param item The menu item that was selected.
     *
     * @return
     */
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
