package com.example.myapplication.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

import com.example.myapplication.Domain.KurskEventParser;
import com.example.myapplication.Domain.KurskEventsParser;
import com.example.myapplication.databinding.ActivityDetailEventBinding;

public class Detail_EventActivity extends AppCompatActivity {
    ActivityDetailEventBinding binding;
    private KurskEventsParser.Event event; // Объект события из KurskEventsParser
    private KurskEventParser.Event kurskEvent; // Объект события из KurskEventParser

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra(); // Получаем данные из Intent

        if (event != null) {
            setVariableFromObject(event);
            new FetchEventTask().execute(event.getLink()); // Запускаем асинхронный процесс получения данных
        }

        binding.backBtn.setOnClickListener(v -> finish());

        // Кнопка для перехода в браузер
        binding.addToCartBtn.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.getLink())); // Создаем Intent для открытия браузера
            startActivity(browserIntent); // Запускаем Intent
        });
    }

    private void getIntentExtra() {
        // Получение объекта события из KurskEventsParser
        event = getIntent().getParcelableExtra("object");
    }

    private void setVariableFromObject(KurskEventsParser.Event event) {
        // Установка данных из KurskEventsParser
        binding.titleTxt.setText(event.getTitle());
        binding.distanceTxt.setText(event.getPrice());
        binding.bedTxt.setText(event.getCategory());
        binding.durationTxt.setText(event.getDate());
        Glide.with(this).load(event.getImageUrl()).into(binding.pic);
    }

    private void setVariable(KurskEventParser.Event kurskEvent) {
        // Установка данных из KurskEventParser
        binding.descriptionTxt.setText(kurskEvent.getDescription());
        binding.adressTxt.setText(kurskEvent.getAddress());
        binding.locationTxt.setText(kurskEvent.getLocation());
    }

    private class FetchEventTask extends AsyncTask<String, Void, KurskEventParser.Event> {
        @Override
        protected KurskEventParser.Event doInBackground(String... params) {
            String url = params[0];
            return KurskEventParser.parseEventFromPage(url); // Парсим событие из страницы
        }

        @Override
        protected void onPostExecute(KurskEventParser.Event result) {
            if (result != null) {
                kurskEvent = result; // Сохраняем результат
                setVariable(kurskEvent); // Устанавливаем данные в представления
            }
        }
    }
}