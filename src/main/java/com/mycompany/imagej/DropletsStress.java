package com.mycompany.imagej;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import customnode.CustomPointMesh;
import ij.ImagePlus;
import ij3d.Image3DUniverse;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.display.ImageDisplayService;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Vertices;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Shape;

@Plugin(type = Command.class, menuPath = "Plugins>3D Droplets Stress")
public class DropletsStress<T extends RealType<T>> implements Command {

    @Parameter
    private ImageJ ij;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Parameter
    private ImageDisplayService imageDisplayService;


    @Override
    public void run() {

        /*SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Passez opService comme argument au constructeur de DropletsStressGUI
                new DropletsStressGUI(opService).setVisible(true);
            }
        });*/

        uiService.showUI();

		// ask the user for a file to open
		File file = uiService.chooseFile(null, "FileWidget.OPEN_STYLE");

		if (file == null) {
			return;
		}

		// load the dataset
		Dataset dataset;
		try {
			dataset = ij.scifio().datasetIO().open(file.getPath());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		final Img<T> image_ini = (Img<T>) dataset.getImgPlus();
		uiService.show("initial image", image_ini);

		int radius = 3;
		Shape shape = new HyperSphereShape(radius);
		Img<T> dec = image_ini.copy();
		opService.filter().median(dec, image_ini, shape);
		uiService.show("median filtered image", dec);

		RandomAccessibleInterval<T> edge = opService.filter().sobel(dec);
		//uiService.show("3D sobel edges filtered image", edge);

		// Parameters for refinement
        int iterations_PSF = 10;
        int smoothing_sigma = 1;
        int n_smoothing_iterations = 10;
        double resampling_length = 1.5;
        int n_tracing_iterations = 10;
        int trace_length = 12;
        double outlier_tolerance = 0.5;
        boolean blurred = true;

		// Rescale
        ImagePlus tmp = ImageJFunctions.wrap(dec, null);
        ProcessImage.initializeTargetScalingFactor(tmp);
        RandomAccessibleInterval<T> rescaled_image = ProcessImage.rescaleImage(edge, ProcessImage.scalingFactor);
		uiService.show("rescaled image", rescaled_image);

		// It is sometimes necessary to blurr the image if it's still too noisy
		RandomAccessibleInterval<T> blurred_image = opService.filter().gauss(rescaled_image, smoothing_sigma);
		//uiService.show("blurred image", blurred_image);

		// Binarization and marching cube
		// https://javadoc.scijava.org/ImageJ2/net/imagej/mesh/Mesh.html
		IterableInterval<T> iterableBlurredImage = Views.iterable(blurred_image);
		IterableInterval<BitType> binarized_image = opService.threshold().otsu(iterableBlurredImage);
		uiService.show("otsu", binarized_image);

		Mesh mesh = opService.geom().marchingCubes((RandomAccessibleInterval<T>) binarized_image, 1);
		Vertices vertices = mesh.vertices();

		List<Point3f> points = new ArrayList<Point3f>();
		for(int i = 0; i < vertices.size(); i++) {
		    points.add(new Point3f(vertices.xf(i), vertices.yf(i), vertices.zf(i)));
		}

		List<Point3f> resampled_points = ResamplePointCloud.resamplePointCloud(points, resampling_length);
		for (int i = 0; i<n_tracing_iterations; i++)
		    resampled_points = ResamplePointCloud.resamplePointCloud(resampled_points, resampling_length);
		
        System.out.println("Taille finale du nuage de points : " + resampled_points.size());
      
		// Affichage
		// https://github.com/bene51/3DViewer_Examples/blob/master/src/main/java/examples/Plot_Points.java
		CustomPointMesh cm = new CustomPointMesh(resampled_points);

		// Create a universe and show it
		Image3DUniverse univ = new Image3DUniverse();
		univ.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);
		univ.show();

		// Add the mesh
		String name = "points";
		univ.addCustomMesh(cm, name);

		cm.setPointSize(1);
		cm.setColor(new Color3f(255,255,255));
		
		// Curvature


    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services

        final ImageJ ij = new ImageJ();

        // invoke the plugin
        ij.command().run(DropletsStress.class, true);

    }

}