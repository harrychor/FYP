package com.example.ar_test_01;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener{
    private ArFragment arView;
    private Config config;
    private Session session;
    private boolean shouldConfigureSession=false;
    private final Map<AugmentedImage, AR_Node> augmentedImageMap = new HashMap<>();
    private Switch focusModeSwitch;

    ViewRenderable show_name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //for view
        arView =  (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arView);
        arView.getPlaneDiscoveryController().hide();
        arView.getPlaneDiscoveryController().setInstructionView(null);
        arView.getArSceneView().getPlaneRenderer().setEnabled(false);

        //request permission
        Dexter.withActivity(this).withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setupSession();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "Permission need camera", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();



        initSceneView();
    }



    private void initSceneView() {
        arView.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
    }

    private void setupSession() {
        if(session == null){
            try{
                session = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e){
                e.printStackTrace();
            } catch (UnavailableApkTooOldException e){
                e.printStackTrace();
            } catch (UnavailableSdkTooOldException e){
                e.printStackTrace();
            } catch (UnavailableDeviceNotCompatibleException e){
                e.printStackTrace();
            }
            shouldConfigureSession =true;
        }
        if(shouldConfigureSession){
            configSession();
            shouldConfigureSession=false;
            arView.getArSceneView().setupSession(session);
        }
        try {
            session.resume();
            arView.getArSceneView().resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
            session = null;
            return;
        }
    }

    private void configSession() {
        Config config = new Config(session);
        if (!buildDatabase(config)){
            Toast.makeText(this, "Error Database", Toast.LENGTH_SHORT).show();
        }
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        config.setFocusMode(Config.FocusMode.AUTO);
        session.configure(config);
    }

    private boolean buildDatabase(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;
    //    Bitmap bitmap = loadImage();
    //    if(bitmap == null)
    //        return false;
        try {
            InputStream inputStream = getAssets().open("edmtdev.imgdb");
            augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, inputStream);
            config.setAugmentedImageDatabase(augmentedImageDatabase);
            return true;
        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    private Bitmap loadImage(){
        try {
            InputStream is = getAssets().open("test_ar.jpg");
            return BitmapFactory.decodeStream(is);
        } catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public void onUpdate(FrameTime frameTime){
        Frame frame = arView.getArSceneView().getArFrame();
        final ImageView view = findViewById(R.id.image_view_fit_to_scan);
        if (frame == null) {
            return;
        }
        Collection<AugmentedImage> updateAugmentedImg = frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage image:updateAugmentedImg){
            switch (image.getTrackingState()) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    String text = "Detected Image "+image.getName();
                    SnackbarHelper.getInstance().showMessage(this, text);
                    break;
                case TRACKING:
                    // Have to switch to UI Thread to update View.
                    view.setVisibility(View.GONE);
                if (image.getName().equals("halo.jpg")) {
                    AR_Node node = new AR_Node(this, R.raw.halo);
                    node.setImage(image);
                    augmentedImageMap.put(image, node);
                    arView.getArSceneView().getScene().addChild(node);
                    show_text();
                    view.setVisibility(View.INVISIBLE);
                } else if (image.getName().equals("ball.jpg")) {
                    AR_Node node = new AR_Node(this, R.raw.ball);
                    node.setImage(image);
                    augmentedImageMap.put(image, node);
                    arView.getArSceneView().getScene().addChild(node);
                    show_text();
                    view.setVisibility(View.INVISIBLE);
                }
                case STOPPED:
                    augmentedImageMap.remove(image);
                    break;
            }


            /*
            if(image.getTrackingState() == TrackingState.TRACKING) {


                } else if (image.getName().equals("halo.jpg")) {
                    AR_Node node = new AR_Node(this, R.raw.halo);
                    node.setImage(image);
                    arView.getScene().addChild(node);
                    view.setVisibility(View.INVISIBLE);
                    String text = "Detected Image ";
                    SnackbarHelper.getInstance().showMessage(this, text);
                } else if (image.getName().equals("ball.jpg")) {
                    AR_Node node = new AR_Node(this, R.raw.ball);
                    node.setImage(image);
                    arView.getScene().addChild(node);
                    view.setVisibility(View.INVISIBLE);
                    String text = "Detected Image ";
                    SnackbarHelper.getInstance().showMessage(this, text);
                } else {
                    //view.setVisibility(View.VISIBLE);
                }
            }

             */
        }
    }

    private void show_text(){
        ViewRenderable.builder()
                .setView(this, R.layout.show_text)
                .build()
                .thenAccept(renderable -> show_name = renderable)
        ;



    }


    @Override
    protected void onResume(){
        super.onResume();
        final ImageView view = findViewById(R.id.image_view_fit_to_scan);
        if (augmentedImageMap.isEmpty()) {
            view.setVisibility(View.VISIBLE);
        }
        Dexter.withActivity(this).withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setupSession();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "Permission need camera", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(session != null){
            arView.getArSceneView().pause();
            session.pause();
        }
    }





}
