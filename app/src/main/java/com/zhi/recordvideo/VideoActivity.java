package com.zhi.recordvideo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

/**
 * 描述：录像姐main控制
 * 作者：shaobing
 * 时间： 2016/11/28 17:08
 */
public class VideoActivity extends Activity implements View.OnClickListener,SurfaceHolder.Callback {

    public static final String OUTPUT_PATH = "output_path";
    private static final String TAG = "VideoActivity";
    //图像预览
    private SurfaceView mCameraGLView;
    private SurfaceHolder surfaceHolder;
    //录像按钮
    private  ImageView mRecordButton;
    //使用按钮
    private TextView mUseButton;
    //重新开始录制按钮
    private TextView mReStartButton;
    //定时时间
    private TextView tvTimerTxt;
    //播放录像
    private  ImageView ivPlay;

    private RelativeLayout rlytRecord;

    private  RelativeLayout rlytFinish;

    private RelativeLayout rlytCamera;
    //记录当前的时间
    private long mCurrentTime;
    private boolean isRecording = false;
    //权限判断
    private boolean isPermisson;
    private boolean isDisplay43;
    private Camera mCamera;
    private Camera.Size mCameraSize;
    private int mRotation;
    private String filePath = "";
    //判断横竖屏
    MyOrientationDetector myOrientationDetector;

