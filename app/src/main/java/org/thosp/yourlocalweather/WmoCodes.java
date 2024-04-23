package org.thosp.yourlocalweather;

public enum WmoCodes {
    WMO_0(0, "Clear sky", "01d"),
    WMO_1(1, "Mainly clear", ""),
    WMO_2(2, "Partly cloudy", "02d"),
    WMO_3(3, "Overcast", "03d"),
    WMO_45(45, "Fog", "50d"),
    WMO_48(48, "Depositing rime fog", "50d"),
    WMO_51(51, "Drizzle", "09d"),
    WMO_53(53, "Light, moderate intensity", "10d"),
    WMO_55(55, "Dense intensity", "10d"),
    WMO_56(56, "Freezing Drizzle", "10d"),
    WMO_57(57, "Light and dense intensity", "10d"),
    WMO_61(61, "Rain", "10d"),
    WMO_63(63, "Slight, moderate intensity", "10d"),
    WMO_65(65, "Heavy intensity", "10d"),
    WMO_66(66, "Freezing Rain", "13d"),
    WMO_67(67, "Light and heavy intensity", "13d"),
    WMO_71(71, "Snow fall", "13d"),
    WMO_73(73, "Slight, moderate intensity", "10d"),
    WMO_75(75, "Heavy intensity", "10d"),
    WMO_77(77, "Snow grains", "10d"),
    WMO_80(80, "Rain showers", "10d"),
    WMO_81(81, "Slight, moderate intensity", "10d"),
    WMO_82(82, "Violent intensity", "10d"),
    WMO_85(85, "Snow showers slight", "10d"),
    WMO_86(86, "Snow showers heavy", "10d"),
    WMO_95(95, "Thunderstorm: Slight or moderate", "10d"),
    WMO_96(96, "Thunderstorm with slight hail", "11d"),
    WMO_99(99, "Thunderstorm with heavy hail", "11d");

    Integer id;
    String description;
    String iconId;

    WmoCodes(Integer id, String description, String iconId) {
        this.description = description;
        this.iconId = iconId;
        this.id = id;
    }

    public static WmoCodes getById(Integer id) {
        for(WmoCodes wmoCode: WmoCodes.values()) {
            if (wmoCode.id.equals(id)) {
                return wmoCode;
            }
        }
        return null;
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getIconId() {
        return iconId;
    }
}
