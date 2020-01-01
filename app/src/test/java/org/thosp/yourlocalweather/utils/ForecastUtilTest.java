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
    public void addition_isCorrect() throws Exception {
        //when
        createDetailedWeatherForecastForDayWithNegativeTemperatures();

        Set<ForecastUtil.WeatherForecastPerDay> result = ForecastUtil.calculateWeatherForDays(weatherForecastRecord);
        assertEquals(1, result.size());
        ForecastUtil.WeatherForecastPerDay firstDay = result.iterator().next();
        assertTrue(firstDay.weatherMaxMinForDay.maxTemp > firstDay.weatherMaxMinForDay.minTemp);
        assertTrue(0 > firstDay.weatherMaxMinForDay.minTemp);
        assertTrue(0 > firstDay.weatherMaxMinForDay.maxTemp);
    }

    private void createDetailedWeatherForecastForDayWithNegativeTemperatures() {
        for (int i = 1; i < 24; i += 3) {
            double temp = -1.0 * i;
            detailedWeatherForecasts.add(createDetailedWeatherForecast(i, temp));
        }
    }

    private DetailedWeatherForecast createDetailedWeatherForecast(int hour, double temp) {
        Calendar forecastDay = Calendar.getInstance();
        forecastDay.set(Calendar.HOUR_OF_DAY, hour);
        forecastDay.set(Calendar.MINUTE, 0);
        forecastDay.set(Calendar.SECOND, 0);
        forecastDay.set(Calendar.MILLISECOND, 0);
        DetailedWeatherForecast detailedWeatherForecast = new DetailedWeatherForecast();
        detailedWeatherForecast.setDateTime(forecastDay.getTimeInMillis()/1000);
        detailedWeatherForecast.setTemperature(temp);
        detailedWeatherForecast.addWeatherCondition(800, "01n", "clear sky");

        return detailedWeatherForecast;
    }
}