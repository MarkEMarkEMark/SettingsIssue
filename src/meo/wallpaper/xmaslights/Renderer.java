package meo.wallpaper.xmaslights;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import javax.microedition.khronos.opengles.GL10;
import meo.wallpaper.xmaslights.materials.XmasParticleMaterial;
import rajawali.animation.Animation3D;
import rajawali.animation.Animation3D.RepeatMode;
import rajawali.animation.EllipticalOrbitAnimation3D;
import rajawali.animation.EllipticalOrbitAnimation3D.OrbitDirection;
import rajawali.animation.RotateAnimation3D;
import rajawali.animation.TranslateAnimation3D;
import rajawali.materials.textures.ATexture.TextureException;
import rajawali.materials.textures.Texture;
import rajawali.math.Vector3;
import rajawali.renderer.RajawaliRenderer;
import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

public class Renderer extends RajawaliRenderer {
	private static final String TAG_SETTINGS = "MEOTagSettings";

	// Settings (values, and flags that show values just changed) >>>>
	String meoprefMode;
	boolean prefModeChanged;

	Set<String> meoprefPixelLayout;
	boolean prefPixelLayoutChanged;

	Set<String> meoprefPatterns;
	boolean prefPatternsChanged;

	Set<String> meoprefCrossfades;
	boolean prefCrossfadesChanged;

	int meoprefStarburst;
	boolean prefStarburstChanged;

	int meoprefFadeTime;
	boolean prefFadeTimeChanged;

	int meoprefLayoutTime;
	boolean prefFadeLayoutChanged;

	int meoprefDisplayTime;
	boolean prefDisplayTimeChanged;

	int meoprefDev;
	boolean prefDevChanged;
	
	int meoprefRotate;
	boolean prefRotateChanged;

	int meoprefSize;
	boolean prefSizeChanged;
	// <<<<

	private final int NUM_PIXELS = 256;
	private final int PIXELS_SQROOT = (int) Math.sqrt(NUM_PIXELS); // for cube

	private final int NUM_PROGS = 20; // number of light patterns
	private final int NUM_FADES = 8; // number of fade programs
	private final int MAX_LEVEL = 256; // maximum brightness level

	private final float pi = 3.1415926535897932384626433832795f;
	private final float two_pi = 2f * pi; // 360 degrees or 2pi radians

	// Pattern+, Pattern-, Variation+, Variation-, Shape+, Shape-
	private int control = 0;

	// how the lights are laid out on the screen
	private final int NUM_ARRG = 6; // number of layouts (arrangements)
	private final int ANIM_SPEED = 1500; // speed of change
	private int arrangmentNo = 0;
	// so doesn't try when screen is touched anywhere:
	private boolean animationAllowed = false;
	Animation3D[] animTranslate = new TranslateAnimation3D[NUM_PIXELS];
	Animation3D[] animRotate = new RotateAnimation3D[NUM_PIXELS];

	// camera possibilities
	private int cameraNo = 0;
	private int lastCameraNo = 0;
	// private long lastCamDuration;
	// Light Program number for back & front images
	private int[] patternNo = new int[2];
	// variation number for each pattern
	private int[][] variationNo = new int[2][NUM_PROGS]; // not sure if 1st
															// index is needed
	// Program number for crossfade
	private int xFadeNo;
	// Countdown to next transition
	private int tCounter = -1;
	// Countdown to next layout change
	private int lCounter = -1;

	// for one-off random numbers
	// rand.nextInt(max - min + 1) + min; - but remember these are mostly 0
	// based
	private Random rand = new Random();

	public int backImgIdx = 0;

	private MultiParticleSystem[] mPixelString = new MultiParticleSystem[NUM_PIXELS];

	// Camera orbit
	EllipticalOrbitAnimation3D camAnim = new EllipticalOrbitAnimation3D(
			new Vector3(), new Vector3(2, 3, 131), 0, 360,
			OrbitDirection.CLOCKWISE);

	// display size
	private int maxX;
	private int maxY;

	public Renderer(Context context) {
		super(context);
		setFrameRate(60);

		// Get current display size
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		maxX = dm.widthPixels;
		maxY = dm.heightPixels;
		// Log.i(TAG_SETTINGS, "Max co-ords: " + maxX + ", " + maxY);
	}

