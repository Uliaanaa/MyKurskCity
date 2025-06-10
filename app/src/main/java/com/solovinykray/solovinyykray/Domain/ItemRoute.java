package com.solovinykray.solovinyykray.Domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс представляет маршрут в приложении, содержащий информацию о туре, такую как название,
 * адрес, описание, цена, гид, координаты и мультимедийные данные. Реализует интерфейс
 * {@link Serializable} для передачи объектов между активностями или сохранения состояния.
 */
public class ItemRoute implements Serializable {
    private String id;
    private String title;
    private String address;
    private String description;
    private String detail;
    private String pic;
    private String duration;
    private String timeTour;
    private String dateTour;
    private String tourGuideName;
    private String tourGuidePhone;
    private String tourGuidePic;
    private int price;
    private String bed;
    private String distance;
    private double score;
    private List<List<Double>> points;
    private String audioToken;
    private String videoUrl;

    public ItemRoute() {
        this.points = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getAudioToken() {
        return audioToken;
    }

    public void setAudioToken(String audioToken) {
        this.audioToken = audioToken;
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

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTimeTour() {
        return timeTour;
    }

    public void setTimeTour(String timeTour) {
        this.timeTour = timeTour;
    }

    public String getDateTour() {
        return dateTour;
    }

    public void setDateTour(String dateTour) {
        this.dateTour = dateTour;
    }

    public String getTourGuideName() {
        return tourGuideName;
    }

    public void setTourGuideName(String tourGuideName) {
        this.tourGuideName = tourGuideName;
    }

    public String getTourGuidePhone() {
        return tourGuidePhone;
    }

    public void setTourGuidePhone(String tourGuidePhone) {
        this.tourGuidePhone = tourGuidePhone;
    }

    public String getTourGuidePic() {
        return tourGuidePic;
    }

    public void setTourGuidePic(String tourGuidePic) {
        this.tourGuidePic = tourGuidePic;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getBed() {
        return bed;
    }

    public void setBed(String bed) {
        this.bed = bed;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public List<List<Double>> getPoints() {
        return points;
    }

    public void setPoints(List<List<Double>> points) {
        this.points = points != null ? points : new ArrayList<>();
    }

    public void addCoordinate(double latitude, double longitude) {
        this.points.add(List.of(latitude, longitude));
    }
}