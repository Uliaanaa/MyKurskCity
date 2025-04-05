package com.example.solovinyykray.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

import com.example.solovinyykray.Domain.KurskEventParser;
import com.example.solovinyykray.Domain.KurskEventsParser;
import com.example.solovinyykray.databinding.ActivityDetailEventBinding;

/**
 * Активность для отображения детальной информации о событии, получает данные из Intent,
 * а также загружает дополнительные данные с сайта асинхронно.
 * Позволяет перейти на сайт события.
 */
public class Detail_EventActivity extends AppCompatActivity {
    ActivityDetailEventBinding binding;
    private KurskEventsParser.Event event;
    private KurskEventParser.Event kurskEvent;

    /**
     * Инициализирует активность, привязку данных, получает переданные данные,
     * настраивает интерфейс и запускает загрузку дополнительных данных о событии.
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra();
        enableImmersiveMode();

        if (event != null) {
            setVariableFromObject(event);
            new FetchEventTask().execute(event.getLink());
        }

        binding.backBtn.setOnClickListener(v -> finish());

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.getLink()));
            startActivity(browserIntent);
        });
    }

    /**
     * Получает объект события из Intent.
     */

    private void getIntentExtra() {
        event = getIntent().getParcelableExtra("object");
    }

    /**
     * Устанавливает переданные данные и элементы интерфейса.
     * @param event объект с переданными данными.
     */

    private void setVariableFromObject(KurskEventsParser.Event event) {
        binding.titleTxt.setText(event.getTitle());
        binding.distanceTxt.setText(event.getPrice());
        binding.bedTxt.setText(event.getCategory());
        binding.durationTxt.setText(event.getDate());
        Glide.with(this).load(event.getImageUrl()).into(binding.pic);
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
     * Устанавливает данные, которые получены с сайта.
     * @param kurskEvent объект с дополнительными данными.
     */

    private void setVariable(KurskEventParser.Event kurskEvent) {
        binding.descriptionTxt.setText(kurskEvent.getDescription());
        binding.adressTxt.setText(kurskEvent.getAddress());
        binding.locationTxt.setText(kurskEvent.getLocation());
    }

    /**
     * AsyncTask для асинхронной загрузки и парсинга дополнительных данных о событии.
     */

    private class FetchEventTask extends AsyncTask<String, Void, KurskEventParser.Event> {
        /**
         * Выполняет парсинг данных о событии в фоновом потокею
         * @param params URL страницы.
         * @return объект с дополнительными данными.
         */
        @Override
        protected KurskEventParser.Event doInBackground(String... params) {
            String url = params[0];
            return KurskEventParser.parseEventFromPage(url);
        }

        /**
         * Обрабатывает результат парсинга в фоновом потоке
         * @param result результатом является объект дополнительных данных.
         *
         */

        @Override
        protected void onPostExecute(KurskEventParser.Event result) {
            if (result != null) {
                kurskEvent = result;
                setVariable(kurskEvent);
            }
        }
    }
}