package com.dji.GSDemo.GoogleMap;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
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
import com.kenai.jffi.Main;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GoHomeExecutionState;
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
    String contractAddr;
    BigInteger gasPrice = BigInteger.valueOf(660000000240L);
    BigInteger gasLimit = BigInteger.valueOf(200000L);
    Web3j web3;
    Dronechain droneChain;
    Credentials credentials;
    protected static final String TAG = "GSDemoActivity";
    private GoogleMap gMap; //구글맵
    //private GoogleMap mMap;
    //private Button locate, add, clear; // 버튼변수
    //private Button config, upload, start, stop; //버튼변수

    private boolean isAdd = true;

    private double droneLocationLat = 181, droneLocationLng = 181; //위도경도 초기값
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f; //고도 초기값
    private float mSpeed = 10.0f; //속도 초기값
    private List<Integer> veryShort = new ArrayList<Integer>();
    private List<Waypoint> waypointList = new ArrayList<>(); //웨이포인트 저장될 리스트

    //프로젝트 위해 선언한 변수
    private boolean makercheck = false;
    private MarkerOptions makerOptions = new MarkerOptions();
    int missionLengthFromChain = 0;
    int CurrentMissionnumber = 0;
    int missionStateFromChain; //0 : wait, 1: progress, 2: finish


    //tsp 위한 변수선언부 ↓
    private LatLng currentPOS = new LatLng(droneLocationLat, droneLocationLng); //현재위치
    private static double distance[][]; //두 목적지 사이의 거리 저장할 2차원배열
    private static float dist[] = new float[1];
    private int alltargetsize = 0;
    private List<Integer>path = new ArrayList<Integer>(); //경로를 저장할 list
    private List<LatLng>target = new ArrayList<LatLng>();
    private double fastestpath = 0.0;
    private int missionsize = 0;
    private int[] permutation;
    private boolean statecheck = false;
    private int[] shortestway;
    private double shortdistance =100000;
    private double tempshort = 0;
    private boolean startcheck = false;
    private boolean sendcheck = false;
    private int operatecount = 0;
    private GetMission getMission;
    //tsp 위한 변수선언부 ↑



    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;


    class WorkObject{ // 싱크로나이즈 위한 공유 객체 클래스
        public synchronized void methodA(){
            try{
                wait(); // ThreadA를 일시 정지 상태로 전환
            }
            catch(InterruptedException e){}
        }

        public synchronized void methodB(){
            notify(); // 일시 정지 상태에 있는 ThreadA를 실행 대기 상태로 전환
        }
    }
    WorkObject sharedObject = new WorkObject();


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

    private void initUI() { //인터페이스 화면

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
        /* --------- 지갑 로드---------- */
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
         getMission = new GetMission();
        /*contract 연동 ----------------------*/
        //버튼 리스너 함수 시작
        addListener();
    }
    private class RegistDrone extends AsyncTask<Void, String,String> {


        @Override
        protected  String doInBackground(Void... params) {
            String result = "??";
            if(startcheck) {
                registDrone();
                sendcheck = true;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

    }
    private void registDrone(){
        setResultToToast("가스를 전송합니다.");
        Double inputLat = droneLocationLat *1000000;
        Double inputLon = droneLocationLng *1000000;
        long inputLatLONG = inputLat.longValue();
        long inputLngLONG = inputLon.longValue();
        try{
            droneChain.registerDrone(BigInteger.valueOf(inputLatLONG),BigInteger.valueOf(inputLngLONG)).send();
            setResultToToast("가스전송완료");
            getMission.execute();
        }catch(Exception e){
            setResultToToast("예외처리3 : " + e );
        }
    }
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private class GetMission extends AsyncTask<Void, String,String> {
        @Override
        protected  String doInBackground(Void... params) {

            Tuple3<List<BigInteger>, List<BigInteger>, BigInteger> chainMissions;
            //droneChain.updateState(index,BigInteger.valueOf()).send(); //미션 번호의 스태이트를 바꾸겠다
            try {
                //미션길이 받아오는 코드 작업 여기 무한히 확인 스레드 체인 미션 길이가 크면 밑에서 try
                double inputLat, inputLng;
                while(true) {//0 : 대기중, 1: 수행중, 2:피니시, 3: 거절
                    missionLengthFromChain = Integer.parseInt(droneChain.returnMissionLength().send().toString());
                    if(missionLengthFromChain-1 >= CurrentMissionnumber) //
                    {

                        chainMissions = droneChain.getMission(credentials.getAddress(), BigInteger.valueOf(CurrentMissionnumber)).send();
                        missionStateFromChain = Integer.parseInt(chainMissions.getValue3().toString());
                        switch (missionStateFromChain) {
                            case 0 :
                                setResultToToast(CurrentMissionnumber + "번 미션 상태 : 대기");
                                setResultToToast(CurrentMissionnumber + "번 미션을 수행합니다.");
                                droneChain.updateState(BigInteger.valueOf(CurrentMissionnumber),BigInteger.valueOf(1)).send(); //상태를 1 (progress) 로 바꿈
                                setResultToToast("읽은 지갑주소 : " + credentials.getAddress());
                                inputLat = Double.valueOf(chainMissions.getValue1().get(0).toString()) / Double.valueOf("1000000");
                                inputLng = Double.valueOf(chainMissions.getValue2().get(0).toString()) / Double.valueOf("1000000");
                                waypointMissionBuilder = new WaypointMission.Builder();
                                makercheck = true;
                                setResultToToast("미션을 읽습니다");
                                for (int i = 0; i < chainMissions.getValue1().size(); i++) {
                                    inputLat = (Double.valueOf(chainMissions.getValue1().get(i).toString()) / Double.valueOf("1000000"));
                                    inputLng = (Double.valueOf(chainMissions.getValue2().get(i).toString()) / Double.valueOf("1000000"));
                                    //gMap.addMarker(new MarkerOptions().position(new LatLng(inputLat,inputLng)));
                                    Waypoint mWaypoint = new Waypoint(inputLat, inputLng, altitude);
                                    LatLng destination = new LatLng(inputLat, inputLng); // HOME
                                    makerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                                    //updateDroneLocation();
                                    //setResultToToast("3");
                                    runOnUiThread(new Runnable() { //읽은 목적지를 마킹
                                        @Override
                                        public void run() { //목적지 마킹
                                            {
                                                gMap.addMarker(new MarkerOptions().position(destination).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                            }
                                        }
                                    });

                                    waypointList.add(mWaypoint);
                                    missionsize++;
                                }
                                waypointList.add(new Waypoint(37.600943,126.865383,altitude));
                                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
                                //TPS 알고리즘 작업은 여기서 ↓

                                // 이 영역에 TPS 문제 Permutation 으로 모든 경우 구한 후 최단 경로 판단 및 최단 경로로 미션 빌더 재구성 코드 작업

                                //TPS 알고리즘 작업은 여기서 ↑
                                setResultToToast("목적지의 개수" + missionsize);
                                configWayPointMission();
                                uploadWayPointMission();
                                sharedObject.methodA();//미션완료되면 꺠운다 스레드
                                setResultToToast("종료 상태 전송");
                                droneChain.updateState(BigInteger.valueOf(CurrentMissionnumber),BigInteger.valueOf(2)).send(); //미션을 수행했으므로 2 보냄
                                CurrentMissionnumber++;
                                break;
                            case 1 :
                                setResultToToast(CurrentMissionnumber + "번 미션 상태 : 수행중");
                                setResultToToast(CurrentMissionnumber + "번 미션을 재수행합니다");
                                setResultToToast("읽은 지갑주소 : " + credentials.getAddress());
                                inputLat = Double.valueOf(chainMissions.getValue1().get(0).toString()) / Double.valueOf("1000000");
                                inputLng = Double.valueOf(chainMissions.getValue2().get(0).toString()) / Double.valueOf("1000000");
                                waypointMissionBuilder = new WaypointMission.Builder();
                                makercheck = true;
                                for (int i = 0; i < chainMissions.getValue1().size(); i++) {
                                    inputLat = (Double.valueOf(chainMissions.getValue1().get(i).toString()) / Double.valueOf("1000000"));
                                    inputLng = (Double.valueOf(chainMissions.getValue2().get(i).toString()) / Double.valueOf("1000000"));
                                    //gMap.addMarker(new MarkerOptions().position(new LatLng(inputLat,inputLng)));
                                    Waypoint mWaypoint = new Waypoint(inputLat, inputLng, altitude);
                                    LatLng destination = new LatLng(inputLat, inputLng); // HOME
                                    makerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                                    //updateDroneLocation();
                                    //setResultToToast("3");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() { //목적지 마킹
                                            {
                                                gMap.addMarker(new MarkerOptions().position(destination).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                            }
                                        }
                                    });

                                    waypointList.add(mWaypoint);
                                    missionsize++;
                                }
                                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
                                //TPS 알고리즘 작업은 여기서 ↓

                                // 이 영역에 TPS 문제 Permutation 으로 모든 경우 구한 후 최단 경로 판단 및 최단 경로로 미션 빌더 재구성 코드 작업

                                //TPS 알고리즘 작업은 여기서 ↑
                                waypointList.add(new Waypoint(37.600943,126.865383,altitude));
                                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
                                setResultToToast("목적지 개수" + waypointList.size());
                                configWayPointMission();
                                uploadWayPointMission();
                                sharedObject.methodA();//미션완료되면 꺠운다
                                setResultToToast("종료 상태 전송");
                                droneChain.updateState(BigInteger.valueOf(CurrentMissionnumber),BigInteger.valueOf(2)).send(); //미션을 수행했으므로 2 보냄
                                CurrentMissionnumber++;
                                break;
                            case 2 :
                                setResultToToast(CurrentMissionnumber + "번 미션 상태 : 완료");
                                CurrentMissionnumber++;
                                break;
                            case 3 :
                                setResultToToast(CurrentMissionnumber + "번 미션 상태 : 거절");
                                CurrentMissionnumber++;
                                break;
                        }

                    } else CurrentMissionnumber = 0;
                Thread.sleep(1000);
                }
            } catch (Exception e) {

            }
            return "리턴스트링";
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

    }
    //TPS를 브루트포스해서 최단거라를 구하는 함수 ↓
    //TPS를 브루트포스해서 최단경로를 구하는 함수가 되도록 고민 필요↓
    public double TPS(List<Integer> path, boolean visited[], double currentLength){ //path:경로,vistited:방문여부,currentLength:경로거리. 재귀로 호출되며 돈다.
        int len = path.size(); //경로 사이즈 맨처음값은 1이고 목적지가 3개면 풀스캔했을때 사이즈가 5가되야함 (0->1->2->3->0)
        if(len == alltargetsize) return currentLength + distance[path.get(0)][path.get(len -1)]; //마지막 번호랑 거리
        double shortpath = 1000000.0;//최단거리가 될 변수, 임의로 큰 거리 값
        for(int next = 0; next < alltargetsize; ++next) //다음 목적지 검사
        {
            if(visited[next]) continue; //방문했던 노드면 그냥 진행 아니면 아래 코드 진행
            int here = path.size();//현재 여기다;
            path.add(next); //경로에 `목적지 추가
            visited[next] = true; //방문으로 변경
            double shortestpath = TPS(path,visited,currentLength + distance[here][here]);
            shortpath = Math.min(shortpath, shortestpath);

            visited[next] = false;
            path.remove(here);
        }
        return shortpath;
    }

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
                    //mFlightController.confirmLanding();
                    /*if(djiFlightControllerCurrentState.isLandingConfirmationNeeded())
                    {
                        setResultToToast("고투그라운드!");
                        //mFlightController.
                        djiFlightControllerCurrentState.setGoHomeExecutionState(GoHomeExecutionState.GO_DOWN_TO_GROUND);
                    }*/
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
            setResultToToast("미션 완료! " + (error == null ? "Success!" : error.getDescription()));
           // waypointMissionBuilder = null;
            //waypointMissionBuilder = new WaypointMission.Builder();
            runOnUiThread(new Runnable() {
                @Override
                public void run() { //목적지 마킹
                    {
                        try {
                            gMap.clear(); //목적지를 다 돌았으므로 목적지 마커 삭제!
                            LatLng kau = new LatLng(37.600943, 126.865383); // HOME 송골매탑 입구
                            gMap.addMarker(new MarkerOptions().position(kau).title("송골매 광장 입구"));
                            gMap.moveCamera(CameraUpdateFactory.newLatLng(kau));
                            waypointList.clear(); //웨이포인트 클리어
                            target.clear(); //임시저장 타겟클리어
                            waypointMissionBuilder = null; //미션빌더 클리어
                            //getWaypointMissionOperator().destroy();
                            getWaypointMissionOperator().clearMission();//미션 오퍼레이터에 있던 미션 클리어
                            //깨워야함
                            sharedObject.methodB();
                            //getMission.notifyAll();
                            //this.notifyAll();
                            //updateWaypointMissionState();
                            //겟미션 웨이껍!
                            //droneChain.updateState(BigInteger.valueOf(CurrentMissionnumber), BigInteger.valueOf(1)).send();

                        }
                        catch (Exception e) {
                        }
                    }
                }
            });

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
    public void onMapClick(LatLng point) { //맵 클릭시 위도경도 읽어옴 . 클릭을 찍을때마다 이 함수가 실행된다고 보면됌. 맵클릭으로 테스트할 때 쓰는 코드
        if (isAdd == true){
            markWaypoint(point);
            //Waypoint 자료형은 위도, 경도, 고도
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude); //웨이포인트 자료형은 위도, 경도, 고도
            //Add Waypoints to Waypoint arraylist; //웨이포인트를 웨이포인트 어레이 리스트에 넣는다.
            if (waypointMissionBuilder != null) {//널이 아니면 (즉 미션 빌더가 존재하면)
                    waypointList.add(mWaypoint);//웨이포인트 리스너에 포인팅된 웨이포인트 추가
                    waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size()); //웨이포인트 리스트를 올려서
                setResultToToast("size :" + waypointList.size());
                  if(waypointList.size() == 3) { //목적지 3개로 테스팅, 5개로 테스팅하고싶을 땐 5를 이 조건문에 5를 넣으면 된다.
                     /* alltargetsize = waypointList.size() + 1; //목적지 개수. +1을 하는 이유는 home 추가
                      distance = new double[alltargetsize][alltargetsize]; //목적지 갯수만큼 동적할당,각 노드와 노드 사이의 거리 저장할 2차원 배열
                      //boolean visited[] = new boolean[alltargetsize];//방문했는지 안했는지 체크. tps 함수 쓸땐 필요한 변수. 퍼뮤테이션에선 안쓸거임


                      //------0번은 홈 1번부터는 목적지 ~~ 0:home ,  1,2,3 ....:  target -------//
                      target.add(new LatLng(37.37.599321, 126.126.863507)); //제일 처음 인덱스는 HOME을 넣음.
                      for (Waypoint i : waypointList) {
                          target.add(new LatLng(i.coordinate.getLatitude(), i.coordinate.getLongitude()));
                      }//타겟 복사
                      setResultToToast("타겟 복사 완료");
                      setResultToToast("타겟은" + target.get(1).longitude);

                     for(int i=0; i<distance.length;i++)
                          for(int j=0; j<distance[i].length;j++) {
                              Location.distanceBetween(target.get(i).latitude, target.get(i).latitude, target.get(j).latitude, target.get(j).latitude, dist); //dist에 결과값저장
                              distance[i][j] = dist[0];
                          }
                      //순열로 모든 경우의 수를 구한다↓
                      permutation = new int[waypointList.size()]; //목적지 개수만큼
                      shortestway = new int[waypointList.size()];
                      for (int i = 0; i < waypointList.size(); i++)
                          permutation[i] = i + 1; //목적지가 3개면 1,2,3 (타겟번호)

                      do { //모든 경우(경로)에 대한 최단거리, 최단경로를 구한다↓
                          for (int i = 0; i <= permutation.length; i++) { //포문이 한번 돌때마다 한 경우임.
                             if(i==0) tempshort = distance[0][permutation[i]];
                             else if(i>0 && i<permutation.length) tempshort = tempshort + distance[permutation[i-1]][permutation[i]];
                             else if(i == permutation.length) tempshort = tempshort + distance[permutation[i-1]][0]; //다시 HOME으로 돌아옴. 헤밀턴이니까.
                          } //한 경우에 대한 거리 뽑음.
                          setResultToToast("경우의수" + permutation[0]+permutation[1]+permutation[2]);
                          if(tempshort<shortdistance) {// 2 3 4 1 이 나옴 why?
                              shortdistance = Math.min(tempshort, shortdistance);
                              for (int k = 0; k < waypointList.size(); k++) {
                                  //setResultToToast(k + "번째 펴뮤테이션은" + permutation[k]);
                                  shortestway[k] = permutation[k];
                              }
                          }
                      } while (next_permutation(permutation, 0, waypointList.size()));//모든 경우의수를 뽑아냄setResultToToast("최종적으로 최단 거리 값은 : " + shortdistance);
                    waypointList.clear();//기존 경로 리스트를 초기화. 이 리스트에 최단경로 순서로 넣을것임

                    setResultToToast("웨이포인트 리스트 클리어완료");
                    for(int z=0; z<shortestway.length ;z++) setResultToToast("최종 최단경로 : " + shortestway[z]);

                    for(int i =0; i<(target.size()-1); i++)
                    {
                        waypointList.add(new Waypoint(target.get(shortestway[i]).latitude,target.get(shortestway[i]).longitude,altitude));
                    }

                        //\waypointList.add(new Waypoint(i.get(path.get(i)).latitude,target.get(path.get(i)).longitude,altitude)

                    setResultToToast("최단경로 목적지 리스트가 생성되었습니다");

                    //새로 waypointList 생성*/
                    waypointList.add(new Waypoint(37.600943,126.865383,altitude));
                    waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
                    setResultToToast("목적지 개수" + waypointList.size());
                    //현재 위치와 비교해서 wayPointlist에 있는 순서를 변경

                    configWayPointMission();
                    uploadWayPointMission();
                    //startWaypointMission();
                }
            }else //waypointMissionBuilder 가 널이면. 즉 빌더가 없으면
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                setResultToToast("미션빌더 생성");
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }
    /*public static boolean next_permutation(int[] arr, int s, int e){
        //arr[i-1] < arr[i]를 만족하는 가장 큰 i
        int i=e-1;
        for(; i>s; i--){
            if(arr[i-1] >arr[i])
                continue;
            else
                break;
        }
        if(i==0) return false;

        //i<=j를 만족하고, A[j]>A[i-1]를 만족하는 가장 큰 j
        int j=e-1;
        for(; j>=i; j--){
            if(arr[j]<arr[i-1])
                continue;
            else
                break;
        }

        //A[i-1], A[j] swap
        int tmp=arr[i-1];
        arr[i-1]=arr[j];
        arr[j]=tmp;
        //1234567
        //i부터 끝까지 뒤집음
        j=e-1;
        while(i<j){
            int temp=arr[i];
            arr[i]=arr[j];
            arr[j]=temp;
            i++; j--;
        }
        return true;
    }*/
    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    /*synchronized (this){
          // wait();//내용
       }*/
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
                    if(droneLocationLat != 181 && droneLocationLng!= 181 && statecheck == false)
                    {
                        statecheck = true;
                        try {
                         RegistDrone rgDrone = new RegistDrone();
                            rgDrone.execute();
                            setResultToToast("현재 드론의 위치를 체인에 등록했습니다.");
                            }
                            catch (Exception e) {
                                setResultToToast(e.toString());
                            }
                    }
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
        }
    }
    private void cameraUpdate(){
        // LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        LatLng pos = new LatLng(37.600943,  126.865383);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);

    }
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
        mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
        mHeadingMode = WaypointMissionHeadingMode.AUTO;
        altitude = 10.0f;
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
        //빌드값을 로드해서 에러가 없으면(즉 널이면) 석세스.
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

        LatLng kau = new LatLng(37.600943, 126.865383); // HOME 송골매탑 입구
        gMap.addMarker(new MarkerOptions().position(kau).title("송골매 광장 입구"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(kau));

        cameraUpdate();
        String s = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/UTC--2018-12-02T13-45-27.588Z--91a06552824285f6777bfc672e8081aacb1baa94";
        //Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT);
       // setResultToToast("경로값은 : " + s);
        /* --------- 지갑 로드---------- */
        try {
            credentials = WalletUtils.loadCredentials("whdtjfwhdtjf",s);
            //Toast.makeText(MainActivity.this,credentials.getAddress(),Toast.LENGTH_SHORT);
            //setResultToToast("블록체인 ");
        } catch (IOException e) {
            e.printStackTrace();
            setResultToToast("예외처리1");
        } catch (CipherException e) {
            e.printStackTrace();
            setResultToToast("예외처리2");
        }
        Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT);
        //contract 연동 ----------------------
        contractAddr= "0x0DC90a09D3190111dCf3bfc22dC267A2422662dd";
        web3 = Web3jFactory.build(new HttpService("https://rinkeby.infura.io/v3/faa0a68fa9bc43b0a56c79f82069e283"));
        droneChain = Dronechain.load(contractAddr,web3,credentials,gasPrice,gasLimit);
        startcheck = true;
    }


}
