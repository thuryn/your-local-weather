package org.thosp.yourlocalweather;

public class MultiselectionItem {

    private String name;
    private Boolean value;
    private String address;

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
