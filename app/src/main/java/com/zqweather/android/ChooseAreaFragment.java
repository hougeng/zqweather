package com.zqweather.android;
/*
碎片
 */
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.zqweather.android.R;
import com.zqweather.android.db.City;
import com.zqweather.android.db.County;
import com.zqweather.android.db.Province;
import com.zqweather.android.util.HttpUtil;
import com.zqweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    /*省列表*/
    private List<Province> provinceList;
    /*市列表*/
    private List<City> cityList;
    /*县列表*/
    private List<County> countyList;

    /*选中的省份*/
    private Province selectedProvince;
    /*选中的城市*/
    private City selectedCity;
    /*当前选中的级别*/
    private int currentLevel;

    /*创建Fragement对应的视图*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container ,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList); //适配器初始化
        //参数一：使用到的上下文对象   参数二：使用到的布局文件。给item进行使用的,此处为单行显示文字    参数三：数据源对象
        listView.setAdapter(adapter);//将适配器设置到listView上
//        return inflater.inflate(R.layout.choose_area, container,false);
        return view;
    }

    /*创建Fragement活动*/

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /*ListView点击事件*/
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            //parent:适配器设置到的adapterView对象，在这里表示的是ListView（就是代表的是当前的ListView对象listView）
            //AdapterView即Adapter（适配器）控件，其内容通常是一个包含多项相同格式资源的列表，每次只显示其中一项
            //view：适配器item对应的View
            //position：索引位置
            //id：在listView中的item对应的id
            //position是从0开始依次向下递增
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        /*Button点击事件*/
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                }else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /*queryProvinces方法实现，查询全国所有省，优先在本地数据库查询，没有则去服务器上查询*/
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE); //隐藏返回键
        provinceList = DataSupport.findAll(Province.class); //调用LitePal查询接口读取省级数据
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {  //依次取出字符串数组provinceList中的元素
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();//如果适配器变化，需要当前的listView也通知到
            listView.setSelection(0);//将第position个(这里是第0个）item显示在listView的最上面一项。
                                     //listView滚动到最后一个条目的方法:listview.setSelection(n-1)(n为数据的个数)
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }

    }

    /*queryCities方法实现，查询所有市，优先在本地数据库查询，没有则去服务器上查询*/
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceCode = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0 ) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /*queryCounties方法实现，查询所有县，优先在本地数据库查询，没有则去服务器上查询*/
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityId= ?",String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0 ) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else  {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode +"/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /*服务器查询省市县数据*/
    private void queryFromServer(String address, final String type) {
        showProgressDialog(); //进度条
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {   //涉及UI,从子线程切换到主线程
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }

            }
        });
    }

    /*显示进度对话框*/
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("加载中...");
            progressDialog.setCanceledOnTouchOutside(false); //点击屏幕外对话框不消失
        }
        progressDialog.show();
    }

    /*关闭进度对话框*/
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
