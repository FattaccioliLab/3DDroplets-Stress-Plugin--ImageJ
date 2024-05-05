package com.mycompany.imagej;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;

import org.scijava.vecmath.Point3f;


public class EllipsoidExpander {

    private double[] center;
    private double[] axes;
    private double[][] eigenvectors;
    private double[] coefficients;

    public EllipsoidExpander() {
    }
    
    public double[] getCenter() {
    	return center;
    }
    
    public double[] getAxes() {
    	return axes;
    }
    
    public double[][] getEigenvectors(){
    	return eigenvectors;
    }
    
    public void setCenter(double center[]) {
    	this.center = center;
    }
    
    public void setAxes(double axes[]) {
    	this.axes = axes;
    }
    
    public void setEigenvectors(double eigenvectors[][]){
    	this.eigenvectors = eigenvectors;
    }
    
    public double[] getCoefficients() {
    	return coefficients;
    }

    

    public double[][] fit(ArrayList<Point3f> points) {
        coefficients = fitEllipsoidToPoints(points);
        extractCharacteristics(coefficients);
        return generateOutput();
    }

    private double[] fitEllipsoidToPoints(List<Point3f> points) {
        RealMatrix designMatrix = new Array2DRowRealMatrix(points.size(), 9);
        int j = 0;
        for (Point3f point: points) {
            double x = point.getX();
            double y = point.getY();
            double z = point.getZ();
            designMatrix.setRow(j, new double[]{x*x, y*y, z*z, x*y, x*z, y*z, x, y, z});
            j++;
        }
        
        double[] ones = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            ones[i] = 1.0;
        }
        RealMatrix columnOfOnes = new Array2DRowRealMatrix(ones);

