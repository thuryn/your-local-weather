package org.thosp.yourlocalweather.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CompleteWeatherInfo implements Parcelable {

    long timestamp = System.currentTimeMillis();
    String locationDescription;
    Location location;

    Weather currentWeather;

    String currentWeatherDescription;

    List<DetailedWeatherForecastWithDetails> weatherForecastList = new ArrayList<>();

    public CompleteWeatherInfo() {
    }

    public void addDetailedWeatherForecast(DetailedWeatherForecastWithDetails detailedWeatherForecast) {
        weatherForecastList.add(detailedWeatherForecast);
    }

    public List<DetailedWeatherForecastWithDetails> getWeatherForecastList() {
        return weatherForecastList;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void updateLocation(Context context, Location location) {
        this.locationDescription = Utils.getCityAndCountry(context, location);
        this.location = location;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public Weather getCurrentWeather() {
        return currentWeather;
    }

    public void updateCurrentWeather(Context context, Weather currentWeather) {
        this.currentWeather = currentWeather;
        currentWeatherDescription = Utils.getWeatherDescription(context, currentWeather);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void updateWeatherForecastList(Context context, List<DetailedWeatherForecast> mWeatherForecastList) {
        String timeStylePreference = AppPreference.getTimeStylePreference(context);
        weatherForecastList = new ArrayList<>();
        for(DetailedWeatherForecast detailedWeatherForecast: mWeatherForecastList ) {
            DetailedWeatherForecastWithDetails detailedWeatherForecastWithDetails = new DetailedWeatherForecastWithDetails();
            detailedWeatherForecastWithDetails.weatherDescription = Utils.getWeatherDescription(detailedWeatherForecast.getWeatherId(), context);
            detailedWeatherForecastWithDetails.dateTimeTxt = AppPreference.getLocalizedTime(
                    context,
                    new Date(detailedWeatherForecast.getDateTime()), timeStylePreference, location.getLocale());
            detailedWeatherForecastWithDetails.setWeatherId(detailedWeatherForecast.getWeatherId());
            detailedWeatherForecastWithDetails.setDateTime(detailedWeatherForecast.getDateTime());
            detailedWeatherForecastWithDetails.setTemperature(detailedWeatherForecast.getTemperature());
            detailedWeatherForecastWithDetails.setTemperatureMax(detailedWeatherForecast.getTemperatureMax());
            detailedWeatherForecastWithDetails.setTemperatureMin(detailedWeatherForecast.getTemperatureMin());
            detailedWeatherForecastWithDetails.setPressure(detailedWeatherForecast.getPressure());
            detailedWeatherForecastWithDetails.setHumidity(detailedWeatherForecast.getHumidity());
            detailedWeatherForecastWithDetails.setWindDegree(detailedWeatherForecast.getWindDegree());
            detailedWeatherForecastWithDetails.setWindSpeed(detailedWeatherForecast.getWindSpeed());
            detailedWeatherForecastWithDetails.setCloudiness(detailedWeatherForecast.getCloudiness());
            detailedWeatherForecastWithDetails.setRain(detailedWeatherForecast.getRain());
            detailedWeatherForecastWithDetails.setSnow(detailedWeatherForecast.getSnow());
            weatherForecastList.add(detailedWeatherForecastWithDetails);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(timestamp);
        parcel.writeParcelable(location, 0);
        parcel.writeString(locationDescription);
        parcel.writeParcelable(currentWeather, 0);
        parcel.writeString(currentWeatherDescription);
        parcel.writeTypedList(weatherForecastList);
    }

    public static final Parcelable.Creator<CompleteWeatherInfo> CREATOR
            = new Parcelable.Creator<CompleteWeatherInfo>() {
        public CompleteWeatherInfo createFromParcel(Parcel in) {
            return new CompleteWeatherInfo(in);
        }

        public CompleteWeatherInfo[] newArray(int size) {
            return new CompleteWeatherInfo[size];
        }
    };

    private CompleteWeatherInfo(Parcel in) {
        timestamp = in.readLong();
        location = in.readParcelable(Location.class.getClassLoader());
        locationDescription = in.readString();
        currentWeather = in.readParcelable(Weather.class.getClassLoader());
        currentWeatherDescription = in.readString();
        in.readTypedList(weatherForecastList, DetailedWeatherForecastWithDetails.CREATOR);
    }
}
