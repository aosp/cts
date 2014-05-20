/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.hardware.camera2.cts.testcases;

import static android.hardware.camera2.cts.CameraTestUtils.*;
import static com.android.ex.camera2.blocking.BlockingStateListener.STATE_CLOSED;

import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata.Key;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Size;
import android.hardware.camera2.CameraDevice.CaptureListener;
import android.hardware.camera2.cts.Camera2SurfaceViewStubActivity;
import android.hardware.camera2.cts.CameraTestUtils;
import android.hardware.camera2.cts.CameraTestUtils.SimpleCaptureListener;
import android.hardware.camera2.cts.helpers.CameraErrorCollector;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.hardware.camera2.cts.helpers.StaticMetadata.CheckLevel;

import com.android.ex.camera2.blocking.BlockingStateListener;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Camera2 Preview test case base class by using SurfaceView as rendering target.
 *
 * <p>This class encapsulates the SurfaceView based preview common functionalities.
 * The setup and teardown of CameraManager, test HandlerThread, Activity, Camera IDs
 * and CameraStateListener are handled in this class. Some basic preview related utility
 * functions are provided to facilitate the derived preview-based test classes.
 * </p>
 */

public class Camera2SurfaceViewTestCase extends
        ActivityInstrumentationTestCase2<Camera2SurfaceViewStubActivity> {
    private static final String TAG = "SurfaceViewTestCase";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS = 1000;
    private static final int MAX_READER_IMAGES = 5;

    private Size mPreviewSize;

    // TODO: Use internal storage for this to make sure the file is only visible to test.
    protected static final String DEBUG_FILE_NAME_BASE =
            Environment.getExternalStorageDirectory().getPath();
    protected static final int WAIT_FOR_RESULT_TIMEOUT_MS = 3000;
    protected static final float FRAME_DURATION_ERROR_MARGIN = 0.005f; // 0.5 percent error margin.
    protected static final int NUM_RESULTS_WAIT_TIMEOUT = 100;

    protected Context mContext;
    protected CameraManager mCameraManager;
    protected String[] mCameraIds;
    protected HandlerThread mHandlerThread;
    protected Handler mHandler;
    protected BlockingStateListener mCameraListener;
    protected CameraErrorCollector mCollector;
    // Per device fields:
    protected StaticMetadata mStaticInfo;
    protected CameraDevice mCamera;
    protected ImageReader mReader;
    protected Surface mReaderSurface;
    protected Surface mPreviewSurface;
    protected List<Size> mOrderedPreviewSizes; // In descending order.
    protected List<Size> mOrderedVideoSizes; // In descending order.
    protected List<Size> mOrderedStillSizes; // In descending order.
    protected HashMap<Size, Long> mMinPreviewFrameDurationMap;

    public Camera2SurfaceViewTestCase() {
        super(Camera2SurfaceViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        /**
         * Set up the camera preview required environments, including activity,
         * CameraManager, HandlerThread, Camera IDs, and CameraStateListener.
         */
        super.setUp();
        mContext = getActivity();
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Unable to get CameraManager", mCameraManager);
        mCameraIds = mCameraManager.getCameraIdList();
        assertNotNull("Unable to get camera ids", mCameraIds);
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCameraListener = new BlockingStateListener();
        mCollector = new CameraErrorCollector();
    }

    @Override
    protected void tearDown() throws Exception {
        // Teardown the camera preview required environments.
        mHandlerThread.quitSafely();
        mHandler = null;
        mCameraListener = null;

        try {
            mCollector.verify();
        } catch (Throwable e) {
            // When new Exception(e) is used, exception info will be printed twice.
            throw new Exception(e.getMessage());
        } finally {
            super.tearDown();
        }
    }

    /**
     * Start camera preview by using the given request, preview size and capture
     * listener.
     * <p>
     * If preview is already started, calling this function will stop the
     * current preview stream and start a new preview stream with given
     * parameters. No need to call stopPreview between two startPreview calls.
     * </p>
     *
     * @param request The request builder used to start the preview.
     * @param previewSz The size of the camera device output preview stream.
     * @param listener The callbacks the camera device will notify when preview
     *            capture is available.
     */
    protected void startPreview(CaptureRequest.Builder request, Size previewSz,
            CaptureListener listener) throws Exception {
        // Update preview size.
        updatePreviewSurface(previewSz);
        if (VERBOSE) {
            Log.v(TAG, "start preview with size " + mPreviewSize.toString());
        }

        configurePreviewOutput(request);

        mCamera.setRepeatingRequest(request.build(), listener, mHandler);
    }

    /**
     * Configure the preview output stream.
     *
     * @param request The request to be configured with preview surface
     */
    protected void configurePreviewOutput(CaptureRequest.Builder request)
            throws CameraAccessException {
        List<Surface> outputSurfaces = new ArrayList<Surface>(/*capacity*/1);
        outputSurfaces.add(mPreviewSurface);
        configureCameraOutputs(mCamera, outputSurfaces, mCameraListener);

        request.addTarget(mPreviewSurface);
    }

    /**
     * Create a {@link CaptureRequest#Builder} and add the default preview surface.
     *
     * @return The {@link CaptureRequest#Builder} to be created
     * @throws CameraAccessException When create capture request from camera fails
     */
    protected CaptureRequest.Builder createRequestForPreview() throws CameraAccessException {
        if (mPreviewSurface == null) {
            throw new IllegalStateException(
                    "Preview surface is not set yet, call updatePreviewSurface or startPreview"
                    + "first to set the preview surface properly.");
        }
        CaptureRequest.Builder requestBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        requestBuilder.addTarget(mPreviewSurface);
        return requestBuilder;
    }

    /**
     * Stop preview for current camera device.
     */
    protected void stopPreview() throws Exception {
        if (VERBOSE) Log.v(TAG, "Stopping preview and waiting for idle");
        // Stop repeat, wait for captures to complete, and disconnect from surfaces
        configureCameraOutputs(mCamera, /*outputSurfaces*/null, mCameraListener);
    }

    /**
     * Setup still (JPEG) capture configuration and start preview.
     * <p>
     * The default max number of image is set to image reader.
     * </p>
     *
     * @param previewRequest The capture request to be used for preview
     * @param stillRequest The capture request to be used for still capture
     * @param previewSz Preview size
     * @param stillSz The still capture size
     * @param resultListener Capture result listener
     * @param imageListener The still capture image listener
     */
    protected void prepareStillCaptureAndStartPreview(CaptureRequest.Builder previewRequest,
            CaptureRequest.Builder stillRequest, Size previewSz, Size stillSz,
            CaptureListener resultListener,
            ImageReader.OnImageAvailableListener imageListener) throws Exception {
        prepareCaptureAndStartPreview(previewRequest, stillRequest, previewSz, stillSz,
                ImageFormat.JPEG, resultListener, MAX_READER_IMAGES, imageListener);
    }

    /**
     * Setup still (JPEG) capture configuration and start preview.
     *
     * @param previewRequest The capture request to be used for preview
     * @param stillRequest The capture request to be used for still capture
     * @param previewSz Preview size
     * @param stillSz The still capture size
     * @param resultListener Capture result listener
     * @param maxNumImages The max number of images set to the image reader
     * @param imageListener The still capture image listener
     */
    protected void prepareStillCaptureAndStartPreview(CaptureRequest.Builder previewRequest,
            CaptureRequest.Builder stillRequest, Size previewSz, Size stillSz,
            CaptureListener resultListener, int maxNumImages,
            ImageReader.OnImageAvailableListener imageListener) throws Exception {
        prepareCaptureAndStartPreview(previewRequest, stillRequest, previewSz, stillSz,
                ImageFormat.JPEG, resultListener, maxNumImages, imageListener);
    }

    /**
     * Setup raw capture configuration and start preview.
     *
     * <p>
     * The default max number of image is set to image reader.
     * </p>
     *
     * @param previewRequest The capture request to be used for preview
     * @param rawRequest The capture request to be used for raw capture
     * @param previewSz Preview size
     * @param rawSz The raw capture size
     * @param resultListener Capture result listener
     * @param imageListener The raw capture image listener
     */
    protected void prepareRawCaptureAndStartPreview(CaptureRequest.Builder previewRequest,
            CaptureRequest.Builder rawRequest, Size previewSz, Size rawSz,
            CaptureListener resultListener,
            ImageReader.OnImageAvailableListener imageListener) throws Exception {
        prepareCaptureAndStartPreview(previewRequest, rawRequest, previewSz, rawSz,
                ImageFormat.RAW_SENSOR, resultListener, MAX_READER_IMAGES, imageListener);
    }

    /**
     * Wait for expected result key value available in a certain number of results.
     *
     * <p>
     * Check the result immediately if numFramesWait is 0.
     * </p>
     *
     * @param listener The capture listener to get capture result
     * @param resultKey The capture result key associated with the result value
     * @param expectedValue The result value need to be waited for
     * @param numResultsWait Number of frame to wait before times out
     * @throws TimeoutRuntimeException If more than numResultsWait results are
     * seen before the result matching myRequest arrives, or each individual wait
     * for result times out after {@value #WAIT_FOR_RESULT_TIMEOUT_MS}ms.
     */
    protected static <T> void waitForResultValue(SimpleCaptureListener listener, Key<T> resultKey,
            T expectedValue, int numResultsWait) {
        List<T> expectedValues = new ArrayList<T>();
        expectedValues.add(expectedValue);
        waitForAnyResultValue(listener, resultKey, expectedValues, numResultsWait);
    }

    /**
     * Wait for any expected result key values available in a certain number of results.
     *
     * <p>
     * Check the result immediately if numFramesWait is 0.
     * </p>
     *
     * @param listener The capture listener to get capture result.
     * @param resultKey The capture result key associated with the result value.
     * @param expectedValues The list of result value need to be waited for,
     * return immediately if the list is empty.
     * @param numResultsWait Number of frame to wait before times out.
     * @throws TimeoutRuntimeException If more than numResultsWait results are.
     * seen before the result matching myRequest arrives, or each individual wait
     * for result times out after {@value #WAIT_FOR_RESULT_TIMEOUT_MS}ms.
     */
    protected static <T> void waitForAnyResultValue(SimpleCaptureListener listener, Key<T> resultKey,
            List<T> expectedValues, int numResultsWait) {
        if (numResultsWait < 0 || listener == null || expectedValues == null) {
            throw new IllegalArgumentException(
                    "Input must be non-negative number and listener/expectedValues "
                    + "must be non-null");
        }

        int i = 0;
        CaptureResult result;
        do {
            result = listener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
            T value = result.get(resultKey);
            for ( T expectedValue : expectedValues) {
                if (VERBOSE) {
                    Log.v(TAG, "Current result value for key " + resultKey.getName() + " is: "
                            + value.toString());
                }
                if (value.equals(expectedValue)) {
                    return;
                }
            }
        } while (i++ < numResultsWait);

        throw new TimeoutRuntimeException(
                "Unable to get the expected result value " + expectedValues + " for key " +
                        resultKey.getName() + " after waiting for " + numResultsWait + " results");
    }

    /**
     * Wait for numResultWait frames
     *
     * @param resultListener The capture listener to get capture result back.
     * @param numResultsWait Number of frame to wait
     */
    protected static void waitForNumResults(SimpleCaptureListener resultListener,
            int numResultsWait) {
        if (numResultsWait <= 0 || resultListener == null) {
            throw new IllegalArgumentException(
                    "Input must be positive number and listener must be non-null");
        }

        CaptureResult result;
        for (int i = 0; i < numResultsWait; i++) {
            result = resultListener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
        }
        return;
    }

    /**
     * Wait for AE to be stabilized before capture: CONVERGED or FLASH_REQUIRED.
     *
     * @param resultListener The capture listener to get capture result back.
     */
    protected static void waitForAeStable(SimpleCaptureListener resultListener) {
        List<Integer> expectedAeStates = new ArrayList<Integer>();
        expectedAeStates.add(new Integer(CaptureResult.CONTROL_AE_STATE_CONVERGED));
        expectedAeStates.add(new Integer(CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED));
        waitForAnyResultValue(resultListener, CaptureResult.CONTROL_AE_STATE, expectedAeStates,
                NUM_RESULTS_WAIT_TIMEOUT);
    }

    /**
     * Wait for AE to be: LOCKED
     *
     * @param resultListener The capture listener to get capture result back.
     */
    protected static void waitForAeLocked(SimpleCaptureListener resultListener) {
        List<Integer> expectedAeStates = new ArrayList<Integer>();
        expectedAeStates.add(new Integer(CaptureResult.CONTROL_AE_STATE_LOCKED));
        waitForAnyResultValue(resultListener, CaptureResult.CONTROL_AE_STATE, expectedAeStates,
                NUM_RESULTS_WAIT_TIMEOUT);
    }

    /**
     * Create an {@link ImageReader} object and get the surface.
     *
     * @param size The size of this ImageReader to be created.
     * @param format The format of this ImageReader to be created
     * @param maxNumImages The max number of images that can be acquired simultaneously.
     * @param listener The listener used by this ImageReader to notify callbacks.
     */
    protected void createImageReader(Size size, int format, int maxNumImages,
            ImageReader.OnImageAvailableListener listener) throws Exception {
        closeImageReader();

        mReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), format, maxNumImages);
        mReaderSurface = mReader.getSurface();
        mReader.setOnImageAvailableListener(listener, mHandler);
        if (VERBOSE) Log.v(TAG, "Created ImageReader size " + size.toString());
    }

    /**
     * Close the pending images then close current active {@link ImageReader} object.
     */
    protected void closeImageReader() {
        if (mReader != null) {
            try {
                // Close all possible pending images first.
                Image image = mReader.acquireLatestImage();
                if (image != null) {
                    image.close();
                }
            } finally {
                mReader.close();
                mReader = null;
            }
        }
    }

    /**
     * Open a camera device and get the StaticMetadata for a given camera id.
     *
     * @param cameraId The id of the camera device to be opened.
     */
    protected void openDevice(String cameraId) throws Exception {
        mCamera = CameraTestUtils.openCamera(
                mCameraManager, cameraId, mCameraListener, mHandler);
        mCollector.setCameraId(cameraId);
        mStaticInfo = new StaticMetadata(mCameraManager.getCameraCharacteristics(cameraId),
                CheckLevel.ASSERT, /*collector*/null);
        mOrderedPreviewSizes = getSupportedPreviewSizes(cameraId, mCameraManager, PREVIEW_SIZE_BOUND);
        mOrderedVideoSizes = getSupportedVideoSizes(cameraId, mCameraManager, PREVIEW_SIZE_BOUND);
        mOrderedStillSizes = getSupportedStillSizes(cameraId, mCameraManager, null);
        // Use ImageFormat.YUV_420_888 for now. TODO: need figure out what's format for preview
        // in public API side.
        mMinPreviewFrameDurationMap =
                mStaticInfo.getAvailableMinFrameDurationsForFormatChecked(ImageFormat.YUV_420_888);
    }

    /**
     * Close the current actively used camera device.
     */
    protected void closeDevice() {
        if (mCamera != null) {
            mCamera.close();
            mCameraListener.waitForState(STATE_CLOSED, CAMERA_CLOSE_TIMEOUT_MS);
            mCamera = null;
            mStaticInfo = null;
            mOrderedPreviewSizes = null;
            mOrderedVideoSizes = null;
            mOrderedStillSizes = null;
        }
    }

    protected void updatePreviewSurface(Size size) {
        if (size.equals(mPreviewSize)) {
            return;
        }

        mPreviewSize = size;
        Camera2SurfaceViewStubActivity stubActivity = getActivity();
        final SurfaceHolder holder = stubActivity.getSurfaceView().getHolder();
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                holder.setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }
        });

        boolean res = stubActivity.waitForSurfaceSizeChanged(
                WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS, mPreviewSize.getWidth(),
                mPreviewSize.getHeight());
        assertTrue("wait for surface change to " + mPreviewSize.toString() + " timed out", res);
        mPreviewSurface = holder.getSurface();
        assertTrue("Preview surface is invalid", mPreviewSurface.isValid());
    }

    /**
     * Setup single capture configuration and start preview.
     *
     * @param previewRequest The capture request to be used for preview
     * @param stillRequest The capture request to be used for still capture
     * @param previewSz Preview size
     * @param captureSz Still capture size
     * @param format The single capture image format
     * @param resultListener Capture result listener
     * @param maxNumImages The max number of images set to the image reader
     * @param imageListener The single capture capture image listener
     */
    private void prepareCaptureAndStartPreview(CaptureRequest.Builder previewRequest,
            CaptureRequest.Builder stillRequest, Size previewSz, Size captureSz, int format,
            CaptureListener resultListener, int maxNumImages,
            ImageReader.OnImageAvailableListener imageListener) throws Exception {
        if (VERBOSE) {
            Log.v(TAG, String.format("Prepare single capture (%s) and preview (%s)",
                    captureSz.toString(), previewSz.toString()));
        }

        // Update preview size.
        updatePreviewSurface(previewSz);

        // Create ImageReader.
        createImageReader(captureSz, format, maxNumImages, imageListener);

        // Configure output streams with preview and jpeg streams.
        List<Surface> outputSurfaces = new ArrayList<Surface>();
        outputSurfaces.add(mPreviewSurface);
        outputSurfaces.add(mReaderSurface);
        configureCameraOutputs(mCamera, outputSurfaces, mCameraListener);

        // Configure the requests.
        previewRequest.addTarget(mPreviewSurface);
        stillRequest.addTarget(mPreviewSurface);
        stillRequest.addTarget(mReaderSurface);

        // Start preview.
        mCamera.setRepeatingRequest(previewRequest.build(), resultListener, mHandler);
    }

    /**
     * Get the max preview size that supports the given fpsRange.
     *
     * @param fpsRange The fps range the returned size must support.
     * @return max size that support the given fps range.
     */
    protected Size getMaxPreviewSizeForFpsRange(int[] fpsRange) {
        if (fpsRange == null || fpsRange[0] <= 0 || fpsRange[1] <= 0) {
            throw new IllegalArgumentException("Invalid fps range argument");
        }
        if (mOrderedPreviewSizes == null || mMinPreviewFrameDurationMap == null) {
            throw new IllegalStateException("mOrderedPreviewSizes and mMinPreviewFrameDurationMap"
                    + " must be initialized");
        }

        long[] frameDurationRange =
                new long[]{(long) (1e9 / fpsRange[1]), (long) (1e9 / fpsRange[0])};
        for (Size size : mOrderedPreviewSizes) {
            long minDuration = mMinPreviewFrameDurationMap.get(size);
            if (minDuration <= frameDurationRange[0]) {
                return size;
            }
        }

        return null;
    }
}