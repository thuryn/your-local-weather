package org.thosp.yourlocalweather;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.TaskStackBuilder;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.thosp.yourlocalweather.help.HelpActivity;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.service.WeatherRequestDataHolder;
import org.thosp.yourlocalweather.settings.fragments.AboutPreferenceFragment;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.LanguageUtil;
import org.thosp.yourlocalweather.utils.Utils;

public abstract class BaseActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    //private TextView mHeaderCity;
    protected LocationsDbHelper locationsDbHelper;
    protected Location currentLocation;
    protected TextView localityView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        getToolbar();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavDrawer();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageUtil.setLanguage(base, AppPreference.getInstance().getLanguage(base)));
    }

    public void switchLocation(View arg0) {
        int newLocationOrderId = 0;
        if (currentLocation != null) {
            newLocationOrderId = 1 + currentLocation.getOrderId();
        }
        currentLocation = locationsDbHelper.getLocationByOrderId(newLocationOrderId);

        if (currentLocation == null) {
            newLocationOrderId = 0;
            currentLocation = locationsDbHelper.getLocationByOrderId(newLocationOrderId);
            if ((currentLocation.getOrderId() == 0) && !currentLocation.isEnabled() && (locationsDbHelper.getAllRows().size() > 1)) {
                newLocationOrderId++;
                currentLocation = locationsDbHelper.getLocationByOrderId(newLocationOrderId);
            }
        }
        Context savedContext = this;
        YourLocalWeather.executor.submit(() -> {
            AppPreference.setCurrentLocationId(savedContext, currentLocation);
            runOnUiThread(() -> localityView.setText(Utils.getCityAndCountry(savedContext, currentLocation)));
            updateUI();
        });
    }

    protected abstract void updateUI();

    private void setupNavDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout);

        if (mDrawerLayout == null) {
            return;
        }
        mDrawerToggle = new ActionBarDrawerToggle(this,
                                                  mDrawerLayout,
                                                  mToolbar,
                                                  R.string.navigation_drawer_open,
                                                  R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        if (mToolbar != null) {
            mToolbar.setNavigationOnClickListener(view -> mDrawerLayout.openDrawer(GravityCompat.START));
        }

        configureNavView();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerLayout != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    private void configureNavView() {
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(navigationViewListener);

        View headerLayout = navigationView.getHeaderView(0);
        //mHeaderCity = headerLayout.findViewById(R.id.nav_header_city);
        //mHeaderCity.setText(Utils.getCityAndCountry(this));
    }

    private final NavigationView.OnNavigationItemSelectedListener navigationViewListener =
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.nav_menu_current_weather:
                            startActivity(new Intent(BaseActivity.this, MainActivity.class));
                            break;
                        case R.id.nav_menu_graphs:
                            createBackStack(new Intent(BaseActivity.this,
                                                       GraphsActivity.class));
                            break;
                        case R.id.nav_menu_weather_forecast:
                            createBackStack(new Intent(BaseActivity.this,
                                                       WeatherForecastActivity.class));
                            break;
                        case R.id.nav_settings:
                            createBackStack(new Intent(BaseActivity.this,
                                                       SettingsActivity.class));
                            break;
                        case R.id.nav_about:
                            Intent intent = new Intent(BaseActivity.this,
                                    SettingsActivity.class);
                            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                                    AboutPreferenceFragment.class.getName());
                            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                                    R.string.preference_title_activity_about);
                            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_SHORT_TITLE,
                                    R.string.preference_title_activity_about);
                            createBackStack(intent);
                            break;
                        case R.id.nav_menu_help:
                            createBackStack(new Intent(BaseActivity.this,
                                    HelpActivity.class));
                            break;
                        case R.id.nav_feedback:
                            Intent sendMessage = new Intent(Intent.ACTION_SEND);
                            sendMessage.setType("message/rfc822");
                            sendMessage.putExtra(Intent.EXTRA_EMAIL, new String[]{
                                    getResources().getString(R.string.feedback_email)});
                            try {
                                startActivity(Intent.createChooser(sendMessage, "Send feedback"));
                            } catch (android.content.ActivityNotFoundException e) {
                                Toast.makeText(BaseActivity.this, "Communication app not found",
                                               Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }

                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
            };

    private void createBackStack(Intent intent) {
        TaskStackBuilder builder = TaskStackBuilder.create(this);
        builder.addNextIntentWithParentStack(intent);
        builder.startActivities();
    }

    protected void getToolbar() {
        if (mToolbar == null) {
            mToolbar = findViewById(R.id.toolbar);
            if (mToolbar != null) {
                setSupportActionBar(mToolbar);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDraw();
        } else {
            super.onBackPressed();
        }
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected void closeNavDraw() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    @NonNull
    protected ProgressDialog getProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.isIndeterminate();
        dialog.setMessage(getString(R.string.load_progress));
        dialog.setCancelable(false);
        return dialog;
    }

    protected void sendMessageToReconciliationDbService() {
        String TAG = "BaseActivity";
        appendLog(this,
                TAG,
                "going run reconciliation DB service");
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_RECONCILIATION");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("force", true);
        startService(intent);
    }

    protected void sendMessageToCurrentWeatherService(Location location, String updateSource) {
        if ((location.getOrderId() == 0) &&
                (location.getLongitude() == 0) &&
                (location.getLatitude() == 0) &&
                ((location.getAddress() == null) || (location.getLastLocationUpdate() == 0))) {
            return;
        }
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_WEATHER_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(location.getId(), updateSource, UpdateWeatherService.START_CURRENT_WEATHER_UPDATE));
        startService(intent);
    }
}
