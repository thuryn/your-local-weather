package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WeatherCondition;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class LongWeatherForecastItemViewHolder extends RecyclerView.ViewHolder {

    private final String TAG = "LongForecastViewHolder";

    private DetailedWeatherForecast mWeatherForecast;

    private Context mContext;

    private TextView mTime;
    private TextView mDate;
    private TextView mIcon;
    private TextView mTemperature;
    private TextView mApparentTemperature;
    private TextView mWind;
    private TextView windDirection;
    private TextView mHumidity;
    private TextView mPressure;
    private TextView mRainSnow;
    private TextView mDescription;

    public LongWeatherForecastItemViewHolder(View itemView, Context context) {
        super(itemView);
        mContext = context;

        mTime = itemView.findViewById(R.id.forecast_time);
        mDate = itemView.findViewById(R.id.forecast_date);
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
                     Set<Integer> visibleColumns) {
        mWeatherForecast = weather;

        Typeface typeface = Typeface.createFromAsset(mContext.getAssets(),
                "fonts/weathericons-regular-webfont.ttf");
        WeatherCondition weatherCondition = weather.getFirstWeatherCondition();

        if (visibleColumns.contains(1)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d.M", locale);
            Date date = new Date(weather.getDateTime() * 1000);
            Calendar currentRowDate = Calendar.getInstance();
            currentRowDate.setTime(date);
            mDate.setVisibility(View.VISIBLE);
            mDate.setTypeface(typeface);
            mDate.setText(dateFormat.format(date));
        } else {
            mDate.setVisibility(View.GONE);
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
                    TemperatureUtil.getForecastedTemperatureWithUnit(mContext, weather, locale));
            mTemperature.setText(temperature);
        } else {
            mTemperature.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(5)) {
            mApparentTemperature.setVisibility(View.VISIBLE);
            String apparentTemperature = mContext.getString(R.string.temperature_with_degree,
                    TemperatureUtil.getForecastedApparentTemperatureWithUnit(mContext, latitude, weather, locale));
            mApparentTemperature.setText(apparentTemperature);
        } else {
            mApparentTemperature.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(6)) {
            mWind.setVisibility(View.VISIBLE);
            mWind.setText(AppPreference.getWindInString(mContext, weather.getWindSpeed(), locale));
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
                String rain = AppPreference.getFormatedRainOrSnow(context, weather.getRain(), locale);
                String snow = AppPreference.getFormatedRainOrSnow(context, weather.getSnow(), locale);
                if (!noRain && !noSnow) {
                    mRainSnow.setText(rain + "/" + snow);
                } else if (noSnow) {
                    mRainSnow.setText(rain);
                } else {
                    mRainSnow.setText(snow);
                }
            }
            ViewGroup.LayoutParams params=mRainSnow.getLayoutParams();
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
            mPressure.setText(AppPreference.getPressureInString(mContext, weather.getPressure(), locale));
        } else {
            mPressure.setVisibility(View.GONE);
        }
    }
}