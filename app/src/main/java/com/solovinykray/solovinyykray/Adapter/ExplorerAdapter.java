package com.solovinykray.solovinyykray.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Activity.DetailActivity;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.solovinyykray.solovinyykray.databinding.ViewholderExplorerBinding;


import java.util.ArrayList;

/**
 * Адаптер для отображения списка маршрутов/достопримечательностей в режиме исследователя.
 * Поддерживает фильтрацию по названию и адресу.
 */
public class ExplorerAdapter extends RecyclerView.Adapter<ExplorerAdapter.Viewholder> {
    private final ArrayList<ItemRoute> originalItems;
    private final ArrayList<ItemRoute> currentItems;
    private Context context;

    /**
     * Конструктор адаптера
     * @param items начальный список маршрутов/достопримечательностей
     */
    public ExplorerAdapter(ArrayList<ItemRoute> items) {
        this.originalItems = new ArrayList<>(items);
        this.currentItems = new ArrayList<>(items);
    }

    /**
     * Создает новый ViewHolder для элемента списка
     */
    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderExplorerBinding binding = ViewholderExplorerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new Viewholder(binding);
    }

    /**
     * Привязывает данные маршрута к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position позиция в списке
     */
    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        ItemRoute currentItem = currentItems.get(position);

        holder.binding.titleTxt.setText(currentItem.getTitle());
        holder.binding.adressTxt.setText(currentItem.getAddress());

        if (currentItem.getScore() > 0) {
            holder.binding.scoreTxt.setText(String.format("%.1f", currentItem.getScore()));
            holder.binding.imageView6.setVisibility(View.VISIBLE);
        } else {
            holder.binding.scoreTxt.setText("Нет отзывов");
            holder.binding.imageView6.setVisibility(View.GONE);
        }

        Glide.with(context).load(currentItem.getPic()).into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("object", currentItem);
            context.startActivity(intent);
        });
    }

    /**
     * Возвращает количество элементов в текущем списке
     * @return количество элементов
     */
    @Override
    public int getItemCount() {
        return currentItems.size();
    }

    /**
     * Фильтрует список по текстовому запросу
     * @param query текст для поиска (в названии или адресе)
     */
    public void filter(String query) {
        currentItems.clear();
        if (query.isEmpty()) {
            currentItems.addAll(originalItems);
        } else {
            for (ItemRoute item : originalItems) {
                if (item.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        item.getAddress().toLowerCase().contains(query.toLowerCase())) {
                    currentItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * ViewHolder для отображения отдельного маршрута/достопримечательности
     */
    public static class Viewholder extends RecyclerView.ViewHolder {
        private final ViewholderExplorerBinding binding;

        /**
         * Конструктор ViewHolder
         * @param binding привязка данных к макету
         */
        public Viewholder(ViewholderExplorerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}