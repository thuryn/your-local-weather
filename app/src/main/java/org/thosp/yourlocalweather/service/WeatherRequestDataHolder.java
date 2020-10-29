package org.thosp.yourlocalweather.service;

import java.io.Serializable;

public class WeatherRequestDataHolder implements Serializable {
    private final long locationId;
    private final String updateSource;
    private final boolean updateWeatherOnly;
    private int attempts;
    private final long timestamp;
    private final boolean forceUpdate;
    private final int updateType;

    public WeatherRequestDataHolder(long locationId, String updateSource, int updateType) {
        this.locationId = locationId;
        this.updateSource = updateSource;
        this.attempts = 0;
        this.timestamp = System.currentTimeMillis();
        this.forceUpdate = false;
        this.updateWeatherOnly = false;
        this.updateType = updateType;
    }

    public WeatherRequestDataHolder(long locationId, String updateSource, boolean forceUpdate, int updateType) {
        this.locationId = locationId;
        this.updateSource = updateSource;
        this.attempts = 0;
        this.timestamp = System.currentTimeMillis();
        this.forceUpdate = forceUpdate;
        this.updateWeatherOnly = false;
        this.updateType = updateType;
    }

    public WeatherRequestDataHolder(long locationId, String updateSource, boolean forceUpdate, boolean updateWeatherOnly, int updateType) {
        this.locationId = locationId;
        this.updateSource = updateSource;
        this.attempts = 0;
        this.timestamp = System.currentTimeMillis();
        this.forceUpdate = forceUpdate;
        this.updateWeatherOnly = updateWeatherOnly;
        this.updateType = updateType;
    }

    public void increaseAttempts() {
        attempts++;
    }

    public int getAttempts() {
        return attempts;
    }

    public long getLocationId() {
        return locationId;
    }

    public String getUpdateSource() {
        return updateSource;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public boolean isUpdateWeatherOnly() {
        return updateWeatherOnly;
    }

    public int getUpdateType() {
        return updateType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof WeatherRequestDataHolder)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        WeatherRequestDataHolder objToCompareTo = (WeatherRequestDataHolder) obj;
        return (this.locationId == objToCompareTo.locationId) &&
               (this.updateSource == null ? objToCompareTo.updateSource == null
                       : this.updateSource.equals(objToCompareTo.updateSource)) &&
                (this.forceUpdate == objToCompareTo.forceUpdate) &&
               (this.updateType == objToCompareTo.updateType) &&
               (this.updateWeatherOnly == objToCompareTo.updateWeatherOnly);
    }

    @Override
    public int hashCode() {
        return (int) locationId;
    }
    
    @Override
    public String toString() {
        return "WeatherRequestDataHolder:locationId=" + locationId + ", updateSource="
                + updateSource + ", attempts=" + attempts +
                ", forceUpdate=" + forceUpdate + ", updateWeatherOnly=" + updateWeatherOnly
                + ", updateType=" + updateType;
    }
}
