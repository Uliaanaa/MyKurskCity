package com.example.myapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.Adapter.CategoryAdapter;
import com.example.myapplication.Adapter.EventsAdapter;
import com.example.myapplication.Domain.Category;
import com.example.myapplication.Domain.KurskEventsParser;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityEventBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

public class EventActivity extends BaseActivity {
    private ActivityEventBinding binding;
    private EventsAdapter adapter;
    private ArrayList<KurskEventsParser.Event> eventList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initEvents();
        initCategory();
        setupSearchListener(); // Подключаем поиск

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(id -> {
            Intent intent;
            if (id == R.id.explorer) {
                intent = new Intent(getApplicationContext(), ExplorerActivity.class);
            } else if (id == R.id.attractions) {
                intent = new Intent(getApplicationContext(), AttractionsActivity.class);
            } else if (id == R.id.profile) {
                intent = new Intent(getApplicationContext(), ProfileActivity.class);
            } else if (id == R.id.cart) {
                intent = new Intent(getApplicationContext(), FavoritesActivity.class);
            } else {
                intent = new Intent(getApplicationContext(), MainActivity.class);
            }
            startActivity(intent);
        });
    }

    private void initEvents() {
        binding.progressBarEvent.setVisibility(View.VISIBLE);

        new Thread(() -> {
            eventList.addAll(KurskEventsParser.parseEvents("https://welcomekursk.ru/events", 100));

            runOnUiThread(() -> {
                if (!eventList.isEmpty()) {
                    binding.RecyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
                    adapter = new EventsAdapter(eventList);
                    binding.RecyclerViewEvents.setAdapter(adapter);
                }
                binding.progressBarEvent.setVisibility(View.GONE);
            });
        }).start();
    }

    private void initCategory() {
        DatabaseReference myRef = database.getReference("Category");
        binding.progressBarCategory.setVisibility(View.VISIBLE);
        ArrayList<Category> categoryList = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        categoryList.add(issue.getValue(Category.class));
                    }
                    if (!categoryList.isEmpty()) {
                        binding.recyclerViewCategory.setLayoutManager(new LinearLayoutManager(EventActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        CategoryAdapter adapter = new CategoryAdapter(categoryList, new CategoryAdapter.OnCategoryClickListener() {
                            @Override
                            public void onCategoryClick(String category) {
                                filterEventsByCategory(category);
                            }
                        });
                        binding.recyclerViewCategory.setAdapter(adapter);
                    }
                    binding.progressBarCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void filterEventsByCategory(String category) {
        // Используем метод адаптера для фильтрации по категории
        if (adapter != null) {
            adapter.filterByCategory(category);
        }
    }


    private void setupSearchListener() {
        binding.editTextText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString()); // Фильтруем список мероприятий
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}