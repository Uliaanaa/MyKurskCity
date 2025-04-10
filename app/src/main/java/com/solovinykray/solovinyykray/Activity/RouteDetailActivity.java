package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.solovinykray.solovinyykray.Adapter.AttractionsAdapter;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.solovinyykray.solovinyykray.R;
import java.util.ArrayList;

/**
 * Activity для отображения выбранных достопримечательностей и построения маршрута.
 * Показывает список выбранных пользователем достопримечательностей и предоставляет
 * возможность построить маршрут через них.
 */

public class RouteDetailActivity extends AppCompatActivity {
    private RecyclerView recyclerViewSelected;
    private Button buildRouteButton;
    private AttractionsAdapter adapter;
    private ArrayList<ItemAttractions> selectedItems;

    /**
     * Инициализирует активность, настраивает интерфейс и обработчики событий.
     * Получает выбранные достопримечательности из Intent и отображает их в списке.
     *
     * @param savedInstanceState Сохраненное состояние активности. Может быть null.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        recyclerViewSelected = findViewById(R.id.recyclerViewSelected);
        buildRouteButton = findViewById(R.id.buildRouteButton);

        selectedItems = (ArrayList<ItemAttractions>) getIntent().getSerializableExtra("selectedItems");

        if (selectedItems != null && !selectedItems.isEmpty()) {
            adapter = new AttractionsAdapter(selectedItems);
            recyclerViewSelected.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewSelected.setAdapter(adapter);
        }

        buildRouteButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("selectedItems", selectedItems);
            startActivity(intent);
        });

        enableImmersiveMode();
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
}