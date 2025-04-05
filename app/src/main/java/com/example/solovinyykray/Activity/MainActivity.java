package com.example.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;

import com.example.solovinyykray.Adapter.CategoryAdapter;
import com.example.solovinyykray.Adapter.EventAdapter;
import com.example.solovinyykray.Adapter.PopularAdapter;
import com.example.solovinyykray.Adapter.RecomendedAdapter;
import com.example.solovinyykray.Adapter.SliderAdapter;
import com.example.solovinyykray.Domain.Category;
import com.example.solovinyykray.Domain.ItemAttractions;
import com.example.solovinyykray.Domain.ItemRoute;
import com.example.solovinyykray.Domain.KurskEventsParser;
import com.example.solovinyykray.Domain.SliderItems;
import com.example.solovinyykray.R;
import com.example.solovinyykray.databinding.ActivityMainBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Главная активность приложения, содержащая основные разделы:
 * - Баннеры (слайдер)
 * - Категории мероприятий
 * - Популярные маршруты
 * - Рекомендуемые достопримечательности
 * - Ближайшие мероприятия
 * А также предоставляет навигацию по основным разделам приложения.
 */

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private EventAdapter eventAdapter;
    private List<KurskEventsParser.Event> allEventsList = new ArrayList<>();

    /**
     * Инициализирует интерфейс,
     * загружает данные и настраивает обработчики событий.
     * @param savedInstanceState Сохраненное состояние активности, если оно существует
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initCategory();
        initEvents();
        initPopular();
        initRecomended();
        initBanner();
        enableImmersiveMode();

        TextView routeButton = findViewById(R.id.route_btn);
        TextView attractionsButton = findViewById(R.id.attractions_btn);
        TextView eventButton = findViewById(R.id.event_btn);


        routeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExplorerActivity.class);
                startActivity(intent);
            }
        });

        attractionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AttractionsActivity.class);
                startActivity(intent);
            }
        });

        eventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EventActivity.class);
                startActivity(intent);
            }
        });


        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if (id == R.id.explorer) {
                    intent = new Intent(MainActivity.this, ExplorerActivity.class);
                    startActivity(intent);

                }
                if (id == R.id.attractions) {
                    intent = new Intent(getApplicationContext(), AttractionsActivity.class);
                    startActivity(intent);

                }
                if (id == R.id.profile) {
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);
                }
                if (id == R.id.cart) {
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);

                }
            }
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
     * Загружает и отображает популярные маршруты (топ-5 по рейтингу).
     */

    private void initPopular() {
        DatabaseReference routesRef = database.getReference("Route");
        DatabaseReference reviewsRef = database.getReference("reviews");
        binding.progressBarPopular.setVisibility(View.VISIBLE);
        ArrayList<ItemRoute> list = new ArrayList<>();

        routesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot issue : snapshot.getChildren()) {
                    ItemRoute item = issue.getValue(ItemRoute.class);
                    if (item != null) {
                        list.add(item);
                    }
                }

                reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                        for (ItemRoute route : list) {
                            double totalRating = 0;
                            int reviewCount = 0;

                            for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                                String productId = review.child("productId").getValue(String.class);
                                Double rating = review.child("rating").getValue(Double.class);

                                if (productId != null && productId.equals(route.getTitle()) && rating != null) {
                                    totalRating += rating;
                                    reviewCount++;
                                }
                            }

                            double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                            route.setScore(averageRating);
                        }

                        Collections.sort(list, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));

                        ArrayList<ItemRoute> topRatedList = new ArrayList<>();
                        int limit = Math.min(list.size(), 5);
                        for (int i = 0; i < limit; i++) {
                            topRatedList.add(list.get(i));
                        }

                        if (!topRatedList.isEmpty()) {
                            binding.recyclerViewPopular.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                            RecyclerView.Adapter adapter = new PopularAdapter(topRatedList);
                            binding.recyclerViewPopular.setAdapter(adapter);
                        }
                        binding.progressBarPopular.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Ошибка загрузки отзывов: " + error.getMessage());
                        binding.progressBarPopular.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки маршрутов: " + error.getMessage());
                binding.progressBarPopular.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Настраивает слайдер баннеров.
     * @param items Список элементов для слайдера
     */


    private void banners(ArrayList<SliderItems> items) {
        binding.viewPagerSlider.setAdapter(new SliderAdapter(items, binding.viewPagerSlider));
        binding.viewPagerSlider.setClipToPadding(false);
        binding.viewPagerSlider.setClipChildren(false);
        binding.viewPagerSlider.setOffscreenPageLimit(3);
        binding.viewPagerSlider.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        binding.viewPagerSlider.setPageTransformer(compositePageTransformer);
    }

    /**
     * Загружает баннеры из Firebase Database.
     */

    private void initBanner() {
        DatabaseReference myRef = database.getReference("Banner");
        binding.progressBarBanner.setVisibility(RecyclerView.VISIBLE);
        ArrayList<SliderItems> items = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        items.add(issue.getValue(SliderItems.class));
                    }
                    banners(items);
                    binding.progressBarBanner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * Загружает и отображает популярные достопримечательности (топ-5 по рейтингу).
     */

    private void initRecomended() {
        DatabaseReference attractionsRef = database.getReference("Attractions");
        DatabaseReference reviewsRef = database.getReference("reviews");
        binding.progressBarRecomended.setVisibility(View.VISIBLE);
        ArrayList<ItemAttractions> list = new ArrayList<>();

        attractionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot issue : snapshot.getChildren()) {
                    ItemAttractions item = issue.getValue(ItemAttractions.class);
                    if (item != null) {
                        item.setId(issue.getKey());
                        list.add(item);
                    }
                }

                reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                        for (ItemAttractions attraction : list) {
                            double totalRating = 0;
                            int reviewCount = 0;

                            for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                                String productId = review.child("productId").getValue(String.class);
                                Double rating = review.child("rating").getValue(Double.class);

                                if (productId != null && productId.equals(attraction.getTitle()) && rating != null) {
                                    totalRating += rating;
                                    reviewCount++;
                                }
                            }

                            double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                            attraction.setScore(averageRating);
                        }

                        Collections.sort(list, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));

                        ArrayList<ItemAttractions> topRatedList = new ArrayList<>();
                        int limit = Math.min(list.size(), 5);
                        for (int i = 0; i < limit; i++) {
                            topRatedList.add(list.get(i));
                        }

                        if (!topRatedList.isEmpty()) {
                            binding.recyclerViewRecomended.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                            RecyclerView.Adapter adapter = new RecomendedAdapter(topRatedList);
                            binding.recyclerViewRecomended.setAdapter(adapter);
                        }
                        binding.progressBarRecomended.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Ошибка загрузки отзывов: " + error.getMessage());
                        binding.progressBarRecomended.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки достопримечательностей: " + error.getMessage());
                binding.progressBarRecomended.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Загружает категории мероприятий из Firebase Database.
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
                        binding.recyclerViewCategory.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
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
     * Загружает список мероприятий с сайта.
     */

    private void initEvents() {
        binding.progressBarEvent.setVisibility(View.VISIBLE);

        new Thread(() -> {
            allEventsList = KurskEventsParser.parseEvents("https://welcomekursk.ru/events", 15);

            runOnUiThread(() -> {
                if (!allEventsList.isEmpty()) {
                    binding.recyclerViewEvent.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    eventAdapter = new EventAdapter(allEventsList);
                    binding.recyclerViewEvent.setAdapter(eventAdapter);
                }
                binding.progressBarEvent.setVisibility(View.GONE);
            });
        }).start();
    }

    /**
     * Фильтрует мероприятия по выбранной категории.
     * @param category категория выбранная пользователем.
     */

    private void filterEventsByCategory(String category) {
        List<KurskEventsParser.Event> filteredList = new ArrayList<>();

        for (KurskEventsParser.Event event : allEventsList) {
            if (event.getCategory().equalsIgnoreCase(category)) {
                filteredList.add(event);
            }
        }

        eventAdapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            binding.tvNoEventsFound.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoEventsFound.setVisibility(View.GONE);
        }
    }


}
