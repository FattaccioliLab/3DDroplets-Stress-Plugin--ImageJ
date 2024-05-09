package com.mycompany.imagej;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Cette classe contient les méthodes pour le traitement des images.
 */
public class ProcessImage {
    public static double targetVoxelSize;
    public static double[] scalingFactor = new double[3];

    /**
     * Initialise les facteurs d'échelle pour chaque dimension de l'image
     * en fonction de la taille du voxel dans chaque dimension par rapport à une taille cible.
     * 
     * @param image L'image à partir de laquelle les facteurs d'échelle seront calculés.
     */
    public static void initializeTargetScalingFactor(ImagePlus image) {
        
        // Récupérer les informations de calibration de l'image
        Calibration cal = image.getCalibration();
        
        double z; // Profondeur Z (taille du voxel en Z)
        double x = cal.pixelWidth; // Largeur X (taille du voxel en X)
        double y = cal.pixelHeight; // Hauteur Y (taille du voxel en Y)
        
        if (cal != null && cal.pixelDepth > 0) {
            z = cal.pixelDepth; // Récupérer la taille du voxel dans la dimension Z si disponible
        } else {
            z = 1.0;
        }

        // On s'assure que les valeurs ne sont pas nulles (pour éviter une division par zéro)
        x = (x != 0 ? x : 1.0);
        y = (y != 0 ? y : 1.0);
        z = (z != 0 ? z : 1.0);

        double voxelSize[] = new double[]{z, x, y};
        double targetVoxelSize = x; // Taille cible du voxel
        
        // Calculer le tableau des facteurs d'échelle pour chaque dimension
        for (int i = 0; i < 3; i++) {
            scalingFactor[i] = voxelSize[i] / targetVoxelSize;
            System.out.println("Coefficient d'échelle pour la dimension " + i + ": " + scalingFactor[i]);
        }
    }

    public static double getTargetVoxelSize() {
        return targetVoxelSize;
    }

    public static double[] getScalingFactor() {
        return scalingFactor;
    }

    /**
     * Redimensionne une image avec les facteurs d'échelle spécifiés.
     * @param image L'image à redimensionner.
     * @param scalingFactors Les facteurs d'échelle pour chaque dimension de l'image (z, x, y).
     * 
     * @return L'image redimensionnée.
     */
    public static <T extends RealType<T>> RandomAccessibleInterval<T> rescaleImage(RandomAccessibleInterval<T> image, double[] scalingFactors) {
        // Créer une transformation affine avec les facteurs d'échelle spécifiés
        AffineTransform3D transform = new AffineTransform3D();
        double x = scalingFactors[1];
        double y = scalingFactors[2];
        double z = scalingFactors[0];
        transform.scale(x, y, z);

        // Interpoler l'image avec la méthode du plus proche voisin
        RealRandomAccessible<T> interpolated = Views.interpolate(Views.extendBorder(image), new NearestNeighborInterpolatorFactory<>());
        RealRandomAccessible<T> transformed = RealViews.transform(interpolated, transform);

        if (image.numDimensions() != 3) {
            System.out.println("Erreur dans les dimensions de l'image");
            return null;
        }

        // Calculer les nouvelles dimensions de l'image
        long[] newDimensions = new long[3];
        newDimensions[0] = (long) (image.dimension(0) * x);
        newDimensions[1] = (long) (image.dimension(1) * y);
        newDimensions[2] = (long) (image.dimension(2) * z);

        // Créer une nouvelle RandomAccessibleInterval avec les nouvelles dimensions
        IntervalView<T> rescaledImage = Views.interval((RandomAccessible<T>) transformed, Intervals.createMinSize(0,  0,  0, newDimensions[0], newDimensions[1], newDimensions[2]));

        return rescaledImage;
    }

}