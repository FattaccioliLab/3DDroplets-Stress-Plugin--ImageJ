package com.mycompany.imagej;

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

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import customnode.CustomPointMesh;
import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import ij3d.Image3DUniverse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



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

	
	public  <T extends RealType<T>> Img<T> applyMedianFilter(Img<T> image, double radius) {
	        // Convertir Img<T> en ImagePlus pour l'utiliser avec ImageJ1
	        ImagePlus imagePlus = ImageJFunctions.wrap(image, "wrapped");

	        // Appliquer le filtre médian avec radius
	        RankFilters filter = new RankFilters();
	        ImageProcessor ip = imagePlus.getProcessor();
	        filter.rank(ip, radius, RankFilters.MEDIAN);
	        
	        // Reconvertir le résultat en Img<T>
	        Img<T> result = ImageJFunctions.wrapReal(imagePlus);

	        return result;
	}
	
	    
	@Override
	public void run() {
		
		uiService.showUI();

		// ask the user for a file to open
		File file = uiService.chooseFile(null, "FileWidget.OPEN_STYLE");

		if (file == null) {
			return;
		}
        
		// load the dataset
		Dataset currentData;
		try {
			currentData = ij.scifio().datasetIO().open(file.getPath());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// show the image
		//uiService.show(currentData);

		final Img<T> image = (Img<T>)currentData.getImgPlus();
		
		int mean = 1; //TODO
		Img<T> medianFiltered = applyMedianFilter(image, mean);
		//uiService.show("Median Filtered", medianFiltered);

		// Parameters for refinement
		int iterations_PSF = 10;
		int smoothing_sigma = 1;
		int n_smoothing_iterations = 10;
		double resampling_length = 2.5;
		int n_tracing_iterations = 10;
		int trace_length = 12;
		double outlier_tolerance = 0.5;
		boolean blurred = true;

		// Rescale
		double[] scalingFactors = {0, 0, 0}; //TODO?
		
		// It is sometimes necessary to blurr the image if it's still too noisy
		RandomAccessibleInterval<T> blurred_image = opService.filter().gauss(medianFiltered, smoothing_sigma);
		//uiService.show(blurred_image);
		
		// Binarization and marching cube
		// https://javadoc.scijava.org/ImageJ2/net/imagej/mesh/Mesh.html
		IterableInterval<T> iterableBlurredImage = Views.iterable(blurred_image);
		IterableInterval<BitType> binarized_image = opService.threshold().otsu(iterableBlurredImage);

		Mesh mesh = opService.geom().marchingCubes((RandomAccessibleInterval<T>) binarized_image, 0.5);
		Vertices vertices = mesh.vertices();

		float[][] points = new float[(int) vertices.size()][3];
		for (long i = 0; i < vertices.size(); i++) {
			float x = vertices.xf(i);
			float y = vertices.yf(i);
			float z = vertices.zf(i);

			points[(int) i][0] = x;
			points[(int) i][1] = y;
			points[(int) i][2] = z;
		}

		// Create a CustomMesh from them
		List<Point3f> custom_mesh = new ArrayList<Point3f>();
		for(int i = 0; i < points.length; i++) {
			custom_mesh.add(new Point3f(points[i][0], points[i][1], points[i][2]));
		}

		// Affichage
		// https://github.com/bene51/3DViewer_Examples/blob/master/src/main/java/examples/Plot_Points.java
		CustomPointMesh cm = new CustomPointMesh(custom_mesh);

		// Create a universe and show it
		Image3DUniverse univ = new Image3DUniverse();
		univ.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, false);
		univ.show();

		// Add the mesh
		String name = "points";
		univ.addCustomMesh(cm, name);

		cm.setPointSize(1);
		cm.setColor(new Color3f(255,255,255));

	}


	public static void main(final String... args) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();
		
		// invoke the plugin
		ij.command().run(DropletsStress.class, true);
	}

}