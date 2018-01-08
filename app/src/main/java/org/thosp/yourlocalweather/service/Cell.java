package org.thosp.yourlocalweather.service;

public class Cell {

    public Cell() {

    }

    public int cellId;
    public int area;
    public int mcc;
    public int mnc;
    public int technology;
    public int psc;
    public int signal;

    public String toString() {
        return mcc + "|" + mnc + "|" + area + "|" + cellId + "|" + technology;
    }
}
