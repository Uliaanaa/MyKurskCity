package com.example.myapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myapplication.Adapter.ExplorerAdapter;
import com.example.myapplication.Adapter.PopularAdapter;
import com.example.myapplication.Domain.ItemDomain;
import com.example.myapplication.Domain.ItemRoute;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityExplorerBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

public class ExplorerActivity extends BaseActivity {
    ActivityExplorerBinding binding;
    ExplorerAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExplorerBinding.inflate(getLayoutInflater());
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

                }if(id==R.id.attractions){
                    intent = new Intent(getApplicationContext(), AttractionsActivity.class);
                    startActivity(intent);

                }if(id==R.id.profile){
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    startActivity(intent);

                }
                if(id==R.id.cart){
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);

                }
            }

        });
    }

    private void initRoute() {
        DatabaseReference myRef=database.getReference("Route");
        binding.progressBarExplorer.setVisibility(View.VISIBLE);

        ArrayList<ItemRoute> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for(DataSnapshot issue:snapshot.getChildren()) {
                        list.add(issue.getValue(ItemRoute.class));
                    }
                    if(!list.isEmpty()) {
                        binding.RecyclerViewExplorer.setLayoutManager(new LinearLayoutManager(ExplorerActivity.this,LinearLayoutManager.VERTICAL,false));
                        adapter=new ExplorerAdapter(list);
                        binding.RecyclerViewExplorer.setAdapter(adapter);
                    }
                    binding.progressBarExplorer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}

