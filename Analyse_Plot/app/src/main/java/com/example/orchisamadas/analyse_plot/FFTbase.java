package com.example.orchisamadas.analyse_plot;

public class FFTbase {
    public static int fft(final double[] inputReal, double[]inputImag, boolean DIRECT) {
        int n = inputReal.length;
        double ld = Math.log(n) / Math.log(2.0);

        if (((int) ld) - ld != 0) {return -1;}
        int nu = (int) ld; int n2 = n / 2; int nu1 = nu - 1;
        double tReal, tImag, p, arg, c, s;
        double constant;
        // Here I check if I'm going to do the direct transform or the inverse transform.
        if (DIRECT) {constant = -2 * Math.PI;}
        // First phase - calculation
        else {constant = 2 * Math.PI;}
        int k = 0;
        for (int l = 1; l <= nu; l++) {
            while (k < n) {
                for (int i = 1; i <= n2; i++) {
                    p = bitreverseReference(k >> nu1, nu);
                    arg = constant * p / n;
                    c = Math.cos(arg); s = Math.sin(arg);
                    tReal = inputReal[k + n2] * c + inputImag[k + n2] * s;
                    tImag = inputImag[k + n2] * c - inputReal[k + n2] * s;
                    inputReal[k + n2] = inputReal[k] - tReal;
                    inputImag[k + n2] = inputImag[k] - tImag;
                    inputReal[k] += tReal; inputImag[k] += tImag;
                    k++;
                }
                k += n2;
            }
            k = 0; nu1--; n2 /= 2;
        }
        // Second phase - recombination
        k = 0; int r;
        while (k < n) {
            r = bitreverseReference(k, nu);
            if (r > k) {
                tReal = inputReal[k]; tImag = inputImag[k];
                inputReal[k] = inputReal[r]; inputImag[k] =inputImag[r];
                inputReal[r] = tReal; inputImag[r] = tImag;
            }
            k++;
        }
        double radice = 1 / Math.sqrt(n);
        for (int i = 0; i < inputReal.length; i++) {
            inputReal[i] = inputReal[i] * radice;
            inputImag[i] = inputImag[i] * radice;
        }
        return 0;
    }//The reference bitreverse function
    private static int bitreverseReference(int j, int nu) {
        int j2; int j1 = j; int k = 0;
        for (int i = 1; i <= nu; i++) {j2 = j1 / 2; k = 2 * k + j1 - 2 *j2; j1 = j2; }
        return k;
    }
}