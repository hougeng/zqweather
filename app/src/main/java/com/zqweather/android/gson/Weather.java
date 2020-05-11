package com.zqweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/*实例类*/
public class Weather {
    public String status;

    public Basic basic;

    public Update update;
    public AQI aqi;


    public Now now;

//    public Suggestion suggestion;

    @SerializedName("lifestyle")
    public List<Suggestion> suggestionList;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;  //List集合引用数组类
}

