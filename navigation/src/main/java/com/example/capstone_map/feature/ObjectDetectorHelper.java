package com.example.capstone_map.feature;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;
import java.io.IOException;
import java.util.List;

public class ObjectDetectorHelper {
    private final float threshold;
    private final int numThreads;
    private final int maxResults;
    private final String modelName;
    private final Context context;
    private final DetectorListener detectorListener;
    private ObjectDetector objectDetector;

    public ObjectDetectorHelper(Context context, String modelName, float threshold, int numThreads, int maxResults, DetectorListener detectorListener) {
        this.context = context;
        this.modelName = modelName;
        this.threshold = threshold;
        this.numThreads = numThreads;
        this.maxResults = maxResults;
        this.detectorListener = detectorListener;
        setupObjectDetector();
    }

    private void setupObjectDetector() {
        ObjectDetector.ObjectDetectorOptions.Builder optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults);
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads);
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build());
        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build());
        } catch (IOException e) {
            detectorListener.onError("Object detector failed to initialize. See error logs for details");
            Log.e("ObjectDetectorHelper", "TFLite failed to load model with error: " + e.getMessage());
        }
    }

    public void detect(Bitmap image, int imageRotation) {
        if (objectDetector == null) {
            setupObjectDetector();
        }
        long inferenceTime = SystemClock.uptimeMillis();
        ImageProcessor imageProcessor = new ImageProcessor.Builder().add(new Rot90Op(-imageRotation / 90)).build();
        TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(image));
        List<Detection> results = objectDetector.detect(tensorImage);
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
        detectorListener.onResults(results, inferenceTime);
    }

    public interface DetectorListener {
        void onError(String error);
        void onResults(List<Detection> results, long inferenceTime);
    }
}