package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.solovinykray.solovinyykray.Adapter.RequestsAdapter;
import com.solovinykray.solovinyykray.Domain.Attractions;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityRequestBinding;

import java.util.ArrayList;

/**
 * Активность для просмотра и управления заявками пользователя.
 * Позволяет просматривать заявки в разных статусах (одобренные, отклоненные, на рассмотрении).
 */
public class RequestActivity extends BaseActivity {

    private ActivityRequestBinding binding;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView noRequestsText;
    private RequestsAdapter adapter;
    private ArrayList<Attractions> requestsList = new ArrayList<>();
    private FirebaseUser currentUser;

    /**
     * Инициализация активности при создании.
     *
     * @param savedInstanceState сохраненное состояние активности
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        binding.addBtn.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), FormActivity.class);
            startActivity(intent);
        });

        initializeViews();
        setupSpinner();
        enableImmersiveMode();

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(id -> {
            Intent intent;
            if (id == R.id.home) {
                intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            } else if (id == R.id.explorer) {
                intent = new Intent(getApplicationContext(), ExplorerActivity.class);
                startActivity(intent);
            } else if (id == R.id.profile) {
                intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
            } else if (id == R.id.cart) {
                intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Инициализация всех view-элементов активности.
     */
    private void initializeViews() {
        recyclerView = findViewById(R.id.RecyclerViewRequest);
        progressBar = findViewById(R.id.progressBarRequest);
        noRequestsText = findViewById(R.id.textView8);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RequestsAdapter(requestsList);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Включение иммерсивного режима (полноэкранного режима с скрытием панели навигации).
     */
    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Настройка Spinner для фильтрации заявок по статусу.
     */
    private void setupSpinner() {
        Spinner requestSpinner = findViewById(R.id.request_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.request_items, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        requestSpinner.setAdapter(spinnerAdapter);

        requestSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = parent.getItemAtPosition(position).toString();
                if (selectedStatus.equals("Одобренные")) {
                    loadUserRequests("approved");
                } else if (selectedStatus.equals("Отклоненные")) {
                    loadUserRequests("rejected");
                } else {
                    loadUserRequests("pending");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Загрузка заявок пользователя с определенным статусом из Firebase.
     *
     * @param status статус заявок для загрузки (approved, rejected, pending)
     */
    private void loadUserRequests(String status) {
        progressBar.setVisibility(View.VISIBLE);
        noRequestsText.setVisibility(View.GONE);
        requestsList.clear();

        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Attractions");
        requestsRef.orderByChild("status").equalTo(status).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestsList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Attractions attraction = dataSnapshot.getValue(Attractions.class);
                    if (attraction != null && attraction.getAuthor() != null &&
                            attraction.getAuthor().equals(currentUser.getUid())) {

                        attraction.setId(dataSnapshot.getKey());
                        requestsList.add(attraction);
                    }
                }

                if (requestsList.isEmpty()) {
                    noRequestsText.setVisibility(View.VISIBLE);
                    noRequestsText.setText(status.equals("approved") ?
                            "Нет одобренных заявок" :
                            status.equals("rejected") ? "Нет отклоненных заявок" : "Нет заявок");
                } else {
                    noRequestsText.setVisibility(View.GONE);
                }

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                noRequestsText.setVisibility(View.VISIBLE);
                noRequestsText.setText("Ошибка загрузки данных");
                Toast.makeText(RequestActivity.this, "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}