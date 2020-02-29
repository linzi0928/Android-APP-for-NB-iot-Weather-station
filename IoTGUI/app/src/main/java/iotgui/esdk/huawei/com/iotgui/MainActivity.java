package iotgui.esdk.huawei.com.iotgui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import static android.content.ContentValues.TAG;
import static clwater.androiddashboard.DashBoard.pos_setting;
import static clwater.androiddashboard.DashBoard.size_setting;
import static iotgui.esdk.huawei.com.iotgui.Constant.APPID;
import static iotgui.esdk.huawei.com.iotgui.Constant.SECRET;

/**
户外长时气象站User Terminal
本APP所获数据从华为物联网平台OceanConnect抓取并显示，相关节点信息用户可在节点设置中调整与保存
初次写安卓应用，代码如有不规范还请多担待。
本文件主要是信息获取与显示相关代码。
DashBoard.java主要包含仪表盘相关代码。
BottonActivity.java主要包含节点配置相关代码。
DensityUtil.java主要包含分辨率适配相关代码
折线图的库来自：com.github.mikephil.charting.charts.LineChart
仪表盘的库来自：clwater.androiddashboard
华为OceanConnect相关代码来自华为官方SDK
 Android Studio版本：3.5.3
天津大学 GIE工作室2020年2月8日V1.0
关注b站up主：GIE工作室，获得更多干货与信息
*/

// import org.apache.http.client.utils.URIBuilder; // The package is not usable!

public class MainActivity extends Activity {

    public final static String HTTPGET = "GET";
    public final static String HTTPPUT = "PUT";
    public final static String HTTPPOST = "POST";
    public final static String HTTPDELETE = "DELETE";
    public final static String HTTPACCEPT = "Accept";
    public final static String CONTENT_LENGTH = "Content-Length";
    public final static String CHARSET_UTF8 = "UTF-8";
    public static String fff_time = "20190124 010101";
    public String save_data_final;
    public String save_data_old;
    public String [] Weather_log={"1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1","1",};//存储24小时内的数据
    public String [] Setting_inf={"","","","","",""};//第0位节点名称，第1位设备ID，第2位应用ID，第3位应用秘钥，第4位仪表盘字体大小，第5位仪表盘字体位置
    public  String dev_id;

    private static HttpClient httpClient;
    private static String token;
    private static IoTData data;

    private static TextView tx01;
    private Handler handler = new Handler();
    LineData mLineData1;
    LineData mLineData2;
    LineChart chart1;
    LineChart chart2;
    clwater.androiddashboard.DashBoard d1;
    clwater.androiddashboard.DashBoard d2;

    private Button mBtn_Menu;
    private ImageView cover_img;

