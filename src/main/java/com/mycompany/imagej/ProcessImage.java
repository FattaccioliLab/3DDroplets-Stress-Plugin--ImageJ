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


public class ProcessImage {
    public static double targetVoxelSize;
    public static double[] scalingFactor;

    public static void initializeTargetScalingFactor(ImagePlus image) {
        Calibration cal = image.getCalibration();
        double z;

        // Tenter de récupérer 'spacing' comme la profondeur Z à partir des métadonnées de l'image
        // Sinon, utiliser la profondeur de pixel de la calibration de l'image comme valeur par défaut
        if (cal != null && cal.pixelDepth > 0) {
            z = cal.pixelDepth; // Utiliser la profondeur de pixel si disponible
        } else {
            // Si 'spacing' n'est pas disponible et cal.pixelDepth n'est pas défini, utiliser 1.0 comme valeur par défaut
            z = 1.0;
        }

        double y = cal.pixelHeight; // Hauteur Y (taille du voxel en Y)
        double x = cal.pixelWidth; // Largeur X (taille du voxel en X)

        // Assurez-vous que les valeurs sont significatives, sinon attribuez 1.0 par défaut
        z = (z != 0 ? z : 1.0);
        y = (y != 0 ? y : 1.0);
        x = (x != 0 ? x : 1.0);

        double voxelSize[] = new double[]{z, y, x};

        // Définissez ici comment vous souhaitez déterminer le targetVoxelSize
        double targetVoxelSize = y; // Par exemple, utiliser y ou une autre logique pour déterminer la taille cible
        scalingFactor = new double[3];

        // Calculer le facteur d'échelle pour chaque dimension
        for (int i = 0; i < 3; i++) {
            scalingFactor[i] = voxelSize[i] / targetVoxelSize;
            System.out.println("Scaling factor for dimension " + i + ": " + scalingFactor[i]);
        }
    }

    public static double getTargetVoxelSize() {
        return targetVoxelSize;
    }

    public static double[] getScalingFactor() {
        return scalingFactor;
    }

    public static <T extends RealType<T>> RandomAccessibleInterval<T> rescaleImage(RandomAccessibleInterval<T> image, double[] scalingFactors) {
        AffineTransform3D transform = new AffineTransform3D();
        double x = scalingFactors[1];
        double y = scalingFactors[2];
        double z = scalingFactors[0];
        transform.scale(x, y, z);

        RealRandomAccessible<T> interpolated = Views.interpolate(Views.extendBorder(image), new NearestNeighborInterpolatorFactory<>());
        RealRandomAccessible<T> transformed = RealViews.transform(interpolated, transform);

        if (image.numDimensions() != 3) {
            System.out.println("Erreur dans les dimensions de l'image");
            return null;
        }

        // Calculer les nouvelles dimensions
        long[] newDimensions = new long[3];
        newDimensions[0] = (long) (image.dimension(0) * x);
        newDimensions[1] = (long) (image.dimension(1) * y);
        newDimensions[2] = (long) (image.dimension(2) * z);

        // Créer une nouvelle RandomAccessibleInterval avec les nouvelles dimensions
        IntervalView<T> rescaledImage = Views.interval((RandomAccessible<T>) transformed, Intervals.createMinSize(0,  0,  0, newDimensions[0], newDimensions[1], newDimensions[2]));

        return rescaledImage;
    }

}