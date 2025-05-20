package com.solovinykray.solovinyykray.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.solovinykray.solovinyykray.Domain.Attractions;
import com.solovinyykray.solovinyykray.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Адаптер для управления заявками на добавление достопримечательностей администратором.
 * Позволяет просматривать, одобрять и отклонять заявки с возможностью указания причины отклонения.
 */
public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    private List<Attractions> attractions;
    private Context context;
    private OnAttractionActionListener listener;

    /**
     * Интерфейс для обработки событий после действий с достопримечательностями
     * Вызывается после удаления достопримечательности из списка
     */
    public interface OnAttractionActionListener {
        void onAttractionRemoved();
    }

    /**
     * Конструктор адаптера
     * @param attractions список достопримечательностей для модерации
     * @param context контекст приложения
     * @param listener слушатель событий после действий
     */
    public AdminAdapter(List<Attractions> attractions, Context context, OnAttractionActionListener listener) {
        this.attractions = attractions;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_attraction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attractions attraction = attractions.get(position);
        holder.title.setText(attraction.getTitle());
        holder.address.setText(attraction.getAddress());
        holder.description.setText(attraction.getDescription());

        Glide.with(context)
                .load(attraction.getPic())
                .into(holder.imageView);

        holder.rejectComment.setVisibility(View.GONE);
        holder.rejectComment.setText("");
        holder.rejectBtn.setText("Отклонить");

        holder.approveBtn.setOnClickListener(v -> approveAttraction(attraction));
        holder.rejectBtn.setOnClickListener(v -> {
            if (holder.rejectComment.getVisibility() == View.GONE) {
                holder.rejectComment.setVisibility(View.VISIBLE);
                holder.rejectBtn.setText("Подтвердить отклонение");
            } else {
                String comment = holder.rejectComment.getText().toString().trim();
                if (comment.isEmpty()) {
                    Toast.makeText(context, "Пожалуйста, укажите причину отклонения", Toast.LENGTH_SHORT).show();
                } else {
                    rejectAttraction(attraction, comment);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return attractions.size();
    }

    /**
     * ViewHolder для отображения элементов списка заявок
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView title;
        TextView address;
        TextView description;
        Button approveBtn;
        Button rejectBtn;
        EditText rejectComment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            title = itemView.findViewById(R.id.title);
            address = itemView.findViewById(R.id.address);
            description = itemView.findViewById(R.id.description);
            approveBtn = itemView.findViewById(R.id.approveBtn);
            rejectBtn = itemView.findViewById(R.id.rejectBtn);
            rejectComment = itemView.findViewById(R.id.rejectComment);
        }
    }

    /**
     * Одобряет заявку на добавление достопримечательности
     * @param attraction объект достопримечательности для одобрения
     */
    private void approveAttraction(Attractions attraction) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String author = attraction.getAuthor();

        DatabaseReference attractionsRef = FirebaseDatabase.getInstance().getReference("Attractions");

        Map<String, Object> data = new HashMap<>();
        data.put("id", attraction.getId());
        data.put("title", attraction.getTitle());
        data.put("address", attraction.getAddress());
        data.put("description", attraction.getDescription());
        data.put("bed", attraction.getBed());
        data.put("width", attraction.getWidth());
        data.put("longitude", attraction.getLongitude());
        data.put("score", attraction.getScore());
        data.put("pic", attraction.getPic());
        data.put("author", author);
        data.put("status", "approved");

        attractionsRef.child(attraction.getId()).setValue(data)
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
     * @param attraction объект достопримечательности для отклонения
     * @param comment причина отклонения
     */
    private void rejectAttraction(Attractions attraction, String comment) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String author = attraction.getAuthor();

        DatabaseReference rejectedRef = FirebaseDatabase.getInstance().getReference("Attractions");
        Map<String, Object> data = new HashMap<>();
        data.put("id", attraction.getId());
        data.put("title", attraction.getTitle());
        data.put("address", attraction.getAddress());
        data.put("description", attraction.getDescription());
        data.put("pic", attraction.getPic());
        data.put("author", author);
        data.put("status", "rejected"); // Используем ключ "rejected" для фильтрации
        data.put("rejectComment", comment);

        rejectedRef.child(attraction.getId()).setValue(data)
                .addOnSuccessListener(unused -> {
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
                });
    }

}