	protected void initScene() {
		// Preferences
		// setOnPreferenceChange();
		setPrefsToLocal();

		// Camera
		// getCurrentCamera().setPosition(-12.0f, 0, 100);
		getCurrentCamera().setLookAt(0.0f, 0.0f, 0.0f);
		// getCurrentCamera().setFogEnabled(false);
		getCurrentCamera().setFarPlane(500.0f);

		camAnim.setDuration(20000);
		camAnim.setRepeatMode(RepeatMode.INFINITE);
		camAnim.setTransformable3D(getCurrentCamera());
		registerAnimation(camAnim);
		camAnim.play();

		// Materials
		XmasParticleMaterial materialMultiParticle = new XmasParticleMaterial();
		try {
			materialMultiParticle.addTexture(new Texture(
					R.drawable.starburst128_sheet));
		} catch (TextureException e) {
			e.printStackTrace();
		}

		// positions, blending and adding to scene
		float spiralX;
		float spiralY;
		float spiralZ;
		float radius = 0.15f;
		int bulbID;
		for (bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {
			mPixelString[bulbID] = new MultiParticleSystem();
			mPixelString[bulbID].setMaterial(materialMultiParticle);
			mPixelString[bulbID].setPointSize((float) meoprefSize);
			mPixelString[bulbID].setTransparent(true);
			mPixelString[bulbID].setBlendingEnabled(true);
			mPixelString[bulbID].setBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);

			spiralY = ((float) bulbID) / NUM_PIXELS * 256f;
			spiralZ = (float) (spiralY * radius * Math.sin(radius * spiralY));
			spiralX = (float) (spiralY * radius * Math.cos(radius * spiralY));

			mPixelString[bulbID].setPosition(spiralX,
					(-spiralY / 3.0f) + 48.0f, spiralZ);
			addChild(mPixelString[bulbID]);
		}

		// MEO Initialisation
	}

