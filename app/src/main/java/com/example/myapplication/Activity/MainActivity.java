package com.example.myapplication.Activity;

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

import com.example.myapplication.Adapter.CategoryAdapter;
import com.example.myapplication.Adapter.EventAdapter;
import com.example.myapplication.Adapter.PopularAdapter;
import com.example.myapplication.Adapter.RecomendedAdapter;
import com.example.myapplication.Adapter.SliderAdapter;
import com.example.myapplication.Domain.Category;
import com.example.myapplication.Domain.ItemAttractions;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.Domain.KurskEventsParser;
import com.example.myapplication.Domain.SliderItems;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private EventAdapter eventAdapter;
    private List<KurskEventsParser.Event> allEventsList = new ArrayList<>();

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

        TextView routeButton = findViewById(R.id.route_btn);
        TextView attractionsButton = findViewById(R.id.attractions_btn);
        TextView eventButton = findViewById(R.id.event_btn);


        routeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExplorerActivity.class); // Укажите целевую активность
                startActivity(intent);
            }
        });

        attractionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AttractionsActivity.class); // Укажите целевую активность
                startActivity(intent);
            }
        });

        eventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EventActivity.class); // Укажите целевую активность
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

                // Загружаем отзывы для расчета рейтинга
                reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                        for (ItemRoute route : list) {
                            double totalRating = 0;
                            int reviewCount = 0;

                            // Проходим по всем отзывам
                            for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                                String productId = review.child("productId").getValue(String.class);
                                Double rating = review.child("rating").getValue(Double.class);

                                // Если productId совпадает с id маршрута
                                if (productId != null && productId.equals(route.getTitle()) && rating != null) {
                                    totalRating += rating;
                                    reviewCount++;
                                }
                            }

                            // Рассчитываем средний рейтинг
                            double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                            route.setScore(averageRating); // Обновляем рейтинг маршрута
                        }

                        // Сортируем список маршрутов по убыванию рейтинга
                        Collections.sort(list, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));

                        // Создаем новый список с ограничением до 5 элементов
                        ArrayList<ItemRoute> topRatedList = new ArrayList<>();
                        int limit = Math.min(list.size(), 5); // Ограничиваем до 5 элементов
                        for (int i = 0; i < limit; i++) {
                            topRatedList.add(list.get(i));
                        }

                        // Обновляем RecyclerView
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
                        item.setId(issue.getKey()); // Устанавливаем id достопримечательности
                        list.add(item);
                    }
                }

                // Загружаем отзывы для расчета рейтинга
                reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reviewsSnapshot) {
                        for (ItemAttractions attraction : list) {
                            double totalRating = 0;
                            int reviewCount = 0;

                            // Проходим по всем отзывам
                            for (DataSnapshot review : reviewsSnapshot.getChildren()) {
                                String productId = review.child("productId").getValue(String.class);
                                Double rating = review.child("rating").getValue(Double.class);

                                // Если productId совпадает с id достопримечательности
                                if (productId != null && productId.equals(attraction.getTitle()) && rating != null) {
                                    totalRating += rating;
                                    reviewCount++;
                                }
                            }

                            // Рассчитываем средний рейтинг
                            double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;
                            attraction.setScore(averageRating); // Обновляем рейтинг достопримечательности
                        }

                        // Сортируем список достопримечательностей по убыванию рейтинга
                        Collections.sort(list, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));

                        // Создаем новый список с ограничением до 5 элементов
                        ArrayList<ItemAttractions> topRatedList = new ArrayList<>();
                        int limit = Math.min(list.size(), 5); // Ограничиваем до 5 элементов
                        for (int i = 0; i < limit; i++) {
                            topRatedList.add(list.get(i));
                        }

                        // Обновляем RecyclerView
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

    private void filterEventsByCategory(String category) {
        List<KurskEventsParser.Event> filteredList = new ArrayList<>();

        for (KurskEventsParser.Event event : allEventsList) {
            if (event.getCategory().equalsIgnoreCase(category)) {
                filteredList.add(event);
            }
        }

        eventAdapter.updateList(filteredList);
    }


}
