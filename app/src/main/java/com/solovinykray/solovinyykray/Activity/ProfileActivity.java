package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityProfileBinding;

import java.io.IOException;

/**
 * Activity для управления профилем пользователя.
 * Позволяет пользователю изменять никнейм, пароль и аватар,
 * а также содержит навигацию по приложению.
 */

 public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private Uri filePath;

    /**
     * Инициализирует активность, настраивает интерфейс и обработчики событий.
     * @param savedInstanceState Сохраненное состояние активности
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        }

        binding.saveChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newNickname = binding.editNickname.getText().toString().trim();
                String newPassword = binding.editPassword.getText().toString().trim();

                if (newNickname.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseDatabase.getInstance().getReference("users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("username")
                        .setValue(newNickname)
                        .addOnSuccessListener(unused -> Toast.makeText(ProfileActivity.this, "Никнейм изменен", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Не удалось изменить никнейм", Toast.LENGTH_SHORT).show());

                FirebaseAuth.getInstance().getCurrentUser().updatePassword(newPassword)
                        .addOnSuccessListener(unused -> Toast.makeText(ProfileActivity.this, "Пароль изменен", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Не удалось изменить пароль", Toast.LENGTH_SHORT).show());
            }
        });

        binding.adminPanelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, AdminActivity.class);
                startActivity(intent);
            }
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

                }if(id==R.id.explorer){
                    intent = new Intent(getApplicationContext(), ExplorerActivity.class);
                    startActivity(intent);

                }if(id==R.id.cart){
                    intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                    startActivity(intent);

                }

            }

        });

        loadUserInfo();
        enableImmersiveMode();

        binding.profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        binding.exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(ProfileActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }


    ActivityResultLauncher<Intent> pickImageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == ProfileActivity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        filePath = result.getData().getData();

                        try {
                            Bitmap bitmap = handleImageOrientation(filePath);
                            bitmap = MediaStore.Images.Media
                                    .getBitmap(
                                            getContentResolver(),
                                            filePath
                                    );
                            binding.profileImage.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(ProfileActivity.this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show();
                        }
                        uploadImage();
                    }
                }
            }
    );

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
     * Загружает информацию о пользователе из Firebase Database.
     * Включает никнейм, аватар и роль пользователя.
     */

    private void loadUserInfo() {
        FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.child("username").getValue().toString();
                        String profileImage = snapshot.child("profileImage").getValue().toString();
                        String role = snapshot.child("role").getValue(String.class);

                        binding.usernameTv.setText(username);

                        if (!profileImage.isEmpty()) {
                            Glide.with(ProfileActivity.this).load(profileImage).into(binding.profileImage);
                        }

                        if ("admin".equals(role)) {
                            binding.adminPanelBtn.setVisibility(View.VISIBLE);
                        } else {
                            binding.adminPanelBtn.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    /**
     * Инициирует выбор изображения для аватара из галереи устройства.
     */

    private void selectImage() {
        Intent intent= new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        pickImageActivityResultLauncher.launch(intent);
    }

    /**
     * Обрабатывает ориентацию выбранного изображения и корректирует ее при необходимости.
     *
     * @param imageUri URI выбранного изображения
     * @return Bitmap с корректной ориентацией
     * @throws IOException если возникла ошибка при чтении изображения
     */


    private Bitmap handleImageOrientation(Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(imageUri));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(bitmap, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(bitmap, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(bitmap, 270);
            default:
                return bitmap;
        }
    }

    /**
     * Поворачивает изображение на заданный угол.
     * @param source Исходное изображение
     * @param angle Угол поворота в градусах
     * @return Повернутое изображение
     */

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Загружает выбранное изображение в Firebase Storage
     * и обновляет ссылку на аватар в Firebase Database.
     */

    private void uploadImage() {
        if(filePath!=null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseStorage.getInstance().getReference().child("images/"+uid)
                    .putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(getApplicationContext(), "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();

                            FirebaseStorage.getInstance().getReference().child("images/"+uid).getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                    .child("profileImage").setValue(uri.toString());
                                        }
                                    });

                        }
                    });
        }
    }
}