	public void onDrawFrame(GL10 glUnused) {
		super.onDrawFrame(glUnused);

		// Debugger - comment out when not using... **********
		// android.os.Debug.waitForDebugger();

		// get settings (note - always executed, as listener is unreliable)
		setPrefsToLocal();
		changeBulbSize();

		if (prefDevChanged) {
			prefDevChanged = false;

			for (int bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {
				if (meoprefDev == 0) {
					mPixelString[bulbID].setTransparent(true);
					mPixelString[bulbID].setBlendingEnabled(true);
					mPixelString[bulbID].setBlendFunc(GLES20.GL_ONE,
							GLES20.GL_ONE);
					camAnim.setRepeatMode(RepeatMode.INFINITE);
					camAnim.setDuration(10000);
					camAnim.reset();
					camAnim.play();
					animationAllowed = true;
				} else {
					mPixelString[bulbID].setTransparent(false);
					mPixelString[bulbID].setBlendingEnabled(false);
					mPixelString[bulbID].setBlendFunc(GLES20.GL_NONE,
							GLES20.GL_NONE);
					// lastCamDuration = camAnim.getDuration();
					camAnim.setDuration(1);
					camAnim.setRepeatMode(RepeatMode.NONE);
					mPixelString[bulbID].setPosition(
							((float) ((bulbID / 16) - 8) * 3.5f) + 2f,
							(float) ((bulbID % 16) - 8) * 6.5f, 0.0f);
					mPixelString[bulbID].setPointSize(30.0f);
					// camAnim.pause();
				}
			}
		}
		
		//rotate or not - not quite working right when "set wallpaper", as the flag has already been reset
		if (prefRotateChanged) {
			prefRotateChanged = false;
			if ((meoprefRotate == 1) && (meoprefDev == 0)){
				camAnim.setRepeatMode(RepeatMode.INFINITE);
				camAnim.setDuration(10000);
				camAnim.reset();
				camAnim.play();
			}else{
				camAnim.setDuration(1);
				camAnim.setRepeatMode(RepeatMode.NONE);
			}
			
		}

		// WS2801 Stuff from - LightFrame
		int frontImgIdx = 1 - backImgIdx; // only ever 1 or 0
		int redPixelColor, grnPixelColor, bluPixelColor;
		int i;

		//runSelectedLightProgram(backImgIdx);

		// layout change
		if ((lCounter > 0) && (meoprefDev == 0)) {
			animationAllowed = true;
			arrangmentNo++;
			if (arrangmentNo == NUM_ARRG) {
				arrangmentNo = 0;
			}
			changeLayout();
			animationAllowed = false;
		} else {
			lCounter++;
		}

		// Front render and composite only happen during transitions...
		//if (tCounter > 0) {
			// Transition in progress
			// int *frontPtr = &pixelState[frontImgIdx][0];
			//float alpha, invAlpha;

			// Render front image and alpha mask based on current effect
			// indices...
			//crossfades.tCounter = tCounter;
			//patterns.tCounter = tCounter;

			//runSelectedLightProgram(frontImgIdx);
			//runSelectedCrossfade();

			/*for (i = 0; i < NUM_PIXELS; i++) {
				alpha = crossfades.fadeMask[i];
				invAlpha = 1.0f - alpha;

				redPixelColor = (int) (((float) patterns.pixelState[frontImgIdx][(i * 3) + 0] * alpha) + ((float) patterns.pixelState[backImgIdx][(i * 3) + 0] * invAlpha));
				grnPixelColor = (int) (((float) patterns.pixelState[frontImgIdx][(i * 3) + 1] * alpha) + ((float) patterns.pixelState[backImgIdx][(i * 3) + 1] * invAlpha));
				bluPixelColor = (int) (((float) patterns.pixelState[frontImgIdx][(i * 3) + 2] * alpha) + ((float) patterns.pixelState[backImgIdx][(i * 3) + 2] * invAlpha));

				mPixelString[i].setOverlayColor(redPixelColor / 255f,
						grnPixelColor / 255f, bluPixelColor / 255f, meoprefDev);
				mPixelString[i].setAnimFrame(meoprefStarburst);

			}

		} else {
			// No transition in progress; just show back image
			for (i = 0; i < NUM_PIXELS; i++) {
				// See note above re: r, g, b vars.
				redPixelColor = patterns.pixelState[backImgIdx][(i * 3) + 0];
				grnPixelColor = patterns.pixelState[backImgIdx][(i * 3) + 1];
				bluPixelColor = patterns.pixelState[backImgIdx][(i * 3) + 2];

				mPixelString[i].setOverlayColor(redPixelColor / 255f,
						grnPixelColor / 255f, bluPixelColor / 255f, meoprefDev);
				mPixelString[i].setAnimFrame(meoprefStarburst);
			}
		}

		// Count up to next transition (or end of current one):
		tCounter++;
		crossfades.tCounter = tCounter;
		patterns.tCounter = tCounter;
		if (tCounter == 0) { // Transition start

			// how to change pattern/variation
			switch (control) {
			case 0: // Pattern+
				patternNo[frontImgIdx]++;
				// skip any patterns not ticked in prefs
				skipProgram(frontImgIdx);
				if (patternNo[frontImgIdx] == NUM_PROGS) {
					patternNo[frontImgIdx] = 0;
				}
				break;
			case 1: // Pattern-
				patternNo[frontImgIdx]--;
				// skip any patterns not ticked in prefs
				skipProgram(frontImgIdx);
				if (patternNo[frontImgIdx] == -1) {
					patternNo[frontImgIdx] = NUM_PROGS - 1;
				}
				break;
			case 2: // Variation+ //currently only reloads pattern with random
					// variation
				variationNo[frontImgIdx][patternNo[frontImgIdx]]++;
				if (variationNo[frontImgIdx][patternNo[frontImgIdx]] == 65535) {
					variationNo[frontImgIdx][patternNo[frontImgIdx]] = 0;
				}
				break;
			}

			control = 0; // revert to pattern++

			// use next crossfade, and skip if not in list
			xFadeNo++;
			if (xFadeNo == NUM_FADES) {
				xFadeNo = 0;
			}
			skipCrossfade();

			crossfades.transitionTime = meoprefFadeTime;

			// Effect not yet initialized
			patterns.isInitialised[frontImgIdx] = false;
			// Transition not yet initialized //
			patterns.isInitialised[2] = false;

		} else if (tCounter >= crossfades.transitionTime) { // End transition
			// Move front effect index to back
			patternNo[backImgIdx] = patternNo[frontImgIdx];
			backImgIdx = 1 - backImgIdx; // Invert back index
			tCounter = -meoprefDisplayTime; // -120 - random(240);
			crossfades.tCounter = -meoprefDisplayTime; // -120 - random(240);
			patterns.tCounter = -meoprefDisplayTime; // -120 - random(240);
		}*/
	}

	// ***************************************************

	// skip patterns based of preferences - I'm embarrassed how this function
	// works!

	private void skipProgram(int idx) {
		int lastProg = patternNo[idx];

		// if none are checked, then use solid color only
		if (meoprefPatterns.isEmpty()) {
			patternNo[idx] = 0;
		} else {

			if (patternNo[idx] == 0) {
				if (!meoprefPatterns.contains("Solid Color")) {
					if (control == 0) {
						patternNo[idx] = 1;
					} else if (control == 1) {
						patternNo[idx] = NUM_PROGS - 1;
					}
				}
			}

			if (patternNo[idx] == 1) {
				if (!meoprefPatterns.contains("Simplex Noise")) {
					if (control == 0) {
						patternNo[idx] = 2;
					} else if (control == 1) {
						patternNo[idx] = 0;
					}
				}
			}

			if (patternNo[idx] == 2) {
				if (!meoprefPatterns.contains("Line Fade")) {
					if (control == 0) {
						patternNo[idx] = 3;
					} else if (control == 1) {
						patternNo[idx] = 1;
					}
				}
			}

			if (patternNo[idx] == 3) {
				if (!meoprefPatterns.contains("Chaser")) {
					if (control == 0) {
						patternNo[idx] = 4;
					} else if (control == 1) {
						patternNo[idx] = 2;
					}
				}
			}

			if (patternNo[idx] == 4) {
				if (!meoprefPatterns.contains("Rainbow")) {
					if (control == 0) {
						patternNo[idx] = 5;
					} else if (control == 1) {
						patternNo[idx] = 3;
					}
				}
			}

			if (patternNo[idx] == 5) {
				if (!meoprefPatterns.contains("Wave")) {
					if (control == 0) {
						patternNo[idx] = 6;
					} else if (control == 1) {
						patternNo[idx] = 4;
					}
				}
			}

			if (patternNo[idx] == 6) {
				if (!meoprefPatterns.contains("Wave Phasing")) {
					if (control == 0) {
						patternNo[idx] = 7;
					} else if (control == 1) {
						patternNo[idx] = 5;
					}
				}
			}

			if (patternNo[idx] == 7) {
				if (!meoprefPatterns.contains("Plasma")) {
					if (control == 0) {
						patternNo[idx] = 8;
					} else if (control == 1) {
						patternNo[idx] = 6;
					}
				}
			}

			if (patternNo[idx] == 8) {
				if (!meoprefPatterns.contains("Strobe Fade")) {
					if (control == 0) {
						patternNo[idx] = 9;
					} else if (control == 1) {
						patternNo[idx] = 7;
					}
				}
			}

			if (patternNo[idx] == 9) {
				if (!meoprefPatterns.contains("Splash Fade")) {
					if (control == 0) {
						patternNo[idx] = 10;
					} else if (control == 1) {
						patternNo[idx] = 8;
					}
				}
			}

			if (patternNo[idx] == 10) {
				if (!meoprefPatterns.contains("Slow Fill")) {
					if (control == 0) {
						patternNo[idx] = 11;
					} else if (control == 1) {
						patternNo[idx] = 9;
					}
				}
			}

			if (patternNo[idx] == 11) {
				if (!meoprefPatterns.contains("Larson Scanners")) {
					if (control == 0) {
						patternNo[idx] = 12;
					} else if (control == 1) {
						patternNo[idx] = 10;
					}
				}
			}

			if (patternNo[idx] == 12) {
				if (!meoprefPatterns.contains("Solid Color Pulse")) {
					if (control == 0) {
						patternNo[idx] = 13;
					} else if (control == 1) {
						patternNo[idx] = 11;
					}
				}
			}

			if (patternNo[idx] == 13) {
				if (!meoprefPatterns.contains("Random Strobe")) {
					if (control == 0) {
						patternNo[idx] = 14;
					} else if (control == 1) {
						patternNo[idx] = 12;
					}
				}
			}

			if (patternNo[idx] == 14) {
				if (!meoprefPatterns.contains("Larson Heartbeat")) {
					if (control == 0) {
						patternNo[idx] = 15;
					} else if (control == 1) {
						patternNo[idx] = 13;
					}
				}
			}

			if (patternNo[idx] == 15) {
				if (!meoprefPatterns.contains("Flames")) {
					if (control == 0) {
						patternNo[idx] = 16;
					} else if (control == 1) {
						patternNo[idx] = 14;
					}
				}
			}

			if (patternNo[idx] == 16) {
				if (!meoprefPatterns.contains("Wavy Flag")) {
					if (control == 0) {
						patternNo[idx] = 17;
					} else if (control == 1) {
						patternNo[idx] = 15;
					}
				}
			}

			if (patternNo[idx] == 17) {
				if (!meoprefPatterns.contains("Complimentary Fade")) {
					if (control == 0) {
						patternNo[idx] = 18;
					} else if (control == 1) {
						patternNo[idx] = 16;
					}
				}
			}

			if (patternNo[idx] == 18) {
				if (!meoprefPatterns.contains("Random Levels")) {
					if (control == 0) {
						patternNo[idx] = 19;
					} else if (control == 1) {
						patternNo[idx] = 17;
					}
				}
			}

			if (patternNo[idx] == 19) {
				if (!meoprefPatterns.contains("Gentle Random Fade")) {
					if (control == 0) {
						patternNo[idx] = 0;
					} else if (control == 1) {
						patternNo[idx] = 18;
					}
				}
			}

			// keep running this function until settles down - i.e. doens't
			// change program any more
			if (patternNo[idx] != lastProg) {
				skipProgram(idx);
			}
		}
	}

	

	// skip crossfades based of preferences
	private void skipCrossfade() {
		int lastProg = xFadeNo;

		// if none are checked, then use simple fade only
		if (meoprefCrossfades.isEmpty()) {
			xFadeNo = 0;
		} else {

			if (xFadeNo == 0) {
				if (!meoprefCrossfades.contains("Simple Fade")) {
					xFadeNo = 1;
				}
			}

			if (xFadeNo == 1) {
				if (!meoprefCrossfades.contains("Side to Side Wipe")) {
					xFadeNo = 2;
				}
			}

			if (xFadeNo == 2) {
				if (!meoprefCrossfades.contains("Shutters Wipe")) {
					xFadeNo = 3;
				}
			}

			if (xFadeNo == 3) {
				if (!meoprefCrossfades.contains("Random Fade")) {
					xFadeNo = 4;
				}
			}

			if (xFadeNo == 4) {
				if (!meoprefCrossfades.contains("Interlaced Wipe")) {
					xFadeNo = 5;
				}
			}

			if (xFadeNo == 5) {
				if (!meoprefCrossfades.contains("Double Wipe")) {
					xFadeNo = 6;
				}
			}

			if (xFadeNo == 6) {
				if (!meoprefCrossfades.contains("Centre Wipe")) {
					xFadeNo = 7;
				}
			}

			if (xFadeNo == 7) {
				if (!meoprefCrossfades.contains("Dithered Fade")) {
					xFadeNo = 0;
				}
			}

			// keep running this function until settles down - i.e. doens't
			// change crossfade any more
			if (xFadeNo != lastProg) {
				skipCrossfade();
			}
		}
	}

	

	// ================ Touch ================================

	public void onTouchEvent(MotionEvent e) {
		float x = (float) e.getRawX() / (float) maxX;
		float y = (float) e.getRawY() / (float) maxY;

		// Log.i(TAG_SETTINGS, "Co-ords: " + x + ", " + y);

		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// Top of screen

			if (y < 0.33f) { // pattern change
				if (x > 0.5f) {
					control = 0;
				} else {
					control = 1;
				}
				if (tCounter < 0) { // only if not already crossfading
					tCounter = -1; // trigger crossfade
				}
				// bottom of screen
			} else if ((y > 0.6667f) && (y < 0.85f)) { // arrangement or
														// camera change
				control = 99; // i.e. ignore

				if ((x > 0.5f) && (meoprefDev == 0)) {
					animationAllowed = true;
					arrangmentNo++;
					if (arrangmentNo == NUM_ARRG) {
						arrangmentNo = 0;
					}
				} else {

					// ToDo: another setting

				}
				// middle of screen
			} else { // variation change
				if (x > 0.5f) {
					control = 2;
				} else { // texture change
					control = 3;
				}
				if (tCounter < 0) { // only if not already crossfading
					tCounter = -1; // trigger crossfade
				}
			}
			break;
		}

		// positions
		changeLayout();

		// handle camera changes
		// ****** change to resetting up the cameras from scratch
		if (cameraNo != lastCameraNo) {
			switch (cameraNo) {
			case 0:// rotate
					// camAnim = new EllipticalOrbitAnimation3D(new
					// Vector3(),
					// new Vector3(2, 3, 131), 0, 360,
					// OrbitDirection.CLOCKWISE);
				camAnim.setDuration(10000);
				// camAnim.setRepeatMode(RepeatMode.INFINITE);
				// camAnim.setTransformable3D(getCurrentCamera());
				// registerAnimation(camAnim);
				camAnim.play();
				lastCameraNo = 0;
				break;
			case 1:// slower
					// camAnim = new EllipticalOrbitAnimation3D(new
					// Vector3(),
					// new Vector3(2, 3, 131), 0, 360,
					// OrbitDirection.CLOCKWISE);
				camAnim.setDuration(100000);
				// camAnim.setRepeatMode(RepeatMode.INFINITE);
				// camAnim.setTransformable3D(getCurrentCamera());
				// registerAnimation(camAnim);
				camAnim.play();
				lastCameraNo = 1;
				break;
			default:// still
				camAnim = new EllipticalOrbitAnimation3D(new Vector3(),
						new Vector3(2, 3, 131), 0, 0, OrbitDirection.CLOCKWISE);
				camAnim.setDuration(10000);
				camAnim.setRepeatMode(RepeatMode.INFINITE);
				camAnim.setTransformable3D(getCurrentCamera());
				registerAnimation(camAnim);
				camAnim.play();
				lastCameraNo = 1;
				break;
			}
		}
	}

