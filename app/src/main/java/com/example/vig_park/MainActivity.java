package com.example.vig_park;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.vig_park.myCameraView;

import com.example.vig_park.R;
import com.example.vig_park.data.GET_API;
import com.example.vig_park.model.GET_CODE;
import com.google.gson.GsonBuilder;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String APP_TAG = "retrofit-json-variable";
    private static final String BASE_URL = "http://192.168.48.131/";

    myCameraView                   cameraBridgeViewBase;
    BaseLoaderCallback             baseLoaderCallback;
    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private Mat mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private float                  mRelativeFaceSize   = 0.1f;
    private int                    mAbsoluteFaceSize   = 0;
    private int                    count = 0;

    private static final long      TIMER_DURATION = 2000L;
    private static final long      TIMER_INTERVAL = 100L;

    private CountDownTimer         mCountDownTimer;
    private CountDownTimer         mCountDownTimer2;
    private boolean         can_take_photo;

    private TextView textView;












    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);

        can_take_photo = true;
        mCountDownTimer = new CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {

            @Override
            public void onTick(long millisUntilFinished) {
                can_take_photo=false;
            }

            @Override
            public void onFinish() {
                count=0;
                can_take_photo=true;
                textView.setText("...");
            }
        }.start();
        mCountDownTimer2 = new CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                count=0;
                textView.setText("...");
            }
        }.start();


        cameraBridgeViewBase = (myCameraView) findViewById(R.id.myCameraView);
//        cameraBridgeViewBase.setVisibility(View.VISIBLE);
        cameraBridgeViewBase.setCameraIndex(0);

        cameraBridgeViewBase.setCvCameraViewListener(MainActivity.this);
        cameraBridgeViewBase.setMaxFrameSize(1280, 720);

        baseLoaderCallback = new BaseLoaderCallback(this) {

            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch (status) {
                    case BaseLoaderCallback.SUCCESS: {
                        Log.i(TAG, "OpenCV loaded successfully");

                        // Load native library after(!) OpenCV initialization
//                        System.loadLibrary("ndklibrarysample");

                        try {
                            // load cascade file from application resources
                            InputStream is = getResources().openRawResource(R.raw.haarcascade_russian_plate_number);
                            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                            mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
                            FileOutputStream os = new FileOutputStream(mCascadeFile);

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                            is.close();
                            os.close();

                            mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                            if (mJavaDetector.empty()) {
                                Log.e(TAG, "Failed to load cascade classifier");
                                mJavaDetector = null;
                            } else
                                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());


                            cascadeDir.delete();

                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                        }

                        cameraBridgeViewBase.enableView();
                    }
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };


        ///getWindow().addFlags(WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY);


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();

        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {


        mRgba = inputFrame.rgba();
       // mGray = inputFrame.gray();
        mGray = rotateMat(inputFrame.gray());

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
//            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (true) {
            if (true)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }
        mGray.release();
        Mat newMat = rotateMat(mRgba);

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {

            Imgproc.rectangle(newMat, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
            Log.i("MainActivity.this", "x:"+facesArray[0].x);
            Log.i("MainActivity.this", "y:"+facesArray[0].y);
            Log.i("MainActivity.this", "w:"+facesArray[0].width);
            Log.i("MainActivity.this", "h:"+facesArray[0].height);
            Log.e(TAG, "plate detected!");
            count++;
            if (can_take_photo) {
                switch(count){
                    case 50:
                        Log.i(TAG, "toast wait---------------------------------------------------------------------------------------------------");

                        textView.setText("Подождите");
                        mCountDownTimer2.cancel();
                        mCountDownTimer2.start();

                        count++;

                        break;


                    case 99:

                        cameraBridgeViewBase.setFace_array(facesArray);
                        String uuid = UUID.randomUUID().toString() + ".png";
                        cameraBridgeViewBase.takePicture(uuid);
                        mCountDownTimer.cancel();
                        mCountDownTimer.start();
                        count=0;
                        textView.setText("Фото сделано");
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                AlertDialog.Builder a_builder = new AlertDialog.Builder(MainActivity.this);
//                                a_builder.setMessage("Правильный ли номер?");
//                                ImageView image = new ImageView(this);
//                                image.setImageResource();
//                                a_builder.setView();
//                                a_builder.show();
//                            }
//                        });

                        break;
                    default:
                }
//                if (count == 10) {
//
//                    cameraBridgeViewBase.setFace_array(facesArray);
//                    String uuid = UUID.randomUUID().toString() + ".png";
//                    cameraBridgeViewBase.takePicture(uuid);
//                    mCountDownTimer.start();
//                    count=0;
//
//
//
//                }

            }

                            }












//            cameraBridgeViewBase.takePicture("test1.jpg");
        //TODO face detected
        //Intent intent = new Intent(ScreenSaver.this, MainActivity.class);
        // startActivity(intent);

        Imgproc.resize(newMat, mRgba, new Size(mRgba.width(), mRgba.height()));
        newMat.release();

        return mRgba;
    }



    //todo onCameraViewStarted
    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();

    }

    //todo on cameraviewstop
    @Override
    public void onCameraViewStopped(){
        mGray.release();
        mRgba.release();
    }

    //todo onDestroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }
    public static Bitmap rotateBitmap(Bitmap srcBitmap, String path) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(0));
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                break;
        }
        Bitmap destBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                srcBitmap.getHeight(), matrix, true);
        return destBitmap;
    }

    public Mat rotateMat(Mat matImage) {
        Mat rotated = matImage.t();
        Core.flip(rotated, rotated, 1);
        return rotated;
    }


    }


