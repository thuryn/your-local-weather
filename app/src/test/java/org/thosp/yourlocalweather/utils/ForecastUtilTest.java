package org.thosp.yourlocalweather.utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ForecastUtilTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord;

    @Mock
    CompleteWeatherForecast completeWeatherForecast;

    List<DetailedWeatherForecast> detailedWeatherForecasts;

    @Before
    public void setup() {
        detailedWeatherForecasts = new ArrayList<>();
        when(weatherForecastRecord.getCompleteWeatherForecast()).thenReturn(completeWeatherForecast);
        when(completeWeatherForecast.getWeatherForecastList()).thenReturn(detailedWeatherForecasts);
    }

    @Test
    public void calculateMaxTemperatureWhenAllValuesAreNegative() throws Exception {
        //given
        createDetailedWeatherForecastForDayWithNegativeTemperatures();

        //when
        Set<ForecastUtil.WeatherForecastPerDay> result = ForecastUtil.calculateWeatherForDays(weatherForecastRecord);

        //then
        assertEquals(1, result.size());
        ForecastUtil.WeatherForecastPerDay firstDay = result.iterator().next();
        assertTrue(firstDay.weatherMaxMinForDay.maxTemp > firstDay.weatherMaxMinForDay.minTemp);
        assertTrue(0 > firstDay.weatherMaxMinForDay.minTemp);
        assertTrue(0 > firstDay.weatherMaxMinForDay.maxTemp);
    }

    @Test
    public void createForecastFor5DaysAtTheEndOfYear() throws Exception {
        //given
        createDetailedWeatherForecastForDay();

        //when
        Set<ForecastUtil.WeatherForecastPerDay> result = ForecastUtil.calculateWeatherForDays(weatherForecastRecord);

        //then
        assertEquals(5, result.size());
    }

    private void createDetailedWeatherForecastForDay() {
        Calendar forecastDay = Calendar.getInstance();
        forecastDay.set(Calendar.YEAR, 2019);
        forecastDay.set(Calendar.MONTH, 11);
        forecastDay.set(Calendar.DAY_OF_MONTH, 28);
        forecastDay.set(Calendar.MINUTE, 0);
        forecastDay.set(Calendar.SECOND, 0);
        forecastDay.set(Calendar.MILLISECOND, 0);
        for (int dayCounter = 0; dayCounter < 5; dayCounter++) {
            for (int i = 1; i < 24; i += 3) {
                double temp = -1.0 * i;
                forecastDay.set(Calendar.HOUR_OF_DAY, i);
                detailedWeatherForecasts.add(createDetailedWeatherForecast(forecastDay, temp));
            }
            forecastDay.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void createDetailedWeatherForecastForDayWithNegativeTemperatures() {
        Calendar forecastDay = Calendar.getInstance();
        forecastDay.set(Calendar.MINUTE, 0);
        forecastDay.set(Calendar.SECOND, 0);
        forecastDay.set(Calendar.MILLISECOND, 0);
        for (int i = 1; i < 24; i += 3) {
            double temp = -1.0 * i;
            forecastDay.set(Calendar.HOUR_OF_DAY, i);
            detailedWeatherForecasts.add(createDetailedWeatherForecast(forecastDay, temp));
        }
    }

    private DetailedWeatherForecast createDetailedWeatherForecast(Calendar forecastDay, double temp) {
        DetailedWeatherForecast detailedWeatherForecast = new DetailedWeatherForecast();
        detailedWeatherForecast.setDateTime(forecastDay.getTimeInMillis()/1000);
        detailedWeatherForecast.setTemperature(temp);
        detailedWeatherForecast.addWeatherCondition(800, "01n", "clear sky");

        return detailedWeatherForecast;
    }
}