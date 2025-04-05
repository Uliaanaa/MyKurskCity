package com.example.solovinyykray.Adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.solovinyykray.Domain.Review;
import com.example.solovinyykray.R;
import java.util.List;

/**
 * Адаптер для отображения списка отзывов в RecyclerView.
 * Отображает информацию о пользователе, рейтинг, комментарий и время публикации.
 */
public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {
    private final List<Review> reviews;

    /**
     * Конструктор адаптера
     * @param reviews список отзывов для отображения
     */
    public ReviewsAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    /**
     * Создает новый ViewHolder для элемента списка
     * @param parent родительская ViewGroup
     * @param viewType тип представления
     * @return новый ViewHolder
     */
    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    /**
     * Привязывает данные отзыва к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position позиция в списке
     */
    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        bindReviewData(holder, review);
    }

    /**
     * Заполняет ViewHolder данными отзыва
     * @param holder ViewHolder для заполнения
     * @param review данные отзыва
     */
    private void bindReviewData(ReviewViewHolder holder, Review review) {
        holder.username.setText(review.getUsername());
        holder.ratingBar.setRating(review.getRating());
        holder.comment.setText(review.getComment());
        holder.timestamp.setText(formatTimestamp(review.getTimestamp()));
        loadProfileImage(holder, review);
    }

    /**
     * Загружает изображение профиля пользователя
     * @param holder ViewHolder содержащий ImageView
     * @param review данные отзыва с URL изображения
     */
    private void loadProfileImage(ReviewViewHolder holder, Review review) {
        if (review.getProfileImageUrl() != null && !review.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(review.getProfileImageUrl())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(R.drawable.person_icon)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.person_icon);
        }
    }

    /**
     * Форматирует timestamp в относительное время (например, "2 часа назад")
     * @param timestamp время публикации отзыва
     * @return отформатированная строка времени
     */
    private String formatTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        return DateUtils.getRelativeTimeSpanString(
                timestamp,
                now,
                DateUtils.MINUTE_IN_MILLIS
        ).toString();
    }

    /**
     * Возвращает количество отзывов в списке
     * @return количество отзывов
     */
    @Override
    public int getItemCount() {
        return reviews.size();
    }

    /**
     * ViewHolder для элементов списка отзывов
     */
    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final ImageView profileImage;
        final TextView username;
        final RatingBar ratingBar;
        final TextView comment;
        final TextView timestamp;

        /**
         * Конструктор ViewHolder
         * @param itemView корневое представление элемента
         */
        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            username = itemView.findViewById(R.id.username);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            comment = itemView.findViewById(R.id.comment);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}