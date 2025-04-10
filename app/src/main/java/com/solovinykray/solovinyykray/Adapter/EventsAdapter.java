package com.solovinykray.solovinyykray.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Activity.Detail_EventActivity;
import com.solovinykray.solovinyykray.Domain.KurskEventsParser;
import com.solovinyykray.solovinyykray.databinding.ViewholderEventsBinding;


import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения и фильтрации списка событий.
 * Поддерживает фильтрацию по категориям и текстовому запросу.
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.Viewholder> {
    private List<KurskEventsParser.Event> events;
    private List<KurskEventsParser.Event> originalEvents;
    private Context context;

    /**
     * Конструктор адаптера событий
     * @param events начальный список событий
     */
    public EventsAdapter(List<KurskEventsParser.Event> events) {
        this.events = new ArrayList<>(events);
        this.originalEvents = new ArrayList<>(events);
    }

    /**
     * Создает новый ViewHolder для элемента списка
     */
    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderEventsBinding binding = ViewholderEventsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new Viewholder(binding);
    }

    /**
     * Привязывает данные события к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position позиция в списке
     */
    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        KurskEventsParser.Event event = events.get(position);

        holder.binding.titleTxt.setText(event.getTitle());
        holder.binding.priceTxt.setText(event.getPrice());
        holder.binding.categoryTxt.setText(event.getCategory());
        holder.binding.dateTxt.setText(event.getDate());

        Glide.with(context)
                .load(event.getImageUrl())
                .into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Detail_EventActivity.class);
            intent.putExtra("object", event);
            context.startActivity(intent);
        });
    }

    /**
     * Возвращает количество событий в списке
     * @return количество событий
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Фильтрует события по категории
     * @param category категория для фильтрации
     */
    public void filterByCategory(String category) {
        List<KurskEventsParser.Event> filteredList = new ArrayList<>();

        for (KurskEventsParser.Event event : originalEvents) {
            if (event.getCategory().equalsIgnoreCase(category)) {
                filteredList.add(event);
            }
        }

        updateEventsList(filteredList);
    }

    /**
     * Фильтрует события по текстовому запросу
     * @param query текст для поиска (в названии или категории)
     */
    public void filter(String query) {
        List<KurskEventsParser.Event> filteredList = new ArrayList<>();

        if (query.isEmpty()) {
            filteredList.addAll(originalEvents);
        } else {
            for (KurskEventsParser.Event event : originalEvents) {
                if (event.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        event.getCategory().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(event);
                }
            }
        }

        updateEventsList(filteredList);
    }

    /**
     * Обновляет список событий и уведомляет об изменениях
     * @param newList новый список событий
     */
    private void updateEventsList(List<KurskEventsParser.Event> newList) {
        events.clear();
        events.addAll(newList);
        notifyDataSetChanged();
    }

    /**
     * ViewHolder для отображения отдельного события
     */
    public static class Viewholder extends RecyclerView.ViewHolder {
        ViewholderEventsBinding binding;

        /**
         * Конструктор ViewHolder
         * @param binding привязка данных к макету
         */
        public Viewholder(ViewholderEventsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}