package org.thosp.yourlocalweather.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeather;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.core.content.ContextCompat;

public class Utils {

    private static final String TAG = "Utils";

    public static Bitmap createWeatherIcon(Context context, String text) {
        return createWeatherIconWithColor(context, text, AppPreference.getWidgetTextColor(context));
    }

    public static Bitmap createWeatherIconWithColor(Context context, String text, int iconColor) {
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        Typeface weatherFont = Typeface.createFromAsset(context.getAssets(),
                                                        "fonts/weathericons-regular-webfont.ttf");

        paint.setAntiAlias(true);
        paint.setSubpixelText(true);
        paint.setTypeface(weatherFont);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(iconColor);
        paint.setTextSize(180);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, 128, 200, paint);
        return bitmap;
    }

    public static String getStrIconFromWEatherRecord(Context context, CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        if ((weatherRecord == null) || (weatherRecord.getWeather() == null)) {
            return context.getString(R.string.icon_clear_sky_day);
        }
        return getStrIcon(context, weatherRecord.getWeather().getWeatherId());
    }

    public static String getStrIcon(Context context, int weatherId) {
        if (weatherId == 0) {
            return context.getString(R.string.icon_clear_sky_day);
        }
        String icon;
        switch (weatherId) {
            case 0:
                icon = context.getString(R.string.icon_clear_sky_day);
                //icon = context.getString(R.string.icon_clear_sky_night);
                break;
            case 1:
                icon = context.getString(R.string.icon_few_clouds_day);
                //icon = context.getString(R.string.icon_few_clouds_night);
                break;
            case 2:
                icon = context.getString(R.string.icon_scattered_clouds);
                break;
            case 3:
                icon = context.getString(R.string.icon_broken_clouds);
                break;
            case 51:
            case 61:
            case 56:
            case 66:
            case 80:
                icon = context.getString(R.string.icon_shower_rain);
                break;
            case 53:
            case 55:
            case 57:
            case 63:
            case 65:
            case 67:
            case 81:
            case 82:
                icon = context.getString(R.string.icon_rain_day);
                break;
            case 96:
            case 95:
            case 99:
                icon = context.getString(R.string.icon_thunderstorm);
                break;
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                icon = context.getString(R.string.icon_snow);
                break;
            case 45:
            case 48:
                icon = context.getString(R.string.icon_mist);
                break;
            default:
                icon = context.getString(R.string.icon_weather_default);
        }

        return icon;
    }

    public static void setWeatherIconWithColor(ImageView imageView,
                                      Context context,
                                      CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                      int fontColorId, boolean fontBasedIconSet) {
        if (fontBasedIconSet) {
            imageView.setImageBitmap(createWeatherIconWithColor(context, getStrIconFromWEatherRecord(context, weatherRecord), fontColorId));
        } else {
            imageView.setImageResource(Utils.getWeatherResourceIcon(weatherRecord));
        }
    }

    public static void setWeatherIcon(ImageView imageView,
                                      Context context,
                                      CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                      int textColor,
                                      boolean fontBasedIconSet) {
        setWeatherIconWithColor(imageView, context, weatherRecord, textColor, fontBasedIconSet);
    }

    public static void setForecastIcon(RemoteViews remoteViews,
                                      Context context,
                                      int viewIconId,
                                       boolean fontBasedIcons,
                                       Integer weatherId,
                                       double maxTemp,
                                       double maxWind,
                                       int fontColorId) {
        if (fontBasedIcons) {
            remoteViews.setImageViewBitmap(viewIconId,
                    createWeatherIconWithColor(context, getStrIcon(context, weatherId), fontColorId));
        } else {
            remoteViews.setImageViewResource(viewIconId, Utils.getWeatherResourceIcon(weatherId, maxTemp, maxWind));
        }
    }

    public static void setWeatherIconWithColor(RemoteViews remoteViews,
                                      Context context,
                                      CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                      boolean fontBasedIcons,
                                      int widgetIconId,
                                      int fontColorId) {
        if (fontBasedIcons) {
            remoteViews.setImageViewBitmap(widgetIconId,
                    createWeatherIconWithColor(context, getStrIconFromWEatherRecord(context, weatherRecord), fontColorId));
        } else {
            remoteViews.setImageViewResource(widgetIconId, Utils.getWeatherResourceIcon(weatherRecord));
        }
    }

    public static int getWeatherResourceIcon(Integer weatherId,
                                             double maxTemp,
                                             double maxWind) {
        if (weatherId == null) {
            return R.drawable.ic_weather_set_1_31;
        }
        boolean strongWind = maxWind > 5;
        boolean veryStrongWind = maxWind > 15;
        switch (weatherId) {
            case 0:
                if (maxTemp > 30) {
                    return R.drawable.ic_weather_set_1_36;
                } else if (veryStrongWind) {
                    return R.drawable.ic_weather_set_1_24;
                } else {
                    return R.drawable.ic_weather_set_1_32;
                }
            /*case 1:
                if (day)
                    return R.drawable.ic_weather_set_1_34;
                else
                    return R.drawable.ic_weather_set_1_33;*/
            case 1:
                if (veryStrongWind) {
                    return R.drawable.ic_weather_set_1_24;
                } else {
                    return R.drawable.ic_weather_set_1_30;
                }
            case 2:
                if (veryStrongWind) {
                    return R.drawable.ic_weather_set_1_24;
                } else {
                    return R.drawable.ic_weather_set_1_28;
                }
            case 3:
                if (veryStrongWind) {
                    return R.drawable.ic_weather_set_1_24;
                } else {
                    return R.drawable.ic_weather_set_1_26;
                }
            case 51:
            case 80:
                return R.drawable.ic_weather_set_1_39;
            case 53:
            case 55:
            case 81:
                return R.drawable.ic_weather_set_1_11;
            case 61:
            case 63:
            case 65:
            case 82:
                return R.drawable.ic_weather_set_1_12;
            case 56:
            case 57:
            case 66:
            case 67:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_10;
                else
                    return R.drawable.ic_weather_set_1_08;
            case 45:
                return R.drawable.ic_weather_set_1_22;
            case 48:
                return R.drawable.ic_weather_set_1_20;
            case 96:
                return R.drawable.ic_weather_set_1_38;
            case 95:
            case 99:
                return R.drawable.ic_weather_set_1_17;
            case 71:
            case 85:
                return R.drawable.ic_weather_set_1_13;
            case 73:
            case 86:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_15;
                else
                    return R.drawable.ic_weather_set_1_14;
            case 75:
            case 77:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_15;
                else
                    return R.drawable.ic_weather_set_1_16;
            default:
                return R.drawable.ic_weather_set_1_24;
        }
    }

    public static int getWeatherResourceIcon(CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        if (weatherRecord == null) {
            return R.drawable.ic_weather_set_1_31;
        }
        Weather weather = weatherRecord.getWeather();
        if ((weather == null) || (weather.getWeatherId() == 0)) {
            return R.drawable.ic_weather_set_1_31;
        }
        boolean strongWind = weather.getWindSpeed() > 5;
        boolean veryStrongWind = weather.getWindSpeed() > 15;
        Calendar timeNow = getLocalTimeWithoutDate(weatherRecord.getLastUpdatedTime());
        Calendar sunrise = getLocalTimeWithoutDate(weather.getSunrise() * 1000);
        Calendar sunset = getLocalTimeWithoutDate(weather.getSunset() * 1000);
        boolean day = sunrise.before(timeNow) && timeNow.before(sunset);
        switch (weather.getWeatherId()) {
            case 0:
                if (day) {
                    if (weather.getTemperature() > 30) {
                        return R.drawable.ic_weather_set_1_36;
                    } else if (veryStrongWind) {
                        return R.drawable.ic_weather_set_1_24;
                    } else {
                        return R.drawable.ic_weather_set_1_32;
                    }
                } else if (veryStrongWind) {
                    return R.drawable.ic_weather_set_1_24;
                } else {
                    return R.drawable.ic_weather_set_1_31;
                }
            /*case 1:
                if (day)
                    return R.drawable.ic_weather_set_1_34;
                else
                    return R.drawable.ic_weather_set_1_33;*/
            case 1:
                if (veryStrongWind) {
                    return R.drawable.ic_weather_set_1_24;
                } else if (day) {
                    return R.drawable.ic_weather_set_1_30;
                } else {
                    return R.drawable.ic_weather_set_1_29;
                }
            case 2:
                if (veryStrongWind) {
                    return R.drawable.ic_weather_set_1_24;
                } else if (day) {
                    return R.drawable.ic_weather_set_1_28;
                } else {
                    return R.drawable.ic_weather_set_1_27;
                }
            case 3:
                if (veryStrongWind) {
                    return R.drawable.ic_weather_set_1_24;
                } else {
                    return R.drawable.ic_weather_set_1_26;
                }
            case 51:
            case 80:
                if (day)
                    return R.drawable.ic_weather_set_1_39;
                else
                    return R.drawable.ic_weather_set_1_45;
            case 53:
            case 55:
            case 81:
                return R.drawable.ic_weather_set_1_11;
            case 61:
            case 63:
            case 65:
            case 82:
                return R.drawable.ic_weather_set_1_12;
            case 56:
            case 57:
            case 66:
            case 67:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_10;
                else
                    return R.drawable.ic_weather_set_1_08;
            case 45:
                if (day)
                    return R.drawable.ic_weather_set_1_22;
                else
                    return R.drawable.ic_weather_set_1_21;
            case 48:
                return R.drawable.ic_weather_set_1_20;
            case 96:
                if (day)
                    return R.drawable.ic_weather_set_1_38;
                else
                    return R.drawable.ic_weather_set_1_45;
            case 95:
            case 99:
                return R.drawable.ic_weather_set_1_17;
            case 71:
            case 85:
                return R.drawable.ic_weather_set_1_13;
            case 73:
            case 86:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_15;
                else
                    return R.drawable.ic_weather_set_1_14;
            case 75:
            case 77:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_15;
                else
                    return R.drawable.ic_weather_set_1_16;
                //return R.drawable.ic_weather_set_1_05;
                //return R.drawable.ic_weather_set_1_42;
                //return R.drawable.ic_weather_set_1_18;
        }
        return R.drawable.ic_weather_set_1_na;
    }

    public static String getLastUpdateTime(Context context, Location location) {
        return getLastUpdateTime(context, null, null, location);
    }

    public static String getLastUpdateTime(Context context,
                                           CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                           String timeStylePreference,
                                           Location location) {
        return getLastUpdateTime(context, weatherRecord, null, timeStylePreference, location);
    }

    public static String getLastUpdateTime(Context context,
                                           CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                           WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
                                           String timeStylePreference,
                                           Location location) {
        Calendar lastUpdate = Calendar.getInstance();
        lastUpdate.setTimeInMillis(getLastUpdateTimeInMilis(weatherRecord, weatherForecastRecord, location));
        int lastUpdateDayOrYear = lastUpdate.get(Calendar.DAY_OF_YEAR);
        int lastUpdateYear = lastUpdate.get(Calendar.YEAR);
        Calendar today = Calendar.getInstance();
        int todayDayOrYear = today.get(Calendar.DAY_OF_YEAR);
        int todayYear = today.get(Calendar.YEAR);
        if ((lastUpdateDayOrYear == todayDayOrYear) && (lastUpdateYear == todayYear)) {
            return AppPreference.getLocalizedTime(context, lastUpdate.getTime(), timeStylePreference, location.getLocale())
                    + " "
                    + getUpdateSource(context, (location != null) ? location.getLocationSource() : "");
        } else {
            return AppPreference.getLocalizedDateTime(context, lastUpdate.getTime(), (lastUpdateYear != todayYear), timeStylePreference, location.getLocale())
                    + " "
                    + getUpdateSource(context, (location != null) ? location.getLocationSource() : "");
        }
    }

    public static long getLastUpdateTimeInMilis(
                                         CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                         WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
                                         Location location) {
        List<Long> lastUpdateTimes = new ArrayList<>();
        lastUpdateTimes.add((weatherForecastRecord != null)?weatherForecastRecord.getLastUpdatedTime():0);
        lastUpdateTimes.add((weatherRecord != null)?weatherRecord.getLastUpdatedTime():0);
        lastUpdateTimes.add((location != null)?location.getLastLocationUpdate():0);

        return Collections.max(lastUpdateTimes);
    }

    public static long intervalMillisForAlarm(String intervalMinutes) {
        switch (intervalMinutes) {
            case "0":
            case "15":
                return AlarmManager.INTERVAL_FIFTEEN_MINUTES;
            case "30":
                return AlarmManager.INTERVAL_HALF_HOUR;
            case "60":
                return AlarmManager.INTERVAL_HOUR;
            case "720":
                return AlarmManager.INTERVAL_HALF_DAY;
            case "1440":
                return AlarmManager.INTERVAL_DAY;
            case "OFF":
            case "regular_only":
                return Long.MAX_VALUE;
            default:
                return Integer.parseInt(intervalMinutes) * 60 * 1000;
        }
    }


    public static String getUpdateSource(Context context, String locationSource) {
        String updateDetailLevel = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_UPDATE_DETAIL, "preference_display_update_nothing");
        switch (updateDetailLevel) {
            case "preference_display_update_value":
            case "preference_display_update_location_source":
                return locationSource;
            case "preference_display_update_nothing":
            default:
                return "";
        }
    }

    public static String unixTimeToFormatTime(Context context, long unixTime, String timeStylePreference, Locale locale) {
        long unixTimeToMillis = unixTime * 1000;
        return AppPreference.getLocalizedTime(context, new Date(unixTimeToMillis), timeStylePreference, locale);
    }

    public static void copyToClipboard(Context context, String string) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(string, string);
        clipboardManager.setPrimaryClip(clipData);
    }

    public static String windDegreeToDirections(Context context, double windDegree) {
        String[] directions = context.getResources().getStringArray(R.array.wind_directions);
        String[] arrows = context.getResources().getStringArray(R.array.wind_direction_arrows);
        int index = (int) Math.abs(Math.round(windDegree % 360) / 45);

        return directions[index] + " " + arrows[index];
    }

    public static URL getOwmUrl(Context context, Location location) throws MalformedURLException {
        String latitude = String.valueOf(location.getLatitude()).replace(",", ".");
        String longitude = String.valueOf(location.getLongitude()).replace(",", ".");
        Object[] params = new Object[]{latitude, longitude};
        String url = MessageFormat.format(Constants.WEATHER_ENDPOINT, params);
        appendLog(context, TAG, url);
        return new URL(url);
    }
    
    public static Address getAndWriteAddressFromGeocoder(Geocoder geocoder,
                                                      Address address,
                                                      double latitude,
                                                      double longitude,
                                                      boolean resolveAddressByOS,
                                                      Context context) {
        try {
            final LocationsDbHelper locationDbHelper = LocationsDbHelper.getInstance(context);
            if (resolveAddressByOS) {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if((addresses != null) && (addresses.size() > 0)) {
                    address = addresses.get(0);
                }
            }
            if(address != null) {
                locationDbHelper.updateAutoLocationAddress(context, AppPreference.getInstance().getLanguage(context), address);
            } else {
                locationDbHelper.setNoLocationFound();
            }
        } catch (IOException | NumberFormatException ex) {
            Log.e(Utils.class.getName(), "Unable to get address from latitude and longitude", ex);
        }
        return address;
    }

    public static String getCityAndCountry(Context context, Location location) {
        if (location == null) {
            return context.getString(R.string.location_not_found);
        }
        if ("E".equals(location.getLocationSource())) {
            return context.getString(R.string.subscription_expired);
        }
        if (!location.isAddressFound()) {
            return context.getString(R.string.location_not_found);
        }

        return getCityAndCountryFromAddress(location.getAddress());
    }

    public static String getWeatherDescription(Context context, String locale, Weather weather) {
        if((weather == null) || AppPreference.hideDescription(context)) {
            return " ";
        }
        return getWeatherDescription(
                weather.getWeatherId(),
                context);
    }

    public static String getWeatherDescription(int weatherId, Context context) {
        return context.getString(getWeatherDescriptionResourceId(weatherId));
    }

    public static boolean isWeatherDescriptionWithRain(int weatherId) {
        if (((weatherId > 50) && (weatherId < 70)) || ((weatherId >= 80) && (weatherId < 85))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isWeatherDescriptionWithSnow(int weatherId) {
        if (((weatherId >= 70) && (weatherId < 80)) || ((weatherId >= 85) && (weatherId < 90))) {
            return true;
        } else {
            return false;
        }
    }

    private static int getWeatherDescriptionResourceId(int weatherId) {
        switch (weatherId) {
            case 96:
                return R.string.weather_condition_description_200;
            case 99:
                return R.string.weather_condition_description_201;
            /*case 202:
                return R.string.weather_condition_description_202;
            case 210:
                return R.string.weather_condition_description_210;*/
            case 95:
                return R.string.weather_condition_description_211;
            /*case 212:
                return R.string.weather_condition_description_212;
            case 221:
                return R.string.weather_condition_description_221;
            case 230:
                return R.string.weather_condition_description_230;
            case 231:
                return R.string.weather_condition_description_231;
            case 232:
                return R.string.weather_condition_description_232;*/
            case 51:
                return R.string.weather_condition_description_300;
            case 53:
                return R.string.weather_condition_description_301;
            case 55:
                return R.string.weather_condition_description_302;
            /*case 310:
                return R.string.weather_condition_description_310;
            case 311:
                return R.string.weather_condition_description_311;
            case 312:
                return R.string.weather_condition_description_312;
            case 313:
                return R.string.weather_condition_description_313;
            case 314:
                return R.string.weather_condition_description_314;
            case 321:
                return R.string.weather_condition_description_321;*/
            case 61:
                return R.string.weather_condition_description_500;
            case 63:
                return R.string.weather_condition_description_501;
            case 65:
                return R.string.weather_condition_description_502;
            /*case 503:
                return R.string.weather_condition_description_503;
            case 504:
                return R.string.weather_condition_description_504;*/
            case 56:
            case 57:
            case 66:
            case 67:
                return R.string.weather_condition_description_511;
            case 80:
                return R.string.weather_condition_description_520;
            case 81:
                return R.string.weather_condition_description_521;
            case 82:
                return R.string.weather_condition_description_522;
            case 531:
                return R.string.weather_condition_description_531;
            case 71:
                return R.string.weather_condition_description_600;
            case 73:
            case 77:
                return R.string.weather_condition_description_601;
            case 75:
                return R.string.weather_condition_description_602;
            /*case 611:
                return R.string.weather_condition_description_611;
            case 612:
                return R.string.weather_condition_description_612;
            case 615:
                return R.string.weather_condition_description_615;
            case 616:
                return R.string.weather_condition_description_616;*/
            case 85:
                return R.string.weather_condition_description_620;
            case 86:
                return R.string.weather_condition_description_621;
            /*case 622:
                return R.string.weather_condition_description_622;
            case 701:
                return R.string.weather_condition_description_701;
            case 711:
                return R.string.weather_condition_description_711;
            case 721:
                return R.string.weather_condition_description_721;
            case 731:
                return R.string.weather_condition_description_731;*/
            case 45:
                return R.string.weather_condition_description_701;
            case 48:
                return R.string.weather_condition_description_741;
            /*case 751:
                return R.string.weather_condition_description_751;
            case 761:
                return R.string.weather_condition_description_761;
            case 762:
                return R.string.weather_condition_description_762;
            case 771:
                return R.string.weather_condition_description_771;
            case 781:
                return R.string.weather_condition_description_781;*/
            case 0:
                return R.string.weather_condition_description_800;
            case 1:
                return R.string.weather_condition_description_801;
            case 2:
                return R.string.weather_condition_description_802;
            /*case 803:
                return R.string.weather_condition_description_803;*/
            case 3:
                return R.string.weather_condition_description_804;
            default:
                return R.string.weather_condition_description_none;
        }
    }

    private static Calendar getLocalTimeWithoutDate(long timeInMilis) {
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(timeInMilis);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.YEAR, 1970);
        return calendar;
    }

    public static String getCityAndCountryFromAddress(Address address) {
        if (address == null) {
            return "";
        }
        String geoCity = getCityFromAddress(address);
        String geoCountryDistrict = null;
        if(address.getAdminArea() != null) {
            geoCountryDistrict = address.getAdminArea();
        }
        String geoDistrictOfCity = address.getSubLocality();
        String geoCountryName = address.getCountryName();
        if ((geoDistrictOfCity == null) || "".equals(geoDistrictOfCity) || geoCity.equalsIgnoreCase(geoDistrictOfCity)) {
            if ((geoCountryDistrict == null) || "".equals(geoCountryDistrict) || geoCity.equals(geoCountryDistrict)) {
                return formatLocalityToTwoLines((("".equals(geoCity))?"":(geoCity)) + (("".equals(geoCountryName))?"":(", " + geoCountryName)));
            }
            return formatLocalityToTwoLines((("".equals(geoCity))?"":(geoCity + ", ")) + geoCountryDistrict + (("".equals(geoCountryName))?"":(", " + geoCountryName)));
        }
        return formatLocalityToTwoLines((("".equals(geoCity))?"":(geoCity + " - ")) + geoDistrictOfCity + (("".equals(geoCountryName))?"":(", " + geoCountryName)));
    }

    public static String getCityFromAddress(Address address) {
        if (address == null) {
            return "";
        }
        String geoCity;
        if ((address.getLocality() != null) && !"".equals(address.getLocality())) {
            geoCity = address.getLocality();
        } else {
            geoCity = address.getSubAdminArea();
        }
        if (geoCity == null) {
            geoCity = "";
        }
        return geoCity;
    }

    public static String getLocationForSharingFromAddress(Address address) {
        if (address == null) {
            return "";
        }
        String geoCity = getCityFromAddress(address);
        String geoDistrictOfCity = address.getSubLocality();
        if ((geoDistrictOfCity == null) || "".equals(geoDistrictOfCity) || geoCity.equalsIgnoreCase(geoDistrictOfCity)) {
            return geoCity;
        }
        return (("".equals(geoCity))?"":(geoCity + " - ")) + geoDistrictOfCity;
    }

    public static String getLocationForVoiceFromAddress(Address address) {
        if (address == null) {
            return "";
        }
        String geoCity = getCityFromAddress(address);
        String geoDistrictOfCity = address.getSubLocality();
        if ((geoDistrictOfCity == null) || "".equals(geoDistrictOfCity) || geoCity.equalsIgnoreCase(geoDistrictOfCity)) {
            return geoCity;
        }
        return (("".equals(geoCity))?"":(geoCity + " ")) + geoDistrictOfCity;
    }

    private static String formatLocalityToTwoLines(String inputLocation) {
        if (inputLocation.length() < 30) {
            return inputLocation;
        }
        if (inputLocation.indexOf(",") < 30) {
            inputLocation.replaceFirst(", ", "\n");
        }
        return inputLocation;
    }

    public static int spToPx(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static BluetoothAdapter getBluetoothAdapter(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return null;
            }
            return bluetoothManager.getAdapter();
        }
        return BluetoothAdapter.getDefaultAdapter();
    }

    public static boolean isBluetoothHeadsetEnabledConnected(Context context) {
        BluetoothAdapter bluetoothAdapter = Utils.getBluetoothAdapter(context);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        boolean isBtConnected = (bluetoothAdapter != null && (
                BluetoothAdapter.STATE_CONNECTED == bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) ||
                        BluetoothAdapter.STATE_CONNECTED == bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP)));
        if (!isBtConnected) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (!sharedPreferences.getStringSet(Constants.CONNECTED_BT_DEVICES, new HashSet<String>()).isEmpty()) {
                sharedPreferences.edit().putStringSet(Constants.CONNECTED_BT_DEVICES, new HashSet<String>()).apply();
            }
        }
        return isBtConnected;
    }

    public static Set<String> getAllConnectedBtDevices(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getStringSet(Constants.CONNECTED_BT_DEVICES, new HashSet<String>());
    }

}
