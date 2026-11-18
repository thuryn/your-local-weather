package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.utils.ForecastUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastViewHolder> {

    private final Context mContext;
    private final Set<Integer> visibleColumns;

    private final boolean showMinMaxOnly;
    private final Set<Integer> expandedDays = new HashSet<>();
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
        Calendar forecastCalendar = Calendar.getInstance();
        for (DetailedWeatherForecast forecast: weatherForecastList) {
            if (forecast == null) {
                continue;
            }
            long forecastDateTimeInMs = forecast.getDateTime() * 1000;
            forecastCalendar.setTimeInMillis(forecastDateTimeInMs);
            int forecastDay = forecastCalendar.get(Calendar.DAY_OF_YEAR) + (forecastCalendar.get(Calendar.YEAR) * 1000);
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

    public int getTodayPosition() {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        Calendar todayCalendar = Calendar.getInstance();
        int todayKey = todayCalendar.get(Calendar.DAY_OF_YEAR) + (todayCalendar.get(Calendar.YEAR) * 1000);
        int exactIndex = keys.indexOf(todayKey);
        if (exactIndex >= 0) {
            return exactIndex;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i) >= todayKey) {
                return i;
            }
        }
        return keys.size() - 1;
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
        int dayKey = keys.get(position);
        List<DetailedWeatherForecast> weather = mWeatherList.get(dayKey);

        if (showMinMaxOnly) {
            ForecastUtil.WeatherMaxMinForDay maxMin = ForecastUtil.calculateWeatherMaxMinForDay(weather);
            ForecastUtil.WeatherIdsForDay weatherIds = maxMin != null
                    ? ForecastUtil.getWeatherIdForDay(mContext, weather, maxMin)
                    : null;
            boolean isExpanded = expandedDays.contains(dayKey);
            holder.bindSummaryAndWeather(
                    mContext,
                    latitude,
                    locale,
                    windUnitFromPreferences,
                    weather,
                    maxMin,
                    weatherIds,
                    isExpanded,
                    v -> {
                        int adapterPos = holder.getBindingAdapterPosition();
                        if (adapterPos == RecyclerView.NO_POSITION) {
                            return;
                        }
                        if (expandedDays.contains(dayKey)) {
                            expandedDays.remove(dayKey);
                        } else {
                            expandedDays.add(dayKey);
                        }
                        notifyItemChanged(adapterPos);
                    });
        } else {
            holder.bindWeather(mContext, latitude, locale, windUnitFromPreferences, weather);
        }
    }

    @Override
    public int getItemCount() {
        return (mWeatherList != null ? mWeatherList.keySet().size() : 0);
    }
}

