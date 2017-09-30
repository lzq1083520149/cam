/*
 *          Copyright (C) 2016 jarlen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.photoedit.demo;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.photoedit.demo.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import cn.jarlen.photoedit.operate.OperateUtils;
import cn.jarlen.photoedit.utils.FileUtils;

/**
 * 测试首页
 *
 * @author jarlen
 */
public class PhotoEditActivity extends Activity implements View.OnClickListener {

    private LinearLayout content_layout;
    private ImageView pictureShow;
    private Class<?> intentClass;
    private int intentType = 0;

    /* 用来标识请求照相功能的activity */
    private static final int CAMERA_WITH_DATA = 3023;

    /* 用来标识请求gallery的activity */
    private static final int PHOTO_PICKED_WITH_DATA = 3021;

    /* 边框 */
    private static final int PHOTO_FRAME_WITH_DATA = 3024;

    /* 马赛克 */
    private static final int PHOTO_MOSAIC_WITH_DATA = 3025;

    /* 涂鸦 */
    private static final int PHOTO_DRAW_WITH_DATA = 3026;

    /* 剪切 */
    private static final int PHOTO_CROP_WITH_DATA = 3027;

    /* 滤镜 */
    private static final int PHOTO_FILTER_WITH_DATA = 3028;

    /* 增强 */
    private static final int PHOTO_ENHANCE_WITH_DATA = 3029;

    /* 旋转 */
    private static final int PHOTO_REVOLVE_WITH_DATA = 3030;

    /* 图像变形 */
    private static final int PHOTO_WARP_WITH_DATA = 3031;

    /* 添加水印图片 */
    private static final int PHOTO_ADD_WATERMARK_DATA = 3032;
    /* 添加文字 */
    private static final int PHOTO_ADD_TEXT_DATA = 3033;

    /*  测试接口 */
    private static final int PHOTO_TEST_TEXT_DATA = 3034;

    /* 照相机拍照得到的图片 */
    private File mCurrentPhotoFile;
    private String photoPath = null, tempPhotoPath, camera_path;

    private int scale = 2;
    int width = 0;
    private boolean lone = false;
    private String resultPath;

