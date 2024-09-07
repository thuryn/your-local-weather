package org.thosp.yourlocalweather;

public class MultiselectionItem {

    private final String name;
    private final Boolean value;
    private final String address;

    public MultiselectionItem(String name, String address, Boolean value) {
        this.name = name;
        this.value = value;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public Boolean getValue() {
        return value;
    }

    public String getAddress() {
        return address;
    }
}
