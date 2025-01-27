package com.example.myapplication.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Activity.DetailActivity;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.databinding.ViewholderExplorerBinding;

import java.util.ArrayList;

public class ExplorerAdapter extends RecyclerView.Adapter<ExplorerAdapter.Viewholder> {
    private final ArrayList<ItemRoute> originalItems; // Оригинальный список данных
    private final ArrayList<ItemRoute> currentItems;  // Текущий отображаемый список
    private Context context;

    // Конструктор
    public ExplorerAdapter(ArrayList<ItemRoute> items) {
        this.originalItems = new ArrayList<>(items); // Сохраняем оригинальные данные
        this.currentItems = new ArrayList<>(items);  // Инициализируем текущие данные
    }

    @NonNull
    @Override
    public ExplorerAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderExplorerBinding binding = ViewholderExplorerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExplorerAdapter.Viewholder holder, int position) {
        ItemRoute currentItem = currentItems.get(position);

        holder.binding.titleTxt.setText(currentItem.getTitle());
        holder.binding.adressTxt.setText(currentItem.getAddress());
        holder.binding.scoreTxt.setText(String.valueOf(currentItem.getScore()));

        Glide.with(context)
                .load(currentItem.getPic())
                .into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("object", currentItem);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return currentItems.size();
    }

    // Метод фильтрации
    public void filter(String query) {
        currentItems.clear();
        if (query.isEmpty()) {
            currentItems.addAll(originalItems); // Восстанавливаем полный список
        } else {
            for (ItemRoute item : originalItems) {
                if (item.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        item.getAddress().toLowerCase().contains(query.toLowerCase())) {
                    currentItems.add(item);
                }
            }
        }
        notifyDataSetChanged(); // Уведомляем об изменениях
    }

    // ViewHolder для RecyclerView
    public static class Viewholder extends RecyclerView.ViewHolder {
        private final ViewholderExplorerBinding binding;

        public Viewholder(ViewholderExplorerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
