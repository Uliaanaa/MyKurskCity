package com.example.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.solovinyykray.Adapter.CategoryAdapter;
import com.example.solovinyykray.Adapter.EventsAdapter;
import com.example.solovinyykray.Domain.Category;
import com.example.solovinyykray.Domain.KurskEventsParser;
import com.example.solovinyykray.R;
import com.example.solovinyykray.databinding.ActivityEventBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

/**
 * Активность для отображения списка событий. Позволяет просматривать мероприятия,
 * фильтровать их по категориям и выполнять поиск.
 * Также активность включает в себя нижнее навигационное меню для перехода к другим экранам.
 */
public class EventActivity extends BaseActivity {
    private ActivityEventBinding binding;
    private EventsAdapter adapter;
    private ArrayList<KurskEventsParser.Event> eventList = new ArrayList<>();

    /**
     * Инициализирует активность, загружает список категорий и настраивает поиск и навигацию
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initEvents();
        initCategory();
        setupSearchListener();
        enableImmersiveMode();

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

    /**
     * Скрывает системную навигацию (включает иммерсивный режим).
     */

    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Загружает список событий с сайта в фоновом режиме.
     * После загружки обновляет RecyclerView.
     */


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

    /**
     * Загружает список категорий из Firebase, настраивает горизонтальный
     * RecyclerView для их отображения.
     */

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
                        }, binding.recyclerViewCategory);
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

    /**
     * Фильтрует события по категориям.
     * @param category выбранная пользовтелем категория.
     */

    private void filterEventsByCategory(String category) {
        if (adapter != null) {
            adapter.filterByCategory(category);
        }
    }

    /**
     * Настраивает слушатель текста для реализации поиска событий
     * в реальном времени.
     */

    private void setupSearchListener() {
        binding.editTextText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}