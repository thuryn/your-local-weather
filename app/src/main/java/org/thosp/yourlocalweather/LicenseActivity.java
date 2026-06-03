package org.thosp.yourlocalweather;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.thosp.yourlocalweather.databinding.ActivityLicenseBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.thosp.yourlocalweather.R.string.title_activity_license;

public class LicenseActivity extends AppCompatActivity {

    // 1. Deklarujeme instanční proměnnou pro vygenerovaný binding
    private ActivityLicenseBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        // 2. Inicializujeme View Binding a nastavíme root view
        binding = ActivityLicenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupActionBar();

        final String path = getIntent().getData().getPath();

        setTitle(getString(title_activity_license).replace("%s", path.substring(24)));

        try {
            final String licenseText = readLicense(getAssets().open(path.substring(15)));

            // 3. K TextView přistupujeme bezpečně a přímo skrze binding objekt
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.licenseLicenseText.setText(Html.fromHtml(licenseText.replace("\n\n", "<br/><br/>"), Html.FROM_HTML_MODE_LEGACY));
            } else {
                binding.licenseLicenseText.setText(Html.fromHtml(licenseText.replace("\n\n", "<br/><br/>")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupActionBar() {
        // 4. Toolbar získáváme typově bezpečně přímo z bindingu
        setSupportActionBar(binding.toolbar);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}