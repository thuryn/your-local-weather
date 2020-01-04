package org.thosp.yourlocalweather.utils;

import android.content.Context;

import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WeatherCondition;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLogLastUpdateTime;

public class ForecastUtil {

    private static final String TAG = "ForecastUtil";

    public static long AUTO_FORECAST_UPDATE_TIME_MILIS = 3600000; // 1h

    public static boolean shouldUpdateForecast(Context context, long locationId) {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord =
                weatherForecastDbHelper.getWeatherForecast(locationId);
        long now = Calendar.getInstance().getTimeInMillis();
        if (weatherForecastRecord == null) {
            return true;
        }
        long firstForecastTime = 1000 * weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList().get(0).getDateTime();
        if ((firstForecastTime < now) || (weatherForecastRecord.getLastUpdatedTime() +
                        AUTO_FORECAST_UPDATE_TIME_MILIS) <  now) {
            return true;
        }
        appendLogLastUpdateTime(context,
                TAG,
                "weatherForecastRecord.getLastUpdatedTime():",
                weatherForecastRecord,
                ", now:",
                now);
        return false;
    }

    public static Set<WeatherForecastPerDay> calculateWeatherForDays(WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord) {
        Set<WeatherForecastPerDay> result = new HashSet<>();
        Calendar forecastCalendar = Calendar.getInstance();
        int initialYearForTheList = forecastCalendar.get(Calendar.YEAR);
        Map<Integer, List<DetailedWeatherForecast>> weatherList = createWeatherList(weatherForecastRecord);
        Integer firstDayOfYear = Collections.min(weatherList.keySet());
        int dayCounter = 0;
        int daysInList = firstDayOfYear + weatherList.keySet().size();
        for (int dayInYear = firstDayOfYear; dayInYear < daysInList; dayInYear++) {
            int dayInYearForList;
            int yearForList;
            if (dayInYear > 365) {
                dayInYearForList = dayInYear - 365;
                yearForList = initialYearForTheList + 1;
            } else {
                dayInYearForList = dayInYear;
                yearForList = initialYearForTheList;
            }
            if ((weatherList.get(dayInYear) == null) || (weatherList.get(dayInYear).size() < 3)) {
                continue;
            }
            dayCounter++;
            WeatherMaxMinForDay weatherMaxMinForDay = calculateWeatherMaxMinForDay(weatherList.get(dayInYear));
            if (weatherMaxMinForDay == null) {
                continue;
            }
            WeatherIdsForDay weatherIdsForTheDay = getWeatherIdForDay(weatherList.get(dayInYear), weatherMaxMinForDay);
            result.add(new WeatherForecastPerDay(dayCounter, weatherIdsForTheDay, weatherMaxMinForDay, getWeatherIconId(weatherIdsForTheDay.mainWeatherId, weatherList.get(dayInYear)), dayInYearForList, yearForList));
        }
        return result;
    }

    public static Map<Integer, List<DetailedWeatherForecast>> createWeatherList(WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord) {
        Map<Integer, List<DetailedWeatherForecast>> weatherList = new HashMap<>();
        Calendar forecastCalendar = Calendar.getInstance();
        int maxForecastDay = 0;
        for (DetailedWeatherForecast detailedWeatherForecast : weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList()) {
            forecastCalendar.setTimeInMillis(detailedWeatherForecast.getDateTime() * 1000);
            int forecastDay = forecastCalendar.get(Calendar.DAY_OF_YEAR);
            if (maxForecastDay > forecastDay) {
                forecastDay += 365;
            }
            maxForecastDay = forecastDay;
            if (!weatherList.keySet().contains(forecastDay)) {
                List<DetailedWeatherForecast> dayForecastList = new ArrayList<>();
                weatherList.put(forecastDay, dayForecastList);
            }
            //appendLog(context, TAG, "preLoadWeather:forecastDay=" + forecastDay + ":detailedWeatherForecast=" + detailedWeatherForecast);
            weatherList.get(forecastDay).add(detailedWeatherForecast);
        }
        return weatherList;
    }

