package com.solovinykray.solovinyykray.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Activity.AttractionsActivity;
import com.solovinykray.solovinyykray.Activity.Detail_AttractionActivity;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.solovinyykray.solovinyykray.databinding.ViewholderAttractionsBinding;


import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения списка достопримечательностей в RecyclerView.
 * Поддерживает два режима работы: обычный режим просмотра и режим выбора элементов.
 * В обычном режиме при клике открывается экран с деталями достопримечательности.
 * В режиме выбора (активируется долгим нажатием) можно отмечать несколько элементов
 * для последующего построения маршрута.
 */

public class AttractionsAdapter extends RecyclerView.Adapter<AttractionsAdapter.Viewholder> {
    private ArrayList<ItemAttractions> currentItems;
    private ArrayList<ItemAttractions> originalItems;
    private boolean isSelectable = false;
    private Context context;

    /**
     * Конструктор адаптера
     * @param items начальный список достопримечательностей
     */

    public AttractionsAdapter(ArrayList<ItemAttractions> items) {
        this.originalItems = new ArrayList<>(items);
        this.currentItems = new ArrayList<>(items);
    }


    /**
     * Устанавливает режим выбора элементов
     * @param isSelectable true - включить режим выбора, false - выключить
     */

    public void setSelectable(boolean isSelectable) {
        this.isSelectable = isSelectable;
        notifyDataSetChanged();
    }

    /**
     * Возвращает список выбранных достопримечательностей
     * @return список выбранных элементов
     */

    public List<ItemAttractions> getSelectedAttractions() {
        List<ItemAttractions> selectedItems = new ArrayList<>();
        for (ItemAttractions item : currentItems) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    /**
     * Создает новый ViewHolder при необходимости
     * @param parent родительская ViewGroup
     * @param viewType тип View
     * @return новый ViewHolder
     */

    @NonNull
    @Override
    public AttractionsAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderAttractionsBinding binding = ViewholderAttractionsBinding.inflate(
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
    public void onBindViewHolder(@NonNull AttractionsAdapter.Viewholder holder, int position) {
        ItemAttractions currentItem = currentItems.get(position);

        if (currentItem == null) {
            Log.e("AttractionsAdapter", "currentItem is null");
            return;
        }

        holder.binding.titleTxt.setText(currentItem.getTitle());
        holder.binding.adressTxt.setText(currentItem.getAddress());

        if (currentItem.getScore() > 0) {
            holder.binding.scoreTxt.setText(String.format("%.1f", currentItem.getScore()));
            holder.binding.imageView6.setVisibility(View.VISIBLE);
        } else {
            holder.binding.scoreTxt.setText("Нет отзывов"); // Если рейтинг равен 0
            holder.binding.imageView6.setVisibility(View.GONE);
        }

        Glide.with(context).load(currentItem.getPic()).into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            if (isSelectable) {

                currentItem.setSelected(!currentItem.isSelected());
                notifyItemChanged(position);

                boolean hasSelectedItems = false;
                for (ItemAttractions item : currentItems) {
                    if (item.isSelected()) {
                        hasSelectedItems = true;
                        break;
                    }
                }

                if (!hasSelectedItems) {
                    isSelectable = false;
                    notifyDataSetChanged();
                    if (context instanceof AttractionsActivity) {
                        ((AttractionsActivity) context).hideRouteButton();
                        ((AttractionsActivity) context).showFormButton();
                    }
                }
            } else {
                Intent intent = new Intent(context, Detail_AttractionActivity.class);
                intent.putExtra("object", currentItem);
                context.startActivity(intent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectable) {
                isSelectable = true;
                currentItem.setSelected(true);
                notifyDataSetChanged();
                if (context instanceof AttractionsActivity) {
                    ((AttractionsActivity) context).showRouteButton();
                    ((AttractionsActivity) context).hideFormButton();
                }
            }
            return true;
        });

        if (isSelectable) {
            holder.binding.checkBox.setVisibility(View.VISIBLE);
            holder.binding.checkBox.setChecked(currentItem.isSelected());
        } else {
            holder.binding.checkBox.setVisibility(View.GONE);
        }
    }

    /**
     * Возвращает количество элементов в списке
     * @return количество элементов
     */

    @Override
    public int getItemCount() {
        return currentItems.size();
    }

    /**
     * Фильтрует список по заданному запросу
     * @param query строка для поиска (по названию или адресу)
     */

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
        notifyDataSetChanged();
    }

    /**
     * ViewHolder для отображения элемента списка достопримечательностей
     */

    public static class Viewholder extends RecyclerView.ViewHolder {
        private final ViewholderAttractionsBinding binding;

        public Viewholder(ViewholderAttractionsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}