package org.thosp.yourlocalweather.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.utils.NotificationForecastEvaluator.ConditionType;
import org.thosp.yourlocalweather.utils.NotificationForecastEvaluator.EvaluationResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class NotificationForecastEvaluatorTest {

    private static final long HOUR_MS = 3_600_000L;
    private static final long MINUTE_MS = 60_000L;
    private static final long BASE_NOW_MS = 1_700_000_000_000L;

    private static DetailedWeatherForecast forecastAtOffsetHours(
            int offsetHours,
            int weatherId,
            double rain,
            double snow,
            double windSpeedMs) {
        DetailedWeatherForecast forecast = new DetailedWeatherForecast();
        forecast.setDateTime((BASE_NOW_MS + offsetHours * HOUR_MS) / 1000L);
        forecast.setWeatherId(weatherId);
        forecast.setRain(rain);
        forecast.setSnow(snow);
        forecast.setWindSpeed(windSpeedMs);
        return forecast;
    }

    @Test
    public void evaluateForecast_emptyOrNull_returnsNoMatch() {
        Set<ConditionType> conditions = EnumSet.of(ConditionType.RAIN);
        assertFalse(NotificationForecastEvaluator.evaluateForecast(null, BASE_NOW_MS, 6, conditions, 10.0).isMatched());
        assertFalse(NotificationForecastEvaluator.evaluateForecast(Collections.emptyList(), BASE_NOW_MS, 6, conditions, 10.0).isMatched());
        assertFalse(NotificationForecastEvaluator.evaluateForecast(
                Collections.singletonList(forecastAtOffsetHours(1, 61, 2.0, 0.0, 3.0)),
                BASE_NOW_MS,
                6,
                EnumSet.noneOf(ConditionType.class),
                10.0).isMatched());
    }

    @Test
    public void evaluateForecast_respectsLookaheadWindow() {
        List<DetailedWeatherForecast> list = Arrays.asList(
                forecastAtOffsetHours(-1, 61, 5.0, 0.0, 2.0), // past -> ignored
                forecastAtOffsetHours(2, 0, 0.0, 0.0, 2.0),   // clear
                forecastAtOffsetHours(8, 63, 4.0, 0.0, 2.0)   // beyond 6h window -> ignored
        );

        EvaluationResult result = NotificationForecastEvaluator.evaluateForecast(
                list, BASE_NOW_MS, 6, EnumSet.of(ConditionType.RAIN), 10.0);
        assertFalse(result.isMatched());

        EvaluationResult result12h = NotificationForecastEvaluator.evaluateForecast(
                list, BASE_NOW_MS, 12, EnumSet.of(ConditionType.RAIN), 10.0);
        assertTrue(result12h.isMatched());
        assertEquals(BASE_NOW_MS + 8 * HOUR_MS, result12h.getFirstMatchTimeMs());
    }

    @Test
    public void evaluateForecast_matchesRainSnowThunderstormFog() {
        List<DetailedWeatherForecast> list = Arrays.asList(
                forecastAtOffsetHours(1, 45, 0.0, 0.0, 2.0),  // fog
                forecastAtOffsetHours(2, 71, 0.0, 1.5, 2.0),  // snow
                forecastAtOffsetHours(3, 95, 3.0, 0.0, 5.0)   // thunderstorm + rain
        );

        EvaluationResult result = NotificationForecastEvaluator.evaluateForecast(
                list,
                BASE_NOW_MS,
                6,
                EnumSet.of(ConditionType.RAIN, ConditionType.SNOW, ConditionType.THUNDERSTORM, ConditionType.FOG),
                10.0);

        assertTrue(result.isMatched());
        assertEquals(
                EnumSet.of(ConditionType.FOG, ConditionType.SNOW, ConditionType.THUNDERSTORM, ConditionType.RAIN),
                result.getMatchedConditions());
        assertEquals(BASE_NOW_MS + 1 * HOUR_MS, result.getFirstMatchTimeMs());
    }

    @Test
    public void evaluateForecast_windThreshold() {
        List<DetailedWeatherForecast> list = Arrays.asList(
                forecastAtOffsetHours(1, 0, 0.0, 0.0, 9.5),   // below 10 m/s
                forecastAtOffsetHours(2, 0, 0.0, 0.0, 10.0),  // at threshold
                forecastAtOffsetHours(3, 0, 0.0, 0.0, 14.2)   // above threshold
        );

        EvaluationResult below = NotificationForecastEvaluator.evaluateForecast(
                Collections.singletonList(list.get(0)),
                BASE_NOW_MS,
                6,
                EnumSet.of(ConditionType.WIND),
                10.0);
        assertFalse(below.isMatched());

        EvaluationResult matched = NotificationForecastEvaluator.evaluateForecast(
                list,
                BASE_NOW_MS,
                6,
                EnumSet.of(ConditionType.WIND),
                10.0);
        assertTrue(matched.isMatched());
        assertEquals(EnumSet.of(ConditionType.WIND), matched.getMatchedConditions());
        assertEquals(BASE_NOW_MS + 2 * HOUR_MS, matched.getFirstMatchTimeMs());
        assertEquals(14.2, matched.getMaxWindSpeedMs(), 0.001);
    }

    @Test
    public void isInQuietHours_overnightAndDaytime() {
        // Overnight 22:00 - 07:00
        assertTrue(NotificationForecastEvaluator.isInQuietHours(22, 22, 7));
        assertTrue(NotificationForecastEvaluator.isInQuietHours(23, 22, 7));
        assertTrue(NotificationForecastEvaluator.isInQuietHours(0, 22, 7));
        assertTrue(NotificationForecastEvaluator.isInQuietHours(6, 22, 7));
        assertFalse(NotificationForecastEvaluator.isInQuietHours(7, 22, 7));
        assertFalse(NotificationForecastEvaluator.isInQuietHours(15, 22, 7));
        assertFalse(NotificationForecastEvaluator.isInQuietHours(21, 22, 7));

        // Daytime 09:00 - 17:00
        assertTrue(NotificationForecastEvaluator.isInQuietHours(9, 9, 17));
        assertTrue(NotificationForecastEvaluator.isInQuietHours(12, 9, 17));
        assertFalse(NotificationForecastEvaluator.isInQuietHours(8, 9, 17));
        assertFalse(NotificationForecastEvaluator.isInQuietHours(17, 9, 17));

        // Disabled when start == end
        assertFalse(NotificationForecastEvaluator.isInQuietHours(12, 12, 12));
    }

    @Test
    public void isWithinCooldown_suppressesSameConditionOnlyWithinWindow() {
        Set<ConditionType> rain = EnumSet.of(ConditionType.RAIN);
        Set<ConditionType> rainAndWind = EnumSet.of(ConditionType.RAIN, ConditionType.WIND);

        long lastAlertMs = BASE_NOW_MS - 60 * MINUTE_MS;

        // Same condition within 180 min -> suppress
        assertTrue(NotificationForecastEvaluator.isWithinCooldown(
                BASE_NOW_MS, lastAlertMs, 180, rain, rain));

        // Different / escalated condition within 180 min -> do not suppress
        assertFalse(NotificationForecastEvaluator.isWithinCooldown(
                BASE_NOW_MS, lastAlertMs, 180, rainAndWind, rain));

        // Same condition after cooldown elapsed -> do not suppress
        assertFalse(NotificationForecastEvaluator.isWithinCooldown(
                BASE_NOW_MS + 180 * MINUTE_MS, lastAlertMs, 180, rain, rain));

        // Cooldown disabled (0) -> do not suppress
        assertFalse(NotificationForecastEvaluator.isWithinCooldown(
                BASE_NOW_MS, lastAlertMs, 0, rain, rain));
    }

    @Test
    public void conditionType_serializeAndDeserializeRoundTrip() {
        Set<ConditionType> original = EnumSet.of(ConditionType.RAIN, ConditionType.WIND, ConditionType.FOG);
        String serialized = ConditionType.serialize(original);
        assertEquals(original, ConditionType.deserialize(serialized));
        assertTrue(ConditionType.deserialize("").isEmpty());
    }
}
