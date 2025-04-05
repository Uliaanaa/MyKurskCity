package com.example.solovinyykray.Domain;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Парсер подробной информации о событиях с сайта для города Курска.
 * Использует библиотеку Jsoup для извлечения структурированных данных.
 */

public class KurskEventParser {

    /**
     * Класс для хранения информации о событии.
     */

    public static class Event {
        private String location;
        private String address;
        private String description;

        /**
         * Конструктор события.
         * @param location место проведения
         * @param address адрес
         * @param description описание события
         */

        public Event(String location, String address, String description) {
            this.location = location;
            this.address = address;
            this.description = description;
        }

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

    /**
     * Парсит страницу события и извлекает информацию.
     * @param url URL страницы события
     * @return объект Event или null, если не удалось распарсить
     */

    public static Event parseEventFromPage(String url) {
        Event event = null;

        try {
            Document doc = Jsoup.connect(url).get();
            System.out.println("Страница загружена: " + url);

            String eventDescription = doc.select(".SingleEvent_entity_content__1rkUT .TextContent_content__2S5Bh").text();

            String eventLocation = doc.select(".TimetableBlock_timetableBlock_placeWrapper__name__2tiDK a").first() != null ?
                    doc.select(".TimetableBlock_timetableBlock_placeWrapper__name__2tiDK a").first().text() : "";

            String eventAddress = doc.select(".TimetableBlock_timetableBlock_placeWrapper__address__2W3Uy").first() != null ?
                    doc.select(".TimetableBlock_timetableBlock_placeWrapper__address__2W3Uy").first().text() : "";

            if (!eventLocation.isEmpty() && !eventAddress.isEmpty() && !eventDescription.isEmpty()) {
                event = new Event(eventLocation, eventAddress, eventDescription);
            } else {
                System.err.println("Не удалось извлечь данные. Проверьте селекторы.");
            }

        } catch (IOException e) {
            System.err.println("Ошибка парсинга страницы " + url + ": " + e.getMessage());
        }

        return event;
    }



}
