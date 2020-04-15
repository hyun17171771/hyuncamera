package com.hyun.openglcamera;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class GLUtil {

    private static final String TAG = GLUtil.class.getSimpleName();

    public static int createProgram(Context context, int vertexRawId, int fragmentRawId) {
        return createProgram(getStringFromRaw(context, vertexRawId),
                getStringFromRaw(context, fragmentRawId));
    }


    public static int createProgram(String vertexSource, String fragmentSource) {

        final int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            Log.e(TAG, "## vertexShader == 0");
            return 0;
        }

        final int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            Log.e(TAG, "## fragmentShader == 0");
            return 0;
        }

        int program = GLES20.glCreateProgram(); //빈 쉐이더 프로그램 생성
        if (program == 0) {

            Log.e(TAG, "## program == 0");

            return 0;
        }

        GLES20.glAttachShader(program, vertexShader); //버텍스 쉐이더를 프로그램에 붙인다
        GLES20.glAttachShader(program, fragmentShader); //프레그먼트 쉐이더를 프로그램에 붙인다
        GLES20.glLinkProgram(program); //program객체를 OpenGL에 연결한다. program에 추가된 shader들이 OpenGL에 연결

        int[] linkStates = new int[1];
        //프로그램이 잘 연결 되었는 지 확인
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStates, 0);
        if (linkStates[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "## 프로그램이 연결되지 않았음");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program); //프로그램 삭제
            program = 0;

        }


        return program;
    }


    private static String getStringFromRaw(Context context, int id) {
        String string;
        try {
            Resources resources = context.getResources();
            InputStream input = resources.openRawResource(id);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int i = input.read();
            while (i != -1) {
                output.write(i);
                i = input.read();
            }

            string = output.toString();
            input.close();
        } catch (IOException e) {
            string = "";
        }

        Log.d(TAG, "##shader - " + string);

        return string;
    }


    /**
     * 제공된 쉐이더 소스를 컴파일 한다
     *
     * @param shaderType
     * @param shaderSource
     * @return 쉐이더 아이디를 리턴한다. 실패시 0을 리턴한다
     */
    public static int loadShader(int shaderType, String shaderSource) {
        int shader = GLES20.glCreateShader(shaderType); //쉐이더 핸들을 만든다

        if (shader == 0) {
            return shader;
        }

        GLES20.glShaderSource(shader, shaderSource); //쉐이더소스를 연결한다
        GLES20.glCompileShader(shader); //컴파일

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0); //컴파일에 문제 없는 지 확인
        if (compiled[0] == 0) {
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        return shader;
    }

    /**
     * C++ 레벨의 float 배열을 저장하기 위한 메모리 생성
     *
     * @param coords
     * @return
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {

        //1.ByteBuffer를 할당 받는다
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                coords.length * 4);

        //2. ByteBuffer에서 사용할 엔디안을 지정함
        //버퍼의 byte order로써 디바이스 하드웨어의 native byte order를 사용
        bb.order(ByteOrder.nativeOrder());

        //3. ByteBuffer를 FloatBuffer로 변환
        FloatBuffer floatBuffer = bb.asFloatBuffer();

        //4. float 배열에 정의된 좌표들을 FloatBuffer에 저장한다
        floatBuffer.put(coords);

        //5. 읽어올 버퍼의 위치를 0으로 설정한다. 첫번째 좌표부터 읽어오게됨
        floatBuffer.position(0);

        return floatBuffer;
    }

    public static int generateTexture(int textureType) {

        int[] textures = new int[1];

        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(textureType, textures[0]);

        if (textureType == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {

            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        } else {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        }

        return textures[0];
    }

}
