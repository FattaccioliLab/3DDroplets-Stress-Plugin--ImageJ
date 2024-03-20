package com.mycompany.imagej;

import org.scijava.vecmath.Point3f;

import java.util.ArrayList;
import java.util.List;

public class ResamplePointCloud {

    public static List<Point3f> resamplePointCloud(List<Point3f> points, double samplingLength) {

        // Estimate point number according to passed sampling length
        double meanRadius = 0;
        for (Point3f point : points) {
            meanRadius += Math.sqrt(point.x * point.x + point.y * point.y + point.z * point.z);
        }
        meanRadius = meanRadius / points.size();
        double surfaceArea = meanRadius * meanRadius * 4 * Math.PI;
        int n = (int) (surfaceArea / (samplingLength * samplingLength));

        // Sample points on unit sphere according to Fibonacci scheme
        List<Point3f> sampledPoints = fibonacciSampling2(n);

        // Interpolate cartesian coordinates
        List<Point3f> resampledPoints = new ArrayList<>();
        double x, y, z;
        for (Point3f sampledPoint : sampledPoints) {
            x = interpolateCoordinate(points, sampledPoint, 0);
            y = interpolateCoordinate(points, sampledPoint, 1);
            z = interpolateCoordinate(points, sampledPoint, 2);
            resampledPoints.add(new Point3f((float)x, (float)y, (float)z));
        }

        return resampledPoints;

    }

    private static List<Point3f> fibonacciSampling2(int numberOfPoints) {
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
        for (int i = 0; i < points.size(); i++) {
            Point3f point = points.get(i);
            double dist = point.distance(sampledPoint);
            double weight = 1 / dist;
            weightSum += weight;
            if (axe == 0)
                weightedSum += points.get(i).x * weight;
            else if (axe == 1)
                weightedSum += points.get(i).y * weight;
            else if (axe == 2)
                weightedSum += points.get(i).z * weight;
        }
        return weightedSum / weightSum;
    }

}