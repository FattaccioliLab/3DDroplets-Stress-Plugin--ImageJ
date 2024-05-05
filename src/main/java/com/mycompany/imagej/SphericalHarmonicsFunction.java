package com.mycompany.imagej;

import org.apache.commons.math3.complex.Complex;

public class SphericalHarmonicsFunction {
	private double[][] sphCoefficients;
    private int sphDegree;

    public SphericalHarmonicsFunction(double[][] coefficients, int degree) {
        this.sphCoefficients = coefficients;
        this.sphDegree = degree;
    }

    public double eval(double theta, double phi) {
        double sphValue = 0;
        for (int l = 0; l <= sphDegree; l++) {
            for (int m = -l; m <= l; m++) {
                int index = m >= 0 ? m : -m;
                Complex ylm = sphericalHarmonicY(l, m, theta, phi);
                if (m >= 0) {
                    sphValue += sphCoefficients[l][index] * ylm.getReal();  
                } else {
                    sphValue += sphCoefficients[l][index] * ylm.getImaginary();
                }
            }
        }
        return sphValue;
    }
    
    
    
    public static double legendreP(int l, double x) {
        if (l == 0) return 1;
        if (l == 1) return x;
        return ((2 * l - 1) * x * legendreP(l - 1, x) - (l - 1) * legendreP(l - 2, x)) / l;
    }

    public static double legendreP(int l, int m, double x) {
        boolean negativeM = m < 0;
        if (negativeM) {
            m = -m;
        }

        if (m > l) {
            throw new IllegalArgumentException("Invalid values of l or m: m cannot be greater than l.");
        }
        if (m == 0) return legendreP(l, x);  //cas de base pour m = 0

        double pmm = 1.0;
        double somx2 = Math.sqrt((1.0 - x) * (1.0 + x));
        double fact = 1.0;
        for (int i = 1; i <= m; i++) {
            pmm *= (-fact) * somx2;
            fact += 2.0;
        }
        if (l == m) return pmm;

        double pmmp1 = x * (2 * m + 1) * pmm;
        if (l == m + 1) return pmmp1;

        double pll = 0;
        for (int ll = m + 2; ll <= l; ++ll) {
            pll = ((2 * ll - 1) * x * pmmp1 - (ll + m - 1) * pmm) / (ll - m);
            pmm = pmmp1;
            pmmp1 = pll;
        }

        if (negativeM) {
            double sign = (m % 2 == 0) ? 1 : -1;
            pll *= sign * factorial(l - m) / factorial(l + m);
        }

        return pll;
    }

    
    public static Complex sphericalHarmonicY(int l, int m, double theta, double phi) {

    	double legendreValue = legendreP(l, m, Math.cos(theta));
        double normalization = Math.sqrt((2 * l + 1) / (4 * Math.PI) * factorial(l - m) / factorial(l + m));

        double realPart = Math.cos(m * phi);
        double imagPart = Math.sin(m * phi);

        return new Complex(normalization * legendreValue * realPart, normalization * legendreValue * imagPart);
    }
    
    public static double factorial(int n) {
        double result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
