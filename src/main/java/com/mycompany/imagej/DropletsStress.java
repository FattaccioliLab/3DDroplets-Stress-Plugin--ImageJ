package com.mycompany.imagej;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

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

		// Demander à l'utilisateur d'ouvrir un fichier
		File file = uiService.chooseFile(null, "FileWidget.OPEN_STYLE");

		if (file == null) {
			return;
		}

		// Charger le dataset
		Dataset dataset;
		try {
			dataset = ij.scifio().datasetIO().open(file.getPath());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		final Img<T> image_ini = (Img<T>) dataset.getImgPlus();
		uiService.show("initial image", image_ini);

		// L'image donnée en entrée est celle avec le filtre médian déjà appliqué car c'est celle avec la bonne correction
		/*int radius = 3;
		Shape shape = new HyperSphereShape(radius);
		Img<T> dec = image_ini.copy();
		opService.filter().median(dec, image_ini, shape);
		uiService.show("median filtered image", dec);*/

		// On ignore cette étape dans notre cas pour éviter une double couche 
		//RandomAccessibleInterval<T> edge = opService.filter().sobel(image_ini);
		//uiService.show("3D sobel edges filtered image", edge);

		// Paramètres de raffinement
        int iterations_PSF = 10;
        int smoothing_sigma = 1;
        int n_smoothing_iterations = 10;
        double resampling_length = 2.5;
        int n_tracing_iterations = 10;
        int trace_length = 12;
        double outlier_tolerance = 0.5;
        boolean blurred = true;

		// Rescale
        ImagePlus tmp = ImageJFunctions.wrap(image_ini, null);
        ProcessImage.initializeTargetScalingFactor(tmp);
        RandomAccessibleInterval<T> rescaled_image = ProcessImage.rescaleImage(image_ini, ProcessImage.scalingFactor);
		uiService.show("rescaled image", rescaled_image);

		// Il est parfois nécessaire de flouter l'image si elle est encore trop bruyante
		RandomAccessibleInterval<T> blurred_image = opService.filter().gauss(rescaled_image, smoothing_sigma);
		uiService.show("blurred image", blurred_image);

		
		// Binarisation et marching cube
		IterableInterval<T> iterableBlurredImage = Views.iterable(blurred_image);
		IterableInterval<BitType> binarized_image = opService.threshold().otsu(iterableBlurredImage);
		uiService.show("otsu", binarized_image);

		Mesh mesh = opService.geom().marchingCubes((RandomAccessibleInterval<T>) binarized_image, 1);
		Vertices vertices = mesh.vertices();

		List<Point3f> points = new ArrayList<Point3f>();
		for(int i = 0; i < vertices.size(); i++) {
		    points.add(new Point3f(vertices.xf(i), vertices.yf(i), vertices.zf(i)));
		}
		
		// Affichage 3D du marching cubes
		CustomPointMesh cm_marching_cubes = new CustomPointMesh(points);
        Image3DUniverse univ1 = new Image3DUniverse();
        univ1.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);
        univ1.show();
        univ1.addCustomMesh(cm_marching_cubes, "marching_cubes");
        cm_marching_cubes.setPointSize(2);
        cm_marching_cubes.setColor(new Color3f(255,255,255));

        
        // Nuage de points
		List<Point3f> resampled_points = ResamplePointCloud.resamplePointCloud(points, resampling_length);
        System.out.println("Taille finale du nuage de points : " + resampled_points.size());
      
		// Affichage 3D du nuage de points
		CustomPointMesh cm_point_cloud = new CustomPointMesh(resampled_points);
		Image3DUniverse univ2 = new Image3DUniverse();
		univ2.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);
		univ2.show();
		univ2.addCustomMesh(cm_point_cloud, "resampled_points");
		cm_point_cloud.setPointSize(2);
		cm_point_cloud.setColor(new Color3f(255,255,255));
		
		
		// Courbure
		int max_degree = 5;
        List<Point3f> fitted_points = new SphericalHarmonicsExpansion(resampled_points, max_degree).expand();
        
        SphericalHarmonicsExpansion.printPoints3D(fitted_points);
        
        // Affichage 3D des harmoniques sphériques
        CustomPointMesh cm_spherical_harmonics = new CustomPointMesh(fitted_points);
        Image3DUniverse univ3 = new Image3DUniverse();
        univ3.showAttribute(Image3DUniverse.ATTRIBUTE_COORD_SYSTEM, true);
        univ3.show();
        univ3.addCustomMesh(cm_spherical_harmonics, "fitted_points");
        cm_spherical_harmonics.setPointSize(3);
        cm_spherical_harmonics.setColor(new Color3f(255,255,255));
        
        SphericalHarmonicsExpansion.writePointsToCSV("test", fitted_points);

    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services

        final ImageJ ij = new ImageJ();

        // invoke the plugin
        ij.command().run(DropletsStress.class, true);

    }

}