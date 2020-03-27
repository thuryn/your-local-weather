package org.thosp.yourlocalweather.licence;

public class LicenseNotValidException extends Exception {

    String errorMessage;

    public LicenseNotValidException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
