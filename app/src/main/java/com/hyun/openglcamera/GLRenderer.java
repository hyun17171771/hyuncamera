package com.hyun.openglcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;


import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.hyun.openglcamera.GlobalConstant.HORIZONTAL_MODE1;
import static com.hyun.openglcamera.GlobalConstant.HORIZONTAL_MODE2;
import static com.hyun.openglcamera.GlobalConstant.VERTICAL_MODE1;
import static com.hyun.openglcamera.GlobalConstant.VERTICAL_MODE2;

public class GLRenderer implements GLSurfaceView.Renderer {

    public static final String TAG = GLRenderer.class.getSimpleName();


    private SurfaceTexture mSurfaceTexture;
    private int mTextureId;


    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private final String CAMERA_THREAD_NAME = "CameraBackground";
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Size mPreviewSize;
    private Size mViewSize;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private static final int MAX_PREVIEW_WIDTH = 1600;
    private static final int MAX_PREVIEW_HEIGHT = 1200;
    private int mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;
    public static final String CAMERA_FRONT = "1";
    public static final String CAMERA_BACK = "0";
    private String mCameraId = CAMERA_BACK;


    private int mScreenMode;
    private int mSelectedFilterProgramPosition;


    private Context mContext;
    private CameraFilter mCameraFilter;
    private boolean mTakePicture;


    public GLRenderer(Context context) {
        mContext = context;


    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        Log.d(TAG, "## onSurfaceCreated");
        mCameraFilter = new CameraFilter(mContext);
        mSelectedFilterProgramPosition = FilterType.ORIGINAL.getFilterIndex();


    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d(TAG, "## onSurfaceChanged");
        Log.d(TAG, "## width - " + width);
        Log.d(TAG, "## height - " + height);


        mViewSize = new Size(width, height);

        GLES20.glViewport(0, 0, width, height);
        mTextureId = GLUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);


        Log.d(TAG, "## mTextureId - " + mTextureId);

        openCamera(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


        if (mSurfaceTexture != null) {

            mSurfaceTexture.updateTexImage();
            mCameraFilter.drawCameraPreview(mTextureId, mSelectedFilterProgramPosition, mCameraId);


            if (mTakePicture) {
                mTakePicture = false;

                Bitmap bitmap = createBitmapFromGLSurface(0, 0, mViewSize.getWidth(), mViewSize.getHeight(), gl);
                FileUtils.createImageFile(mContext, bitmap);

            }


        } else {
            Log.d(TAG, "## onDrawFrame mSurfaceTexture null");
        }
    }


    public void onResume() {

        Log.d(TAG, "## onResume");
        startBackgroundThread();

    }

    public void onPause() {

        Log.d(TAG, "## onPause");

        closeCamera();
        stopBackgroundThread();

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Camera
    ////////////////////////////////////////////////////////////////////////////////////////////////


    private void startBackgroundThread() {

        mCameraThread = new HandlerThread(CAMERA_THREAD_NAME);
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    private void stopBackgroundThread() {
        mCameraThread.quitSafely();
        try {
            mCameraThread.join();
            mCameraThread = null;
            mCameraThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private final CameraDevice.StateCallback mStateCallback =
            new CameraDevice.StateCallback() {

                @Override
                public void onOpened(@NonNull CameraDevice currentCameraDevice) {

                    Log.d(TAG, "## mStateCallback - onOpened");


                    //카메라가 열릴 때 호출, 카메라 미리보기 시작
                    mCameraDevice = currentCameraDevice;
                    startPreview();
                    mCameraOpenCloseLock.release();

                }

                @Override
                public void onDisconnected(@NonNull CameraDevice currentCameraDevice) {
                    mCameraOpenCloseLock.release();
                    currentCameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice currentCameraDevice, int error) {
                    mCameraOpenCloseLock.release();
                    currentCameraDevice.close();
                    mCameraDevice = null;

                }

                public void onClosed(@NonNull CameraDevice camera) {

                    Log.d(TAG, "## mStateCallback onClosed");
                }
            };


    private void openCamera(int width, int height) {

        Log.d(TAG, "## openCamera");

        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("## Time out waiting to lock camera opening.");
            }

            for (String cameraId : mCameraManager.getCameraIdList()) {

                //카메라 정보를 담고 있는 CameraCharacteristics 객체 얻기
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != mCameraFacing) {
                    continue;
                }


                //카메라 각종 지원 정보  얻는 객체
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }


                //카메라에서 지원되는 크기 목록

                Size aspectRatio = new Size(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT);
                Size[] choices = map.getOutputSizes(SurfaceTexture.class);

                if(choices != null) {
                    mPreviewSize = chooseOptimalSize(choices,
                            width, height, aspectRatio);
                }

                if (mCameraId == null) {
                    mCameraId = cameraId;
                }

            }

            mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "## Failed to open Camera", e);
            ((Activity) mContext).finish();
        } catch (InterruptedException e) {
            throw new RuntimeException("## Interrupted while trying to lock camera opening.", e);
        } catch (SecurityException e) {
            throw new RuntimeException("## Security exception.", e);
        }
    }


    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, Size aspectRatio) {


        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        List<Size> notRatioEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= MAX_PREVIEW_WIDTH && option.getHeight() <= MAX_PREVIEW_HEIGHT) {
                if (option.getHeight() == option.getWidth() * h / w) {
                    if (option.getWidth() >= textureViewHeight && option.getHeight() >= textureViewWidth) {
                        bigEnough.add(option);
                    } else {
                        notBigEnough.add(option);
                    }
                }
            }
        }


        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
