package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WeatherForecastViewHolder extends RecyclerView.ViewHolder {

    private final String TAG = "ForecastViewHolder";

    private final RecyclerView mRecyclerView;
    private final Context mContext;
    private final Set<Integer> visibleColumns;
    private final String pressureUnitFromPreferences;
    private final String rainSnowUnitFromPreferences;
    private final String windUnitFromPreferences;
    private final String temperatureUnitFromPreferences;
    private final String timeStylePreference;

    private final TextView mDate;
    private final TextView headerTime;
    private final TextView headerIcon;
    private final TextView headerDescription;
    private final TextView headerTemperature;
    private final TextView headerApparentTemperature;
    private final TextView headerWind;
    private final TextView headerWindDirection;
    private final TextView headerRainSnow;
    private final TextView headerHumidity;
    private final TextView headerPressure;

    private final TextView headerTimeUnit;
    private final TextView headerIconUnit;
    private final TextView headerDescriptionUnit;
    private final TextView headerTemperatureUnit;
    private final TextView headerApparentTemperatureUnit;
    private final TextView headerWindUnit;
    private final TextView headerWindDirectionUnit;
    private final TextView headerRainSnowUnit;
    private final TextView headerHumidityUnit;
    private final TextView headerPressureUnit;

    public WeatherForecastViewHolder(View itemView,
                                     Context context,
                                     String pressureUnitFromPreferences,
                                     String rainSnowUnitFromPreferences,
                                     String windUnitFromPreferences,
                                     String temperatureUnitFromPreferences,
                                     String timeStylePreference,
                                     Set<Integer> visibleColumns) {
        super(itemView);
        mContext = context;
        this.visibleColumns = visibleColumns;
        this.pressureUnitFromPreferences = pressureUnitFromPreferences;
        this.rainSnowUnitFromPreferences = rainSnowUnitFromPreferences;
        this.windUnitFromPreferences = windUnitFromPreferences;
        this.temperatureUnitFromPreferences = temperatureUnitFromPreferences;
        this.timeStylePreference = timeStylePreference;

        mDate = itemView.findViewById(R.id.forecast_date);
        headerTime = itemView.findViewById(R.id.forecast_header_time);
        headerIcon = itemView.findViewById(R.id.forecast_header_icon);
        headerDescription = itemView.findViewById(R.id.forecast_header_description);
        headerTemperature = itemView.findViewById(R.id.forecast_header_temperature);
        headerApparentTemperature = itemView.findViewById(R.id.forecast_header_apparent_temperature);
        headerWind = itemView.findViewById(R.id.forecast_header_wind);
        headerWindDirection = itemView.findViewById(R.id.forecast_header_wind_direction);
        headerRainSnow = itemView.findViewById(R.id.forecast_header_rainsnow);
        headerHumidity = itemView.findViewById(R.id.forecast_header_humidity);
        headerPressure = itemView.findViewById(R.id.forecast_header_presure);

        headerTimeUnit = itemView.findViewById(R.id.forecast_header_time_unit);
        headerIconUnit = itemView.findViewById(R.id.forecast_header_icon_unit);
        headerDescriptionUnit = itemView.findViewById(R.id.forecast_header_description_unit);
        headerTemperatureUnit = itemView.findViewById(R.id.forecast_header_temperature_unit);
        headerApparentTemperatureUnit = itemView.findViewById(R.id.forecast_header_apparent_temperature_unit);
        headerWindUnit = itemView.findViewById(R.id.forecast_header_wind_unit);
        headerWindDirectionUnit = itemView.findViewById(R.id.forecast_header_wind_direction_unit);
        headerRainSnowUnit = itemView.findViewById(R.id.forecast_header_rainsnow_unit);
        headerHumidityUnit = itemView.findViewById(R.id.forecast_header_humidity_unit);
        headerPressureUnit = itemView.findViewById(R.id.forecast_header_presure_unit);

        mRecyclerView = itemView.findViewById(R.id.forecast_recycler_view_item);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
    }

    void bindWeather(Context context, double latitude, Locale locale, String windUnitFromPreferences, List<DetailedWeatherForecast> weather) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMMM", locale);
        Date date = new Date(weather.get(0).getDateTime() * 1000);
        Calendar currentRowDate = Calendar.getInstance();
        currentRowDate.setTime(date);
        mDate.setText(dateFormat.format(date));

        Typeface typeface = Typeface.createFromAsset(mContext.getAssets(),
                "fonts/weathericons-regular-webfont.ttf");

        if (visibleColumns.contains(1)) {
            headerTemperature.setVisibility(View.VISIBLE);
            headerTime.setTypeface(typeface);
            headerTime.setText(String.valueOf((char) 0xf08b));
            headerTimeUnit.setVisibility(View.VISIBLE);
            if (AppPreference.is12TimeStyle(context)) {
                ViewGroup.LayoutParams params=headerTime.getLayoutParams();
                params.width= Utils.spToPx(85, context);
                headerTime.setLayoutParams(params);
                params=headerTimeUnit.getLayoutParams();
                params.width= Utils.spToPx(85, context);
                headerTimeUnit.setLayoutParams(params);
            }
        } else {
            headerTime.setVisibility(View.GONE);
            headerTimeUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(2)) {
            headerIcon.setVisibility(View.VISIBLE);
            headerIconUnit.setVisibility(View.VISIBLE);
        } else {
            headerIcon.setVisibility(View.GONE);
            headerIconUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(3)) {
            headerDescription.setVisibility(View.VISIBLE);
            headerDescriptionUnit.setVisibility(View.VISIBLE);
        } else {
            headerDescription.setVisibility(View.GONE);
            headerDescriptionUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(4)) {
            headerTemperature.setVisibility(View.VISIBLE);
            headerTemperature.setTypeface(typeface);
            headerTemperature.setText(String.valueOf((char) 0xf055));
            headerTemperatureUnit.setVisibility(View.VISIBLE);
        } else {
            headerTemperature.setVisibility(View.GONE);
            headerTemperatureUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(5)) {
            headerApparentTemperature.setVisibility(View.VISIBLE);
            headerApparentTemperature.setTypeface(typeface);
            headerApparentTemperature.setText(String.valueOf((char) 0xf055));
            headerApparentTemperatureUnit.setVisibility(View.VISIBLE);
            headerApparentTemperatureUnit.setText("~");
        } else {
            headerApparentTemperature.setVisibility(View.GONE);
            headerApparentTemperatureUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(6)) {
            headerWind.setVisibility(View.VISIBLE);
            headerWind.setTypeface(typeface);
            headerWind.setText(String.valueOf((char) 0xf050));
            headerWindUnit.setVisibility(View.VISIBLE);
            headerWindUnit.setText(AppPreference.getWindUnit(mContext, windUnitFromPreferences));
        } else {
            headerWind.setVisibility(View.GONE);
            headerWindUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(7)) {
            headerWindDirection.setVisibility(View.VISIBLE);
            headerWindDirection.setTypeface(typeface);
            headerWindDirection.setText(String.valueOf((char) 0xf050));
            headerWindDirectionUnit.setVisibility(View.VISIBLE);
            headerWindDirectionUnit.setText(mContext.getString(R.string.forecast_column_wind_direction_unit));
        } else {
            headerWindDirection.setVisibility(View.GONE);
            headerWindDirectionUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(8)) {
            headerRainSnow.setVisibility(View.VISIBLE);
            headerRainSnow.setTypeface(typeface);
            headerRainSnow.setText((char) 0xf01a + "/" + (char) 0xf01b);
            headerRainSnowUnit.setVisibility(View.VISIBLE);
            headerRainSnowUnit.setText(AppPreference.getRainOrSnowUnit(rainSnowUnitFromPreferences));
            ViewGroup.LayoutParams params=headerRainSnow.getLayoutParams();
            params.width = Utils.spToPx(AppPreference.getRainOrSnowForecastWeadherWidth(context), context);
            headerRainSnow.setLayoutParams(params);
            params=headerRainSnowUnit.getLayoutParams();
            params.width = Utils.spToPx(AppPreference.getRainOrSnowForecastWeadherWidth(context), context);
            headerRainSnowUnit.setLayoutParams(params);
        } else {
            headerRainSnow.setVisibility(View.GONE);
            headerRainSnowUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(9)) {
            headerHumidity.setVisibility(View.VISIBLE);
            headerHumidity.setTypeface(typeface);
            headerHumidity.setText(String.valueOf((char) 0xf07a));
            headerHumidityUnit.setVisibility(View.VISIBLE);
            headerHumidityUnit.setText(R.string.percent_sign);
        } else {
            headerHumidity.setVisibility(View.GONE);
            headerHumidityUnit.setVisibility(View.GONE);
        }
        if (visibleColumns.contains(10)) {
            headerPressure.setVisibility(View.VISIBLE);
            headerPressure.setTypeface(typeface);
            headerPressure.setText(String.valueOf((char) 0xf079));
            headerPressureUnit.setVisibility(View.VISIBLE);
            headerPressureUnit.setText(AppPreference.getPressureUnit(mContext, pressureUnitFromPreferences));
        } else {
            headerPressure.setVisibility(View.GONE);
            headerPressureUnit.setVisibility(View.GONE);
        }
        updateUI(latitude, locale, weather);
    }

    private void updateUI(double latitude, Locale locale, List<DetailedWeatherForecast> detailedWeatherForecast) {
        WeatherForecastItemAdapter adapter = new WeatherForecastItemAdapter(
                mContext,
                detailedWeatherForecast,
                latitude,
                locale,
                pressureUnitFromPreferences,
                rainSnowUnitFromPreferences,
                windUnitFromPreferences,
                temperatureUnitFromPreferences,
                timeStylePreference,
                visibleColumns);
        mRecyclerView.setAdapter(adapter);
    }
}