package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WeatherForecastItemViewHolder  extends RecyclerView.ViewHolder {

    private final String TAG = "ForecastViewHolder";

    private DetailedWeatherForecast mWeatherForecast;

    private RecyclerView mRecyclerView;
    private Context mContext;

    private TextView mTime;
    private TextView mIcon;
    private TextView mTemperature;
    private TextView mWind;
    private TextView mHumidity;
    private TextView mPressure;
    private TextView mRainSnow;
    private TextView mDescription;

    public WeatherForecastItemViewHolder(View itemView, Context context) {
        super(itemView);
        mContext = context;

        mTime = (TextView) itemView.findViewById(R.id.forecast_time);
        mIcon = (TextView) itemView.findViewById(R.id.forecast_icon);
        mTemperature = (TextView) itemView.findViewById(R.id.forecast_temperature);
        mWind = (TextView) itemView.findViewById(R.id.forecast_wind);
        mHumidity = (TextView) itemView.findViewById(R.id.forecast_humidity);
        mPressure = (TextView) itemView.findViewById(R.id.forecast_pressure);
        mRainSnow = (TextView) itemView.findViewById(R.id.forecast_rainsnow);
        mDescription = (TextView) itemView.findViewById(R.id.forecast_description);
    }

    void bindWeather(DetailedWeatherForecast weather, Set<Integer> visibleColumns) {
        mWeatherForecast = weather;

        Typeface typeface = Typeface.createFromAsset(mContext.getAssets(),
                "fonts/weathericons-regular-webfont.ttf");
        DetailedWeatherForecast.WeatherCondition weatherCondition = weather.getFirstWeatherCondition();

        if (visibleColumns.contains(1)) {
            mTime.setVisibility(View.VISIBLE);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = new Date(weather.getDateTime() * 1000);
            mTime.setText(timeFormat.format(date));
        } else {
            mTime.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(2)) {
            mIcon.setVisibility(View.VISIBLE);
            mIcon.setTypeface(typeface);
            if (weatherCondition != null) {
                mIcon.setText(Utils.getStrIcon(mContext, weatherCondition.getIcon()));
            }
        } else {
            mIcon.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(3)) {
            mDescription.setVisibility(View.VISIBLE);
            if (weatherCondition != null) {
                mDescription.setText(weather.getFirstWeatherCondition().getDescription());
            }
        } else {
            mDescription.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(4)) {
            mTemperature.setVisibility(View.VISIBLE);
            String temperature = mContext.getString(R.string.temperature_with_degree,
                    AppPreference.getTemperatureWithUnit(mContext, weather.getTemperature()));
            if (weather.getTemperature() > 0) {
                temperature = "+" + temperature;
            }
            mTemperature.setText(temperature);
        } else {
            mTemperature.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(5)) {
            mWind.setVisibility(View.VISIBLE);
            mWind.setText(AppPreference.getWindInString(mContext, weather.getWindSpeed()));
        } else {
            mWind.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(6)) {
            mRainSnow.setVisibility(View.VISIBLE);
            boolean noRain = weather.getRain() < 0.1;
            boolean noSnow = weather.getSnow() < 0.1;
            if (noRain && noSnow) {
                mRainSnow.setText("");
            } else {
                String rain = String.format(Locale.getDefault(), "%.1f", weather.getRain());
                String snow = String.format(Locale.getDefault(), "%.1f", weather.getSnow());
                if (!noRain && !noSnow) {
                    mRainSnow.setText(rain + "/" + snow);
                } else if (noSnow) {
                    mRainSnow.setText(rain);
                } else {
                    mRainSnow.setText(snow);
                }
            }
        } else {
            mRainSnow.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(7)) {
            mHumidity.setVisibility(View.VISIBLE);
            mHumidity.setText(String.format(Locale.getDefault(), "%d", weather.getHumidity()));
        } else {
            mHumidity.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(8)) {
            mPressure.setVisibility(View.VISIBLE);
            mPressure.setText(String.format(Locale.getDefault(), "%.0f", weather.getPressure()));
        } else {
            mPressure.setVisibility(View.GONE);
        }
    }
}