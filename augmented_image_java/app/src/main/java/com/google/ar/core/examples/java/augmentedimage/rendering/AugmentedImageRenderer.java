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

import java.io.IOException;

import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Renders an augmented image.
 */
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

  private Pose[] teapotPoses = new Pose[4];
  private float[] teapotDegrees = {0, 0, 0, 0};

  private float[] cameraRotateForPickUp = {0f, 0.7071068f, 0f, 0.7071068f};

  public AugmentedImageRenderer() {
  }

  public void createOnGlThread(Context context) throws IOException {
    teapot0.createOnGlThread(
            context, "models/Teapot.obj", "models/teapot_texture.png");
    teapot0.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

    teapot1.createOnGlThread(
            context, "models/Teapot.obj", "models/teapot_texture.png");
    teapot1.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

    teapot2.createOnGlThread(
            context, "models/Teapot.obj", "models/teapot_texture.png");
    teapot2.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

    teapot3.createOnGlThread(
            context, "models/Teapot.obj", "models/teapot_texture.png");
    teapot3.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

    //Debug Andy
    debugAndy0.createOnGlThread(
            context, "models/andy.obj", "models/andy.png");
    debugAndy0.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
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

    // OpenGL Matrix operation is in the order: Scale, rotation and Translation
    // We need to do this adjustment because the teapot obj file
    // is not centered around origin. Normally when you
    // work with your own model, you don't have this problem.

    final float teapot_edge_size = 132113.73f; // Magic number of teapot size
    final float max_image_edge = Math.max(augmentedImage.getExtentX(), augmentedImage.getExtentZ()); // Get largest detected image edge size

    float teapotScaleFactor = max_image_edge / (teapot_edge_size * 5); // scale to set Maze to image size
    float[] modelMatrix;

    for (int i = 0; i < 4; ++i) {
      Pose teapotPose = teapotAnchors[i].getPose().compose(Pose.makeTranslation(
              262143.57f * teapotScaleFactor,
              -427295.75f * teapotScaleFactor,
              -218310.41f * teapotScaleFactor));
      teapotPoses[i] = teapotPose;
    }

    //Check for pickedUp
    if (pickedUpTeapot != -1) {
      teapotPoses[pickedUpTeapot] = frame.getCamera().getPose().compose(Pose.makeTranslation(0, 0, -0.15f).compose(Pose.makeRotation(0.7071068f, 0f, 0f, 0.7071068f)) //up pose 90 around x axis
              .compose(Pose.makeRotation(cameraRotateForPickUp)) // to align with the way teapot is with respect to image.
              .compose(Pose.makeTranslation(
                      262143.57f * teapotScaleFactor,
                      -427295.75f * teapotScaleFactor,
                      -218310.41f * teapotScaleFactor)));
    }

    modelMatrix = calculateAndReturnRotationTeapot(teapotPoses[0], teapotDegrees[0], teapotScaleFactor);
    teapot0.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
    teapot0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

    modelMatrix = calculateAndReturnRotationTeapot(teapotPoses[1], teapotDegrees[1], teapotScaleFactor);
    teapot1.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
    teapot1.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

    modelMatrix = calculateAndReturnRotationTeapot(teapotPoses[2], teapotDegrees[2], teapotScaleFactor);
    teapot2.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
    teapot2.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

    modelMatrix = calculateAndReturnRotationTeapot(teapotPoses[3], teapotDegrees[3], teapotScaleFactor);
    teapot3.updateModelMatrix(modelMatrix, teapotScaleFactor, teapotScaleFactor, teapotScaleFactor);
    teapot3.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
  }

  public void updateTeapotRotation(int teapotIndex, float degrees) {
    teapotDegrees[teapotIndex] = degrees;
  }

  //To change the rotation of teapot when picked up to be same direction as when it was picked up
  public void updateCameraRotateForPickUp(float[] newRotation) {
    cameraRotateForPickUp = newRotation;
  }

  /*Change the rotation of teapot using the same degree rotation system
   *  Coordinate System looks like this Note: DEGREES
   *            270
   *             |
   *             |
   *  0------------------180
   *             |
   *             |
   *            90
   */
  public void changeByOffsetTeapotRotation(int teapotIndex, float offsetDegrees) {
    teapotDegrees[teapotIndex] += offsetDegrees; //can be neg, could be set up to -180 or 360+180
    if (teapotDegrees[teapotIndex] >= 360) {
      teapotDegrees[teapotIndex] = teapotDegrees[teapotIndex] % 360;
    }
    if (teapotDegrees[teapotIndex] < 0) {
      teapotDegrees[teapotIndex] = 360 + teapotDegrees[teapotIndex];
    }
  }

  /*
   * augmented image axis was
   *            -z
   *             |
   *             |
   *  -x------------------+x
   *             |
   *             |
   *            +z
   *
   */
  public float[] calculateAndReturnRotationTeapot(Pose modelPose, float degree, float teapotScaleFactor) {
    //Because teapot is not centered in origin, figure out how the translation needs to change as a result
    //Assume teapot got moved so it was centered on the anchor...
    float teapot_x = 262143.57f * teapotScaleFactor;
    float teapot_z = 218310.41f * teapotScaleFactor;

    float[] modelMatrix = new float[16];
    modelPose.toMatrix(modelMatrix, 0);

    // in our program we only care about rotate on y
    Matrix.rotateM(modelMatrix, 0, degree, 0f, 1f, 0f);

    float[] trans = getTranslationToCenterCircle(degree, teapot_x, teapot_z);

    // Now need to move it back to the center because rotation not at origin
    Matrix.translateM(modelMatrix, 0, trans[0], 0f, trans[1]);

    return modelMatrix;
  }

  /*Because the teapot was not centered at origin, do some math... to translate it back....
   * note to self: try to never use a model that is not centered at origin ever again...
   *  Coordinate System looks like this Note: DEGREES
   *            270
   *       Q4    |   Q3
   *             |
   *  0------------------180
   *       Q1    |    Q2
   *             |
   *            90
   *
   * *note1
   *  Because teapot not at origin, rotation would not seem to be at center.
   * So need to move it +x and -z to make it seem like it would be at center since rotating also changed the axis of the teapot, as it was relative to the teapot.
   * Because the obj file looked something like this(once teapot was on augmented image)
   *             -z
   *             |
   *             |
   *  -x-------------------+x
   *       T     |
   *             |
   *            +z
   * After rotation and following code, Teapot is now at
   *    returnTrans[0] = teapot_x;
   *    returnTrans[1] = -teapot_z;
   *
   *            -z
   *             |
   *             |
   *  -x---------T---------+x
   *             |
   *             |
   *            +z
   *
   *
   *  *note2, then needed to translate it back to the anchor positions, which was using this kind of coordinate axis
   * teapot axis was I think. where A is the anchor that could be located now anywhere after rotation but on same direction. So had to find the angle the axis rotated and translate T to wherever A is.
   *            -z
   *             |
   *             |
   *  -x---------T---------+x
   *             |
   *     A       |
   *            +z
   *
   * purpose was to move rotated teapot back to the center of where its rotation is (but not move it back to teapot anchor).
   * So had to calculate the triangles to move it back to the center
   */
  public float[] getTranslationToCenterCircle(float degree, float teapot_x, float teapot_z) {
    float[] returnTrans = new float[2]; //0 is x, 1 is z, since y axis is pointing into the picture
    //*note1 This moves the teapot back to center point of rotation (0,0)
    returnTrans[0] = teapot_x;
    returnTrans[1] = -teapot_z;

    float radius = (float) Math.sqrt((double) (teapot_x * teapot_x + teapot_z * teapot_z));
    float angle = (float) Math.toDegrees(Math.atan((double) (teapot_z / teapot_x))); //angle of the rectangle of box for teapot

    //*note2 Now need to figure out how to move the teapot back to where the anchor is. based on the size of teapot
    if (0 <= degree && degree < 90) {
      //Quad 1
      returnTrans[0] += -Math.cos(Math.toRadians(angle - degree)) * radius;
      returnTrans[1] += Math.sin(Math.toRadians(angle - degree)) * radius;
    } else if (90 <= degree && degree < 180) {
      //Quad 2
      float deg = degree - 90f;
      returnTrans[0] += -Math.sin(Math.toRadians(angle - deg)) * radius;
      returnTrans[1] += -Math.cos(Math.toRadians(angle - deg)) * radius;
    } else if (180 <= degree && degree < 270) {
      //Quad 3
      float deg = degree - 180f;
      returnTrans[0] += Math.cos(Math.toRadians(angle - deg)) * radius;
      returnTrans[1] += -Math.sin(Math.toRadians(angle - deg)) * radius;
    } else {
      float deg = degree - 270f;
      returnTrans[0] += Math.sin(Math.toRadians(angle - deg)) * radius;
      returnTrans[1] += Math.cos(Math.toRadians(angle - deg)) * radius;
    }
    return returnTrans;
  }

  public void debug_draw(
          float[] viewMatrix,
          float[] projectionMatrix,
          AugmentedImage augmentedImage,
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
    debugAndy0.updateModelMatrix(modelMatrix, .25f, .25f, .25f);
    debugAndy0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
  }

  private static float[] convertHexToColor(int colorHex) {
    // colorHex is in 0xRRGGBB format
    float red = ((colorHex & 0xFF0000) >> 16) / 255.0f * TINT_INTENSITY;
    float green = ((colorHex & 0x00FF00) >> 8) / 255.0f * TINT_INTENSITY;
    float blue = (colorHex & 0x0000FF) / 255.0f * TINT_INTENSITY;
    return new float[]{red, green, blue, TINT_ALPHA};
  }
}
