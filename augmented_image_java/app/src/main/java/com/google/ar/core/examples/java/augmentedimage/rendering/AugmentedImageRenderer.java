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

  private Pose[] teapotPoses = new Pose[4];
  private float[] teapotDegrees = {0, 0, 0, 0};

  public AugmentedImageRenderer() {}

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

      //Debug Andys
      debugAndy0.createOnGlThread(
              context, "models/andy.obj", "models/andy.png");
      debugAndy0.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

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

    // OpenGL Matrix operation is in the order: Scale, rotation and Translation
    // We need to do this adjustment because the teapot obj file
    // is not centered around origin. Normally when you
    // work with your own model, you don't have this problem.

    final float teapot_edge_size = 132113.73f; // Magic number of teapot size
    final float max_image_edge = Math.max(augmentedImage.getExtentX(), augmentedImage.getExtentZ()); // Get largest detected image edge size
    Pose anchorPose = centerAnchor.getPose();

    float teapotScaleFactor = max_image_edge / (teapot_edge_size * 5); // scale to set Maze to image size
    float[] modelMatrix = new float[16];

    for (int i = 0; i < 4; ++i) {
      Pose teapotPose = teapotAnchors[i].getPose().compose(Pose.makeTranslation(
              262143.57f * teapotScaleFactor,
              -427295.75f * teapotScaleFactor,
              -218310.41f * teapotScaleFactor));
      teapotPoses[i] = teapotPose;
    }
    //Check for pickedUp
    if (pickedUpTeapot != -1) {
//        //float[] rotateDirectlyFromAbove = eulerAnglesRadToQuat((float)Math.toRadians((double)teapotDegrees[pickedUpTeapot]),0,0);
//        float[] quatCam = frame.getCamera().getPose().getRotationQuaternion();
//        float degreesToRot = getRoll(quatCam);
//        float[] rotateDirectlyFromAbove = eulerAnglesRadToQuat((float)Math.toRadians(degreesToRot),0,0);

        /* For reference, the center of the rotation is at
          teapotAnchors[0].getPose().compose(Pose.makeTranslation(
                  2.0f * 262143.57f * teapotScaleFactor,
                  -427295.75f * teapotScaleFactor,
                  2.0f * -218310.41f * teapotScaleFactor)).toMatrix(modelMatrix, 0);
         */

      teapotPoses[pickedUpTeapot] = frame.getCamera().getPose().compose(Pose.makeTranslation(0, 0, -0.15f).compose(Pose.makeRotation(0.7071068f, 0f, 0f, 0.7071068f)) //up pose 90 around x axis
              .compose(Pose.makeRotation(0f, 0.7071068f, 0f, 0.7071068f)) // rotate camera to be same orientation of teapots 90 around y axis
              //.compose(Pose.makeRotation(0f, frame.getCamera().getPose().extractRotation().qy(), 0f, frame.getCamera().getPose().extractRotation().qw()))
              //.compose(Pose.makeRotation(rotateDirectlyFromAbove))
              .compose(Pose.makeTranslation(
                      262143.57f * teapotScaleFactor,
                      -427295.75f * teapotScaleFactor,
                      -218310.41f * teapotScaleFactor)));
      //teapotDegrees[pickedUpTeapot] = teapotDegrees[pickedUpTeapot]+90; //because camera is rotated.

      //Log.i("aug image rotatae", centerAnchor.getPose().extractRotation().toString());

//        Pose andyPose0 = frame.getCamera().getPose().compose(Pose.makeTranslation(0, 0, -0.5f).compose(Pose.makeRotation(.66f,0f,0f,.77f)));
//
//        andyPose0.toMatrix(modelMatrix, 0);
//        debugAndy0.updateModelMatrix(modelMatrix, 1f, 1f, 1f);
//        debugAndy0.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
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

  /**Code remixed from libgdx Quaternion class
   * creates a quaternion from the given euler angles in radians.
   * @param yaw the rotation around the y axis in radians
   * @param pitch the rotation around the x axis in radians
   * @param roll the rotation around the z axis in radians
   * @return a quaternion as a float array*/
  public float[] eulerAnglesRadToQuat (float yaw, float pitch, float roll) {
    final float hr = roll * 0.5f;
    final float shr = (float)Math.sin(hr);
    final float chr = (float)Math.cos(hr);
    final float hp = pitch * 0.5f;
    final float shp = (float)Math.sin(hp);
    final float chp = (float)Math.cos(hp);
    final float hy = yaw * 0.5f;
    final float shy = (float)Math.sin(hy);
    final float chy = (float)Math.cos(hy);
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

    /* Following code from libgdx Quaternion library and has changed for this use case. Not importing the entire library since only a few functions are applicable.
   Source: https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java
   */
  /** Normalizes this quaternion to unit length
   * @return the quaternion for chaining */
  public float[] normalizeQuat(float[] quat) {
    float x = quat[0];
    float y = quat[1];
    float z = quat[2];
    float w = quat[3];
    float len = x * x + y * y + z * z + w * w; // length of the quarternion without sqrt
    if (len != 0.f && !(len == 1f)) {
      len = (float)Math.sqrt(len);
      w /= len;
      x /= len;
      y /= len;
      z /= len;
    }
    float[] newquat = {x, y, z ,w};
    return newquat;
  }


  /** Get the pole of the gimbal lock, if any.
   * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock */
  public int getGimbalPole (float[] quat) {
    float x = quat[0];
    float y = quat[1];
    float z = quat[2];
    float w = quat[3];
    final float t = y * x + z * w;
    return t > 0.499f ? 1 : (t < -0.499f ? -1 : 0);
  }

  /** Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
   * @return the rotation around the z axis in radians (between -PI and +PI) */
  public float getRollRad (float[] quat) {
    float x = quat[0];
    float y = quat[1];
    float z = quat[2];
    float w = quat[3];
    final int pole = getGimbalPole(quat);
    return (float) (pole == 0 ? Math.atan2((double)(2f * (w * z + y * x)), (double)(1f - 2f * (x * x + z * z))) : (float)pole * 2f
            * Math.atan2(y, w));
  }

  /** Get the roll euler angle in degrees, which is the rotation around the z axis. Requires that this quaternion is normalized.
   * @return the rotation around the z axis in degrees (between 0 and 360) */
  public float getRoll (float[] quat) {
    float[] newquat = normalizeQuat(quat);
    float degreeReturn = (float)Math.toDegrees((double)getRollRad(newquat)); // +180 to return between 0 and 360

//    //-90 to 180
    if(-90f <= degreeReturn && degreeReturn <= 180f) {
      return degreeReturn + 90f;
    }
    else {
      //-180 to -90
      return degreeReturn + 180 + 270;
    }
  }

  public void updateTeapotRotation(int teapotIndex, float degrees) {
    teapotDegrees[teapotIndex] = degrees;
  }

  public void changeByOffsetTeapotRotation(int teapotIndex, float offsetDegrees) {
    teapotDegrees[teapotIndex] += offsetDegrees; //can be neg, could be set up to -180 or 360+180
    if (teapotDegrees[teapotIndex] >= 360) {
      teapotDegrees[teapotIndex] = teapotDegrees[teapotIndex] % 360;
    }
    if(teapotDegrees[teapotIndex] < 0) {
      teapotDegrees[teapotIndex] = 360 + teapotDegrees[teapotIndex];
    }
  }

  public float[] calculateAndReturnRotationTeapot(Pose modelPose, float degree, float teapotScaleFactor) {
    //Because teapot is not centered in origin, figure out how the translation needs to change as a result
    //Assume teapot got moved so it was centered on the anchor...
    float teapot_x = 262143.57f*teapotScaleFactor;
    float teapot_z = 218310.41f*teapotScaleFactor;

    float[] modelMatrix = new float[16];
    modelPose.toMatrix(modelMatrix, 0);

    // in our program we only care about rotate on y
    Matrix.rotateM(modelMatrix, 0, degree, 0f, 1f, 0f);

    float[] trans = getTranslationToCenterCircle(degree, teapot_x, teapot_z);

    // No need to move it back to the center because rotation not at origin
    Matrix.translateM(modelMatrix, 0,trans[0],0f,trans[1]);

    return modelMatrix;
  }

  public float[] getTranslationToCenterCircle(float degree, float teapot_x, float teapot_z) {
    float[] returnTrans = new float[2]; //0 is x, 1 is z, since y axis is pointing into the picture
    returnTrans[0] = teapot_x;
    returnTrans[1] = -teapot_z;

    float radius = (float)Math.sqrt((double)(teapot_x*teapot_x + teapot_z*teapot_z));
    float angle = (float)Math.toDegrees(Math.atan((double)(teapot_z/teapot_x))); //angle of the rectangle of box for teapot

    //Now need to figure out how to move the teapot back to where the anchor is. I think this is based on the size of teapot
    //Not one of the 4 corners
    if (0 <= degree && degree < 90) {
      //Quad 1
      returnTrans[0] += -Math.cos(Math.toRadians(angle-degree)) * radius;
      returnTrans[1] += Math.sin(Math.toRadians(angle-degree)) * radius;
    } else if (90 <= degree && degree < 180) {
      //Quad 2
      float deg = degree-90f;
      returnTrans[0] += -Math.sin(Math.toRadians(angle-deg)) * radius;
      returnTrans[1] += -Math.cos(Math.toRadians(angle-deg)) * radius;
    }
    else if (180 <= degree && degree < 270) {
      //Quad 3
      float deg = degree-180f;
      returnTrans[0] += Math.cos(Math.toRadians(angle-deg)) * radius;
      returnTrans[1] += -Math.sin(Math.toRadians(angle-deg)) * radius;
    } else {
      float deg = degree-270f;
      returnTrans[0] += Math.sin(Math.toRadians(angle-deg)) * radius;
      returnTrans[1] += Math.cos(Math.toRadians(angle-deg)) * radius;
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
    return new float[] {red, green, blue, TINT_ALPHA};
  }
}
