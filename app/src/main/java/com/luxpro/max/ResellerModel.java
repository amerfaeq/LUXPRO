package com.luxpro.max;

public class ResellerModel {
    private String name;
    private String telegram;
    private String image;
    private String country;

    public ResellerModel() {
        // Required for Firebase
    }

    public ResellerModel(String name, String telegram, String image, String country) {
        this.name = name;
        this.telegram = telegram;
        this.image = image;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelegram() {
        return telegram;
    }

    public void setTelegram(String telegram) {
        this.telegram = telegram;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}