package com.example.myapplication.Adapter;

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
import com.example.myapplication.Domain.Attractions;
import com.example.myapplication.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    private List<Attractions> attractions;
    private Context context;

    public AdminAdapter(List<Attractions> attractions, Context context) {
        this.attractions = attractions;
        this.context = context;
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


        // Одобрить заявку
        holder.approveBtn.setOnClickListener(v -> {
            approveAttraction(attraction);
        });

        // Отклонить заявку
        holder.rejectBtn.setOnClickListener(v -> {
            rejectAttraction(attraction);
        });
    }

    @Override
    public int getItemCount() {
        return attractions.size();
    }

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

    private void approveAttraction(Attractions attraction) {
        // Переносим достопримечательность в коллекцию Attractions
        DatabaseReference attractionsRef = FirebaseDatabase.getInstance().getReference("Attractions");
        attractionsRef.child(attraction.getId()).setValue(attraction)
                .addOnSuccessListener(aVoid -> {
                    // Удаляем заявку из PendingAttractions
                    DatabaseReference pendingRef = FirebaseDatabase.getInstance().getReference("PendingAttractions");
                    pendingRef.child(attraction.getId()).removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(context, "Заявка одобрена", Toast.LENGTH_SHORT).show();
                                attractions.remove(attraction);
                                notifyDataSetChanged();
                            });
                });
    }

    private void rejectAttraction(Attractions attraction) {
        // Удаляем заявку из PendingAttractions
        DatabaseReference pendingRef = FirebaseDatabase.getInstance().getReference("PendingAttractions");
        pendingRef.child(attraction.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Заявка отклонена", Toast.LENGTH_SHORT).show();
                    attractions.remove(attraction);
                    notifyDataSetChanged();
                });
    }
}