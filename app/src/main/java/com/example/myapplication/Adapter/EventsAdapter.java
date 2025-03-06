package com.example.myapplication.Adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Activity.Detail_EventActivity;
import com.example.myapplication.Domain.KurskEventParser;
import com.example.myapplication.Domain.KurskEventsParser;
import com.example.myapplication.databinding.ViewholderEventsBinding;

import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.Viewholder> {
    private List<KurskEventsParser.Event> events;
    private List<KurskEventsParser.Event> originalEvents;
    private Context context;

    public EventsAdapter(List<KurskEventsParser.Event> events) {
        this.events = new ArrayList<>(events);
        this.originalEvents = new ArrayList<>(events); // Сохраняем оригинальный список
    }

    @NonNull
    @Override
    public EventsAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderEventsBinding binding = ViewholderEventsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new EventsAdapter.Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventsAdapter.Viewholder holder, int position) {
        KurskEventsParser.Event event = events.get(position);

        // Устанавливаем данные
        holder.binding.titleTxt.setText(event.getTitle());
        holder.binding.priceTxt.setText(event.getPrice());
        holder.binding.categoryTxt.setText(event.getCategory());
        holder.binding.dateTxt.setText(event.getDate());

        // Загружаем изображение
        Glide.with(context)
                .load(event.getImageUrl())
                .into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Detail_EventActivity.class);
            intent.putExtra("object", event); // Передаем конкретный объект Event
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class Viewholder extends RecyclerView.ViewHolder {
        ViewholderEventsBinding binding;

        public Viewholder(ViewholderEventsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public void filterByCategory(String category) {
        List<KurskEventsParser.Event> filteredList = new ArrayList<>();

        // Фильтруем по категории
        for (KurskEventsParser.Event event : originalEvents) {
            if (event.getCategory().equalsIgnoreCase(category)) {
                filteredList.add(event);
            }
        }

        // Обновляем список событий и уведомляем об изменениях
        events.clear();
        events.addAll(filteredList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        events.clear();
        if (query.isEmpty()) {
            events.addAll(originalEvents);
        } else {
            for (KurskEventsParser.Event event : originalEvents) {
                if (event.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        event.getCategory().toLowerCase().contains(query.toLowerCase())) {
                    events.add(event);
                }
            }
        }
        notifyDataSetChanged();
    }
}