    public static WeatherIdsForDay getWeatherIdForDay(List<DetailedWeatherForecast> weatherListForDay,
                                                      WeatherMaxMinForDay weatherMaxMinForDay) {
        Map<Integer, String> weatherDescriptionsInDay = new HashMap<>();
        Map<Integer, Integer> weatherIdsInDay = new HashMap<>();
        for (DetailedWeatherForecast weatherForecastForDay : weatherListForDay) {
            WeatherCondition weatherCondition = weatherForecastForDay.getFirstWeatherCondition();
                /*appendLog(context, TAG, "preLoadWeather:dayInYear=" + dayInYearForList + ":dayCounter=" + dayCounter +
                        ":weatherCondition.getWeatherId()=" + weatherCondition.getWeatherId() +
                        ":weatherIdsInDay.get(weatherCondition.getWeatherId())=" + weatherIdsInDay.get(weatherCondition.getWeatherId()));*/
            if (weatherIdsInDay.get(weatherCondition.getWeatherId()) == null) {
                weatherIdsInDay.put(weatherCondition.getWeatherId(), 1);
            } else {
                weatherIdsInDay.put(weatherCondition.getWeatherId(), 1 + weatherIdsInDay.get(weatherCondition.getWeatherId()));
            }
            if (!weatherDescriptionsInDay.containsKey(weatherCondition.getWeatherId())) {
                weatherDescriptionsInDay.put(weatherCondition.getWeatherId(), weatherCondition.getDescription());
            }
        }
        Integer maxWeatherIdWithRain = 0;
        Integer maxWeatherIdWithSnow = 0;
        Integer weatherIdForTheDay = 0;
        int maxIconOccurrence = 0;
        for (Integer weatherId : weatherIdsInDay.keySet()) {
            int iconCount = weatherIdsInDay.get(weatherId);
            if (iconCount > maxIconOccurrence) {
                weatherIdForTheDay = weatherId;
                maxIconOccurrence = iconCount;
            }
            if (Utils.isWeatherDescriptionWithRain(weatherId) && (weatherId > maxWeatherIdWithRain)) {
                maxWeatherIdWithRain = weatherId;
            }
            if (Utils.isWeatherDescriptionWithSnow(weatherId) && (weatherId > maxWeatherIdWithSnow)) {
                maxWeatherIdWithSnow = weatherId;
            }
        }
        Integer warningWeatherId = null;
        if ((maxWeatherIdWithRain > 0) || (maxWeatherIdWithSnow > 0)) {
            double rainToConsider = (weatherMaxMinForDay.maxRain > 0.5) ? weatherMaxMinForDay.maxRain : 0;
            double snowToConsider = (weatherMaxMinForDay.maxSnow > 0.5) ? weatherMaxMinForDay.maxSnow : 0;
            if ((rainToConsider > 0) && (rainToConsider > snowToConsider)) {
                warningWeatherId = maxWeatherIdWithRain;
            } else if (snowToConsider > 0) {
                warningWeatherId = maxWeatherIdWithSnow;
            }
        }
        if (weatherIdForTheDay == warningWeatherId) {
            warningWeatherId = null;
        }
        return new WeatherIdsForDay(weatherIdForTheDay,
                warningWeatherId,
                weatherDescriptionsInDay.get(weatherIdForTheDay),
                (warningWeatherId != null) ? weatherDescriptionsInDay.get(warningWeatherId): null);
    }

