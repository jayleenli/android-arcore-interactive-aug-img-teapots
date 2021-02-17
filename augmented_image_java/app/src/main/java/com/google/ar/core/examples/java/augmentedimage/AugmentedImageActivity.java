/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.augmentedimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.examples.java.augmentedimage.rendering.AugmentedImageRenderer;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This app extends the HelloAR Java app to include image tracking functionality.
 *
 * <p>In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * AugmentedImage.getTrackingMethod() and render only when the tracking method equals to
 * FULL_TRACKING. See details in <a
 * href="https://developers.google.com/ar/develop/java/augmented-images/">Recognize and Augment
 * Images</a>.
 */
public class AugmentedImageActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final String TAG = AugmentedImageActivity.class.getSimpleName();

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;
  private ImageView fitToScanView;
  private RequestManager glideRequestManager;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

  private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
  private final AugmentedImageRenderer augmentedImageRenderer = new AugmentedImageRenderer();

  private boolean shouldConfigureSession = false;

  // Augmented image configuration and rendering.
  // Load a single image (true) or a pre-generated image database (false).
  private final boolean useSingleImage = true;
  // Augmented image and its associated center pose anchor, keyed by index of the augmented image in
  // the
  // database.
  private final Map<Integer, Pair<AugmentedImage, Anchor>> augmentedImageMap = new HashMap<>();

  DisplayMetrics displaymetrics = new DisplayMetrics();
  private int pickedUpTeapot = -1; //-1 if not picked up, id if true
  private boolean putDownDisabled = false;
  private boolean pickedUpDisabled = false;

  private Anchor[] teapotAnchors = {null, null, null, null};
  private float[] cameraPickUpRotation = new float[4];
  private float[] cameraPutDownRotation = new float[4];

  private Frame globalFrameVar;
  private float globalTeapotScaleFactor;
  private Pose globalCenterPose;
  private float[] cameraRotateForPickUp;

  float[][] teapotTranslations = new float[4][3];

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    //Listener for taps
    surfaceView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        Log.i("TOUCH", event.getX() + " ,  " + event.getY());

        //If pickup
        if (pickedUpTeapot == -1) {
          int teapot_touched = onTapHittingTeapotPickUp(event, globalFrameVar, globalTeapotScaleFactor);
          if (teapot_touched != -1 && !pickedUpDisabled && !(cameraTouchingBoundingSphere(globalFrameVar, teapotAnchors, globalTeapotScaleFactor) == teapot_touched)) {
            pickUpTeapot(teapot_touched);
          }
          //else do nothing, can't pick up
        } else {
          //we are holding a teapot
          Pose hitPose = onTapHittingAugImagePutDown(event, globalFrameVar);
          if (pickedUpTeapot != -1 && !putDownDisabled && hitPose != null && cameraTouchingImage(globalFrameVar) == null) {
            //Put down the teapot
            putDownTeapot(hitPose);
          }
          //Else can't do anything, we can't put down yet
        }
        return true;
      }
    });


    // Set up renderer.
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    surfaceView.setWillNotDraw(false);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);
    glideRequestManager = Glide.with(this);
    glideRequestManager
            .load(Uri.parse("file:///android_asset/fit_to_scan.png"))
            .into(fitToScanView);

    installRequested = false;
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      // Explicitly close ARCore Session to release native resources.
      // Review the API reference for important considerations before calling close() in apps with
      // more complicated lifecycle requirements:
      // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
      session.close();
      session = null;
    }

    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        session = new Session(/* context = */ this);
      } catch (UnavailableArcoreNotInstalledException
              | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (Exception e) {
        message = "This device does not support AR";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }

      shouldConfigureSession = true;
    }

    if (shouldConfigureSession) {
      configureSession();
      shouldConfigureSession = false;
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }
    surfaceView.onResume();
    displayRotationHelper.onResume();

    fitToScanView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(
              this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
    try {
      // Create the texture and pass it to ARCore session to be filled during update().
      backgroundRenderer.createOnGlThread(/*context=*/ this);
      augmentedImageRenderer.createOnGlThread(/*context=*/ this);
    } catch (IOException e) {
      Log.e(TAG, "Failed to read an asset file", e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    // Clear screen to notify driver it should not load any pixels from previous frame.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    if (session == null) {
      return;
    }
    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session);

    try {
      session.setCameraTextureName(backgroundRenderer.getTextureId());

      // Obtain the current frame from ARSession. When the configuration is set to
      // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
      // camera framerate.
      Frame frame = session.update();
      globalFrameVar = frame;
      Camera camera = frame.getCamera();

      // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
      trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

      // If frame is ready, render camera preview image to the GL surface.
      backgroundRenderer.draw(frame);

      // Get projection matrix.
      float[] projmtx = new float[16];
      camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

      // Get camera matrix and draw.
      float[] viewmtx = new float[16];
      camera.getViewMatrix(viewmtx, 0);

      // Compute lighting from average intensity of the image.
      final float[] colorCorrectionRgba = new float[4];
      frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

      // Visualize augmented images.
      drawAugmentedImages(frame, projmtx, viewmtx, colorCorrectionRgba);
    } catch (Throwable t) {
      // Avoid crashing the application due to unhandled exceptions.
      Log.e(TAG, "Exception on the OpenGL thread", t);
    }
  }

  private void configureSession() {
    Config config = new Config(session);
    config.setFocusMode(Config.FocusMode.AUTO);
    if (!setupAugmentedImageDatabase(config)) {
      messageSnackbarHelper.showError(this, "Could not setup augmented image database");
    }
    session.configure(config);
  }

  private void drawAugmentedImages(
          Frame frame, float[] projmtx, float[] viewmtx, float[] colorCorrectionRgba) {
    Collection<AugmentedImage> updatedAugmentedImages =
            frame.getUpdatedTrackables(AugmentedImage.class);

    // Iterate to update augmentedImageMap, remove elements we cannot draw.
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          String text = String.format("Detected Image %d", augmentedImage.getIndex());
          messageSnackbarHelper.showMessage(this, text);
          break;

        case TRACKING:
          // Have to switch to UI Thread to update View.
          this.runOnUiThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      fitToScanView.setVisibility(View.GONE);
                    }
                  });

          // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage.getIndex())) {
            Anchor centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
            augmentedImageMap.put(
                    augmentedImage.getIndex(), Pair.create(augmentedImage, centerPoseAnchor));
          }
          break;

        case STOPPED:
          augmentedImageMap.remove(augmentedImage.getIndex());
          break;

        default:
          break;
      }
    }

    // Draw all images in augmentedImageMap
    for (Pair<AugmentedImage, Anchor> pair : augmentedImageMap.values()) {
      AugmentedImage augmentedImage = pair.first;
      Anchor centerAnchor = augmentedImageMap.get(augmentedImage.getIndex()).second;
      globalCenterPose = centerAnchor.getPose();

      //Create 4 anchors for each teapot as well
      switch (augmentedImage.getTrackingState()) {
        case TRACKING:
          // Make anchors once
          if (teapotAnchors[0] == null) {
            teapotAnchors[0] = session.createAnchor(centerAnchor.getPose().compose(Pose.makeTranslation(
                    -0.3f * augmentedImage.getExtentX(),
                    0.0f,
                    -0.3f * augmentedImage.getExtentZ())));
            teapotAnchors[1] = session.createAnchor(centerAnchor.getPose().compose(Pose.makeTranslation(
                    -0.1f * augmentedImage.getExtentX(),
                    0.0f,
                    -0.3f * augmentedImage.getExtentZ())));
            teapotAnchors[2] = session.createAnchor(centerAnchor.getPose().compose(Pose.makeTranslation(
                    0.1f * augmentedImage.getExtentX(),
                    0.0f,
                    -0.3f * augmentedImage.getExtentZ())));
            teapotAnchors[3] = session.createAnchor(centerAnchor.getPose().compose(Pose.makeTranslation(
                    0.3f * augmentedImage.getExtentX(),
                    0.0f,
                    -0.3f * augmentedImage.getExtentZ())));

            float[] teapot0 = {-0.3f * augmentedImage.getExtentX(), 0.0f, -0.3f * augmentedImage.getExtentZ()};
            teapotTranslations[0] = teapot0;
            float[] teapot1 = {-0.1f * augmentedImage.getExtentX(), 0.0f, -0.3f * augmentedImage.getExtentZ()};
            teapotTranslations[1] = teapot1;
            float[] teapot2 = {0.1f * augmentedImage.getExtentX(), 0.0f, -0.3f * augmentedImage.getExtentZ()};
            teapotTranslations[2] = teapot2;
            float[] teapot3 = {0.3f * augmentedImage.getExtentX(), 0.0f, -0.3f * augmentedImage.getExtentZ()};
            teapotTranslations[3] = teapot3;
          } else {
            //Using anchors for the android because hard to do the translations back to image using only one anchor
            teapotAnchors[0].detach();
            teapotAnchors[1].detach();
            teapotAnchors[2].detach();
            teapotAnchors[3].detach();

            teapotAnchors[0] = session.createAnchor(centerAnchor.getPose().compose(
                    Pose.makeTranslation(teapotTranslations[0])));
            teapotAnchors[1] = session.createAnchor(centerAnchor.getPose().compose(
                    Pose.makeTranslation(teapotTranslations[1])));
            teapotAnchors[2] = session.createAnchor(centerAnchor.getPose().compose(
                    Pose.makeTranslation(teapotTranslations[2])));
            teapotAnchors[3] = session.createAnchor(centerAnchor.getPose().compose(
                    Pose.makeTranslation(teapotTranslations[3])));
          }

          //Calculate the scale factor
          final float teapot_edge_size = 132113.73f; // Calculated externally
          final float max_image_edge = Math.max(augmentedImage.getExtentX(), augmentedImage.getExtentZ()); // Get largest detected image edge size
          float teapotScaleFactor = max_image_edge / (teapot_edge_size * 5);

          globalTeapotScaleFactor = teapotScaleFactor;

          if (pickedUpTeapot != -1 && !putDownDisabled && cameraTouchingImage(frame) != null) {
            Pose hitPose = cameraTouchingImage(frame);
            putDownTeapot(hitPose);
          }

          //Check if camera is hitting one of the teapots
          int checkCameraTouching = cameraTouchingBoundingSphere(frame, teapotAnchors, globalTeapotScaleFactor);
          if (pickedUpTeapot == -1 && !pickedUpDisabled && checkCameraTouching != -1) {
            pickUpTeapot(checkCameraTouching);
          }

          augmentedImageRenderer.draw(
                  viewmtx, projmtx, augmentedImage, centerAnchor, colorCorrectionRgba, frame, teapotAnchors, pickedUpTeapot); //pass frame so we can do camera interaction

          break;
        default:
          break;
      }
    }
  }

  /*Given a Pose, find out the translation of it with regard to the center pose (AR image)
   *  Coordinate System looks like this with respect to image
   *            -z
   *             |
   *             |
   *  -x------------------+x
   *             |
   *             |
   *            +z
   */
  private void updateTranslationfromCenterAnchor(Pose pose, int teapotId) {
    float poseX = pose.tx();
    float poseZ = pose.tz();

    float anchorPoseX = globalCenterPose.tx();
    float anchorPoseZ = globalCenterPose.tz();

    float[] translate = new float[3];

    if (poseX > anchorPoseX) {
      translate[0] = poseX - anchorPoseX;
    } else {
      translate[0] = -(anchorPoseX - poseX);
    }

    if (poseZ > anchorPoseZ) {
      translate[2] = poseZ - anchorPoseZ;
    } else {
      translate[2] = -(anchorPoseZ - poseZ);
    }

    teapotTranslations[teapotId] = translate;
  }

  //Touch screen teapot pickup check
  private int onTapHittingTeapotPickUp(MotionEvent motionEvent, Frame frame, float teapotScaleFactor) {
    float x_pos = motionEvent.getX();
    float y_pos = motionEvent.getY();
    float teapot_r = (132113.73f / 2.0f) * teapotScaleFactor * 1.1f; // increase

    for (HitResult hit : frame.hitTest(x_pos, y_pos)) {
      Trackable trackable = hit.getTrackable();
      if (trackable instanceof AugmentedImage) {
        Pose poseHit = hit.getHitPose();

        //Now check if the poseHit is within bounding sphere of a teapot
        for (int teapot_id = 0; teapot_id < 4; teapot_id++) {
          float dx = teapotAnchors[teapot_id].getPose().tx() - poseHit.tx();
          float dy = teapotAnchors[teapot_id].getPose().ty() - poseHit.ty();
          float dz = teapotAnchors[teapot_id].getPose().tz() - poseHit.tz();
          float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

          if (distanceMeters < teapot_r) {
            Log.i("TOUCH", "hit " + teapot_id);
            return teapot_id;
          }
        }
      }
    }
    return -1;
  }

  //Touch screen teapot putdown check
  private Pose onTapHittingAugImagePutDown(MotionEvent motionEvent, Frame frame) {
    float x_pos = motionEvent.getX();
    float y_pos = motionEvent.getY();

    for (HitResult hit : frame.hitTest(x_pos, y_pos)) {
      Trackable trackable = hit.getTrackable();
      if (trackable instanceof AugmentedImage) {
        Pose poseHit = hit.getHitPose();
        return poseHit;
      }
    }
    return null;
  }

  private void pickUpTeapot(int teapot_id) {
    Log.i("HIT", "TOUCH BOUNDING TEAPOT ID " + teapot_id);

    pickedUpTeapot = teapot_id;
    cameraPickUpRotation = globalFrameVar.getCamera().getPose().getRotationQuaternion();
    setCameraRotateForPickUp(cameraPickUpRotation);

    putDownDisabled = true;
    Log.i("teapot put down", "put down is disabled!");
    //delay so you have time to pick up teapot
    new java.util.Timer().schedule(
            new java.util.TimerTask() {
              @Override
              public void run() {
                putDownDisabled = false;
                Log.i("teapot put down", "put down is enabled!");
              }
            },
            3000
    );
  }

  private void putDownTeapot(Pose hitPose) {
    Log.i("PUT DOWN", "PUTTING DOWN TEAPOT");

    updateTranslationfromCenterAnchor(hitPose, pickedUpTeapot);

    //calculate difference between the axis of teapot and axis of image
    cameraPutDownRotation = globalFrameVar.getCamera().getPose().getRotationQuaternion();

    //convert back to degrees
    //Only need z
    float putDownDeg = getRoll(cameraPutDownRotation);
    float pickUpDeg = getRoll(cameraPickUpRotation);
    float degreeOffset = getDiff(pickUpDeg, putDownDeg);

    augmentedImageRenderer.changeByOffsetTeapotRotation(pickedUpTeapot, degreeOffset);
    Log.i("roll", " CHANGE BY " + degreeOffset);

    pickedUpTeapot = -1;
    pickedUpDisabled = true;
    Log.i("teapot pick up", "pick up is disabled!");
    //delay so you have time to pick up teapot
    new java.util.Timer().schedule(
            new java.util.TimerTask() {
              @Override
              public void run() {
                pickedUpDisabled = false;
                Log.i("teapot pick up", "pick up is enabled!");
              }
            },
            3000
    );
  }

  //For rotating the camera so it is on same plane as it is on augmented image
  public void setCameraRotateForPickUp(float[] quat) {
    float[] newquat = normalizeQuat(quat);
    float rollRad = getRollRad(newquat);
    //Need to convert radians to convert to rotation that camera is using
    //pass to augmented image renderer and convert back into quaternion
    augmentedImageRenderer.updateCameraRotateForPickUp(eulerAnglesRadToQuat(-rollRad, 0, 0));
  }

  //camera to image teapot pickup check
  private int cameraTouchingBoundingSphere(Frame frame, Anchor[] teapotAnchors, float teapotScaleFactor) {
    float teapot_r = (132113.73f / 2.0f) * teapotScaleFactor * 1.1f; // increase
    getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    float y_pos = displaymetrics.heightPixels / 2.0f;
    float x_pos = displaymetrics.widthPixels / 2.0f;

    //Check if camera is hitting one of the teapots
    for (int teapot_id = 0; teapot_id < 4; teapot_id++) {
      for (HitResult hit : frame.hitTest(x_pos, y_pos)) {
        Trackable trackable = hit.getTrackable();
        if (trackable instanceof AugmentedImage) {
          Pose poseHit = hit.getHitPose();
          Pose cameraPose = frame.getCamera().getPose(); // need to check if camera is decently close
          float cdx = teapotAnchors[teapot_id].getPose().tx() - cameraPose.tx();
          float cdy = teapotAnchors[teapot_id].getPose().ty() - cameraPose.ty();
          float cdz = teapotAnchors[teapot_id].getPose().tz() - cameraPose.tz();
          float cdistanceMeters = (float) Math.sqrt(cdx * cdx + cdy * cdy + cdz * cdz);

          float dx = teapotAnchors[teapot_id].getPose().tx() - poseHit.tx();
          float dy = teapotAnchors[teapot_id].getPose().ty() - poseHit.ty();
          float dz = teapotAnchors[teapot_id].getPose().tz() - poseHit.tz();
          float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

          if (distanceMeters < teapot_r && cdistanceMeters <= .15f) {
            return teapot_id;
          }
        }
      }
    }
    return -1;
  }

  //camera to image teapot putdown pose return
  private Pose cameraTouchingImage(Frame frame) {
    getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    float y_pos = displaymetrics.heightPixels / 2.0f;
    float x_pos = displaymetrics.widthPixels / 2.0f;

    for (HitResult hit : frame.hitTest(x_pos, y_pos)) {
      Trackable trackable = hit.getTrackable();
      if (trackable instanceof AugmentedImage) {
        Pose poseHit = hit.getHitPose();
        Pose cameraPose = frame.getCamera().getPose(); // need to check if camera is decently close
        float dx = poseHit.tx() - cameraPose.tx();
        float dy = poseHit.ty() - cameraPose.ty();
        float dz = poseHit.tz() - cameraPose.tz();
        float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distanceMeters <= .15f) {
          return poseHit;
        }
      }
    }
    return null;
  }


  /* Gets the difference in angle based on this kind of rotation system(equal to opengl rotation, which is in degrees)
   * This is the reason for the multiple cases, so that the conversion can be kept to a 0-360 degree format with this kind of system
   *  Coordinate System looks like this Note: DEGREES
   *            270
   *             |
   *             |
   *  0------------------180
   *             |
   *             |
   *            90
   */

  public float getDiff(float pickUpDeg, float putDownDeg) {
    if (pickUpDeg == putDownDeg) {
      return 0;
    }

    if (pickUpDeg + 180 > 360) {
      float spillover = (pickUpDeg + 180) % 360;
      if (spillover > 0 && putDownDeg >= 0 && putDownDeg < spillover) {
        //forward but putdown is in spill over
        return putDownDeg + 360 - pickUpDeg;
      } else if (spillover >= 0 && pickUpDeg <= putDownDeg && putDownDeg <= 360) {
        //forward but putdown is not in spill over
        return putDownDeg - pickUpDeg;
      } else {
        //will always spill over but not in forward then it must be backward
        //if spill over here no way it can cross 0
        //backward
        return putDownDeg - pickUpDeg;
      }
    } else {
      // no spillover for forward
      if (pickUpDeg <= putDownDeg && putDownDeg <= pickUpDeg + 180) {
        //forward
        return putDownDeg - pickUpDeg;
      } else {
        //backward
        //Check if spillover for backward
        return -((360 - putDownDeg) + (pickUpDeg));
      }
    }
  }

  /* Following code from libgdx Quaternion library and has changed for this use case. Not importing the entire library since only a few functions are applicable.
   Source: https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java
   */

  /**
   * Normalizes this quaternion to unit length
   *
   * @return the quaternion for chaining
   */
  public float[] normalizeQuat(float[] quat) {
    float x = quat[0];
    float y = quat[1];
    float z = quat[2];
    float w = quat[3];
    float len = x * x + y * y + z * z + w * w; // length of the quarternion without sqrt
    if (len != 0.f && !(len == 1f)) {
      len = (float) Math.sqrt(len);
      w /= len;
      x /= len;
      y /= len;
      z /= len;
    }
    float[] newquat = {x, y, z, w};
    return newquat;
  }


  /**
   * Get the pole of the gimbal lock, if any.
   *
   * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
   */
  public int getGimbalPole(float[] quat) {
    float x = quat[0];
    float y = quat[1];
    float z = quat[2];
    float w = quat[3];
    final float t = y * x + z * w;
    return t > 0.499f ? 1 : (t < -0.499f ? -1 : 0);
  }

  /**
   * Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
   *
   * @return the rotation around the z axis in radians (between -PI and +PI)
   */
  public float getRollRad(float[] quat) {
    float x = quat[0];
    float y = quat[1];
    float z = quat[2];
    float w = quat[3];
    final int pole = getGimbalPole(quat);
    return (float) (pole == 0 ? Math.atan2((double) (2f * (w * z + y * x)), (double) (1f - 2f * (x * x + z * z))) : (float) pole * 2f
            * Math.atan2(y, w));
  }

  /**
   * Get the roll euler angle in degrees, which is the rotation around the z axis. Requires that this quaternion is normalized.
   *
   * @return the rotation around the z axis in degrees (between 0 and 360)
   */
  public float getRoll(float[] quat) {
    float[] newquat = normalizeQuat(quat);
    float degreeReturn = (float) Math.toDegrees((double) getRollRad(newquat)); // +180 to return between 0 and 360

    //-90 to 180
    if (-90f <= degreeReturn && degreeReturn <= 180f) {
      return degreeReturn + 90f;
    } else {
      //-180 to -90
      return degreeReturn + 180 + 270;
    }
  }

  /**
   * creates a quaternion from the given euler angles in radians.
   *
   * @param yaw   the rotation around the y axis in radians
   * @param pitch the rotation around the x axis in radians
   * @param roll  the rotation around the z axis in radians
   * @return a quaternion as a float array
   */
  public float[] eulerAnglesRadToQuat(float yaw, float pitch, float roll) {
    final float hr = roll * 0.5f;
    final float shr = (float) Math.sin(hr);
    final float chr = (float) Math.cos(hr);
    final float hp = pitch * 0.5f;
    final float shp = (float) Math.sin(hp);
    final float chp = (float) Math.cos(hp);
    final float hy = yaw * 0.5f;
    final float shy = (float) Math.sin(hy);
    final float chy = (float) Math.cos(hy);
    final float chy_shp = chy * shp;
    final float shy_chp = shy * chp;
    final float chy_chp = chy * chp;
    final float shy_shp = shy * shp;

    float x = (chy_shp * chr) + (shy_chp * shr); // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
    float y = (shy_chp * chr) - (chy_shp * shr); // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
    float z = (chy_chp * shr) - (shy_shp * chr); // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
    float w = (chy_chp * chr) + (shy_shp * shr); // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)

    float[] new_quat = {x, y, z, w};
    return new_quat;
  }

  private boolean setupAugmentedImageDatabase(Config config) {
    AugmentedImageDatabase augmentedImageDatabase;

    // There are two ways to configure an AugmentedImageDatabase:
    // 1. Add Bitmap to DB directly
    // 2. Load a pre-built AugmentedImageDatabase
    // Option 2) has
    // * shorter setup time
    // * doesn't require images to be packaged in apk.
    if (useSingleImage) {
      Bitmap augmentedImageBitmap = loadAugmentedImageBitmap();
      if (augmentedImageBitmap == null) {
        return false;
      }

      augmentedImageDatabase = new AugmentedImageDatabase(session);
      augmentedImageDatabase.addImage("image_name", augmentedImageBitmap);
      // If the physical size of the image is known, you can instead use:
      //     augmentedImageDatabase.addImage("image_name", augmentedImageBitmap, widthInMeters);
      // This will improve the initial detection speed. ARCore will still actively estimate the
      // physical size of the image as it is viewed from multiple viewpoints.
    } else {
      // This is an alternative way to initialize an AugmentedImageDatabase instance,
      // load a pre-existing augmented image database.
      try (InputStream is = getAssets().open("sample_database.imgdb")) {
        augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
      } catch (IOException e) {
        Log.e(TAG, "IO exception loading augmented image database.", e);
        return false;
      }
    }

    config.setAugmentedImageDatabase(augmentedImageDatabase);
    return true;
  }

  private Bitmap loadAugmentedImageBitmap() {
    Log.i("Loaded", "trying to load img!!");
    try (InputStream is = getAssets().open("default.jpg")) {
      Log.i("Loaded", "Loaded img!!");
      return BitmapFactory.decodeStream(is);
    } catch (IOException e) {
      Log.e(TAG, "IO exception loading augmented image bitmap.", e);
    }
    return null;
  }
}
