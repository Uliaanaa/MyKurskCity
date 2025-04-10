package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityLoginBinding;

/**
 * Активность для авторизации пользователей в приложении.
 * Предоставляет форму входа с полями email и пароля,
 * а также нижнее меню для перехода к другим разделам приложения.
 */

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    /**
     * Инициализирует интерфейс,настраивает навигационное меню
     * и обработчики событий.
     * @param savedInstanceState Сохраненное состояние активности (может быть null).
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        enableImmersiveMode();

        ChipNavigationBar chipNavigationBar = findViewById(R.id.menu);
        chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Intent intent;
                if(id==R.id.explorer){
                    intent = new Intent(getApplicationContext(), ExplorerActivity.class);
                    startActivity(intent);

                }if(id==R.id.attractions){
                    intent = new Intent(getApplicationContext(), AttractionsActivity.class);
                    startActivity(intent);

                }if(id==R.id.home){
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);

                }
                if(id==R.id.cart){
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);

                }
            }
        });

        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.emailEt.getText().toString().isEmpty() || binding.passwordEt.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(binding.emailEt.getText().toString(), binding.passwordEt.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                                    }
                                }
                            });
                }
            }
        });
        binding.goToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
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

}