package com.dji.GSDemo.GoogleMap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.web3j.protocol.Web3j;

//tsp 위한 라이브러리 선언부 ↓

//tsp 위한 라이브러리 선언부 ↑

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.common.error.DJIError;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    protected static final String TAG = "GSDemoActivity";
    private GoogleMap gMap; //구글맵

    private Button locate, add, clear; // 버튼변수
    private Button config, upload, start, stop; //버튼변수

    private boolean isAdd = true;

    private double droneLocationLat = 181, droneLocationLng = 181; //위도경도 초기값
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f; //고도 초기값
    private float mSpeed = 10.0f; //속도 초기값

    private List<Waypoint> waypointList = new ArrayList<>(); //웨이포인트 저장될 리스트

    //tsp 위한 변수선언부 ↓
    private LatLng currentPOS = new LatLng(droneLocationLat, droneLocationLng); //현재위치
    private static double distance[][]; //두 목적지 사이의 거리 저장할 2차원배열
    private static float dist[] = new float[1];
    private int targetsize = 0;
    private List<Integer>path = new ArrayList<Integer>(); //경로를 저장할 list
    private List<LatLng> target;
    private double fastestpath = 0.0;
    //tsp 위한 변수선언부 ↑

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;


    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mReceiver);
        removeListener();
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() { //유저인터페이스 레이아웃

        //버튼변수
       /* locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        // 버튼 이벤트 리스너
        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);*/

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        //드론 등록이 완료되면 ui 실행

        initUI(); //UI 실행
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //버튼 리스너 함수 시작
        addListener();
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() { //현재 쥐피에스값 받기

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    updateDroneLocation();
                }
            });
        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null){
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) { //웨이포인트를 다운로드

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) { //웨이포인트를 업로드

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null){
                instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return instance;
    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object //맵 클릭

    }
    /*for(LatLng a : target){ //리스트 포문
                  }*/
    @Override
    public void onMapClick(LatLng point) { //맵 클릭시 위도경도 읽어옴 . 클릭을 찍을때마다 이 함수가 실행된다고 보면됌.
        if (isAdd == true){
            markWaypoint(point);
            //Waypoint 자료형은 위도, 경도, 고도
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude); //웨이포인트 자료형은 위도, 경도, 고도
            //Add Waypoints to Waypoint arraylist; //웨이포인트를 웨이포인트 어레이 리스트에 넣는다.
            if (waypointMissionBuilder != null) {//널이 아니면
                waypointList.add(mWaypoint);//웨이포인트 리스너에 포인팅된 웨이포인트 추가
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size()); //웨이포인트 리스트를 올려서
                if(waypointList.size() == 3)
                {
                    /*targetsize = waypointList.size() + 1; //목적지 개수. +1을 하는 이유는 home 추가
                    distance = new double[targetsize][targetsize]; //목적지 갯수만큼 동적할당
                    boolean visited [] = new boolean[targetsize];//방문했는지 안했는지 체크
                    //목적지들 위도 경도 저장할 객체
                    //0번은 홈 1번부터는 목적지 ~~ 0:home and else target
                    for(int i=0; i<targetsize; i++)
                    {
                        if(i==0) target.add(new LatLng(37.5993,126.865147)); //Home 위도,경도
                        else target.add(new LatLng(waypointList.get(i).coordinate.getLatitude(),waypointList.get(i).coordinate.getLongitude())); //목적지들
                    }
                    for(int i=0; i<distance.length;++i)
                        for(int j=0; j<distance[i].length;++j) {
                            Location.distanceBetween(target.get(i).latitude, target.get(i).latitude, target.get(j).latitude, target.get(j).latitude, dist);
                            distance[i][j] = dist[0];
                            //Location.distanceBetween(위도,경도,위도,경도,저장될변수); 위도 경도주면 거리 알려주는 함수
                        }
                    waypointList.clear();//복사했으니까 클리어
                    for(int k = 0; k<targetsize-1;k++) waypointMissionBuilder.removeWaypoint(k);
                    visited[0] = true; //홈을 시작점으로 (홈은 무조건 방문이니까)
                    path.add(0); //각 목적지간 경로 길이 홈을 추가
                    fastestpath = TPS(path,visited,0); // fastestpath는 최단경로
                    waypointList.clear();
                    for(int i=0; i<targetsize;i++){
                        waypointList.add(new Waypoint(target.get(path.get(path.get(i))).latitude,target.get(path.get(path.get(i))).longitude,altitude));
                    } //새로 waypointList 생성
                    waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
                    //현재 위치와 비교해서 wayPointlist에 있는 순서를 변경
                    */
                    configWayPointMission();
                    uploadWayPointMission();
                    //startWaypointMission();
                }
            }else //waypointMissionBuilder 가 널이면. 즉 빌더가 없으면
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }
   /* public void setting()
    {
        @Override
        public void onCheckedChanged()
        {
            mSpeed = 5.0f;
        }
    }*/


    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng); //드론의 현재 위치를 위한 변수 pos
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos); //pos
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft)); //드론의 아이콘
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }
                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) { //gps 채킹을 계속 하면서 위도경도를 받아옴
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markWaypoint(LatLng point){ //위도경도를 포인팅
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);//마크를 표시
    }

    @Override
    public void onClick(View v) { // res폴더 각버튼에 대한 이벤트
        switch (v.getId()) {
            /*case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear:{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gMap.clear();
                    }

                });
                waypointList.clear();
                waypointMissionBuilder.waypointList(waypointList);
                updateDroneLocation();
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.upload:{ //업로드 버튼 눌렀을떄 최단경로로 좌표값 정리
               //여기에 tsp 부르트포스 작업
                uploadWayPointMission();
                break;
            }
            case R.id.start:{
                startWaypointMission();
                break;
            }
            case R.id.stop:{
                stopWaypointMission();
                break;
            }
            default:
                break;*/
        }
    }
    //TPS를 브루트포스해서 최단경로를 구하는 함수 ↓
    public double TPS(List<Integer> path, boolean visited[], double currentLength){ //path:경로,vistited:방문여부,currentLength:경로거리
        int len = path.size();
        if(len == targetsize) return currentLength + distance[path.get(0)][path.get(len -1)]; //
        double shortpath = 1000000.0;//최단거리가 될 변수, 임의로 큰 거리 값
        for(int next = 0; next < targetsize; ++next) //다음 목적지 검사
        {
            if(visited[next]) continue; //방문했던 노드면 그냥 진행 아니면 아래 코드 진행
            int here = path.size();//현재 여기다;
            path.add(next); //경로에 목적지 추가
            visited[next] = true; //방문으로 변경
            double shortestpath = TPS(path,visited,currentLength + distance[here][here]);
            shortpath = Math.min(shortpath,shortestpath); //최단경로로
            visited[next] = false;
            path.remove(here);
        }
        return shortpath;
    }

    private void cameraUpdate(){
        // LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        LatLng pos = new LatLng(37.5993, 126.865147);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {

            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }

    /*private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude); //고도를 입력받음
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed); //스피드
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished); //액션 선택
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading); //

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }*/
    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){ //config 값 넣는 함수

        mSpeed =5.0f;
        mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
        mHeadingMode = WaypointMissionHeadingMode.AUTO;
        altitude = 50.0f;
        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude; //각 미션마다 고도를 저장할 수 있음
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build()); //빌드가 성공해서 null이면 업로드 성공
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }
    }

    private void uploadWayPointMission(){

            getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) { //미션업로드
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                    startWaypointMission();
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

   /* private missionLoop(){
        //미션 체크

        //미션 수신

        //가능 여부 판단
    }*/
    @Override
    public void onMapReady(GoogleMap googleMap) { //맵
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }

        LatLng kau = new LatLng(37.5993, 126.865147); // HOME
        gMap.addMarker(new MarkerOptions().position(kau).title("Marker in KAU"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(kau));
        cameraUpdate();
        //missionLoop();
    }

}