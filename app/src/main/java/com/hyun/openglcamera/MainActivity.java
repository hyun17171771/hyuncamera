package com.hyun.openglcamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;


import java.util.ArrayList;

import static com.hyun.openglcamera.GlobalConstant.HORIZONTAL_MODE1;
import static com.hyun.openglcamera.GlobalConstant.HORIZONTAL_MODE2;
import static com.hyun.openglcamera.GlobalConstant.VERTICAL_MODE1;
import static com.hyun.openglcamera.GlobalConstant.VERTICAL_MODE2;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ImageView mIvShutter;
    private GLSurfaceView mGLSurfaceView;
    private GLRenderer mGLRenderer;

    private RecyclerView mRecyclerView;
    private FilterListAdapter mAdapter;
    private ArrayList<FilterVO> mFilterList;
    private RequestManager mGlideManager;


    private ImageButton mIbtnTakePicture;
    private ImageButton mIbtnChangeCamera;
    private ImageButton mIbtnSelectFilter;

    private OrientationEventListener mOrientEventListener;

    private int mScreenMode;

    private boolean mFilterListVisible;


    //private ShapeRenderer mGLRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createFilterList();
        init();
    }


    private void init() {


        mIvShutter = findViewById(R.id.iv_shutter);


        mGlideManager = Glide.with(this);
        mRecyclerView = findViewById(R.id.rcv_filter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mAdapter = new FilterListAdapter(this, mGlideManager, mFilterList);
        mAdapter.setOnItemClickListener(new FilterListAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {

                int program = mFilterList.get(position).getType().getFilterIndex();
                mGLRenderer.setSelectedFilterProgram(program);

            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setVisibility(View.GONE);

        mGLRenderer = new GLRenderer(this);

        mGLSurfaceView = findViewById(R.id.surface_view);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mGLRenderer);


        mIbtnChangeCamera = findViewById(R.id.ibtn_change);
        mIbtnChangeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGLRenderer.switchCamera();
            }
        });

        mIbtnTakePicture = findViewById(R.id.ibtn_take_picture);
        mIbtnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "## mIbtnTakePicture clicked");


                //셔터 애니메이션
                mIvShutter.setAlpha(0.7f);
                mIvShutter.animate().alpha(0.7f).setDuration(250);
                mIvShutter.animate().alpha(0f).setDuration(250).setStartDelay(150);
                mIvShutter.setAlpha(0f);

                mGLRenderer.takePicture(true, mScreenMode);
            }
        });

        mIbtnSelectFilter = findViewById(R.id.ibtn_filter);
        mIbtnSelectFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mFilterListVisible) {
                    mRecyclerView.setVisibility(View.GONE);
                    mFilterListVisible = false;
                } else {

                    mRecyclerView.setVisibility(View.VISIBLE);
                    mFilterListVisible = true;

                }

            }
        });




        /*
         * 안드로이드 screenOrientation 고정상태에서 360 회전 감지
         * 4개의 각도 범위 내에서 뷰들을 특정 각도로 회전시키는 메소드를 각각 호출한다
         *
         * */

        mOrientEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int angle) {

                if ((angle >= 0 && angle < 45) || (angle >= 315 && angle < 360)) {
                    if (mScreenMode != VERTICAL_MODE1) {
                        setVertical1();
                    }
                } else if (angle >= 45 && angle < 135) {
                    if (mScreenMode != HORIZONTAL_MODE1) {
                        setHorizontal1();
                    }
                } else if (angle >= 135 && angle < 225) {
                    if (mScreenMode != VERTICAL_MODE2) {
                        setVertical2();
                    }
                } else if (angle >= 225 && angle < 315) {
                    if (mScreenMode != HORIZONTAL_MODE2) {
                        setHorizontal2();
                    }
                }
            }
        };

        mOrientEventListener.enable();

    }


    private void setVertical1() {
        mScreenMode = VERTICAL_MODE1;

        mIbtnChangeCamera.setRotation(0);
        mIbtnSelectFilter.setRotation(0);

    }

    private void setHorizontal1() {
        mScreenMode = HORIZONTAL_MODE1;

        mIbtnChangeCamera.setRotation(270);
        mIbtnSelectFilter.setRotation(270);
    }

    private void setVertical2() {
        mScreenMode = VERTICAL_MODE2;

        mIbtnChangeCamera.setRotation(180);
        mIbtnSelectFilter.setRotation(180);

    }

    private void setHorizontal2() {
        mScreenMode = HORIZONTAL_MODE2;

        mIbtnChangeCamera.setRotation(90);
        mIbtnSelectFilter.setRotation(90);

    }


    @Override
    protected void onResume() {

        mGLRenderer.onResume();

        super.onResume();

    }

    @Override
    protected void onPause() {

        mGLRenderer.onPause();

        super.onPause();

    }


    private void createFilterList() {

        mFilterList = new ArrayList<>();
        mFilterList.add(new FilterVO(FilterType.ORIGINAL, getResources().getDrawable(R.drawable.original)));
        mFilterList.add(new FilterVO(FilterType.RAINBOW, getResources().getDrawable(R.drawable.rainbow)));
        mFilterList.add(new FilterVO(FilterType.GRAYSCALE, getResources().getDrawable(R.drawable.grayscale)));
        mFilterList.add(new FilterVO(FilterType.INVERSION, getResources().getDrawable(R.drawable.inversion)));
        mFilterList.add(new FilterVO(FilterType.CARTOON, getResources().getDrawable(R.drawable.cartoon)));
        mFilterList.add(new FilterVO(FilterType.METAL, getResources().getDrawable(R.drawable.metal)));

    }

}
