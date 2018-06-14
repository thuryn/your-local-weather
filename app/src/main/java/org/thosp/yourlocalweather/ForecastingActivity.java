package org.thosp.yourlocalweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WeatherForecastResultHandler;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.WeatherForecastUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public abstract class ForecastingActivity extends BaseActivity {

    private final String TAG = "ForecastingActivity";

    protected Map<Long, List<DetailedWeatherForecast>> weatherForecastList = new HashMap<>();
    protected Map<Long, Long> locationWeatherForecastLastUpdate = new HashMap<>();
    protected ConnectionDetector mConnectionDetector;
    private ProgressDialog mGetWeatherProgress;
    private Handler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((YourLocalWeather) getApplication()).applyTheme(this);
        mConnectionDetector = new ConnectionDetector(this);
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
                    case Constants.PARSE_RESULT_SUCCESS:
                        setVisibleUpdating(false);
                        updateUI();
                        break;
                }
            }
        };

    }

    protected abstract void updateUI();

    protected void setVisibleUpdating(boolean visible) {
        if (visible) {
            mGetWeatherProgress.show();
        } else {
            mGetWeatherProgress.cancel();
        }
    }

    protected void updateWeatherForecastFromNetwork() {
        if (mConnectionDetector.isNetworkAvailableAndConnected()) {
            WeatherForecastUtil.getWeather(this, new ForecastGraphsWeatherForecastResultHandler(this));
            setVisibleUpdating(true);
        }
    }

    public class ForecastGraphsWeatherForecastResultHandler implements WeatherForecastResultHandler {

        private Context context;

        public ForecastGraphsWeatherForecastResultHandler(Context context) {
            this.context = context;
        }

        public void processResources(CompleteWeatherForecast completeWeatherForecast, long lastUpdate) {
            long locationId = AppPreference.getCurrentLocationId(context);
            weatherForecastList.put(locationId, completeWeatherForecast.getWeatherForecastList());
            locationWeatherForecastLastUpdate.put(locationId, lastUpdate);
            mHandler.sendEmptyMessage(Constants.PARSE_RESULT_SUCCESS);
        }

        public void processError(Exception e) {
            mHandler.sendEmptyMessage(Constants.TASK_RESULT_ERROR);
            appendLog(getBaseContext(), TAG, "JSONException:", e);
        }
    }
}
