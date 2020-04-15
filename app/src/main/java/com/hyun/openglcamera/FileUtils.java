package com.hyun.openglcamera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    // 저장할 이미지 파일명 만들기
    public static String getThumbFilePath(Context context) {
        final String root =
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyunCamera";
        final File myDir = new File(root);

        if (!myDir.exists()) {

            boolean result = myDir.mkdirs();
            Log.d(TAG, "## getThumbFilePath - no dir " + result);
        }

        context.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(myDir)));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        final String fileName = "picture" + sdf.format(timestamp);
        final File file = new File(myDir, fileName);
        if (file.exists()) {
            file.delete();
        }

        return root + "/" + fileName + ".jpg";
    }


    public static void createImageFile(Context context, Bitmap bitmap) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageFileAndroidQ(context, bitmap);
            return;
        }

        String path = getThumbFilePath(context);
        File file = new File(path);


        try {

            //빈 파일을 생성
            file.createNewFile();

            // 파일을 쓸 수 있는 스트림을 준비합니다.
            FileOutputStream out = new FileOutputStream(file);

            // compress 함수를 사용해 스트림에 비트맵을 저장
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            // 스트림 사용후 닫기
            out.close();

            context.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException : " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IOException : " + e.getMessage());
        }


    }


    public static void saveImageFileAndroidQ(Context context, Bitmap bitmap) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        final String fileName = "picture" + sdf.format(timestamp) + ".jpg";


        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);


        ContentResolver contentResolver = context.getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(item, "w", null);

            if (parcelFileDescriptor == null) {
                Log.d("parcelFileDescriptor", "null");
            } else {

                FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.close();
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                contentResolver.update(item, values, null, null);


            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
