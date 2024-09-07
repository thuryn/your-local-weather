package org.thosp.yourlocalweather.adapter;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastViewHolder> {

    private final String TAG = "WeatherForecastAdapter";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yyyy");
    private final Context mContext;
    private final Set<Integer> visibleColumns;
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
                                  Set<Integer> visibleColumns) {
        mContext = context;
        this.visibleColumns = visibleColumns;
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
            mWeatherList.get(forecastDay).add(forecast);
        }
    }

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
        holder.bindWeather(mContext, latitude, locale, windUnitFromPreferences, weather);
    }

    @Override
    public int getItemCount() {
        return (mWeatherList != null ? mWeatherList.keySet().size() : 0);
    }
}

