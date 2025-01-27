package com.example.myapplication.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.Adapter.AttractionsAdapter;
import com.example.myapplication.Domain.ItemAttractions;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityAttractionsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FavoritesActivity extends BaseActivity {
    ActivityAttractionsBinding binding;
    AttractionsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttractionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initFavorites();

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if (id == R.id.home) {
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
                if (id == R.id.explorer) {
                    intent = new Intent(getApplicationContext(), ExplorerActivity.class);
                    startActivity(intent);
                }
                if (id == R.id.profile) {
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);
                }if(id==R.id.attractions){
                    intent = new Intent(getApplicationContext(), AttractionsActivity.class);
                    startActivity(intent);

                }
            }
        });
    }

    private void initFavorites() {
        binding.progressBarAttractions.setVisibility(View.VISIBLE);

        ArrayList<ItemAttractions> favoriteList = new ArrayList<>();

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Attractions");

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemAttractions item = issue.getValue(ItemAttractions.class);
                        if (item != null && isFavorite(item.getTitle())) {
                            favoriteList.add(item);
                        }
                    }
                    if (!favoriteList.isEmpty()) {
                        binding.RecyclerViewAttractions.setLayoutManager(new LinearLayoutManager(FavoritesActivity.this, LinearLayoutManager.VERTICAL, false));
                        adapter = new AttractionsAdapter(favoriteList);
                        binding.RecyclerViewAttractions.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("Favorites", "No favorites found.");
                    }
                }
                binding.progressBarAttractions.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        });
    }

    private boolean isFavorite(String title) {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        return prefs.getBoolean("favorite_" + title, false);
    }

}
