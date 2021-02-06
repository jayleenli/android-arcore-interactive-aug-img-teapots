/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.augmentedimage.rendering;

import android.content.Context;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer.BlendMode;
import java.io.IOException;
import android.util.Log;

import androidx.annotation.NonNull;

/** Renders an augmented image. */
public class AugmentedImageRenderer {
  private static final String TAG = "AugmentedImageRenderer";

  private static final float TINT_INTENSITY = 0.1f;
  private static final float TINT_ALPHA = 1.0f;
  private static final int[] TINT_COLORS_HEX = {
    0x000000, 0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4,
    0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800,
  };

  /*
    Instead of the image frame render the teapot 4 times
    Because the maze should attach to the image, it makes sense to put the mazeRenderer inside the AugmentedImageRenderer class.
   */


  //private ObjectRenderer[] teapotObjectRenderer = new ObjectRenderer[4];
  private final ObjectRenderer teapot0 = new ObjectRenderer();
  private final ObjectRenderer teapot1 = new ObjectRenderer();
  private final ObjectRenderer teapot2 = new ObjectRenderer();
  private final ObjectRenderer teapot3 = new ObjectRenderer();


  private Pose[] teapotPoses = new Pose[4];

//  private final ObjectRenderer mazeRenderer = new ObjectRenderer();

  public AugmentedImageRenderer() {}

  public void createOnGlThread(Context context) throws IOException {
//      teapotObjectRenderer[0] = teapot0;
//      teapotObjectRenderer[1] = teapot1;
//      teapotObjectRenderer[2] = teapot2;
//      teapotObjectRenderer[3] = teapot3;
//    mazeRenderer.createOnGlThread(
//            context, "models/Teapot.obj", "models/frame_base.png");
//    mazeRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);


      teapot0.createOnGlThread(
              context, "models/Teapot.obj", "models/frame_base.png");
      teapot0.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

      teapot1.createOnGlThread(
              context, "models/Teapot.obj", "models/frame_base.png");
      teapot1.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

      teapot2.createOnGlThread(
              context, "models/Teapot.obj", "models/frame_base.png");
      teapot2.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

      teapot3.createOnGlThread(
              context, "models/Teapot.obj", "models/frame_base.png");
      teapot3.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);


