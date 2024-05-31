package com.example.drawerbottomnavigative;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.drawerbottomnavigative.fragment.FavoriteFragment;
import com.example.drawerbottomnavigative.fragment.HomeFragment;
import com.example.drawerbottomnavigative.fragment.SlideshowFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout mDrawerLayout;
    private BottomNavigationView bottomNavigationView;
    private MqttHandler mqttManager;

    private static int FRAGMENT_HOME = 0;
    private static int FRAGMENT_FAVORITE = 1;
    private static int FRAGMENT_SLIDESHOW = 2;
    private int mCurrentFragment = FRAGMENT_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mqttManager = MqttHandler.getInstance();

        // Ánh xạ và thiết lập thanh công cụ (toolbar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Ánh xạ và thiết lập giao diện trượt (DrawerLayout)
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Ánh xạ và thiết lập NavigationView (giao diện điều hướng)
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Thay thế Fragment ban đầu bằng HomeFragment
        replacementFragment(new HomeFragment());
        // Đánh dấu mục "Home" trong menu điều hướng là đã được chọn
        navigationView.getMenu().findItem(R.id.nav_home).setChecked(true);

        // Ánh xạ và thiết lập BottomNavigationView (thanh điều hướng dưới cùng)
        bottomNavigationView = findViewById(R.id.bottom_nav);
        if (savedInstanceState == null) {
            replacementFragment(new HomeFragment());
            mCurrentFragment = FRAGMENT_HOME;
            selectDrawerMenuItem(R.id.nav_home); // Cập nhật NavigationView và BottomNavigationView
        }
        // Xử lý sự kiện khi người dùng chọn một mục trong BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_home:
                        if (mCurrentFragment != FRAGMENT_HOME) {
                            replacementFragment(new HomeFragment());
                            mCurrentFragment = FRAGMENT_HOME;
                            selectDrawerMenuItem(R.id.nav_home); // Cập nhật NavigationView
                        }
                        break;
                    case R.id.nav_favorite:
                        if (mCurrentFragment != FRAGMENT_FAVORITE) {
                            replacementFragment(new FavoriteFragment());
                            mCurrentFragment = FRAGMENT_FAVORITE;
                            selectDrawerMenuItem(R.id.nav_favorite); // Cập nhật NavigationView
                        }
                        break;
                    case R.id.nav_slideshow:
                        if (mCurrentFragment != FRAGMENT_SLIDESHOW) {
                            replacementFragment(new SlideshowFragment());
                            mCurrentFragment = FRAGMENT_SLIDESHOW;
                            selectDrawerMenuItem(R.id.nav_slideshow); // Cập nhật NavigationView
                        }
                        break;
                }
                // Đóng giao diện trượt sau khi thay đổi fragment
                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }
    public MqttHandler getMQTTManager() {
        return mqttManager;
    }
    // Hàm này được sử dụng để cập nhật mục được chọn trong cả NavigationView và BottomNavigationView
    private void selectDrawerMenuItem(int menuItemId) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(menuItemId);

        switch (menuItemId) {
            case R.id.nav_home:
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
                break;
            case R.id.nav_favorite:
                bottomNavigationView.setSelectedItemId(R.id.nav_favorite);
                break;
            case R.id.nav_slideshow:
                bottomNavigationView.setSelectedItemId(R.id.nav_slideshow);
                break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        selectDrawerMenuItem(itemId);

        if (itemId == R.id.nav_home) {
            replacementFragment(new HomeFragment());
            mCurrentFragment = FRAGMENT_HOME;
        } else if (itemId == R.id.nav_favorite) {
            if (mCurrentFragment != FRAGMENT_FAVORITE) {
                replacementFragment(new FavoriteFragment());
                mCurrentFragment = FRAGMENT_FAVORITE;
            }
        } else if (itemId == R.id.nav_slideshow) {
            if (mCurrentFragment != FRAGMENT_SLIDESHOW) {
                replacementFragment(new SlideshowFragment());
                mCurrentFragment = FRAGMENT_SLIDESHOW;
            }
        }

        // Đóng giao diện trượt sau khi thay đổi mục
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Hàm này thực hiện thay thế Fragment hiện tại bằng một Fragment mới
    private void replacementFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }

}
