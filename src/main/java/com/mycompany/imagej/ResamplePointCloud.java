package com.mycompany.imagej;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.scijava.vecmath.Point3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Cette classe contient les methodes pour le reechantillonnage et l'interpolation d'un nuage de points en 3D.
 */
public class ResamplePointCloud {

    /**
     * Reechantillonne un nuage de points de type spherique sur une grille de Fibonacci.
     * 
     * @param points La liste des points du nuage a reechantillonner.
     * @param samplingLength Distance entre les emplacements des points echantillonnes.
     * @return Les points reechantillonnes.
     * 
     * @see https://github.com/campaslab/napari-stress/blob/8ede222ebfb5f1209c368ce8263ca74a1914e4c9/src/napari_stress/_reconstruction/refine_surfaces.py#L234
     * 
     */
    public static List<Point3f> resamplePointCloud(List<Point3f> points, double samplingLength) {
        // Conversion en coordonnees spheriques, relatives
        Point3f center = calculateCenter(points);
        List<Point3f> pointsCentered = new ArrayList<>();
        for (Point3f point : points) {
            pointsCentered.add(new Point3f(point.x - center.x, point.y - center.y, point.z - center.z));
        }
        List<Point3f> pointsSpherical = cart2spher(pointsCentered);
        
        // Estimation du nombre de points en fonction de la longueur d'echantillonnage passee
        double meanRadius = 0;
        for (Point3f point : pointsSpherical) {
            meanRadius += Math.sqrt(point.x * point.x + point.y * point.y + point.z * point.z);
        }
        meanRadius = meanRadius / pointsSpherical.size();
        double surfaceArea = meanRadius * meanRadius * 4 * Math.PI;
        int n = (int) (surfaceArea / (samplingLength * samplingLength));

        // Echantillonnage des points sur la sphere unitaire selon le schema de Fibonacci
        List<Point3f> sampledPoints = fibonacciSampling(n);

        // Interpolation des coordonnees
        List<Point3f> interpolatedPoints = interpolateCoordinates(pointsSpherical, sampledPoints);

        return spher2cart(interpolatedPoints);
    }
    
    /**
     * Calcule le centre de gravite du nuage de points.
     * 
     * @param points La liste des points du nuage.
     * 
     * @return Le centre de gravite du nuage de points.
     */
    private static Point3f calculateCenter(List<Point3f> points) {
        float sumX = 0, sumY = 0, sumZ = 0;
        for (Point3f point : points) {
            sumX += point.x;
            sumY += point.y;
            sumZ += point.z;
        }
        return new Point3f(sumX / points.size(), sumY / points.size(), sumZ / points.size());
    }
    
    /**
     * Convertit les coordonnees cartesiennes en coordonnees spheriques.
     * 
     * @param points La liste des points en coordonnees cartesiennes.
     * @return La liste des points en coordonnees spheriques.
     * 
     * @see https://github.com/marcomusy/vedo/blob/cb0b514f5026afd841cef65fa8a4d5eb4bdc5fd5/vedo/transformations.py#L1110
     */
    private static List<Point3f> cart2spher(List<Point3f> points) {
        List<Point3f> pointsSpherical = new ArrayList<>();

        for (Point3f p : points) {
            double hxy = Math.hypot(p.x, p.y);
            double rho = Math.hypot(hxy, p.z);
            double theta = Math.atan2(hxy, p.z);
            double phi = Math.atan2(p.y, p.x);

            pointsSpherical.add(new Point3f((float)rho, (float)theta, (float)phi));
        }

        return pointsSpherical;
    }
    