//			Log.e(TAG, "## Couldn't find any suitable preview size");
//			return choices[0];
            //4:3 비율이 맞지 않는 것들 중
            for (Size option : choices) {
                if (option.getWidth() <= MAX_PREVIEW_WIDTH && option.getHeight() <= MAX_PREVIEW_HEIGHT) {
                    notRatioEnough.add(option);
                }
            }
            if (notRatioEnough.size() > 0) {
                return Collections.max(notRatioEnough, new CompareSizesByArea());
            } else {
                Log.e(TAG, "## Couldn't find any suitable preview size");
                return choices[choices.length - 1];
            }
        }
    }


    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {

            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }


    private void closeCamera() {


        Log.d(TAG, "## closeCamera");

        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }


        } catch (InterruptedException e) {
            throw new RuntimeException("## Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void startPreview() {

        Log.d(TAG, "## mTextureId - " + mTextureId);

        Log.d(TAG, "## startPreview");

        try {
            closePreviewSession();


            mSurfaceTexture = new SurfaceTexture(mTextureId);


            if (mSurfaceTexture == null) {
                Log.e(TAG, "## texture is null");
                ((Activity) mContext).finish();
                return;
            }

            //프리뷰 사이즈 설정
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // 프리뷰에 필요한 surface 생성
            Surface surface = new Surface(mSurfaceTexture);

            // 타겟 surface 설정
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);


            // 카메라 프리뷰를 위한 캡쳐 세션 생성
            mCameraDevice.createCaptureSession(
//					Arrays.asList(surface),
                    Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                            if (mCameraDevice == null) {
                                return;
                            }


                            // 세션이 준비되면 프리뷰를 보여준다
                            mCaptureSession = cameraCaptureSession;
                            updatePreview();

                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "## Camera failed");
                        }
                    },
                    null);
            //mCameraHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "## Failed to preview Camera", e);
        }
    }


    private void updatePreview() {

        if (mCameraDevice == null) {
            return;
        }
        try {

            Log.d(TAG, "## updatePreview");

            //미리보기에서 자동 초점이 계속 작동한다
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);


            //반복적으로 이미지 버퍼를 얻기 위해 호출
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mCameraHandler);

            //전면 카메라일 경우 화면 좌우 반전
            mCameraFilter.setInVerseView((mCameraId == CAMERA_FRONT));

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void switchCamera() {

        Log.d(TAG, "## switchCamera");


        switch (mCameraId) {
            case CAMERA_FRONT:
                mCameraId = CAMERA_BACK;

                break;
            case CAMERA_BACK:
                mCameraId = CAMERA_FRONT;


                break;
        }

        closeCamera();
        openCamera(mViewSize.getWidth(), mViewSize.getHeight());

    }


    public void setSelectedFilterProgram(int programPosition) {
        mSelectedFilterProgramPosition = programPosition;
    }


    public void takePicture(boolean takePicture, int screenMode) {
        mTakePicture = takePicture;
        mScreenMode = screenMode;
    }


    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL10 gl)
            throws OutOfMemoryError {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }

        Bitmap src = Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
        return Bitmap.createBitmap(src, 0, 0, w, h, rotateImage(), true);
    }

    private Matrix rotateImage() {

        int degree;

        switch (mScreenMode) {

            case VERTICAL_MODE1: {
                degree = 0;
                break;
            }
            case HORIZONTAL_MODE1: {
                degree = 90;
                break;
            }
            case VERTICAL_MODE2: {
                degree = 180;
                break;
            }
            case HORIZONTAL_MODE2: {
                degree = 270;
                break;
            }
            default:
                degree = 0;
                break;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degree);

        return matrix;

    }


}
