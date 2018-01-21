[![Build Status](https://travis-ci.org/thuryn/your-local-weather.svg?branch=master)](https://travis-ci.org/thuryn/your-local-weather)
[![Release](https://img.shields.io/github/release/thuryn/your-local-weather.svg)](https://github.com/thuryn/your-local-weather/releases)
[![License](https://img.shields.io/badge/license-GNU_GPLv3-orange.svg)](https://raw.githubusercontent.com/thuryn/your-local-weather/HEAD/LICENSE)

# Your local weather
Application displays current weather and location wherever you are.

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/repository/browse/?fdid=org.thosp.yourlocalweather)
[<img src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' alt='Get it on Google Play' width='210' heigh='80'>](https://play.google.com/store/apps/details?id=org.thosp.yourlocalweather)

## Description
Application uses cell network, WIFI and GPS to get your location and show you weather on the place where you are actually.
if you use widget on your phone or tablet, you can see your location and current weather on the main screen. Update of location could be used by some specific period (ex. hourly) or application can use accelerometer to detect movement. You can switch update of location off - weather only is updated in this case.

Update by specific period of time:
Application update the location and weather by specified amount of time. For example, you can set update period to one hour o'clock.

Update by accelerometer:
Location is updated by a distance counted by accelerometer. When distance is longer then minimal value for update, application update your location. Distance derived by accelerometer depends on the way how you carry your phone - when you carry your phone in a pocket the distance is increases faster - the phone is moving back and forth as you go. The weather is updated immediately after the update of location. The weather is updated when screen goes on too - but not more often then once per 15 minutes (and when weather is not updated in the last 15 minutes).

To get the location, cell network (BTS) and WIFIs available on the place are used. These information are used to get location coordinates by Mozilla location service. Application try to use GPS when location is not available from cell network and WIFIs. Location coordinates are used to get address by Nominatim service. The address is used in the application or widget.

Application uses OpenWeatherMap.org service to get current weather .

Additional features include:
* Different languages: Czech, Basque, Belarusian, Czech, English, French, German, Japanese, Spanish, Polish, Russian
* Current weather
* 7 day forecast
* Many locations
* Notifications
* Support different measuring units
* Ad-free

## List of contributors of the original app (good-weather)
[qqq3](https://github.com/qqq3) (author of the good-weather app), [berian](https://github.com/beriain), [mahula](https://github.com/mahula), [naofum](https://github.com/naofum), 
[thuryn](https://github.com/thuryn), [monolifed](https://github.com/monolifed), [marcoM32](https://github.com/marcoM32),
[Zagur](https://github.com/Zagur)

## License
```
Good Weather. App displays weather information.
Copyright (C) 2015-2017 Eugene Kislyakov <aiqcms@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
# your-local-weather
