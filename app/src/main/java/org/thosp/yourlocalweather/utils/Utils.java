package org.thosp.yourlocalweather.utils;

import android.app.AlarmManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeather;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsContract;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.ReverseGeocodingCacheContract;
import org.thosp.yourlocalweather.model.ReverseGeocodingCacheDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class Utils {

    public static Bitmap createWeatherIcon(Context context, String text) {
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        Typeface weatherFont = Typeface.createFromAsset(context.getAssets(),
                                                        "fonts/weathericons-regular-webfont.ttf");

        paint.setAntiAlias(true);
        paint.setSubpixelText(true);
        paint.setTypeface(weatherFont);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AppPreference.getTextColor(context));
        paint.setTextSize(180);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, 128, 200, paint);
        return bitmap;
    }

    public static String getStrIcon(Context context, CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        if (weatherRecord == null) {
            return context.getString(R.string.icon_clear_sky_day);
        }
        return getStrIcon(context, weatherRecord.getWeather().getCurrentWeathers().iterator().next().getIdIcon());
    }

    public static String getStrIcon(Context context, String iconId) {
        if (iconId == null) {
            return context.getString(R.string.icon_clear_sky_day);
        }
        String icon;
        switch (iconId) {
            case "01d":
                icon = context.getString(R.string.icon_clear_sky_day);
                break;
            case "01n":
                icon = context.getString(R.string.icon_clear_sky_night);
                break;
            case "02d":
                icon = context.getString(R.string.icon_few_clouds_day);
                break;
            case "02n":
                icon = context.getString(R.string.icon_few_clouds_night);
                break;
            case "03d":
                icon = context.getString(R.string.icon_scattered_clouds);
                break;
            case "03n":
                icon = context.getString(R.string.icon_scattered_clouds);
                break;
            case "04d":
                icon = context.getString(R.string.icon_broken_clouds);
                break;
            case "04n":
                icon = context.getString(R.string.icon_broken_clouds);
                break;
            case "09d":
                icon = context.getString(R.string.icon_shower_rain);
                break;
            case "09n":
                icon = context.getString(R.string.icon_shower_rain);
                break;
            case "10d":
                icon = context.getString(R.string.icon_rain_day);
                break;
            case "10n":
                icon = context.getString(R.string.icon_rain_night);
                break;
            case "11d":
                icon = context.getString(R.string.icon_thunderstorm);
                break;
            case "11n":
                icon = context.getString(R.string.icon_thunderstorm);
                break;
            case "13d":
                icon = context.getString(R.string.icon_snow);
                break;
            case "13n":
                icon = context.getString(R.string.icon_snow);
                break;
            case "50d":
                icon = context.getString(R.string.icon_mist);
                break;
            case "50n":
                icon = context.getString(R.string.icon_mist);
                break;
            default:
                icon = context.getString(R.string.icon_weather_default);
        }

        return icon;
    }

    public static void setWeatherIcon(ImageView imageView,
                                      Context context,
                                      CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        if ("weather_icon_set_fontbased".equals(AppPreference.getIconSet(context))) {
            imageView.setImageBitmap(createWeatherIcon(context, getStrIcon(context, weatherRecord)));
        } else {
            imageView.setImageResource(Utils.getWeatherResourceIcon(weatherRecord));
        }
    }

    public static void setForecastIcon(RemoteViews remoteViews,
                                      Context context,
                                      int viewIconId,
                                       Integer weatherId,
                                       String iconId,
                                       double maxTemp,
                                       double maxWind) {
        if ("weather_icon_set_fontbased".equals(AppPreference.getIconSet(context))) {
            remoteViews.setImageViewBitmap(viewIconId,
                    createWeatherIcon(context, getStrIcon(context, iconId)));
        } else {
            remoteViews.setImageViewResource(viewIconId, Utils.getWeatherResourceIcon(weatherId, maxTemp, maxWind));
        }
    }

    public static void setWeatherIcon(RemoteViews remoteViews,
                                      Context context,
                                      CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                      int widgetIconId) {
        if ("weather_icon_set_fontbased".equals(AppPreference.getIconSet(context))) {
            remoteViews.setImageViewBitmap(widgetIconId,
                    createWeatherIcon(context, getStrIcon(context, weatherRecord)));
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
        switch (weatherId) {
            case 800:
                if (maxTemp > 30) {
                    return R.drawable.ic_weather_set_1_36;
                } else {
                    return R.drawable.ic_weather_set_1_32;
                }
            case 801:
                return R.drawable.ic_weather_set_1_34;
            case 802:
                return R.drawable.ic_weather_set_1_30;
            case 803:
                return R.drawable.ic_weather_set_1_28;
            case 804:
                return R.drawable.ic_weather_set_1_26;
            case 300:
            case 500:
                return R.drawable.ic_weather_set_1_39;
            case 301:
            case 302:
            case 310:
            case 501:
                return R.drawable.ic_weather_set_1_11;
            case 311:
            case 312:
            case 313:
            case 314:
            case 321:
            case 502:
            case 503:
            case 504:
            case 520:
            case 521:
            case 522:
            case 531:
                return R.drawable.ic_weather_set_1_12;
            case 511:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_10;
                else
                    return R.drawable.ic_weather_set_1_08;
            case 701:
                return R.drawable.ic_weather_set_1_22;
            case 711:
            case 721:
            case 731:
            case 741:
            case 751:
            case 761:
                return R.drawable.ic_weather_set_1_20;
            case 762:
                return R.drawable.ic_weather_set_1_na;
            case 771:
            case 781:
                return R.drawable.ic_weather_set_1_24;
            case 200:
            case 210:
            case 230:
                return R.drawable.ic_weather_set_1_38;
            case 201:
            case 202:
            case 211:
            case 212:
            case 221:
            case 231:
            case 232:
                return R.drawable.ic_weather_set_1_17;
            case 600:
                return R.drawable.ic_weather_set_1_13;
            case 601:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_15;
                else
                    return R.drawable.ic_weather_set_1_14;
            case 602:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_15;
                else
                    return R.drawable.ic_weather_set_1_16;
            case 611:
            case 615:
            case 620:
                return R.drawable.ic_weather_set_1_05;
            case 612:
            case 616:
            case 621:
            case 622:
                return R.drawable.ic_weather_set_1_42;
            case 900:
            case 901:
            case 902:
                return R.drawable.ic_weather_set_1_24;
            case 903:
                return R.drawable.ic_weather_set_1_na;
            case 904:
                return R.drawable.ic_weather_set_1_36;
            case 905:
                return R.drawable.ic_weather_set_1_24;
            case 906:
                return R.drawable.ic_weather_set_1_18;
            case 951:
                return R.drawable.ic_weather_set_1_26;
            case 952:
            case 953:
            case 954:
            case 955:
            case 956:
            case 957:
            case 958:
            case 959:
            case 960:
            case 961:
            case 962:
            default:
                return R.drawable.ic_weather_set_1_24;
        }
    }

    public static int getWeatherResourceIcon(CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        if (weatherRecord == null) {
            return R.drawable.ic_weather_set_1_31;
        }
        Weather weather = weatherRecord.getWeather();
        if ((weather.getCurrentWeathers() == null) || weather.getCurrentWeathers().isEmpty()) {
            return R.drawable.ic_weather_set_1_31;
        }
        int weatherId = weather.getCurrentWeathers().iterator().next().getWeatherId();
        boolean strongWind = weather.getWindSpeed() > 5;
        Calendar timeNow = getLocalTimeWithoutDate(weatherRecord.getLastUpdatedTime());
        Calendar sunrise = getLocalTimeWithoutDate(weather.getSunrise() * 1000);
        Calendar sunset = getLocalTimeWithoutDate(weather.getSunset() * 1000);
        boolean day = sunrise.before(timeNow) && timeNow.before(sunset);
        switch (weatherId) {
            case 800:
                if (day) {
                    if (weather.getTemperature() > 30) {
                        return R.drawable.ic_weather_set_1_36;
                    } else {
                        return R.drawable.ic_weather_set_1_32;
                    }
                } else {
                    return R.drawable.ic_weather_set_1_31;
                }
            case 801:
                if (day)
                    return R.drawable.ic_weather_set_1_34;
                else
                    return R.drawable.ic_weather_set_1_33;
            case 802:
                if (day)
                    return R.drawable.ic_weather_set_1_30;
                else
                    return R.drawable.ic_weather_set_1_29;
            case 803:
                if (day)
                    return R.drawable.ic_weather_set_1_28;
                else
                    return R.drawable.ic_weather_set_1_27;
            case 804:
                return R.drawable.ic_weather_set_1_26;
            case 300:
            case 500:
                if (day)
                    return R.drawable.ic_weather_set_1_39;
                else
                    return R.drawable.ic_weather_set_1_45;
            case 301:
            case 302:
            case 310:
            case 501:
                return R.drawable.ic_weather_set_1_11;
            case 311:
            case 312:
            case 313:
            case 314:
            case 321:
            case 502:
            case 503:
            case 504:
            case 520:
            case 521:
            case 522:
            case 531:
                return R.drawable.ic_weather_set_1_12;
            case 511:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_10;
                else
                    return R.drawable.ic_weather_set_1_08;
            case 701:
                if (day)
                    return R.drawable.ic_weather_set_1_22;
                else
                    return R.drawable.ic_weather_set_1_21;
            case 711:
            case 721:
            case 731:
            case 741:
            case 751:
            case 761:
                return R.drawable.ic_weather_set_1_20;
            case 762:
                return R.drawable.ic_weather_set_1_na;
            case 771:
            case 781:
                return R.drawable.ic_weather_set_1_24;
            case 200:
            case 210:
            case 230:
                if (day)
                    return R.drawable.ic_weather_set_1_38;
                else
                    return R.drawable.ic_weather_set_1_45;
            case 201:
            case 202:
            case 211:
            case 212:
            case 221:
            case 231:
            case 232:
                return R.drawable.ic_weather_set_1_17;
            case 600:
                return R.drawable.ic_weather_set_1_13;
            case 601:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_15;
                else
                    return R.drawable.ic_weather_set_1_14;
            case 602:
                if (strongWind)
                    return R.drawable.ic_weather_set_1_15;
                else
                    return R.drawable.ic_weather_set_1_16;
            case 611:
            case 615:
            case 620:
                return R.drawable.ic_weather_set_1_05;
            case 612:
            case 616:
            case 621:
            case 622:
                return R.drawable.ic_weather_set_1_42;
            case 900:
            case 901:
            case 902:
                return R.drawable.ic_weather_set_1_24;
            case 903:
                return R.drawable.ic_weather_set_1_na;
            case 904:
                return R.drawable.ic_weather_set_1_36;
            case 905:
                return R.drawable.ic_weather_set_1_24;
            case 906:
                return R.drawable.ic_weather_set_1_18;
            case 951:
                return R.drawable.ic_weather_set_1_26;
            case 952:
            case 953:
            case 954:
            case 955:
            case 956:
            case 957:
            case 958:
            case 959:
            case 960:
            case 961:
            case 962:
            default:
                return R.drawable.ic_weather_set_1_24;
        }
    }

    public static String getLastUpdateTime(Context context, Location location) {
        return getLastUpdateTime(context, null, null, location);
    }

    public static String getLastUpdateTime(Context context,
                                           CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                           Location location) {
        return getLastUpdateTime(context, weatherRecord, null, location);
    }

    public static String getLastUpdateTime(Context context,
                                           CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                           WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
                                           Location location) {
        Calendar lastUpdate = Calendar.getInstance();
        lastUpdate.setTimeInMillis(getLastUpdateTimeInMilis(weatherRecord, weatherForecastRecord, location));
        int lastUpdateDayOrYear = lastUpdate.get(Calendar.DAY_OF_YEAR);
        int lastUpdateYear = lastUpdate.get(Calendar.YEAR);
        Calendar today = Calendar.getInstance();
        int todayDayOrYear = today.get(Calendar.DAY_OF_YEAR);
        int todayYear = today.get(Calendar.YEAR);
        if ((lastUpdateDayOrYear == todayDayOrYear) && (lastUpdateYear == todayYear)) {
            return AppPreference.getLocalizedTime(context, lastUpdate.getTime(), location.getLocale())
                    + " "
                    + getUpdateSource(context, (location != null) ? location.getLocationSource() : "");
        } else {
            return AppPreference.getLocalizedDateTime(context, lastUpdate.getTime(), (lastUpdateYear != todayYear),location.getLocale())
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

    public static String unixTimeToFormatTime(Context context, long unixTime, Locale locale) {
        long unixTimeToMillis = unixTime * 1000;
        return AppPreference.getLocalizedTime(context, new Date(unixTimeToMillis), locale);
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

    public static URL getWeatherForecastUrl(Context context,
                                            String endpoint,
                                            double lat,
                                            double lon,
                                            String units,
                                            String lang) throws MalformedURLException {
        String url = Uri.parse(endpoint)
                        .buildUpon()
                        .appendQueryParameter("appid", ApiKeys.getOpenweathermapApiKey(context))
                        .appendQueryParameter("lat", String.valueOf(lat).replace(",", "."))
                        .appendQueryParameter("lon", String.valueOf(lon).replace(",", "."))
                        .appendQueryParameter("units", units)
                        .appendQueryParameter("lang", OWMLanguages.getOwmLanguage(lang))
                        .build()
                        .toString();
        return new URL(url);
    }
    
    public static void getAndWriteAddressFromGeocoder(Geocoder geocoder,
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
                locationDbHelper.updateAutoLocationAddress(context, PreferenceUtil.getLanguage(context), address);
            } else {
                locationDbHelper.setNoLocationFound();
            }
        } catch (IOException | NumberFormatException ex) {
            Log.e(Utils.class.getName(), "Unable to get address from latitude and longitude", ex);
        }
    }

    public static String getCityAndCountry(Context context, int locationOrderId) {
        final LocationsDbHelper locationDbHelper = LocationsDbHelper.getInstance(context);
        Location foundLocation = locationDbHelper.getLocationByOrderId(locationOrderId);
        if (foundLocation == null) {
            return context.getString(R.string.location_not_found);
        }
        if ("E".equals(foundLocation.getLocationSource())) {
            if (ApiKeys.isDefaultOpenweatherApiKey(context)) {
                return context.getString(R.string.subscription_expired);
            } else {
                return context.getString(R.string.subscription_is_wrong);
            }
        }
        if (!foundLocation.isAddressFound()) {
            return context.getString(R.string.location_not_found);
        }

        return getCityAndCountryFromAddress(foundLocation.getAddress());
    }

    public static String getWeatherDescription(Context context, String locale, Weather weather) {
        if(AppPreference.hideDescription(context)) {
            return " ";
        }
        StringBuilder currentWeatherDescription = new StringBuilder();
        boolean first = true;
        for (CurrentWeather currentWeather: weather.getCurrentWeathers()) {
            if (!first) {
                currentWeatherDescription.append(", ");
            }
            String owmWeatherDescritpion;
            if ((currentWeather.getDescription() == null) || !OWMLanguages.isLanguageSupportedByOWMAndNotTranslatedLocaly(locale)) {
                owmWeatherDescritpion = context.getString(getWeatherDescriptionResourceId(currentWeather.getWeatherId()));
            } else {
                owmWeatherDescritpion = capitalizeFirstLetter(currentWeather.getDescription());
            }
            currentWeatherDescription.append(owmWeatherDescritpion);
            first = false;
        }
        return currentWeatherDescription.toString();
    }

    private static int getWeatherDescriptionResourceId(int weatherId) {
        switch (weatherId) {
            case 200:
                return R.string.weather_condition_description_200;
            case 201:
                return R.string.weather_condition_description_201;
            case 202:
                return R.string.weather_condition_description_202;
            case 210:
                return R.string.weather_condition_description_210;
            case 211:
                return R.string.weather_condition_description_211;
            case 212:
                return R.string.weather_condition_description_212;
            case 221:
                return R.string.weather_condition_description_221;
            case 230:
                return R.string.weather_condition_description_230;
            case 231:
                return R.string.weather_condition_description_231;
            case 232:
                return R.string.weather_condition_description_232;
            case 300:
                return R.string.weather_condition_description_300;
            case 301:
                return R.string.weather_condition_description_301;
            case 302:
                return R.string.weather_condition_description_302;
            case 310:
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
                return R.string.weather_condition_description_321;
            case 500:
                return R.string.weather_condition_description_500;
            case 501:
                return R.string.weather_condition_description_501;
            case 502:
                return R.string.weather_condition_description_502;
            case 503:
                return R.string.weather_condition_description_503;
            case 504:
                return R.string.weather_condition_description_504;
            case 511:
                return R.string.weather_condition_description_511;
            case 520:
                return R.string.weather_condition_description_520;
            case 521:
                return R.string.weather_condition_description_521;
            case 522:
                return R.string.weather_condition_description_522;
            case 531:
                return R.string.weather_condition_description_531;
            case 600:
                return R.string.weather_condition_description_600;
            case 601:
                return R.string.weather_condition_description_601;
            case 602:
                return R.string.weather_condition_description_602;
            case 611:
                return R.string.weather_condition_description_611;
            case 612:
                return R.string.weather_condition_description_612;
            case 615:
                return R.string.weather_condition_description_615;
            case 616:
                return R.string.weather_condition_description_616;
            case 620:
                return R.string.weather_condition_description_620;
            case 621:
                return R.string.weather_condition_description_621;
            case 622:
                return R.string.weather_condition_description_622;
            case 701:
                return R.string.weather_condition_description_701;
            case 711:
                return R.string.weather_condition_description_711;
            case 721:
                return R.string.weather_condition_description_721;
            case 731:
                return R.string.weather_condition_description_731;
            case 741:
                return R.string.weather_condition_description_741;
            case 751:
                return R.string.weather_condition_description_751;
            case 761:
                return R.string.weather_condition_description_761;
            case 762:
                return R.string.weather_condition_description_762;
            case 771:
                return R.string.weather_condition_description_771;
            case 781:
                return R.string.weather_condition_description_781;
            case 800:
                return R.string.weather_condition_description_800;
            case 801:
                return R.string.weather_condition_description_801;
            case 802:
                return R.string.weather_condition_description_802;
            case 803:
                return R.string.weather_condition_description_803;
            case 804:
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

    private static String capitalizeFirstLetter(String input) {
        if ((input == null) || (input.length() < 1)) {
            return "";
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String getCityAndCountryFromAddress(Address address) {
        if (address == null) {
            return "";
        }
        String geoCity;
        if((address.getLocality() != null) && !"".equals(address.getLocality())) {
            geoCity = address.getLocality();
        } else {
            geoCity = address.getSubAdminArea();
        }
        if (geoCity == null) {
            geoCity = "";
        }
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
}
