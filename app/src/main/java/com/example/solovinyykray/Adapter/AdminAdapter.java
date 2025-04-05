package com.example.solovinyykray.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.solovinyykray.Domain.Attractions;
import com.example.solovinyykray.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

/**
 * Адаптер для отображения и управления заявками на достопримечательности в RecyclerView.
 * Предоставляет администратору возможность одобрять или отклонять заявки,
 * перенося одобренные достопримечательности в основную коллекцию и удаляя отклоненные.
 */

 public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    private List<Attractions> attractions;
    private Context context;
    private OnAttractionActionListener listener;

    /**
     * Интерфейс для обработки действий с достопримечательностями
     */

    public interface OnAttractionActionListener {
        void onAttractionRemoved();
    }

    /**
     * Конструктор адаптера
     * @param attractions список заявок на достопримечательности
     * @param context контекст приложения
     * @param listener слушатель действий с достопримечательностями
     */

    public AdminAdapter(List<Attractions> attractions, Context context, OnAttractionActionListener listener) {
        this.attractions = attractions;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Создает новый ViewHolder при необходимости
     */

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_attraction, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Привязывает данные о достопримечательности к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position позиция в списке
     */

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attractions attraction = attractions.get(position);
        holder.title.setText(attraction.getTitle());
        holder.address.setText(attraction.getAddress());
        holder.description.setText(attraction.getDescription());

            Glide.with(context)
                    .load(attraction.getPic())
                    .into(holder.imageView);


        holder.approveBtn.setOnClickListener(v -> {
            approveAttraction(attraction);
        });

        holder.rejectBtn.setOnClickListener(v -> {
            rejectAttraction(attraction);
        });
    }

    /**
     * Возвращает общее количество элементов в списке
     * @return количество заявок
     */

    @Override
    public int getItemCount() {
        return attractions.size();
    }

    /**
     * ViewHolder для отображения данных о достопримечательности
     */

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView title, address, description;
        Button approveBtn, rejectBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            title = itemView.findViewById(R.id.title);
            address = itemView.findViewById(R.id.address);
            description = itemView.findViewById(R.id.description);
            approveBtn = itemView.findViewById(R.id.approveBtn);
            rejectBtn = itemView.findViewById(R.id.rejectBtn);
        }
    }

    /**
     * Одобряет заявку на добавление достопримечательности
     * Переносит достопримечательность в основную коллекцию и удаляет из ожидающих
     * @param attraction достопримечательность для одобрения
     */

    private void approveAttraction(Attractions attraction) {
        DatabaseReference attractionsRef = FirebaseDatabase.getInstance().getReference("Attractions");
        attractionsRef.child(attraction.getId()).setValue(attraction)
                .addOnSuccessListener(aVoid -> {
                    DatabaseReference pendingRef = FirebaseDatabase.getInstance().getReference("PendingAttractions");
                    pendingRef.child(attraction.getId()).removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(context, "Заявка одобрена", Toast.LENGTH_SHORT).show();
                                attractions.remove(attraction);
                                notifyDataSetChanged();

                                if (listener != null) {
                                    listener.onAttractionRemoved();
                                }
                            });
                });
    }

    /**
     * Отклоняет заявку на добавление достопримечательности
     * Удаляет достопримечательность из списка ожидающих
     * @param attraction достопримечательность для отклонения
     */

    private void rejectAttraction(Attractions attraction) {
        DatabaseReference pendingRef = FirebaseDatabase.getInstance().getReference("PendingAttractions");
        pendingRef.child(attraction.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Заявка отклонена", Toast.LENGTH_SHORT).show();
                    attractions.remove(attraction);
                    notifyDataSetChanged();

                    if (listener != null) {
                        listener.onAttractionRemoved();
                    }
                });
    }
}