package com.solovinykray.solovinyykray.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Activity.Detail_AttractionActivity;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.solovinyykray.solovinyykray.databinding.ViewholderRecomendedBinding;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Адаптер для отображения рекомендуемых достопримечательностей в RecyclerView.
 * Обеспечивает отображение информации и обработку нажатий для перехода к деталям.
 */
public class RecomendedAdapter extends RecyclerView.Adapter<RecomendedAdapter.Viewholder> {
    private final List<ItemAttractions> items;
    private Context context;

    /**
     * Конструктор адаптера
     * @param items список рекомендуемых достопримечательностей
     */
    public RecomendedAdapter(ArrayList<ItemAttractions> items) {
        this.items = new ArrayList<>(items); // Защитная копия списка
    }

    /**
     * Создает новый ViewHolder
     * @param parent родительская ViewGroup
     * @param viewType тип представления
     * @return новый экземпляр ViewHolder
     */
    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderRecomendedBinding binding = ViewholderRecomendedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new Viewholder(binding);
    }

    /**
     * Привязывает данные к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position позиция в списке
     */
    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        ItemAttractions item = items.get(position);
        holder.binding.titleTxt.setText(item.getTitle());
        holder.binding.adressTxt.setText(item.getAddress());

        if (item.getScore() > 0) {
            holder.binding.scoreTxt.setText(String.format(Locale.US, "%.1f", item.getScore()));
        } else {
            holder.binding.scoreTxt.setText("Нет отзывов");
        }

        Glide.with(context)
                .load(item.getPic())
                .into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Detail_AttractionActivity.class);
            intent.putExtra("object", item);
            context.startActivity(intent);
        });
    }

    /**
     * Возвращает общее количество элементов
     * @return количество достопримечательностей
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder для хранения представлений элемента списка
     */
    public static class Viewholder extends RecyclerView.ViewHolder {
        private final ViewholderRecomendedBinding binding;

        /**
         * Конструктор ViewHolder
         * @param binding привязка данных к макету
         */
        public Viewholder(ViewholderRecomendedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}