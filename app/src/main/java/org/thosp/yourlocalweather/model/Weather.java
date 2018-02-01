package org.thosp.yourlocalweather.model;

import java.util.ArrayList;
import java.util.List;

public class Weather {

    public Temperature temperature = new Temperature();
    public Wind wind = new Wind();
    private List<CurrentWeather> currentWeathers = new ArrayList<>();
    public CurrentCondition currentCondition = new CurrentCondition();
    public Cloud cloud = new Cloud();
    public Sys sys = new Sys();
    public Coord coord = new Coord();

    public void addCurrentWeather(Integer id, String description, String iconId) {
        currentWeathers.add(new CurrentWeather(id, description, iconId));
    }

    public List<CurrentWeather> getCurrentWeathers() {
        return currentWeathers;
    }

    public class Coord {

        private float lon;
        private float lat;

        public float getLon() {
            return lon;
        }

        public void setLon(float lon) {
            this.lon = lon;
        }

        public float getLat() {
            return lat;
        }

        public void setLat(float lat) {
            this.lat = lat;
        }
    }

    public class Temperature {

        private float mTemp;

        public float getTemp() {
            return mTemp;
        }

        public void setTemp(float temp) {
            mTemp = temp;
        }
    }

    public class Wind {

        private float mSpeed;
        private float mDirection;

        public float getSpeed() {
            return mSpeed;
        }

        public void setSpeed(float speed) {
            mSpeed = speed;
        }

        public float getDirection() {
            return mDirection;
        }

        public void setDirection(float direction) {
            mDirection = direction;
        }
    }

    public class CurrentWeather {

        private String mDescription;
        private String mIdIcon;
        private Integer mWeatherId;

        public CurrentWeather(Integer id, String description, String iconId) {
            this.mWeatherId = id;
            this.mDescription = description;
            this.mIdIcon = iconId;
        }

        public String getDescription() {
            return mDescription;
        }

        public void setDescription(String description) {
            mDescription = description.substring(0, 1).toUpperCase() + description.substring(1);
        }

        public String getIdIcon() {
            return mIdIcon;
        }

        public void setIdIcon(String idIcon) {
            mIdIcon = idIcon;
        }

        public Integer getWeatherId() {
            return mWeatherId;
        }

        public void setWeatherId(Integer mWeatherId) {
            this.mWeatherId = mWeatherId;
        }
    }

    public class CurrentCondition {

        private float mPressure;
        private int mHumidity;

        public float getPressure() {
            return mPressure;
        }

        public void setPressure(float pressure) {
            mPressure = pressure;
        }

        public int getHumidity() {
            return mHumidity;
        }

        public void setHumidity(int humidity) {
            mHumidity = humidity;
        }
    }

    public class Cloud {

        private int mClouds;

        public int getClouds() {
            return mClouds;
        }

        public void setClouds(int clouds) {
            mClouds = clouds;
        }
    }

    public class Sys {

        private long mSunrise;
        private long mSunset;

        public long getSunrise() {
            return mSunrise;
        }

        public void setSunrise(long sunrise) {
            mSunrise = sunrise;
        }

        public long getSunset() {
            return mSunset;
        }

        public void setSunset(long sunset) {
            mSunset = sunset;
        }
    }
}
