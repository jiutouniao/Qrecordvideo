package com.zhi.recordvideo;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import java.io.File;
public class MainActivity extends Activity implements View.OnClickListener,SurfaceHolder.Callback{

    private SurfaceHolder surfaceHolder;
    private SurfaceView mSurfaceView;
    private RelativeLayout mRlRecord;
    private Button mBtnRecord;
    private Button mBtnStop;
    private Camera mCamera;
    private Camera.Size mCameraSize;
    private int mRotation;
    private String filePath = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        LogUtil.d("onCreate");
        initViews();
        initEvents();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d("onDestroy");
        CameraManager.getInstance().stopPreview();
        MediaManager.getInstance().stopVideo();
    }

    private void initEvents() {
        mBtnRecord.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);

        mCamera =  CameraManager.getInstance().init(MainActivity.this, 320, 240, new CameraManager.OnCameraListener() {
            @Override
            public void OnPermisson(boolean flag) {
                LogUtil.d("OnPermisson");
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

        surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        LogUtil.d("surfaceHolder  "+mCameraSize.width+"  "+mCameraSize.height );
        surfaceHolder.setFixedSize(mCameraSize.width, mCameraSize.height);
        surfaceHolder.addCallback(this);

        ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
       int width = DisplayMetricsUtil.getDisplayWidth(MainActivity.this);
        layoutParams.width =width ;
        layoutParams.height = width*mCameraSize.width/mCameraSize.height;

        LogUtil.d("mSurfaceView  : "+layoutParams.width +"   "+layoutParams.height);
        mSurfaceView.setLayoutParams(layoutParams);

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

    private void initViews() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mRlRecord = (RelativeLayout) findViewById(R.id.rl_record);
        mBtnRecord = (Button) findViewById(R.id.btn_record);
        mBtnStop = (Button) findViewById(R.id.btn_stop);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record:
                CameraManager.getInstance().start();
                MediaManager.getInstance().startVideo(mCamera,mSurfaceView.getHolder(),mCameraSize,mRotation,filePath);
                mRlRecord.setEnabled(false);
                mBtnStop.setEnabled(true);
                break;
            case R.id.btn_stop:
                LogUtil.d("btn_stop");
                MediaManager.getInstance().stopVideo();
                CameraManager.getInstance().stop();
                mRlRecord.setEnabled(true);
                mBtnStop.setEnabled(false);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mRlRecord.setVisibility(View.VISIBLE);
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        CameraManager.getInstance().setPreview(holder);
        CameraManager.getInstance().start();
        LogUtil.d("surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtil.d("surfaceDestroyed");

    }






















}