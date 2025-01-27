package com.example.myapplication.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Activity.DetailActivity;

import com.example.myapplication.Activity.Detail_AttractionActivity;
import com.example.myapplication.Domain.ItemAttractions;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.databinding.ViewholderAttractionsBinding;


import java.util.ArrayList;
public class AttractionsAdapter extends RecyclerView.Adapter<AttractionsAdapter.Viewholder> {
    private ArrayList<ItemAttractions> currentItems; // Текущий список данных
    private ArrayList<ItemAttractions> originalItems; // Оригинальный список
    private Context context;

    public AttractionsAdapter(ArrayList<ItemAttractions> items) {
        this.originalItems = new ArrayList<>(items); // Сохраняем оригинальный список
        this.currentItems = new ArrayList<>(items); // Инициализируем текущий список
    }

    @NonNull
    @Override
    public AttractionsAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderAttractionsBinding binding = ViewholderAttractionsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AttractionsAdapter.Viewholder holder, int position) {
        ItemAttractions currentItem = currentItems.get(position);
        holder.binding.titleTxt.setText(currentItem.getTitle());
        holder.binding.adressTxt.setText(currentItem.getAddress());
        holder.binding.scoreTxt.setText(String.valueOf(currentItem.getScore()));

        Glide.with(context).load(currentItem.getPic()).into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Detail_AttractionActivity.class);
            intent.putExtra("object", currentItem);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return currentItems.size(); // Возвращаем размер текущего списка
    }

    public void filter(String query) {
        currentItems.clear();
        if (query.isEmpty()) {
            currentItems.addAll(originalItems);
        } else {
            for (ItemAttractions item : originalItems) {
                if (item.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        item.getAddress().toLowerCase().contains(query.toLowerCase())) {
                    currentItems.add(item);
                }
            }
        }
        notifyDataSetChanged(); // Обновляем RecyclerView
    }

    public static class Viewholder extends RecyclerView.ViewHolder {
        private final ViewholderAttractionsBinding binding;

        public Viewholder(ViewholderAttractionsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
