package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.solovinykray.solovinyykray.Domain.Review;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityDetailRouteBinding;

import java.io.IOException;
import java.util.Locale;

/**
 * Активность для просмотра детальной информации о маршруте,
 * позволяет просмотреть полное описание, рейтнг изображения и отзывы.
 * Поддерживает функцию добавления в избранное, переход к карте и переход к разделу с отзывами.
 */

public class Detail_RouteActivity extends BaseActivity {
    ActivityDetailRouteBinding binding;
    private ItemRoute object;
    boolean isFavorite;
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private Handler handler = new Handler();
    private SeekBar seekBar;
    private ImageButton playPauseBtn;
    private LinearLayout audioContainer;
    private ProgressBar audioProgress;

    /**
     * Инициализирует активность, получает переданные данные из Intent,
     * настраивает интерфейс и загружает рейтинг маршрута.
     * Содержит медиаплеер для воспроизведения аудиосопровождения маршрута
     * с возможностью управления воспроизведением и отображением прогресса.
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        seekBar = binding.seekBar;
        playPauseBtn = binding.playPauseBtn;
        audioContainer = binding.audioContainer;
        audioProgress = binding.audioProgress;

        getIntentExtra();
        setVariable();
        loadAndCalculateRating();
        enableImmersiveMode();
        setupAudioPlayer();
    }

    /**
     * Настраивает элементы интерфейса для работы с аудиоплеером.
     * Проверяет наличие аудиофайла и инициализирует MediaPlayer при его наличии.
     * Устанавливает обработчики событий для кнопки воспроизведения/паузы и ползунка прогресса.
     */
    private void setupAudioPlayer() {
        binding.audioContainer.setVisibility(View.GONE);

        if (object.getAudioToken() != null && !object.getAudioToken().isEmpty()) {
            if (isValidFirebaseStorageUrl(object.getAudioToken())) {
                binding.audioContainer.setVisibility(View.VISIBLE);
                initMediaPlayer(object.getAudioToken());
                Log.d("AudioDebug", "Используем аудио из Firebase Storage: " + object.getAudioToken());
            } else {
                Log.d("AudioDebug", "Аудио ссылка не является валидным Firebase Storage URL");
            }
        } else {
            Log.d("AudioDebug", "Аудио токен отсутствует");
        }



        binding.playPauseBtn.setOnClickListener(v -> {
            if (mediaPlayer != null && isPrepared) {
                if (mediaPlayer.isPlaying()) {
                    pauseAudio();
                } else {
                    playAudio();
                }
            }
        });

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * Проверяет, является ли переданный URL валидной ссылкой Firebase Storage.
     * @param url Строка с URL для проверки
     * @return true если URL является валидной ссылкой Firebase Storage, иначе false
     */
    private boolean isValidFirebaseStorageUrl(String url) {
        return url != null && url.startsWith("https://firebasestorage.googleapis.com/");
    }

    /**
     * Инициализирует MediaPlayer для воспроизведения аудио по указанному URL.
     * Устанавливает обработчики событий подготовки, завершения и ошибок воспроизведения.
     * @param audioUrl URL аудиофайла для воспроизведения
     */
    private void initMediaPlayer(String audioUrl) {
        binding.audioProgress.setVisibility(View.VISIBLE);
        binding.playPauseBtn.setEnabled(false);

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(audioUrl);

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                binding.audioProgress.setVisibility(View.GONE);
                binding.playPauseBtn.setEnabled(true);
                binding.seekBar.setMax(mediaPlayer.getDuration());
                updateSeekBar();
                Log.d("AudioDebug", "Аудио успешно подготовлено");
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                binding.playPauseBtn.setImageResource(R.drawable.ic_play);
                binding.seekBar.setProgress(0);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("AudioError", "Ошибка воспроизведения: " + what + ", " + extra);
                binding.audioContainer.setVisibility(View.GONE);
                return true;
            });

            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("AudioError", "Ошибка инициализации аудио", e);
            binding.audioContainer.setVisibility(View.GONE);
            Toast.makeText(this, "Ошибка загрузки аудио", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Обновляет положение ползунка воспроизведения и отображаемое время.
     * Вызывается периодически во время воспроизведения аудио.
     */
    private void updateSeekBar() {
        if (mediaPlayer != null) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            TextView timeText = findViewById(R.id.timeText);
            timeText.setText(formatTime(mediaPlayer.getCurrentPosition()));

            if (mediaPlayer.isPlaying()) {
                handler.postDelayed(this::updateSeekBar, 1000);
            }
        }
    }

    /**
     * Форматирует время в миллисекундах в строку формата "мм:сс".
     * @param milliseconds Время в миллисекундах
     * @return Отформатированная строка времени
     */
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * Начинает воспроизведение аудио, если MediaPlayer инициализирован и подготовлен.
     * Обновляет иконку кнопки воспроизведения и запускает обновление ползунка.
     */
    private void playAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            updateSeekBar();
        }
    }

    /**
     * Приостанавливает воспроизведение аудио, если MediaPlayer активен.
     * Обновляет иконку кнопки воспроизведения.
     */
    private void pauseAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            playPauseBtn.setImageResource(R.drawable.ic_play);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    /**
     * Освобождает ресурсы MediaPlayer при уничтожении активности.
     * Удаляет все pending callback'и Handler'а.
     */
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            handler.removeCallbacksAndMessages(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    /**
     * Настраивает переменные и элементы интерфейса,
     * устанавливает обработчики событий для кнопок.
     */

    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.ratingBar.setRating((float) object.getScore());

        Glide.with(Detail_RouteActivity.this)
                .load(object.getPic())
                .into(binding.pic);

        binding.backBtn.setOnClickListener(v -> finish());

        binding.allbtn.setOnClickListener(v -> {
            Intent intent = new Intent(Detail_RouteActivity.this, ReviewsActivity.class);
            intent.putExtra("productId", object.getTitle());
            startActivity(intent);
        });

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Detail_RouteActivity.this, MapActivity.class);
            intent.putExtra("object", object);
            startActivity(intent);
        });

        binding.descriptionTxt.setText(Html.fromHtml(object.getDetail(), Html.FROM_HTML_MODE_LEGACY));

        if (isFavorite) {
            binding.favIcon.setImageResource(R.drawable.fav);
        } else {
            binding.favIcon.setImageResource(R.drawable.fav_icon);
        }
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
     * Получает объект маршрута из Intent, загружает его статус избранного.
     */

    private void getIntentExtra() {
        object = (ItemRoute) getIntent().getSerializableExtra("object");
        isFavorite = getIntent().getBooleanExtra("isFavorite", false);
    }

    /**
     * Загружает отзывы из Firebase и на их основе высчитывает рейтинг достопримечательности,
     * в соотвествии с рейтингом обновляет RatingBar.
     */

    private void loadAndCalculateRating() {
        String productId = object.getTitle();

        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");
        reviewsRef.orderByChild("productId").equalTo(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double totalRating = 0;
                int reviewCount = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Review review = snapshot.getValue(Review.class);
                    if (review != null && review.getProductId().equals(productId)) {
                        totalRating += review.getRating();
                        reviewCount++;
                    }
                }

                if (reviewCount > 0) {
                    double averageRating = totalRating / reviewCount;
                    binding.ratingBar.setRating((float) averageRating);

                } else {
                    binding.ratingBar.setRating(0);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Ошибка загрузки отзывов: " + databaseError.getMessage());
            }
        });
    }
}