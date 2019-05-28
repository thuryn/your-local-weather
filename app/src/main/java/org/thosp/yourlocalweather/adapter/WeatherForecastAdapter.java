package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    private final Map<Integer, List<DetailedWeatherForecast>> mWeatherList;
    double latitude;
    Locale locale;
    private final List<Integer> keys;

    public WeatherForecastAdapter(Context context,
                                  List<DetailedWeatherForecast> weatherForecastList,
                                  double latitude,
                                  Locale locale,
                                  Set<Integer> visibleColumns) {
        mContext = context;
        this.visibleColumns = visibleColumns;
        this.latitude = latitude;
        this.locale = locale;

        mWeatherList = new HashMap<>();
        keys = new ArrayList<>();
        Calendar forecastCalendar = Calendar.getInstance();
        for (DetailedWeatherForecast forecast: weatherForecastList) {
            forecastCalendar.setTimeInMillis(forecast.getDateTime() * 1000);
            int forecastDay = forecastCalendar.get(Calendar.DAY_OF_YEAR);
            if (!mWeatherList.keySet().contains(forecastDay)) {
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
        return new WeatherForecastViewHolder(v, mContext, visibleColumns);
    }

    @Override
    public void onBindViewHolder(WeatherForecastViewHolder holder, int position) {
        List<DetailedWeatherForecast> weather = mWeatherList.get(keys.get(position));
        holder.bindWeather(mContext, latitude, locale, weather);
    }

    @Override
    public int getItemCount() {
        return (mWeatherList != null ? mWeatherList.keySet().size() : 0);
    }
}

