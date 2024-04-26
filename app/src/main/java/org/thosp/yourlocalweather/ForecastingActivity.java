package org.thosp.yourlocalweather;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public abstract class ForecastingActivity extends BaseActivity {

    private final String TAG = "ForecastingActivity";

    protected Map<Long, List<DetailedWeatherForecast>> weatherForecastList = new HashMap<>();
    protected Map<Long, Long> locationWeatherForecastLastUpdate = new HashMap<>();
    protected ConnectionDetector connectionDetector;
    private ProgressDialog mGetWeatherProgress;
    private Handler mHandler;
    protected BroadcastReceiver mWeatherUpdateReceiver;
    protected String pressureUnitFromPreferences;
    protected String rainSnowUnitFromPreferences;
    protected String temperatureUnitFromPreferences;
    protected AppCompatImageButton switchLocationButton;
    //protected Switch forecastType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        mGetWeatherProgress = getProgressDialog();

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case Constants.TASK_RESULT_ERROR:
                        Toast.makeText(ForecastingActivity.this,
                                R.string.toast_parse_error,
                                Toast.LENGTH_SHORT).show();
                        setVisibleUpdating(false);
                        break;
                    case Constants.PARSE_RESULT_ERROR:
                        Toast.makeText(ForecastingActivity.this,
                                R.string.toast_parse_error,
                                Toast.LENGTH_SHORT).show();
                        setVisibleUpdating(false);
                        break;
                    case Constants.TOO_EARLY_UPDATE_ERROR:
                        Toast.makeText(ForecastingActivity.this,
                                R.string.too_early_update_error,
                                Toast.LENGTH_SHORT).show();
                        setVisibleUpdating(false);
                        break;
                    case Constants.PARSE_RESULT_SUCCESS:
                        setVisibleUpdating(false);
                        updateUI();
                        break;
                }
            }
        };

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGetWeatherProgress != null) {
            mGetWeatherProgress.dismiss();
        }
        unregisterReceiver(mWeatherUpdateReceiver);
    }

    @Override
    protected abstract void updateUI();

    protected void setVisibleUpdating(boolean visible) {
        try {
            if (visible) {
                mGetWeatherProgress.show();
            } else {
                mGetWeatherProgress.cancel();
            }
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception in setVisibleUpdating method:", e);
        }
    }

    protected void initializeWeatherForecastReceiver(final String actionResult) {
        mWeatherUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                YourLocalWeather.executor.submit(() -> {
                    appendLog(context, TAG, "BroadcastReceiver:", intent);
                    if ((mGetWeatherProgress != null) && (mHandler != null)) {
                        mHandler.post(new Runnable() {
                            public void run() {
                                if (mGetWeatherProgress != null) {
                                    mGetWeatherProgress.dismiss();
                                }
                            }
                        });
                    }
                    switch (intent.getStringExtra(actionResult)) {
                        case UpdateWeatherService.ACTION_WEATHER_UPDATE_OK:
                            updateUI();
                            break;
                        case UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL:
                            Toast.makeText(ForecastingActivity.this,
                                    getString(R.string.toast_parse_error),
                                    Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }
}
