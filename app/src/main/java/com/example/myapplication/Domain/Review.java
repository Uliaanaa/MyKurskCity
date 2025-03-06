package com.example.myapplication.Domain;

public class Review {
    private String userId;
    private String productId;
    private int rating;
    private String comment;
    private long timestamp;
    private String username; // Ник пользователя
    private String profileImageUrl; // URL аватарки

    public Review() {
        // Пустой конструктор для Firebase
    }

    public Review(String userId, String productId, int rating, String comment, long timestamp) {
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}