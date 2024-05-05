package com.mycompany.imagej;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.complex.Complex;
import org.scijava.vecmath.Point3f;

public class SphericalHarmonicsExpansion {
    private SphericalHarmonicsFunction xFitSph, yFitSph, zFitSph;
    private EllipsoidExpander ellipsoidExpander;
    private int maxDegree;
    private ArrayList<Point3f> points;

    public SphericalHarmonicsExpansion(ArrayList<Point3f> points, int maxDegree) {
        this.points = points;
        this.maxDegree = maxDegree;
        this.ellipsoidExpander = new EllipsoidExpander();
    }

    public double[][] expand() {
        ellipsoidExpander.fit(points);
        double[][] ellipticalCoordinates = ellipsoidExpander.cartesianToElliptical(points, true);

        double[] xValues = new double[points.size()];
        double[] yValues = new double[points.size()];
        double[] zValues = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            xValues[i] = points.get(i).x;
            yValues[i] = points.get(i).y;
            zValues[i] = points.get(i).z;
        }
        
        // fit spherical harmonics for each coordinate 
        double[][] coefficientsX = leastSquaresHarmonicFit(maxDegree, ellipticalCoordinates, xValues);
        double[][] coefficientsY = leastSquaresHarmonicFit(maxDegree, ellipticalCoordinates, yValues);
        double[][] coefficientsZ = leastSquaresHarmonicFit(maxDegree, ellipticalCoordinates, zValues);

        //create spherical harmonics function 
        xFitSph = new SphericalHarmonicsFunction(coefficientsX, maxDegree);
        yFitSph = new SphericalHarmonicsFunction(coefficientsY, maxDegree);
        zFitSph = new SphericalHarmonicsFunction(coefficientsZ, maxDegree);

        // evaluate spherical harmonics functions
        double[][] fittedPoints = new double[points.size()][3];
        for (int i = 0; i < points.size(); i++) {
            double theta = ellipticalCoordinates[i][0];
            double phi = ellipticalCoordinates[i][1];
            fittedPoints[i][0] = xFitSph.eval(theta, phi);
            fittedPoints[i][1] = yFitSph.eval(theta, phi);
            fittedPoints[i][2] = zFitSph.eval(theta, phi);
        }

        return fittedPoints;
    }


    public double[][] leastSquaresHarmonicFit(int fitDegree, double[][] ellipticalCoordinates, double[] values) {
        int numPoints = ellipticalCoordinates.length;
        RealMatrix designMatrix = new Array2DRowRealMatrix(numPoints, (fitDegree + 1) * (fitDegree + 1));
        double[] U = new double[numPoints];
        double[] V = new double[numPoints];

        //extract U and V from sampleLocations
        for (int i = 0; i < numPoints; i++) {
            U[i] = ellipticalCoordinates[i][0];
            V[i] = ellipticalCoordinates[i][1];
        }

        // fill the design matrix with spherical harmonics evaluated at each (U, V)
        int colIndex = 0;
        for (int n = 0; n <= fitDegree; n++) {
            for (int m = -n; m <= n; m++) {
                for (int i = 0; i < numPoints; i++) {
                    Complex ylm = SphericalHarmonicsFunction.sphericalHarmonicY(n, m, U[i], V[i]);
                    designMatrix.setEntry(i, colIndex, m >= 0 ? ylm.getReal() : ylm.getImaginary());
                }
                colIndex++;
            }
        }

        //least squares fitting
        RealVector b = new ArrayRealVector(values);
        DecompositionSolver solver = new QRDecomposition(designMatrix).getSolver();
        RealVector coefficients = solver.solve(b);

        //reshape coefficients into a matrix
        double[][] coefMatrix = new double[fitDegree + 1][2 * fitDegree + 1];
        colIndex = 0;
        for (int n = 0; n <= fitDegree; n++) {
            for (int m = -n; m <= n; m++) {
                int rowIndex = n;
                int colOffset = m + fitDegree;  
                coefMatrix[rowIndex][colOffset] = coefficients.getEntry(colIndex);
                colIndex++;
            }
        }

        return coefMatrix;
    }
    
    public static void testInitialization() { //test for Empty array
        ArrayList<Point3f> points = new ArrayList<>();
        int maxDegree = 5;

        SphericalHarmonicsExpansion she = new SphericalHarmonicsExpansion(points, maxDegree);
        System.out.println("Initialization test passed: " + (she != null));
    }
    
    public static void testEllipsoidFitting() {
        ArrayList<Point3f> points = EllipsoidExpander.generateEllipsoidPoints(1.0, 1.0, 1.0, 100); // Generate points for a unit sphere
        SphericalHarmonicsExpansion she = new SphericalHarmonicsExpansion(points, 5);

        double[][] fitted = she.ellipsoidExpander.fit(points);
        System.out.println("Ellipsoid Fitting Test:");
        System.out.println("Center: " + Arrays.toString(she.ellipsoidExpander.getCenter()));
        System.out.println("Axes: " + Arrays.toString(she.ellipsoidExpander.getAxes()));
    }
    
    public static void testConversionToElliptical() {
        ArrayList<Point3f> points = EllipsoidExpander.generateEllipsoidPoints(1.0, 1.0, 1.0, 100); // Use a unit sphere for simplicity
        SphericalHarmonicsExpansion she = new SphericalHarmonicsExpansion(points, 5);
        she.ellipsoidExpander.fit(points);

        double[][] elliptical = she.ellipsoidExpander.cartesianToElliptical(points, true);
        System.out.println("Conversion to Elliptical Coordinates Test:");
        for (double[] coords : elliptical) {
            System.out.println("U: " + coords[0] + ", V: " + coords[1]);
        }
    }
    
    public static void testCompleteExpansion() {
        ArrayList<Point3f> points = EllipsoidExpander.generateEllipsoidPoints(1.0, 1.0, 1.0, 100); // Simple sphere
        SphericalHarmonicsExpansion she = new SphericalHarmonicsExpansion(points, 5);

        double[][] results = she.expand();
        System.out.println("Complete Expansion Test:");
        for (double[] result : results) {
            System.out.println("Fitted Point: X=" + result[0] + " Y=" + result[1] + " Z=" + result[2]);
        }
    }
    
    
    public static void main(String[] args) {
        testInitialization();
        testEllipsoidFitting();
        testConversionToElliptical();
        testCompleteExpansion();
    }

}