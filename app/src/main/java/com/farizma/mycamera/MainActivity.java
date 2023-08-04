package com.farizma.mycamera;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;

public class MainActivity extends Activity {

    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_CODE = 10;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private Camera camera;
    private CameraPreview cameraPreview;
    private MediaRecorder mediaRecorder;
    private FrameLayout preview;
    private Button photo, video;
    private ImageButton capture, record, flip, pausePlayButton,toggleCardButton,gridViewOverlay;
    private TextView textRecording;
    private boolean isRecording = false;
    private boolean isPause = false;
    private boolean isBack = true;
    private File videoFile;
    private boolean imagesVisible = true;
    private ImageButton flashToggleButton;
    private boolean cardsVisible = true;

    private boolean isFlashOn = false;
    private boolean gridLinesVisible = true;
    private GridLinesView gridLinesView;

    private View flashView;










    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE);

        if (!allPermissionsGranted()) {
            finish();
        }

        capture = findViewById(R.id.capture);
        record = findViewById(R.id.record);
        flip = findViewById(R.id.flip);
        photo = findViewById(R.id.photo);
        video = findViewById(R.id.video);
        textRecording = findViewById(R.id.textRecording);
        pausePlayButton = findViewById(R.id.pausePlay);
        toggleCardButton = findViewById(R.id.toggleCardButton);
        flashToggleButton = findViewById(R.id.flashToggle);
        gridLinesView = findViewById(R.id.gridLinesView);
        ImageButton showHideGridButton = findViewById(R.id.showHideGridButton);
        flashView = findViewById(R.id.flashView);
        // Check the currently selected camera facing
        if (isBack) {
            // Front camera is not available, so hide the flash button
            if (!hasFrontCamera()) {
                flashToggleButton.setVisibility(View.GONE);
            } else {
                // Set the OnClickListener for the flash button
                flashToggleButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        toggleFlash();
                    }
                });
            }
        } else {
            // Disable the flash button when using the front camera
            flashToggleButton.setEnabled(false);
        }



        showHideGridButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle grid lines visibility when the button is clicked
                toggleGridLines();
            }
        });



        flashToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFlash();
            }
        });
        toggleCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCardsVisibility();
            }
        });

        // Add some sample images (you can add your own images here)







        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Turn on the flashlight before taking the picture
                turnOnFlash();

                // Capture the picture
                camera.takePicture(null, null, Picture);
            }
        });
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isBack) startRecording(Camera.CameraInfo.CAMERA_FACING_BACK);
                else startRecording(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
        });

        flip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipCamera();
            }
        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        if(isBack) start(Camera.CameraInfo.CAMERA_FACING_BACK);
        else  start(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private void start(int face) {
        if(camera != null)
            releaseCamera();

        camera = getCameraInstance(face);
        cameraPreview = new CameraPreview(this, camera);
        camera.setDisplayOrientation(90);

        preview = findViewById(R.id.cameraPreview);
        preview.addView(cameraPreview);

        capture.setVisibility(View.VISIBLE);
        record.setVisibility(View.INVISIBLE);

        photo.setTextColor(getColor(R.color.red));
        video.setTextColor(getColor(R.color.black));
    }

    private void toggleFlash() {
        // Check if the device has a camera with a flash
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            // Create a Camera object if not already created
            if (camera == null) {
                camera = getCameraInstance(isBack ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
            }

            // Check if the front camera is active, show the flash effect
            if (!isBack) {
                flashView.setVisibility(View.VISIBLE);
                flashView.animate().alpha(1).setDuration(300).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        flashView.setVisibility(View.INVISIBLE);
                        flashView.setAlpha(0);
                    }
                });
            } else {
                // Get the Camera.Parameters to control the flash
                Camera.Parameters params = camera.getParameters();

                // Check the current flash mode
                String flashMode = params.getFlashMode();

                // Toggle the flash based on the current state
                if (flashMode.equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH); // Turn on the flash
                    flashToggleButton.setImageResource(R.drawable.ic_flash_on); // Update ImageButton icon
                } else {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF); // Turn off the flash
                    flashToggleButton.setImageResource(R.drawable.ic_flash_off); // Update ImageButton icon
                }

                // Apply the new parameters to the camera
                camera.setParameters(params);
            }
        } else {
            // The device does not have a camera with a flash, show an error message or handle accordingly
            Toast.makeText(this, "Your device does not have a flash.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasFrontCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return true; // Front camera is available
            }
        }
        return false; // Front camera is not available
    }

    private void toggleGridLines() {
        // Toggle grid lines visibility and update the custom view's visibility
        gridLinesVisible = !gridLinesVisible;
        gridLinesView.setVisibility(gridLinesVisible ? View.VISIBLE : View.GONE);
    }

    private void toggleCardsVisibility() {
        LinearLayout cardLinearLayout = findViewById(R.id.cardLinearLayout);
        cardLinearLayout.removeAllViews(); // Clear previously added cards

        if (cardsVisible) {
            // Hide cards by removing them from the layout
            cardsVisible = false;
            toggleCardButton.setImageResource(R.drawable.ic_show_cards);
        } else {
            // Show cards by adding them to the layout
            cardsVisible = true;
            toggleCardButton.setImageResource(R.drawable.ic_hide_cards);

            // Add sample cards to the horizontal scroll view (you can add your own data here)
            addCard(R.drawable.hi2, "Card 1", "This is card 1 description.");
            addCard(R.drawable.hi2, "Card 2", "This is card 2 description.");
            addCard(R.drawable.hi2, "Card 3", "This is card 3 description.");
        }
    }

    private void addCard(int imageResId, String title, String description) {
        LinearLayout cardLinearLayout = findViewById(R.id.cardLinearLayout);
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_view_layout, cardLinearLayout, false);

        ImageView cardImage = cardView.findViewById(R.id.cardImage);
        TextView cardTitle = cardView.findViewById(R.id.cardTitle);
        TextView cardDescription = cardView.findViewById(R.id.cardDescription);

        cardImage.setImageResource(imageResId);
        cardTitle.setText(title);
        cardDescription.setText(description);

        cardLinearLayout.addView(cardView);
    }
    private void flipCamera() {
        if(isBack) {
            isBack = false;
            start(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else {
            isBack = true;
            start(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
    }

    private void turnOnFlash() {
        // Check if the device has a camera with a flash
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            // Get the Camera.Parameters to control the flash
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH); // Turn on the flash
            camera.setParameters(params);
            isFlashOn = true;
        }
    }
    private void turnOffFlash() {
        // Check if the device has a camera with a flash
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            // Get the Camera.Parameters to control the flash
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF); // Turn off the flash
            camera.setParameters(params);
            isFlashOn = false;
        }
    }

    // ... Your existing code ...

    private Camera.PictureCallback Picture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.startPreview();
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (isFlashOn) {
                turnOffFlash();
            }
            if(pictureFile == null) {
                Log.d("MediaFile", "Error creating media file, check storage permissions");
                return;
            }

            try {
                Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(1, info);
                Bitmap bitmap;

                if(isBack) bitmap = rotate(realImage, 90);
                else bitmap = rotate(realImage, 270);

                FileOutputStream fos = new FileOutputStream(pictureFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                Toast.makeText(MainActivity.this, R.string.saved_image, Toast.LENGTH_SHORT).show();
                //fos.write(data);
                fos.flush();
                fos.close();

                // scan to make it visible in gallery
                scanFile(pictureFile.toString());
            }catch (FileNotFoundException e) {
                Log.d("MediaFile", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("MediaFile", "Error accessing file: " + e.getMessage());
            }
        }
    };

    private boolean prepareVideoRecorder(int face) {
        videoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);

        camera = getCameraInstance(face);
        camera.setDisplayOrientation(90);
        mediaRecorder = new MediaRecorder();
        if(isBack) mediaRecorder.setOrientationHint(90);
        else mediaRecorder.setOrientationHint(270);
        camera.unlock();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mediaRecorder.setOutputFile(videoFile.toString());
        mediaRecorder.setPreviewDisplay(cameraPreview.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("VIDEO_RECORDER", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("VIDEO_RECORDER", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void startRecording(int face) {
        if(isRecording) {
            mediaRecorder.stop();
            releaseMediaRecorder();
            camera.lock();
            // inform the user that recording has stopped
            Toast.makeText(this, R.string.saved_video, Toast.LENGTH_SHORT).show();
            stopTimer();
            flip.setVisibility(View.VISIBLE);
            isRecording = false;
            // scan to make it visible in gallery
            scanFile(videoFile.toString());
        } else {
            if(prepareVideoRecorder(face)) {
                Toast.makeText(this, R.string.recording_start, Toast.LENGTH_SHORT).show();
                flip.setVisibility(View.INVISIBLE);
                mediaRecorder.start();
                // inform the user that recording has started
                startTimer();
                isRecording = true;
            } else {
                releaseMediaRecorder();
            }
        }
    }

    private void startTimer() {
        pausePlayButton.setVisibility(View.VISIBLE);
        pausePlayButton.setImageDrawable(getDrawable(R.drawable.ic_pause));
        isPause = false;
        //TODO: start timer
        textRecording.setText("Recording...");
    }

    private void stopTimer() {
        pausePlayButton.setVisibility(View.INVISIBLE);
        isPause = false;
        //TODO: stop timer
        textRecording.setText("");
    }

    public void playPause(View view) {
        if(!isPause) {
            //TODO: pause timer
            Toast.makeText(this, R.string.recording_pause, Toast.LENGTH_SHORT).show();
            textRecording.setText("Pause");
            mediaRecorder.pause();
            pausePlayButton.setImageDrawable(getDrawable(R.drawable.ic_play));
            isPause = true;
        } else {
            //TODO: resume timer
            textRecording.setText("Recording...");
            mediaRecorder.resume();
            pausePlayButton.setImageDrawable(getDrawable(R.drawable.ic_pause));
            isPause = false;
        }
    }

    public void switchMode(View view) {
        if(view.getId() == R.id.photo) {
            photo.setTextColor(getColor(R.color.red));
            video.setTextColor(getColor(R.color.black));
            record.setVisibility(View.INVISIBLE);
            capture.setVisibility(View.VISIBLE);
        }
        else {
            photo.setTextColor(getColor(R.color.black));
            video.setTextColor(getColor(R.color.red));
            capture.setVisibility(View.INVISIBLE);
            record.setVisibility(View.VISIBLE);
        }
    }

    private static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }

    private boolean allPermissionsGranted() {
        boolean flag = true;
        for(int i=0; i< REQUIRED_PERMISSIONS.length; i++)
            if(ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED)
                flag = false;
        return flag;
    }

    public Camera getCameraInstance(int face) {
        Camera c = null;
        try {
            c = Camera.open(face);
        }
        catch (Exception e) {
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
        }
        return c;
    }

    private  File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCamera");
        if(!mediaStorageDir.exists())
            if(!mediaStorageDir.mkdirs()) {
                Log.d("MyCamera", "Failed to create directory");
                return null;
            }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if(type == MEDIA_TYPE_IMAGE)
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpeg");
        else if(type == MEDIA_TYPE_VIDEO)
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        else
            return null;

        return mediaFile;
    }

    private void scanFile(String file) {
        MediaScannerConnection.scanFile(this,
                new String[]{file}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String s, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + s + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    private void releaseCamera() {
        if(camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void releaseMediaRecorder() {
        if(mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        releaseMediaRecorder();
    }
}