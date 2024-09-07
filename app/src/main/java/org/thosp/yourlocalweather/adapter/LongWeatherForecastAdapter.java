package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LongWeatherForecastAdapter extends RecyclerView.Adapter<LongWeatherForecastViewHolder> {

    private final Context mContext;
    private final Set<Integer> visibleColumns;
    private final List<DetailedWeatherForecast> mWeatherList;
    private final double latitude;
    private final Locale locale;
    private final String pressureUnitFromPreferences;
    private final String rainSnowUnitFromPreferences;
    private final String windUnitFromPreferences;
    private final String temperatureUnitFromPreferences;

    public LongWeatherForecastAdapter(Context context,
                                      List<DetailedWeatherForecast> weatherForecastList,
                                      double latitude,
                                      Locale locale,
                                      String pressureUnitFromPreferences,
                                      String rainSnowUnitFromPreferences,
                                      String windUnitFromPreferences,
                                      String temperatureUnitFromPreferences,
                                      Set<Integer> visibleColumns) {
        this.mContext = context;
        this.visibleColumns = visibleColumns;
        this.latitude = latitude;
        this.locale = locale;
        this.mWeatherList = weatherForecastList;
        this.pressureUnitFromPreferences = pressureUnitFromPreferences;
        this.rainSnowUnitFromPreferences = rainSnowUnitFromPreferences;
        this.windUnitFromPreferences = windUnitFromPreferences;
        this.temperatureUnitFromPreferences = temperatureUnitFromPreferences;
    }

    @Override
    public LongWeatherForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.long_forecast_item, parent, false);
        return new LongWeatherForecastViewHolder(v, mContext, pressureUnitFromPreferences, rainSnowUnitFromPreferences, windUnitFromPreferences, temperatureUnitFromPreferences, visibleColumns);
    }

    @Override
    public void onBindViewHolder(LongWeatherForecastViewHolder holder, int position) {
        holder.bindWeather(mContext, latitude, locale, windUnitFromPreferences, mWeatherList);
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}

