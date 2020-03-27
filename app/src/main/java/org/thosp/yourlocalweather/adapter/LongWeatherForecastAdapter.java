package org.thosp.yourlocalweather.adapter;

import android.content.Context;
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

public class LongWeatherForecastAdapter extends RecyclerView.Adapter<LongWeatherForecastViewHolder> {

    private final Context mContext;
    private final Set<Integer> visibleColumns;
    private final List<DetailedWeatherForecast> mWeatherList;
    private double latitude;
    private Locale locale;

    public LongWeatherForecastAdapter(Context context,
                                      List<DetailedWeatherForecast> weatherForecastList,
                                      double latitude,
                                      Locale locale,
                                      Set<Integer> visibleColumns) {
        this.mContext = context;
        this.visibleColumns = visibleColumns;
        this.latitude = latitude;
        this.locale = locale;
        this.mWeatherList = weatherForecastList;
    }

    @Override
    public LongWeatherForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.long_forecast_item, parent, false);
        return new LongWeatherForecastViewHolder(v, mContext, visibleColumns);
    }

    @Override
    public void onBindViewHolder(LongWeatherForecastViewHolder holder, int position) {
        holder.bindWeather(mContext, latitude, locale, mWeatherList);
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}

