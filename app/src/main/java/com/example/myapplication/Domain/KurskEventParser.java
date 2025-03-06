package com.example.myapplication.Domain;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class KurskEventParser {

    // Класс для хранения информации о событии
    public static class Event {
        private String location;
        private String address;
        private String description;

        // Конструктор
        public Event(String location, String address, String description) {
            this.location = location;
            this.address = address;
            this.description = description;
        }

        // Getters
        public String getLocation() { return location; }
        public String getAddress() { return address; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return "Event{" +
                    "location='" + location + '\'' +
                    ", address='" + address + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }

    public static Event parseEventFromPage(String url) {
        Event event = null;

        try {
            // Загружаем документ по URL
            Document doc = Jsoup.connect(url).get();
            System.out.println("Страница загружена: " + url); // Проверка загрузки страницы

            // Извлекаем описание события
            String eventDescription = doc.select(".SingleEvent_entity_content__1rkUT .TextContent_content__2S5Bh").text();

            // Получение первого найденного элемента для места и адреса
            String eventLocation = doc.select(".TimetableBlock_timetableBlock_placeWrapper__name__2tiDK a").first() != null ?
                    doc.select(".TimetableBlock_timetableBlock_placeWrapper__name__2tiDK a").first().text() : "";

            String eventAddress = doc.select(".TimetableBlock_timetableBlock_placeWrapper__address__2W3Uy").first() != null ?
                    doc.select(".TimetableBlock_timetableBlock_placeWrapper__address__2W3Uy").first().text() : "";

            // Проверка на пустые значения
            if (!eventLocation.isEmpty() && !eventAddress.isEmpty() && !eventDescription.isEmpty()) {
                event = new Event(eventLocation, eventAddress, eventDescription);
            } else {
                System.err.println("Не удалось извлечь данные. Проверьте селекторы."); // Для отладки
            }

        } catch (IOException e) {
            System.err.println("Ошибка парсинга страницы " + url + ": " + e.getMessage());
        }

        return event;
    }



}