      //imageFrameUpperLeft.setBlendMode(BlendMode.AlphaBlending);
  }

  public void draw(
      float[] viewMatrix,
      float[] projectionMatrix,
      AugmentedImage augmentedImage,
      Anchor centerAnchor,
      float[] colorCorrectionRgba,
      @NonNull Frame frame,
      Anchor[] teapotAnchors) {
      float[] tintColor =
          convertHexToColor(TINT_COLORS_HEX[augmentedImage.getIndex() % TINT_COLORS_HEX.length]);

      //JAYLEEN: From the tutorial to load in custom obj. Reason for  this is because a lot of models are not centered at origin

//      final float maze_edge_size = 132113.73f; // Magic number of maze size
//      final float max_image_edge = Math.max(augmentedImage.getExtentX(), augmentedImage.getExtentZ()); // Get largest detected image edge size
//      Pose anchorPose = centerAnchor.getPose();
//
//      float mazsScaleFactor = max_image_edge / maze_edge_size; // scale to set Maze to image size
//      float[] modelMatrix = new float[16];
//
//      // OpenGL Matrix operation is in the order: Scale, rotation and Translation
//      // So the manual adjustment is after scale
//      // The 251.3f and 129.0f is magic number from the maze obj file
//      // We need to do this adjustment because the maze obj file
//      // is not centered around origin. Normally when you
//      // work with your own model, you don't have this problem.
//      Pose mozeModelLocalOffset = Pose.makeTranslation(
//              262143.57f * mazsScaleFactor,
//              -427295.75f * mazsScaleFactor,
//              -218310.41f * mazsScaleFactor);
//      anchorPose.compose(mozeModelLocalOffset).toMatrix(modelMatrix, 0);
//      mazeRenderer.updateModelMatrix(modelMatrix, mazsScaleFactor,mazsScaleFactor,mazsScaleFactor);
//      mazeRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

      final float teapot_edge_size = 132113.73f; // Magic number of teapot size
      final float max_image_edge = Math.max(augmentedImage.getExtentX(), augmentedImage.getExtentZ()); // Get largest detected image edge size
      Pose anchorPose = centerAnchor.getPose();

      float teapotScaleFactor = max_image_edge / (teapot_edge_size*5); // scale to set Maze to image size
      float[] modelMatrix = new float[16];

//      //Rotate teapot
//      float[] trans = {262143.57f * teapotScaleFactor + -0.0f * augmentedImage.getExtentX(),
//              -427295.75f * teapotScaleFactor + 0.0f,
//              -218310.41f * teapotScaleFactor + -0.3f * augmentedImage.getExtentZ()};
//      float[] rotate = {0.0f, 1.0f, 0.0f, 0.7071068f};
//      Pose teapotRotate = new Pose(trans, rotate);

//      Pose[] localBoundaryPoses = {
//          Pose.makeTranslation(
//                      262143.57f * teapotScaleFactor + -0.30f * augmentedImage.getExtentX(),
//                      -427295.75f * teapotScaleFactor + 0.0f,
//                      -218310.41f * teapotScaleFactor + -0.4f * augmentedImage.getExtentZ()), // upper mid left
//          Pose.makeTranslation(
//                  262143.57f * teapotScaleFactor + -0.10f * augmentedImage.getExtentX(),
//                  -427295.75f * teapotScaleFactor + 0.0f,
//                  -218310.41f * teapotScaleFactor + -0.4f * augmentedImage.getExtentZ()), // upper mid left
//          Pose.makeTranslation(
//                  262143.57f * teapotScaleFactor + 0.10f * augmentedImage.getExtentX(),
//                  -427295.75f * teapotScaleFactor + 0.0f,
//                  -218310.41f * teapotScaleFactor + -0.4f * augmentedImage.getExtentZ()), // upper mid right
//          Pose.makeTranslation(
//                  262143.57f * teapotScaleFactor + 0.3f * augmentedImage.getExtentX(),
//                  -427295.75f * teapotScaleFactor + 0.0f,
//                  -218310.41f * teapotScaleFactor + -0.4f * augmentedImage.getExtentZ()), // upper right
//        };
      //Log.i("Tag", "scale is " + teapotScaleFactor);

//      Pose[] worldBoundaryPoses = new Pose[4];
//
//
//      for (int i = 0; i < 4; ++i) {
//          worldBoundaryPoses[i] = anchorPose.compose(localBoundaryPoses[i]);
//      }

//      worldBoundaryPoses[0].toMatrix(modelMatrix, 0);
//      teapotLeft.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
//      teapotLeft.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

//      worldBoundaryPoses[1].toMatrix(modelMatrix, 0);
//      teapotMidLeft.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
//      teapotMidLeft.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
//
//      worldBoundaryPoses[2].toMatrix(modelMatrix, 0);
//      teapotMidRight.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
//      teapotMidRight.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
//
//      worldBoundaryPoses[3].toMatrix(modelMatrix, 0);
//      teapotRight.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
//      teapotRight.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

      for (int i = 0; i < 4; ++i) {
          Pose teapotPose = teapotAnchors[i].getPose().compose(Pose.makeTranslation(
                  262143.57f * teapotScaleFactor,
                  -427295.75f * teapotScaleFactor,
                  -218310.41f * teapotScaleFactor));
          teapotPoses[i] = teapotPose;

      }

      teapotPoses[0].toMatrix(modelMatrix, 0);
      teapot0.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
      teapot0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

      teapotPoses[1].toMatrix(modelMatrix, 0);
      teapot0.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
      teapot0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

      teapotPoses[2].toMatrix(modelMatrix, 0);
      teapot0.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
      teapot0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

      teapotPoses[3].toMatrix(modelMatrix, 0);
      teapot0.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
      teapot0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

//      //Check for intersection with camera
      Pose cameraPose = frame.getCamera().getPose();
      float dx = teapotPoses[0].tx() - cameraPose.tx();
      float dy = teapotPoses[0].ty() - cameraPose.ty();
      float dz = teapotPoses[0].tz() - cameraPose.tz();
      float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
      Log.i("setText","Distance from camera: " + distanceMeters + " metres");

//      frame.getCamera().getPose()
//              .compose(Pose.makeTranslation(0, 0, -1f))

//      Anchor anchor =  session.createAnchor(new Pose(pos, rotation));
//      anchorNode = new AnchorNode(anchor);
//      anchorNode.setRenderable(andyRenderable);
//      anchorNode.setParent(arFragment.getArSceneView().getScene());
  }

  private static float[] convertHexToColor(int colorHex) {
    // colorHex is in 0xRRGGBB format
    float red = ((colorHex & 0xFF0000) >> 16) / 255.0f * TINT_INTENSITY;
    float green = ((colorHex & 0x00FF00) >> 8) / 255.0f * TINT_INTENSITY;
    float blue = (colorHex & 0x0000FF) / 255.0f * TINT_INTENSITY;
    return new float[] {red, green, blue, TINT_ALPHA};
  }
}
