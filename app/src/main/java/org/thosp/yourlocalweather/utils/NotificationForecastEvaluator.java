package org.thosp.yourlocalweather.utils;

import org.thosp.yourlocalweather.model.DetailedWeatherForecast;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Pure evaluator for forecast-based conditional notifications (Issue #109).
 */
public final class NotificationForecastEvaluator {

    private static final long MILLIS_PER_HOUR = 3_600_000L;
    private static final long MILLIS_PER_MINUTE = 60_000L;

    public enum ConditionType {
        RAIN("rain"),
        SNOW("snow"),
        THUNDERSTORM("thunderstorm"),
        FOG("fog"),
        WIND("wind");

        private final String key;

        ConditionType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static ConditionType fromKey(String key) {
            if (key == null) {
                return null;
            }
            for (ConditionType type : values()) {
                if (type.key.equals(key)) {
                    return type;
                }
            }
            return null;
        }

        public static Set<ConditionType> fromKeys(Set<String> keys) {
            if (keys == null || keys.isEmpty()) {
                return EnumSet.noneOf(ConditionType.class);
            }
            Set<ConditionType> result = EnumSet.noneOf(ConditionType.class);
            for (String key : keys) {
                ConditionType type = fromKey(key);
                if (type != null) {
                    result.add(type);
                }
            }
            return result;
        }

        public static String serialize(Set<ConditionType> conditions) {
            if (conditions == null || conditions.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (ConditionType type : values()) {
                if (conditions.contains(type)) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(type.key);
                }
            }
            return sb.toString();
        }

        public static Set<ConditionType> deserialize(String serialized) {
            if (serialized == null || serialized.trim().isEmpty()) {
                return EnumSet.noneOf(ConditionType.class);
            }
            Set<ConditionType> result = EnumSet.noneOf(ConditionType.class);
            for (String part : serialized.split(",")) {
                ConditionType type = fromKey(part.trim());
                if (type != null) {
                    result.add(type);
                }
            }
            return result;
        }
    }

    public static final class EvaluationResult {
        private final boolean matched;
        private final Set<ConditionType> matchedConditions;
        private final long firstMatchTimeMs;
        private final double maxWindSpeedMs;

        public EvaluationResult(
                boolean matched,
                Set<ConditionType> matchedConditions,
                long firstMatchTimeMs,
                double maxWindSpeedMs) {
            this.matched = matched;
            this.matchedConditions = Collections.unmodifiableSet(matchedConditions);
            this.firstMatchTimeMs = firstMatchTimeMs;
            this.maxWindSpeedMs = maxWindSpeedMs;
        }

        public static EvaluationResult noMatch() {
            return new EvaluationResult(false, EnumSet.noneOf(ConditionType.class), 0L, 0.0);
        }

        public boolean isMatched() {
            return matched;
        }

        public Set<ConditionType> getMatchedConditions() {
            return matchedConditions;
        }

        public long getFirstMatchTimeMs() {
            return firstMatchTimeMs;
        }

        public double getMaxWindSpeedMs() {
            return maxWindSpeedMs;
        }
    }

    private NotificationForecastEvaluator() {
    }

    /**
     * Evaluates whether any forecast entry within [nowMs, nowMs + lookaheadHours]
     * matches at least one of the enabled conditions.
     */
    public static EvaluationResult evaluateForecast(
            List<DetailedWeatherForecast> forecastList,
            long nowMs,
            int lookaheadHours,
            Set<ConditionType> enabledConditions,
            double windThresholdMs) {
        if (forecastList == null || forecastList.isEmpty()
                || enabledConditions == null || enabledConditions.isEmpty()
                || lookaheadHours <= 0) {
            return EvaluationResult.noMatch();
        }

        long windowEndMs = nowMs + (lookaheadHours * MILLIS_PER_HOUR);
        Set<ConditionType> matchedConditions = EnumSet.noneOf(ConditionType.class);
        long firstMatchTimeMs = 0L;
        double maxWindSpeedMs = 0.0;

        for (DetailedWeatherForecast entry : forecastList) {
            if (entry == null) {
                continue;
            }
            long entryTimeMs = entry.getDateTime() * 1000L;
            if (entryTimeMs < nowMs || entryTimeMs > windowEndMs) {
                continue;
            }

            boolean entryMatched = false;
            int weatherId = entry.getWeatherId();

            if (enabledConditions.contains(ConditionType.RAIN)
                    && (entry.getRain() > 0 || Utils.isWeatherDescriptionWithRain(weatherId))) {
                matchedConditions.add(ConditionType.RAIN);
                entryMatched = true;
            }
            if (enabledConditions.contains(ConditionType.SNOW)
                    && (entry.getSnow() > 0 || Utils.isWeatherDescriptionWithSnow(weatherId))) {
                matchedConditions.add(ConditionType.SNOW);
                entryMatched = true;
            }
            if (enabledConditions.contains(ConditionType.THUNDERSTORM)
                    && Utils.isWeatherDescriptionWithThunderstorm(weatherId)) {
                matchedConditions.add(ConditionType.THUNDERSTORM);
                entryMatched = true;
            }
            if (enabledConditions.contains(ConditionType.FOG)
                    && Utils.isWeatherDescriptionWithFog(weatherId)) {
                matchedConditions.add(ConditionType.FOG);
                entryMatched = true;
            }
            if (enabledConditions.contains(ConditionType.WIND)
                    && entry.getWindSpeed() >= windThresholdMs) {
                matchedConditions.add(ConditionType.WIND);
                if (entry.getWindSpeed() > maxWindSpeedMs) {
                    maxWindSpeedMs = entry.getWindSpeed();
                }
                entryMatched = true;
            }

            if (entryMatched && (firstMatchTimeMs == 0L || entryTimeMs < firstMatchTimeMs)) {
                firstMatchTimeMs = entryTimeMs;
            }
        }

        if (matchedConditions.isEmpty()) {
            return EvaluationResult.noMatch();
        }
        return new EvaluationResult(true, matchedConditions, firstMatchTimeMs, maxWindSpeedMs);
    }

    /**
     * Checks whether currentHour (0-23) falls within [startHour, endHour).
     * Supports overnight intervals such as 22..7 as well as daytime intervals such as 9..17.
     */
    public static boolean isInQuietHours(int currentHour, int startHour, int endHour) {
        if (startHour == endHour) {
            return false;
        }
        if (startHour < endHour) {
            return currentHour >= startHour && currentHour < endHour;
        }
        return currentHour >= startHour || currentHour < endHour;
    }

    /**
     * Returns true when a notification should be suppressed because the same conditions
     * were already alerted within cooldownMinutes. If conditions changed, does not suppress.
     */
    public static boolean isWithinCooldown(
            long nowMs,
            long lastAlertTimeMs,
            int cooldownMinutes,
            Set<ConditionType> currentConditions,
            Set<ConditionType> lastAlertConditions) {
        if (cooldownMinutes <= 0 || lastAlertTimeMs <= 0L) {
            return false;
        }
        if (nowMs < lastAlertTimeMs) {
            return false;
        }
        long elapsedMs = nowMs - lastAlertTimeMs;
        if (elapsedMs >= cooldownMinutes * MILLIS_PER_MINUTE) {
            return false;
        }
        if (lastAlertConditions == null || lastAlertConditions.isEmpty() || currentConditions == null) {
            return false;
        }
        return currentConditions.equals(lastAlertConditions);
    }
}
