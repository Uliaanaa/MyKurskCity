package com.solovinykray.solovinyykray.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Activity.DetailActivity;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.solovinyykray.solovinyykray.databinding.ViewholderPopularBinding;


import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения популярных маршрутов.
 * Обеспечивает отображение основных данных и обработку кликов для перехода к детальной информации.
 */
public class PopularAdapter extends RecyclerView.Adapter<PopularAdapter.Viewholder> {
    private final List<ItemRoute> items;
    private Context context;

    /**
     * Конструктор адаптера
     * @param items список популярных маршрутов/достопримечательностей
     */
    public PopularAdapter(ArrayList<ItemRoute> items) {
        this.items = new ArrayList<>(items);
    }

    /**
     * Создает новый ViewHolder для элемента списка
     * @param parent родительская ViewGroup
     * @param viewType тип View
     * @return новый ViewHolder
     */
    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderPopularBinding binding = ViewholderPopularBinding.inflate(
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
        ItemRoute item = items.get(position);
        holder.binding.titleTxt.setText(item.getTitle());
        holder.binding.adressTxt.setText(item.getAddress());
        holder.binding.scoreTxt.setText(String.valueOf(item.getScore()));

        Glide.with(context)
                .load(item.getPic())
                .into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("object", item);
            context.startActivity(intent);
        });
    }

    /**
     * Возвращает количество элементов в списке
     * @return количество элементов
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder для отображения отдельного популярного маршрута
     */
    public static class Viewholder extends RecyclerView.ViewHolder {
        private final ViewholderPopularBinding binding;

        /**
         * Конструктор ViewHolder
         * @param binding привязка данных к макету
         */
        public Viewholder(ViewholderPopularBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}