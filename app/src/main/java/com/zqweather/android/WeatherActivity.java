package com.zqweather.android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zqweather.android.gson.Forecast;
import com.zqweather.android.gson.Suggestion;
import com.zqweather.android.gson.Weather;
import com.zqweather.android.util.HttpUtil;
import com.zqweather.android.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;
import java.lang.reflect.Array;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = null;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private LinearLayout forecastLayout;

    protected void onCreate(Bundle saveInstanceState) {

        super.onCreate(saveInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        /*初始化控件*/
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sporttext);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); //缓存
        String weatherString = prefs.getString("weather",null);
        if(weatherString != null) {
            /*有缓存时直接读取本地数据*/
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);

        } else {
            //无缓存时读取服务器
            String weatherId =getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.VISIBLE);
            requestWeather(weatherId);
        }
    }

    /*根据天气id请求城市天气信息*/

    public void requestWeather(final String weatherId) {
        String weatherUrl = "https://free-api.heweather.com/s6/weather?key=eabcda814101431285989fc01fb82f84&location=" + weatherId; //https://free-api.heweather.com/s6/weather?key=eabcda814101431285989fc01fb82f84&location=%E8%8B%8F%E5%B7%9E
        //+ "&key=4a2de9703301462085674a22ad704c92"
        Log.d(TAG, "requestWeather: "+ weatherUrl);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);  //转换成weather对象
                runOnUiThread(new Runnable() {  //切换到主线程
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else {
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    /*处理并展示Weather 实体类中的数据*/
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.update.updateTime.split(" ")[1];  //空格隔开，取第二个，第一个是日期
        String degree = weather.now.temperature + "°C";
        String weatherInfo = weather.now.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        forecastLayout.removeAllViews();
        weatherInfoText.setText(weatherInfo);
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.info);
            maxText.setText(forecast.max);
            minText.setText(forecast.min);
            forecastLayout.addView(view); // 往forecast_layout添加页面

        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        for (Suggestion suggestion : weather.suggestionList) {
            String comfort = "舒适度： " + suggestion.comfort_info;
            Log.d(TAG, "showWeatherInfo: " + comfort);
            String carWash = "洗车指数: " + suggestion.comfort_info;
//            String sport = "运动建议: " + suggestion.sport_info;
            comfortText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(suggestion.comfort_info);
            weatherLayout.setVisibility(View.VISIBLE);
        }
    }
}