    private float cameraWidth = 320.0f;
    private float cameraHeight = 240.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //判断屏幕是否为4：3
        float width = DisplayMetricsUtil.getDisplayWidth(VideoActivity.this);
        float height = DisplayMetricsUtil.getDisplayHeight(VideoActivity.this);
        if( width/height ==cameraWidth/ cameraHeight){
            setContentView(R.layout.activity_video4_3);
            isDisplay43 = true;
        }else{
            setContentView(R.layout.activity_video);
        }
        LogUtil.d("onCreate"+(height/(float)width));        initView();
        initView();
        initData();
        initEvent();
    }
    /**
     * 初始化控件
     */
    private void initView(){
        mCameraGLView = (SurfaceView)findViewById(R.id.cameraView);
        mRecordButton = (ImageView) findViewById(R.id.record_button);
        mUseButton = (TextView) findViewById(R.id.use_button);
        mReStartButton = (TextView)findViewById(R.id.restart_button);
        tvTimerTxt = (TextView) findViewById(R.id.tv_countdown);
        ivPlay = (ImageView) findViewById(R.id.iv_play);
        rlytRecord = (RelativeLayout) findViewById(R.id.record_view);
        rlytFinish = (RelativeLayout) findViewById(R.id.finish_view);
        rlytCamera = (RelativeLayout) findViewById(R.id.rlyt_camera);
    }

    private void initData(){
        mCamera =  CameraManager.getInstance().init(VideoActivity.this, (int)cameraWidth, (int)cameraHeight, new CameraManager.OnCameraListener() {
            @Override
            public void OnPermisson(boolean flag) {
                ToastUtil.showLongToast(VideoActivity.this,"摄像头权限拒绝");
                isPermisson = true;
            }
            @Override
            public void OnCameraSize(Camera.Size size) {
                LogUtil.d("OnCameraSize  "+size.width+"  "+size.height );
                mCameraSize =size;
            }
            @Override
            public void OnCameraRotation(int rotation) {
                mRotation = rotation;
            }
        });
        RecorderManager.getInstanse().init(new RecorderManager.OnAudioPermissionListener() {
            @Override
            public void OnAudioPermission(boolean flag) {
                ToastUtil.showLongToast(VideoActivity.this,"录音权限拒绝");
                isPermisson = true;
            }
        });
        surfaceHolder = mCameraGLView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if(mCameraSize!=null){
            surfaceHolder.setFixedSize(mCameraSize.width, mCameraSize.height);
        }
        surfaceHolder.addCallback(this);

        if(!isDisplay43 &&!isPermisson)
        {
            ViewGroup.LayoutParams layoutParams = rlytCamera.getLayoutParams();
            int width = DisplayMetricsUtil.getDisplayWidth(VideoActivity.this);
            layoutParams.width =width ;
            layoutParams.height = width*mCameraSize.width/mCameraSize.height;

            LogUtil.d("mSurfaceView  : "+layoutParams.width +"   "+layoutParams.height);
            rlytCamera.setLayoutParams(layoutParams);
        }
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "/aaaaaa/"+System.currentTimeMillis() + ".mp4");
            LogUtil.d(file.getAbsolutePath());
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdir();
            }
            if(file.exists()){
                file.delete();
            }
            file.createNewFile();
            filePath =file.getAbsolutePath();
        }catch (Exception e){
            LogUtil.d("filePath  ",e);
        }
    }

    private void initEvent(){
        mRecordButton.setOnClickListener(this);
        mUseButton.setOnClickListener(this);
        mReStartButton.setOnClickListener(this);
        ivPlay.setOnClickListener(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        myOrientationDetector = new MyOrientationDetector(this);
        myOrientationDetector.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myOrientationDetector.disable();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraManager.getInstance().stopPreview();
        MediaManager.getInstance().stopVideo();
    }
    //进行倒计时计数（最大3分钟）
    private CountDownTimer countDownTimer = new CountDownTimer(1000 * 60 * 3, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            tvTimerTxt.setText(DateTimeUtils.getMMSS(millisUntilFinished));
        }
        @Override
        public void onFinish() {
            //听着录音
            tvTimerTxt.setText("00:00");
            //模拟点击停止按钮
            mRecordButton.performClick();
        }
    };
    /**
     * 开启定时器
     */
    private void  startTimer(){
        if(countDownTimer!= null){
            countDownTimer.start();
        }
        tvTimerTxt.setText("3:00");
    }
    /**
     * 关闭定时器
     */
    private void stopTimer(){
        if(countDownTimer!= null){
            countDownTimer.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.record_button:{

                if(!isRecording){
                    if(isPermisson){return;}
                    CameraManager.getInstance().start();
                    //在现有的基础上将屏幕旋转，保证旋转之后视频为正的
                    mRotation = CameraManager.getInstance().setRotation(VideoActivity.this,mCamera.getParameters(),null);
                    mRotation = mRotation+myOrientationDetector.getScreenOrientation();
                    MediaManager.getInstance().startVideo(mCamera,mCameraGLView.getHolder(),mCameraSize,mRotation,filePath);
                    mCurrentTime = System.currentTimeMillis();
                    handleView(0);
                    isRecording = true;
                    startTimer();
                }else{
                    if((System.currentTimeMillis()-mCurrentTime)<3000){
                        ToastUtil.showLongToast(this,"最少录制3秒");
                    }else{
                        //停止录像
                        isRecording = false;
                        LogUtil.d("btn_stop");
                        MediaManager.getInstance().stopVideo();
                        CameraManager.getInstance().stop();
                        handleView(1);
                        stopTimer();
                    }
                }
                break;
            }
            case R.id.use_button:{
                break;
            }
            case R.id.restart_button:{
                CameraManager.getInstance().start();
                mRotation = CameraManager.getInstance().setRotation(VideoActivity.this,mCamera.getParameters(),null);
                mRotation = mRotation+myOrientationDetector.getScreenOrientation();
                mCurrentTime = System.currentTimeMillis();
                //界面控制
                handleView(0);
                isRecording = true;
                startTimer();
                break;
            }
        }
    }
    /**
     * 控制控件
     * @param state
     */
    private  void handleView(int state){
        switch (state){
            case 0:{
                //开始
                rlytRecord.setVisibility(View.VISIBLE);
                rlytFinish.setVisibility(View.GONE);
                mRecordButton.setImageResource(R.drawable.btn_ic_suspend);
                ivPlay.setVisibility(View.GONE);
                break;
            }
            case 1:{
                //停止
                rlytRecord.setVisibility(View.GONE);
                rlytFinish.setVisibility(View.VISIBLE);
                mRecordButton.setImageResource(R.drawable.btn_ic_recording);
                ivPlay.setVisibility(View.VISIBLE);
                break;
            }
            case 2:{
                //开始
                rlytRecord.setVisibility(View.VISIBLE);
                rlytFinish.setVisibility(View.GONE);
                mRecordButton.setImageResource(R.drawable.btn_ic_recording);
                ivPlay.setVisibility(View.GONE);
                break;
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtil.d("surfaceCreated");
        CameraManager.getInstance().setPreview(holder);
        CameraManager.getInstance().start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //中间退出则重新录制
        try {
            isRecording = false;
            MediaManager.getInstance().stopVideo();
            CameraManager.getInstance().stop();
            handleView(2);
            stopTimer();
            if(tvTimerTxt!=null){
                tvTimerTxt.setText("3:00");
            }
        }catch (Exception e){
            LogUtil.d("surfaceDestroyed",e);
        }
    }
}




