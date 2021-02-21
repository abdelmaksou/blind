package com.abdelmaksou.blind.tfLiteClassifier;

import android.app.Activity;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class tfLiteClassifier {

    // declaration
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float IMAGE_MEAN = 0.0f;
    private static final int MAX_SIZE = 5;
    private final List<String> labels;
    private final Interpreter tensorClassifier;
    private final int imageResizeX, imageResizeY;
    private TensorImage inputImageBuffer;
    private final TensorBuffer probabilityImageBuffer;
    private final TensorProcessor probabilityProcessor;

    public tfLiteClassifier(Activity activity) throws IOException {
        // load tflite pre-trained model and labels
        MappedByteBuffer classifierModel = FileUtil.loadMappedFile(activity, "mobilenet_v2_1.0_224_quant.tflite");
        labels = FileUtil.loadLabels(activity, "labels_mobilenet_quant_v1_224.txt");

        // define tf interpreter
        tensorClassifier = new Interpreter(classifierModel, null);

        // shape parameters
        int imageIndex = 0;
        int probabilityIndex = 0;

        int[] inputImageShape = tensorClassifier.getInputTensor(imageIndex).shape();
        DataType inputDataType = tensorClassifier.getInputTensor(imageIndex).dataType();

        int[] outputImageShape = tensorClassifier.getOutputTensor(probabilityIndex).shape();
        DataType outputDataType = tensorClassifier.getOutputTensor(probabilityIndex).dataType();

        imageResizeX = inputImageShape[1];
        imageResizeY = inputImageShape[2];

        // buffers
        inputImageBuffer = new TensorImage(inputDataType);
        probabilityImageBuffer = TensorBuffer.createFixedSize(outputImageShape, outputDataType);

        // processing
        probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD))
                .build();

    }


    public List<Recognition> recognizeImage(final Bitmap bitmap, final int sensorOrientation)
    {
        List<Recognition> results = new ArrayList<>();
        inputImageBuffer = loadImage(bitmap, sensorOrientation);
        tensorClassifier.run(inputImageBuffer.getBuffer(), probabilityImageBuffer.getBuffer().rewind());
        Map<String, Float> lProbability = new TensorLabel(labels, probabilityProcessor.process(probabilityImageBuffer)).getMapWithFloatValue();
        for (Map.Entry<String, Float> entry : lProbability.entrySet())
        {
            results.add(new Recognition(entry.getKey(), entry.getValue()));
        }
        // sort predictions based on confidence rate
        Collections.sort(results);
        return results;
    }

    // load bitmap image method
    private TensorImage loadImage(Bitmap bitmap, int sensorOrientation) {
        inputImageBuffer.load(bitmap);
        int no_of_rot = sensorOrientation / 90;
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(imageResizeX, imageResizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(new Rot90Op(no_of_rot))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();
        return imageProcessor.process(inputImageBuffer);
    }

    // Recognition object type class
    public class Recognition implements Comparable
    {
        private String name;
        private float Confidence;

        public Recognition() {}

        public Recognition(String name, float Confidence)
        {
            this.name = name;
            this.Confidence = Confidence;
        }

        public float getConfidence() {
            return Confidence;
        }

        public String getName() {
            return name;
        }

        public void setConfidence(float confidence) {
            Confidence = confidence;
        }

        public void setName(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return "Recognition : { name = ' " + name + " ', confidence = " + Confidence + " }";
        }

        @Override
        public int compareTo(Object o) {
            return Float.compare(((Recognition)o).Confidence, this.Confidence);
        }
    }
}