    public static WeatherForecastForVoice calculateWeatherVoiceForecast(Context context, Long locationId) {
        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId);
        Map<Integer, List<DetailedWeatherForecast>> weatherListForOneDay = getOneDayForecast(weatherForecastRecord);
        WeatherForecastForVoice result = new WeatherForecastForVoice();
        double maxTemp = -Double.MAX_VALUE;
        double minTemp = Double.MAX_VALUE;
        double maxWind = 0;
        double windDegreeForDay = 0;
        Long maxRainTime = null;
        Long maxSnowTime = null;
        Long maxTempTime = null;
        Long minTempTime = null;
        Long maxWindTime = null;
        for (int i = 0; i < 4; i++) {
            List<DetailedWeatherForecast> periodWeatherCondition = weatherListForOneDay.get(i);
            if (periodWeatherCondition == null) {
                continue;
            }
            WeatherMaxMinForDay weatherMaxMinForPeriod = calculateWeatherMaxMinForDay(periodWeatherCondition);
            if (weatherMaxMinForPeriod == null) {
                continue;
            }
            WeatherIdsForDay weatherIdsForPeriod = getWeatherIdForDay(periodWeatherCondition, weatherMaxMinForPeriod);
            if (i == 0) {
                result.nightWeatherIds = weatherIdsForPeriod;
                result.nightWeatherMaxMin = weatherMaxMinForPeriod;
                if (maxTemp < result.nightWeatherMaxMin.maxTemp) {
                    maxTemp = result.nightWeatherMaxMin.maxTemp;
                    maxTempTime = result.nightWeatherMaxMin.maxTempTime;
                }
                if (minTemp > result.nightWeatherMaxMin.minTemp) {
                    minTemp = result.nightWeatherMaxMin.minTemp;
                    minTempTime = result.nightWeatherMaxMin.minTempTime;
                }
                if (maxWind < result.nightWeatherMaxMin.maxWind) {
                    maxWind = result.nightWeatherMaxMin.maxWind;
                    windDegreeForDay = result.nightWeatherMaxMin.windDegree;
                    maxWindTime = result.nightWeatherMaxMin.maxWindTime;
                }
            } else if (i == 1) {
                result.morningWeatherIds = weatherIdsForPeriod;
                result.morningWeatherMaxMin = weatherMaxMinForPeriod;
                if (maxTemp < result.morningWeatherMaxMin.maxTemp) {
                    maxTemp = result.morningWeatherMaxMin.maxTemp;
                    maxTempTime = result.morningWeatherMaxMin.maxTempTime;
                }
                if (minTemp > result.morningWeatherMaxMin.minTemp) {
                    minTemp = result.morningWeatherMaxMin.minTemp;
                    minTempTime = result.morningWeatherMaxMin.minTempTime;
                }
                if (maxWind < result.morningWeatherMaxMin.maxWind) {
                    maxWind = result.morningWeatherMaxMin.maxWind;
                    windDegreeForDay = result.morningWeatherMaxMin.windDegree;
                    maxWindTime = result.morningWeatherMaxMin.maxWindTime;
                }
            } else if (i == 2) {
                result.afternoonWeatherIds = weatherIdsForPeriod;
                result.afternoonWeatherMaxMin = weatherMaxMinForPeriod;
                if (maxTemp < result.afternoonWeatherMaxMin.maxTemp) {
                    maxTemp = result.afternoonWeatherMaxMin.maxTemp;
                    maxTempTime = result.afternoonWeatherMaxMin.maxTempTime;
                }
                if (minTemp > result.afternoonWeatherMaxMin.minTemp) {
                    minTemp = result.afternoonWeatherMaxMin.minTemp;
                    minTempTime = result.afternoonWeatherMaxMin.minTempTime;
                }
                if (maxWind < result.afternoonWeatherMaxMin.maxWind) {
                    maxWind = result.afternoonWeatherMaxMin.maxWind;
                    windDegreeForDay = result.afternoonWeatherMaxMin.windDegree;
                    maxWindTime = result.afternoonWeatherMaxMin.maxWindTime;
                }
            } else {
                result.eveningWeatherIds = weatherIdsForPeriod;
                result.eveningWeatherMaxMin = weatherMaxMinForPeriod;
                if (maxTemp < result.eveningWeatherMaxMin.maxTemp) {
                    maxTemp = result.eveningWeatherMaxMin.maxTemp;
                    maxTempTime = result.eveningWeatherMaxMin.maxTempTime;
                }
                if (minTemp > result.eveningWeatherMaxMin.minTemp) {
                    minTemp = result.eveningWeatherMaxMin.minTemp;
                    minTempTime = result.eveningWeatherMaxMin.minTempTime;
                }
                if (maxWind < result.eveningWeatherMaxMin.maxWind) {
                    maxWind = result.eveningWeatherMaxMin.maxWind;
                    windDegreeForDay = result.eveningWeatherMaxMin.windDegree;
                    maxWindTime = result.eveningWeatherMaxMin.maxWindTime;
                }
                if (result.dayOfYear == null) {
                    result.dayOfYear = weatherMaxMinForPeriod.dayOfYear;
                }
            }
        }
        result.maxTempForDay = maxTemp;
        result.maxRainTime = maxRainTime;
        result.maxSnowTime = maxSnowTime;
        result.maxTempTime = maxTempTime;
        result.minTempTime = minTempTime;
        result.maxWindTime = maxWindTime;
        result.minTempForDay = minTemp;
        result.maxWindForDay = maxWind;
        result.windDegreeForDay = windDegreeForDay;
        return result;
    }

    public static Map<Integer, List<DetailedWeatherForecast>> getOneDayForecast(WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord) {
        Map<Integer, List<DetailedWeatherForecast>> weatherList = createWeatherList(weatherForecastRecord);
        Integer firstDayInForecast = Collections.min(weatherList.keySet());
        List<DetailedWeatherForecast> wholeDayForecast = weatherList.get(firstDayInForecast);
        Map<Integer, List<DetailedWeatherForecast>> oneDayForecastMap = new HashMap<>();
        Calendar forecastCalendar = Calendar.getInstance();
        for (DetailedWeatherForecast detailedWeatherForecast: wholeDayForecast) {
            forecastCalendar.setTimeInMillis(detailedWeatherForecast.getDateTime() * 1000);
            int hourOfDay = forecastCalendar.get(Calendar.HOUR_OF_DAY);
            int dayPeriodIndex;
            if (hourOfDay < 6) {
                dayPeriodIndex = 0;
            } else if (hourOfDay <= 12) {
                dayPeriodIndex = 1;
            } else if (hourOfDay <= 19) {
                dayPeriodIndex = 2;
            } else {
                dayPeriodIndex = 3;
            }
            if (oneDayForecastMap.get(dayPeriodIndex) == null) {
                oneDayForecastMap.put(dayPeriodIndex, new ArrayList<DetailedWeatherForecast>());
            }
            oneDayForecastMap.get(dayPeriodIndex).add(detailedWeatherForecast);
        }
        return oneDayForecastMap;
    }

    public static WeatherMaxMinForDay calculateWeatherMaxMinForDay(List<DetailedWeatherForecast> forecastListForDay) {
        double maxRain = Double.MIN_VALUE;
        double maxSnow = Double.MIN_VALUE;
        double maxTemp = -Double.MAX_VALUE;
        double minTemp = Double.MAX_VALUE;
        double maxWind = 0;
        Long maxRainTime = null;
        Long maxSnowTime = null;
        Long maxTempTime = null;
        Long minTempTime = null;
        Long maxWindTime = null;
        Integer dayOfYear = null;

        if (forecastListForDay.isEmpty()) {
            return null;
        }

        Map <Double, Integer> windDirectionCounter = new HashMap<>();
        for (DetailedWeatherForecast weatherForecastForDay : forecastListForDay) {
                //WeatherCondition weatherCondition = weatherForecastForDay.getFirstWeatherCondition();
                /*appendLog(context, TAG, "preLoadWeather:weatherIdForTheDay=" + weatherIdForTheDay +
                        ":weatherForecastForDay.getTemperature()=" + weatherForecastForDay.getTemperature());*/
            long currentWeatherForecastDateTime = weatherForecastForDay.getDateTime() * 1000;
            if (dayOfYear == null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(currentWeatherForecastDateTime);
                dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
            }
            double currentTemp = weatherForecastForDay.getTemperature();
            if (maxTemp < currentTemp) {
                maxTemp = currentTemp;
                maxTempTime = currentWeatherForecastDateTime;
            }
            if (minTemp > currentTemp) {
                minTemp = currentTemp;
                minTempTime = currentWeatherForecastDateTime;
            }
            if (maxWind < weatherForecastForDay.getWindSpeed()) {
                maxWind = weatherForecastForDay.getWindSpeed();
                maxWindTime = currentWeatherForecastDateTime;
            }
            if (!windDirectionCounter.containsKey(weatherForecastForDay.getWindDegree())) {
                windDirectionCounter.put(weatherForecastForDay.getWindDegree(), 0);
            }
            windDirectionCounter.put(weatherForecastForDay.getWindDegree(), 1 + windDirectionCounter.get(weatherForecastForDay.getWindDegree()));
            if (maxRain < weatherForecastForDay.getRain()) {
                maxRain = weatherForecastForDay.getRain();
                maxRainTime = currentWeatherForecastDateTime;
            }
            if (maxSnow < weatherForecastForDay.getSnow()) {
                maxSnow = weatherForecastForDay.getSnow();
                maxSnowTime = currentWeatherForecastDateTime;
            }
        }
        double resultWindDegree = 0;
        int windCounter = 0;
        for (double windDegree: windDirectionCounter.keySet()) {
            int windDegreeCount = windDirectionCounter.get(windDegree);
            if (windCounter < windDegreeCount) {
                windCounter = windDegreeCount;
                resultWindDegree = windDegree;
            }
        }
        return new WeatherMaxMinForDay(dayOfYear, maxTemp, maxTempTime, minTemp, minTempTime, maxWind, maxWindTime, maxRain, maxRainTime, maxSnow, maxSnowTime, resultWindDegree);
    }

    public static String getWeatherIconId(int weatherId, List<DetailedWeatherForecast> forecastListForDay) {
        for (DetailedWeatherForecast weatherForecastForDay : forecastListForDay) {
            WeatherCondition weatherCondition = weatherForecastForDay.getFirstWeatherCondition();
            if (weatherCondition.getWeatherId().equals(weatherId)) {
                return weatherCondition.getIcon();
            }
        }
        return null;
    }

    public static class WeatherForecastForVoice {
        public Integer dayOfYear;
        public double maxTempForDay;
        public double minTempForDay;
        public double maxWindForDay;
        public double windDegreeForDay;
        public Long maxRainTime;
        public Long maxSnowTime;
        public Long maxTempTime;
        public Long minTempTime;
        public Long maxWindTime;
        public WeatherIdsForDay nightWeatherIds;
        public WeatherMaxMinForDay nightWeatherMaxMin;
        public WeatherIdsForDay morningWeatherIds;
        public WeatherMaxMinForDay morningWeatherMaxMin;
        public WeatherIdsForDay afternoonWeatherIds;
        public WeatherMaxMinForDay afternoonWeatherMaxMin;
        public WeatherIdsForDay eveningWeatherIds;
        public WeatherMaxMinForDay eveningWeatherMaxMin;
    }

    public static class WeatherForecastPerDay {
        int dayIndex;
        String iconId;
        WeatherIdsForDay weatherIds;
        WeatherMaxMinForDay weatherMaxMinForDay;
        int dayInYear;
        int year;

        public WeatherForecastPerDay(
                int dayIndex,
                WeatherIdsForDay weatherIds,
                WeatherMaxMinForDay weatherMaxMinForDay,
                String iconId,
                int dayInYear,
                int year) {
            this.dayIndex = dayIndex;
            this.iconId = iconId;
            this.weatherIds = weatherIds;
            this.weatherMaxMinForDay = weatherMaxMinForDay;
            this.year = year;
            this.dayInYear = dayInYear;
        }
    }

    public static class WeatherMaxMinForDay {
        public Integer dayOfYear;
        public double maxTemp;
        public double minTemp;
        public double maxWind;
        public double maxRain;
        public double maxSnow;
        public double windDegree;
        public Long maxRainTime;
        public Long maxSnowTime;
        public Long maxTempTime;
        public Long minTempTime;
        public Long maxWindTime;

        public WeatherMaxMinForDay(
                Integer dayOfYear,
                double maxTemp,
                Long maxTempTime,
                double minTemp,
                Long minTempTime,
                double maxWind,
                Long maxWindTime,
                double maxRain,
                Long maxRainTime,
                double maxSnow,
                Long maxSnowTime,
                double windDegree) {
            this.dayOfYear = dayOfYear;
            this.maxTemp = maxTemp;
            this.minTemp = minTemp;
            this.maxWind = maxWind;
            this.maxRain = maxRain;
            this.maxSnow = maxSnow;
            this.windDegree = windDegree;
            this.maxRainTime = maxRainTime;
            this.maxSnowTime = maxSnowTime;
            this.maxTempTime = maxTempTime;
            this.minTempTime = minTempTime;
            this.maxWindTime = maxWindTime;
        }
    }

    public static class WeatherIdsForDay {
        public Integer mainWeatherId;
        public Integer warningWeatherId;
        public String mainWeatherDescriptionsFromOwm;
        public String warningWeatherDescriptionsFromOwm;

        public WeatherIdsForDay(
                Integer mainWeatherId,
                Integer warningWeatherId,
                String mainWeatherDescriptionsFromOwm,
                String warningWeatherDescriptionsFromOwm) {
            this.mainWeatherId = mainWeatherId;
            this.warningWeatherId = warningWeatherId;
            this.mainWeatherDescriptionsFromOwm = mainWeatherDescriptionsFromOwm;
            this.warningWeatherDescriptionsFromOwm = warningWeatherDescriptionsFromOwm;
        }
    }
}
