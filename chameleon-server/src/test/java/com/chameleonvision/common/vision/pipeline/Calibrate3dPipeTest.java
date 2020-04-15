package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.calibration.CameraCalibrationCoefficients;
import com.chameleonvision.common.vision.pipe.CVPipeResult;
import com.chameleonvision.common.vision.pipe.impl.Calibrate3dPipe;
import com.chameleonvision.common.vision.pipe.impl.FindBoardCornersPipe;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;

public class Calibrate3dPipeTest extends Application {
    static List<Double> data = new ArrayList<Double>();

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        List<Mat> frames = new ArrayList<>();

        File rootDir = new File("D:\\dotboardpics");
        File[] files = rootDir.listFiles();
        for (File file : files) {
            String src = file.getAbsolutePath();
            Mat imgread = Imgcodecs.imread(src);
            frames.add(imgread);
        }

        FindBoardCornersPipe findBoardCornersPipe = new FindBoardCornersPipe();
        findBoardCornersPipe.setParams(
                new FindBoardCornersPipe.FindCornersPipeParams(11, 4, false, 15));
        CVPipeResult<List<List<Mat>>> findBoardCornersPipeOutput = findBoardCornersPipe.apply(frames);

        Calibrate3dPipe calibrate3dPipe = new Calibrate3dPipe();
        calibrate3dPipe.setParams(new Calibrate3dPipe.CalibratePipeParams(new Size(640, 480)));

        CVPipeResult<CameraCalibrationCoefficients> calibrate3dPipeOutput =
                calibrate3dPipe.apply(findBoardCornersPipeOutput.result);
        for (double d : calibrate3dPipeOutput.result.getPerViewErrors()) data.add(d);
        findBoardCornersPipeOutput
                .result
                .get(0)
                .forEach(
                        mat -> {
                            HighGui.imshow("Frame", mat);
                            HighGui.waitKey(300);
                        });

        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Per View Errors");
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final BarChart<String, Number> bc = new BarChart<>(xAxis, yAxis);
        bc.setTitle("Snapshot Re-projection Errors");
        xAxis.setLabel("Snapshot");
        yAxis.setLabel("px");
        XYChart.Series series1 = new XYChart.Series();
        System.out.println(data);
        for (int i = 0; i < data.size(); i++) {
            series1.getData().add(new XYChart.Data(i + 1 + "", data.get(i)));
        }

        Scene scene = new Scene(bc, 800, 600);
        bc.getData().addAll(series1);
        stage.setScene(scene);
        stage.show();
    }
}
