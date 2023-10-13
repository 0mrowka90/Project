package com.example.project;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import android.graphics.PointF;
import android.graphics.Rect;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;


public class FaceRecognitionActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView previewView;

    Button bTakePicture, bRecording;
    private ImageCapture imageCapture;
    ImageAnalysis analyzer;

    private static final String TAG = "FaceRecognition";
    private String recognitionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        recognitionMode = getIntent().getStringExtra("mode");
        if(recognitionMode == null) {
            Toast.makeText(this, "Invalid mode, check intent", Toast.LENGTH_LONG).show();
            recognitionMode = "";
        }

        bTakePicture = findViewById(R.id.bCapture);
        bRecording = findViewById(R.id.bRecord);
        previewView = findViewById(R.id.previewView);

        bTakePicture.setOnClickListener(this);
        bRecording.setOnClickListener(this);


        if (allPermissionsGranted()) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    startCameraX(cameraProvider);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, ContextCompat.getMainExecutor(this));
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }


    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {

        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        analyzer = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        //analyzer.setAnalyzer(Executors.newSingleThreadExecutor(), new FaceAnalyzer());


        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, analyzer);
        //cameraProvider.bindToLifecycle(this, cameraSelector, analyzer);
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        capturePhoto();
    }

    private void capturePhoto() {
        analyzer.clearAnalyzer();
        analyzer.setAnalyzer(Executors.newSingleThreadExecutor(), new FaceAnalyzer());
    }

    private class FaceAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(ImageProxy imageProxy) {
            @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                detectFaces(image);
            }
        }
    }

    private void detectFaces(InputImage image) {
        // [START set_detector_options]
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);


        // [START run_detector]
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        Toast.makeText(FaceRecognitionActivity.this, "Its working", Toast.LENGTH_LONG).show();
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):
                                            PointF leftEarPos = null;
                                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {
                                                leftEarPos = leftEar.getPosition();
                                            }

                                            PointF rightEarPos = null;
                                            FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
                                            if (rightEar != null) {
                                                rightEarPos = rightEar.getPosition();
                                            }

                                            FaceRecognitionData data = new FaceRecognitionData(calculateDistance(leftEarPos, rightEarPos));
                                            Log.d(TAG, "Read face: " + data);

                                            if(recognitionMode.equals("register")) {
                                                //save face
                                                StorageManager.saveFaceRecognition(getDir("data", MODE_PRIVATE), data);
                                                Toast.makeText(FaceRecognitionActivity.this, "Face saved!", Toast.LENGTH_LONG).show();
                                                finishActivity("success");
                                            } else if(recognitionMode.equals("login")) {
                                                //compare faces
                                                Boolean success = recognizeFace(data);
                                                if(success != null) {
                                                    if(success) {
                                                        Toast.makeText(FaceRecognitionActivity.this, "Correct", Toast.LENGTH_LONG).show();
                                                        finishActivity("success");
                                                    } else {
                                                        Toast.makeText(FaceRecognitionActivity.this, "Invalid", Toast.LENGTH_LONG).show();
                                                    }
                                                } else {
                                                    Toast.makeText(FaceRecognitionActivity.this, "Face not stored!", Toast.LENGTH_LONG).show();
                                                }
                                            }

                                        }

                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                    }
                                });
        // [END run_detector]
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private Boolean recognizeFace(FaceRecognitionData newFace) {
        FaceRecognitionData savedFace = StorageManager.loadFaceRecognition(getDir("data", MODE_PRIVATE));
        if(savedFace == null) {
            return null;
        }
        if(Math.abs(newFace.getEarsDistance() - savedFace.getEarsDistance()) < 30) {
            return true;
        }
        else {
            return false;
        }
    }

    private double calculateDistance(PointF p1, PointF p2) {
        return Math.sqrt(Math.pow(p1.x-p2.x, 2) + Math.pow(p1.y-p2.y, 2));
    }

    private void finishActivity(String result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("resullt", result);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}