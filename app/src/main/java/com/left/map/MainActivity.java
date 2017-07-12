package com.left.map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.poisearch.PoiSearch.OnPoiSearchListener;
import com.amap.api.services.poisearch.PoiSearch.SearchBound;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.RouteSearch.FromAndTo;
import com.amap.api.services.route.RouteSearch.OnRouteSearchListener;
import com.amap.api.services.route.RouteSearch.WalkRouteQuery;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;


public class MainActivity extends Activity implements OnPoiSearchListener, AMapLocationListener,
        OnRouteSearchListener {

    //声明mLocationOption对象
    public AMapLocationClientOption mLocationOption;
    public AMapLocationClient mlocationClient;
    private AMapNavi mapNavi;
    private PoiSearch.Query query;// Poi查询条件类
	private PoiSearch poiSearch;
	private List<PoiItem> poiItems;// poi数据
	private PoiResult poiResult; // poi返回的结果
	private LatLonPoint lp;// 记录用户位置
	private FromAndTo fromAndTo;//用以导航
	private RouteSearch routeSearch;
	private WalkRouteResult walkRouteResult;//walkroute返回的结果
	private WalkRouteQuery query2;//步行路径查询条件类
	private Camera camera;   
    private boolean preview  = false ;  
    private RelativeLayout top;
    private ImageView choose;
    private ImageView back;
    private TextView head;
    private SurfaceView surfaceView ;
    private RelativeLayout nave;
    private TextView navepath;
    private TextView stop;
    private LinearLayout mark;
    private TextView busname;
    private TextView busdistance;
    private LinearLayout path;
    private TextView pathname;
    private TextView pathdistance;
    private TextView pathcontent;
    private TextView pathtime;
    private ImageView naveimg;
    private boolean isChoose;//用以记录是否开启导航
    private int whatbus;//用以确认导航时的公交站点
    private SensorManager sensorManager;  //获取传感器数据，用以动态改变naveimg
    private MySensorEventListener sensorEventListener; 
    private String Cloum;
    private boolean only;//用以解决无数据时反复出现提示信息的问题
    //两点方位角
	private double pab;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();  
        Cloum = intent.getStringExtra("Cloum"); 
        //获取感应器管理器    
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
        sensorEventListener = new MySensorEventListener();  
        surfaceView=(SurfaceView) findViewById(R.id.surfaceView); 
        top=(RelativeLayout) findViewById(R.id.top);
        nave=(RelativeLayout) findViewById(R.id.nave);
        mark=(LinearLayout) findViewById(R.id.mark);
        path=(LinearLayout) findViewById(R.id.path);
        naveimg=(ImageView) findViewById(R.id.naveimg);
        //获取到数据后再显示，刚开始不可见
        mark.setVisibility(View.INVISIBLE);
        path.setVisibility(View.INVISIBLE);
        naveimg.setVisibility(View.INVISIBLE);
        choose=(ImageView) top.findViewById(R.id.choose);
        back=(ImageView) top.findViewById(R.id.back);
        head=(TextView) top.findViewById(R.id.head);
        navepath=(TextView) nave.findViewById(R.id.navepath);
        stop=(TextView) nave.findViewById(R.id.stop);
        busname=(TextView) mark.findViewById(R.id.busname);
        busdistance=(TextView) mark.findViewById(R.id.busdistance);
        pathname=(TextView) path.findViewById(R.id.pathname);
        pathdistance=(TextView) path.findViewById(R.id.pathdistance);
        pathcontent=(TextView) path.findViewById(R.id.pathcontent);
        pathtime=(TextView) path.findViewById(R.id.pathtime);
        surfaceView.getHolder().addCallback(new SurfaceViewCallback());  
        head.setText(Cloum);
        only=true;
        /**
         * 地图定位初始化
         */
        mapNavi = AMapNavi.getInstance(this);
        mlocationClient = new AMapLocationClient(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位监听
        mlocationClient.setLocationListener(this);
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //设置定位参数
        mlocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        mlocationClient.startLocation();
        choose.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(only){
				Intent intent=new Intent(MainActivity.this,ChooseActivity.class);
				//将poi数据传递给详情activity
                    intent.putExtra("poiItems", (Serializable) poiItems);
                    startActivityForResult(intent,0);
				}
				else
					Toast.makeText(MainActivity.this, "附近2000米范围内没有相关数据", Toast.LENGTH_LONG).show();
			}
		});
        back.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
        mark.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				isChoose=!isChoose;
				if(isChoose){
					busdistance.setBackgroundColor(Color.parseColor("#ff0099"));
					Toast.makeText(MainActivity.this, "开始导航", Toast.LENGTH_SHORT).show();
					top.setVisibility(View.INVISIBLE);
					naveimg.setVisibility(View.VISIBLE);
					startNave();
                } else{
					busdistance.setBackgroundColor(Color.parseColor("#00ff99"));
					top.setVisibility(View.VISIBLE);
					naveimg.setVisibility(View.INVISIBLE);
					pathtime.setText("点击图标开启导航");
			        pathdistance.setText(poiItems.get(whatbus).getDistance()+"米");
			        busdistance.setText(poiItems.get(whatbus).getDistance()+"米");
				}
			}
		});
        stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				isChoose=false;
				busdistance.setBackgroundColor(Color.parseColor("#00ff99"));
				top.setVisibility(View.VISIBLE);
				naveimg.setVisibility(View.INVISIBLE);
				pathtime.setText("点击图标开启导航");
		        pathdistance.setText(poiItems.get(whatbus).getDistance()+"米");
		        busdistance.setText(poiItems.get(whatbus).getDistance()+"米");
			}
		});
    }

 
    @Override    
    protected void onResume()     
    {    
        //获取方向传感器    
        Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);  
        //传感器监听
        sensorManager.registerListener(sensorEventListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);    
        //获取加速度传感器    
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);    
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);    
        super.onResume();    
    }    
    
    /**
     * 此方法需存在
     */
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    /**
	 * 开始进行poi搜索
	 */
	protected void doSearchQuery() {
		// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
		query = new PoiSearch.Query("", Cloum, "");
		query.setPageSize(30);// 设置每页最多返回多少条poiitem
		query.setPageNum(0);// 设置查第一页
		if (lp != null) {
			poiSearch = new PoiSearch(this, query);
			poiSearch.setOnPoiSearchListener(this);
			// 设置搜索区域为以lp点为圆心，其周围2000米范围
			poiSearch.setBound(new SearchBound(lp, 2000, true));
			poiSearch.searchPOIAsyn();// 异步搜索
		}
	}
	
	/**
	 * POI搜索回调方法
	 */
	@Override
	public void onPoiSearched(PoiResult result, int rCode) {
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {
				if (result.getQuery().equals(query)) {
					poiResult = result;
					// 取得第一页的poiitem数据，页数从数字0开始
					poiItems = poiResult.getPois();
					if ( poiResult.getPois().size()== 0){
						if(only){
							Toast.makeText(this, "附近2000米范围内没有相关数据", Toast.LENGTH_LONG).show();
							only=false;
						}
					}	  
					else{
						busname.setText(poiItems.get(0).toString());
						//doSearchPoiDetail(poiItems.get(0).getPoiId());
						busdistance.setText(poiItems.get(0).getDistance()+"米");
						pathname.setText(poiItems.get(0).toString());
						pathdistance.setText(poiItems.get(0).getDistance()+"米");
						pathcontent.setText(poiItems.get(0).getSnippet());
						//获取到数据后再显示
				        mark.setVisibility(View.VISIBLE);
				        path.setVisibility(View.VISIBLE);
					}
						
				}
			} 
		} 
	}

	/**
	 * POI详情回调
	 */
	@Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {
        if (i == 1000) {
            if (poiItem != null) {
                // 搜索poi的结果
                String sb = poiItem.getSnippet();
                Toast.makeText(this, sb, Toast.LENGTH_LONG).show();
            }
		}
	}


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
            //获取位置信息
            Double geoLat = aMapLocation.getLatitude();
            Double geoLng = aMapLocation.getLongitude();
            lp = new LatLonPoint(geoLat, geoLng);
            doSearchQuery();
        }
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            //resultCode为回传的标记，我在ChooseActivity中回传的是RESULT_OK
            case RESULT_OK:
                //data为B中回传的Intent
		    Bundle b=data.getExtras();
		    //str即为回传的值
		    String str=b.getString("listenB");
		    whatbus=Integer.parseInt(str);
		    busname.setText(poiItems.get(whatbus).toString());
			busdistance.setText(poiItems.get(whatbus).getDistance()+"米");
			busdistance.setBackgroundColor(Color.parseColor("#00ff99"));
			pathname.setText(poiItems.get(whatbus).toString());
			pathdistance.setText(poiItems.get(whatbus).getDistance()+"米");
			pathcontent.setText(poiItems.get(whatbus).getSnippet());
			break;
		default:
		break;
		}
	}

    //开始路线规划，进行导航
	public void startNave() {
		fromAndTo=new FromAndTo(lp, poiItems.get(whatbus).getLatLonPoint());
		routeSearch = new RouteSearch(this);
		routeSearch.setRouteSearchListener(this);
		query2 = new WalkRouteQuery(fromAndTo,0);
		routeSearch.calculateWalkRouteAsyn(query2);
		NaviLatLng naviLatLng=new NaviLatLng(poiItems.get(whatbus).getLatLonPoint()
				.getLatitude(), poiItems.get(whatbus).getLatLonPoint().getLongitude());
		mapNavi.calculateWalkRoute(naviLatLng);
		//计算方位角
        pab=gps2d(naviLatLng.getLatitude(),naviLatLng.getLongitude(),lp.getLatitude(),lp.getLongitude());
		//naviFlag为AMapNavi.GPSNaviMode表示真实导航，naviFlag为AMapNavi.EmulatorNaviMode表示模拟导航
		AMapNavi.getInstance(this).startNavi(AMapNavi.GPSNaviMode);
	}

    @Override
    public void onBusRouteSearched(BusRouteResult arg0, int arg1) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult arg0, int arg1) {

    }

    @Override
    // 路径规划中步行模式回调函数
    public void onWalkRouteSearched(WalkRouteResult result, int rCode) {
        if (rCode == 1000 && result != null && result.getPaths() != null &&
                result.getPaths().size() > 0) {
            walkRouteResult = result;
            WalkPath walkPath = walkRouteResult.getPaths().get(0);
            navepath.setText(walkPath.getSteps().get(0).getInstruction());
            pathtime.setText("大约" + (int) (walkPath.getDuration() / 60) + "分钟");
            pathdistance.setText(walkPath.getDistance() + "米");
            busdistance.setText(walkPath.getDistance() + "米");
        }
    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }

    //根据经纬度计算方位角pab,其中lat_a, lng_a是A的纬度和经度； lat_b, lng_b是B的纬度和经度
    private double gps2d(double lat_a, double lng_a, double lat_b, double lng_b) {
        double d = 0;
        lat_a = lat_a * Math.PI / 180;
        lng_a = lng_a * Math.PI / 180;
        lat_b = lat_b * Math.PI / 180;
        lng_b = lng_b * Math.PI / 180;
        d = Math.sin(lat_a) * Math.sin(lat_b) + Math.cos(lat_a) * Math.cos(lat_b) * Math.cos(lng_b - lng_a);
        d = Math.sqrt(1 - d * d);
        d = Math.cos(lat_b) * Math.sin(lng_b - lng_a) / d;
        d = Math.asin(d) * 180 / Math.PI;
        return d;
    }

    private final class SurfaceViewCallback implements Callback {
        /**
         * surfaceView 被创建成功后调用此方法
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            /*
             * 在SurfaceView创建好之后 打开摄像头
             * 注意是 android.hardware.Camera
             */
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            /*
             * This method must be called before startPreview(). otherwise surfaceview没有图像
             */
            try {
                camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
            preview = true;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        /**
         * SurfaceView 被销毁时释放掉 摄像头
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (camera != null) {
                /* 若摄像头正在工作，先停止它 */
                if (preview) {
                    camera.stopPreview();
                    preview = false;
                }
                //如果注册了此回调，在release之前调用，否则release之后还回调，crash
                camera.setPreviewCallback(null);
                camera.release();
            }
        }
    }

    private final class MySensorEventListener implements SensorEventListener {
        //可以得到传感器实时测量出来的变化值
        @SuppressLint("NewApi")
        @Override
        public void onSensorChanged(SensorEvent event) {
            //得到方向的值
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {

            	//方向角
            	float a=event.values[0];
            	//俯仰角
            	float b=event.values[1];
            	//翻转角
            	float c=event.values[2];
            	pathname.setText("方向角："+a);
            	pathcontent.setText("俯仰角："+b);
            	//手机先得竖起来，这时候翻转角的数据起不到具体作用
            	if(b>-100 && b<-80){
                  	pathdistance.setText("方位角："+pab);
            		naveimg.setX(a);
            		naveimg.setY(a);
            	}
            }
            //得到加速度的值
            else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                //float x = event.values[SensorManager.DATA_X];
                //float y = event.values[SensorManager.DATA_Y];
                //float z = event.values[SensorManager.DATA_Z];
            }

        }

        //重写变化
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
