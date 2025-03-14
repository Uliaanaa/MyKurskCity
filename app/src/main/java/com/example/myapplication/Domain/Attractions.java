package com.example.myapplication.Domain;

public class Attractions {
    private String id; // Уникальный идентификатор достопримечательности
    private String title; // Название
    private String address; // Адрес
    private String description; // Описание
    private String bed; // Тип достопримечательности
    private String width; // Широта (ранее latitude)
    private String longitude; // Долгота
    private int score; // Рейтинг
    private String pic; // Ссылка на изображение
    private String status; // Статус заявки (pending, approved, rejected)

    // Пустой конструктор для Firebase
    public Attractions() {
    }

    // Конструктор для создания новой заявки
    public Attractions(String title, String address, String description, String bed, String width, String longitude, int score, String pic, String status) {
        this.title = title;
        this.address = address;
        this.description = description;
        this.bed = bed;
        this.width = width;
        this.longitude = longitude;
        this.score = score; // Рейтинг по умолчанию
        this.pic = pic;
        this.status = status;
    }

    // Конструктор для одобренной достопримечательности
    public Attractions(String id, String title, String address, String description, String bed, String width, String longitude, int score, String pic) {
        this.id = id;
        this.title = title;
        this.address = address;
        this.description = description;
        this.bed = bed;
        this.width = width;
        this.longitude = longitude;
        this.score = score;
        this.pic = pic;
        this.status = "approved"; // Статус по умолчанию для одобренных
    }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBed() {
        return bed;
    }

    public void setBed(String bed) {
        this.bed = bed;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}