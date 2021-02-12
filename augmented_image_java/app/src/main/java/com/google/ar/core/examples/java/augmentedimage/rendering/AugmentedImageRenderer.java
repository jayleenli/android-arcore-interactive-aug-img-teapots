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
import com.google.ar.sceneform.math.Quaternion;

import java.io.IOException;

import android.opengl.Matrix;
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

  private final ObjectRenderer teapot0 = new ObjectRenderer();
  private final ObjectRenderer teapot1 = new ObjectRenderer();
  private final ObjectRenderer teapot2 = new ObjectRenderer();
  private final ObjectRenderer teapot3 = new ObjectRenderer();

  private final ObjectRenderer debugAndy0 = new ObjectRenderer();
  private final ObjectRenderer debugAndy1 = new ObjectRenderer();
  private final ObjectRenderer debugAndy2 = new ObjectRenderer();
  private final ObjectRenderer debugAndy3 = new ObjectRenderer();


  private Pose[] teapotPoses = new Pose[4];
  private float degreeIncTest = 0;

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

      //Debug Andys
      debugAndy0.createOnGlThread(
              context, "models/andy.obj", "models/andy.png");
      debugAndy0.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

      debugAndy1.createOnGlThread(
                context, "models/andy.obj", "models/andy.png");
      debugAndy1.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

      debugAndy2.createOnGlThread(
                context, "models/andy.obj", "models/andy.png");
      debugAndy2.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

      debugAndy3.createOnGlThread(
                context, "models/andy.obj", "models/andy.png");
      debugAndy3.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

      //imageFrameUpperLeft.setBlendMode(BlendMode.AlphaBlending);
  }

  public void draw(
      float[] viewMatrix,
      float[] projectionMatrix,
      AugmentedImage augmentedImage,
      Anchor centerAnchor,
      float[] colorCorrectionRgba,
      @NonNull Frame frame,
      Anchor[] teapotAnchors,
      int pickedUpTeapot) {
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
//      // We need to do this adjustment because the obj file
//      // is not centered around origin. Normally when you
//      // work with your own model, you don't have this problem.

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

      for (int i = 0; i < 4; ++i) {
          Pose teapotPose = teapotAnchors[i].getPose().compose(Pose.makeTranslation(
                  262143.57f * teapotScaleFactor,
                  -427295.75f * teapotScaleFactor,
                  -218310.41f * teapotScaleFactor));
          teapotPoses[i] = teapotPose;
      }
      //Check for pickedUp
      if (pickedUpTeapot != -1) {
        teapotPoses[pickedUpTeapot] = frame.getCamera().getPose().compose(Pose.makeTranslation(
                262143.57f * teapotScaleFactor,
                -427295.75f * teapotScaleFactor,
                -218310.41f * teapotScaleFactor)).compose(Pose.makeTranslation(0, 0, -0.1f));
      }

      //TESTING
      degreeIncTest += 90;
      if (degreeIncTest == 360) {
        degreeIncTest = 0;
      }
      modelMatrix = calculateAndReturnRotationTeapot(teapotPoses[0], degreeIncTest, teapotScaleFactor);


      //test.toMatrix(modelMatrix, 0);
      //TESTING

      teapot0.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
      teapot0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

      teapotPoses[1].toMatrix(modelMatrix, 0);
      teapot1.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
      teapot1.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

      teapotPoses[2].toMatrix(modelMatrix, 0);
      teapot2.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
      teapot2.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

    teapotAnchors[0].getPose().compose(Pose.makeTranslation(
            2.0f * 262143.57f * teapotScaleFactor,
            -427295.75f * teapotScaleFactor,
            2.0f * -218310.41f * teapotScaleFactor)).toMatrix(modelMatrix, 0);
    teapot3.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
    teapot3.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);


//      frame.getCamera().getPose()
//              .compose(Pose.makeTranslation(0, 0, -1f))

