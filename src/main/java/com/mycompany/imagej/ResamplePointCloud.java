package com.mycompany.imagej;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.scijava.vecmath.Point3f;

import java.util.ArrayList;
import java.util.List;

public class ResamplePointCloud {

    public static List<Point3f> resamplePointCloud(List<Point3f> points, double samplingLength) {

        List<double[]> convertedPoints = new ArrayList<double[]>();
        for (Point3f point : points) {
            convertedPoints.add(new double[] {point.x, point.y, point.z});
        }

        List<Coordinate> coordinateList = new ArrayList<>();
        for (double[] point : convertedPoints) {
            coordinateList.add(new Coordinate(point[0], point[1], point[2]));
        }

        // Convertir les coordonnées en JTS en points JTS
        GeometryFactory geometryFactory = new GeometryFactory();
        List<Point> jtsPoints = new ArrayList<>();
        for (Coordinate coordinate : coordinateList) {
            jtsPoints.add(geometryFactory.createPoint(coordinate));
        }

        // Convertir les points JTS en Apache Commons Math Vectors pour l'interpolation
        List<Vector3D> vectors = new ArrayList<>();
        for (Point point : jtsPoints) {
            Coordinate coordinate = point.getCoordinate();
            vectors.add(new Vector3D(coordinate.getX(), coordinate.getY(), coordinate.getZ()));
        }

        // Estimate point number according to passed sampling length
        double meanRadius = 0;
        for (Vector3D vector : vectors) {
            meanRadius += vector.getNorm();
        }
        meanRadius /= vectors.size();
        double surfaceArea = meanRadius * meanRadius * 4 * Math.PI;
        int n = (int) (surfaceArea / (samplingLength * samplingLength));

        // Sample points on unit sphere according to Fibonacci scheme
        List<Vector3D> sampledPoints = fibonacciSampling(n);

        // Interpolate cartesian coordinates
        List<double[]> resampledPoints = new ArrayList<>();
        for (Vector3D sampledPoint : sampledPoints) {
            double[] interpolatedPoint = new double[3];
            for (int i = 0; i < 3; i++) {
                interpolatedPoint[i] = interpolateCoordinate(vectors, convertedPoints, sampledPoint, i);
            }
            resampledPoints.add(interpolatedPoint);
        }

        List<Point3f> convertedResampledPoints = new ArrayList<Point3f>();
        for (double[] point : resampledPoints) {
            convertedResampledPoints.add(new Point3f((float)point[0], (float)point[1], (float)point[2]));
        }

        return convertedResampledPoints;
    }

    private static List<Vector3D> fibonacciSampling(int numberOfPoints) {
        List<Vector3D> points = new ArrayList<>();
        double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;

        for (int i = 0; i < numberOfPoints; i++) {
            double theta = 2.0 * Math.PI * i / goldenRatio;
            double phi = Math.acos(1 - 2 * (i + 0.5) / numberOfPoints);
            double x = Math.cos(theta) * Math.sin(phi);
            double y = Math.sin(theta) * Math.sin(phi);
            double z = Math.cos(phi);

            points.add(new Vector3D(x, y, z));
        }

        return points;
    }

    private static double interpolateCoordinate(List<Vector3D> vectors, List<double[]> points, Vector3D sampledPoint, int index) {
        double weightSum = 0;
        double weightedSum = 0;
        for (int i = 0; i < vectors.size(); i++) {
            Vector3D vector = vectors.get(i);
            double dist = vector.distance(sampledPoint);
            double weight = 1 / dist;
            weightSum += weight;
            weightedSum += points.get(i)[index] * weight;
        }
        return weightedSum / weightSum;
    }

}
