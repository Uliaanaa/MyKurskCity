package com.solovinykray.solovinyykray.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;

import com.solovinykray.solovinyykray.Adapter.CategoryAdapter;
import com.solovinykray.solovinyykray.Adapter.EventAdapter;
import com.solovinykray.solovinyykray.Adapter.PopularAdapter;
import com.solovinykray.solovinyykray.Adapter.RecomendedAdapter;
import com.solovinykray.solovinyykray.Adapter.SliderAdapter;
import com.solovinykray.solovinyykray.Domain.Category;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.solovinykray.solovinyykray.Domain.KurskEventsParser;
import com.solovinykray.solovinyykray.Domain.SliderItems;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityMainBinding;


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
    private EventAdapter adapter;
    private List<KurskEventsParser.Event> allEventsList = new ArrayList<>();
    private static final String PREFS_NAME = "TutorialPrefs";
    private static final String KEY_TUTORIAL_SHOWN = "main_tutorial_shown";

    /**
     * Инициализирует интерфейс, загружает данные, настраивает обработчики событий
     * и показывает обучающие подсказки при первом запуске.
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
        showTutorialIfFirstLaunch();

        TextView routeButton = findViewById(R.id.route_btn);
        TextView attractionsButton = findViewById(R.id.attractions_btn);
        TextView eventButton = findViewById(R.id.event_btn);

        routeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    Intent intent = new Intent(MainActivity.this, ExplorerActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                }
            }
        });

        attractionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    Intent intent = new Intent(MainActivity.this, MarkerActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                }
            }
        });

        eventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    Intent intent = new Intent(MainActivity.this, EventActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(MainActivity.this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent;
                if (id == R.id.explorer) {
                    intent = new Intent(MainActivity.this, ExplorerActivity.class);
                    startActivity(intent);
                } else if (id == R.id.attractions) {
                    intent = new Intent(getApplicationContext(), AttractionsActivity.class);
                    startActivity(intent);
                } else if (id == R.id.profile) {
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);
                } else if (id == R.id.cart) {
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * Проверяет наличие подключения к интернету.
     * @return true, если интернет доступен, false в противном случае.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Проверяет, был ли показан туториал, и отображает подсказки последовательно при первом запуске.
     * Завершает туториал при прокрутке ScrollView.
     */
    private void showTutorialIfFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean tutorialShown = prefs.getBoolean(KEY_TUTORIAL_SHOWN, false);

        if (!tutorialShown) {
            // Показываем первую подсказку
            binding.tutorialOverlayTop.setVisibility(View.VISIBLE);
            binding.tutorialOverlayBottom.setVisibility(View.GONE);

            // При клике на первую подсказку показываем вторую (возле меню)
            binding.tutorialOverlayTop.setOnClickListener(v -> {
                binding.tutorialOverlayTop.setVisibility(View.GONE);
                binding.tutorialOverlayBottom.setVisibility(View.VISIBLE);
            });

            // При клике на вторую подсказку завершаем туториал
            binding.tutorialOverlayBottom.setOnClickListener(v -> {
                completeTutorial(prefs);
            });

            // Завершаем туториал при прокрутке ScrollView
            binding.scrollView2.setOnScrollChangeListener((View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
                if (scrollY != oldScrollY) {
                    completeTutorial(prefs);
                }
            });
        } else {
            // Если туториал уже показан, скрываем оба оверлея
            binding.tutorialOverlayTop.setVisibility(View.GONE);
            binding.tutorialOverlayBottom.setVisibility(View.GONE);
        }
    }

    /**
     * Завершает туториал, скрывая оверлеи и сохраняя состояние в SharedPreferences.
     */
    private void completeTutorial(SharedPreferences prefs) {
        binding.tutorialOverlayTop.setVisibility(View.GONE);
        binding.tutorialOverlayBottom.setVisibility(View.GONE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_TUTORIAL_SHOWN, true);
        editor.apply();
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
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            binding.progressBarPopular.setVisibility(View.GONE);
            return;
        }

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
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            binding.progressBarBanner.setVisibility(View.GONE);
            return;
        }

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
                Log.e("Firebase", "Ошибка загрузки баннеров: " + error.getMessage());
                binding.progressBarBanner.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Загружает и отображает популярные достопримечательности (топ-5 по рейтингу).
     */
    private void initRecomended() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            binding.progressBarRecomended.setVisibility(View.GONE);
            return;
        }

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
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            binding.progressBarCategory.setVisibility(View.GONE);
            return;
        }

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
                Log.e("Firebase", "Ошибка загрузки категорий: " + error.getMessage());
                binding.progressBarCategory.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Загружает список мероприятий с сайта.
     */
    private void initEvents() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            binding.progressBarEvent.setVisibility(View.GONE);
            return;
        }

        binding.progressBarEvent.setVisibility(View.VISIBLE);

        new Thread(() -> {
            allEventsList = KurskEventsParser.parseEvents("https://welcomekursk.ru/events", 15);

            runOnUiThread(() -> {
                if (!allEventsList.isEmpty()) {
                    binding.recyclerViewEvent.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    adapter = new EventAdapter(allEventsList);
                    binding.recyclerViewEvent.setAdapter(adapter);
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

        adapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            binding.tvNoEventsFound.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoEventsFound.setVisibility(View.GONE);
        }
    }
}