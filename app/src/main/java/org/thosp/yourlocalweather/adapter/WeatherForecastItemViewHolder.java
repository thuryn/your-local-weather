package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class WeatherForecastItemViewHolder extends RecyclerView.ViewHolder {

    private final Context mContext;

    private final TextView mTime;
    private final TextView mIcon;
    private final TextView mTemperature;
    private final TextView mApparentTemperature;
    private final TextView mWind;
    private final TextView windDirection;
    private final TextView mHumidity;
    private final TextView mPressure;
    private final TextView mRainSnow;
    private final TextView mDescription;

    public WeatherForecastItemViewHolder(View itemView, Context context) {
        super(itemView);
        mContext = context;

        mTime = itemView.findViewById(R.id.forecast_time);
        mIcon = itemView.findViewById(R.id.forecast_icon);
        mTemperature = itemView.findViewById(R.id.forecast_temperature);
        mApparentTemperature = itemView.findViewById(R.id.forecast_apparent_temperature);
        mWind = itemView.findViewById(R.id.forecast_wind);
        windDirection = itemView.findViewById(R.id.forecast_wind_direction);
        mHumidity = itemView.findViewById(R.id.forecast_humidity);
        mPressure = itemView.findViewById(R.id.forecast_pressure);
        mRainSnow = itemView.findViewById(R.id.forecast_rainsnow);
        mDescription = itemView.findViewById(R.id.forecast_description);
    }

    void bindWeather(Context context,
                     double latitude,
                     Locale locale,
                     DetailedWeatherForecast weather,
                     String pressureUnitFromPreferences,
                     String rainSnowUnitFromPreferences,
                     String windUnitFromPreferences,
                     String temperatureUnitFromPreferences,
                     String timeStylePreference,
                     Set<Integer> visibleColumns,
                     boolean isMin,
                     boolean isMax) {

        Typeface typeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/weathericons-regular-webfont.ttf");

        if (visibleColumns.contains(1)) {
            mTime.setVisibility(View.VISIBLE);
            Date date = new Date(weather.getDateTime() * 1000);
            mTime.setText(AppPreference.getLocalizedTime(context, date, timeStylePreference, locale));
            if (AppPreference.is12TimeStyle(context)) {
                ViewGroup.LayoutParams params = mTime.getLayoutParams();
                params.width = Utils.spToPx(85, context);
                mTime.setLayoutParams(params);
            }
        } else {
            mTime.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(2)) {
            mIcon.setVisibility(View.VISIBLE);
            mIcon.setTypeface(typeface);
            mIcon.setText(Utils.getStrIcon(mContext, weather.getWeatherId()));
        } else {
            mIcon.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(3)) {
            mDescription.setVisibility(View.VISIBLE);
            mDescription.setText(Utils.getWeatherDescription(weather.getWeatherId(), context));
        } else {
            mDescription.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(4)) {
            mTemperature.setVisibility(View.VISIBLE);
            String temperature = mContext.getString(R.string.temperature_with_degree, TemperatureUtil.getForecastedTemperatureWithUnit(mContext, weather, temperatureUnitFromPreferences, locale));
            mTemperature.setText(temperature);
            if (isMin == isMax) {
                mTemperature.setTextColor(PreferenceUtil.getTextColor(context));
            } else if (isMax) {
                mTemperature.setTextColor(ContextCompat.getColor(context, R.color.max_temperature_color));
            } else {
                mTemperature.setTextColor(ContextCompat.getColor(context, R.color.min_temperature_color));
            }
        } else {
            mTemperature.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(5)) {
            mApparentTemperature.setVisibility(View.VISIBLE);
            String apparentTemperature = mContext.getString(R.string.temperature_with_degree, TemperatureUtil.getForecastedApparentTemperatureWithUnit(mContext, latitude, weather, temperatureUnitFromPreferences, locale));
            mApparentTemperature.setText(apparentTemperature);
        } else {
            mApparentTemperature.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(6)) {
            mWind.setVisibility(View.VISIBLE);
            mWind.setText(AppPreference.getWindInString(mContext, windUnitFromPreferences, weather.getWindSpeed(), locale));
        } else {
            mWind.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(7)) {
            windDirection.setVisibility(View.VISIBLE);
            windDirection.setText(AppPreference.getWindDirection(mContext, weather.getWindDegree(), locale));
        } else {
            windDirection.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(8)) {
            mRainSnow.setVisibility(View.VISIBLE);
            boolean noRain = weather.getRain() < 0.1;
            boolean noSnow = weather.getSnow() < 0.1;
            if (noRain && noSnow) {
                mRainSnow.setText("");
            } else {
                String rain = AppPreference.getFormatedRainOrSnow(rainSnowUnitFromPreferences, weather.getRain(), locale);
                String snow = AppPreference.getFormatedRainOrSnow(rainSnowUnitFromPreferences, weather.getSnow(), locale);
                if (!noRain && !noSnow) {
                    mRainSnow.setText(rain + "/" + snow);
                } else if (noSnow) {
                    mRainSnow.setText(rain);
                } else {
                    mRainSnow.setText(snow);
                }
            }
            ViewGroup.LayoutParams params = mRainSnow.getLayoutParams();
            params.width = Utils.spToPx(AppPreference.getRainOrSnowForecastWeadherWidth(context), context);
            mRainSnow.setLayoutParams(params);
        } else {
            mRainSnow.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(9)) {
            mHumidity.setVisibility(View.VISIBLE);
            mHumidity.setText(String.format(locale, "%d", weather.getHumidity()));
        } else {
            mHumidity.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(10)) {
            mPressure.setVisibility(View.VISIBLE);
            mPressure.setText(AppPreference.getPressureInString(mContext, weather.getPressure(), pressureUnitFromPreferences, locale));
        } else {
            mPressure.setVisibility(View.GONE);
        }
    }
}