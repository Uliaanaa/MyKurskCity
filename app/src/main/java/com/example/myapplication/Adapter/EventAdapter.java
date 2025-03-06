package com.example.myapplication.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.Activity.Detail_EventActivity;
import com.example.myapplication.Domain.KurskEventParser;
import com.example.myapplication.Domain.KurskEventsParser;
import com.example.myapplication.Domain.KurskEventsParser.Event;
import com.example.myapplication.databinding.ViewholderEventBinding;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.Viewholder> {
    private List<Event> events;
    private Context context;
    private KurskEventParser.Event kurskEvent;

    public EventAdapter(List<Event> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderEventBinding binding = ViewholderEventBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        Event event = events.get(position);



        // Устанавливаем заголовок и цену
        holder.binding.titleTxt.setText(event.getTitle());
        holder.binding.priceTxt.setText(event.getPrice());
        holder.binding.categoryTxt.setText(event.getCategory());
        holder.binding.dateTxt.setText(event.getDate());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Detail_EventActivity.class);
            intent.putExtra("object", event); // Передаем конкретный объект Event
            context.startActivity(intent);
        });


        // Загружаем изображение
        Glide.with(context)
                .load(event.getImageUrl())
                .into(holder.binding.pic);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateList(List<KurskEventsParser.Event> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    public static class Viewholder extends RecyclerView.ViewHolder {
        ViewholderEventBinding binding;

        public Viewholder(ViewholderEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
