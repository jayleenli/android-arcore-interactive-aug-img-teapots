Assignment 4: Interactive Teapots

How to run:
- Open this project within Android Studio.
- Connect desired device. (I used my Google Pixel 4, I do not have any other android devices to test with)
- Run Project
- The image it recognizes is "default.jpg" (picture of the earth from augmented images hello world) in the home of the project library. This was chosen because it does better when tracked than other images tested.

Approach:
Using ARCore directly, and Java OpenGL, which caused a lot of difficulties (listed below)
-Used bounding sphere instead of bounding boxes to simplify things.

Difficulties:
- Attempted to use Sceneform, but saw that the library is now deprecated, so support is to incoporate the deprecated library causes some difficulities. I was able to import it into the project but did not get to use it as I already made a lot of progress with just ARCore and JavaGL so refactoring at that point would have been unreasonable.
- I found a teapot model online off of Google poly, but the model was not centered at the origin. This caused a lot of headaches when trying to rotate it around the axis to do the interaction. I spent a majority of this assignment trying to figure out the math of how to rotate the object and translate it back to the anchor that it was located at. 
- Other difficulties was different libraries did rotations differently. Some started with axis x on a coordinate system, others started with y, so translating them to be consistent (I choose from y and going counter clockwise) also caused difficulty.
-quarternion translation system is not very intuitive but Euler system is. Going back and forth from them was hard to keep track of.
-Rotation axis rules change depending on object. For example, the camera in ARCore is like OpenGL (OpenGL camera pose with +X pointing right, +Y pointing right up, and -Z pointing in the direction the camera is looking) but augmented img has rotation axis (pos y pointing outwards from the img, pos z pointing downwards parellel to img, and pos x pointing to the right). 
-See comment in code above getTranslationToCenterCircle() function for my attempt to explain why this was so frustrating. I forgot a bit of the steps since I did it a while back but I think I was able to follow my logic again to document it better.
-Stayed with the default augmentedImages hello world for Android image because it did better when moving camera close to img. I tried a black and white QR code tracker but it didn't do so well.


External Sources:
-Used this library to find out how to translate between Quaternions and euler
https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java
-Started with augmented image sample from and added on top of it.
https://github.com/google-ar/arcore-android-sdk/