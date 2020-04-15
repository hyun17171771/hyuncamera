package com.hyun.openglcamera;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class CameraFilter {


    private static final String TAG = CameraFilter.class.getSimpleName();

    private static final float VERTICES[] = new float[]{
            -1f, -1f, 0f,  //bottom left
            1f, -1f, 0f,  //bottom right
            1f, 1f, 0f,  // top right
            -1f, 1f, 0f, // top left

    };


    private static final float REVERSEVERTICES[] = new float[]{
            1f, -1f, 0f,  //bottom left
            -1f, -1f, 0f,  //bottom right
            -1f, 1f, 0f,  // top right
            1f, 1f, 0f, // top left

    };

    private static final float UVS[] = new float[]{
            0f, 1f,
            1f, 1f,
            1f, 0f,
            0f, 0f
    };


    private static final byte INDEX[] = new byte[]{
            0, 1, 2,
            2, 3, 0
    };


    private FloatBuffer mVertexBuffer;
    private static final int COORDS_PER_VERTEX = 3;
    private static final int COORDS_PER_UV = 2;
    private int mVertexStride = 3 * 4;
    private int mUVStride = 2 * 4;
    private final FloatBuffer VERTEX_BUFFER; //버텍스 버퍼, 후면 카메라용
    private final FloatBuffer UV_BUFFER; //uv 버퍼
    private final FloatBuffer REVERSEVERTEX_BUFFER; //reverse 정점 버퍼, 정면 카메라용
    private final ByteBuffer INDEX_BUFFER; //사각형 그리는 순서 버퍼


    private final float[] MODEL_MATRIX = new float[16];
    private final float[] MVP_MATRIX = new float[16];
    private final float[] PROJECTION_MATRIX = new float[16];
    private final float[] VIEW_MATRIX = new float[16];
    private final float[] VP_MATRIX = new float[16];


    final long TIME = System.currentTimeMillis();

    private SparseArray<Integer> mCameraFilterList = new SparseArray<>();


    private int cameraTextureId = 0;


    public CameraFilter(Context context) {

        createProgram(context);

        Matrix.setIdentityM(PROJECTION_MATRIX, 0);
        Matrix.setIdentityM(VIEW_MATRIX, 0);
        Matrix.setIdentityM(VP_MATRIX, 0);
        Matrix.setRotateM(MODEL_MATRIX, 0, 90, 0, 0, -1); //화면 정방향 회전

        VERTEX_BUFFER = GLUtil.createFloatBuffer(VERTICES);
        REVERSEVERTEX_BUFFER = GLUtil.createFloatBuffer(REVERSEVERTICES);
        UV_BUFFER = GLUtil.createFloatBuffer(UVS);


        INDEX_BUFFER = ByteBuffer.allocateDirect(INDEX.length);
        INDEX_BUFFER.put(INDEX);
        INDEX_BUFFER.position(0);

        mVertexBuffer = VERTEX_BUFFER;

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    }


    final public void drawCameraPreview(int cameraTextureId, int programPosition, String cameraId) {

        int program = mCameraFilterList.get(programPosition);

        GLES20.glUseProgram(program);

        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, mVertexStride, mVertexBuffer);

        int uvHandle = GLES20.glGetAttribLocation(program, "a_texCoord");
        GLES20.glEnableVertexAttribArray(uvHandle);
        GLES20.glVertexAttribPointer(uvHandle, COORDS_PER_UV, GLES20.GL_FLOAT, false, mUVStride, UV_BUFFER);


        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        Matrix.multiplyMM(MVP_MATRIX, 0, VP_MATRIX, 0, MODEL_MATRIX, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, MVP_MATRIX, 0);


        float time = ((float) (System.currentTimeMillis() - TIME)) / 1000.0f;
        int timeHandle = GLES20.glGetUniformLocation(program, "u_time");
        GLES20.glUniform1f(timeHandle, time);


        int textureHandle = GLES20.glGetUniformLocation(program, "s_texture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);

        GLES20.glUniform1i(textureHandle, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, INDEX.length, GLES20.GL_UNSIGNED_BYTE, INDEX_BUFFER);

    }


    //카메라가 전면일 경우 좌우 반전
    //카메라 전환까지 시간이 걸리므로 딜레이를 준다
    public void setInVerseView(boolean isFront) {

        Log.d(TAG, "## setInVerseView");


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);

                    if (isFront) {
                        mVertexBuffer = REVERSEVERTEX_BUFFER;
                    } else {
                        mVertexBuffer = VERTEX_BUFFER;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();


    }


    private void createProgram(Context context) {

        mCameraFilterList = new SparseArray<>();
        mCameraFilterList.append(FilterType.ORIGINAL.getFilterIndex(), GLUtil.createProgram(context, R.raw.vertex, R.raw.original_s));
        mCameraFilterList.append(FilterType.RAINBOW.getFilterIndex(), GLUtil.createProgram(context, R.raw.vertex, R.raw.rainbow_s));
        mCameraFilterList.append(FilterType.GRAYSCALE.getFilterIndex(), GLUtil.createProgram(context, R.raw.vertex, R.raw.grayscale_s));
        mCameraFilterList.append(FilterType.INVERSION.getFilterIndex(), GLUtil.createProgram(context, R.raw.vertex, R.raw.inversion_s));
        mCameraFilterList.append(FilterType.CARTOON.getFilterIndex(), GLUtil.createProgram(context, R.raw.vertex, R.raw.cartoon_s));
        mCameraFilterList.append(FilterType.METAL.getFilterIndex(), GLUtil.createProgram(context, R.raw.vertex, R.raw.metal_s));

    }


}