    int line_count=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tx01 = (TextView) findViewById(R.id.tx01);
        data = new IoTData();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        chart1 = (LineChart) findViewById(R.id.chart1);
        chart2 = (LineChart) findViewById(R.id.chart2);
        setChartStyle(chart1, mLineData1, Color.parseColor("#000044")," ");
        setChartStyle(chart2, mLineData2, Color.parseColor("#000044")," ");
        d1 = (clwater.androiddashboard.DashBoard) findViewById(R.id.dash1);
        d2=(clwater.androiddashboard.DashBoard) findViewById(R.id.dash2);
        mBtn_Menu=(Button)findViewById(R.id.btn_menu);
        cover_img = (ImageView) findViewById(R.id.cover);
        mBtn_Menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,ButtonActivity.class);
                startActivity(intent);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {//主函数入口
                try{
                Thread.sleep(2000);//显示2s启动界面
                cover_img.setVisibility(View.INVISIBLE);}//隐藏启动界面
                catch (Exception e) {

                }
                while (true) {//主循环
                    read_setting();
                    try {
                        token = cert();//使用应用ID和应用秘钥登录华为云获取token
                    } catch (Exception e) {
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "配置错误，请检查网络或节点设置", Toast.LENGTH_LONG).show();
                        Looper.loop();

                    } finally {
                        //if (token == null)
                            //tx01.setText("无法获取环境数据！");
                    }
                    Calendar nowTime = Calendar.getInstance();
                    nowTime.add(Calendar.HOUR, -8);
                    Date ago = nowTime.getTime();
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                    String now = format.format(ago) + "T";
                    format = new SimpleDateFormat("HHmmss");
                    now += format.format(ago) + "Z";
                    try {
                        if (token == null) {
                            token = cert();
                            if (token == null) {
                               // status.setText("无法获取环境数据！");
                                Looper.prepare();
                               Toast.makeText(MainActivity.this, "配置错误，请检查网络或节点设置", Toast.LENGTH_LONG).show();
                                Looper.loop();
                                continue;
                            }
                        }
                        String result = queryNode(token, dev_id, now);//使用获取的token和提前设置的设备ID查询设备信息，返回查询结果
                        if (parseIoTString(result)) {
                            String rff_time;
                            rff_time = data.getTime();
                            rff_time = rff_time.substring(0, 8) + ' ' + rff_time.substring(9, 14);
                            fff_time = timeZoneTransfer(rff_time, "yyyyMMdd HHmmss", "0", "+8");
                            fff_time=fff_time.substring(0,8)+' '+fff_time.substring(9,11)+':'+fff_time.substring(11,13);
                           // show_data();
                        }
                    } catch (Exception e) {
                        Log.d("MainActivity", "++++++++++++++++");
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            reportEnv();
                        }
                    });
                    try {
                        Thread.sleep(50000);
                    } catch (Exception e) {

                    }

                }

            }
        }).start();

    }

    /**
     * 从文件中获取相关配置，如果未读取到配置则创建配置文件模板并告知用户进行节点相关配置。
     */
    private  void read_setting() {
        int line_log=0;
    String setting_file = Environment.getExternalStorageDirectory().toString() + File.separator +"Weather_station_settings.txt";
    File fs_inf=new File(setting_file);
        try {
            InputStream instream = new FileInputStream(fs_inf);//尝试打开配置文件
            if (instream != null)
            {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                //分行读取配置信息
                while ((( line = buffreader.readLine()) != null)&&(line_log<=5)) {
                    Setting_inf[line_log] = line+"\n" ;
                    line_log++;
                }
                instream.close();
                if(Setting_inf[1].length()<=17)//检查读取到的设置是否为空。
                    dev_id="0";
                else
                    dev_id=Setting_inf[1].substring(Setting_inf[1].indexOf(">")+1,Setting_inf[1].indexOf("\n"));//截取并更新配置
                if(Setting_inf[2].length()<=17)
                    APPID="0";
                else
                    APPID=Setting_inf[2].substring(Setting_inf[2].indexOf(">")+1,Setting_inf[2].indexOf("\n"));
                if(Setting_inf[3].length()<=17)
                    SECRET="0";
                else
                    SECRET=Setting_inf[3].substring(Setting_inf[3].indexOf(">")+1,Setting_inf[3].indexOf("\n"));
                if(Setting_inf[4].length()<=11)
                    size_setting=Integer.valueOf("15").intValue();
                else
                    size_setting=Integer.valueOf(Setting_inf[4].substring(Setting_inf[4].indexOf(">")+1,Setting_inf[4].indexOf("\n"))).intValue();
                if(Setting_inf[5].length()<=10)
                    pos_setting=Integer.valueOf("20").intValue();
                else
                    pos_setting=Integer.valueOf(Setting_inf[5].substring(Setting_inf[5].indexOf(">")+1,Setting_inf[5].indexOf("\n"))).intValue();

            }
        }
        catch (java.io.FileNotFoundException e)//未找到配置文件则创建之
        {
            try {
                FileOutputStream outStream_log =new FileOutputStream(fs_inf,true);
                outStream_log.write("dev_name>\n".getBytes("gbk"));
                outStream_log.write("device_id>\n".getBytes("gbk"));
                outStream_log.write("app_id>\n".getBytes("gbk"));
                outStream_log.write("app_secret>\n".getBytes("gbk"));
                outStream_log.write("font_size>\n".getBytes("gbk"));
                outStream_log.write("font_pos>\n".getBytes("gbk"));
                outStream_log.flush();
                outStream_log.close();
            } catch (FileNotFoundException ee) {
                e.printStackTrace();
            }
            catch (IOException ee) {
                e.printStackTrace();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * 将获取的数据保存到本地文件中
     * */
    private void saveData(String finalContent) {
        if(!finalContent.equals(save_data_old))
        {
            line_count=0;
            String data_Dir = Environment.getExternalStorageDirectory().toString() + File.separator +"Weather_data.txt";//自运行以来所有接收到的数据
            String temp_data_Dir = Environment.getExternalStorageDirectory().toString() + File.separator +"24h_Weather_data.log";//过去24小时的数据，仅用于绘制折线图
            File fs = new File(data_Dir);
            File fs_log = new File(temp_data_Dir);

            try {
                InputStream instream = new FileInputStream(fs_log);
                if (instream != null)
                {
                    InputStreamReader inputreader = new InputStreamReader(instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String line;
                    //分行读取
                    while ((( line = buffreader.readLine()) != null)&&(line_count<=23)) {
                        Weather_log[line_count] = line+"\n" ;
                        line_count++;
                    }
                    instream.close();
                }
            }
            catch (java.io.FileNotFoundException e)//未找到最近24h记录文件则创建之
            {
                try {
                    FileOutputStream outStream_log =new FileOutputStream(fs_log,true);
                    if(save_data_final!=null) {
                        outStream_log.write(finalContent.getBytes("gbk"));
                        outStream_log.write(finalContent.getBytes("gbk"));
                    }
                    outStream_log.flush();
                    outStream_log.close();
                } catch (FileNotFoundException ee) {
                    e.printStackTrace();
                }
                catch (IOException ee) {
                    e.printStackTrace();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            if(line_count==0)//避免数组越界，如果为空文件则不再继续
                return;
            if(!(save_data_final).equals(Weather_log[line_count-1]))//比较当前获得的数据与最新一行数据是否相同，不同才写入，避免重复行
            {
                try {
                    FileOutputStream outStream;
                    if(line_count>=23)//如果积累到24小时的数据则删除最早的数据，加入最新的数据（类似FIFO队列）
                    {
                        fs_log.delete();
                        fs_log = new File(temp_data_Dir);
                        outStream =new FileOutputStream(fs_log,true);
                        for(int co=1;co<23;co++) {
                            outStream.write(Weather_log[co].getBytes("gbk"));
                        }
                        outStream.write(save_data_final.getBytes("gbk"));
                        outStream.flush();
                        outStream.close();
                        save_data_old = save_data_final;
                    }
                    else {
                        outStream = new FileOutputStream(fs_log, true);
                        outStream.write(finalContent.getBytes("gbk"));
                        outStream.flush();
                        outStream.close();
                        save_data_old = save_data_final;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    FileOutputStream outStream =new FileOutputStream(fs,true);
                    outStream.write(finalContent.getBytes("gbk"));
                    outStream.flush();
                    outStream.close();
                    save_data_old=save_data_final;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }
    /**
     * Classify the environment status.
     */
    private void reportEnv() {
        // Thresholds

        String text_output = "环境参数\n";
        float value;
        String rf_time,save_temp_data;
        value = data.getTemperature();
       // text_output += "室温 " + Float.toString(value) + "℃  " + "\n";
        save_temp_data= Float.toString(value) + "C  ";

        value = data.getHumidity();
       // text_output += "湿度 " + Float.toString(value) + "%  " + "\n";
        save_temp_data+= Float.toString(value) + "%  ";

        value = data.getPressure();
        text_output += "气压：" + Float.toString(value) + "Pa  " + "\n";
        save_temp_data+=Float.toString(value) + "Pa  ";

        value = data.getPm2_5();
        text_output += "PM2.5：" + Float.toString(value/100f) + "ug  " + "\n";
        save_temp_data+=Float.toString(value) + "ug ";

        value = data.getUvv();
        text_output += "紫外线：" + Float.toString(value) + "uW  " + "\n";
        save_temp_data+=Float.toString(value) + "uW ";

        value = data.getGas_r();
        text_output += "烟阻：" + Float.toString(value) + "hOhm  " + "\n";
        save_temp_data+=Float.toString(value) + "hOhm ";

        rf_time = fff_time;
        text_output += "更新时间" + (rf_time) + "\n";
        save_temp_data+=rf_time+"\n" ;

        save_data_final=save_temp_data;
        tx01.setText(text_output);
        saveData(save_data_final);
        show_data();

    }
    /**
     * 调用相关库显示数据
     * */
    private void show_data()
    {
        float temp_tt,temp_ht;
         mLineData1 = makeLineData(line_count,Color.CYAN,0);//根据读取的历史数据制作折线图库能够识别的数据格式
         mLineData2 = makeLineData(line_count,Color.parseColor("#8556CC"),1);
        chart1.setData(mLineData1);//将数据装入折线图
        chart1.setAutoScaleMinMaxEnabled(true);
        chart1.invalidate();//刷新折线图显示使数据生效
        chart2.setData(mLineData2);
        chart2.setAutoScaleMinMaxEnabled(true);
        chart2.invalidate();
        temp_tt=Float.parseFloat(save_data_final.substring(0,save_data_final.indexOf("C")));//从最新数据中截取出浮点格式温度值
        temp_ht=Float.parseFloat(save_data_final.substring(save_data_final.indexOf("C")+2,save_data_final.indexOf("%")));//从最新数据中截取出浮点格式湿度值
        d1.setBackGroundColor(Color.parseColor("#0B1557"));
        d1.setMode(0);
       // d1.setTitleText("温度","℃");
        d1.setScale(-15,5);
        d1.setNumber(save_data_final.substring(0,save_data_final.indexOf("C")));
        if(temp_tt<-15)
            temp_tt=-15;
        else if(temp_tt>45)
            temp_tt=45;
        d1.cgangePer((temp_tt+15)/(60f));
        d2.setBackGroundColor(Color.parseColor("#0B1557"));
        d2.setMode(1);
        //d2.setTitleText("湿度","%");
        d2.setScale(0,8);
        d2.setNumber(save_data_final.substring(save_data_final.indexOf("C")+2,save_data_final.indexOf("%")));
        d2.cgangePer(temp_ht/(100f));
    }
    /**
     * 以下相关调用折线图代码均来自互联网
     * */
    private void setChartStyle(LineChart mLineChart, LineData lineData,int color,String description) {
        // 是否在折线图上添加边框
        mLineChart.setDrawBorders(false);
        mLineChart.setDescription(description);
        // 如果没有数据的时候，会显示这个，类似listview的emtpyview
        mLineChart
                .setNoDataTextDescription("加载中...");

        mLineChart.setDrawGridBackground(false);
        mLineChart.setGridBackgroundColor(Color.CYAN);

        // 触摸
        mLineChart.setTouchEnabled(true);

        // 拖拽
        mLineChart.setDragEnabled(true);

        // 缩放
        mLineChart.setScaleEnabled(true);

        mLineChart.setPinchZoom(false);
        // 隐藏右边 的坐标轴
        mLineChart.getAxisRight().setEnabled(false);
        // 让x轴在下面
        mLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        mLineChart.getAxisRight().setEnabled(true); // 隐藏右边 的坐标轴(true不隐藏)
        mLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM); // 让x轴在下面
        mLineChart.getXAxis().setTextColor(Color.WHITE);
        mLineChart.getAxisRight().setTextColor(Color.WHITE);
        mLineChart.getAxisLeft().setTextColor(Color.WHITE);
        // 设置背景
        mLineChart.setBackgroundColor(color);

        // 设置x,y轴的数据
        mLineChart.setData(lineData);
        mLineChart.setAutoScaleMinMaxEnabled(true);
        mLineChart.invalidate();

    }
    private LineData makeLineData(int count,int color_fill,int type) {
        ArrayList<String> x = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            // x轴显示的数据
            x.add(i+"h");
        }
        ArrayList<Entry> y = new ArrayList<Entry>();
        // y轴的数据
        if(type==0) {
            for (int i = 0; i < count; i++) {
                float val = Float.parseFloat(Weather_log[i].substring(0,Weather_log[i].indexOf("C")));
                Entry entry = new Entry(val, i);
                y.add(entry);
            }
        }
        else{
            for (int i = 0; i < count; i++) {
                float val = Float.parseFloat(Weather_log[i].substring(Weather_log[i].indexOf("ug")+3,Weather_log[i].indexOf("uW")));
                Entry entry = new Entry(val, i);
                y.add(entry);
            }
        }

        // y轴数据集
        count++;
        LineDataSet mLineDataSet = new LineDataSet(y, " ");

        // 用y轴的集合来设置参数
        // 线宽
        mLineDataSet.setLineWidth(3.0f);

        // 显示的圆形大小
        mLineDataSet.setCircleSize(5.0f);

        // 折线的颜色
        mLineDataSet.setColor(Color.WHITE);

        // 圆球的颜色
        mLineDataSet.setCircleColor(Color.WHITE);

        // 设置mLineDataSet.setDrawHighlightIndicators(false)后，
        // Highlight的十字交叉的纵横线将不会显示，
        // 同时，mLineDataSet.setHighLightColor(Color.CYAN)失效。
        mLineDataSet.setDrawHighlightIndicators(true);

        // 按击后，十字交叉线的颜色
        mLineDataSet.setHighLightColor(Color.WHITE);

        // 设置这项上显示的数据点的字体大小。
        mLineDataSet.setValueTextSize(10.0f);

        // mLineDataSet.setDrawCircleHole(true);

        // 改变折线样式，用曲线。
         mLineDataSet.setDrawCubic(true);
        // 默认是直线
        // 曲线的平滑度，值越大越平滑。
         mLineDataSet.setCubicIntensity(0.2f);
        mLineDataSet.setDrawValues(false);

        // 填充曲线下方的区域，红色，半透明。
         mLineDataSet.setDrawFilled(true);
         mLineDataSet.setFillAlpha(128);
         mLineDataSet.setFillColor(color_fill);

        // 填充折线上数据点、圆球里面包裹的中心空白处的颜色。
        mLineDataSet.setCircleColorHole(Color.LTGRAY);

        // 设置折线上显示数据的格式。如果不设置，将默认显示float数据格式。
        mLineDataSet.setValueFormatter(new ValueFormatter() {


            @Override
            public String getFormattedValue(float value, Entry entry,
                                            int dataSetIndex, ViewPortHandler viewPortHandler) {
                // TODO Auto-generated method stub
                int n = (int) value;
                String s = "y:" + n;
                return s;
            }
        });

        ArrayList<LineDataSet> mLineDataSets = new ArrayList<LineDataSet>();
        mLineDataSets.add(mLineDataSet);

        LineData mLineData = new LineData(x, mLineDataSets);

        return mLineData;
    }

    private boolean parseIoTString(String str) throws Exception {
        try {
            JSONObject top = new JSONObject(str);
            JSONArray jarray = top.getJSONArray("deviceDataHistoryDTOs");
            JSONObject obj0 = jarray.getJSONObject(0);
            JSONObject obj = obj0.getJSONObject("data");
            data.setHumidity(Float.valueOf(obj.getString("humidity")));
            data.setPressure(Float.valueOf(obj.getString("pressure")));
            data.setTemperature(Float.valueOf(obj.getString("temperature")));
            data.setGas_r(Float.valueOf(obj.getString("Gas_R")));
            data.setUvv(Float.valueOf(obj.getString("UV")));
            data.setPm2_5(Float.valueOf(obj.getString("PM2_5")));

            data.setTime(obj0.getString("timestamp"));
        } catch (Exception e) {
            System.out.println("Format error in acquired data!");
            return false;
        }

        return true;
    }
/**
 * 获取节点信息
 * */
    public String queryNode(String accessToken, String devId, String startTime) throws Exception {
        String result = null;
        Map<String, String> header = new HashMap<>();
        header.put(Constant.HEADER_APP_KEY, APPID);
        header.put(Constant.HEADER_APP_AUTH, "Bearer" + " " + accessToken);
        header.put("Content-Type", "application/json");

        String myUrl = Constant.QUERY_DEVICE_HISTORY_DATA + "?deviceId=" + devId + "&gatewayId=" + devId + "&pageSize=1" + "&endTime=" + startTime;

        HttpGet request = new HttpGet(myUrl);
        addRequestHeader(request, header);

        HttpResponse response = executeHttpRequest(request);

        if (response.getStatusLine().getStatusCode() == 200) {
            //System.out.print(bodyQueryDevices.getStatusLine());
            result = convertStreamToString(response.getEntity().getContent());
            System.out.println("QueryDevices, response content:" + result);
        }
        return result;
    }

    private static void addRequestHeader(HttpUriRequest request,
                                         Map<String, String> headerMap) {
        if (headerMap == null) {
            return;
        }

        for (String headerName : headerMap.keySet()) {
            if (CONTENT_LENGTH.equalsIgnoreCase(headerName)) {
                continue;
            }
            String headerValue = headerMap.get(headerName);
            request.addHeader(headerName, headerValue);
        }
    }

    /**
     * Do certification and login.
     *
     * @return Return token if connection was accepted, else return null.
     * @throws Exception
     */
    public String cert() throws Exception {
        String appId = APPID;
        String secret = Constant.SECRET;
        String urlLogin = Constant.APP_AUTH;
        String token = null;

        // 服务器端需要验证的客户端证书
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        // 客户端信任的服务器端证书
        KeyStore trustStore = KeyStore.getInstance("BKS");

        InputStream ksIn = getApplicationContext().getAssets().open(Constant.SELFCERTPATH);
        InputStream tsIn = getApplicationContext().getAssets().open(Constant.TRUSTCAPATH);
        try {
            keyStore.load(ksIn, Constant.SELFCERTPWD.toCharArray());
            trustStore.load(tsIn, Constant.TRUSTCAPWD.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ksIn.close();
            } catch (Exception ignore) {
            }
            try {
                tsIn.close();
            } catch (Exception ignore) {
            }
        }

//        SSLContext sc = SSLContext.getInstance("TLS");
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
        keyManagerFactory.init(keyStore, Constant.SELFCERTPWD.toCharArray());
//        sc.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        SSLSocketFactory ssl = new SSLSocketFactory(keyStore, Constant.SELFCERTPWD, trustStore);
        ssl.setHostnameVerifier(new AllowAllHostnameVerifier());
//        ssl.getSocketFactory().setHostnameVerifier(new AllowAllHostnameVerifier());
        httpClient = new DefaultHttpClient();
        if (ssl != null) {
            Scheme sch = new Scheme("https", ssl, 443);
            httpClient.getConnectionManager().getSchemeRegistry().register(sch);
        }
        Map<String, String> param = new HashMap<>();
        param.put("appId", appId);
        param.put("secret", secret);

        /*List<NameValuePair> nvps = new LinkedList<NameValuePair>();
        Set<Map.Entry<String, String>> paramsSet = param.entrySet();
        for (Map.Entry<String, String> paramEntry : paramsSet) {
            nvps.add(new BasicNameValuePair(paramEntry.getKey(), paramEntry.getValue()));
        }*/
        List<NameValuePair> nvps = paramsConverter(param);
        HttpPost request = new HttpPost(urlLogin);
        request.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = executeHttpRequest(request);

//        KeyStore selfCert = KeyStore.getInstance("pkcs12");
//        selfCert.load(getApplicationContext().getAssets().open(Constant.SELFCERTPATH),
//                Constant.SELFCERTPWD.toCharArray());
////		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//        KeyManagerFactory kmf = KeyManagerFactory.getInstance("x509");
//        kmf.init(selfCert, Constant.SELFCERTPWD.toCharArray());
//
//
////		keyManagerFactory.init(clientKeyStore, "123456".toCharArray());
//
//        // 2 Import the CA certificate of the server,
//        KeyStore caCert = KeyStore.getInstance("bks");
//        caCert.load(getApplicationContext().getAssets().open(Constant.TRUSTCAPATH), Constant.TRUSTCAPWD.toCharArray());
//        TrustManagerFactory tmf = TrustManagerFactory.getInstance("x509");
//        tmf.init(caCert);
//
//        SSLContext sc = SSLContext.getInstance("TLS");
//        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
//        OkHttpClient client = new OkHttpClient();
//
//        if (sc != null) {
//            client.newBuilder().sslSocketFactory(sc.getSocketFactory());
//        }
//        client.newBuilder().hostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//        RequestBody requestBodyPost = new FormBody.Builder()
//                .add("appId", appId)
//                .add("secret", secret)
//                .build();
//        Request requestPost = new Request.Builder()
//                .url(urlLogin)
//                .post(requestBodyPost)
//                .build();
//
////        Response response = client.newCall(requestPost).execute();
//        client.newCall(requestPost).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.d("MainActivity","The response is failure");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final String string = response.body().string();
//                Log.d("MainActivity","The response is "+response.body().string());
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                    }
//                });
//            }
//        });

//        if (response.isSuccessful()) {
////            return response.body().string();
//            Log.d("MainActivity","The response is "+response.body().string());
//        } else {
//            throw new IOException("Unexpected code " + response);
//        }
        if (response.getStatusLine().getStatusCode() == 200) {
            try {
                String result = convertStreamToString(response.getEntity().getContent());
                JSONObject json = new JSONObject(result);
                token = json.getString("accessToken");
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        return token;
    }

    private HttpResponse executeHttpRequest(HttpUriRequest request) {
        HttpResponse response = null;
        try {
            response = httpClient.execute(request);
        } catch (Exception e) {
            System.out.println("executeHttpRequest failed.");
        } finally {
            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println("executeHttpRequest succeeded.");
               /* try {
                    result = convertStreamToString(response.getEntity().getContent());
                    System.out.println("result code" + result);
                 } catch (Exception e) {
                    e.printStackTrace();
                }*/
            } else {
                try {
                    Log.e(TAG, "CommonPostWithJson | response error | "
                            + response.getStatusLine().getStatusCode()
                            + " error :"
                            + EntityUtils.toString(response.getEntity()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
//            try {
//                System.out.println("aaa  ==  response -> " + response);
//                HttpEntity entity = response.getEntity();
//
//            } catch (Exception e) {
//                System.out.println("IOException: " + e.getMessage());
//            }
        }

        return response;
    }

    public static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    private List<NameValuePair> paramsConverter(Map<String, String> params) {
        List<NameValuePair> nvps = new LinkedList<NameValuePair>();
        Set<Map.Entry<String, String>> paramsSet = params.entrySet();
        for (Map.Entry<String, String> paramEntry : paramsSet) {
            nvps.add(new BasicNameValuePair(paramEntry.getKey(),
                    paramEntry.getValue()));
        }

        return nvps;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static String timeZoneTransfer(String time, String pattern, String nowTimeZone, String targetTimeZone) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT" + nowTimeZone));
        Date date;
        try {
            date = simpleDateFormat.parse(time);
        } catch (ParseException e) {
           // logger.error("时间转换出错。", e);
            return "";
        }
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT" + targetTimeZone));
        return simpleDateFormat.format(date);
    }
}
