package meo.wallpaper.xmaslights;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import meo.wallpaper.xmaslights.materials.XmasParticleMaterial;

import rajawali.Camera;
import rajawali.Geometry3D;
import rajawali.math.Vector3;
import rajawali.primitives.Particle;
import rajawali.util.ObjectColorPicker.ColorPickerInfo;
import android.opengl.GLES20;

public class MultiParticleSystem extends Particle {
	protected Vector3 mFriction;
	protected FloatBuffer mVelocityBuffer;
	protected FloatBuffer mAnimOffsetBuffer;
	protected int mVelocityBufferHandle;
	protected float mTime;

	//MEO
	protected float mredMultiplier;
	protected float mgrnMultiplier;
	protected float mbluMultiplier;
	protected int mDevMode;

	public MultiParticleSystem() {
		super();
	}
	
	protected void init() {
		setDrawingMode(GLES20.GL_POINTS);
		//setTransparent(true);

		final int numParticles = 1;

		float[] vertices = new float[numParticles * 3];
		float[] velocity = new float[numParticles * 3];
		float[] textureCoords = new float[numParticles * 2];
		float[] normals = new float[numParticles * 3];
		float[] colors = new float[numParticles * 4];
		int[] indices = new int[numParticles];
		float[] animOffsets = new float[numParticles];

		
		//int index = 0;
		for (int i = 0; i < numParticles; ++i) {
			//index = i * 3;
			//vertices[index] = 0;
			//vertices[index + 1] = 0;
			//vertices[index + 2] = 0;

			//velocity[index] = (-.5f + ((float) Math.random() * 1f)) / 10.0f;
			//velocity[index + 1] = (-.5f + ((float) Math.random() * 1f)) / 10.0f;
			//velocity[index + 2] = (((float) Math.random() * -1f)) / 10.0f;

			//normals[index] = 0;
			//normals[index + 1] = 0;
			//normals[index + 2] = 1;

			//index = i * 2;
			//textureCoords[i] = 0;
			//textureCoords[i + 1] = 0;

			//index = i * 4;
			//colors[i] = (float) Math.random() * 1f;
			//colors[i + 1] = (float) Math.random() * 1f;
			//colors[i + 2] = (float) Math.random() * 1f;
			//colors[i + 3] = i;

			//indices[i] = i;
			
			
			animOffsets[i] = 3;
		}
		
		mVelocityBuffer = ByteBuffer
				.allocateDirect(velocity.length * Geometry3D.FLOAT_SIZE_BYTES)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mVelocityBuffer.put(velocity);
		mVelocityBuffer.position(0);
		
		mAnimOffsetBuffer = ByteBuffer
				.allocateDirect(animOffsets.length * Geometry3D.FLOAT_SIZE_BYTES)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mAnimOffsetBuffer.put(animOffsets);
		mAnimOffsetBuffer.position(0);

		mFriction = new Vector3(.95f, .95f, .95f);

		int buff[] = new int[1];
		GLES20.glGenBuffers(1, buff, 0);
		mVelocityBufferHandle = buff[0];

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVelocityBufferHandle);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVelocityBuffer.limit()
				* Geometry3D.FLOAT_SIZE_BYTES, mVelocityBuffer,
				GLES20.GL_DYNAMIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		setData(vertices, normals, textureCoords, colors, indices);
	}

	public void reload() {
		super.reload();

		int buff[] = new int[1];
		GLES20.glGenBuffers(1, buff, 0);
		mVelocityBufferHandle = buff[0];

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVelocityBufferHandle);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVelocityBuffer.limit()
				* Geometry3D.FLOAT_SIZE_BYTES, mVelocityBuffer,
				GLES20.GL_DYNAMIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}

	public void setTime(float time) {
		mTime = time;
	}

	public float getTime() {
		return mTime;
	}

	// MEO
	public void setOverlayColor(float redMultiplier, float grnMultiplier, float bluMultiplier, int devMode) {
		mredMultiplier = redMultiplier;
		mgrnMultiplier = grnMultiplier;
		mbluMultiplier = bluMultiplier;
		mDevMode = devMode;
	}
	
	//MEO - note still set up for multiple particle system, but only one used
	public void setAnimFrame(int animOffset) {
		final int numParticles = 1;
		float[] animOffsets = new float[numParticles];
		for (int i = 0; i < numParticles; ++i) {
			animOffsets[i] = animOffset;
		}
		mAnimOffsetBuffer = ByteBuffer
				.allocateDirect(animOffsets.length * Geometry3D.FLOAT_SIZE_BYTES)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mAnimOffsetBuffer.put(animOffsets);
		mAnimOffsetBuffer.position(0);
	}

	protected void setShaderParams(Camera camera) {
		super.setShaderParams(camera);
		XmasParticleMaterial particleShader = (XmasParticleMaterial) mParticleShader;
		//particleShader.setFriction(mFriction);
		//particleShader.setVelocity(mVelocityBufferHandle);
		//particleShader.setMultiParticlesEnabled(true);
		particleShader.setAnimOffsets(mAnimOffsetBuffer);
		particleShader.setTileSize(1 / 8f);
		particleShader.setNumTileRows(8);
		//particleShader.setTime(mTime);
		//particleShader.setCameraPosition(camera.getPosition());
		//MEO
		particleShader.setOverlayColor(mredMultiplier, mgrnMultiplier, mbluMultiplier, mDevMode);
	}

	public void render(Camera camera, float[] projMatrix, float[] vMatrix,
			final float[] parentMatrix, ColorPickerInfo pickerInfo) {
		super.render(camera, projMatrix, vMatrix, parentMatrix, pickerInfo);
	}

}
