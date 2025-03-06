package com.example.myapplication.Adapter;

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
import com.example.myapplication.Domain.Review;
import com.example.myapplication.R;
import java.util.List;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {
    private List<Review> reviews;

    public ReviewsAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);

        // Устанавливаем ник пользователя
        holder.username.setText(review.getUsername());

        // Устанавливаем рейтинг
        holder.ratingBar.setRating(review.getRating());

        // Устанавливаем комментарий
        holder.comment.setText(review.getComment());

        // Устанавливаем время отзыва
        long timestamp = review.getTimestamp();
        String timeAgo = getTimeAgo(timestamp); // Форматируем время
        holder.timestamp.setText(timeAgo);

        // Загружаем аватарку с помощью Glide
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

    // Метод для форматирования времени
    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        return DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS).toString();
    }



    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username;
        RatingBar ratingBar;
        TextView comment;
        TextView timestamp; // Время отзыва

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            username = itemView.findViewById(R.id.username);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            comment = itemView.findViewById(R.id.comment);
            timestamp = itemView.findViewById(R.id.timestamp); // Инициализация времени
        }
    }
}