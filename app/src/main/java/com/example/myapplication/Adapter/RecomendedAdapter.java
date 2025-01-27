package com.example.myapplication.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Activity.DetailActivity;
import com.example.myapplication.Activity.Detail_AttractionActivity;
import com.example.myapplication.Domain.ItemAttractions;
import com.example.myapplication.Domain.ItemDomain;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.databinding.ViewholderRecomendedBinding;

import java.util.ArrayList;

public class RecomendedAdapter extends RecyclerView.Adapter<RecomendedAdapter.Viewholder> {
    ArrayList<ItemAttractions> items;
    @SuppressLint("RestrictedApi")
    Context context;
    ViewholderRecomendedBinding binding;

    public RecomendedAdapter(ArrayList<ItemAttractions> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public RecomendedAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding=ViewholderRecomendedBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        context=parent.getContext();
        return new Viewholder(binding);

    }

    @Override
    public void onBindViewHolder(@NonNull RecomendedAdapter.Viewholder holder, int position) {
    binding.titleTxt.setText(items.get(position).getTitle());
    binding.adressTxt.setText(items.get(position).getAddress());
    binding.scoreTxt.setText(""+items.get(position).getScore());

        Glide.with(context)
                .load(items.get(position).getPic())
                .into(binding.pic);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Detail_AttractionActivity.class);
                intent.putExtra("object", items.get(position));
                context.startActivity(intent);

            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        public Viewholder(ViewholderRecomendedBinding binding) {
            super(binding.getRoot());
        }
    }
}