    Locale locale;
    OperateUtils operateUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_potoedit);


        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        width = metric.widthPixels; // 屏幕宽度（像素）
        pictureShow = (ImageView) findViewById(R.id.pictureShow);
        content_layout = (LinearLayout) findViewById(R.id.mainLayout);
        operateUtils = new OperateUtils(this);

        initView();

        lone = getIntent().getBooleanExtra("photo", false);
        if (lone) {
            photoPath = getIntent().getStringExtra("photo_path");
            compressed();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!lone) {
                    getPictureFromPhoto();
                }
            }
        }, 100);

    }

    private void initView() {


        findViewById(R.id.addPictureFromPhoto).setOnClickListener(this);
        findViewById(R.id.addPictureFromCamera).setOnClickListener(this);
        findViewById(R.id.testBtn).setOnClickListener(this);
        findViewById(R.id.tv_1).setOnClickListener(this);
        findViewById(R.id.tv_2).setOnClickListener(this);
        findViewById(R.id.tv_3).setOnClickListener(this);
        findViewById(R.id.tv_4).setOnClickListener(this);
        findViewById(R.id.tv_5).setOnClickListener(this);
        findViewById(R.id.tv_6).setOnClickListener(this);

        TextView tv_7 = (TextView) findViewById(R.id.tv_7);
        tv_7.setOnClickListener(this);

        findViewById(R.id.tv_8).setOnClickListener(this);
        findViewById(R.id.tv_9).setOnClickListener(this);
        findViewById(R.id.tv_10).setOnClickListener(this);

        if(!getSystemLanguage()){
            tv_7.setVisibility(View.GONE);
        }
    }

    private Boolean getSystemLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = getResources().getConfiguration().locale;
        }
        return locale.getLanguage().contains("zh");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addPictureFromCamera:
                getPictureFormCamera();
                break;
            case R.id.addPictureFromPhoto:
                getPictureFromPhoto();
                break;
            case R.id.testBtn:
                if (resultPath != null) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    shareIntent.setType("image/jpeg");
                    ArrayList<Uri> photoUris = new ArrayList<Uri>();
                    photoUris.add(Uri.fromFile(new File(resultPath)));
                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, photoUris);
                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
                } else {
                    Toast.makeText(PhotoEditActivity.this, "Edit first！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.tv_1:
                intentClass = ImageFilterActivity.class;
                intentType = PHOTO_FILTER_WITH_DATA;
                changePhoto();
                break;
            case R.id.tv_2:
                intentClass = WarpActivity.class;
                intentType = PHOTO_WARP_WITH_DATA;
                changePhoto();
                break;
            case R.id.tv_6:
                intentClass = ImageCropActivity.class;
                intentType = PHOTO_CROP_WITH_DATA;
                changePhoto();
                break;
            case R.id.tv_4:
                intentClass = DrawBaseActivity.class;
                intentType = PHOTO_DRAW_WITH_DATA;
                changePhoto();
                break;
            case R.id.tv_3:
                intentClass = PhotoFrameActivity.class;
                intentType = PHOTO_FRAME_WITH_DATA;
                changePhoto();
                break;
            case R.id.tv_10:
                intentClass = AddTextActivity.class;
                intentType = PHOTO_ADD_TEXT_DATA;
                changePhoto();
                break;
            case R.id.tv_7:
                intentClass = AddWatermarkActivity.class;
                intentType = PHOTO_ADD_WATERMARK_DATA;
                changePhoto();
                break;
            case R.id.tv_5:
                intentClass = MosaicActivity.class;
                intentType = PHOTO_MOSAIC_WITH_DATA;
                changePhoto();
                break;
            case R.id.tv_8:
                intentClass = EnhanceActivity.class;
                intentType = PHOTO_ENHANCE_WITH_DATA;
                changePhoto();
                break;
            case R.id.tv_9:
                intentClass = RevolveActivity.class;
                intentType = PHOTO_REVOLVE_WITH_DATA;
                changePhoto();
                break;
            default:
                break;
        }

    }

    /* 从相册中获取照片 */
    private void getPictureFromPhoto() {
        Intent openphotoIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(openphotoIntent, PHOTO_PICKED_WITH_DATA);
    }

    /* 从相机中获取照片 */
    private void getPictureFormCamera() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

        tempPhotoPath = FileUtils.DCIMCamera_PATH + FileUtils.getNewFileName()
                + ".jpg";

        mCurrentPhotoFile = new File(tempPhotoPath);

        if (!mCurrentPhotoFile.exists()) {
            try {
                mCurrentPhotoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(mCurrentPhotoFile));
        startActivityForResult(intent, CAMERA_WITH_DATA);
    }

    private void compressed() {
        Bitmap resizeBmp = operateUtils.compressionFiller(photoPath,
                content_layout);
        pictureShow.setImageBitmap(resizeBmp);
        camera_path = SaveBitmap(resizeBmp, "saveTemp");
    }

    final Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if (content_layout.getWidth() != 0) {
                    Log.i("LinearLayoutW", content_layout.getWidth() + "");
                    Log.i("LinearLayoutH", content_layout.getHeight() + "");
                    // 取消定时器
                    timer.cancel();
                    compressed();
                }
            }
        }
    };
    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        public void run() {
            Message message = new Message();
            message.what = 1;
            myHandler.sendMessage(message);
        }
    };

    // 将生成的图片保存到内存中
    public String SaveBitmap(Bitmap bitmap, String name) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File dir = new File(Constants.filePath);
            if (!dir.exists())
                dir.mkdir();
            File file = new File(Constants.filePath + name + ".jpg");
            FileOutputStream out;
            try {
                out = new FileOutputStream(file);
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                    out.flush();
                    out.close();
                }
                return file.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case CAMERA_WITH_DATA:

                photoPath = tempPhotoPath;
                if (content_layout.getWidth() == 0) {
                    timer.schedule(task, 10, 1000);
                } else {
                    compressed();
                }

                break;

            case PHOTO_PICKED_WITH_DATA:

                Uri selectedImage = data.getData();
                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePathColumns[0]);
                photoPath = c.getString(columnIndex);
                c.close();

                // 延迟每次延迟10 毫秒 隔1秒执行一次
                if (content_layout.getWidth() == 0) {
                    timer.schedule(task, 10, 1000);
                } else {
                    compressed();
                }

                break;
            case PHOTO_FRAME_WITH_DATA:
            case PHOTO_MOSAIC_WITH_DATA:
            case PHOTO_DRAW_WITH_DATA:
            case PHOTO_CROP_WITH_DATA:
            case PHOTO_FILTER_WITH_DATA:
            case PHOTO_ENHANCE_WITH_DATA:
            case PHOTO_REVOLVE_WITH_DATA:
            case PHOTO_WARP_WITH_DATA:
            case PHOTO_ADD_WATERMARK_DATA:
            case PHOTO_ADD_TEXT_DATA:
            case PHOTO_TEST_TEXT_DATA:

                resultPath = data.getStringExtra("camera_path");
                Bitmap resultBitmap = BitmapFactory.decodeFile(resultPath);
                pictureShow.setImageBitmap(resultBitmap);
                break;

            default:
                break;
        }

    }

    private void changePhoto() {
        // 将图片路径photoPath传到所要调试的模块
        Intent photoFrameIntent = new Intent(PhotoEditActivity.this,
                intentClass);
        photoFrameIntent.putExtra("camera_path", camera_path);
        PhotoEditActivity.this.startActivityForResult(photoFrameIntent,
                intentType);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.base_toolbar_menu, menu);
        if(!getSystemLanguage()){
            menu.getItem(6).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (photoPath == null) {
            Toast.makeText(PhotoEditActivity.this, "Please select a picture...",
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_filter:
                intentClass = ImageFilterActivity.class;
                intentType = PHOTO_FILTER_WITH_DATA;
                break;
            case R.id.action_wrap:
                intentClass = WarpActivity.class;
                intentType = PHOTO_WARP_WITH_DATA;
                break;
            case R.id.action_crop:
                intentClass = ImageCropActivity.class;
                intentType = PHOTO_CROP_WITH_DATA;
                break;
            case R.id.action_draw:
                intentClass = DrawBaseActivity.class;
                intentType = PHOTO_DRAW_WITH_DATA;
                break;
            case R.id.action_frame:
                intentClass = PhotoFrameActivity.class;
                intentType = PHOTO_FRAME_WITH_DATA;
                break;
            case R.id.action_addtv:
                intentClass = AddTextActivity.class;
                intentType = PHOTO_ADD_TEXT_DATA;
                break;
            case R.id.action_addwm:
                intentClass = AddWatermarkActivity.class;
                intentType = PHOTO_ADD_WATERMARK_DATA;
                break;
            case R.id.action_mosaic:
                intentClass = MosaicActivity.class;
                intentType = PHOTO_MOSAIC_WITH_DATA;
                break;
            case R.id.action_enchance:
                intentClass = EnhanceActivity.class;
                intentType = PHOTO_ENHANCE_WITH_DATA;
                break;
            case R.id.action_rotate:
                intentClass = RevolveActivity.class;
                intentType = PHOTO_REVOLVE_WITH_DATA;
                break;
            default:
                intentClass = null;
                intentType = 0;
                break;
        }

        changePhoto();

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        File file = new File(Environment.getExternalStorageDirectory() + "/Camlog/", "saveTemp.jpg");
        if (file.exists()) {
            file.delete();
        }

    }
}