    /**
     * Convertit les coordonnees spheriques en coordonnees cartesiennes.
     * 
     * @param points La liste des points en coordonnees spheriques.
     * @return La liste des points en coordonnees cartesiennes.
     * 
     * @see https://github.com/marcomusy/vedo/blob/cb0b514f5026afd841cef65fa8a4d5eb4bdc5fd5/vedo/transformations.py#L1121
     */
    private static List<Point3f> spher2cart(List<Point3f> points) {
        List<Point3f> pointsCartesian = new ArrayList<>();

        for (Point3f p : points) {
            double st = Math.sin(p.y);
            double sp = Math.sin(p.z);
            double ct = Math.cos(p.y);
            double cp = Math.cos(p.z);
            double rst = p.x * st;
            double x = rst * cp;
            double y = rst * sp;
            double z = p.x * ct;

            pointsCartesian.add(new Point3f((float)x, (float)y, (float)z));
        }

        return pointsCartesian;
    }

    /**
     * Echantillonne les points sur une sphere selon le schema de Fibonacci.
     * 
     * @param numberOfPoints Le nombre de points a echantillonner.
     * @return Une liste de points echantillonnes sur la sphere.
     * 
     * @see https://github.com/campaslab/napari-stress/blob/8ede222ebfb5f1209c368ce8263ca74a1914e4c9/src/napari_stress/_reconstruction/fit_utils.py#L129
     */
    private static List<Point3f> fibonacciSampling(int numberOfPoints) {
        List<Point3f> points = new ArrayList<>();
        double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;

        for (int i = 0; i < numberOfPoints; i++) {
            double theta = 2.0 * Math.PI * i / goldenRatio;
            double phi = Math.acos(1 - 2 * (i + 0.5) / numberOfPoints);
            double x = Math.cos(theta) * Math.sin(phi);
            double y = Math.sin(theta) * Math.sin(phi);
            double z = Math.cos(phi);

            points.add(new Point3f((float)x, (float)y, (float)z));
        }

        return points;
    }
    
    /**
     * Interpole les coordonnees des points pour correspondre aux points echantillones.
     * 
     * @param points Les coordonnees des points a interpoler.
     * @param sampledPoints Les points echantillonnes.
     * @return Une liste de points interpoles.
     */
    public static List<Point3f> interpolateCoordinates(List<Point3f> points, List<Point3f> sampledPoints) {
        // Extraire les coordonnees x, y, z des points
        double[] x = new double[points.size()];
        double[] y = new double[points.size()];
        double[] z = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            Point3f point = points.get(i);
            x[i] = point.x;
            y[i] = point.y;
            z[i] = point.z;
        }

        // Interpoler les coordonnees x, y, z separement
        double[] interpolatedX = interpolate1D(x, sampledPoints.size());
        double[] interpolatedY = interpolate1D(y, sampledPoints.size());
        double[] interpolatedZ = interpolate1D(z, sampledPoints.size());

        // Combiner les coordonnees interpolees en objets Point3f
        List<Point3f> interpolatedPoints = new ArrayList<>();
        for (int i = 0; i < sampledPoints.size(); i++) {
            interpolatedPoints.add(new Point3f((float) interpolatedX[i], (float) interpolatedY[i], (float) interpolatedZ[i]));
        }

        return interpolatedPoints;
    }

    /**
     * Interpole un ensemble de donnees 1D.
     * 
     * @param data Les donnees a interpoler.
     * @param n Le nombre de points a interpoler.
     * @return Les valeurs interpolees.
     */
    private static double[] interpolate1D(double[] data, int n) {
        // On utilise un interpolateur de spline pour interpoler les donnees
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction splineFunction = interpolator.interpolate(getSampleIndices(data.length), data);

        // evaluer la fonction de spline aux points echantillonnes
        double[] interpolatedValues = new double[n];
        double step = (double) (data.length - 1) / (n - 1);
        for (int i = 0; i < n; i++) {
            double x = i * step;
            interpolatedValues[i] = splineFunction.value(x);
        }

        return interpolatedValues;
    }

    /**
     * Genere les indices d'echantillonnage.
     * 
     * @param n Le nombre d'indices a generer.
     * @return Les indices d'echantillonnage.
     */
    private static double[] getSampleIndices(int n) {
        double[] indices = new double[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        return indices;
    }
    

}