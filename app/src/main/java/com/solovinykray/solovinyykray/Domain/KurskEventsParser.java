package com.solovinykray.solovinyykray.Domain;

import android.os.Parcel;
import android.os.Parcelable;

import com.bumptech.glide.Glide;
import com.solovinyykray.solovinyykray.databinding.ActivityDetailEventBinding;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Парсер событий с сайта welcomekursk.ru.
 * Извлекает информацию о событиях и поддерживает пагинацию.
 */

public class KurskEventsParser {

    /**
     * Класс, представляющий событие с возможностью сериализации (Parcelable).
     * Содержит основную информацию о событии: название, дату, цену, категорию,
     * ссылку и URL изображения.
     */

    public static class Event implements Parcelable {
        private String category;
        private String date;
        private String title;
        private String price;
        private String link;
        private String imageUrl;

        public String getCategory() { return category; }
        public String getTitle() { return title; }
        public String getDate() { return date; }
        public String getPrice() { return price; }
        public String getLink() { return link; }
        public String getImageUrl() { return imageUrl; }

        public Event(String category, String date, String title, String price, String link, String imageUrl) {
            this.category = category;
            this.date = date;
            this.title = title;
            this.price = price;
            this.link = link;
            this.imageUrl = imageUrl;
        }

        protected Event(Parcel in) {
            category = in.readString();
            date = in.readString();
            title = in.readString();
            price = in.readString();
            link = in.readString();
            imageUrl = in.readString();
        }

        public static final Creator<Event> CREATOR = new Creator<Event>() {
            @Override
            public Event createFromParcel(Parcel in) { return new Event(in); }

            @Override
            public Event[] newArray(int size) { return new Event[size]; }
        };

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(category);
            dest.writeString(date);
            dest.writeString(title);
            dest.writeString(price);
            dest.writeString(link);
            dest.writeString(imageUrl);
        }

        @Override
        public String toString() {
            return "Event{" +
                    "category='" + category + '\'' +
                    ", date='" + date + '\'' +
                    ", title='" + title + '\'' +
                    ", price='" + price + '\'' +
                    ", link='" + link + '\'' +
                    ", imageUrl='" + imageUrl + '\'' +
                    '}';
        }
    }

    /**
     * Парсит список событий с указанной страницы.
     * @param url URL страницы для парсинга
     * @return список объектов Event, может быть пустым
     */

    public static List<Event> parseEventsFromPage(String url) {
        List<Event> events = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url).get();
            Elements eventElements = doc.select(".EventCard_wrapper__18Uhg");

            for (Element eventElement : eventElements) {
                String category = eventElement.select(".EventCard_badge__CNhie").text();
                String date = eventElement.select(".EventCard_date__3kT5M").text();
                String title = eventElement.select(".EventCard_title__1eRmT").text();
                String price = eventElement.select(".EventCard_price__PtHTE").text();

                String link = "";
                Element parentLink = eventElement.parent();
                if (parentLink != null && parentLink.tagName().equals("a") && parentLink.hasAttr("href")) {
                    link = parentLink.attr("abs:href");
                }
                if (link.isEmpty() || !link.startsWith("http")) {
                    link = "https://welcomekursk.ru" + link;
                }

                String imageUrl = eventElement.select(".Picture_image__3LsT8 img").attr("srcset");
                if (!imageUrl.isEmpty()) {
                    imageUrl = imageUrl.split(",")[0].trim().split(" ")[0];
                } else {
                    imageUrl = eventElement.select(".Picture_image__3LsT8 img").attr("src");
                }
                if (!imageUrl.isEmpty() && !imageUrl.startsWith("http")) {
                    imageUrl = "https://welcomekursk.ru" + imageUrl;
                }

                events.add(new Event(category, date, title, price, link, imageUrl));
            }
        } catch (IOException e) {
            System.err.println("Ошибка парсинга страницы " + url + ": " + e.getMessage());
        }
        return events;
    }

    /**
     * Парсит события с учетом пагинации.
     * @param baseUrl базовый URL (без параметров пагинации)
     * @param totalEvents желаемое количество событий
     * @return список событий (может содержать меньше элементов, чем запрошено)
     */

    public static List<Event> parseEvents(String baseUrl, int totalEvents) {
        List<Event> allEvents = new ArrayList<>();
        int offset = 0;
        int eventsPerPage = 20;

        while (allEvents.size() < totalEvents) {
            String urlWithOffset = baseUrl + "?offset=" + offset;
            System.out.println("Загрузка: " + urlWithOffset);
            List<Event> pageEvents = parseEventsFromPage(urlWithOffset);
            if (pageEvents.isEmpty()) {
                break;
            }
            allEvents.addAll(pageEvents);
            offset += eventsPerPage;
        }

        if (allEvents.size() > totalEvents) {
            return allEvents.subList(0, totalEvents);
        }
        return allEvents;
    }

    /**
     * Привязывает данные события к элементам интерфейса.
     * @param binding объект binding для доступа к View
     * @param event событие для отображения (может быть null)
     */

    public static void setEventData(ActivityDetailEventBinding binding, Event event) {
        if (event != null) {
            binding.titleTxt.setText(event.getTitle());
            binding.distanceTxt.setText(event.getPrice());
            binding.bedTxt.setText(event.getCategory());
            binding.durationTxt.setText(event.getDate());
            Glide.with(binding.getRoot().getContext())
                    .load(event.getImageUrl())
                    .into(binding.pic);
        } else {
            binding.titleTxt.setText("Данные не найдены");
            binding.distanceTxt.setText("Добавьте событие для отображения.");
            binding.bedTxt.setText("");
            binding.durationTxt.setText("");
            binding.pic.setImageDrawable(null);
        }
    }

    /**
     * Точка входа для тестирования парсера (не используется в Android приложении).
     * @param args аргументы командной строки (не используются)
     */

    public static void main(String[] args) {
        String baseUrl = "https://welcomekursk.ru/events";
        int desiredEventsCount = 100;
        List<Event> events = parseEvents(baseUrl, desiredEventsCount);

        for (Event event : events) {
            System.out.println(event);
        }
    }
}
