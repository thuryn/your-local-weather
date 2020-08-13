package org.thosp.yourlocalweather.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WeatherForecastItemAdapter extends RecyclerView.Adapter<WeatherForecastItemViewHolder> {

    private Context mContext;
    private Set<Integer> visibleColumns;
    private List<DetailedWeatherForecast> mWeatherList;
    private double latitude;
    private Locale locale;
    double minTemp, maxTemp;

    public WeatherForecastItemAdapter(Context context,
                                      List<DetailedWeatherForecast> weather,
                                      double latitude,
                                      Locale locale,
                                      Set<Integer> visibleColumns) {
        mContext = context;
        mWeatherList = weather;
        this.visibleColumns = visibleColumns;
        this.latitude = latitude;
        this.locale = locale;

        minTemp = Integer.MAX_VALUE;
        maxTemp = Integer.MIN_VALUE;
        for(DetailedWeatherForecast item : weather){
            double temp = item.getTemperature();
            if(temp > maxTemp) maxTemp = temp;
            if(temp < minTemp) minTemp = temp;
        }
    }

    @Override
    public WeatherForecastItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.forecast_item_detail, parent, false);
        return new WeatherForecastItemViewHolder(v, mContext);
    }

    @Override
    public void onBindViewHolder(WeatherForecastItemViewHolder holder, int position) {
        DetailedWeatherForecast weather = mWeatherList.get(position);
        double temp = weather.getTemperature();
        boolean isMin = temp == minTemp;
        boolean isMax = temp == maxTemp;
        holder.bindWeather(mContext, latitude, locale, weather, visibleColumns, isMin, isMax);
    }

    @Override
    public int getItemCount() {
        return (mWeatherList != null ? mWeatherList.size() : 0);
    }
}
