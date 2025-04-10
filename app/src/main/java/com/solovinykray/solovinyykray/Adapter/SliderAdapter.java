package com.solovinykray.solovinyykray.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Domain.SliderItems;
import com.solovinyykray.solovinyykray.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения слайдера изображений в ViewPager2.
 * Поддерживает бесконечную прокрутку слайдов.
 */
public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {
    private final List<SliderItems> sliderItems;
    private final ViewPager2 viewPager2;
    private Context context;

    /**
     * Конструктор адаптера слайдера
     * @param sliderItems список элементов слайдера
     * @param viewPager2 ViewPager2 для отображения слайдов
     */
    public SliderAdapter(ArrayList<SliderItems> sliderItems, ViewPager2 viewPager2) {
        this.sliderItems = new ArrayList<>(sliderItems);
        this.viewPager2 = viewPager2;
    }

    /**
     * Создает новый ViewHolder для элемента слайдера
     * @param parent родительская ViewGroup
     * @param viewType тип представления
     * @return новый ViewHolder
     */
    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.slider_item_container, parent, false);
        return new SliderViewHolder(view);
    }

    /**
     * Привязывает данные к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position позиция в списке
     */
    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        holder.setImage(sliderItems.get(position));

        // Активируем бесконечную прокрутку при достижении предпоследнего элемента
        if (position == sliderItems.size() - 2) {
            viewPager2.post(this::updateSliderForInfiniteScroll);
        }
    }

    /**
     * Обновляет слайдер для бесконечной прокрутки
     */
    private void updateSliderForInfiniteScroll() {
        sliderItems.addAll(sliderItems);
        notifyDataSetChanged();
    }

    /**
     * Возвращает количество элементов в слайдере
     * @return количество элементов
     */
    @Override
    public int getItemCount() {
        return sliderItems.size();
    }

    /**
     * ViewHolder для элементов слайдера
     */
    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        /**
         * Конструктор ViewHolder
         * @param itemView корневое представление элемента
         */
        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageSlide);
        }

        /**
         * Устанавливает изображение в ImageView
         * @param sliderItems элемент слайдера с URL изображения
         */
        void setImage(SliderItems sliderItems) {
            Glide.with(itemView.getContext())
                    .load(sliderItems.getUrl())
                    .into(imageView);
        }
    }
}