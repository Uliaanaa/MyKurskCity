package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.solovinykray.solovinyykray.Adapter.AdminAdapter;
import com.solovinykray.solovinyykray.Domain.Attractions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityAdminBinding;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Активность администратора, позволяет админу управлять завками на добавление достопримечательности.
 * Админ может просматривать, одобрять или отклонять полученные заявки чтобы информация была достоверная.
 * Также активность включает в себя нижнее навигационное меню для перехода к другим экранам.
 */

public class AdminActivity extends AppCompatActivity implements AdminAdapter.OnAttractionActionListener {

    private ActivityAdminBinding binding;
    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<Attractions> pendingAttractions;
    private TextView noRequestsTextView;

    /**
     * Инициализирует активность, настраивает интерфейс и загружает данные.
     * @param savedInstanceState Сохраненное состояние активности (может быть null).
     */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        noRequestsTextView = findViewById(R.id.textView8);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        pendingAttractions = new ArrayList<>();
        adapter = new AdminAdapter(pendingAttractions, this, this); // Передаем this как слушатель
        recyclerView.setAdapter(adapter);

        loadPendingAttractions();
        enableImmersiveMode();

        binding.backBtn.setOnClickListener(v -> finish());

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if (id == R.id.explorer) {
                    intent = new Intent(AdminActivity.this, ExplorerActivity.class);
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
     * Загружает список заявок на добавление достопримечательности из Firebase.
     * Обновляет RecyclerView и проверяет наличие данных.
     */
    private void loadPendingAttractions() {
        DatabaseReference pendingRef = FirebaseDatabase.getInstance().getReference("PendingAttractions");
        pendingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingAttractions.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Attractions attraction = dataSnapshot.getValue(Attractions.class);
                    if (attraction != null) {
                        attraction.setId(dataSnapshot.getKey()); // Сохраняем ID заявки
                        pendingAttractions.add(attraction);
                    }
                }
                adapter.notifyDataSetChanged();
                updateNoRequestsTextView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Обновляет видимость TextView в зависимости от наличия заявок.
     */
    private void updateNoRequestsTextView() {
        if (pendingAttractions.isEmpty()) {
            noRequestsTextView.setVisibility(View.VISIBLE);
        } else {
            noRequestsTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Обновляет видимость TextView после одобрения или отклонения заявки.
     */
    @Override
    public void onAttractionRemoved() {
        updateNoRequestsTextView();
    }
}