        QRDecomposition qr = new QRDecomposition(designMatrix.transpose().multiply(designMatrix));
        DecompositionSolver solver = qr.getSolver();
        RealMatrix coefficients = solver.solve(designMatrix.transpose().multiply(columnOfOnes));
        return MatrixUtils.createRealVector(coefficients.getColumn(0)).append(-1).toArray();
    }

    private void extractCharacteristics(double[] coefficients) {
        RealMatrix Amat = new Array2DRowRealMatrix(new double[][] {
            {coefficients[0], coefficients[3] / 2.0, coefficients[4] / 2.0, coefficients[6] / 2.0},
            {coefficients[3] / 2.0, coefficients[1], coefficients[5] / 2.0, coefficients[7] / 2.0},
            {coefficients[4] / 2.0, coefficients[5] / 2.0, coefficients[2], coefficients[8] / 2.0},
            {coefficients[6] / 2.0, coefficients[7] / 2.0, coefficients[8] / 2.0, -1}
        });

        RealMatrix A3 = Amat.getSubMatrix(0, 2, 0, 2);
        RealMatrix A3inv = MatrixUtils.inverse(A3);
        double[] ofs = new double[]{coefficients[6] / 2.0, coefficients[7] / 2.0, coefficients[8] / 2.0};
        center = A3inv.preMultiply(MatrixUtils.createRealVector(ofs).mapMultiply(-1.0)).toArray();

        RealMatrix Tofs = MatrixUtils.createRealIdentityMatrix(4);
        for (int i = 0; i < 3; i++) {
            Tofs.setEntry(3, i, center[i]);
        }

        RealMatrix R = Tofs.multiply(Amat).multiply(Tofs.transpose());
        RealMatrix R3 = R.getSubMatrix(0, 2, 0, 2);
        EigenDecomposition eigen = new EigenDecomposition(R3.scalarMultiply(1.0 / -R.getEntry(3, 3)));
        axes = eigen.getRealEigenvalues();
        for (int i = 0; i < axes.length; i++) {
            axes[i] = Math.sqrt(1.0 / Math.abs(axes[i]));
        }
        eigenvectors = eigen.getV().getData();
    }

    private double[][] generateOutput() {
        double[][] vectors = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                vectors[i][j] = eigenvectors[j][i] * axes[i];
            }
        }
        return vectors;
    }
    
    
    public static ArrayList<Point3f> generateEllipsoidPoints(double a, double b, double c, int n) {
    	ArrayList<Point3f> points = new ArrayList<Point3f>();
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            double theta = Math.PI * 2 * rand.nextDouble();
            double phi = Math.PI * rand.nextDouble() - Math.PI / 2;
            double x = a * Math.cos(theta) * Math.cos(phi);
            double y = b * Math.sin(theta) * Math.cos(phi);
            double z = c * Math.sin(phi);
            points.add(new Point3f((float)x,(float)y,(float)z));
        }
        return points;
    }
    
    public double[][] cartesianToElliptical(ArrayList<Point3f> points, boolean invert) {
        double[] lengths = new double[axes.length];
        for (int i = 0; i < axes.length; i++) {
            lengths[i] = Math.sqrt(axes[i]);
        }

        RealMatrix R = MatrixUtils.createRealMatrix(eigenvectors);
        if (invert) {
            R = R.transpose();
        }
        
        int pointsSize = points.size();

        RealMatrix R_inverse = new LUDecomposition(R).getSolver().getInverse();
        double[][] U_coors_calc = new double[pointsSize][1];
        double[][] V_coors_calc = new double[pointsSize][1];
        
        double[][] results = new double[points.size()][2];
        
        int pt_numb = 0;
        for (Point3f point: points) {
            RealVector pointVector = MatrixUtils.createRealVector(new double[] {point.getX(), point.getY(), point.getY()});
            RealVector centerVector = MatrixUtils.createRealVector(center);
            RealVector y_tilde_pt = R_inverse.preMultiply(pointVector.subtract(centerVector));

            double yt_0 = y_tilde_pt.getEntry(0);
            double yt_1 = y_tilde_pt.getEntry(1);
            double yt_2 = y_tilde_pt.getEntry(2);

            double U_pt = Math.atan2(yt_1 * lengths[0], yt_0 * lengths[1]);
            if (U_pt < 0) {
                U_pt += 2.0 * Math.PI;
            }
            U_coors_calc[pt_numb][0] = U_pt;

            double cylinder_r = Math.sqrt(yt_0 * yt_0 + yt_1 * yt_1);
            double cyl_r_exp = Math.sqrt(
                (lengths[0] * Math.cos(U_pt)) * (lengths[0] * Math.cos(U_pt)) +
                (lengths[1] * Math.sin(U_pt)) * (lengths[1] * Math.sin(U_pt))
            );

            double V_pt = Math.atan2(cylinder_r * lengths[2], yt_2 * cyl_r_exp);
            if (V_pt < 0) {
                V_pt += 2.0 * Math.PI;
            }
            V_coors_calc[pt_numb][0] = V_pt;
            
            results[pt_numb][0] = U_coors_calc[pt_numb][0]; // Assign U coordinates
            results[pt_numb][1] = V_coors_calc[pt_numb][0]; // Assign V coordinates
            
            pt_numb++;
        }

        return results;
    }


    
    
    public static void main(String[] args) {
    	
    	/* test de fit -----------------------*/
        EllipsoidExpander expander = new EllipsoidExpander();

        //génération de points sur un ellipsoïde simple
        double a = 3.0; // demi-axe en x
        double b = 2.0; // demi-axe en y
        double c = 1.0; // demi-axe en z
        int numPoints = 1000; // nombre de points à générer

        ArrayList<Point3f> points = generateEllipsoidPoints(a, b, c, numPoints);

        double[][] fitted = expander.fit(points);

        System.out.println("Centre de l'ellipsoïde : ");
        for (double coord : expander.getCenter()) {
            System.out.print(coord + " ");
        }
        System.out.println();

        System.out.println("Axes de l'ellipsoïde : ");
        for (double axis : expander.getAxes()) {
            System.out.print(axis + " ");
        }
        System.out.println();
        
        System.out.println("Matrice des vecteurs propres (orientation) : ");
        for (double[] vector : expander.getEigenvectors()) {
            for (double component : vector) {
                System.out.print(component + " ");
            }
            System.out.println();
        }
        
    	/* test de cartesianToElliptical -----------------------*/
//        double[][] ellipticalCoordinates = expander.cartesianToElliptical(points, false);
//
//	     // Print elliptical coordinates to visually inspect them
//	     System.out.println("Elliptical Coordinates (U, V):");
//	     for (double[] coord : ellipticalCoordinates) {
//	         System.out.printf("(U: %f, V: %f)\n", coord[0], coord[1]);
//	     }
        
        EllipsoidExpander expander2 = new EllipsoidExpander();
        expander2.setAxes(new double[]{9, 4, 1});
        expander2.setCenter(new double[]{0, 0, 0});
        expander2.setEigenvectors(new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}});

        ArrayList<Point3f> points2 = new ArrayList<Point3f>();
        points2.add(new Point3f(3, 0, 0));
        points2.add(new Point3f(0, 2, 0));
        points2.add(new Point3f(0, 0, 1));

        double[][] ellipticalCoordinates = expander2.cartesianToElliptical(points2, false);

        System.out.printf("(U: %f for x-axis)\n", ellipticalCoordinates[0][0]);
        System.out.printf("(V: %f for x-axis)\n", ellipticalCoordinates[0][1]);
        System.out.printf("(U: %f for y-axis)\n", ellipticalCoordinates[1][0]);
        System.out.printf("(V: %f for y-axis)\n", ellipticalCoordinates[1][1]);
        System.out.printf("(U: %f for z-axis)\n", ellipticalCoordinates[2][0]);
        System.out.printf("(V: %f for z-axis)\n", ellipticalCoordinates[2][1]);

           

    }
    
}

