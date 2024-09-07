package org.thosp.yourlocalweather;

public class MultiselectionLocationItem {

    private final Long id;
    private final String name;
    private final Boolean value;

    public MultiselectionLocationItem(Long id, String name, Boolean value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Boolean getValue() {
        return value;
    }

    public Long getId() {
        return id;
    }
}