//      Anchor anchor =  session.createAnchor(new Pose(pos, rotation));
//      anchorNode = new AnchorNode(anchor);
//      anchorNode.setRenderable(andyRenderable);
//      anchorNode.setParent(arFragment.getArSceneView().getScene());
  }

  public void debug_draw(
          float[] viewMatrix,
          float[] projectionMatrix,
          AugmentedImage augmentedImage,
          Anchor targetAnchor,
          float[] colorCorrectionRgba,
          float[] point0, float[] point1, float[] point2, float[] point3) {
    float[] tintColor =
            convertHexToColor(TINT_COLORS_HEX[augmentedImage.getIndex() % TINT_COLORS_HEX.length]);
    float[] modelMatrix = new float[16];

    Pose andyPose0 = Pose.makeTranslation(
            point0[0],
            point0[1],
            point0[2]);

    andyPose0.toMatrix(modelMatrix, 0);
    debugAndy0.updateModelMatrix(modelMatrix, .1f, .1f, .1f);
    debugAndy0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

    Pose andyPose1 = Pose.makeTranslation(
            point1[0],
            point1[1],
            point1[2]);

    andyPose1.toMatrix(modelMatrix, 0);
    debugAndy1.updateModelMatrix(modelMatrix, .1f, .1f, .1f);
    debugAndy1.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);


    Pose andyPose2 = Pose.makeTranslation(
            point2[0],
            point2[1],
            point2[2]);

    andyPose2.toMatrix(modelMatrix, 0);
    debugAndy2.updateModelMatrix(modelMatrix, .1f, .1f, .1f);
    debugAndy2.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
    
    Pose andyPose3 = Pose.makeTranslation(
            point3[0],
            point3[1],
            point3[2]);

    andyPose3.toMatrix(modelMatrix, 0);
    debugAndy3.updateModelMatrix(modelMatrix, .1f, .1f, .1f);
    debugAndy3.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
}

  public float[] calculateAndReturnRotationTeapot(Pose modelPose, float degree, float teapotScaleFactor) {
    //Because teapot is not centered in origin, figure out how the translation needs to change as a result
    //Assume teapot got moved so it was centered on the anchor...
    // My brain is too small to understand
    float teapot_x = 262143.57f*teapotScaleFactor;
    float teapot_z = 218310.41f*teapotScaleFactor;

    float[] modelMatrix = new float[16];
    modelPose.toMatrix(modelMatrix, 0);

    // in our program we only care about rotate on y
    Matrix.rotateM(modelMatrix, 0, degree, 0f, 1f, 0f);

    float[] trans = getTranslationToCenterCircle(degree, teapot_x, teapot_z);

    // No need to move it back to the center because rotation not at origin
    Matrix.translateM(modelMatrix, 0,trans[0],0f,trans[1]);

    //Translate it back to where the anchor is
    //Matrix.translateM(modelMatrix, 0,-teapot_x,0f,-teapot_z);

//    Pose testP = new Pose(modelMatrix, test);
//    Pose rot = Pose.makeRotation(0f, 1f, 0f, 45f);

    return modelMatrix;
  }

  public float[] getTranslationToCenterCircle(float degree, float teapot_x, float teapot_z) {
    float[] returnTrans = new float[2]; //0 is x, 1 is z, since y axis is pointing into the picture
    if (degree == 0) {
      returnTrans[0] = teapot_x;
      returnTrans[1] = -teapot_z;

      //translate back to pose
      returnTrans[0] += -teapot_x;
      returnTrans[1] += teapot_z;
      return returnTrans;
    } else if (degree == 90) {
      returnTrans[0] = teapot_x;
      returnTrans[1] = -teapot_z;

      //translate back to pose
      returnTrans[0] += -teapot_x;
      returnTrans[1] += -teapot_z;
      return returnTrans;
    } else if (degree == 180) {
      returnTrans[0] = teapot_x;
      returnTrans[1] = -teapot_z;

      //translate back to pose
      returnTrans[0] += teapot_x;
      returnTrans[1] += -teapot_z;
      return returnTrans;
    } else if (degree == 270) {
      returnTrans[0] = teapot_x;
      returnTrans[1] = -teapot_z;

      //translate back to pose
      returnTrans[0] += teapot_x;
      returnTrans[1] += teapot_z;
      return returnTrans;
    }

    returnTrans[0] = 0f;
    returnTrans[1] = 0f;
    return returnTrans;
//    //Not one of the 4 corners
//    if (0 < degree && degree < 90) {
//      //Quad 1
//      double x1 = Math.cos(Math.toRadians(degree)) * teapot_z;
//      double z1 = Math.sin(Math.toRadians(degree)) * teapot_z;
//
//      double x2 = Math.sin(Math.toRadians(degree)) * teapot_x;
//      double z2 = Math.cos(Math.toRadians(degree)) * teapot_x;
//
//      returnTrans[0] = (float)getDiff(x1, x2);
//      returnTrans[1] = (float)(z1 + z2);
//    } else if (90 < degree && degree < 180) {
//      //Quad 2
//      float deg = degree-90f;
//      double x1 = Math.sin(Math.toRadians(deg)) * teapot_z;
//      double z1 = Math.cos(Math.toRadians(deg)) * teapot_z;
//
//      double x2 = Math.cos(Math.toRadians(deg)) * teapot_x;
//      double z2 = Math.sin(Math.toRadians(deg)) * teapot_x;
//
//      returnTrans[0] = -1*(float)(x1 + x2);
//      returnTrans[1] = (float)getDiff(z1, z2);
//    } else if (180 < degree && degree < 270) {
//      //Quad 3
//      float deg = degree-180f;
//      double x1 = Math.cos(Math.toRadians(deg)) * teapot_z;
//      double z1 = Math.sin(Math.toRadians(deg)) * teapot_z;
//
//      double x2 = Math.sin(Math.toRadians(deg)) * teapot_x;
//      double z2 = Math.cos(Math.toRadians(deg)) * teapot_x;
//
//      returnTrans[0] = -1*(float)getDiff(x1, x2);
//      returnTrans[1] = -1*(float)(z1 + z2);
//    } else {
//      //Quad 4
//      float deg = degree-270f;
//      double x1 = Math.cos(Math.toRadians(deg)) * teapot_z;
//      double z1 = Math.sin(Math.toRadians(deg)) * teapot_z;
//
//      double x2 = Math.sin(Math.toRadians(deg)) * teapot_x;
//      double z2 = Math.cos(Math.toRadians(deg)) * teapot_x;
//
//      returnTrans[0] = (float)(x1 + x2);
//      returnTrans[1] = -1 * (float)getDiff(z1, z2);
//    }
//    return returnTrans;
  }

  public double getDiff(double x, double y) {
    if (x == y) {
      return 0;
    }
    if (x > y) {
      return Math.abs(x-y);
    }
    else {
      return Math.abs(y-x);
    }
  }
  public void debug_draw(
          float[] viewMatrix,
          float[] projectionMatrix,
          AugmentedImage augmentedImage,
          Anchor targetAnchor,
          float[] colorCorrectionRgba,
          float[] point0) {
    float[] tintColor =
            convertHexToColor(TINT_COLORS_HEX[augmentedImage.getIndex() % TINT_COLORS_HEX.length]);
    float[] modelMatrix = new float[16];

    Pose andyPose0 = Pose.makeTranslation(
            point0[0],
            point0[1],
            point0[2]);

    andyPose0.toMatrix(modelMatrix, 0);
    debugAndy0.updateModelMatrix(modelMatrix, .05f, .05f, .05f);
    debugAndy0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
  }
  private static float[] convertHexToColor(int colorHex) {
    // colorHex is in 0xRRGGBB format
    float red = ((colorHex & 0xFF0000) >> 16) / 255.0f * TINT_INTENSITY;
    float green = ((colorHex & 0x00FF00) >> 8) / 255.0f * TINT_INTENSITY;
    float blue = (colorHex & 0x0000FF) / 255.0f * TINT_INTENSITY;
    return new float[] {red, green, blue, TINT_ALPHA};
  }
}
