package com.example.solovinyykray.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.solovinyykray.Domain.Category;
import com.example.solovinyykray.R;
import com.example.solovinyykray.databinding.ViewholderCategoryBinding;

import java.util.List;

/**
 * Адаптер для отображения списка категорий в горизонтальном RecyclerView.
 * Поддерживает выбор категории с автоматической прокруткой для лучшей видимости
 * и уведомляет слушателя о выбранной категории.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Viewholder> {
    private final List<Category> items;
    private final OnCategoryClickListener listener;
    private int selectedPosition = -1;
    private int lastSelectedPosition = -1;
    private Context context;
    private RecyclerView recyclerView;

    /**
     * Интерфейс для обработки кликов по категориям
     */
    public interface OnCategoryClickListener {
        /**
         * Вызывается при выборе категории
         * @param category название выбранной категории
         */
        void onCategoryClick(String category);
    }

    /**
     * Конструктор адаптера
     * @param items список категорий для отображения
     * @param listener слушатель кликов по категориям
     * @param recyclerView RecyclerView, к которому привязан адаптер
     */
    public CategoryAdapter(List<Category> items, OnCategoryClickListener listener, RecyclerView recyclerView) {
        this.items = items;
        this.listener = listener;
        this.recyclerView = recyclerView;
    }

    /**
     * Создает новый ViewHolder
     */
    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderCategoryBinding binding = ViewholderCategoryBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new Viewholder(binding);
    }

    /**
     * Привязывает данные категории к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position позиция категории в списке
     */
    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        Category item = items.get(position);
        holder.binding.title.setText(item.getName());

        Glide.with(holder.itemView.getContext())
                .load(item.getImagePath())
                .into(holder.binding.pic);

        holder.binding.getRoot().setOnClickListener(v -> handleCategoryClick(position, item));

        updateCategoryAppearance(holder, position);
    }

    /**
     * Обрабатывает клик по категории
     * @param position позиция выбранной категории
     * @param item выбранная категория
     */
    private void handleCategoryClick(int position, Category item) {
        lastSelectedPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(lastSelectedPosition);
        notifyItemChanged(selectedPosition);

        if (listener != null) {
            listener.onCategoryClick(item.getName());
        }

        adjustScrollPosition(position);
    }

    /**
     * Настраивает положение прокрутки для лучшей видимости выбранной категории
     * @param position позиция выбранной категории
     */
    private void adjustScrollPosition(int position) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
            int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();

            if (position <= firstVisiblePosition + 1) {
                recyclerView.smoothScrollToPosition(Math.max(position - 1, 0));
            } else if (position >= lastVisiblePosition - 1) {
                recyclerView.smoothScrollToPosition(Math.min(position + 1, items.size() - 1));
            }
        }
    }

    /**
     * Обновляет внешний вид категории в зависимости от выбора
     * @param holder ViewHolder категории
     * @param position позиция категории
     */
    private void updateCategoryAppearance(Viewholder holder, int position) {
        holder.binding.title.setTextColor(context.getResources().getColor(R.color.white));

        if (selectedPosition == position) {
            holder.binding.pic.setBackgroundResource(0);
            holder.binding.mainLayout.setBackgroundResource(R.drawable.blue_bg);
            holder.binding.title.setVisibility(View.VISIBLE);
        } else {
            holder.binding.pic.setBackgroundResource(R.drawable.grey_bg);
            holder.binding.mainLayout.setBackgroundResource(0);
            holder.binding.title.setVisibility(View.GONE);
        }
    }

    /**
     * Возвращает количество категорий
     * @return количество элементов в списке
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder для отображения отдельной категории
     */
    public static class Viewholder extends RecyclerView.ViewHolder {
        private final ViewholderCategoryBinding binding;

        public Viewholder(ViewholderCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}