	private void changeLayout() {
		float layoutX;
		float layoutY;
		float layoutZ;
		int tempX;
		int tempY;
		int tempZ;
		float radius = 0.15f;
		float a = 0.81f;
		int bulbID;

		// handle layout changes
		if (animationAllowed) {
			animationAllowed = false;
			switch (arrangmentNo) {
			case 0:// Strands tree
					// if (animating == false) {
					// animating = true;
				for (bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {
					layoutY = ((float) bulbID) / NUM_PIXELS * 256f;
					layoutZ = (float) (layoutY * radius * Math.sin(a * layoutY));
					layoutX = (float) (layoutY * radius * Math.cos(a * layoutY));

					// move with no animation
					// mHaloString[bulbID].setPosition(layoutX,
					// (-layoutY / 3.0f) + 48.0f, layoutZ);

					// animated
					animTranslate[bulbID] = new TranslateAnimation3D(
							new Vector3(layoutX, (-layoutY / 3.0f) + 48.0f,
									layoutZ));
					animTranslate[bulbID].setDuration(ANIM_SPEED);
					animTranslate[bulbID].setRepeatMode(RepeatMode.NONE);
					animTranslate[bulbID]
							.setTransformable3D(mPixelString[bulbID]);
					registerAnimation(animTranslate[bulbID]);

					animTranslate[bulbID].play();
					// }
				}
				break;
			case 1:// Spiral tree
					// if (animating == false) {
					// animating = true;
				for (bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {
					layoutY = ((float) bulbID) / NUM_PIXELS * 256f;
					layoutZ = (float) (layoutY * radius * Math.sin(radius
							* layoutY));
					layoutX = (float) (layoutY * radius * Math.cos(radius
							* layoutY));

					// move with no animation
					// mHaloString[bulbID].setPosition(layoutX,
					// (-layoutY / 3.0f) + 48.0f, layoutZ);

					// animated
					animTranslate[bulbID] = new TranslateAnimation3D(
							new Vector3(layoutX, (-layoutY / 3.0f) + 48.0f,
									layoutZ));
					animTranslate[bulbID].setDuration(ANIM_SPEED);
					animTranslate[bulbID].setRepeatMode(RepeatMode.NONE);
					animTranslate[bulbID]
							.setTransformable3D(mPixelString[bulbID]);
					registerAnimation(animTranslate[bulbID]);
					animTranslate[bulbID].play();
				}
				// }
				break;
			case 2: // 'Dalek' tree (lines at fixed angles)
				for (bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {
					// the "15 +" is so 15 bulbs aren't wasted on the top
					// point
					int circle = (15 + bulbID) / 16; // height of circle
					int line = (15 + bulbID) % 16; // break into 16 cone
													// lines
					layoutY = (float) (circle * 16) / NUM_PIXELS * 256f; // basically
																			// the
																			// bulb
																			// ID
					// but in steps of 16
					float lineAngle = line * two_pi / 16f; // get angle of
															// each
					layoutZ = (float) (layoutY * radius * Math.sin(lineAngle));
					layoutX = (float) (layoutY * radius * Math.cos(lineAngle));

					// animated
					animTranslate[bulbID] = new TranslateAnimation3D(
							new Vector3(layoutX, (-layoutY / 3.0f) + 48.0f,
									layoutZ));
					animTranslate[bulbID].setDuration(ANIM_SPEED);
					animTranslate[bulbID].setRepeatMode(RepeatMode.NONE);
					animTranslate[bulbID]
							.setTransformable3D(mPixelString[bulbID]);
					registerAnimation(animTranslate[bulbID]);
					animTranslate[bulbID].play();
				}
				break;
			case 3: // random surface - but ordered for height still
				for (bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {
					float randAngle = (two_pi / rand.nextInt(1536)) * 1536f;

					layoutY = ((float) bulbID) / NUM_PIXELS * 256f;
					layoutZ = (float) (layoutY * radius * Math.sin(randAngle));
					layoutX = (float) (layoutY * radius * Math.cos(randAngle));

					// animated
					animTranslate[bulbID] = new TranslateAnimation3D(
							new Vector3(layoutX, (-layoutY / 3.0f) + 48.0f,
									layoutZ));
					animTranslate[bulbID].setDuration(ANIM_SPEED);
					animTranslate[bulbID].setRepeatMode(RepeatMode.NONE);
					animTranslate[bulbID]
							.setTransformable3D(mPixelString[bulbID]);
					registerAnimation(animTranslate[bulbID]);
					animTranslate[bulbID].play();
				}
				break;
			case 4: // Grid - randomized 'Z'
				bulbID = 0;
				for (tempY = 0; tempY < PIXELS_SQROOT; tempY++) {
					// forward
					for (tempX = 0; tempX < PIXELS_SQROOT; tempX++) {
						tempZ = rand.nextInt(PIXELS_SQROOT);

						layoutX = (8f - (float) tempX) * 4f;
						layoutY = (8f - (float) tempY) * 6f;
						layoutZ = (8f - (float) tempZ) * 4f;

						// animated
						animTranslate[bulbID] = new TranslateAnimation3D(
								new Vector3(layoutX, layoutY, layoutZ));
						animTranslate[bulbID].setDuration(ANIM_SPEED);
						animTranslate[bulbID].setRepeatMode(RepeatMode.NONE);
						animTranslate[bulbID]
								.setTransformable3D(mPixelString[bulbID]);
						registerAnimation(animTranslate[bulbID]);
						animTranslate[bulbID].play();

						bulbID++;
					}
				}
				break;
			case 5: // Grid - Flat (same used for Dev mode)
				bulbID = 0;

				for (bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {

					// animated
					animTranslate[bulbID] = new TranslateAnimation3D(
							new Vector3(
									((float) ((bulbID / 16) - 8) * 3.5f) + 2f,
									(float) ((bulbID % 16) - 8) * 6.5f, 0.0f));
					animTranslate[bulbID].setDuration(ANIM_SPEED);
					animTranslate[bulbID].setRepeatMode(RepeatMode.NONE);
					animTranslate[bulbID]
							.setTransformable3D(mPixelString[bulbID]);
					registerAnimation(animTranslate[bulbID]);
					animTranslate[bulbID].play();
				}
				break;
			case 99: // random surface - density correction - but no height
						// order - UNUSED
				for (bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {
					float randAngle = (two_pi / rand.nextInt(1536)) * 1536f;
					float randVal = rand.nextInt(1536) / 1536f; // random
																// between 0
																// and
																// 1

					layoutY = (float) (256f * Math.sqrt(randVal) / NUM_PIXELS * 256f); // less
																						// at
																						// top,
					// more at
					// bottom
					layoutZ = (float) (layoutY * radius * Math.sin(randAngle));
					layoutX = (float) (layoutY * radius * Math.cos(randAngle));

					// animated
					animTranslate[bulbID] = new TranslateAnimation3D(
							new Vector3(layoutX, (-layoutY / 3.0f) + 48.0f,
									layoutZ));
					animTranslate[bulbID].setDuration(ANIM_SPEED);
					animTranslate[bulbID].setRepeatMode(RepeatMode.NONE);
					animTranslate[bulbID]
							.setTransformable3D(mPixelString[bulbID]);
					registerAnimation(animTranslate[bulbID]);
					animTranslate[bulbID].play();
				}
				break;
			}
		}
		lCounter = -meoprefLayoutTime;
	}

	private void changeBulbSize() {
		// if (prefSizeChanged) {
		// prefSizeChanged = false;

		int bulbID;
		for (bulbID = 0; bulbID < NUM_PIXELS; bulbID++) {
			if (meoprefDev == 0) {
				mPixelString[bulbID].setPointSize((float) meoprefSize);
			} else {
				mPixelString[bulbID].setPointSize(50.0f);
			}
		}
	}

	@SuppressLint("NewApi")
	private void setPrefsToLocal() {
		String lastmeoprefMode = meoprefMode;
		Set<String> lastmeoprefPatterns = meoprefPatterns;
		Set<String> lastmeoprefCrossfades = meoprefCrossfades;
		Set<String> lastmeoprefPixelLayout = meoprefPixelLayout;
		int lastmeoprefStarburst = meoprefStarburst;
		int lastmeoprefFadeTime = meoprefFadeTime;
		int lastmeoprefDisplayTime = meoprefDisplayTime;
		int lastmeoprefLayoutTime = meoprefLayoutTime;
		int lastmeoprefDev = meoprefDev;
		int lastmeoprefRotate = meoprefRotate;
		int lastmeoprefSize = meoprefSize;

		if (preferences.getBoolean(Settings.KEY_DEVELOPER, false) == false) {
			meoprefDev = 0;
		} else {
			meoprefDev = 1;
		}
		if (meoprefDev != lastmeoprefDev) {
			Log.i(TAG_SETTINGS, "Developer: " + meoprefDev);
			prefDevChanged = true;
		}
		
		if (preferences.getBoolean(Settings.KEY_ROTATE, false) == false) {
			meoprefRotate = 0;
		} else {
			meoprefRotate = 1;
		}
		if (meoprefRotate != lastmeoprefRotate) {
			Log.i(TAG_SETTINGS, "Rotate: " + meoprefRotate);
			prefRotateChanged = true;
		}

		meoprefMode = preferences.getString(Settings.KEY_MODE, "error");
		if (meoprefMode != lastmeoprefMode) {
			Log.i(TAG_SETTINGS, "Mode: " + meoprefMode);
		}

		meoprefStarburst = Integer.parseInt(preferences.getString(
				Settings.KEY_STARBURST, "0"));
		if (meoprefStarburst != lastmeoprefStarburst) {
			Log.i(TAG_SETTINGS, "Starburst: " + meoprefStarburst);
		}

		meoprefDisplayTime = Integer.parseInt(preferences.getString(
				Settings.KEY_DISPLAYTIME, "600"));
		if (meoprefDisplayTime != lastmeoprefDisplayTime) {
			Log.i(TAG_SETTINGS, "DisplayTime: " + meoprefDisplayTime);
		}

		meoprefFadeTime = Integer.parseInt(preferences.getString(
				Settings.KEY_FADETIME, "60"));
		if (meoprefFadeTime != lastmeoprefFadeTime) {
			Log.i(TAG_SETTINGS, "FadeTime: " + meoprefFadeTime);
		}

		meoprefPatterns = preferences.getStringSet(Settings.KEY_PATTERNS, null);
		if (meoprefPatterns != lastmeoprefPatterns) {
			Iterator<String> it = meoprefPatterns.iterator();
			while (it.hasNext()) {
				String pvalue = (String) it.next();
				Log.i(TAG_SETTINGS, "Patterns: " + pvalue);
			}
		}

		meoprefCrossfades = preferences.getStringSet(Settings.KEY_CROSSFADES,
				null);
		if (meoprefCrossfades != lastmeoprefCrossfades) {
			Iterator<String> it = meoprefCrossfades.iterator();
			while (it.hasNext()) {
				String pvalue = (String) it.next();
				Log.i(TAG_SETTINGS, "Crossfades: " + pvalue);
			}
			prefCrossfadesChanged = true;
		}

		meoprefPixelLayout = preferences.getStringSet(
				Settings.KEY_PIXEL_LAYOUT, null);
		if (meoprefPixelLayout != lastmeoprefPixelLayout) {
			Iterator<String> it = meoprefPixelLayout.iterator();
			while (it.hasNext()) {
				String pvalue = (String) it.next();
				Log.i(TAG_SETTINGS, "Layouts: " + pvalue);
			}
			prefPixelLayoutChanged = true;
		}

		meoprefLayoutTime = Integer.parseInt(preferences.getString(
				Settings.KEY_LAYOUTTIME, "60"));
		if (meoprefLayoutTime != lastmeoprefLayoutTime) {
			Log.i(TAG_SETTINGS, "LayoutTime: " + meoprefLayoutTime);
		}

		meoprefSize = Integer.parseInt(preferences.getString(
				Settings.KEY_BULB_SIZE, "40"));
		if (meoprefSize != lastmeoprefSize) {
			Log.i(TAG_SETTINGS, "Bulb Size: " + meoprefSize);
			prefSizeChanged = true;
		}
	}
}