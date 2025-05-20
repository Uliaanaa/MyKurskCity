package com.solovinykray.solovinyykray.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Domain.Attractions;
import com.solovinyykray.solovinyykray.R;

import java.util.List;

/**
 * Адаптер для отображения списка заявок на достопримечательности в RecyclerView.
 * Показывает название, статус, комментарий (если есть) и изображение.
 */
public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {

    private List<Attractions> requestsList;
    private Context context;

    /**
     * Конструктор адаптера
     *
     * @param requestsList список достопримечательностей для отображения
     */
    public RequestsAdapter(List<Attractions> requestsList) {
        this.requestsList = requestsList;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        Attractions request = requestsList.get(position);

        holder.title.setText(request.getTitle());
        holder.status.setText(getStatusText(request.getStatus()));

        if (request.getRejectComment() != null && !request.getRejectComment().isEmpty()) {
            holder.comment.setText("Комментарий: " + request.getRejectComment());
            holder.comment.setVisibility(View.VISIBLE);
        } else {
            holder.comment.setVisibility(View.GONE);
        }
        holder.imageView.setVisibility(View.VISIBLE);
        Glide.with(context)
                .load(request.getPic())
                .into(holder.imageView);
    }

    /**
     * Преобразует статус из английского в русский для отображения
     *
     * @param status статус на английском ("approved", "rejected" или другой)
     * @return локализованный статус на русском
     */
    private String getStatusText(String status) {
        switch (status) {
            case "approved": return "Одобрено";
            case "rejected": return "Отклонено";
            default: return "На рассмотрении";
        }
    }

    @Override
    public int getItemCount() {
        return requestsList.size();
    }

    /**
     * ViewHolder для элементов списка заявок.
     * Содержит views для отображения названия, статуса, комментария и изображения.
     */
    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView title, status, comment;
        ImageView imageView;

        /**
         * Конструктор ViewHolder
         *
         * @param itemView корневое view элемента списка
         */
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.titleTxt);
            status = itemView.findViewById(R.id.priceTxt);
            comment = itemView.findViewById(R.id.dateTxt);
            imageView = itemView.findViewById(R.id.imageViewSelected);
        }
    }
}