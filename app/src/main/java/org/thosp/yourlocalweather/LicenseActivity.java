package org.thosp.yourlocalweather;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.thosp.yourlocalweather.R.string.title_activity_license;

public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        setupActionBar();

        final String path = getIntent().getData().getPath();

        setTitle(getString(title_activity_license).replace("%s", path.substring(24)));

        try {
            TextView licenseTextView = findViewById(R.id.license_license_text);
            final String licenseText = readLicense(getAssets().open(path.substring(15)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                licenseTextView.setText(Html.fromHtml(licenseText.replace("\n\n", "<br/><br/>"), Html.FROM_HTML_MODE_LEGACY));
            } else {
                licenseTextView.setText(Html.fromHtml(licenseText.replace("\n\n", "<br/><br/>")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String readLicense(final InputStream inputStream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuilder builder = new StringBuilder();
        try {
            String stringRead;
            while ((stringRead = reader.readLine()) != null) {
                builder.append(stringRead).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
        return builder.toString();
    }
}
