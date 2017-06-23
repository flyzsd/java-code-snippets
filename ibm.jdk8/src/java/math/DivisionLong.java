/*
 *  2014-07-22: this file was modified by International Business Machines Corporation.
 *  Modifications Copyright 2014 IBM Corporation.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.math;

import java.util.Arrays;

import com.ibm.Compiler.Internal.Quad;

class DivisionLong {

    /**
     * Performs modular exponentiation using the Montgomery Reduction. It
     * requires that all parameters be positive and the modulus be odd. >
     *
     * @see BigInteger#modPow(BigInteger, BigInteger)
     * @see #monPro(BigInteger, BigInteger, BigInteger, long)
     * @see #slidingWindow(BigInteger, BigInteger, BigInteger, BigInteger,
     *                      long)
     * @see #squareAndMultiply(BigInteger, BigInteger, BigInteger, BigInteger,
     *                      long)
     */
    static BigInteger oddModPow(BigInteger a, BigInteger e, BigInteger m) {
        // PRE: (a > 0), (e > 0), (m > 0) and (odd m), returns (a ^ e) (mod m)
        int k = (((m.numberLength+1) >> 1) << 6); // r = 2^k
        // n-residue of base [base * r (mod modulus)]
        BigInteger abar = a.shiftLeft(k).mod(m);
        // n-residue of base [1 * r (mod modulus)]
        BigInteger xbar = BigInteger.ZERO.setBit(k).mod(m);

        int[] r = oddModPow(a.digits, e.digits, m.digits, xbar.digits, abar.digits);
        BigInteger rBig = new BigInteger(1, r);
        return rBig;
    }

    static int[] oddModPow(int[] a, int[] e, int[] m, int[] xbar, int[] abar) {
        long[] al = arraycopyItoL(a);
        long[] el = arraycopyItoL(e);
        long[] ml = arraycopyItoL(m);
        long[] xbarl = arraycopyItoL(xbar);
        long[] abarl = arraycopyItoL(abar);
        long[] rl = oddModPow(al, el, ml, xbarl, abarl);
        int[] r = arraycopyLtoI(rl);
        return r;
    }

    static long[] oddModPow(long[] a, long[] e, long[] m, long[] xbar, long[] abar) {
        long m0 = m[0];
        long mp0 = 1L; // this is m'[0] = - m[0] ^(-1) (mod 2^w), where w = word size = 64.
        long powerOfTwo = 2L;
        // compute m'[0] by Dusse & Kaliski, A cryptography library for the Motorola DSP56000, EUROCRYPT '90.
        do {
            if (((m0 * mp0) & powerOfTwo) != 0) {
                mp0 |= powerOfTwo;
            }
            powerOfTwo <<= 1;
        } while (powerOfTwo != 0L);
        mp0 = -mp0;

        long[] res;
        int s = numberLength(m);
        if (s == 1) {
            res = squareAndMultiply(xbar, abar, e, m, s, mp0);
        } else {
            res = slidingWindow(xbar, abar, e, m, s, mp0);
        }
        long[] one = {1L};
        res = monPro(res, one, m, s, mp0);
        return res;
    }

    static long[] squareAndMultiply(long[] xbar, long[] abar, long[] e, long[] m, int s, long mp0) {
        for (int i = bitLength(e) - 1; i >= 0; i--) {
            xbar = monPro(xbar, xbar, m, s, mp0);
            if (testBit(e, i)) {
                xbar = monPro(xbar, abar, m, s, mp0);
            }
        }
        return xbar;
    }

    /*Implements the Montgomery modular exponentiation based in <i>The sliding windows algorithm and the Mongomery
     *Reduction</i>.
     *@ar.org.fitc.ref "A. Menezes, P. van Oorschot, S. Vanstone - Handbook of Applied Cryptography";
     *@see #oddModPow(BigInteger, BigInteger,
     *                           BigInteger)
     */
    static long[] slidingWindow(long[] xbar, long[] abar, long[] e, long[] m, int s, long mp0) {
        // fill odd low pows of abar
        long[][] pows = new long[8][];
        int lowexp;
        long[] x3;
        int acc3;
        pows[0] = abar;

        x3 = monSquare(abar, m, s, mp0);

        // do not need to calculate even powers
        for (int i = 1; i <= 7; i++) {
            pows[i] = monPro(pows[i-1], x3, m, s, mp0);
        }

        for (int i = bitLength(e)-1; i >= 0; i--) {
            if (testBit(e, i)) {
                lowexp = 1;
                acc3 = i;

                for (int j = Math.max(i-3, 0); j <= i-1; j++) {
                    if (testBit(e, j)) {
                        if (j < acc3) {
                            acc3 = j;
                            lowexp = (lowexp << (i-j))^1;
                        } else {
                            lowexp = lowexp ^ (1 << (j-acc3));
                        }
                    }
                }

                for (int j = acc3; j <= i; j++) {
                    xbar = monSquare(xbar, m, s, mp0);
                }
                xbar = monPro((pows[(lowexp-1) >> 1]), xbar, m, s, mp0);
                i = acc3;
            } else {
                xbar = monSquare(xbar, m, s, mp0);
            }
        }
        return xbar;
    }

    /** Implements the Montgomery Square of a BigInteger.
     * @see #monPro(BigInteger, BigInteger, BigInteger,
     * long)
     */
    static long[] monSquare(long[] a, long[] m, int s, long mp0) {
        if (s == 1) {
            return monPro(a, a, m, s, mp0);
        }
        //Squaring
        long[]t = new long[(s << 1) + 1];
        int limit = Math.min(s, numberLength(a));

        //Multiplying...
        monMulSq(a, limit, t);

        //Reducing...
        /* t + m*n */
        monReduceSq(a, m, mp0, s, t);

        /* t / r  */
        for (int j = 0; j < s+1; j++) {
            t[j] = t[j+s];
        }
        /*step 3*/
        return finalSubtraction(t, s, m, s);
    }

    static void monMulSq(long[] a, int limit, long[] t) {
        for (int i = 0; i < limit; i++) {
            long cs = 0L;
            long ai = a[i];
            for (int j = i+1; j < limit; j++) {
                Quad z = Quad.mul(a[j], ai);
                z = Quad.add(z, t[i+j]);
                z = Quad.add(z, cs);
                t[i+j] = Quad.lo(z);
                cs = Quad.hi(z);
            }
            t[i+limit] = cs;
        }
        shiftLeft(t, t, 0, 1);

        long cs = 0L;
        for (int i=0, index = 0; i < limit; i++, index++) {
            {
                Quad z = Quad.mul(a[i], a[i]);
                z = Quad.add(z, t[index]);
                z = Quad.add(z, cs);
                t[index] = Quad.lo(z);
                cs = Quad.hi(z);
            }
            index++;
            {
                Quad z = Quad.add(t[index], cs);
                t[index] = Quad.lo(z);
                cs = Quad.hi(z);
            }
        }
    }

    static void monReduceSq(long[] a, long[] m, long mp0, int s, long[] t) {
        /* t + m*n */
        long carry = 0L;
        for (int i = 0; i < s; i++) {
            long cs = 0L;
            long p;
            {
                Quad z = Quad.mul(t[i], mp0);
                p = Quad.lo(z);
            }
            for (int j = 0; j < s; j++){
                Quad z = Quad.mul(p, m[j]);
                z = Quad.add(z, t[i+j]);
                z = Quad.add(z, cs);
                t[i+j] = Quad.lo(z);
                cs = Quad.hi(z);
            }
            //Adding C to the result
            {
                Quad z = Quad.add(carry, t[i+s]);
                z = Quad.add(z, cs);
                t[i+s] = Quad.lo(z);
                carry = Quad.hi(z);
            }
        }
        t[s<<1] = carry;
    }

    /**
     * Implements the Montgomery Product of two integers represented by
     * {@code int} arrays. The arrays are supposed in <i>little
     * endian</i> notation.
     *
     * @param a The first factor of the product.
     * @param b The second factor of the product.
     * @param modulus The modulus of the operations. Z<sub>modulus</sub>.
     * @param n2 The digit modulus'[0].
     * @ar.org.fitc.ref "C. K. Koc - Analyzing and Comparing Montgomery
     *                  Multiplication Algorithms"
     * @see #modPowOdd(BigInteger, BigInteger, BigInteger)
     */
    static long[] monPro(long[] a, long[] b, long[] m, int s, long mp0) {
        int aFirst = numberLength(a) - 1;
        int bFirst = numberLength(b) - 1;

        long p;
        int i, j;
        long[] t = new long[(s << 1) + 1];
        long C;
        long aI;

        for (i = 0; i < s; i++) {
            C = 0L;
            aI = (i > aFirst) ? 0 : a[i];
            for (j = 0; j < s; j++) {
                long bJ = (j > bFirst) ? 0 : b[j];
                Quad z = Quad.mul(bJ, aI);
                z = Quad.add(z, t[j]);
                z = Quad.add(z, C);
                t[j] = Quad.lo(z);
                C = Quad.hi(z);
            }
            {
                Quad z = Quad.add(t[s], C);
                t[s] = Quad.lo(z);
                t[s + 1] = Quad.hi(z);
            }
            {
                Quad z = Quad.mul(t[0], mp0);
                p = Quad.lo(z);
            }
            {
                Quad z = Quad.mul(p, m[0]);
                z = Quad.add(z, t[0]);
                C = Quad.hi(z);
            }
            for (j = 1; j < s; j++) {
                Quad z = Quad.mul(p, m[j]);
                z = Quad.add(z, t[j]);
                z = Quad.add(z, C);
                t[j - 1] = Quad.lo(z);
                C = Quad.hi(z);
            }
            {
                Quad z = Quad.add(t[s], C);
                t[s - 1] = Quad.lo(z);
                C = Quad.hi(z);
            }
            t[s] = t[s + 1] + C;
        }

        return finalSubtraction(t, t.length-1, m, s);

    }
    /*Performs the final reduction of the Montgomery algorithm.
     *@see monPro(BigInteger, BigInteger, BigInteger,
     *long )
     *@see monSquare(BigInteger, BigInteger ,
     *long)
     */
    static long[] finalSubtraction(long[] t, int tLength, long[] m, int s) {
        // skipping leading zeros
        int i;
        boolean lower = false;

        for (i = tLength; (i > 0) && (t[i] == 0); i--)
            ;

        if (i == s - 1) {
            for (; (i >= 0) && (t[i] == m[i]); i--)
                ;
            lower = (i >= 0) && (lessThan(t[i], m[i]));
        } else {
            lower = (i < s - 1);
        }
        // result is first s+1 words of t: t[0],...,t[s]
        // return inplace by clearing t[s+1],...
        Arrays.fill(t, s+1, t.length, 0);

        // if (t >= n) compute (t - n)
        if (!lower) {
            inplaceSubtract(t, numberLength(t), m, s);
        }
        //res.cutOffLeadingZeroes();
        return t;
    }

    // unsigned less than
    static boolean lessThan(long a, long b) {
        if ((a >= 0L) && (b >= 0L)) {
            return a < b;
        } else if ((a < 0L) && (b < 0L)) {
            return a < b;
        } else if (a >= 0L) /* && (b < 0) */ {
            return true;
        } else /* if (a < 0) && (b >= 0) */ {
            return false;
        }
    }
    static void inplaceSubtract(long[] a, int aNumberLength, long[] b, int bNumberLength) {
        // PRE: op1 >= op2 > 0
        subtract(a, a, aNumberLength, b, bNumberLength);
        // op1.cutOffLeadingZeroes ();
    }

    /**
     * Performs {@code res = a - b}. It is assumed the magnitude of a is not
     * less than the magnitude of b.
     */
    private static void subtract(long[] res, long[] a, int aSize, long[] b, int bSize) {
        // PRE: a[] >= b[]
        int i;
        long borrow = 0L;

        for (i = 0; i < bSize; i++) {
            Quad z = Quad.sub(a[i], borrow);
            z = Quad.sub(z, b[i]);
            res[i] = Quad.lo(z);
            borrow = -Quad.hi(z); // 1 or 0
        }
        for (; i < aSize; i++) {
            Quad z = Quad.sub(a[i], borrow);
            res[i] = Quad.lo(z);
            borrow = -Quad.hi(z); // 1 or 0
        }
    }

    /**
     * Abstractly shifts left an array of integers in little endian (i.e. shift
     * it right). Total shift distance in bits is longCount * 64 + count
     *
     * @param result the destination array
     * @param source the source array
     * @param longCount the shift distance in longs
     * @param count an additional shift distance in bits
     */
    static void shiftLeft(long[] result, long[] source, int longCount, int count) {
        if (count == 0) {
            System.arraycopy(source, 0, result, longCount, result.length - longCount);
        } else {
            int rightShiftCount = 64 - count;

            result[result.length - 1] = 0L;
            for (int i = result.length - 1; i > longCount; i--) {
                result[i] |= source[i - longCount - 1] >>> rightShiftCount;
                result[i - 1] = source[i - longCount - 1] << count;
            }
        }
        for (int i = 0; i < longCount; i++) {
            result[i] = 0L;
        }
    }

    static boolean testBit(long[] x, int n) {
        // PRE: 0 <= n < val.bitLength()
        return ((x[n >> 6] & (1L << (n & 63))) != 0L);
    }

    static int bitLength(long[] a) {
        int numberLength = numberLength(a);
        int bLength = (numberLength << 6);
        long highDigit = a[numberLength-1];
        // Subtracting all sign bits
        bLength -= Long.numberOfLeadingZeros(highDigit);
        return bLength;
    }

    final static int numberLength(long[] a) {
        int numberLength = a.length;
        while ((numberLength > 0) && (a[--numberLength] == 0)) {}
        numberLength++;
        return numberLength;
    }

    static long[] arraycopyItoL(int[] x) {
        int nx = numberLengthI(x);
        long[] xl = new long[(nx+1)>>1];
        arraycopyItoL(x, 0, xl, 0, nx);
        return xl;
    }

    /**
     * Copies the contents of x starting at offset sx into y starting at offset sy for length elements.
     *
     * Parameters:
     * @param x the int array to copy out of
     * @param sx the starting index in array1
     * @param y the long array to copy into
     * @param sy the starting index in array2
     * @param length the number of elements in the int array to copy
     */
    static void arraycopyItoL(int[] x, int sx, long[] y, int sy, int length) {
        int i, j;
        for (i=sx, j=sy; i < sx+length-1; i += 2, j++) {
            y[j] = (x[i] & 0xFFFFFFFFL) + ((x[i+1] & 0xFFFFFFFFL) << 32);
        }
        if (i < sx+length) {
            // odd number of ints
            y[j] = (x[i] & 0xFFFFFFFFL);
        }
    }

    static int[] arraycopyLtoI(long[] xl) {
        int nx = 2*numberLength(xl);
        int[] x = new int[nx];
        arraycopyLtoI(xl, 0, x, 0, nx);
        return x;
    }

    /**
     * Copies the contents of x starting at offset sx into y starting at offset sy for length elements.
     *
     * Parameters:
     * @param x the long array to copy out of
     * @param sx the starting index in array1
     * @param y the int array to copy into
     * @param sy the starting index in array2
     * @param length the number of elements in the int array to copy into
     */
    static void arraycopyLtoI(long[] x, int sx, int[] y, int sy, int ly) {
        int i, j;
        int lx = ly >> 1;
        for (i=sx, j=sy; i < sx+lx; i++, j += 2) {
            y[j]   = (int) x[i];
            y[j+1] = (int) (x[i] >>> 32);
        }
        if ((ly & 1) == 1)
            y[j] = (int) x[i];
        // ignore high int of y[j] as it is beyond length of y
//      int hi = (int) (x[i] >>> 32);
    }

//    static public boolean equals(dks.java.math.BigInteger a, java.math.BigInteger b) {
//      return equalsArrays(a.digits, b.digits);
//    }

    static boolean equalsArrays(final int[] a, final int[] b) {
        int i;
        if (numberLengthI(a) != numberLengthI(b)) {
            return false;
        }
        for (i = numberLengthI(a) - 1; (i >= 0) && (a[i] == b[i]); i--) {
            // Empty
        }
        return i < 0;
    }

    final static int numberLengthI(int[] a) {
        int n = a.length;
        while ((n > 0) && (a[--n] == 0)) {}
        n++;
        return n;
    }
}
