package com.mycompany.imagej;

import org.apache.commons.math3.analysis.interpolation.BicubicInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.BicubicInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.scijava.vecmath.Point3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ResamplePointCloud {

    public static List<Point3f> resamplePointCloud(List<Point3f> points, double samplingLength) {
        Point3f center = calculateCenter(points);
        List<Point3f> pointsCentered = new ArrayList<>();
        for (Point3f point : points) {
            pointsCentered.add(new Point3f(point.x - center.x, point.y - center.y, point.z - center.z));
        }
        List<Point3f> pointsSpherical = cart2spher(pointsCentered);
        
        // Estimate point number according to passed sampling length
        double meanRadius = 0;
        for (Point3f point : pointsSpherical) {
            meanRadius += Math.sqrt(point.x * point.x + point.y * point.y + point.z * point.z);
        }
        meanRadius = meanRadius / pointsSpherical.size();
        double surfaceArea = meanRadius * meanRadius * 4 * Math.PI;
        int n = (int) (surfaceArea / (samplingLength * samplingLength));

        // Sample points on unit sphere according to Fibonacci scheme
        List<Point3f> sampledPoints = fibonacciSampling(n);
        //sampledPoints = cart2spher(sampledPoints);

        // Interpolate cartesian coordinates
        List<Point3f> interpolatedPoints = interpolateCoordinates(pointsSpherical, sampledPoints);

        // Ancienne version
        /*List<Point3f> interpolatedPoints = new ArrayList<>();
        double x, y, z;
        for (Point3f sampledPoint : sampledPoints) {
            x = interpolateCoordinate(points, sampledPoint, 0);
            y = interpolateCoordinate(points, sampledPoint, 1);
            z = interpolateCoordinate(points, sampledPoint, 2);
            interpolatedPoints.add(new Point3f((float)x, (float)y, (float)z));
        }*/
  
        return spher2cart(interpolatedPoints);

    }
    
    private static Point3f calculateCenter(List<Point3f> points) {
        float sumX = 0, sumY = 0, sumZ = 0;
        for (Point3f point : points) {
            sumX += point.x;
            sumY += point.y;
            sumZ += point.z;
        }
        return new Point3f(sumX / points.size(), sumY / points.size(), sumZ / points.size());
    }
    
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
    
    private static double interpolateCoordinate(List<Point3f> points, Point3f sampledPoint, int axe) {
        double weightSum = 0;
        double weightedSum = 0;
        for (Point3f point : points) {
            double dist = point.distance(sampledPoint);
            double weight = 1 / dist;
            weightSum += weight;
            switch (axe) {
            case 0:
                weightedSum += point.x * weight;
                break;
            case 1:
                weightedSum += point.y * weight;
                break;
            case 2:
                weightedSum += point.z * weight;
                break;
            default:
                throw new IllegalArgumentException("L'axe doit être égal à 0, 1, ou 2.");
            } 
        }
        return weightedSum / weightSum;
    }
    
    public static List<Point3f> interpolateCoordinates(List<Point3f> points, List<Point3f> sampledPoints) {
        // Extract coordinates from pointsSpherical
        double[] x = new double[points.size()];
        double[] y = new double[points.size()];
        double[] z = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            Point3f point = points.get(i);
            x[i] = point.x;
            y[i] = point.y;
            z[i] = point.z;
        }

        // Interpolate x, y, z coordinates separately
        double[] interpolatedX = interpolate1D(x, sampledPoints.size());
        double[] interpolatedY = interpolate1D(y, sampledPoints.size());
        double[] interpolatedZ = interpolate1D(z, sampledPoints.size());

        // Combine interpolated coordinates into Point3f objects
        List<Point3f> interpolatedPoints = new ArrayList<>();
        for (int i = 0; i < sampledPoints.size(); i++) {
            interpolatedPoints.add(new Point3f((float) interpolatedX[i], (float) interpolatedY[i], (float) interpolatedZ[i]));
        }

        return interpolatedPoints;
    }

    private static double[] interpolate1D(double[] data, int n) {
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction splineFunction = interpolator.interpolate(getSampleIndices(data.length), data);

        // Evaluate the spline function at the sampled points
        double[] interpolatedValues = new double[n];
        double step = (double) (data.length - 1) / (n - 1);
        for (int i = 0; i < n; i++) {
            double x = i * step;
            interpolatedValues[i] = splineFunction.value(x);
        }

        return interpolatedValues;
    }

    private static double[] getSampleIndices(int n) {
        double[] indices = new double[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        return indices;
    }
    

}