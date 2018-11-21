package org.thosp.yourlocalweather.utils;

import java.util.HashMap;
import java.util.Map;

public enum OWMLanguages {

    Arabic("ar"),
    Bulgarian("bg"),
    Catalan("ca"),
    Czech("cz", "cs"),
    German("de"),
    Greek("el"),
    English("en"),
    Persian_Farsi("fa"),
    Finnish("fi"),
    French("fr"),
    Galician("gl"),
    Croatian("hr"),
    Hungarian("hu"),
    Italian("it"),
    Japanese("ja"),
    Korean("kr"),
    Latvian("la"),
    Lithuanian("lt"),
    Macedonian("mk"),
    Dutch("nl"),
    Polish("pl"),
    Portuguese("pt"),
    Romanian("ro"),
    Russian("ru"),
    Swedish("se"),
    Slovak("sk"),
    Slovenian("sl"),
    Spanish("es"),
    Turkish("tr"),
    Ukrainian("ua", "uk"),
    Vietnamese("vi"),
    Chinese("zh_cn", "zh"),
    Chinese_Simplified("zh_cn", "zh-rCN"),
    Chinese_Traditional("zh_tw", "zh-rTW");

    String owmLanguage;
    String javaLanguage;
    boolean translatedByResources;
    static Map<String, OWMLanguages> javaToOwmLanguages;

    OWMLanguages(String owmLocale) {
        this.owmLanguage = owmLocale;
        this.javaLanguage = owmLanguage;
    }

    OWMLanguages(String owmLocale, boolean translatedByResources) {
        this.owmLanguage = owmLocale;
        this.javaLanguage = owmLanguage;
        this.translatedByResources = translatedByResources;
    }

    OWMLanguages(String owmLocale, String javaLocale) {
        this.owmLanguage = owmLocale;
        this.javaLanguage = javaLocale;
    }

    public static String getOwmLanguage(String javaLanguage) {
        OWMLanguages currentOwmLanguage = getOwmLanguageFromMap(javaLanguage);
        return (currentOwmLanguage != null)?currentOwmLanguage.owmLanguage:"en";
    }

    public static boolean isLanguageSupportedByOWMAndNotTranslatedLocaly(String javaLanguage) {
        OWMLanguages currentOwmLanguage = getOwmLanguageFromMap(javaLanguage);
        return (currentOwmLanguage != null) && !currentOwmLanguage.translatedByResources;
    }

    private static OWMLanguages getOwmLanguageFromMap(String javaLanguage) {
        if (javaToOwmLanguages == null) {
            javaToOwmLanguages = new HashMap<>();
            for (OWMLanguages language: values()) {
                javaToOwmLanguages.put(language.javaLanguage, language);
            }
        }
        return javaToOwmLanguages.get(javaLanguage);
    }
}

