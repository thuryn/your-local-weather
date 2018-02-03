package org.thosp.yourlocalweather.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WeatherForecast;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WindWithUnit;

import java.util.Locale;

public class ForecastBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private DetailedWeatherForecast mWeather;

    public ForecastBottomSheetDialogFragment newInstance(DetailedWeatherForecast weather) {
        ForecastBottomSheetDialogFragment fragment = new ForecastBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("weatherForecast", weather);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWeather = (DetailedWeatherForecast) getArguments().getSerializable("weatherForecast");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_forecast_bottom_sheet, parent, false);

        WindWithUnit windWithUnit = AppPreference.getWindWithUnit(getActivity(), mWeather.getWindSpeed());
        String percentSign = getActivity().getString(R.string.percent_sign);
        String pressureMeasurement = getActivity().getString(R.string.pressure_measurement);
        String mmLabel = getString(R.string.millimetre_label);

        double temperatureMorning = AppPreference.getTemperature(getActivity(), mWeather.getTemperature());
        double temperatureDay = AppPreference.getTemperature(getActivity(), mWeather.getTemperature());
        double temperatureEvening = AppPreference.getTemperature(getActivity(), mWeather.getTemperatureMin());
        double temperatureNight = AppPreference.getTemperature(getActivity(), mWeather.getTemperatureMax());

        String description = mWeather.getFirstWeatherCondition().getDescription();
        String temperatureMorningStr = getActivity().getString(R.string.temperature_with_degree,
                AppPreference.getTemperatureWithUnit(getActivity(), temperatureMorning));
        String temperatureDayStr = getActivity().getString(R.string.temperature_with_degree,
                AppPreference.getTemperatureWithUnit(getActivity(), temperatureDay));
        String temperatureEveningStr = getActivity().getString(R.string.temperature_with_degree,
                AppPreference.getTemperatureWithUnit(getActivity(), temperatureEvening));
        String temperatureNightStr = getActivity().getString(R.string.temperature_with_degree,
                AppPreference.getTemperatureWithUnit(getActivity(), temperatureNight));
        String wind = getActivity().getString(R.string.wind_label,
                windWithUnit.getWindSpeed(1),
                windWithUnit.getWindUnit());
        double windDegree = mWeather.getWindDegree();
        String windDirection = Utils.windDegreeToDirections(getActivity(),
                                                            windDegree);
        String rain = getString(R.string.rain_label, String.valueOf(mWeather.getRain()), mmLabel);
        String snow = getString(R.string.snow_label, String.valueOf(mWeather.getSnow()), mmLabel);
        String pressure = getActivity().getString(R.string.pressure_label, String.valueOf(mWeather.getPressure()), pressureMeasurement);
        String humidity = getActivity().getString(R.string.humidity_label, String.valueOf(mWeather.getHumidity()), percentSign);

        TextView descriptionView = (TextView) v.findViewById(R.id.forecast_description);
        TextView windView = (TextView) v.findViewById(R.id.forecast_wind);
        TextView rainView = (TextView) v.findViewById(R.id.forecast_rain);
        TextView snowView = (TextView) v.findViewById(R.id.forecast_snow);
        TextView humidityView = (TextView) v.findViewById(R.id.forecast_humidity);
        TextView pressureView = (TextView) v.findViewById(R.id.forecast_pressure);

        TextView temperatureMorningView = (TextView) v.findViewById(
                R.id.forecast_morning_temperature);
        TextView temperatureDayView = (TextView) v.findViewById(
                R.id.forecast_day_temperature);
        TextView temperatureEveningView = (TextView) v.findViewById(
                R.id.forecast_evening_temperature);
        TextView temperatureNightView = (TextView) v.findViewById(
                R.id.forecast_night_temperature);
        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(),
                                                     "fonts/weathericons-regular-webfont.ttf");

        descriptionView.setText(description);
        windView.setTypeface(typeface);
        windView.setText(wind + " " + windDirection);
        rainView.setText(rain);
        snowView.setText(snow);
        humidityView.setText(humidity);
        pressureView.setText(pressure);
        if (temperatureMorning > 0) {
            temperatureMorningStr = "+" + temperatureMorningStr;
        }
        if (temperatureDay > 0) {
            temperatureDayStr = "+" + temperatureDayStr;
        }
        if (temperatureEvening > 0) {
            temperatureEveningStr = "+" + temperatureEveningStr;
        }
        if (temperatureNight > 0) {
            temperatureNightStr = "+" + temperatureNightStr;
        }
        temperatureMorningView.setText(temperatureMorningStr);
        temperatureDayView.setText(temperatureDayStr);
        temperatureEveningView.setText(temperatureEveningStr);
        temperatureNightView.setText(temperatureNightStr);

        return v;
    }
}
