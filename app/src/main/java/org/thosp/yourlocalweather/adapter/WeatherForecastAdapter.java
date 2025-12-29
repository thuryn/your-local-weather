package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastViewHolder> {

    private final Context mContext;
    private final Set<Integer> visibleColumns;

    private final boolean showMinMaxOnly;
    private final Map<Integer, List<DetailedWeatherForecast>> mWeatherList;
    double latitude;
    Locale locale;
    private final List<Integer> keys;
    private final String pressureUnitFromPreferences;
    private final String rainSnowUnitFromPreferences;
    private final String windUnitFromPreferences;
    private final String temperatureUnitFromPreferences;
    private final String timeStylePreference;

    public WeatherForecastAdapter(Context context,
                                  List<DetailedWeatherForecast> weatherForecastList,
                                  double latitude,
                                  Locale locale,
                                  String pressureUnitFromPreferences,
                                  String rainSnowUnitFromPreferences,
                                  String windUnitFromPreferences,
                                  String temperatureUnitFromPreferences,
                                  String timeStylePreference,
                                  Set<Integer> visibleColumns,
                                  boolean showMinMaxOnly) {
        mContext = context;
        this.visibleColumns = visibleColumns;
        this.showMinMaxOnly = showMinMaxOnly;
        this.latitude = latitude;
        this.locale = locale;
        this.pressureUnitFromPreferences = pressureUnitFromPreferences;
        this.rainSnowUnitFromPreferences = rainSnowUnitFromPreferences;
        this.windUnitFromPreferences = windUnitFromPreferences;
        this.temperatureUnitFromPreferences = temperatureUnitFromPreferences;
        this.timeStylePreference = timeStylePreference;

        mWeatherList = new HashMap<>();
        keys = new ArrayList<>();
        long now = System.currentTimeMillis();
        Calendar forecastCalendar = Calendar.getInstance();
        for (DetailedWeatherForecast forecast: weatherForecastList) {
            if (forecast == null) {
                continue;
            }
            long forecastDateTimeInMs = forecast.getDateTime() * 1000;
            if (forecastDateTimeInMs < now) {
                continue;
            }
            forecastCalendar.setTimeInMillis(forecastDateTimeInMs);
            int forecastDay = forecastCalendar.get(Calendar.DAY_OF_YEAR);
            if (!mWeatherList.containsKey(forecastDay)) {
                List<DetailedWeatherForecast> dayForecastList = new ArrayList<>();
                mWeatherList.put(forecastDay, dayForecastList);
                keys.add(forecastDay);
            }
            List<DetailedWeatherForecast> listForDay = mWeatherList.get(forecastDay);
            if (listForDay != null) {
                listForDay.add(forecast);
            }
        }
    }

    @NonNull
    @Override
    public WeatherForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.forecast_item, parent, false);
        return new WeatherForecastViewHolder(v,
                mContext,
                pressureUnitFromPreferences,
                rainSnowUnitFromPreferences,
                windUnitFromPreferences,
                temperatureUnitFromPreferences,
                timeStylePreference,
                visibleColumns);
    }

    @Override
    public void onBindViewHolder(WeatherForecastViewHolder holder, int position) {
        List<DetailedWeatherForecast> weather = mWeatherList.get(keys.get(position));

        if (showMinMaxOnly) {
            double minTemp = Integer.MAX_VALUE;
            int minTempIndex = 0;
            double maxTemp = Integer.MIN_VALUE;
            int maxTempIndex = 0;
            for(DetailedWeatherForecast item : weather){
                double temp = item.getTemperature();
                if(temp > maxTemp) {
                    maxTempIndex = weather.indexOf(item);
                    maxTemp = temp;
                }
                if(temp < minTemp) {
                    minTempIndex = weather.indexOf(item);
                    minTemp = temp;
                }
            }
            List<DetailedWeatherForecast> minMaxWeather =
                    (maxTempIndex > minTempIndex) ?
                            List.of(weather.get(minTempIndex), weather.get(maxTempIndex)) :
                            List.of(weather.get(maxTempIndex), weather.get(minTempIndex));
            holder.bindWeather(mContext, latitude, locale, windUnitFromPreferences, minMaxWeather);
        } else {
            holder.bindWeather(mContext, latitude, locale, windUnitFromPreferences, weather);
        }
    }

    @Override
    public int getItemCount() {
        return (mWeatherList != null ? mWeatherList.keySet().size() : 0);
    }
}

