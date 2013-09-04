package meo.wallpaper.xmaslights.materials;

import java.nio.FloatBuffer;

import meo.wallpaper.xmaslights.R;

import rajawali.materials.AParticleMaterial;
//import rajawali.math.Vector3;
import android.annotation.SuppressLint;
import android.opengl.GLES20;

@SuppressLint("NewApi")
public class XmasParticleMaterial extends AParticleMaterial {

	protected float mPointSize = 10.0f;

	protected int muPointSizeHandle;
	protected int muCamPosHandle;
	protected int muDistanceAttHandle;
	protected int muCurrentFrameHandle;
	protected int muTileSizeHandle;
	protected int muNumTileRowsHandle;
	protected int maVelocityHandle;
	
	protected int maAnimOffsetHandle;
	protected int muFrictionHandle;
	protected int muTimeHandle;
	protected int muMultiParticlesEnabledHandle;

	protected float[] mDistanceAtt;
	protected boolean mMultiParticlesEnabled;
	protected float[] mFriction;
	protected float[] mCamPos;
	protected float mTime;
	protected int mCurrentFrame;
	protected float mTileSize;
	protected float mNumTileRows;
	protected boolean mIsAnimated;
	
	//MEO - additional overlay color multipliers and flag
	protected int mredMultiplierHandle;
	protected int mgrnMultiplierHandle;
	protected int mbluMultiplierHandle;
	protected int mDevModeHandle;

	public XmasParticleMaterial() {
		this(false);
	}

	public XmasParticleMaterial(boolean isAnimated) {
		this(RawMaterialLoader.fetch(R.raw.xmas_light_vertex), RawMaterialLoader
				.fetch(R.raw.xmas_light_fragment), isAnimated);
	}

	public XmasParticleMaterial(String vertexShader, String fragmentShader, boolean isAnimated) {
		super(vertexShader, fragmentShader);
		mDistanceAtt = new float[] { 1, 1, 1 };
		mFriction = new float[3];
		mCamPos = new float[3];
		mIsAnimated = isAnimated;
		if (mIsAnimated) {
			mUntouchedVertexShader = "\n#define ANIMATED\n" + mUntouchedVertexShader;
			mUntouchedFragmentShader = "\n#define ANIMATED\n" + mUntouchedFragmentShader;
		}
		setShaders(mUntouchedVertexShader, mUntouchedFragmentShader);
	}

	public void setPointSize(float pointSize) {
		mPointSize = pointSize;
		GLES20.glUniform1f(muPointSizeHandle, mPointSize);
	}

	//MEO
	public void setOverlayColor(float redHaloMult, float grnHaloMult, float bluHaloMult, int devMode) {
		GLES20.glUniform1f(mredMultiplierHandle, redHaloMult);
		GLES20.glUniform1f(mgrnMultiplierHandle, grnHaloMult);
		GLES20.glUniform1f(mbluMultiplierHandle, bluHaloMult);
		GLES20.glUniform1i(mDevModeHandle, devMode);
	}
	
	public void setMultiParticlesEnabled(boolean enabled) {
		mMultiParticlesEnabled = enabled;
		GLES20.glUniform1i(muMultiParticlesEnabledHandle, mMultiParticlesEnabled == true ? GLES20.GL_TRUE
				: GLES20.GL_FALSE);
	}

	@Override
	public void useProgram() {
		super.useProgram();
		GLES20.glUniform3fv(muCamPosHandle, 1, mCamPos, 0);
		GLES20.glUniform3fv(muDistanceAttHandle, 1, mDistanceAtt, 0);
		GLES20.glUniform3fv(muFrictionHandle, 1, mFriction, 0);
		GLES20.glUniform1f(muTimeHandle, mTime);
		GLES20.glUniform1f(muCurrentFrameHandle, mCurrentFrame);
		GLES20.glUniform1f(muTileSizeHandle, mTileSize);
		GLES20.glUniform1f(muNumTileRowsHandle, mNumTileRows);
	}
	
	public void setVelocity(final int velocityBufferHandle) {
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, velocityBufferHandle);
		GLES20.glEnableVertexAttribArray(maVelocityHandle);
		GLES20.glVertexAttribPointer(maVelocityHandle, 3, GLES20.GL_FLOAT, false,
				0, 0);
	}

	/*
	public void setFriction(Vector3 friction) {
		mFriction[0] = friction.x;
		mFriction[1] = friction.y;
		mFriction[2] = friction.z;
	}*/

	public void setTime(float time) {
		mTime = time;
	}

	@Override
	public void setShaders(String vertexShader, String fragmentShader)
	{
		super.setShaders(vertexShader, fragmentShader);
		muPointSizeHandle = getUniformLocation("uPointSize");
		muDistanceAttHandle = getUniformLocation("uDistanceAtt");

		maVelocityHandle = getAttribLocation("aVelocity");
		
		maAnimOffsetHandle = getAttribLocation("aAnimOffset");
		muFrictionHandle = getUniformLocation("uFriction");
		muTimeHandle = getUniformLocation("uTime");
		muMultiParticlesEnabledHandle = getUniformLocation("uMultiParticlesEnabled");

		muCurrentFrameHandle = getUniformLocation("uCurrentFrame");
		muTileSizeHandle = getUniformLocation("uTileSize");
		muNumTileRowsHandle = getUniformLocation("uNumTileRows");
		
		//MEO - overlay color
		// assigns a handle 'mredMultiplierHandle' to the fragment shader variable "redMultiplier"
		mredMultiplierHandle = GLES20.glGetUniformLocation(mProgram, "redMultiplier");
		mgrnMultiplierHandle = GLES20.glGetUniformLocation(mProgram, "grnMultiplier");
		mbluMultiplierHandle = GLES20.glGetUniformLocation(mProgram, "bluMultiplier");
		mDevModeHandle = GLES20.glGetUniformLocation(mProgram, "devMode");
	}

	public void setAnimOffsets(FloatBuffer animOffsets) {
		GLES20.glEnableVertexAttribArray(maAnimOffsetHandle);
		GLES20.glVertexAttribPointer(maAnimOffsetHandle, 1, GLES20.GL_FLOAT, false, 0, animOffsets);
	}

	public void setCurrentFrame(int currentFrame) {
		mCurrentFrame = currentFrame;
	}

	public void setTileSize(float tileSize) {
		mTileSize = tileSize;
	}

	public void setNumTileRows(int numTileRows) {
		mNumTileRows = numTileRows;
	}

	/*
	public void setCameraPosition(Vector3 cameraPos) {
		mCamPos[0] = cameraPos.x;
		mCamPos[1] = cameraPos.y;
		mCamPos[2] = cameraPos.z;
	}*/
}
