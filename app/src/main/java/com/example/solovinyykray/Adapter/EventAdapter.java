package com.example.solovinyykray.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.solovinyykray.Activity.Detail_EventActivity;
import com.example.solovinyykray.Domain.KurskEventsParser.Event;
import com.example.solovinyykray.databinding.ViewholderEventBinding;
import java.util.List;

/**
 * Адаптер для отображения списка событий в RecyclerView.
 * Обеспечивает отображение информации о событиях и обработку кликов для перехода к деталям события.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.Viewholder> {
    private List<Event> events;
    private Context context;

    /**
     * Конструктор адаптера событий
     * @param events список событий для отображения
     */
    public EventAdapter(List<Event> events) {
        this.events = events;
    }

    /**
     * Создает новый ViewHolder для элемента списка событий
     * @param parent родительская ViewGroup
     * @param viewType тип View
     * @return новый ViewHolder
     */
    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderEventBinding binding = ViewholderEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new Viewholder(binding);
    }

    /**
     * Привязывает данные события к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position позиция события в списке
     */
    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        Event event = events.get(position);

        holder.binding.titleTxt.setText(event.getTitle());
        holder.binding.priceTxt.setText(event.getPrice());
        holder.binding.categoryTxt.setText(event.getCategory());
        holder.binding.dateTxt.setText(event.getDate());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Detail_EventActivity.class);
            intent.putExtra("object", event);
            context.startActivity(intent);
        });

        Glide.with(context)
                .load(event.getImageUrl())
                .into(holder.binding.pic);
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
     * Обновляет список событий
     * @param newList новый список событий
     */
    public void updateList(List<Event> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder для отображения отдельного события
     */
    public static class Viewholder extends RecyclerView.ViewHolder {
        ViewholderEventBinding binding;

        /**
         * Конструктор ViewHolder
         * @param binding привязка данных к макету элемента события
         */
        public Viewholder(ViewholderEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}