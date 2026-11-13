package org.thosp.yourlocalweather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import org.thosp.yourlocalweather.events.AppEvent;
import org.thosp.yourlocalweather.events.AppEventListener;
import org.thosp.yourlocalweather.events.AppEventsBus;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
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
    protected AppEventListener appEventListener;
    protected String pressureUnitFromPreferences;
    protected String rainSnowUnitFromPreferences;
    protected String temperatureUnitFromPreferences;
    protected AppCompatImageButton switchLocationButton;

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
        if (appEventListener != null) {
            AppEventsBus.unregisterListener(appEventListener);
        }
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

    protected void initializeWeatherForecastReceiver(final String targetAction) {
        appEventListener = event -> {
            boolean isMatch = false;
            String result = null;
            if (event instanceof AppEvent.ForecastWeatherUpdate && UpdateWeatherService.ACTION_FORECAST_UPDATE_RESULT.equals(targetAction)) {
                isMatch = true;
                result = ((AppEvent.ForecastWeatherUpdate) event).getResult();
            } else if (event instanceof AppEvent.GraphsWeatherUpdate && UpdateWeatherService.ACTION_GRAPHS_UPDATE_RESULT.equals(targetAction)) {
                isMatch = true;
                result = ((AppEvent.GraphsWeatherUpdate) event).getResult();
            }
            if (!isMatch || result == null) {
                return;
            }
            final String finalResult = result;
            YourLocalWeather.executor.submit(() -> {
                appendLog(ForecastingActivity.this, TAG, "AppEvent received: ", finalResult);
                if ((mGetWeatherProgress != null) && (mHandler != null)) {
                    mHandler.post(() -> {
                        if (mGetWeatherProgress != null) {
                            mGetWeatherProgress.dismiss();
                        }
                    });
                }
                switch (finalResult) {
                    case UpdateWeatherService.ACTION_WEATHER_UPDATE_OK:
                        updateUI();
                        break;
                    case UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL:
                        Toast.makeText(ForecastingActivity.this,
                                getString(R.string.toast_parse_error),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            });
        };
    }
}
