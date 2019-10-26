package org.thosp.yourlocalweather;

public class MultiselectionItem {

    private String name;
    private Boolean value;

    public MultiselectionItem(String name, Boolean value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Boolean getValue() {
        return value;
    }
}
