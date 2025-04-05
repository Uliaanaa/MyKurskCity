package com.example.solovinyykray.Domain;

/**
 * Класс, представляющий элемент слайдера.
 * Содержит URL изображения для отображения в слайдере.
 * Используется совместно с адаптерами для ViewPager или аналогичных компонентов.
 */

public class SliderItems {
    private String url;

    public SliderItems() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
