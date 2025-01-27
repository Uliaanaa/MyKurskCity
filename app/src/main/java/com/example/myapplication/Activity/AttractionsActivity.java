package com.example.myapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myapplication.Adapter.AttractionsAdapter;
import com.example.myapplication.Adapter.ExplorerAdapter;
import com.example.myapplication.Domain.ItemAttractions;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityAttractionsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

public class AttractionsActivity extends BaseActivity {
    ActivityAttractionsBinding binding;
    AttractionsAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttractionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initRoute();

        binding.editTextText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                     adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });



        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if(id==R.id.home){
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);

                }if(id==R.id.explorer){
                    intent = new Intent(getApplicationContext(), ExplorerActivity.class);
                    startActivity(intent);

                }if(id==R.id.profile){
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);

                }if(id==R.id.cart){
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);

                }
            }

        });
    }

    private void initRoute() {
        DatabaseReference myRef = database.getReference("Attractions");
        binding.progressBarAttractions.setVisibility(View.VISIBLE);

        ArrayList<ItemAttractions> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemAttractions item = issue.getValue(ItemAttractions.class);
                        list.add(item);
                        Log.d("Firebase", "Item: " + item.getTitle());
                    }
                    if (!list.isEmpty()) {
                        binding.RecyclerViewAttractions.setLayoutManager(new LinearLayoutManager(AttractionsActivity.this, LinearLayoutManager.VERTICAL, false));
                        adapter = new AttractionsAdapter(list);
                        binding.RecyclerViewAttractions.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }
                    binding.progressBarAttractions.setVisibility(View.GONE);
                    binding.RecyclerViewAttractions.setVisibility(View.VISIBLE);
                } else {
                    Log.d("Firebase", "No data found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        });
    }

}