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

import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.io.*;

import org.apache.harmony.math.internal.nls.Messages;

/**
 * BigDecimal objects represent an arbitrary precisioned decimal
 * Number. They contain values that cannot be changed.
 * Thus, most operations on the BigDecimal object yield new
 * instances of BigDecimal.
 * <p>
 * BigDecimal is respresented by an unscaled BigInteger value and an integer
 * representing the scale of the object.  The scale of the BigDecimal
 * is the number of digits after the decimal point.  Eg. 1.234 would
 * have a scale of 3 and an unscaled value of 1234.  Therefore,
 * decimal representation of a BigDecimal is BigIntegerValue/10^scale.
 */
public class BigDecimal extends Number implements Serializable, Comparable<BigDecimal> {
   // Used to keep behaviour of DFPHWAvailable consistent between
   // jitted code and interpreted code
   // DO NOT CHANGE OR MOVE THIS LINE
   // IT MUST BE THE FIRST THING IN THE INITIALIZATION
   private static final boolean DFP_HW_AVAILABLE = DFPGetHWAvailable();

   /**
    * Round up constant.
    */
   public static final int ROUND_UP=0;
   /**
    * Round down constant.
    */
   public static final int ROUND_DOWN=1;
   /**
    * Round to positive infinity constant.
    */
   public static final int ROUND_CEILING=2;
   /**
    * Round to negative infinity constant.
    */
   public static final int ROUND_FLOOR=3;
   /**
    * Round constant for round to nearest neighbor unless both neighbors are equidistant, then round up.
    */
   public static final int ROUND_HALF_UP=4;
   /**
    * Round constant for round to nearest neighbor unless both neighbors are equidistant, then round down.
    */
   public static final int ROUND_HALF_DOWN=5;
   /**
    * Round constant for round to nearest neighbor unless both neighbors are equidistant, then round to the even neighbor.
    */
   public static final int ROUND_HALF_EVEN=6;
   /**
    * Round constant that will cause an ArithmeticException if rounding is required.
    */
   public static final int ROUND_UNNECESSARY=7;

   //thread local heap to speed up toString related functionality
   private static final ThreadLocal thLocalToString = new ThreadLocal() {
      /* 
       * 22 is the best number for the fastest path
       * [-]digits.digits when scale > 0 & scale <=19
       */
      protected synchronized Object initialValue() {
            return new char[22];
      }
   };

   /**
    *  LUT for powers of ten
    */
   private static final long [/*19*/]powersOfTenLL = {
      1L, 10L, 100L, 1000L, /*0 to 4 */
      10000L, 100000L, 1000000L, 10000000L, /*5 to 8*/
      100000000L, 1000000000L, 10000000000L, 100000000000L, /*9 to 12*/
      1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L, /*13 to 16 */
      10000000000000000L, 100000000000000000L, 1000000000000000000L /*17 to 18 */
   };

   private static final BigInteger [/*19*/]powersOfTenBI = {
      BigInteger.valueOf(1L), BigInteger.valueOf(10L), BigInteger.valueOf(100L), BigInteger.valueOf(1000L), /*0 to 4 */
      BigInteger.valueOf(10000L), BigInteger.valueOf(100000L), BigInteger.valueOf(1000000L), BigInteger.valueOf(10000000L), /*5 to 8*/
      BigInteger.valueOf(100000000L), BigInteger.valueOf(1000000000L), BigInteger.valueOf(10000000000L), BigInteger.valueOf(100000000000L), /*9 to BigInteger.valueOf(12*/
      BigInteger.valueOf(1000000000000L), BigInteger.valueOf(10000000000000L), BigInteger.valueOf(100000000000000L), BigInteger.valueOf(1000000000000000L), /*BigInteger.valueOf(13 to BigInteger.valueOf(16 */
      BigInteger.valueOf(10000000000000000L), BigInteger.valueOf(100000000000000000L), BigInteger.valueOf(1000000000000000000L) /*17 to 18 */
   };

   /**
    *  LUT for quick double digit to char - Tens digits
    *  100 elements
    */
   private static final char []doubleDigitsTens = {
      '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',  //0 - 9
      '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',  //10 - 19
      '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',  //20 - 29
      '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',  //30 - 39
      '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',  //40 - 49
      '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',  //50 - 59
      '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',  //60 - 69
      '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',  //70 - 79
      '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',  //80 - 89
      '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'   //90 - 99
   };

   /**
    *  LUT for quick double digit to char - Ones digits
    *  100 elements
    */
   private static final char []doubleDigitsOnes = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //0 - 9
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //10 - 19
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //20 - 29
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //30 - 39
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //40 - 49
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //50 - 59
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //60 - 69
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //70 - 79
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  //80 - 89
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'   //90 - 99
   };

   /**
    * LUT for DFP double combination field - gives us the leftmost
    * digit of the coefficient, as well as the first two bits of the
    * exponent in form:  0xXY where X = LMD, Y = first two bits
    */
   private static byte []doubleDFPComboField = comboinit();

   // LUT for converting DPD to BCD, 1024 elements
   private static short []DPD2BCD = dpd2bcdinit();

   // DFP 0 with exponent 0
   private static final long dfpZERO = 2465720795985346560L;

   private static final BigInteger MAXBYTE = BigInteger.valueOf(Byte.MAX_VALUE);
   private static final BigInteger MINBYTE = BigInteger.valueOf(Byte.MIN_VALUE);
   private static final BigInteger MAXSHORT = BigInteger.valueOf(Short.MAX_VALUE);
   private static final BigInteger MINSHORT = BigInteger.valueOf(Short.MIN_VALUE);
   private static final BigInteger MAXINT = BigInteger.valueOf(Integer.MAX_VALUE);
   private static final BigInteger MININT = BigInteger.valueOf(Integer.MIN_VALUE);
   private static final BigInteger MAXLONG = BigInteger.valueOf(Long.MAX_VALUE);
   private static final BigInteger MINLONG = BigInteger.valueOf(Long.MIN_VALUE);
   private static final BigInteger MAXDFP64 = BigInteger.valueOf(9999999999999999L);
   private static final BigInteger MINDFP64 = BigInteger.valueOf(-9999999999999999L);

   // Initialize these after all statics have been initialized to minimize
   // dependencies on other static initialized variables used in BigDecimal
   // constructors.  Must use constructors (can't use valueOf(long) since
   // valueOf(long) requires these to be initialized.
   
   /** The value of zero. */
   public static final BigDecimal ZERO = new BigDecimal(0);
   /** The value of one. */
   public static final BigDecimal ONE = new BigDecimal(1);
   /** The value of ten. */
   public static final BigDecimal TEN = new BigDecimal(10);

   /* The value of five.  This is private because it's not part of the public API */
   private static final BigDecimal FIVE = new BigDecimal(5);

   // Cache of predefined 0 to 10 values with scales 1 and 2
   private static BigDecimal [] CACHE1;
   private static BigDecimal [] CACHE2;

   // Cache of commonly used "0.00" value returned from toString
   private static String zeroDec = "0.00";

   /* Used for Hysteresis for mixed-representation
    * -----------------------------------------------------------
    * We want the BigDecimal class to choose the best representation
    * for construction and operations.  We start assuming the LL
    * is the best representation.  Over the course of time, using
    * hystersis, we might alter this decision.
    *
    * The constructors are annotated with the checks on deciding
    * which representation to use, and other APIs contribute
    * to biasing towards or away from a representation.
    *
    * NOTE: Hysterisis only works on platforms that supports DFP
    * since we prepend a DFPHWAvailable check before performing
    * mods to the counters, and before basing decisions off them.
    */
   private static final int hys_threshold = 1000; // threshold for representation change
   private static boolean hys_type = false; // false = non-dfp, true = dfp
   private static int hys_counter = 0; // increment for dfp, decrement for non-dfp

   // Slow slow-path
   private BigInteger bi;

   /**
    * Flags will be used to represent:
    *
    *    -the type of internal representation of BigDecimal    (all reps)
    *
    *  -whether we have cached the signum                (DFP only)
    *  -signum cache(-1, 0, 1)                     (DFP only)
    *  -whether we have cached the precision             (all reps)
    *  -precision cache                            (all reps)
    *
    *  -whether we have cached the exponent           (DFP only)
    *
    *
    * use bits 30 and 31 for the representation:
    *  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX[RR]
    *  where:
    *    00 - DFP (decimal floating point)
    *    01 - LL (long lookaside)
    *    10 - BI (BigInteger)
    *
    * use bit 29 to indicate whether the signum is cached (DFP):
    *  XXXXXXXXXXXXXXXXXXXXXXXXXXXXX[S]RR
    * where:
    *    1 - cached
    *
    * use bit 28 to indicate whether the exponent is cached (DFP)
    *  XXXXXXXXXXXXXXXXXXXXXXXXXXXX[E]SRR
    *  where
    *  1 - cached
    *
    * use bit 27 to indicate whether the precision is cached (all reps)
    *  XXXXXXXXXXXXXXXXXXXXXXXXXXX[P]ESRR
    *  where:
    *  1 - cached
    *
    * use bits 25 to 26 to cache the signum (DFP):
    *  XXXXXXXXXXXXXXXXXXXXXXXXX[SG]PESRR
    *  where:
    *
    *  00 - iszero
    *  01 - ispos
    *  11 - isneg
    *
    * use bits 0 to 24 to store the precision (all reps):
    *  [XXXXXXXXXXXXXXXXXXXXXXXXX]SGPESRR
    * where:
    *    minimum precision = 0
    *  maximum precision (25 bits worth) = 33554431 (0xFFFFFF80)
    *
    * NOTE:  For BigIntegers with precision greater than the
    * maximum, we are forced to call toString.length().
    *
    * Note that we've put the fast-path bits (i.e. DFP) in the lowest
    * order bits so that we can have fast access to them (i.e.
    * halfword ANDs/ORs etc)
    */
   private transient int flags;

   /*
    * We use the laside to store every long, except for Long.MIN_VALUE
    * which get's stored as a BigInteger.  This is also overloaded to
    * store the DFP value when in DFP mode
    */ 
   private transient long laside;

   /*
    * cached negated scale used for all 3 representations
    */
   private transient int cachedScale;

   // Same serialization ID as BigDecimal 1.5.0
   private static final long serialVersionUID = 6108874887143696463L;

   private static final ObjectStreamField[] serialPersistentFields = {
     new ObjectStreamField("scale", Integer.TYPE),
     new ObjectStreamField("intVal", BigInteger.class)
   };

   // static initializer must appear here, since the field
   // scale needs to be defined before use
   static{
      CACHE1 = new BigDecimal[11];
      CACHE2 = new BigDecimal[11];

      // Cache of commonly used dollar/cents values
      // need to do it this way otherwise on DFP hardware
      // we can't set scale after constructed using int constructor
      for (int i=0; i<CACHE1.length; i++){
         CACHE1[i] = new BigDecimal();
         CACHE1[i].flags |= 0x00000001;
         CACHE1[i].laside=i;
         CACHE1[i].cachedScale=1;
         CACHE2[i] = new BigDecimal();
         CACHE2[i].flags |= 0x00000001;
         CACHE2[i].laside=i;
         CACHE2[i].cachedScale=2;
      }
   }

   private BigDecimal(){
      this.flags=0;
      this.cachedScale=0;
      this.bi = null;
      this.laside=0;
      return;
   }

   /**
    * Constructs a BigDecimal with unscaled value initialized
    * from a BigInteger.
    *
    * @param bi the BigInteger value to convert to a BigDecimal
    */
   public BigDecimal(BigInteger bi){
      this(bi, 0, MathContext.UNLIMITED);
      return;
   }

   /**
    * Constructs a BigDecimal initialized from a BigInteger
    * with the specified scale.
    *
    * @param bi the BigInteger value to convert to a BigDecimal
    * @param scale the scale used for the result
    */
   public BigDecimal(BigInteger bi,int scale){
      this(bi, scale, MathContext.UNLIMITED);
   }

   /**
    * Constructs a BigDecimal with unscaled value initialized
    * from a BigInteger.
    *
    * @param bi the BigInteger value to convert to a BigDecimal
    * @param set defines the precision and rounding behaviour
    */
   public BigDecimal(BigInteger bi, MathContext set){
      this(bi,0,set);
   }

   /**
    * Constructs a BigDecimal initialized from a BigInteger
    * with the specified scale.
    *
    * @param bi the BigInteger value to convert to a BigDecimal
    * @param scale the scale used for the result
    * @param set defines the precision and rounding behaviour
    */
   public BigDecimal(BigInteger bi,int scale, MathContext set){
      super();
      bigIntegerConstructor(bi, scale, set);
   }

   /**
    * Constructs a BigDecimal initialized from a character array.
    *
    * @param inchars the character array to convert to a BigDecimal
    */
   public BigDecimal(char inchars[]){
      this(inchars,0,inchars.length, MathContext.UNLIMITED);
      return;
   }

   /**
    * Constructs a BigDecimal initialized from a character array.
    *
    * @param inchars the character array to convert to a BigDecimal
    * @param offset the index of the first character in the array
    * @param length the number of characters
    */
   public BigDecimal(char inchars[],int offset,int length){
      this(inchars,offset,length,MathContext.UNLIMITED);
      return;
   }

   /**
    * Constructs a BigDecimal initialized from a character array.
    *
    * @param inchars the character array to convert to a BigDecimal
    * @param set defines the precision and rounding behaviour
    */
   public BigDecimal(char inchars[],MathContext set){
      this(inchars,0,inchars.length,set);   // scale is 0
      return;
   }

   /**
    * Constructs a BigDecimal initialized from a character array.
    *
    * @param inchars the character array to convert to a BigDecimal
    * @param offset the index of the first character in the array
    * @param length the number of characters
    * @param set defines the precision and rounding behaviour
    */
   public BigDecimal(char inchars[],int offset,int length, MathContext set){
      super();

      /* handle array out of bounds errors */
      if ((length <= 0) || (length > inchars.length) ||
          (offset < 0 || offset > inchars.length - 1)) {
         bad(inchars); // bad conversion
      }
      
      if (needConversion(inchars, offset, length)) {
         inchars = converToASCII(inchars, offset, length);
      }

      charParser(inchars,offset,length,set,inchars);
      return;
   }

   /**
    * Constructs a BigDecimal with a double value
    * as an arugment.
    *
    * @param num the double value to convert to a BigDecimal
    *
    * @exception  NumberFormatException If the is Infinity, Negative Infinity or NaN.
    */
   public BigDecimal(double num){
      this(num, MathContext.UNLIMITED);
   }

   /**
    * Constructs a BigDecimal with a double value
    * as an arugment.
    *
    * @param num the double value to convert to a BigDecimal
    * @param set defines the precision and rounding behaviour
    *
    * @exception  NumberFormatException If the is Infinity, Negative Infinity or NaN.
    */
   public BigDecimal(double num,MathContext set){
      final int p = 1023 + 52; // the power offset (precision)
      final long signMask = 0x8000000000000000L; // the mask to get the sign of the number
      final long eMask    = 0x7FF0000000000000L; // the mask to get the power bits
      final long fMask    = 0x000FFFFFFFFFFFFFL; // the mask to get the significand bits

      long inputNumberBits = Double.doubleToLongBits (num);
      long sign = (inputNumberBits & signMask) >> 63; // the value of the sign--0 is positive, 1 is negative
      long e    = (long)((inputNumberBits & eMask) >> 52);  // the value of the 'exponent bigs' of the val
      long f    = (long)(inputNumberBits & fMask);    // the value of the 'mantissa bits' of the val
      int pow = 0;

      // signum bits  - [SG]P ESRR
      if (sign == 0)
         sign=1;
      else
         sign=-1;

      if (e == 2047) {
         //number is infinity or not a number
         // math.38 = can't convert infinity or NaN
         throw new NumberFormatException(Messages.getString("math.38")); //$NON-NLS-1$
      }

      if (e == 0) {
         if (f == 0){

            //quick path for 0e0
            if (DFPHWAvailable() && DFPUseDFP()){
               DFPConstructZero();
               return;
            }

            // otherwise, let's create it as a long
            this.laside = 0L;

            // set LL representation
            this.flags |= 0x00000001;

            // store the precision of 1
            this.flags |= 0x10; //cache on
            this.flags |=  1 << 7; //precision of 1

            // exponent remains 0
            return;
         }else
            pow = (int)(e - p + 1); // a denormalized number, modify power
         //so that the decimal point is to the right of the mantissa
      } else {
         // 0 < e < 2047
         // a "normalized" number
         f = f | 0x0010000000000000L; //put in the 1 in front of the .
         pow = (int)(e - p); //again, move decimal point to right of mantissa
         //but no need to add 1, since normalized
      }

      /* Now we try and decipher the real value of the scale
       *
       * We can do this all in a long if the the following criteria is met
       *     if pow is greater or equal to -27 and less then or equal to 62
       * Otherwise it won't fit in a long
       *
       * The idea here is that -5^27 and 2^62 are the highest multipliers
       * you'll be able to handle before you definitely overflow.
       */

      // normalize the mantissa, by getting rid of trailing 0s
      while((f & 1L) == 0) {
            f >>= 1;
            pow++;
        }

      BigInteger bi=null;
      long lon = sign* f;
      long multval = 1L;
      if (pow < 0 && pow > -27) {       // floor(ln(abs(Long.MIN_VALUE))/ln(5)) = 27
         for (int i=pow; i<0; i++) {
             multval *= 5L;
         }
         // manually inline overflowMultiply here
         long laside = lon;
         if (laside < 0) laside *= -1;
         int m = Long.numberOfLeadingZeros(laside)-1;
         int n = Long.numberOfLeadingZeros(multval)-1;
         if (m+n>=63)  //if (!overflowMultiply(lon, multval))
            lon *= multval;
         else
            bi = BigInteger.valueOf(lon).multiply(BigInteger.valueOf(multval));
        }
      else if (pow > 0 && pow < 63) { 
         multval <<= pow;             

         // manually inline overflowMultiply here
         long laside = lon;
         if (laside < 0) laside *= -1;
         int m = Long.numberOfLeadingZeros(laside)-1;
         int n = Long.numberOfLeadingZeros(multval)-1;
         if (m+n>=63) //if (!overflowMultiply(lon, multval))
               lon *= multval;
         else
            bi = BigInteger.valueOf(lon).multiply(BigInteger.valueOf(multval));
         pow=0;
      }
      else if (pow != 0) {

         bi = BigInteger.valueOf(lon);
         if (pow < 0){
            // convert m*^2e to x=(m*5^-e)*10^e and isolate m*5^-e
            // and isolate m*5^-e
            bi = bi.multiply(BigInteger.valueOf(5).pow(-pow));
         }
         //otherwise, we have to decrease the power (i.e. pad with zeros)
         else if (pow > 0){
            bi = bi.shiftLeft(pow);
            pow=0;
         }
      }

      if (bi == null) { // LL path
         longConstructor(lon, -pow, set);
      }
      else {
         bigIntegerConstructor(bi, -pow, set);
      }
   }

   /**
    * Constructs a BigDecimal with unscaled value initialized
    * from an int.
    *
    * @param num the value to convert to a BigDecimal
    */
   public BigDecimal(int num){
      this(num, MathContext.UNLIMITED);
   }
   
   /**
    * Constructs a BigDecimal with unscaled value initialized
    * from an int.
    *
    * @param num the value to convert to a BigDecimal
    * @param set defines the precision and rounding behaviour
    */
   public BigDecimal(int num, MathContext set){

      if (DFPHWAvailable() && DFPUseDFP()){
         if(DFPIntConstructorHelper(num, set)) {
            return;
         }
      }

      // we can fit this beast into a long lookaside, no need for BigInteger

      // store LL indicator
      this.flags |= 0x00000001;

      // store the long lookaside
      this.laside = num;

      // cache the precision
      this.flags|=0x10; //cache on
      this.flags |= numDigits(num) << 7;

      //exp remains zero

      if (set != MathContext.UNLIMITED)
         this.finish(set.getPrecision(), set.getRoundingMode().ordinal());
   }

   /**
    * Constructs a BigDecimal with unscaled value initialized
    * from a long.
    *
    * @param num the value to convert to a BigDecimal
    */
   public BigDecimal(long num){
      this(num, MathContext.UNLIMITED);
   }

   /**
    * Constructs a BigDecimal with unscaled value initialized
    * from a long.
    *
    * @param num the value to convert to a BigDecimal
    * @param set defines the precision and rounding behaviour
    */
   public BigDecimal(long num,MathContext set){
      super();
      longConstructor(num,0,set);
   }

   /**
    * Constructs a BigDecimal from a string, which can only contain digits of 0-9, a
    * decimal point and a negative sign, or represents the scientific notation
    * (for example "-123e+45").
    *
    * @param string the String value to convert to a BigDecimal
    *
    * @exception  NumberFormatException If the argument contained characters other than digits.
    */
   public BigDecimal(String string){
      this(string,MathContext.UNLIMITED);
      return;
   }

   /**
    * Constructs a BigDecimal from a string, which can only contain digits of 0-9, a
    * decimal point and a negative sign, or represents the scientific notation
    * (for example "-123e+45").
    *
    * @param string the String value to convert to a BigDecimal
    * @param set defines the precision and rounding behaviour
    *
    * @exception  NumberFormatException If the argument contained characters other than digits.
    */
   public BigDecimal(String string, MathContext set) {
      super();
      char[] charArr = string.toCharArray();
      // check to make sure we don't need conversion
      if (needConversion(charArr, 0, charArr.length)) {
         charArr = converToASCII(charArr, 0, charArr.length);
      }
      charParser(charArr, 0, charArr.length, set, string.toCharArray());
      return; // scale is 0
   }

   // helper to convert input char to ascii characters
   private final char[] converToASCII(char[] charArr, int offset, int len) {
      for (int i = offset; i < len; i++) {
         if (Character.isDigit(charArr[i])) {
            int digit = Character.digit(charArr[i], 10);
            charArr[i] = (char) ((digit) + '0');
         }
      }
      return charArr;
   }

   // check to see if we need to convert input string
   private final boolean needConversion(char[] charArr, int offset, int len) {
      for (int i = offset; i < len; ++i) {
         // if char codepoint is > 255, we are using non ASCII characters
         if (Character.codePointAt(charArr, i) > 0xFF) {
            return true;
         }
      }
      return false;
   }
 
   private final void longConstructor(long num, int scale, MathContext set){

      if (DFPHWAvailable() && DFPUseDFP()){
         DFPLongConstructorHelper(num, scale, set);
      }
      else{ // try LL or BI form
         if (num != Long.MIN_VALUE){

            // store the number
            this.laside = num;

            /* cache: SGP ESRR */

            //set precision
            this.flags|=0x10; //cache on
            this.flags |= numDigits(num) << 7;

            // set LL rep
            this.flags |= 0x00000001 /* isLL */;

            this.cachedScale= scale;
         }
         else{

            /*
             * we use this path if
             * long == Long.MIN_VALUE since we can't represent it in LL
             */

            //set BigInteger representation
            this.flags |= 0x00000002 /* isBigInteger */;
            this.bi = BigInteger.valueOf(num);

            /* cache: SGP ESRR */

            //set precision
            this.flags|=0x10; //cache on
            this.flags |= 19 << 7;

            this.cachedScale= scale;
         }
      }

      if (set != MathContext.UNLIMITED)
         this.finish(set.getPrecision(), set.getRoundingMode().ordinal());
   }

   private final void DFPPerformHysteresis(int bias) {
      int sum = hys_counter + bias;
      hys_counter += bias & ~(((sum^bias) & (sum^hys_counter)) >>> 31);

      if (hys_counter<-hys_threshold){
         hys_type = false; //nonDFP
         hys_counter=0;
      }
      else if (hys_counter>hys_threshold){
          hys_type = true; // DFP
          hys_counter=0;
      }
   }
   
   private final void charParser(char inchars[],int offset,int length, MathContext set, char originalChars[]){

      final int initLength=length;
      boolean exotic=false; // have extra (illegal) digits (other than 0-9, ., E/e)
      int numDigits=0; // count of digits found
      int dotoff=-1; // offset where dot was found
      int last=-1;// last character of mantissa
      char si=0;
      int j=0;
      char sj=0;
      int dvalue=0;
      long interm_exp=0;

      long bcdVal=0; //used to accumulate digits as shortcut to DFP
      int newInd = 0x20; //ispos
      int newExp=0;

      /*
       * Processes the entire string, one char at a time.
       * Also provides the fastest path if the incoming string is pure BCD.
       */

      int i=offset;
      i:for(int $1=length ; $1>0; $1--,i++){
         si=inchars[i];
         if (si>='0' && si<='9'){  // test for Arabic digit
            last=i;
            bcdVal<<=4;
            bcdVal|=(long)(si&0xF);  //buffer up for BCD to DFP conversion
            numDigits++; // still in mantissa
            continue i;
         }
         if (si=='.'){ // record and ignore
            if (dotoff>=0)
               bad(originalChars); // two dots
            dotoff=i-offset; // offset into mantissa (from the left)
            continue i;
         }

         //these should be the first chars
         if (si=='-'){
            if ($1!=initLength)
               bad(originalChars);
            newInd = 0x60; //isneg
            offset=offset+1;
            length--;
            continue i;
         }
         if (si=='+'){
            if ($1!=initLength)
               bad(originalChars);
            offset=offset+1;
            length--;
            continue i;
         }
         if (si!='e'){
            if (si!='E') { // expect an extra digit
               if ((!java.lang.Character.isDigit(si)))
                  bad(originalChars); // not a number

               exotic=true; // will need conversion later
               last=i;
               numDigits++; // still in mantissa
               continue i;
            }
         }

         /*
          * Processes the digits occuring after the E or e
          * (k is our index into exponent digits)
          */
         int k=0;
         int elen=0;
         boolean eneg=false;

         if (i-offset>length-2)
            bad(originalChars); // no room for even one digit in the exponent
         eneg=false;

         /* check exponent sign */
         if (inchars[i+1]=='-'){
            eneg=true;
            k=i+2;
         }
         else {
            if (inchars[i+1]=='+')
               k=i+2;
            else
               k=i+1;
         }

         // k is offset of first expected exponent digit
         // elen is length of exponent
         elen=length-(k-offset); // possible number of digits

         if (elen==0) {
            // math.1D = Invalid exponent
            throw new NumberFormatException(Messages.getString("math.1D")); //$NON-NLS-1$
         }

         // find first non-zero digit
         int $2=elen;
         j=k;
         for(;$2>0;$2--,j++){
            sj=inchars[j]; // temp store for current digit
            if (sj!='0')
               break;
            elen--;
         }
         k=j;

         // now we have pushed offset to first non-zero digit
         // length of exponent shouldn't be bigger than 32 bits
         if (elen>10) {
            // math.1D = Invalid exponent
            throw new NumberFormatException(Messages.getString("math.1D")); //$NON-NLS-1$
         }

         //if elen = 10, make sure within the range
         //-Integer.MAX_VALUE (Integer.MIN_VALUE+1) and Integer.MAX_VALUE
         //(make sure value is between 2147483647 and -2147483647)
         char [] maxExp = {'2','1','4','7','4','8','3','6','4','7'};

         if (elen == 10){
            boolean equals=true;

            //loop while numbers are equal...
            for (int b=0; b < 10; b++){
               if (inchars[k+b] < maxExp[b])
                  break;
               else if(inchars[k+b] > maxExp[b]){
                  equals=false;
                  break;
               }
            }
            if (!equals) {
               // math.1E = Invalid scale, must be between {0} and {1}
               throw new NumberFormatException(Messages.getString("math.1D", //$NON-NLS-1$
                  new String[] {Integer.toString(Integer.MAX_VALUE), Integer.toString(Integer.MIN_VALUE+1)}));
            }
         }

         // process each exponent digit
         $2=elen;
         //j=k;
         for(;$2>0;$2--,j++){
            sj=inchars[j]; // temp store for current digit
            if (sj<'0')
               bad(originalChars); // always bad
            if (sj>'9'){ // maybe an exotic digit
               if ((!java.lang.Character.isDigit(sj)))
                  bad(originalChars); // not a number
               dvalue=java.lang.Character.digit(sj,10); // check base
               if (dvalue<0)
                  bad(originalChars); // not base 10
            }
            else
               dvalue=((int)(sj))-((int)('0'));

            //running store of our exponent
            newExp = newExp*10+dvalue;
         }

         // check exponent sign
         if (eneg)
            newExp=-newExp; // was negative
         break i; // we are done
      }

      // if no digits were found
      if (numDigits==0)
         bad(originalChars); // no mantissa digits

      /*
       * At this point in the code, the entire String is inspected
       * and looks good!
       */

      //check to see if dot + exp doesn't exceed max scale,
      //and if valid, adjust exponent to include fractional digits
      if (dotoff>=0){
         interm_exp = (long)newExp+(long)dotoff-(long)numDigits;

         if (-interm_exp < Integer.MIN_VALUE || -interm_exp> Integer.MAX_VALUE) {
            // math.1F = BigDecimal scale outside legal range: {0}
            throw new NumberFormatException(Messages.getString("math.1F", //$NON-NLS-1$
               Long.toString(-interm_exp)));
         }

         // adjust exponent/scale so that we can get rid of the dot
         newExp=newExp+dotoff-numDigits; // adjust exponent if inchars had a dot
      }

      /* strip leading zeros/dot (leave final if all 0's) */
      i=offset;
      int $3=last-1;
      i:for(;i<=$3;i++){
         si=inchars[i];
         if (si=='0'){
            offset=offset+1;
            dotoff=dotoff-1;
            numDigits=numDigits-1;
         }
         else
            if (si=='.'){
               offset=offset+1; // step past dot
               dotoff=dotoff-1;
            }
            else {
               if (si<='9')
                  break i;/* non-0 */
               else{/* exotic */
                  if (java.lang.Character.digit(si,10)!=0)
                     break i; // non-0 or bad
                  // is 0 .. strip like '0'
                  offset=offset+1;
                  dotoff=dotoff-1;
                  numDigits=numDigits-1;
               }
            }
      }

      // if we can exploit DFP using an unsigned BCD as the source, let's do it...

      /* NOTE:  Right now we only use the unsigned BCD instruction, which means
       *         we'll perform the case with prec <=16 for positive BCDs only,
       *         in the case of negative BCDs, we'll negate the result, then perform
       *         the rounding...
       */
      boolean tryLL=true;
      boolean DFPCrit = numDigits < 17 && newExp>=-398 && newExp<369;
      if (DFPHWAvailable() && DFPCrit) {
         if(DFPPerformHysteresis()) {
            DFPPerformHysteresis(10);
         }
         if(DFPUseDFP()) {
            if(DFPCharConstructorHelper(bcdVal, newExp, newInd, numDigits, set)) {
               return;
            }
            tryLL = false;
         }
      }

      // place into char array
      char [] sb = new char[numDigits];
      j=offset; // input offset

      // if we encountered an exotic non 0-9.. E/e character
      if (exotic){
         exotica:do{ // slow: check for exotica
            i=0;
            int $4=numDigits;

            for(;$4>0;$4--,i++){
               if (i==dotoff)
                  j=j+1; // at dot (skip it)
               sj=inchars[j];
               if (sj<='9')
                  sb[i]=sj;
               else{
                  dvalue=java.lang.Character.digit(sj,10);
                  if (dvalue<0)
                     bad(originalChars); // not a number after all
                  sb[i]=(char)(dvalue);
               }
               j=j+1;
            }
         }while(false);
      }

      // no exotic characters, just copy the digits into the mantissa
      else {
         simple:do{
            int $5=numDigits;
            i=0;
            for(;$5>0;$5--,i++){
               if (i==dotoff)
                  j=j+1; //skip the dot
               sb[i]=inchars[j];
               j=j+1;
            }
         }while(false);
      }

      /* Looks good.  Set the sign indicator and form, as needed. */
      // Trailing zeros are preserved
      // The rule here for form is:
      //   If no E-notation, then request plain notation
      //   Otherwise act as though add(0,DEFAULT) and request scientific notation
      // [form is already PLAIN]

      // if we're dealing with a zero mantissa value
      if (sb[0]=='0')
         newInd = 0x00;

      //up to this point, assumed BI rep
      charConstructor(sb, newExp, newInd, set, tryLL);
   }

   private final void charConstructor(char [] sb, int newExp,int newInd,
         MathContext set, boolean tryLL){

      // we're still in BI form here...
      if (tryLL && sb.length < 19){

         // set LL representation
         this.flags |= 0x00000001;

         // store the long lookaside
         this.laside = toLongForm(sb);

         //set precision
         this.flags|=0x10;//cache on
         this.flags |= numDigits(this.laside) << 7;

         if (newInd == 0x60 /* isneg */)
            this.laside *=-1;

         // store exponent
         this.cachedScale = -newExp;
      }
      else{
         // store BI representation
         this.flags |= 0x00000002;

         // store the BigInteger
         if (newInd == 0x60 /* isneg */)
            this.bi = new BigInteger("-"+new String(sb));
         else
            this.bi = new BigInteger(new String(sb));

         //store the exponent
         this.cachedScale = -newExp;

         // if less than max cachable precision
         if (sb.length <= 33554431){
            this.flags|=0x10; // cache on
            this.flags |= sb.length << 7;
         }
      }

      if (set != MathContext.UNLIMITED)
         this.finish(set.getPrecision(), set.getRoundingMode().ordinal());
   }

   private void bigIntegerConstructor(BigInteger bi,int scale, MathContext set){

      if (DFPHWAvailable() && DFPUseDFP() ){
         DFPBigIntegerConstructorHelper(bi, scale, set);
         return;
      }

      // At this point, didn't try DFP, so try LL or BI

      // Bit length of < 63 guarantees to fit in a long,
      // but pushes Long.MAX_VALUE & Long.MIN_VALUE and other closely
      // related values to using the byte array.
      if (bi.bitLength()<63){

         // store as LL representation
         this.flags |= 0x00000001;

         // store the long lookaside
         this.laside=bi.longValue();

         /* cache - SGP ESRR */

         // cache the precision
         this.flags|=0x10; // cache on
         this.flags |= numDigits(this.laside) << 7;

         // store the exponent
         this.cachedScale=scale;

         if (set != MathContext.UNLIMITED)
            this.finish(set.getPrecision(), set.getRoundingMode().ordinal());
      }
      else{

         // place into BigInteger representation
         this.flags |= 0x00000002; //the representation
         this.bi = bi; //the BigInteger
         this.cachedScale = scale; //the exponent

         // round
         if (set != MathContext.UNLIMITED)
            this.finish(set.getPrecision(), set.getRoundingMode().ordinal());
         return;
      }
   }

   private final void bad(char s[]){
      // math.20 = Not a valid char constructor input: {0}
      throw new NumberFormatException(Messages.getString("math.03", String.valueOf(s)));//$NON-NLS-1$
   }

   // methods throwing exception used more than 6 times
   private final void badDivideByZero() {
      // math.1C = division by zero
      throw new ArithmeticException(Messages.getString("math.1C")); //$NON-NLS-1$
   }
   private final void conversionOverflow(BigDecimal bd) {
      // math.26 = Conversion overflow: {0}
      throw new ArithmeticException(Messages.getString("math.26", bd)); //$NON-NLS-1$
   }
   private final void nonZeroDecimals(BigDecimal bd) {
      // math.25 = Non-zero decimal digits: {0}
      throw new ArithmeticException(Messages.getString("math.25", bd)); //$NON-NLS-1$
   }
   private final void scaleOutOfRange(long s) {
      // math.1F = BigDecimal scale outside legal range: {0}
      throw new ArithmeticException(Messages.getString("math.1F", Long.toString(s))); //$NON-NLS-1$
   }
   private static final void scaleOverflow() {
      // math.21 = Scale overflow
      throw new ArithmeticException(Messages.getString("math.21")); //$NON-NLS-1$
   }
      
   /**
    * Answers the sum of the receiver and argument.
    *
    * @param rhs the BigDecimal to add
    *
    * @return  The sum of adding two BigDecimal.
    */
   public BigDecimal add(BigDecimal rhs){
      BigDecimal res = new BigDecimal();
      BigDecimal lhsClone = this;
      BigDecimal rhsClone = rhs;

      if(DFPHWAvailable()) {
         if (DFPAddHelper(res, rhs)) {
            return res;
         }
      }

      /*
       * Note: We need to clone this & rhs if we're going to change
       * the internal representation from DFP to anything else this
       * is because the API states that BigDecimals are immutable.
       *
       * We are safe when transforming from LL to BI (i.e. do not
       * need to clone anything).
       */

      // SLOW - PATH
      lhsClone = possibleClone(this);
      rhsClone = possibleClone(rhs);

      // fast path adding of two equally scaled BigDecimals
      if ((lhsClone.flags & rhsClone.flags & 0x00000001) == 0x00000001){
         int lhsScale = lhsClone.cachedScale;
         if (lhsClone.cachedScale == rhsClone.cachedScale){

            long sum = lhsClone.laside + rhsClone.laside;

            if (overflowAdd(lhsClone.laside,rhsClone.laside,sum) ==0){
               res.laside = sum;
               res.cachedScale = lhsScale;  //res exponent remains the same

               // set LL representation
               res.flags |= 0x00000001;
               res.flags &= 0xFFFFFFEF; //clear prec cache bit
               return res;
            }
         }
      }

      return lhsClone.longAdd(rhsClone,res,MathContext.UNLIMITED,false);
   }

   private BigDecimal possibleClone(BigDecimal bd){
      if (DFPHWAvailable() && ((bd.flags & ~0xFFFFFFFC) == 0x00000000)){
         bd = clone(bd);
         bd.DFPToBI();
      }
      return bd;
   }
  
   /**
    * Answers the sum of the receiver and argument.
    *
    * @param rhs the BigDecimal to add
    * @param set defines the precision and rounding behaviour
    *
    * @return  The sum of adding two BigDecimal.
    */
   public BigDecimal add(BigDecimal rhs,MathContext set){

      if (DFPHWAvailable() && DFPPerformHysteresis() &&
         set.getPrecision() == 16 && set.getRoundingMode().ordinal() == BigDecimal.ROUND_HALF_EVEN){
         DFPPerformHysteresis(10);
      }

      BigDecimal res = new BigDecimal();
      BigDecimal lhsClone = this;
      BigDecimal rhsClone = rhs;

      // FAST - PATH
      if (DFPHWAvailable() && (((this.flags | rhs.flags) & ~0xFFFFFFFC) == 0x00000000)){
         if(DFPAddHelper(res, rhs, set)) {
            return res;
         }
      }

      /*
       * Note: We need to clone this & rhs if we're going to change
       * the internal representation from DFP to anything else this
       * is because the API states that BigDecimals are immutable.
       *
       * We are safe when transforming from LL to BI (i.e. do not
       * need to clone anything).
       */

      // SLOW - PATH
      if (((this.flags & ~0xFFFFFFFC) == 0x00000000)){
         lhsClone = clone(this);
         lhsClone.DFPToBI();
      }

      if (((rhs.flags & ~0xFFFFFFFC) == 0x00000000)){
         rhsClone = clone(rhs);
         rhsClone.DFPToBI();
      }

      // we pass in LLs and BIs here

      // fast path adding of two equally scaled BigDecimals
      if ((lhsClone.flags & rhsClone.flags & 0x00000001) == 0x00000001){
         int lhsScale = lhsClone.cachedScale;
         int rhsScale = rhsClone.cachedScale;
         if (lhsScale == rhsScale){

            long lhsLaside = lhsClone.laside;
            long rhsLaside = rhsClone.laside;
            int tempFlags=0;
            long sum = lhsLaside + rhsLaside;

            if (overflowAdd(lhsLaside,rhsLaside,sum) ==0){

               res.laside = sum;
               res.cachedScale = lhsScale;  //res exponent remains the same

               // set LL representation
               tempFlags |= 0x00000001;
            }
            else{ //put into BI form

               // Make sure in BI format
               BigInteger lhsBI = BigInteger.valueOf(lhsLaside);
               BigInteger rhsBI = BigInteger.valueOf(rhsLaside);

               res.bi = lhsBI.add(rhsBI);
               res.cachedScale = lhsScale;  //res exponent remains the same

               // set BI representation
               tempFlags |= 0x00000002;
            }
            tempFlags&=0xFFFFFFEF; //clear prec cache bit
            res.flags = tempFlags;

            //no rounding (no MC passed in)
            if (set != MathContext.UNLIMITED)
               res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            postSetScaleProcessing(res);
            return res;
         }
         else
            res = lhsClone.longAdd(rhsClone,res,set,false);
      }
      else
         res = lhsClone.longAdd(rhsClone,res,set,false);

      postSetScaleProcessing(res);
      return res;
   }

   private final BigDecimal longAdd(BigDecimal rhs,
         BigDecimal allocedRes, MathContext set, boolean onRhs){
         
      /* Incoming BDs might be any of LL or BI representation */

      // let's do the case for same scale, longs first since it means
      // it overflowed in add(BD)

      // do this only for calls from add()
      if (!onRhs && (rhs.flags & this.flags & 0x00000001) == 0x00000001){
         if (rhs.cachedScale == this.cachedScale){
            BigDecimal res = allocedRes;
            BigInteger lhsBI = BigInteger.valueOf(this.laside);
            BigInteger rhsBI = BigInteger.valueOf(rhs.laside);

            res.bi = lhsBI.add(rhsBI);
            res.cachedScale = this.cachedScale;  //res exponent remains the same
            res.flags|=0x00000002;
            res.flags&=0xFFFFFFEF; //clear prec cache bit
            return res;
         }
      }

      BigDecimal res=rhs; //we reuse ths unless told otherwise
      BigDecimal lhs = this;

      long lhsLaside;
      long rhsLaside;
      int lhsScale = lhs.cachedScale;
      int rhsScale = rhs.cachedScale;
      int tempFlags=0;

      //NOTE:  rhs != lhs for functions calling slAdd with 3rd param=true
      //       if lhs changes in the class, need to revisit this method
      //       and do similar save/restores as we do in slMultiply
      //        when the 3rd param is true

      boolean passed = false;
      if ((lhs.flags & rhs.flags & 0x00000001) == 0x00000001){

         // use a shortcut if we are performing non-exact addition
         if (set.getPrecision()!=0){

            // don't do this if both have same scale and not zero
            lhsLaside = lhs.laside;
            rhsLaside = rhs.laside;
            if (lhsScale != rhsScale && lhsLaside != 0 && rhsLaside != 0){
               // the following block will allow us to add a larger set
               // of BigDecimal objects together

               // which one is the bigger BD?
               // we want to reduce the effect of the smaller one
               BigDecimal biggerScaleBD ;
               BigDecimal smallerScaleBD ;
               if ((long)lhsScale-(long)rhsScale < 0) {
                  biggerScaleBD   = lhs;
                  smallerScaleBD = rhs;
               } else {               // lhs is small; augend is big
                  biggerScaleBD   = rhs;
                  smallerScaleBD = lhs;
               }

               // Estimate the scale of a unit in last place for the result
               // We assume there won't be a carryout on an add, or a
               // cancellation when borrowing for a subtract.
               // i.e. 9 + 1 = 10
               // i.e. 10 - 1.1 = 8.9

               int tempPrecision = 0;
               int tempFlags2 = biggerScaleBD.flags;
               if ((tempFlags2 & 0x10) !=0)
                  tempPrecision = (tempFlags2 & 0xFFFFFF80 ) >> 7;
               else
                  tempPrecision = numDigits(biggerScaleBD.laside);

               long ulp =
                  (long)biggerScaleBD.cachedScale - (long)tempPrecision + set.getPrecision();

               tempFlags2 = smallerScaleBD.flags;
               if ((tempFlags2 & 0x10) !=0)
                  tempPrecision = (tempFlags2 & 0xFFFFFF80 ) >> 7;
               else
                  tempPrecision = numDigits(biggerScaleBD.laside);

               // Now, we need to store the most significant digit of smallerScaleBD
               // this will be smallerScaleBD.scale() - (small.precision() - 1)
               long msDigR = (long)smallerScaleBD.cachedScale - (long)tempPrecision + 1;

               // Finally, we can shorten the smallerScaleBD if and only if:
               // (1) -the positions of digits in biggerScaleBD and smallerScaleBD do not overlap
               //       -thus, msDigR must be greater than the location
               //       of the last digit in biggerScaleBD, which will be biggerScaleBD.scale()
               //  (2)-the digits in smallerScaleBD are not visible in the result
               //   (since we have a MathContext != (prec=0 & UNNECESSARY) specified
               //       -thus, msDigR must be greater than the scale
               //       of the unit in last place of the result

               if (msDigR > (long)biggerScaleBD.cachedScale + 2 // don't overlap
                     && msDigR > ulp + 2) { //lhs digits not in result
                  long tempLaside = smallerScaleBD.laside;
                  int smallerSignum = ((int)(tempLaside >> 63)) | ((int) ((-tempLaside) >>> 63));
                  if (smallerScaleBD == rhs){
                     rhs = new BigDecimal();
                     rhs.laside = smallerSignum;
                     rhs.cachedScale = (int)(Math.max((long)biggerScaleBD.cachedScale, ulp)+3);
                     rhs.flags|=0x1; // set the LL representation;
                  }
                  else{
                     lhs = new BigDecimal();
                     lhs.laside = smallerSignum;
                     lhs.cachedScale = (int)(Math.max((long)biggerScaleBD.cachedScale, ulp)+3);
                     lhs.flags|=0x1; // set the LL representation;
                  }
               }
            }

            /*
             * If either number is zero and the precision setting is
             * nonzero then the other number, rounded if necessary, is
             * used as the result.
             */

            lhsLaside = lhs.laside;
            rhsLaside = rhs.laside;

            boolean lZero = (lhsLaside == 0);
            boolean rZero = (rhsLaside == 0);

            // if both zero, return the larger scaled one
            if (lZero && rZero){
               if (!onRhs){
                  clone(allocedRes,rhs);
                  res = allocedRes;
               }

               res.laside =0;
               res.cachedScale = Math.max(lhs.cachedScale,rhs.cachedScale);

               res.roundLL(set.getPrecision(),
                     set.getRoundingMode().ordinal(),false);
               return res;
            }

            // if left is zero
            else if (lZero){

               if (!onRhs){
                  clone(allocedRes,rhs);
                  res = allocedRes;
               }
               res.roundLL(set.getPrecision(),
                     set.getRoundingMode().ordinal(),false);

               int preferredScale = Math.max(lhs.cachedScale,rhs.cachedScale);

               int tempPrecision = 0;
               int tempFlags2 = res.flags;
               if ((tempFlags2 & 0x10) !=0)
                  tempPrecision = (tempFlags2 & 0xFFFFFF80 ) >> 7;
               else
                  tempPrecision = numDigits(res.laside);

               int precisionDiff = set.getPrecision() - tempPrecision;
               long scaleDiff =  preferredScale-res.cachedScale;

               // now perform the rescale iff we don't break the required
               // precision
               if (precisionDiff >= scaleDiff)
                  res = res.setScale(preferredScale,true);
               else
                  res =  res.setScale(res.scale() + precisionDiff, true);
               return res;
            }
            else if (rZero){

               clone(allocedRes,lhs);
               res= allocedRes;
               res.roundLL(set.getPrecision(),
                     set.getRoundingMode().ordinal(),false);

               int preferredScale = Math.max(lhs.cachedScale,rhs.cachedScale);

               int tempPrecision = 0;
               int tempFlags2 = res.flags;
               if ((tempFlags2 & 0x10) !=0)
                  tempPrecision = (tempFlags2 & 0xFFFFFF80 ) >> 7;
               else
                  tempPrecision = numDigits(res.laside);

               int precisionDiff = set.getPrecision() - tempPrecision;
               long scaleDiff =  preferredScale-res.cachedScale;

               if (precisionDiff >= scaleDiff)
                  res = res.setScale(preferredScale,true);
               else
                  res =  res.setScale(res.scale() + precisionDiff, true);
               return res;
            }
         }

         lhsLaside = lhs.laside;
         rhsLaside = rhs.laside;
         lhsScale = lhs.cachedScale;
         rhsScale = rhs.cachedScale;

         boolean lZero = (lhsLaside == 0);
         boolean rZero = (rhsLaside == 0);

         // if got here, still haven't done the add
         if (!passed){

            if (!onRhs)
               res = allocedRes;
            tempFlags=res.flags;

            /*
             * if both have differing scales, need to canonicalize them
             *
             * the way lhs works:
             *  -need to put them both in the largest scale of the two
             */
            /*else*/ if (lhsScale > rhsScale){  //lhs scale is bigger
               //we need to rescale the rhs by the difference
               //i.e. need to pad it with 0s...

               long diff = (long)-rhsScale+(long)lhsScale;
               long pad = powerOfTenLL(diff);
               if (pad != -1){
                  // manually inline overflowMultiply here
                  long tempRhsLaside = rhsLaside;
                  if (tempRhsLaside < 0) tempRhsLaside *= -1;
                  int m = Long.numberOfLeadingZeros(tempRhsLaside)-1;
                  int n = Long.numberOfLeadingZeros(pad)-1;
                  if (m+n>=63){

                     rhsLaside*=pad;

                     long sum = lhsLaside + rhsLaside;
                     if (overflowAdd(lhsLaside,rhsLaside,sum)==0){
                        res.laside = sum;
                        res.cachedScale = lhsScale; //max scale of the two

                        // set LL representation
                        tempFlags &= 0xFFFFFFFC; //clear rep bits
                        tempFlags |= 0x00000001;

                        passed = true;
                     }
                  }
               }
               else{

                  // Make sure in BI format
                  BigInteger lhsBI = BigInteger.valueOf(lhsLaside);
                  BigInteger rhsBI = BigInteger.valueOf(rhsLaside);

                  if (!rZero){
                     if (diff > Integer.MAX_VALUE) {
                        scaleOverflow();
                     }
                     rhsBI = rhsBI.multiply(powerOfTenBI(diff));
                  }
                  res.bi = lhsBI.add(rhsBI);
                  res.cachedScale = lhsScale; //max scale of the two

                  // set BI representation
                  tempFlags &= 0xFFFFFFFC; // clear rep
                  tempFlags |= 0x00000002;

                  passed = true;
               }
            }
            else{ //rhs scale is bigger

               //we need to rescale lhs by the difference
               //i.e. need to pad it with 0s...
               long diff = (long)-lhsScale + (long)rhsScale;
               long pad = powerOfTenLL(diff);
               if (pad !=-1){

                  // manually inline overflowMultiply here
                  long tempLhsLaside = lhsLaside;
                  if (tempLhsLaside < 0) tempLhsLaside*= -1;
                  int m = Long.numberOfLeadingZeros(tempLhsLaside)-1;
                  int n = Long.numberOfLeadingZeros(pad)-1;
                  if (m+n>=63){ //&& !overflowMultiply(lhsLaside,pad)){

                     lhsLaside*=pad;
                     long sum = lhsLaside + rhsLaside;

                     if (overflowAdd(lhsLaside,rhsLaside,sum)==0){

                        res.laside = sum;
                        res.cachedScale = rhsScale; //max scale of the two

                        // set LL representation
                        tempFlags &= 0xFFFFFFFC; // clear rep bits
                        tempFlags |= 0x00000001;

                        passed = true;
                     }
                  }
               }
               else{

                  // Make sure in BI format
                  BigInteger lhsBI = BigInteger.valueOf(lhsLaside);
                  BigInteger rhsBI = BigInteger.valueOf(rhsLaside);

                  if (!lZero){
                     if (diff > Integer.MAX_VALUE) {
                        scaleOverflow();
                     }
                     lhsBI = lhsBI.multiply(powerOfTenBI(diff));
                  }

                  res.bi = lhsBI.add(rhsBI);
                  res.cachedScale = rhsScale; //max scale of the two

                  // set BI representation
                  tempFlags &= 0xFFFFFFFC; // clear rep
                  tempFlags |= 0x00000002;

                  passed = true;
               }
            }
         }
         res.flags=tempFlags;
      }

      //NOTE:  We do not cache precision!
      res.flags&=0xFFFFFFEF; //clear prec cache bit

      //Perform rounding
      if (passed){
         if (set != MathContext.UNLIMITED)
            res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
         return res;
      }
      else
         return slAdd(rhs,allocedRes,set,onRhs);
   }

   private BigDecimal slAdd(BigDecimal rhs,BigDecimal allocedRes, MathContext set, boolean onRhs){

      // Guaranteed that incoming is either LL or BI

      BigDecimal res=rhs; //we reuse ths unless told otherwise
      BigDecimal lhs = this;
      boolean passed = false;

      BigInteger lhsBI=null;
      BigInteger rhsBI=null;
      int lhsScale = lhs.cachedScale;
      int rhsScale = rhs.cachedScale;

      int tempFlags =0;

      // use a shortcut if performing non-exact addition
      if (set.getPrecision()!=0){

         // don't do this if both have same scale and not zero
         if (lhsScale != rhsScale && lhs.signum() != 0 && rhs.signum() != 0){

            // the following block will allow us to add a larger set
            // of BigDecimal objects together

            // which one is the bigger BD?
            // we want to reduce the effect of the smaller one
            BigDecimal biggerScaleBD ;
            BigDecimal smallerScaleBD ;
            if ((long)lhsScale-(long)rhsScale < 0) {
               biggerScaleBD   = lhs;
               smallerScaleBD = rhs;
            } else {               // lhs is small; augend is big
               biggerScaleBD   = rhs;
               smallerScaleBD = lhs;
            }

            // Estimate the scale of a unit in last place for the result
            // We assume there won't be a carryout on an add, or a
            // cancellation when borrowing for a subtract.
            // i.e. 9 + 1 = 10
            // i.e. 10 - 1.1 = 8.9

            long ulp = (long)biggerScaleBD.cachedScale - (long)biggerScaleBD.precision() + set.getPrecision();

            // Now, we need to store the most significant digit of smallerScaleBD
            // this will be smallerScaleBD.scale() - (small.precision() - 1)
            long msDigR = (long)smallerScaleBD.cachedScale- (long)smallerScaleBD.precision() + 1;

            // Finally, we can shorten the smallerScaleBD if and only if:
            // (1) -the positions of digits in biggerScaleBD and smallerScaleBD do not overlap
            //       -thus, msDigR must be greater than the location
            //       of the last digit in biggerScaleBD, which will be biggerScaleBD.scale()
            //  (2)-the digits in smallerScaleBD are not visible in the result
            //   (since we have a MathContext != (prec=0 & UNNECESSARY) specified
            //       -thus, msDigR must be greater than the scale
            //       of the unit in last place of the result

            if (msDigR > (long)biggerScaleBD.cachedScale + 2 // don't overlap
                  && msDigR > ulp + 2) { //lhs digits not in result
               int smallerSignum = smallerScaleBD.signum();
               if (smallerScaleBD == rhs){
                  rhs = new BigDecimal();
                  rhs.bi = BigInteger.valueOf(smallerSignum);
                  rhs.cachedScale = (int)(Math.max((long)biggerScaleBD.cachedScale, ulp)+3);
                  rhs.flags|=0x2; // set the BI representation;
               }
               else{
                  lhs = new BigDecimal();
                  lhs.bi = BigInteger.valueOf(smallerSignum);
                  lhs.cachedScale = (int)(Math.max(biggerScaleBD.cachedScale, (int)ulp)+3);
                  lhs.flags|=0x2; // set the BI representation;
               }
            }
         }

         // can still be in LL or BI here
         boolean lZero = (lhs.signum() == 0);
         boolean rZero = (rhs.signum() == 0);

         /*
          * If either number is zero and the precision setting is
          * nonzero then the other number, rounded if necessary, is
          * used as the result.
          */

         // if both zero, return the larger scaled one
         if (lZero && rZero){
            if (!onRhs){
               clone(allocedRes,rhs);
               res = allocedRes;
            }

            res.laside =0;
            res.cachedScale = Math.max(lhsScale,rhsScale);
            res.roundBI(set.getPrecision(), set.getRoundingMode().ordinal(),false);
            return res;
         }

         // if left is zero
         else if (lZero){

            if (!onRhs){
               clone(allocedRes,rhs);
               res = allocedRes;
            }

            if ((res.flags &0x00000001) == 0x00000001){
               res.roundLL(set.getPrecision(),
                     set.getRoundingMode().ordinal(),false);
               }
               else{
               if ((res.flags & 0x00000003) == 0x00000000){
                  res.DFPToBI();
               }
                  res.roundBI(set.getPrecision(),
                        set.getRoundingMode().ordinal(),false);
            }

            int preferredScale  = Math.max(lhsScale,rhsScale);
            int precisionDiff = set.getPrecision() - res.precision();
            long scaleDiff =  preferredScale-res.cachedScale;

            // now perform the rescale iff we don't break the required precision
            if (precisionDiff >= scaleDiff)
               res = res.setScale(preferredScale,true);
            else
               res =  res.setScale(res.scale() + precisionDiff, true);
            return res;

         }
         else if (rZero){

            clone(allocedRes,lhs);
            res = allocedRes;

            if ((res.flags &0x00000001) == 0x00000001){
               res.roundLL(set.getPrecision(),
                     set.getRoundingMode().ordinal(),false);
               }
               else{
               if ((res.flags & 0x00000003) == 0x00000000){
                  res.DFPToBI();
               }
                  res.roundBI(set.getPrecision(),
                        set.getRoundingMode().ordinal(),false);
            }

            int preferredScale = Math.max(lhsScale,rhsScale);
            int precisionDiff = set.getPrecision() - res.precision();
            long scaleDiff =  preferredScale-res.cachedScale;

            if (precisionDiff >= scaleDiff)
               res = res.setScale(preferredScale,true);
            else
               res =  res.setScale(res.scale() + precisionDiff, true);
            return res;
         }
      }

      // Make sure in BI format
      lhsBI = lhs.bi;
      rhsBI = rhs.bi;
      if ((lhs.flags & 0x00000003) != 0x2) {
         BigDecimal lhsClone = lhs;
         if((lhs.flags & 0x3) == 0x0) {
            lhsClone = clone(lhs);
            lhsClone.DFPToLL();
         }
         lhsBI = BigInteger.valueOf(lhsClone.laside);
      }
      if ((rhs.flags & 0x00000003) != 0x2) {
         BigDecimal rhsClone = rhs;
         if((rhs.flags & 0x3) == 0x0) {
            rhsClone = clone(rhs);
            rhsClone.DFPToLL();
         }
         rhsBI = BigInteger.valueOf(rhsClone.laside);
      }

      lhsScale = lhs.cachedScale;
      rhsScale = rhs.cachedScale;

      boolean lZero = (lhsBI.signum() == 0);
      boolean rZero = (rhsBI.signum() == 0);

      // if we got here, still haven't done the add
      if (!passed){

         if (!onRhs)
            res = allocedRes;
         tempFlags = res.flags;

         // if both have the same scale, perform simple add...
         if (lhsScale == rhsScale){

            res.bi = lhsBI.add(rhsBI);
            res.cachedScale = lhs.cachedScale;  //res exponent remains the same

            // set BI representation
            tempFlags &= 0xFFFFFFFC; // clear rep
            tempFlags |= 0x00000002;
         }

         /*
          * if both have differing scales, need to canonicalize them
          *
          * the way lhs works:
          *  -need to put them both in the largest scale of the two
          */
         else if (lhsScale > rhsScale){  //lhs scale is bigger

            if (!rZero){
               long diff = -(long)rhsScale+(long)lhsScale;
               if (diff > Integer.MAX_VALUE) {
                  scaleOverflow();
               }
               rhsBI = rhsBI.multiply(powerOfTenBI(diff));
            }
            res.bi = lhsBI.add(rhsBI);
            res.cachedScale = lhsScale; //max scale of the two

            // set BI representation
            tempFlags &= 0xFFFFFFFC; //clear rep bits
            tempFlags |= 0x00000002;

            //NOTE: We do not cache precision
         }
         else{ //rhs scale is bigger

            if (!lZero){
               long diff = -(long)lhsScale+(long)rhsScale;
               if (diff > Integer.MAX_VALUE) {
                  scaleOverflow();
               }
               lhsBI = lhsBI.multiply(powerOfTenBI(diff));
            }
            res.bi = lhsBI.add(rhsBI);
            res.cachedScale = rhsScale; //max scale of the two

            // set BI representation
            tempFlags &= 0xFFFFFFFC; // clear rep bits
            tempFlags |= 0x00000002;
         }
         res.flags=tempFlags;
      }

      //NOTE:  We do not cache precision!
      res.flags&=0xFFFFFFEF; //clear prec cache bit

      if (set != MathContext.UNLIMITED)
         res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
      return res;
   }

   /**
    * Answers the subtract result of the receiver and argument.
    *
    * @param rhs the value to subtract from the receiver
    *
    * @return  a new BigDecimal, the result of subtraction.
    */
   public BigDecimal subtract(BigDecimal rhs){

      BigDecimal res = new BigDecimal();
      BigDecimal lhsClone = this;
      BigDecimal rhsClone = rhs;

      if(DFPHWAvailable()) {
         if(DFPSubtractHelper(res, rhs)) {
            return res;
         }
      }

      // SLOW - PATH
      lhsClone = possibleClone(this);
      rhsClone = possibleClone(rhs);

      // we pass in LLs and BIs here
      return lhsClone.subtract(rhsClone,res,MathContext.UNLIMITED);
   }

   /**
    * Answers the subtract result of the receiver and argument.
    *
    * @param rhs the value to subtract from the receiver
    * @param set defines the precision and rounding behaviour
    *
    * @return  a new BigDecimal, the result of subtraction.
    */
   public BigDecimal subtract(BigDecimal rhs,MathContext set){

      if (DFPHWAvailable() && DFPPerformHysteresis() && set.getPrecision() == 16 &&
          set.getRoundingMode().ordinal() == BigDecimal.ROUND_HALF_EVEN){
         DFPPerformHysteresis(10);
         }

      BigDecimal res = new BigDecimal();
      BigDecimal lhsClone = this;
      BigDecimal rhsClone = rhs;

      // FAST - PATH
      if (DFPHWAvailable() && (((this.flags | rhs.flags) & ~0xFFFFFFFC) == 0x00000000)){
         if(DFPSubtractHelper(res, rhs, set)) {
            return res;
         }
      }

      // SLOW - PATH
      if (((this.flags & ~0xFFFFFFFC) == 0x00000000)){
         lhsClone = clone(this);
         lhsClone.DFPToBI();
      }

      if (((rhs.flags & ~0xFFFFFFFC) == 0x00000000)){
         rhsClone = clone(rhs);
         rhsClone.DFPToBI();
      }

      // we pass in LLs and BIs here

      res = lhsClone.subtract(rhsClone, res, set);

      postSetScaleProcessing(res);
      return res;
   }

   private final BigDecimal subtract(BigDecimal rhs,
         BigDecimal allocedRes, MathContext set){

      BigDecimal newrhs=clone(rhs);

      // Flip the rhs indicator before adding
      if ((rhs.flags & 0x00000001) == 0x00000001){ //if rhs is LL
         newrhs.laside*=-1;
      }
      else{ //rhs is BI
         if((newrhs.flags & 0x3) == 0x0) {
            newrhs.DFPToBI();
         }
         newrhs.bi = newrhs.bi.negate();
      }

      //not caching signum anymore
      newrhs.flags&=0xFFFFFFFB;
      
      return this.longAdd(newrhs, allocedRes,set,true);
   }

   /**
    * Answers the multiplication result of the receiver and argument.
    *
    * @param rhs the value to multiply with the receiver
    *
    * @return  the result of multiplying two bigDecimals.
    */
   public BigDecimal multiply(BigDecimal rhs){

      // manually inlined long-lookaside multiply path here
      // this is faster than DFP code path
      if (((this.flags & rhs.flags & 0x00000001) == 0x00000001)){

         BigDecimal res = new BigDecimal();
         long interm_scale=0; //check for overflow
         int tempFlags = this.flags;

         //NOTE:  We do not cache precision!
         interm_scale = (long)this.scale() + (long)rhs.scale();

         // store the final exponent in res
         if (interm_scale < (long)Integer.MIN_VALUE || interm_scale > (long)Integer.MAX_VALUE){
            if (this.signum() == 0) {
               interm_scale = (interm_scale> Integer.MAX_VALUE)?Integer.MAX_VALUE:Integer.MIN_VALUE;
            } else {
               scaleOutOfRange(interm_scale);
            }
         }

         res.cachedScale=(int)interm_scale;

         long lhsLaside = this.laside;
         long rhsLaside = rhs.laside;
         if (lhsLaside < 0) lhsLaside *= -1;
         if (rhsLaside < 0) rhsLaside *= -1;
         int m = Long.numberOfLeadingZeros(lhsLaside)-1;
         int n = Long.numberOfLeadingZeros(rhsLaside)-1;
         if (m+n>=63){

            long result = this.laside * rhs.laside;
            res.laside=result;

            // set LL representation
            tempFlags &= 0xFFFFFFFC; // clear rep bits
            tempFlags |= 0x00000001;

            //NOTE:  Don't cache precision
            tempFlags&=0xFFFFFFEF; //clear prec cache bit
            res.flags=tempFlags;
            return res;
         }
      }

      // catch all clause
      return multiply2(rhs);
   }

   private BigDecimal multiply2(BigDecimal rhs){

      BigDecimal res = new BigDecimal();
      BigDecimal lhsClone = null;
      BigDecimal rhsClone = null;

      // FAST - PATH
      if (DFPHWAvailable() && (((this.flags | rhs.flags) & ~0xFFFFFFFC) == 0x00000000)){
         if(DFPMultiplyHelper(res, rhs)) {
            return res;
         }
      }

      // SLOW - PATH
      if (((this.flags & ~0xFFFFFFFC) == 0x00000000)){
         lhsClone = clone(this);
         lhsClone.DFPToBI();
      }

      if (((rhs.flags & ~0xFFFFFFFC) == 0x00000000)){
         rhsClone = clone(rhs);
         rhsClone.DFPToBI();
      } else {
         rhsClone = rhs;
      }

      // we pass in LLs and BIs here
      if (lhsClone != null)
         lhsClone.longMultiply(rhsClone,res,MathContext.UNLIMITED,false);
      else
         this.longMultiply(rhsClone,res,MathContext.UNLIMITED,false);

      return res;
   }

   /**
    * Answers the multiplication result of the receiver and argument.
    *
    * @param rhs the value to multiply with the receiver
    * @param set defines the precision and rounding behaviour
    *
    * @return  the result of multiplying two bigDecimals.
    */
   public BigDecimal multiply(BigDecimal rhs,MathContext set){

      if (DFPHWAvailable() && DFPPerformHysteresis() && set.getPrecision() == 16 &&
            set.getRoundingMode().ordinal() == BigDecimal.ROUND_HALF_EVEN){
         DFPPerformHysteresis(10);
      }

      BigDecimal res = new BigDecimal();
      BigDecimal lhsClone = this;
      BigDecimal rhsClone = rhs;

      // FAST - PATH
      if (DFPHWAvailable() && (((this.flags | rhs.flags) & ~0xFFFFFFFC) == 0x00000000)){
         if(DFPMultiplyHelper(res, rhs, set)) {
            return res;
         }
      }

      // SLOW - PATH
      if (((this.flags & ~0xFFFFFFFC) == 0x00000000)){
         lhsClone = clone(this);
         lhsClone.DFPToBI();
      }

      if (((rhs.flags & ~0xFFFFFFFC) == 0x00000000)){
         rhsClone = clone(rhs);
         rhsClone.DFPToBI();
      }

      // we pass in LLs and BIs here
      lhsClone.longMultiply(rhsClone,res,set,false);

      return res;
   }

   private final void longMultiply(BigDecimal rhs,
         BigDecimal allocedRes, MathContext set, boolean onLhs){

      long interm_scale=0; //check for overflow
      boolean passed = false;

      int tempFlags;
      if (onLhs)
         tempFlags = this.flags;
      else
         tempFlags = allocedRes.flags;

      //NOTE:  We do not cache precision!
      tempFlags&=0xFFFFFFEF; //clear prec cache bit

      //Since we might overwrite lhs, and since rhs might equal lhs, save some values
      //int lhsExp = this.exp;
      //int rhsExp = rhs.exp;

      interm_scale = (long)this.scale() + (long)rhs.scale();

      // store the final exponent in res
      if (interm_scale < (long)Integer.MIN_VALUE || interm_scale > (long)Integer.MAX_VALUE){
         if (this.signum() == 0) {
            interm_scale = (interm_scale> Integer.MAX_VALUE)?Integer.MAX_VALUE:Integer.MIN_VALUE;
         } else {
            scaleOutOfRange(interm_scale);
         }
      }

      if (onLhs)
         this.cachedScale=(int)interm_scale;
      else
         allocedRes.cachedScale = (int)interm_scale;

      //quick LL multiplication
      if ((this.flags & rhs.flags & 0x00000001) == 0x00000001){

         long lhsLaside = this.laside;
         long rhsLaside = rhs.laside;
         if (lhsLaside < 0) lhsLaside *= -1;
         if (rhsLaside < 0) rhsLaside *= -1;
         int m = Long.numberOfLeadingZeros(lhsLaside)-1;
         int n = Long.numberOfLeadingZeros(rhsLaside)-1;
         if (m+n>=63){

            long result = this.laside * rhs.laside;
            if (onLhs)
               this.laside=result;
            else
               allocedRes.laside=result;

            // set LL representation
            tempFlags &= 0xFFFFFFFC; // clear rep bits
            tempFlags |= 0x00000001;

            passed = true;
         }
         else{

            // Make sure in BI format
            BigInteger lhsBI = BigInteger.valueOf(this.laside);
            BigInteger rhsBI = BigInteger.valueOf(rhs.laside);

            if (onLhs)
               this.bi = rhsBI.multiply(lhsBI);
            else
               allocedRes.bi = rhsBI.multiply(lhsBI);

            // set BI representation
            tempFlags &= 0xFFFFFFFC; // clear rep bits
            tempFlags |= 0x00000002;

            passed = true;
         }

         if (onLhs)
            this.flags=tempFlags;
         else
            allocedRes.flags = tempFlags;
      }

      if (passed){
         if (set != MathContext.UNLIMITED)
            {
            if (onLhs)
               this.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            else
               allocedRes.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            }
      }
      else
         // fall-through slow path multiplication
         {
         if (onLhs)
            slMultiply(rhs, this, set, onLhs);
         else
            slMultiply(rhs, allocedRes, set, onLhs);
         }

      if (onLhs)
         postSetScaleProcessing(this);
      else
         postSetScaleProcessing(allocedRes);
   }

   private final void slMultiply(BigDecimal rhs, BigDecimal res, MathContext set, boolean onLhs){

      //NOTE:  Incoming res already has the correct resulting exponent set

      // Put both in BI form
      BigDecimal lhsClone = null;
      if (onLhs)
         lhsClone = clone(this);

      // Make sure in BI format
      BigInteger lhsBI;
      if (onLhs)
         lhsBI = lhsClone.bi;
      else
         lhsBI = this.bi;
      BigInteger rhsBI = rhs.bi;

      // Make sure in BI format
      lhsBI = this.bi;
      rhsBI = rhs.bi;
      if (onLhs)
      {
         if ((lhsClone.flags & 0x00000003) != 0x2) {
            if((lhsClone.flags & 0x3) == 0x0) {
               lhsClone.DFPToLL();
            }
            lhsBI = BigInteger.valueOf(lhsClone.laside);
         }
      }
      else
      {
         if ((this.flags & 0x00000003) != 0x2) {
            BigDecimal thisClone = clone(this);
            if((thisClone.flags & 0x3) == 0x0) {
               thisClone.DFPToLL();
            }
            lhsBI = BigInteger.valueOf(thisClone.laside);
         }
      }

      if ((rhs.flags & 0x00000003) != 0x2) {
         BigDecimal rhsClone = clone(rhs);
         if((rhsClone.flags & 0x3) == 0x0) {
            rhsClone.DFPToLL();
         }
         rhsBI = BigInteger.valueOf(rhsClone.laside);
      }

      int tempFlags = res.flags;

      // need to do this after the conversion
      tempFlags&= 0xFFFFFFFC ; // clear repb bits
      tempFlags |= 0x00000002; // set as BI

      // Perform the multiplication
      res.bi = lhsBI.multiply(rhsBI);

      // NOTE:  We do not cache precision!
      tempFlags&=0xFFFFFFEF; //clear prec cache bit
      res.flags=tempFlags;

      if (set != MathContext.UNLIMITED)
         res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
   }

   /**
    * Answers the result of (this / rhs).
    *
    * @param rhs the value used to divide the receiver
    * @param rm the rounding constant
    *
    * @return  result of this/rhs.
    */
   public BigDecimal divide(BigDecimal rhs, int rm){
      return this.divide(rhs,this.scale(),rm);
   }

   /**
    * Answers the result of (this / rhs).
    *
    * @param rhs the value used to divide the receiver
    * @param rm the rounding mode
    *
    * @return  result of this/rhs.
    */
   public BigDecimal divide(BigDecimal rhs, java.math.RoundingMode rm){
      return this.divide(rhs,this.scale(),rm.ordinal());
   }

   /**
    * Answers the result of (this / rhs).
    *
    * @param rhs the value used to divide the receiver
    * @param scale the scale used for the result
    * @param rm the rounding mode
    *
    * @return  result of this/rhs.
    */
   public BigDecimal divide(BigDecimal rhs,int scale,java.math.RoundingMode rm){
      return this.divide(rhs,scale,rm.ordinal());
   }

   /**
    * Answers the result of (this / rhs) and whose scale is specified.
    *
    * @param rhs the value used to divide the receiver
    * @param scale the scale used for the result
    * @param rm the rounding constant
    *
    * @return     result of this/rhs.
    * @exception  ArithmeticException     division by zero.
    * @exception  IllegalArgumentException if the roundingMode is not valid.
    */
   public BigDecimal divide(BigDecimal rhs, int scale, int rm){

      //this will throw an exception if an illegal rm
      java.math.RoundingMode.valueOf(rm);

      BigDecimal res = new BigDecimal();
      BigDecimal lhsClone = this;
      BigDecimal rhsClone = rhs;

      // SLOW - PATH
      if (((this.flags & ~0xFFFFFFFC ) == 0x00000000)){
         lhsClone = clone(this);
         lhsClone.DFPToBI();
      }

      if (((rhs.flags & ~0xFFFFFFFC ) == 0x00000000)){
         rhsClone = clone(rhs);
         rhsClone.DFPToBI();
      }

      // we pass in LLs and BIs here
      lhsClone.divide(rhsClone,res,scale,rm);

      return res;
   }

   private final void divide(BigDecimal rhs, BigDecimal allocedRes, int scale, int rm){

      BigDecimal res=null;
      BigDecimal lhsClone=null;
      BigDecimal rhsClone=null;

      // if both are LL
      if (((this.flags & rhs.flags & 0x00000001) == 0x00000001)){ //LL
         res = longScaledDivide(rhs, allocedRes, scale, rm);
      }

      // longScaledDivide failed
      if (res == null){
         lhsClone = clone(this);
         rhsClone = clone(rhs);
         lhsClone.LLToBI();
         rhsClone.LLToBI();
         lhsClone.slScaledDivide(rhsClone, allocedRes, rm,scale);
      }
   }

  /**
    * Answers the result of (this / rhs).
    *
    * @param rhs the value used to divide the receiver
    *
    * @return  result of this/rhs.
    */
   public BigDecimal divide(BigDecimal rhs){

      BigDecimal lhs = this;
      BigDecimal res = new BigDecimal();
      BigDecimal lhsClone = lhs;
      BigDecimal rhsClone = rhs;

      /*
       * ArithmeticException - if the result is inexact but the rounding mode is
       * UNNECESSARY or mc.precision == 0 and the quotient has a
       * non-terminating decimal expansion.
       *
       */

      if (DFPHWAvailable() && (((this.flags | rhs.flags) & ~0xFFFFFFFC ) == 0x00000000)){
         if(DFPDivideHelper(res, this, rhs)) {
            return res;
         }
      }

      // SLOW - PATH
      if (((this.flags & ~0xFFFFFFFC ) == 0x00000000)){
         lhsClone = clone(this);
         lhsClone.DFPToBI();
      }

      if (((rhs.flags & ~0xFFFFFFFC ) == 0x00000000)){
         rhsClone = clone(rhs);
         rhsClone.DFPToBI();
      }

      // we pass in LLs and BIs here
      lhsClone.divide(rhsClone,res,MathContext.UNLIMITED);
      return res;
   }

   /**
    * Answers the result of (this / rhs).
    *
    * @param rhs the value used to divide the receiver
    * @param set defines the precision and rounding behaviour
    *
    * @return  result of this/rhs.
    */
   public BigDecimal divide(BigDecimal rhs, MathContext set){

      BigDecimal lhs = this;
      BigDecimal res = new BigDecimal();
      BigDecimal thisClone = lhs;
      BigDecimal rhsClone = rhs;

      /*
       * ArithmeticException - if the result is inexact but the rounding mode is
       * UNNECESSARY or mc.precision == 0 and the quotient has a
       * non-terminating decimal expansion.
       *
       */
      int prec = set.getPrecision();
      if (prec == 0)
         return lhs.divide(rhs); //same thing as degenerate case
      else{

         if (DFPHWAvailable() && DFPPerformHysteresis() && set.getPrecision() == 16 &&
            set.getRoundingMode().ordinal() == BigDecimal.ROUND_HALF_EVEN){
            DFPPerformHysteresis(10);
         }

         // we can and out the flags because we don't have
         // a sense of the exponent/precision/sign
         // of this result, nor do we want to use DFP hw
         // to figure it out, so we do not cache anything

         /*
          * Interpreted return value:
          * Returns -1 if not JIT compiled.
          *
          * JIT compiled return value:
          * Return 1 if JIT compiled and DFP divide is successful.
          * Return 0 if JIT compiled, but DFP divide was inexact
          * Return -2 if JIT compiled, but other exception (i.e. overflow)
          */

         if (DFPHWAvailable() && DFPUseDFP()){
            thisClone = clone(this);
            rhsClone = clone(rhs);

            //convert to DFP
            postSetScaleProcessing(thisClone);
            postSetScaleProcessing(rhsClone);

         }

         if (DFPHWAvailable() && (((thisClone.flags | rhsClone.flags) & ~0xFFFFFFFC ) == 0x00000000)){
            if(DFPDivideHelper(res, thisClone, rhsClone, set)) {
               return res;
            }
         }

         if (((thisClone.flags & ~0xFFFFFFFC ) == 0x00000000)){
            thisClone = clone(thisClone);
            thisClone.DFPToBI();
         }

         if (((rhsClone.flags & ~0xFFFFFFFC ) == 0x00000000)){
            rhsClone = clone(rhsClone);
            rhsClone.DFPToBI();
         }

         // we pass in LLs and BIs here
         thisClone.divide(rhsClone,res,set);

         postSetScaleProcessing(res);
         return res;
      }
   }

   private final void divide(BigDecimal rhs, BigDecimal allocedRes, MathContext set){

      BigDecimal res=null;
      BigDecimal lhsClone=null;
      BigDecimal rhsClone=null;
      // if both operands are LL
      if (((this.flags & rhs.flags & 0x00000001) == 0x00000001)){ //LL
         res = longPrecisionDivide(rhs, allocedRes, set);
      }

      // longPrecisionDivide failed
      if (res == null){
         lhsClone = clone(this);
         rhsClone = clone(rhs);
         lhsClone.LLToBI();
         rhsClone.LLToBI();
         lhsClone.slPrecisionDivide(rhsClone,allocedRes, set);
      }
   }

   /**
    * Answers the remainder of the receiver divided with the given argument.
    *
    * @param rhs the divisor
    * @return the value of (this % rhs)
    */
   public BigDecimal remainder(BigDecimal rhs){
      BigDecimal first = this.divideToIntegralValue(rhs);
      BigDecimal second = first.multiply(rhs);
      BigDecimal third = this.subtract(second);
      return third;
   }

   /**
    * Answers the remainder of the receiver divided with the given argument.
    *
    * @param rhs the divisor
    * @param set defines the precision and rounding behaviour
    * @return the value of (this % rhs)
    */
   public BigDecimal remainder(BigDecimal rhs,MathContext set){
      BigDecimal first = this.divideToIntegralValue(rhs,set);
      BigDecimal second = first.multiply(rhs);
      BigDecimal third = this.subtract(second);
      return third;
   }

   /**
    * Answers an array with the result of the division
    * and the remainder of the division.
    *
    * @param rhs the divisor
    * @return an array with 2 BigDecimals
    */
   public BigDecimal [] divideAndRemainder(BigDecimal rhs){
      BigDecimal [] ret= new BigDecimal[2];
      ret[0] = divideToIntegralValue(rhs);
      ret[1] = this.subtract(ret[0].multiply(rhs));
      return ret;
   }

   /**
    * Answers an array with the result of the division
    * and the remainder of the division.
    *
    * @param rhs the divisor
    * @param set defines the precision and rounding behaviour
    * @return an array with 2 BigDecimals
    */
   public BigDecimal [] divideAndRemainder(BigDecimal rhs,MathContext set){
      BigDecimal [] ret= new BigDecimal[2];
      ret[0] = divideToIntegralValue(rhs,set);
      ret[1] = this.subtract(ret[0].multiply(rhs));
      return ret;
   }

   /**
    * Answers the integer part of the receiver divided by the given argument.
    *
    * @param rhs the divisor
    * @return the integer result of (this / rhs) rounded down
    */
   public BigDecimal divideToIntegralValue(BigDecimal rhs){
      // check for divide by 0
      if (rhs.signum() == 0) {
         badDivideByZero();
      }

      int desiredScale = (int)Math.max(Math.min((long)this.scale() -
            (long)rhs.scale(),Integer.MAX_VALUE), Integer.MIN_VALUE);
      BigDecimal absLHS = this.abs();
      BigDecimal absRHS = rhs.abs();

      //if numerator > denominator -- return 0, desired scale
      if (absLHS.compareTo(absRHS) < 0)
         return BigDecimal.valueOf(0, desiredScale);

      //otherwise precision based divide
      else{
         int numDigitsL = this.precision();
         int numDigitsR = rhs.precision();

         long desiredPrecision=(long)Math.min(numDigitsL +
               Math.ceil(10.0*numDigitsR/3.0),Integer.MAX_VALUE);

         desiredPrecision+=Math.min(
               Math.abs((long)this.scale()-(long)rhs.scale())+2L+desiredPrecision,
               Integer.MAX_VALUE);

         if (desiredPrecision > Integer.MAX_VALUE) {
            scaleOverflow();
         }

         MathContext mc = new java.math.MathContext((int)desiredPrecision,RoundingMode.DOWN);
         BigDecimal res= this.divide(rhs, mc);
         // res will contain the exact result with decimal digits...
         // i.e. 2.4 / 1  = 2.4

         // step 1 - get rid of decimal digits
         int resScale = res.scale();
         if (resScale > 0)
            res = res.setScale(0, RoundingMode.DOWN);
         // step 2 - get as close to desiredScale as possible..
         if (desiredScale > 0){
            res = res.setScale(desiredScale);
         }
         else{ //desired scale will be < 0 here

            int scaleDiff = Math.abs(desiredScale-res.scale());
            if (scaleDiff != 0){

               // don't have any code here to work with DFP, so convert
               // to something we can work with (i.e. LL)
               if ((res.flags & 0x00000003) == 0)
                  res.DFPToLL();

               // in this case, we need to chop off trailing digits
               // from the integral value..
               // can do this without respect to precision, since
               // we're removing digits, not adding...
               if ((res.flags & 0x00000001) == 0x00000001 ){ //LL
                  long temp = res.laside;
                  int tempSc = res.cachedScale;
                  while (temp%10 == 0 && scaleDiff > 0 ){
                     temp/=10;
                     tempSc--;
                     scaleDiff--;
                  }
                  res.laside = temp;
                  res.cachedScale = tempSc;
               }
               else{ //BI
                  if((res.flags & 0x3) == 0x0) {
                     res.DFPToBI();
                  }
                  BigInteger temp = res.bi;
                  int tempSc = res.cachedScale;
                  while (temp.mod(BigInteger.TEN).equals(BigInteger.ZERO) && scaleDiff > 0 ){
                     temp = temp.divide(BigInteger.TEN);
                     tempSc--;
                     scaleDiff--;
                  }
                  res.bi = temp;
                  res.cachedScale = tempSc;
               }
               // clear out precision cuz we might have chopped 0s
               res.flags&=0xFFFFFFEF; //clear prec cache bit
            }
         }

         return res;
      }
   }

   /**
    * Answers the integer part of the receiver divided by the given argument.
    *
    * @param rhs the divisor
    * @param set defines the precision and rounding behaviour
    * @return the integer result of (this / rhs) rounded according to set
    */
   public BigDecimal divideToIntegralValue(BigDecimal rhs,MathContext set){

      if (set.getPrecision() == 0)
         return divideToIntegralValue(rhs);

      int desiredScale = (int)Math.max(Math.min((long)this.scale() -
            (long)rhs.scale(),Integer.MAX_VALUE), Integer.MIN_VALUE);
      BigDecimal absLHS = this.abs();
      BigDecimal absRHS = rhs.abs();

      //if numerator > denominator -- return 0, desired scale
      if (absLHS.compareTo(absRHS) < 0)
         return BigDecimal.valueOf(0, desiredScale);

      //otherwise precision based divide
      else{
         MathContext mc = new java.math.MathContext((int)set.getPrecision(),RoundingMode.DOWN);
         BigDecimal res= this.divide(rhs, mc);

         // res will contain the exact result with decimal digits...
         // i.e. 2.4 / 1  = 2.4

         // step 1 - get rid of decimal digits
         int resScale = res.scale();
         if (resScale > 0)
            res = res.setScale(0, RoundingMode.DOWN);

         // step 2 - get as close to desiredScale as possible..
         if (desiredScale > 0 && set.getPrecision() == 0)
            res = res.setScale(desiredScale);
         else if (desiredScale >= 0){
            // ex. if precision = 9, and desiredScale is 3
            // we can go the full mile...
            // ex. if precision = 9 and desiredScale is 10,
            // we can only go upt to 9
            int diffOfPrecision = set.getPrecision() - res.precision();
            if (diffOfPrecision > 0)
               res = res.setScale(Math.min(diffOfPrecision,desiredScale));
         }
         else{ //desired scale will be < 0 here

            int scaleDiff = Math.abs(desiredScale-res.scale());
            if (scaleDiff != 0){

               // don't have any code here to work with DFP, so convert
               // to something we can work with (i.e. LL)
               if ((res.flags & 0x00000003) == 0)
                  res.DFPToLL();

               // in this case, we need to chop off trailing digits
               // from the integral value..
               // can do this without respect to precision, since
               // we're removing digits, not adding...
               if ((res.flags & 0x00000001) == 0x00000001 ){ //LL
                  long temp = res.laside;
                  int tempSc = res.cachedScale;
                  while (temp%10 == 0 && scaleDiff > 0 ){
                     temp/=10;
                     tempSc--;
                     scaleDiff--;
                  }
                  res.laside = temp;
                  res.cachedScale = tempSc;
               }
               else{ //BI
                  if((res.flags & 0x3) == 0x0) {
                     res.DFPToBI();
                  }
                  BigInteger temp = res.bi;
                  int tempSc = res.cachedScale;
                  while (temp.mod(BigInteger.TEN).equals(BigInteger.ZERO) && scaleDiff > 0 ){
                     temp = temp.divide(BigInteger.TEN);
                     tempSc--;
                     scaleDiff--;
                  }
                  res.bi = temp;
                  res.cachedScale = tempSc;
               }
               // clear out precision cuz we might have chopped 0s
               res.flags&=0xFFFFFFEF; //clear prec cache bit
            }
         }

         // initial check to see if we can't represent the integral value
         BigDecimal rem = this.subtract(rhs.multiply(res));
         if (rem.abs().compareTo(absRHS)>=0) {
            // math.34 = Integral division is inexact
            throw new ArithmeticException(Messages.getString("math.34")); //$NON-NLS-1$
         }

         //finally check to see if we have more digits then precision..
         if (set.getPrecision() > 0 && res.precision() > set.getPrecision()) {
            // math.35 = Requires more than prec digits
            throw new ArithmeticException(Messages.getString("math.35")); //$NON-NLS-1$
         }

         return res;
      }
   }

   private final BigDecimal longPrecisionDivide(BigDecimal rhs,BigDecimal allocedRes,
         MathContext set){

      BigDecimal res=null;

      long scaleL=this.cachedScale;
      long scaleR=rhs.cachedScale;
      long origScaleL = scaleL;
      long origScaleR = scaleR;

      int tempInd = this.signum() * rhs.signum();
      long newL = this.laside;
      long newR = rhs.laside;
      boolean flag = true;
        if (newL < 0L)
           {
           flag = false;
           newL*=-1;
           }
      if (newR < 0L)
           {
           flag = false;
           newR*=-1;
           }
      long numDigitsL = this.precision();
      long numDigitsR = rhs.precision();

      long digitDiff=0;
      long pow=0;
      boolean checkForNTE=false;
      int desiredPrecision = set.getPrecision();
      boolean closerScale=true;

      // want to get as close to this as possible
      long preferredScale =0;

      if (rhs.laside == 0){
         if (this.laside == 0) {
            // math.24 = Division is undefined
            throw new ArithmeticException(Messages.getString("math.24")); //$NON-NLS-1$
         }
         badDivideByZero();
      }

      //Used to throw ArithmeticException
      if (desiredPrecision == 0){

         preferredScale = (int)Math.max(Math.min((long)this.scale() - (long)rhs.scale(),
                    Integer.MAX_VALUE), Integer.MIN_VALUE);

         desiredPrecision=(int)Math.min(numDigitsL +
               Math.ceil(10.0*numDigitsR/3.0),Integer.MAX_VALUE);
         checkForNTE= true;
      }
      else
         preferredScale = (long)origScaleL - (long)origScaleR;

      if (this.laside== 0L){
         clone(allocedRes,this);
         allocedRes.cachedScale = (int)Math.max(Math.min(preferredScale, Integer.MAX_VALUE),Integer.MIN_VALUE);
         return allocedRes;
      }

      /*
       * Normalize so the result is between 0.1 and 1.
       * This is done so that we can use the required precision
       * interchanbeably with a positive scale (i.e # of digits
       * required after the decimal).
       *
       * Need to make both have the same # of digits
       * and if numerator > denominator, need to scale
       * denominator by a factor of 10.
       */
      if (numDigitsL > numDigitsR){
         digitDiff = numDigitsL - numDigitsR;
         pow = powerOfTenLL(digitDiff);
         if (pow == -1)
            return null;
         if (overflowMultiply(newR,pow))
            return null;
         newR *=pow;
         scaleR+=digitDiff;
      }
      else if (numDigitsL < numDigitsR){
         digitDiff = numDigitsR - numDigitsL;
         pow = BigDecimal.powerOfTenLL(digitDiff);
         if (pow == -1)
            return null;
         if (overflowMultiply(newL,pow))
            return null;
         newL *=pow;
         scaleL +=digitDiff;
      }

      //one more adjustment.. i.e. 33/10
      if (newL > newR){
         if (overflowMultiply(newR,10))
            return null;
         newR *=10;
         scaleR +=1;
      }

           long quotient=0;
           long remainder=0;
           /* Set up for precision-based division */
           pow = powerOfTenLL(desiredPrecision);
           if (pow == -1)
              return null;

           if (overflowMultiply(newL,pow))
              {
              if(numDigitsL > 2 && numDigitsR > 2 && desiredPrecision<17 && numDigitsL<17 && numDigitsR<17 && flag && (!checkForNTE)){
              long q = 0;
              int modifiedPrecision = desiredPrecision/2;
              int evenFlag = desiredPrecision - (modifiedPrecision * 2);

              if(evenFlag!=0)
                 {
                 newL *= 10;
                 q=newL/newR;
                 remainder = newL - q * newR;
                 quotient = (quotient * 10)+ q;
                 newL = remainder;
                 }

                 newL *= 100;
                 for(int i = 0; i < modifiedPrecision; ++i)
                 {
                 q = newL/newR;
                 remainder = newL - q * newR;
                 quotient = (quotient * 100) + q;
                 newL = remainder * 100;
                 }

                 long roundedQuotient = roundPostLLDivision((quotient),tempInd,newR,
                 remainder,set.getRoundingMode().ordinal());
                 int numDigits= numDigits(quotient);
                 int numDigitsAgain = numDigits(roundedQuotient);
                 boolean roundEffect = false;

                 // Not covering case when 999 to 1000
                 if(numDigitsAgain>numDigits)
                    return null;

                 int scale2=(int)(scaleL- scaleR+ desiredPrecision);
                 res = allocedRes;
                 res.laside = (roundedQuotient);
                 res.cachedScale = scale2;

                 // cache - SGP ESRR
                 //set LL representation
                 res.flags &= 0xFFFFFFFC; //clear rep bits
                 res.flags |= 0x00000001;
                 return res;
              }
            return null;
           }
      newL *=pow;

      // Perform the divide
          quotient = newL/newR;
          remainder= newL - (quotient*newR); //what's left of the dividend
      if (remainder!=0)
         closerScale=false;

      if (checkForNTE && remainder !=0) {
         // math.28 = Non-terminating decimal expansion
         throw new ArithmeticException(Messages.getString("math.28")); //$NON-NLS-1$
      }
      if (desiredPrecision != 0 && set.getRoundingMode().ordinal() == BigDecimal.ROUND_UNNECESSARY && remainder !=0) {
         // math.29 = Inexact result requires rounding
         throw new ArithmeticException(Messages.getString("math.29")); //$NON-NLS-1$
      }

      //convert the quotient and remainder to positive values..
      long tempQuotient = quotient;
      long tempRemainder = remainder;
      long newnewR = newR;
      if (tempQuotient < 0L)tempQuotient*=-1L;
      if (tempRemainder < 0L)tempRemainder*=-1L;
      if (newR < 0L) newnewR*=-1L;
      int numDigits= numDigits(tempQuotient);
      boolean roundEffect=false;

      long roundedQuotient = roundPostLLDivision(tempQuotient,tempInd,newnewR,
            tempRemainder,set.getRoundingMode().ordinal());

      //the current scale of the result
      long scale=scaleL-scaleR+desiredPrecision;

      if (roundedQuotient != tempQuotient){
         if (roundedQuotient%10 == 0 &&
               tempQuotient%10 != 0)
            roundEffect=true; //rounding did have an affect, changing
         //from XX to X0
      }

      //Couldn't perform LL round
      if (roundedQuotient == -1)
         return null;

      /* Need to handle case where rounding changed from 9 to 10 */
      int numDigitsAgain = numDigits(roundedQuotient);
      if (numDigits < numDigitsAgain && (numDigitsAgain > desiredPrecision)){
         roundedQuotient/=10;
         scale--;
      }

      long scaleDiff = scale - preferredScale;

      //only check this case when user passed in infinite precision
      //why?  because we can pad as long as we want...
      //if the user passed in a specific precision, then the
      //result is already the required length we want so can't pad
      if (checkForNTE && scale < preferredScale){ //we need to pad with 0s
         scaleDiff = preferredScale - scale;
         pow = powerOfTenLL(scaleDiff);
         if (pow == -1)
            return null;
         if (overflowMultiply(roundedQuotient,pow))
            return null;
         scale+=scaleDiff; //increase the scale
         roundedQuotient *=pow;
      }

      //only check this case when rounding did not have an affect
      //otherwise rounding may have caused 89 to become 90
      //or if we have more digits than precision gives us...
      if (scale > preferredScale && !roundEffect){ //we need to chop off trailing 0s

         if (closerScale){
            //we're performing decimal precision based divide...
            while(roundedQuotient%10==0 && scaleDiff>0 ){ //scaleDiff>0 so that we don't go infinitely
               roundedQuotient /=10;
               scale--; //decrease the scale
               scaleDiff--;
            }

         //if we want to take the scale down further..
         if ((roundedQuotient==0 && scaleDiff >0)) //cuz we don't want decimal digits
            scale-=scaleDiff;
         }
      }

      //check the scale..
      if (scale < (long)Integer.MIN_VALUE || scale > (long)Integer.MAX_VALUE) {
         scaleOutOfRange(scale);
      }

      //make sure to place back into correct form..
      //do this here to alleviate algos above from dealing with it
      if (tempInd ==-1 )
         roundedQuotient*=-1L;

      res = allocedRes;
      res.laside = roundedQuotient;
      res.cachedScale = (int)scale;

      /* cache - SGP ESRR */

      //set LL representation
      res.flags &= 0xFFFFFFFC; //clear rep bits
      res.flags |= 0x00000001;

      // set precision
      res.flags &= 0x7F ; //clear prec
      res.flags |= numDigits(res.laside) << 7;

      return res;
   }

   private final BigDecimal longScaledDivide(BigDecimal rhs,
         BigDecimal allocedRes,int desiredScale, int rm){

      BigDecimal res=null;

      if (rhs.laside == 0){
         if (this.laside == 0) {
            // math.24 = Division is undefined
            throw new ArithmeticException(Messages.getString("math.24")); //$NON-NLS-1$
         }
         badDivideByZero();
      }

      if (this.laside== 0L){
         res = allocedRes;
         res.laside= 0;
         res.cachedScale = desiredScale;

         //set LL representation
         res.flags &= 0xFFFFFFFC ; //clear rep bits
         res.flags |= 0x00000001;

         //set precision
         res.flags &= 0x7F; //clear prec bits
         res.flags |= 1 << 7;
      }

      int tempInd =this.signum()*rhs.signum();
      long lhsL = this.laside;
      long rhsL = rhs.laside;
      if (lhsL < 0L)lhsL*=-1;
      if (rhsL < 0L)rhsL*=-1;

      //Setup for quotient & remainder calculations
      long naturalScale = (long)this.cachedScale-(long)rhs.cachedScale;
      long pow=0;
      long scaleDiff=0;
      long newL = lhsL;
      long newR = rhsL;

      if (naturalScale < desiredScale){
         scaleDiff = (long)desiredScale - naturalScale;
         pow = powerOfTenLL(scaleDiff);
         if (pow == -1 )
            return null;
         if (overflowMultiply(newL,pow))
            return null;
         newL *=pow;
      }
      else if (naturalScale > desiredScale){
         scaleDiff = naturalScale - (long)desiredScale;
         pow = powerOfTenLL(scaleDiff);
         if (pow == -1)
            return null;
         if (overflowMultiply(newR,pow))
            return null;
         newR *=pow;
      }

      //calculate the quotient
      long quotient = newL/newR;
      long remainder= newL - (quotient*newR); //what's left of the dividend

      //convert the quotient and remainder to positive values..
      long tempQuotient = quotient;
      long tempRemainder = remainder;
      long newnewR=newR;
      if (tempQuotient < 0L)tempQuotient*=-1L;
      if (tempRemainder < 0L)tempRemainder*=-1L;
      if (newR < 0L) newnewR*=-1L;

      //perform rounding
      long roundedQuotient =
         roundPostLLDivision(tempQuotient,tempInd,newnewR,tempRemainder,rm);

      //Couldn't perform LL round
      if (roundedQuotient == -1)
         return null;

      if (tempInd ==-1)
         roundedQuotient*=-1L;

      res = allocedRes;
      res.laside = roundedQuotient;
      res.cachedScale = desiredScale;

      //set LL representation
      res.flags &= 0xFFFFFFFC ; //clear rep bits
      res.flags |= 0x00000001;

      //set precision
      res.flags &= 0x7F; //clear prec bits
      res.flags |= numDigits(res.laside) << 7;

      return res;
   }

   private final BigDecimal slPrecisionDivide(BigDecimal rhs,
         BigDecimal allocedRes,MathContext set){

      BigDecimal res=null;

      long scaleL=this.cachedScale;
      long scaleR=rhs.cachedScale;
      long origScaleL = scaleL;
      long origScaleR = scaleR;

      int tempInd = rhs.signum()*this.signum();
      int numDigitsL = this.precision();
      int numDigitsR = rhs.precision();

      long digitDiff=0;
      boolean checkForNTE=false;
      int desiredPrecision = set.getPrecision();
      boolean closerScale=true;

      // want to get as close to this as possible
      long preferredScale = 0;

      // check for divide by 0
      if (rhs.bi.signum() == 0){
         if (this.bi.signum() == 0) {
            // math.24 = Division is undefined
            throw new ArithmeticException(Messages.getString("math.24")); //$NON-NLS-1$
         }
         badDivideByZero();
      }

      //Used to throw ArithmeticException
      if (desiredPrecision == 0){
         preferredScale = (int)Math.max(Math.min((long)this.scale() - (long)rhs.scale(),
                                                       Integer.MAX_VALUE), Integer.MIN_VALUE);

         desiredPrecision=(int)Math.min(numDigitsL +
               Math.ceil(10.0*numDigitsR/3.0),Integer.MAX_VALUE);
         checkForNTE= true;
      }
      else
         preferredScale =(long)origScaleL - (long)origScaleR;

      if (this.bi.signum()== 0L){
         clone(allocedRes,this);
         allocedRes.cachedScale = (int)Math.max(Math.min(preferredScale, Integer.MAX_VALUE),Integer.MIN_VALUE);
         return allocedRes;
      }

      /*
       * Normalize so the result is between 0.1 and 1.
       * This is done so that we can use the required precision
       * interchanbeably with a positive scale (i.e # of digits
       * required after the decimal).
       *
       * Need to make both have the same # of digits
       * and if numerator > denominator, need to scale
       * denominator by a factor of 10.
       */

      BigInteger newLBI = this.bi.abs();
      BigInteger newRBI = rhs.bi.abs();

      if (numDigitsL > numDigitsR){
         digitDiff = numDigitsL - numDigitsR;
         if (digitDiff >= Integer.MAX_VALUE) {
            scaleOverflow();
         }
         newRBI = newRBI.multiply(powerOfTenBI(digitDiff));
         scaleR+=digitDiff;
      }
      else if (numDigitsL < numDigitsR){
         digitDiff = numDigitsR - numDigitsL;
         if (digitDiff >= Integer.MAX_VALUE) {
            scaleOverflow();
         }
         newLBI = newLBI.multiply(powerOfTenBI(digitDiff));
         scaleL +=digitDiff;
      }

      //one more adjustment.. i.e. 33/10
      if (newLBI.compareTo(newRBI) > 0){
         newRBI = newRBI.multiply(BigInteger.TEN);
         scaleR +=1;
      }

      // Set up for precision-based division
      if (desiredPrecision >= Integer.MAX_VALUE) {
         scaleOverflow();
      }
      newLBI = newLBI.multiply(powerOfTenBI(desiredPrecision));

      // Perform the divide
      BigInteger []quotRem = newLBI.divideAndRemainder(newRBI);
      if (quotRem[1].signum()!=0)
         closerScale=false;

      if (checkForNTE && quotRem[1].signum()!=0) {
         // math.28 = Non-terminating decimal expansion
         throw new ArithmeticException(Messages.getString("math.28")); //$NON-NLS-1$
      }
      if (desiredPrecision != 0 && set.getRoundingMode().ordinal() == BigDecimal.ROUND_UNNECESSARY && quotRem[1].signum()!=0) {
         // math.29 = Inexact result requires rounding
         throw new ArithmeticException(Messages.getString("math.29")); //$NON-NLS-1$
      }

      boolean roundEffect=false;

      BigInteger roundedQuotient =
         roundPostSlowDivision(quotRem[0],tempInd,newRBI,
               quotRem[1],set.getRoundingMode().ordinal());

      //the current scale of the result
      long scale=scaleL-scaleR+desiredPrecision;

      // rounding occurred
      if (!roundedQuotient.equals(quotRem[0])){
         if (roundedQuotient.mod(BigInteger.TEN).equals(BigInteger.ZERO) &&
               !quotRem[1].mod(BigInteger.TEN).equals(BigInteger.ZERO))
            roundEffect=true; //rounding did have an affect, changing
         //from XX to X0
      }

      /* Need to handle case where rounding changed from 9 to 10 */
       if (((desiredPrecision==16) && (roundedQuotient.equals(powerOfTenBI(17))))||(desiredPrecision != 16)){
          int numDigits = precisionBI(quotRem[0]);
          int numDigitsAgain = precisionBI(roundedQuotient);

         if (numDigits < numDigitsAgain && (numDigitsAgain > desiredPrecision)){
             roundedQuotient = roundedQuotient.divide(BigInteger.TEN);
           scale--;
         }
       }
      long scaleDiff = scale - preferredScale;

      //only check this case when user passed in infinite precision
      //why?  because we can pad as long as we want...
      //if the user passed in a specific precision, then the
      //result is already the required length we want so can't pad
      if (checkForNTE && scale < preferredScale){ //we need to pad with 0s
         scaleDiff = preferredScale - scale;
         if (scaleDiff >= Integer.MAX_VALUE || scale > Integer.MAX_VALUE ||
                  scale < Integer.MIN_VALUE) {
            scaleOverflow();
         }
          roundedQuotient = roundedQuotient.multiply(powerOfTenBI(scaleDiff));
         scale+=scaleDiff; //increase the scale
      }

      //only check this case when rounding did not have an affect
      //otherwise rounding may have caused 89 to become 90
      //or if we have more digits than precision gives us...
      if (scale > preferredScale && !roundEffect){ //we need to chop off trailing 0s

         if (closerScale){
            while(roundedQuotient.mod(BigInteger.TEN).equals(BigInteger.ZERO) && scaleDiff>0 ){ //scaleDiff>0 so that we don't go infinitely
               roundedQuotient = roundedQuotient.divide(BigInteger.TEN);
               scale--;
               scaleDiff--;
            }

            //if we want to take the scale down further..
            if ((roundedQuotient.equals(BigInteger.ZERO) && scaleDiff >0)) //cuz we don't want decimal digits
               scale-=scaleDiff;
         }
      }

      //check the scale..
      if (scale < (long)Integer.MIN_VALUE || scale > (long)Integer.MAX_VALUE) {
         scaleOutOfRange(scale);
      }

      //make sure to place back into correct form..
      //do this here to alleviate algos above from dealing with it
      if (tempInd == -1 )
         roundedQuotient = roundedQuotient.negate();

      res = allocedRes;
      res.bi = roundedQuotient;
      res.cachedScale = (int)scale;

      /* cache - SGP ESRR */

      //set BI representation
      res.flags &= 0xFFFFFFFC; //clear rep bits
      res.flags |= 0x00000002;

      //cache the sign
      res.flags|=0x4;
      res.flags&=0xFFFFFF9F; //clear the cached sign bits
      res.flags|=( tempInd << 5) & 0x60;

      postSetScaleProcessing(res);
      return res;
   }

   private final void slScaledDivide(BigDecimal rhs,
         BigDecimal allocedRes,int rm,int desiredScale){

      BigDecimal res=null;

      if (rhs.bi.signum() == 0){
         if (this.bi.signum() == 0) {
            // math.24 = Division is undefined
            throw new ArithmeticException(Messages.getString("math.24")); //$NON-NLS-1$
         }
         badDivideByZero();
      }

      if (this.bi.signum()== 0L){
         res = allocedRes;
         res.bi= BigInteger.ZERO;
         res.cachedScale = desiredScale;

         /* cache - SGP ESRR */

         //set BI representation
         res.flags &= 0xFFFFFFFC; //clear rep bits
         res.flags |= 0x00000002;

         //cache the sign
         res.flags|=0x4;
         res.flags&=0xFFFFFF9F; //clear the cached sign bits
         return;
      }

      int tempInd = this.bi.signum()*rhs.bi.signum();

      BigInteger lhsBI = this.bi.abs();
      BigInteger rhsBI = rhs.bi.abs();

      //Setup for quotient & remainder calculations
      long naturalScale = (long)this.cachedScale-(long)rhs.cachedScale;
      long scaleDiff=0;
      BigInteger newLBI= lhsBI;
      BigInteger newRBI = rhsBI;

      long rhsCheck = (long)rhs.cachedScale + desiredScale;
      long thisCheck = (long)this.cachedScale - desiredScale;

      if (naturalScale < desiredScale){
         scaleDiff = (long)desiredScale - naturalScale;
         if (scaleDiff > Integer.MAX_VALUE ||
               rhsCheck > Integer.MAX_VALUE ||
               thisCheck > Integer.MAX_VALUE) {
            scaleOverflow();
         } else if (rhsCheck < Integer.MIN_VALUE) {
            // math.22 = Scale underflow
            throw new ArithmeticException(Messages.getString("math.22")); //$NON-NLS-1$
         }
         newLBI = newLBI.multiply(powerOfTenBI(scaleDiff));
      }
      else if (naturalScale > desiredScale){
         scaleDiff = naturalScale - (long)desiredScale;
         if (scaleDiff > Integer.MAX_VALUE ||
               thisCheck > Integer.MAX_VALUE ||
               rhsCheck > Integer.MAX_VALUE) {
            scaleOverflow();
         } else if ((long)this.cachedScale - desiredScale < Integer.MIN_VALUE ||
               rhsCheck < Integer.MIN_VALUE) {
            // math.22 = Scale underflow
            throw new ArithmeticException(Messages.getString("math.22")); //$NON-NLS-1$
         }
         newRBI = newRBI.multiply(powerOfTenBI(scaleDiff));
      }

      //calculate the quotient and remainder
      BigInteger [] quotRem= newLBI.divideAndRemainder(newRBI);

      // perform rounding
      BigInteger roundedQuotient = roundPostSlowDivision(
            quotRem[0], tempInd, newRBI, quotRem[1], rm);

      if (tempInd == -1)
         roundedQuotient = roundedQuotient.negate();

      // set up the result
      res = allocedRes;
      res.bi = roundedQuotient;
      res.cachedScale = desiredScale;

      //set BI representation
      res.flags &= 0xFFFFFFFC; //clear rep bits
      res.flags |= 0x00000002;

      //cache the sign
      res.flags|=0x4;
      res.flags&=0xFFFFFF9F; //clear the cached sign bits
      res.flags|=( tempInd << 5) & 0x60;

      postSetScaleProcessing(res);
   }

   /**
    * Answers the absolute value of this BigDecimal.
    *
    * @return  the absolute value of the receiver.
    */
   public BigDecimal abs(){
      return this.abs(MathContext.UNLIMITED);
   }

   /**
    * Answers the absolute value of this BigDecimal.
    *
    * @param set defines the precision and rounding behaviour
    *
    * @return  the absolute value of the receiver.
    */
   public BigDecimal abs(MathContext set){
      if (this.signum() < 0)
         return this.negate(set);
      return this.plus(set);
   }

   /**
    * Answers the larger value between the receiver and this BigDecimal.
    *
    * @param rhs the value used to compare
    *
    * @return  the larger BigDecimal
    */
   public BigDecimal max(BigDecimal rhs){
      int res=this.compareTo(rhs);
      if (res>0)
         return this.plus();
      else if (res==0)
         return this;
      else
         return rhs.plus();
   }

   /**
    * Answers the smaller value between the receiver and this BigDecimal.
    *
    * @param rhs the value used to compare
    *
    * @return  the smaller BigDecimal
    */
   public BigDecimal min(BigDecimal rhs){
      int res=this.compareTo(rhs);
      if (res <0)
         return this.plus();
      else if (res ==0)
         return this;
      else
         return rhs.plus();
   }

   /**
    * Negates this BigDecimal value.
    *
    * @return  a new BigDecimal with value negated.
    */
   public BigDecimal negate(){
      return this.negate(MathContext.UNLIMITED);
   }

   /**
    * Negates this BigDecimal value.
    *
    * @param set defines the precision and rounding behaviour
    * @return  a new BigDecimal with value negated.
    */
   public BigDecimal negate(MathContext set){

      BigDecimal res = clone(this);

      /* cache - SGP ESRR */

      if (DFPHWAvailable() && ((res.flags & ~0xFFFFFFFC) == 0x00000000)){  //DFP
         DFPNegateHelper(res);
      }
      else if ((this.flags & ~0xFFFFFFFC) == 0x00000002) {//BI
         res.bi = this.bi.negate();
      }
      else { //LL
         if((res.flags & 0x3) == 0x0) {
            res.DFPToLL();
         }
         res.laside *=-1;
         if (res.bi != null)
            res.bi = res.bi.negate();
      }
      if (set != MathContext.UNLIMITED)
         res.finish(set.getPrecision(), set.getRoundingMode().ordinal());

      return res;
   }

   /**
    * Answers a BigDecimal with the +value of the receiver and the receiver's
    * scale.
    *
    * @return a BigDecimal with the +value of the receiver
    */
   public BigDecimal plus(){
      return this.plus(MathContext.UNLIMITED);
   }

   /**
    * Answers a BigDecimal with the +value of the receiver and the rounding
    * mode given in the Math context.
    *
    * @param set defines the precision and rounding behaviour
    * @return a BigDecimal with the +value of the receiver
    */
   public BigDecimal plus(MathContext set){
      BigDecimal res;
      if (set == MathContext.UNLIMITED)
         return this;
      else{
         res = clone(this);
         if (set != MathContext.UNLIMITED)
            res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
         return res;
      }
   }

   /**
    * Converts this BigDecimal to a byte.
    *
    * @return  the byte representation of the receiver.
    * @throws ArithmeticException if value does not fit into a byte
    *       or has non-zero decimal digits
    */
   public byte byteValueExact(){

      /* range:  -128 to 127 */
      final int numDigits = 3;

      if (this.signum() == 0)
         return 0;

      BigDecimal thisClone = this;
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         thisClone = clone(this);
         thisClone.DFPToLL();
      }

      /*
       * Fast tests:
       *    (A) -for a positive scale: non-zero decimal digits
       *  (B) -overall length > 3
       *
       *  These tests eliminate every except the case where we're left
       *  with a numDigits number that falls outside the Byte range.
       */

      // perform common test (B) first
      long actualNumDigits = (long)thisClone.precision() - (long)thisClone.cachedScale;
      if (actualNumDigits < 0 || actualNumDigits> numDigits) {
         conversionOverflow(thisClone);
      }

      if ((thisClone.flags & 0x00000001) == 0x00000001) {//LL

         // test (A)
         if (thisClone.cachedScale > 0){
            long tenPow = powerOfTenLL(thisClone.cachedScale);
            if (tenPow != -1){
               if (thisClone.laside % tenPow != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
            else{ //the whole mantissa is a fraction
               if (thisClone.laside != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
         }
      }
      else{ //BI
         if((thisClone.flags & 0x3) == 0x0) {
            thisClone = clone(this);
            thisClone.DFPToBI();
         }
         // test (A)
         if (thisClone.cachedScale > 0){
            BigInteger tenPow = powerOfTenBI(thisClone.cachedScale);
            if (thisClone.bi.remainder(tenPow).compareTo(BigInteger.ZERO) != 0) {
               nonZeroDecimals(thisClone);
            }
         }
      }

      BigInteger num=thisClone.toBigInteger();
      if (num.compareTo(MAXBYTE) > 0 || num.compareTo(MINBYTE) < 0) {
         conversionOverflow(thisClone);
      }
      return num.byteValue();
   }

   /**
    * Converts this BigDecimal to a short.
    *
    * @return  the short representation of the receiver.
    * @throws ArithmeticException if value does not fit into a short
    *       or has non-zero decimal digits
    */
   public short shortValueExact(){

      /* range: -32768 to 32767 */

      final int numDigits = 5;

      if (this.signum() == 0)
         return 0;

      BigDecimal thisClone = this;
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         thisClone = clone(this);
         thisClone.DFPToLL();
      }

      /*
       * Fast tests:
       *    (A) -for a positive scale: non-zero decimal digits
       *  (B) -overall length > 3
       *
       *  These tests eliminate every except the case where we're left
       *  with a numDigits number that falls outside the Byte range.
       */

      // perform common test (B) first
      long actualNumDigits = (long)thisClone.precision() - (long)thisClone.cachedScale;
      if (actualNumDigits < 0 || actualNumDigits> numDigits) {
         conversionOverflow(thisClone);
      }

      if ((thisClone.flags & 0x00000001) == 0x00000001) {//LL

         // test (A)
         if (thisClone.cachedScale > 0){
            long tenPow = powerOfTenLL(thisClone.cachedScale);
            if (tenPow != -1){
               if (thisClone.laside % tenPow != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
            else{ //the whole mantissa is a fraction
               if (thisClone.laside != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
         }
      }
      else{ //BI
         if((thisClone.flags & 0x3) == 0x0) {
            thisClone = clone(this);
            thisClone.DFPToBI();
         }
         // test (A)
         if (thisClone.cachedScale > 0){
            BigInteger tenPow = powerOfTenBI(thisClone.cachedScale);
            if (thisClone.bi.remainder(tenPow).compareTo(BigInteger.ZERO) != 0) {
               nonZeroDecimals(thisClone);
            }
         }

      }

      BigInteger num=thisClone.toBigInteger();
      if (num.compareTo(MAXSHORT) > 0 || num.compareTo(MINSHORT) < 0) {
         conversionOverflow(thisClone);
      }
      return num.shortValue();
   }

   /**
    * Converts this BigDecimal to a int.
    *
    * @return  the int representation of the receiver.
    * @throws ArithmeticException if value does not fit into an int
    *       or has non-zero decimal digits
    */
   public int intValueExact(){

      /* range: -2147483648 to 2147483647 */

      final int numDigits = 10;

      if (this.signum() == 0)
         return 0;

      BigDecimal thisClone = this;
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         thisClone = clone(this);
         thisClone.DFPToLL();
      }

      /*
       * Fast tests:
       *    (A) -for a positive scale: non-zero decimal digits
       *  (B) -overall length > 3
       *
       *  These tests eliminate every except the case where we're left
       *  with a numDigits number that falls outside the Byte range.
       */

      // perform common test (B) first
      long actualNumDigits = (long)thisClone.precision() - (long)thisClone.cachedScale;
      if (actualNumDigits < 0 || actualNumDigits> numDigits) {
         conversionOverflow(thisClone);
      }

      if ((thisClone.flags & 0x00000001) == 0x00000001) {//LL

         // test (A)
         if (thisClone.cachedScale > 0){
            long tenPow = powerOfTenLL(thisClone.cachedScale);
            if (tenPow != -1){
               if (thisClone.laside % tenPow != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
            else{ //the whole mantissa is a fraction
               if (thisClone.laside != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
         }
      }
      else{ //BI
         if((thisClone.flags & 0x3) == 0x0) {
            thisClone = clone(this);
            thisClone.DFPToBI();
         }
         // test (A)
         if (thisClone.cachedScale > 0){
            BigInteger tenPow = powerOfTenBI(thisClone.cachedScale);
            if (thisClone.bi.remainder(tenPow).compareTo(BigInteger.ZERO) != 0) {
               nonZeroDecimals(thisClone);
            }
         }
      }

      BigInteger num=thisClone.toBigInteger();
      if (num.compareTo(MAXINT) > 0 || num.compareTo(MININT) < 0) {
         conversionOverflow(thisClone);
      }
      return num.intValue();
   }

   /**
    * Converts this BigDecimal to a long.
    *
    * @return  the long representation of the receiver.
    * @throws ArithmeticException if value does not fit into a long
    *       or has non-zero decimal digits
    */
   public long longValueExact(){

      /* range: -9223372036854775808 to 9223372036854775807 */

      final int numDigits = 19;

      if (this.signum() == 0)
         return 0;

      BigDecimal thisClone = this;
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         thisClone = clone(this);
         thisClone.DFPToLL();
      }

      /*
       * Fast tests:
       *    (A) -for a positive scale: non-zero decimal digits
       *  (B) -overall length > 3
       *
       *  These tests eliminate every except the case where we're left
       *  with a numDigits number that falls outside the Byte range.
       */

      // perform common test (B) first
      long actualNumDigits = (long)thisClone.precision() - (long)thisClone.cachedScale;
      if (actualNumDigits < 0 || actualNumDigits> numDigits) {
         conversionOverflow(thisClone);
      }

      if ((thisClone.flags & 0x00000001) == 0x00000001) {//LL

         // test (A)
         if (thisClone.cachedScale > 0){
            long tenPow = powerOfTenLL(thisClone.cachedScale);
            if (tenPow != -1){
               if (thisClone.laside % tenPow != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
            else{ //the whole mantissa is a fraction
               if (thisClone.laside != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
         }
      }
      else{ //BI
         if((thisClone.flags & 0x3) == 0x0) {
            thisClone = clone(this);
            thisClone.DFPToBI();
         }
         // test (A)
         if (thisClone.cachedScale > 0){
            BigInteger tenPow = powerOfTenBI(thisClone.cachedScale);
            if (thisClone.bi.remainder(tenPow).compareTo(BigInteger.ZERO) != 0) {
               nonZeroDecimals(thisClone);
            }
         }
      }

      BigInteger num=thisClone.toBigInteger();
      if (num.compareTo(MAXLONG) > 0 || num.compareTo(MINLONG) < 0) {
         conversionOverflow(thisClone);
      }
      return num.longValue();
   }

   /**
    * Converts this to a BigInteger.
    *
    * @return  the BigInteger equivalent of this BigDecimal.
    * @throws ArithmeticException if it has non-zero decimal digits
    */
   public BigInteger toBigIntegerExact(){

      if (this.signum() == 0)
         return BigInteger.valueOf(0L);

      // no fast path for this, convert to LL
      BigDecimal thisClone = this;
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         thisClone = clone(this);
         thisClone.DFPToLL();
      }

      if ((thisClone.flags & 0x00000001) == 0x00000001) {//LL

         // test (A)
         if (thisClone.cachedScale > 0){
            long tenPow = powerOfTenLL(thisClone.cachedScale);
            if (tenPow != -1){
               if (thisClone.laside % tenPow != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
            else{ // need to redo it in BigInteger form (d120991)
               BigInteger bi = BigInteger.valueOf(thisClone.laside);
               BigInteger biTenPow = powerOfTenBI(thisClone.cachedScale);
               if (bi.remainder(biTenPow).compareTo(BigInteger.ZERO) != 0) {
                  nonZeroDecimals(thisClone);
               }
            }
         }
      }
      else{ //BI
         if (((thisClone.flags & 0x3) == 0x0)){ //DFP
            thisClone = clone(this);
            thisClone.DFPToBI();
         }
         // test (A)
         if (thisClone.cachedScale > 0){
            BigInteger tenPow = powerOfTenBI(thisClone.cachedScale);
            if (thisClone.bi.remainder(tenPow).compareTo(BigInteger.ZERO) != 0) {
               nonZeroDecimals(thisClone);
            }
         }
      }

      thisClone = thisClone.setScale(0);

      //if long, return it..
      if ((thisClone.flags & ~0xFFFFFFFC) == 0x00000001)
         return BigInteger.valueOf(thisClone.laside);

      //if BI just return it
      else {
         if (((thisClone.flags & 0x3) == 0x0)){ //DFP
            thisClone = clone(this);
            thisClone.DFPToBI();
         }
         return thisClone.bi;
      }
   }

   /**
    * Converts this BigDecimal to a int.
    *
    * @return  the int representation of the receiver.
    */
   public int intValue(){
      return (int)longValue();
   }

   /**
    * Converts this BigDecimal to a long.
    *
    * @return  the long representation of the receiver.
    */
   public long longValue(){
      return toBigInteger().longValue();
   }

   /**
    * Converts this BigDecimal to a double.
    *
    * @return  the double representation of the receiver.
    */
   public double doubleValue(){
      return java.lang.Double.valueOf(this.toString()).doubleValue();
   }

   /**
    * Converts this BigDecimal to a float.
    *
    * @return  the float representation of the receiver.
    */
   public float floatValue(){
      return java.lang.Float.valueOf(this.toString()).floatValue();
   }

   /**
    * Converts this to a BigInteger.
    *
    * @return  the BigInteger equivalent of this BigDecimal.
    */
   public BigInteger toBigInteger(){

      BigDecimal temp = this.setScale(0, RoundingMode.DOWN);

      if (temp == this)// i.e. if original scale was 0
         temp = clone(this);

      if (DFPHWAvailable() && ((temp.flags & ~0xFFFFFFFC) == 0x00000000)) //DFP
         temp.DFPToLL();

      if ((temp.flags & 0x00000001) == 0x00000001) //LL
         return BigInteger.valueOf(temp.laside);
      else {
         if((temp.flags & 0x3) == 0x0) {
            temp.DFPToBI();
         }
         return temp.bi;
      }
   }

   // Refactored to compensate for slower DFP comparisons
   /**
    * Compares the receiver BigDecimal and argument BigDecimal
    * e.x 1.00 & 1.0 will return 0 in compareTo.
    *
    * @param rhs the BigDecimal to compare
    *
    * @return  0 if equal; positive if this > rhs; negative if this < rhs
    */
   public int compareTo(BigDecimal rhs){

      // if both operands are LL
      if ((this.flags & rhs.flags & 0x00000001) == 0x00000001){

         // NOTE: We test for this in longCompareTo anyways, so
         // ok if it doesn't get caught here...

         long lhsLaside = this.laside;
         long rhsLaside = rhs.laside;
         int lhsScale = this.cachedScale;
         int rhsScale = rhs.cachedScale;

         // first set of checks based on having the same exponent
         // used for dealing with dollar/cents BigDecimal objects
         if (lhsScale == rhsScale &&
            // in order to make sure the following quick check works,
            // we need to make sure that the long lookaside ha
            (lhsLaside >> 63) == (rhsLaside >> 63)){

            long temp = lhsLaside - rhsLaside;
            return ((int)( temp >> 63)) | ((int) ((-temp) >>> 63));
         }
      }
      return compareTo2(rhs);
   }
   
   private int compareTo2(BigDecimal rhs){
      int ret=0;

      BigDecimal thisClone = this;
      BigDecimal rhsClone = rhs ;

      // we know this is fast in DFP, so special case this
      // this is a fast way to handle misaligned BigDecimal object comparisons
      if (DFPHWAvailable() && DFPUseDFP()){
         thisClone = clone(this);
         rhsClone = clone(rhs);

         //convert to DFP
         postSetScaleProcessing(thisClone);
         postSetScaleProcessing(rhsClone);
      }

      // if both operands are DFP
      if (DFPHWAvailable() && (((thisClone.flags | rhsClone.flags) & ~0xFFFFFFFC) == 0x00000000)){

         int res = DFPCompareTo(thisClone.laside, rhsClone.laside);
         if (res != -2)
            return res;

         //otherwise convert to LL

         //branch to long compareTo
         thisClone = clone(thisClone);
         thisClone.DFPToLL();
         rhsClone = clone(rhsClone);
         rhsClone.DFPToLL();

         ret = BigDecimal.longCompareTo(thisClone, rhsClone);
         return ret;
      }

      // if both operands are LL
      if ((thisClone.flags & rhsClone.flags & 0x00000001) == 0x00000001){

         // NOTE: We test for this in longCompareTo anyways, so
         // ok if it doesn't get caught here...

         long lhsLaside = thisClone.laside;
         long rhsLaside = rhsClone.laside;
         int lhsScale = thisClone.cachedScale;
         int rhsScale = rhsClone.cachedScale;

         // first set of checks based on having the same exponent
         // used for dealing with frequently occuring dollar/cents BigDecimal objects
         if (lhsScale == rhsScale){
            if (this.laside > rhs.laside)  return 1;
            else if (this.laside < rhs.laside) return -1;
            return 0;
         }

         int lhsSignum =  ((int)( lhsLaside >> 63)) | ((int) ((-lhsLaside) >>> 63));
         int rhsSignum = ((int)( rhsLaside >> 63)) | ((int) ((-rhsLaside) >>> 63));
         int signumDiff = lhsSignum - rhsSignum;
         if (signumDiff != 0){
            ret = (signumDiff > 0 ? 1 : -1);
            return ret;
         }
         //NOTE:  At lhs point both values have the same sign

         // fast check... see which one is bigger based
         // on number of whole digits..
         // i.e.  23.25 vs. 255.25

         int tempLHSFlags = thisClone.flags;
         int tempRHSFlags = rhsClone.flags;
         int lhsNumWholeDigits=-lhsScale;
         int rhsNumWholeDigits=-rhsScale;
         int prec=0;

         // have we cached either?
         if ((tempLHSFlags & 0x10) !=0)
            lhsNumWholeDigits += (tempLHSFlags & 0xFFFFFF80) >> 7;
         else{
            prec = numDigits(lhsLaside);
            lhsNumWholeDigits += prec;
            tempLHSFlags|=0x10; //cache on
            tempLHSFlags&=0x7F; //clear pre-existing bits
            tempLHSFlags |= prec << 7;
            thisClone.flags=tempLHSFlags;
         }
         if ((tempRHSFlags & 0x10) !=0)
            rhsNumWholeDigits += (tempRHSFlags & 0xFFFFFF80) >> 7;
         else{
            prec = numDigits(rhsLaside);
            rhsNumWholeDigits += prec;
            tempRHSFlags|=0x10; //cache on
            tempRHSFlags&=0x7F; //clear pre-existing bits
            tempRHSFlags |= prec << 7;
            rhsClone.flags=tempRHSFlags;
         }

           if (lhsNumWholeDigits < rhsNumWholeDigits){
               ret = -lhsSignum;
            return ret;
           }
           else if (lhsNumWholeDigits > rhsNumWholeDigits){
               ret = lhsSignum;
            return ret;
           }

           // let's canonicalize both to have the same scale...
           // and fall back into the case where we can just compare
           // the significant digits...
           long scaleDiff=0;

           BigInteger lhsBI=null;
           BigInteger rhsBI=null;

           // now place them into the same scale...
           if (lhsScale < rhsScale){
            //i.e. 352.3 vs. 35.30
            //set lhs scale to rhs scale
            scaleDiff = (long)rhsScale -lhsScale; //padding to multiply lhs by
            long pad = powerOfTenLL(scaleDiff);
            if (pad != -1){
               long tempLaside = lhsLaside;
               if (tempLaside < 0) tempLaside *= -1;
               int m = Long.numberOfLeadingZeros(tempLaside)-1;
               int n = Long.numberOfLeadingZeros(pad)-1;
               if (m+n>=63){ //&& !overflowMultiply(lhsLaside,pad)){
                  lhsLaside*=pad;

                  // compare the two here....
                  long temp = lhsLaside - rhsLaside;
                  ret = ((int)( temp >> 63)) | ((int) ((-temp) >>> 63));
                  return ret;
               }
            }
            //perform in BI
            lhsBI = BigInteger.valueOf(lhsLaside);
            rhsBI = BigInteger.valueOf(rhsLaside);
            if (scaleDiff > Integer.MAX_VALUE) {
               scaleOverflow();
            }
            lhsBI = lhsBI.multiply(powerOfTenBI(scaleDiff));
            ret = lhsBI.compareTo(rhsBI);
         return ret;
           }
           else if (lhsScale > rhsScale){
            // i.e. 352.30 vs. 35.3
            //set rhs scale to lhs scale
            scaleDiff = lhsScale -rhsScale; //padding to multiply lhs by
            long pad = powerOfTenLL(scaleDiff);
            if (pad != -1){
               long tempLaside = rhsLaside;
               if (tempLaside < 0) tempLaside *= -1;
               int m = Long.numberOfLeadingZeros(tempLaside)-1;
               int n = Long.numberOfLeadingZeros(pad)-1;
               if (m+n>=63){ 
                  rhsLaside*=pad;

                  // compare the two here....
                  long temp = lhsLaside - rhsLaside;
                  ret = ((int)( temp >> 63)) | ((int) ((-temp) >>> 63));
                  return ret;
               }
            }
            // perform in BI
            lhsBI = BigInteger.valueOf(lhsLaside);
            rhsBI = BigInteger.valueOf(rhsLaside);
            if (scaleDiff > Integer.MAX_VALUE) {
               scaleOverflow();
            }
            rhsBI = rhsBI.multiply(powerOfTenBI(scaleDiff));
            ret = lhsBI.compareTo(rhsBI);
            return ret;
           }
           else{ //lhsExp == rhsExp
            long temp = lhsLaside - rhsLaside;
            ret = ((int)( temp >> 63)) | ((int) ((-temp) >>> 63));
            return ret;
           }
      }

      // at this point, might be DFP, LL or BI...

      // if any are DFP, convert to LL...
      thisClone=clone(this);
      rhsClone=clone(rhs);

      if (((thisClone.flags & ~0xFFFFFFFC) == 0x00000000)){
         thisClone.DFPToLL();
      }

      if (((rhsClone.flags & ~0xFFFFFFFC) == 0x00000000)){
         rhsClone.DFPToLL();
      }

      // at this point, all are LL or BI

      // are both LL?
      if ((thisClone.flags & rhsClone.flags & 0x00000001) == 0x00000001){
         ret = longCompareTo(thisClone, rhsClone);
         return ret;
      }

      // at this point, maybe LL or BI
      if ((thisClone.flags & ~0xFFFFFFFC) == 0x00000001){
         thisClone.LLToBI();
      }

      if ((rhsClone.flags & ~0xFFFFFFFC) == 0x00000001){
         rhsClone.LLToBI();
      }

      ret = slCompareTo(thisClone,rhsClone);
      return ret;
   }

   private final static int longCompareTo(BigDecimal lhs, BigDecimal rhs){
      // rhs=null will raise NullPointerException, as per Comparable interface

      long lhsLaside = lhs.laside;
      long rhsLaside = rhs.laside;
      int lhsSignum =  ((int)( lhsLaside >> 63)) | ((int) ((-lhsLaside) >>> 63));
      int rhsSignum = ((int)( rhsLaside >> 63)) | ((int) ((-rhsLaside) >>> 63));
      int signumDiff = lhsSignum - rhsSignum;
      if (signumDiff != 0)
         return (signumDiff > 0 ? 1 : -1);

      //NOTE:  At lhs point both values have the same sign
      int lhsScale = lhs.cachedScale;
      int rhsScale = rhs.cachedScale;

      int tempLHSFlags = lhs.flags;
      int tempRHSFlags = rhs.flags;
      int lhsNumWholeDigits=-lhsScale;
      int rhsNumWholeDigits=-rhsScale;
      int prec=0;

      // have we cached either?
      if ((tempLHSFlags & 0x10) !=0)
         lhsNumWholeDigits += (tempLHSFlags & 0xFFFFFF80) >> 7;
      else{
         prec = numDigits(lhsLaside);
         lhsNumWholeDigits += prec;
         tempLHSFlags|=0x10; //cache on
         tempLHSFlags&=0x7F; //clear pre-existing bits
         tempLHSFlags |= prec << 7;
         lhs.flags=tempLHSFlags;
      }
      if ((tempRHSFlags & 0x10) !=0)
         rhsNumWholeDigits += (tempRHSFlags & 0xFFFFFF80) >> 7;
      else{
         prec = numDigits(rhsLaside);
         rhsNumWholeDigits += prec;
         tempRHSFlags|=0x10; //cache on
         tempRHSFlags&=0x7F; //clear pre-existing bits
         tempRHSFlags |= prec << 7;
         rhs.flags=tempRHSFlags;
      }

        if (lhsNumWholeDigits < rhsNumWholeDigits)
            return -lhsSignum;
        else if (lhsNumWholeDigits > rhsNumWholeDigits)
            return lhsSignum;

        // let's canonicalize both to have the same scale...
        // and fall back into the case where we can just compare
        // the significant digits...
        int scaleDiff=0;

        BigInteger lhsBI=null;
        BigInteger rhsBI=null;

        // now place them into the same scale...
        if (lhsScale < rhsScale){
         //i.e. 352.3 vs. 35.30
         //set lhs scale to rhs scale
         scaleDiff = rhsScale -lhsScale; //padding to multiply lhs by
         long pad = powerOfTenLL(scaleDiff);
         if (pad != -1){
            long tempLaside = lhsLaside;
            if (tempLaside < 0) tempLaside *= -1;
            int m = Long.numberOfLeadingZeros(tempLaside)-1;
            int n = Long.numberOfLeadingZeros(pad)-1;
            if (m+n>=63){ //&& !overflowMultiply(lhsLaside,pad)){
               lhsLaside*=pad;

               // compare the two here....
               long temp = lhsLaside - rhsLaside;
               return ((int)( temp >> 63)) | ((int) ((-temp) >>> 63));
            }
         }
         //perform in BI
         lhsBI = BigInteger.valueOf(lhsLaside);
         rhsBI = BigInteger.valueOf(rhsLaside);
         if (scaleDiff > Integer.MAX_VALUE) {
            scaleOverflow();
         }
         lhsBI = lhsBI.multiply(powerOfTenBI(scaleDiff));
         return lhsBI.compareTo(rhsBI);
        }
        else if (lhsScale > rhsScale){
         // i.e. 352.30 vs. 35.3
         //set rhs scale to lhs scale
         scaleDiff = lhsScale -rhsScale; //padding to multiply lhs by
         long pad = powerOfTenLL(scaleDiff);
         if (pad != -1){
            long tempLaside = rhsLaside;
            if (tempLaside < 0) tempLaside *= -1;
            int m = Long.numberOfLeadingZeros(tempLaside)-1;
            int n = Long.numberOfLeadingZeros(pad)-1;
            if (m+n>=63){
               rhsLaside*=pad;

               // compare the two here....
               long temp = lhsLaside - rhsLaside;
               return ((int)( temp >> 63)) | ((int) ((-temp) >>> 63));
            }
         }

         // perform in BI
         lhsBI = BigInteger.valueOf(lhsLaside);
         rhsBI = BigInteger.valueOf(rhsLaside);
         if (scaleDiff > Integer.MAX_VALUE) {
            scaleOverflow();
         }
         rhsBI = rhsBI.multiply(powerOfTenBI(scaleDiff));
         return lhsBI.compareTo(rhsBI);
        }
        else{ //lhsExp == rhsExp
         long temp = lhsLaside - rhsLaside;
         return ((int)( temp >> 63)) | ((int) ((-temp) >>> 63));
        }
   }

   private final static int slCompareTo(BigDecimal lhs, BigDecimal rhs){

      // rhs=null will raise NullPointerException, as per Comparable interface

      // see if we can take advantage of cache'd signums...
      int lhsSignum = lhs.bi.signum();
      int rhsSignum = rhs.bi.signum();
      int signumDiff = lhsSignum - rhsSignum;
      if (signumDiff != 0)
         return (signumDiff > 0 ? 1 : -1);

      //NOTE:  At lhs point both values have the same sign

      BigInteger lhsBI = lhs.bi;
      BigInteger rhsBI = rhs.bi;
      int lhsScale = lhs.cachedScale;
      int rhsScale = rhs.cachedScale;

      // fast check... see which one is bigger based
      // on number of whole digits..
      // i.e.  23.25 vs. 255.25
      int lhsNumWholeDigits = lhs.precision() - lhsScale;    // [-1]
        int rhsNumWholeDigits = rhs.precision() - rhsScale;     // [-1]
        if (lhsNumWholeDigits < rhsNumWholeDigits)
            return -lhsSignum;
        else if (lhsNumWholeDigits > rhsNumWholeDigits)
            return lhsSignum;

        // let's canonicalize both to have the same scale...
        // and fall back into the case where we can just compare
        // the significant digits...
        int scaleDiff=0;

        // now place them into the same scale...
        if (lhsScale < rhsScale){
         //i.e. 352.3 vs. 35.30
         //set lhs scale to rhs scale
         scaleDiff = rhsScale -lhsScale; //padding to multiply lhs by
         if (scaleDiff > Integer.MAX_VALUE) {
            scaleOverflow();
         }
         lhsBI = lhsBI.multiply(powerOfTenBI(scaleDiff));
         return lhsBI.compareTo(rhsBI);
        }
        else if (lhsScale > rhsScale){
         // i.e. 352.30 vs. 35.3
         //set rhs scale to lhs scale
         scaleDiff = lhsScale -rhsScale; //padding to multiply lhs by
         if (scaleDiff > Integer.MAX_VALUE) {
            scaleOverflow();
         }
         rhsBI = rhsBI.multiply(powerOfTenBI(scaleDiff));
         return lhsBI.compareTo(rhsBI);
        }
        else{ //lhsScale == rhsScale
         BigInteger temp = lhsBI.subtract(rhsBI);
         return temp.signum();
        }
   }

   /**
    * Compares the argument to the receiver, and answers true
    * if they represent the <em>same</em> object using a class
    * specific comparison. The implementation in Object answers
    * true only if the argument is the exact same object as the
    * receiver (==).
    *
    * @param      obj the object to compare with this object.
    * @return  <code>true</code> if the object is the same as this object
    *       <code>false</code> if it is different from this object.
    * @see  #hashCode
    */
   public boolean equals(java.lang.Object obj){

      //NOTE: Scales MUST be equal (contast to compareTo)
      BigDecimal rhs;

      if (obj==null)
         return false; // not equal

      if (!(obj instanceof BigDecimal))
         return false; // not a decimal

      rhs=(BigDecimal)obj; // cast; we know it will work

      //quick shortcut since BDs are immutable
      if (this == rhs)
         return true;

      //check the signs of the LLs
      if ((this.flags & rhs.flags & 0x00000001) == 0x00000001){
         return ((this.laside == rhs.laside) && (this.cachedScale == rhs.cachedScale)); //final check
      }
      else{ // all other paths (DFP and BI)
         return (this.unscaledValue().equals(rhs.unscaledValue()) &&
               this.scale() == rhs.scale());
      }
   }

   /**
    * Answers an integer hash code for the receiver. Any two
    * objects which answer <code>true</code> when passed to
    * <code>.equals</code> must answer the same value for this
    * method.
    *
    * @return  the receiver's hash.
    * @see        #equals(Object)
    */
   public int hashCode(){
		return  this.unscaledValue().hashCode()^(scale() << 16);
   }

   private int cachePrecision(BigDecimal thisObject, int tempFlags) {
      // cache the precision
      int prec = numDigits(thisObject.laside);
      tempFlags|=0x10; //cache on
      tempFlags&=0x7F; //clear pre-existing bits
      tempFlags |= prec << 7;
      thisObject.flags=tempFlags;
      return prec;
   }

   /**
    * Answers the precision of the receiver
    *
    * @return the precision
    */
   public int precision(){

      /* cache - SGP ESRR */

      int tempFlags = this.flags;

      // have we cached it?
      if ((tempFlags & 0x10) !=0)
         return (tempFlags & 0xFFFFFF80 ) >> 7;
      else{  //we need to cache it for each form

         if (DFPHWAvailable() && ((tempFlags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
            return DFPPrecisionHelper();
         }
         else if ((this.flags & ~0xFFFFFFFC)== 0x00000002){ //BI
            int prec = precisionBI(this.bi);

            /* cache if we can */
            if (prec <= 33554431){
               tempFlags|=0x10; // cache on
               tempFlags&=0x7F; //clear pre-existing bits
               tempFlags|= prec << 7;
            }
            this.flags=tempFlags;
            return prec;
         }
         else{ //LL
            if((this.flags & 0x3) == 0x0) {
               // something is wrong if got in here
               // don't bother caching the precision
               BigDecimal thisClone = clone(this);
               thisClone.DFPToLL();
               int prec = numDigits(thisClone.laside);
                  return prec;
            }
            // cache the precision
            return cachePrecision(this, tempFlags);
         }
      }
   }

   private final static int precisionBI(BigInteger bi){
	    String x = bi.toString();
	    if (bi.signum() ==-1) return x.length()-1;
	    return x.length();
   }

   /**
    * Answers the scale of the receiver.
    *
    * @return the scale
    */
   public int scale(){

      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP

         // have we cached it?
         if ((this.flags & 0x8) != 0)
            return this.cachedScale;

         int newExp = DFPExponent(this.laside);
         if (newExp == 1000)
            this.cachedScale = -(extractDFPExponent(this.laside)-398);
         else
            this.cachedScale = -(newExp-398);

         //CMVC183765 set the flag after cache set to be thread safe
         //caching it
         this.flags|= 0x8; //cache on
      }

      return this.cachedScale;
   }

   /**
    * Answers the signum function of the receiver.
    *
    * @return the signum function
    */
   public int signum(){

      /* cache - SGP ESRR */

      int tempFlags = this.flags;

      if (DFPHWAvailable()&& ((tempFlags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         return DFPSignumHelper();
      }
      else if ((tempFlags& ~0xFFFFFFFC) == 0x00000001){ //LL
         long laside = this.laside;
         return ((int)(laside >> 63)) | ((int) ((-laside) >>> 63));
      }
      else {//BI
         if((this.flags & 0x3) == 0x0 ) {
            BigDecimal thisClone = clone(this);
            thisClone.DFPToBI();
                return thisClone.bi.signum();
         }
         return this.bi.signum();
      }
   }

   /**
    * Answers the ULP (Unit of Least Precision or Unit in the Last Place)
    * of the receiver
    *
    * @return the ULP
    */
   public BigDecimal ulp(){

      BigDecimal res = new BigDecimal();

      // set the long lookaside value
      res.laside = 1;

      /* cache - SGP ESRR */

      // set long lookaside representation
      res.flags |= 0x00000001;

      //set precision to 1
      res.flags&=0x7F; // clear the cached precision
      res.flags|= 0x10; //cache on
      res.flags |= 1 << 7;

      //set scale
      res.cachedScale = this.scale();

      return res;
   }

   /**
    * Returns an unscaled value of this BigDecimal.
    *
    * @return  a BigInteger with the unscaled value of the receiver
    */
   public BigInteger unscaledValue(){

      /* We cache the BigInteger representation even though
       * we might be in DFP or LL format.  This speeds up repeated
       * calls of unscaledValue on this object.
       */

      if (this.bi != null)
         return this.bi;

      if (DFPHWAvailable() && ((this.flags & 0x00000003) == 0)){ //DFP
         return DFPUnscaledValueHelper();
      }
      else if ((this.flags & 0x00000003) == 1){ //LL
         this.bi =BigInteger.valueOf(this.laside);
         return this.bi;
      }
      else {
         BigDecimal thisClone = this;
         if((this.flags & 0x3) == 0x0) {
            thisClone = clone(this);
            thisClone.DFPToBI();
         }
         return thisClone.bi;
      }
   }

   /**
    * Answers the receiver to the power of the given argument
    * using unlimited precision.
    *
    * @param n the power
    * @return the result of this to the power of n
    */
   public BigDecimal pow(int n){
      //helper call handles -999999999 through 999999999
      if (n <0) {
         // math.31 = Negative power: {0}
         throw new ArithmeticException(Messages.getString("math.31", //$NON-NLS-1$
            Integer.toString(n)));
      } else {
         return this.pow(n,MathContext.UNLIMITED);
      }
   }

   /**
    * Answers the receiver to the power of the given argument.
    *
    * @param n the power
    * @param set defines the precision and rounding behaviour
    * @return the result of this to the power of n
    */
   public BigDecimal pow(int n,MathContext set){

      int minPow =-999999999;
      int maxPow=999999999;
      int workDigits=0;
      BigDecimal lhs = this; //heavily used
      BigDecimal res=null;
      int tempN = n;
      boolean seenbit=false;
      short i=0; //will go up to 32 bits as per X3.274-1996

      //X3.274-1996 exception check
      if (n > maxPow || n < minPow) {
         // math.32 = Too large a power: {0}
         throw new ArithmeticException(Messages.getString("math.32", //$NON-NLS-1$
            Integer.toString(n)));
      }

      //X3.274-1996 exception check
      if (set.getPrecision()==0 && n < 0) {
         // math.31 = Negative power: {0}
         throw new ArithmeticException(Messages.getString("math.31", //$NON-NLS-1$
            Integer.toString(n)));
      }

      //X3.274-1996 exception check
      if (set.getPrecision()>0 && (numDigits(n) > set.getPrecision())) {
         // math.33 = Too many digits: {0}
         throw new ArithmeticException(Messages.getString("math.33", //$NON-NLS-1$
            Integer.toString(n)));
      }

      //quick return x^0
      if (n == 0)
         return BigDecimal.ONE;

      //quick return (0^exp)^x = (0^(exp*x)) for BI and LL
      if ((lhs.flags & 0x00000002) ==0x00000002 || (lhs.flags & 0x00000001) == 0x00000001 ){
         if (lhs.signum() == 0){
            if (n >= 0){

               //d117780
               BigDecimal ret = clone(this);
               long newScale = ((long)ret.cachedScale)*n;
               if (newScale < (long)Integer.MIN_VALUE)
                  ret.cachedScale =Integer.MIN_VALUE;
               else if (newScale > (long)Integer.MAX_VALUE)
                  ret.cachedScale = Integer.MAX_VALUE;
               else
                  ret.cachedScale =(int)newScale;
               return ret;
            }
            else {
               badDivideByZero();
            }
         }
      }
      else{// DFP case
         if (lhs.signum() == 0){
            if (n >=0){

               //d117780
               BigDecimal ret = clone(this);
               ret.DFPToLL();
               long newScale =((long)ret.cachedScale)*n;
               if (newScale < (long)Integer.MIN_VALUE)
                  ret.cachedScale =Integer.MIN_VALUE;
               else if (newScale > (long)Integer.MAX_VALUE)
                  ret.cachedScale = Integer.MAX_VALUE;
               else
                  ret.cachedScale =(int)newScale;
               return ret;
            }
            else {
               badDivideByZero();
            }
         }
      }
      /* Now calculate working precision...
       * set.getPrecision() = set.getPrecision() + numDigitsOpt(n) + 1
       * set.getRoundingMode() = set.getRoundingMode()
       */

      if (set.getPrecision()>0)
         workDigits = set.getPrecision()+numDigits(n)+1;

      MathContext workingSet =  new MathContext(workDigits,set.getRoundingMode());

      //Create the accumulator in BI form
      res= new BigDecimal();
      res.bi = BigInteger.ONE;

      //exp is 0

      //cache the precision
      res.flags&=0x7F; // clear the cached precision
      res.flags|= 0x10 ;//cache on
      res.flags|= 1 << 7;

      //set to BigInteger representation
      res.flags|= 0x00000002;

      //If this is DFP, put it into BI form to continue with the rest
      BigDecimal lhsClone = clone(lhs); //136111 FIX
      if (DFPHWAvailable() && ((lhs.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         lhsClone.DFPToBI();
      }

      /* Negative exponents are treated like positives..
       * Except we divide at the end
       */

      if (tempN<0)
         tempN=-tempN;

      seenbit=false; // set once we've seen a 1-bit
      i=1; //starting off at bit 1
      {i:for(;;i++){ // for each bit [top bit ignored]
         tempN=tempN+tempN; // shift left 1 bit
         if (tempN<0){ // top bit is set (i.e. overflow)
            seenbit=true; // OK, we're off
            res.longMultiply(lhsClone,null,workingSet,true); // acc=acc*x

            // following are because longMultiply performs postProcessing
            if (DFPHWAvailable() && ((lhsClone.flags & ~0xFFFFFFFC) == 0x00000000)){
               lhsClone.DFPToBI();
            }
            if (DFPHWAvailable() && ((res.flags & ~0xFFFFFFFC) == 0x00000000)){
               res.DFPToBI();
            }
         }
         if (i==31)
            break i; // that was the last bit
         if ((!seenbit))
            continue i; // we don't have to square 1
         res.longMultiply(res,null,workingSet,true); // acc=acc*acc [square]
         if (DFPHWAvailable() && ((res.flags & ~0xFFFFFFFC) == 0x00000000))
            res.DFPToBI();

      }}/*i*/ // 32 bits

      // take care of the negative exopnent case
      if (n <0)
         res=BigDecimal.ONE.divide(res,workingSet);

      //commented out the strip part...
      if (set != MathContext.UNLIMITED)
         res.finish(set.getPrecision(), set.getRoundingMode().ordinal());

      return res;
   }

   /**
    * Moves the decimal point of this BigDecimal n places to the left.
    *
    * @param n the number of places to move the decimal point
    *
    * @return  a BigDecimal with decimal moved n places to the left.
    */
   public BigDecimal movePointLeft(int n){

      BigDecimal res;
      long interm_scale=0;

      //Algorithm consists of 3 steps...
      /*
        step 1 - store resulting scale in temp
        step 2 - check to see if resulting scale is valid
        step 3 - modify scal
        step 4 - use larger of scale,n, and need to preserve value, so call setScale
       */

      res=clone(this);
      if ((this.flags & ~0xFFFFFFFC) == 0x00000000){ //DFP
         res.DFPToBI();
      }

      interm_scale = (long)res.scale()+(long)n;

      if (interm_scale < (long)Integer.MIN_VALUE || interm_scale > (long)Integer.MAX_VALUE){
         if (this.signum()==0)
            interm_scale = (interm_scale> Integer.MAX_VALUE)?Integer.MAX_VALUE:Integer.MIN_VALUE;
         else {
            scaleOutOfRange(interm_scale);
         }
      }

      res.cachedScale = (int)interm_scale;
      if (interm_scale<0)
         return res.setScale(0,true);

      return res;
   }

   /**
    * Moves the decimal point of this BigDecimal n places to the right.
    *
    * @param n the number of places to move the decimal point
    *
    * @return  a BigDecimal with decimal moved n places to the right.
    */
   public BigDecimal movePointRight(int n){

      BigDecimal res;
      long interm_scale;

      //Algorithm consists of 3 steps...
      /*
        step 1 - store resulting scale in temp
        step 2 - check to see if resulting scale is valid
        step 3 - modify scal
        step 4 - use larger of scale,n, and need to preserve value, so call setScale
       */

      int signum=0;
      res=clone(this);
      if ((this.flags & ~0xFFFFFFFC) == 0x00000000){ //DFP
         res.DFPToBI();
      }

      interm_scale = (long)res.scale()-(long)n;

      if ((this.flags & ~0xFFFFFFFC) == 0x00000001) // if LL
         signum = ((int)(res.laside >> 63)) | ((int) ((-res.laside) >>> 63));
      else // has to be BI
         signum = res.bi.signum();

      if (interm_scale < (long)Integer.MIN_VALUE || interm_scale > (long)Integer.MAX_VALUE){
         if (signum==0) {
            interm_scale = (interm_scale> Integer.MAX_VALUE)?Integer.MAX_VALUE:Integer.MIN_VALUE;
         } else {
            scaleOutOfRange(interm_scale);
         }
      }

      res.cachedScale = (int)interm_scale;
      if (interm_scale<0)
         return res.setScale(0, true);
      return res;
   }

   /**
    * Returns a BigDecimal whose numerical value is equal to (this * 10^n).
    * The scale of the result is (this.scale() - n).
    *
    * @param n the power
    *
    * @return (this * 10^n)
    */
   public BigDecimal scaleByPowerOfTen(int n){

      long interm_scale;
      BigDecimal res;
      BigDecimal thisClone = this;

      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         thisClone = clone(this);
         thisClone.DFPToBI();
      }

      //if resulting scale is outside 32 bit range...
      interm_scale = (long)thisClone.cachedScale-(long)n; //same as -(-thisClone.exp-n)

      if (interm_scale < (long)Integer.MIN_VALUE || interm_scale > (long)Integer.MAX_VALUE){
         if (this.signum()==0) {
            interm_scale = (interm_scale> Integer.MAX_VALUE)?Integer.MAX_VALUE:Integer.MIN_VALUE;
         } else {
            scaleOutOfRange(interm_scale);
         }
      }

      res = clone(thisClone);
      res.cachedScale = (int)interm_scale;
      return res;
   }

   /**
    * Sets the scale of this BigDecimal.
    *
    * @param scale the scale used for the result
    *
    * @return  a BigDecimal with the same value, but specified scale.
    */
   public BigDecimal setScale(int scale){
      return setScale(scale,false);
   }
   
   private BigDecimal setScale(int scale, boolean onThis){

      BigDecimal res = this;
      boolean cloned = false; 

      int ourscale=this.scale(); //need to do this to handle case if in DFP rep.
      if (ourscale == scale)
         return res;

      if (!onThis){
         res = new BigDecimal();
         cloned = true; 
      }

      boolean tryBI=false;
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         if(DFPSetScaleHelper(res, scale)) {
            return res;
         }
         tryBI = true;
      }

      // at this point, we have a DFP, LL or BI
      if (tryBI || ((this.flags & ~0xFFFFFFFC) == 0x00000002)){

         // we had DFP and it failed OR incoming was BI
         if (!cloned){ 
            res = new BigDecimal();
         }
         clone(res,this);
         if ((res.flags & ~0xFFFFFFFC) == 0x00000000)
            res.DFPToBI();

         // quick check if this value is 0
         if (res.bi.signum()==0){
            res.cachedScale = scale;

            //NOTE:  We do not cache precision!
            res.flags&=0xFFFFFFEF; //clear prec cache bit
            return res;
         }

         int tempFlags=res.flags;

         // if we're enlarging the scale - need to pad on the right
         if (ourscale < scale){

            // simply zero-padding/changing form
            // if ourscale is 0 we may have lots of 0s to add
            long padding=(long)scale-(long)ourscale;

            if (padding > (long)Integer.MAX_VALUE) {
               scaleOverflow();
            }
            res.bi = res.bi.multiply(powerOfTenBI(padding));
            res.cachedScale=scale;

            //NOTE:  We do not cache precision!
            tempFlags&=0xFFFFFFEF; //clear prec cache bit
            res.flags=tempFlags;
            return res;
         }

         // if we're decreasing the scale - need to get rid of digits from right)
         else{
            //figure out how many digits to chop off
            long chopoff = ((long)ourscale-(long)scale);

            //if we're chopping more digits then we have...
            if (chopoff > res.precision()) {
               // math.23 = Requires rounding: {0}
               throw new ArithmeticException(Messages.getString("math.23", Long.toString(scale))); //$NON-NLS-1$
            }

            // minimize chances of memory overflow...
            chopoff = Math.min(this.precision(), chopoff);

            // throw exception if reqiured
            BigInteger []divRem= null;
            if (chopoff > (long)Integer.MAX_VALUE) {
               scaleOverflow();
            }
            divRem= res.bi.divideAndRemainder(powerOfTenBI(chopoff));
            if (divRem[1].signum()!=0) {
               // math.23 = Requires rounding: {0}
               throw new ArithmeticException(Messages.getString("math.23", Long.toString(scale))); //$NON-NLS-1$
            }

            // otherwise we can use the quotient
            res.bi = divRem[0];
            res.cachedScale=scale;

            //NOTE:  We do not cache precision!
            tempFlags&=0xFFFFFFEF; //clear prec cache bit
            res.flags=tempFlags;
            return res;
         }
      }
      else { //otherwise, we have LL to deal with

         if (!cloned){
            res = new BigDecimal();
         }

         clone(res,this);
         int tempFlags = res.flags;

         // quick check if this value is 0
         int   signum =  ((int)(res.laside >> 63)) | ((int) ((-res.laside) >>> 63));
         if (signum == 0){

            //NOTE:  We do not cache precision!
            res.flags&=0xFFFFFFEF; //clear prec cache bit
            res.cachedScale = scale;
            return res;
         }

         // if we're enlarging the scale - need to pad on the right
         if (ourscale<scale){

            // simply zero-padding/changing form
            // if ourscale is 0 we may have lots of 0s to add
            long padding=0;
          
            padding=(long)scale-(long)ourscale;

            //see if we can't even pad...
            if (padding > 18){

               //convert to BI and perform padding
               res.LLToBI();
               if (padding > (long)Integer.MAX_VALUE) {
                  scaleOverflow();
               }
               res.bi = res.bi.multiply(powerOfTenBI(padding));
               res.cachedScale=scale;

               tempFlags=res.flags; //cause we went from LL to BI

               //NOTE:  We do not cache precision!
               tempFlags&=0xFFFFFFEF; //clear prec cache bit
               res.flags=tempFlags;
               return res;
            }

            // otherwise we can pad ourselves
            long multiple = powersOfTenLL[(int)padding];

            // manually inline overflowMultiply code here
            long resLaside = res.laside;
            if (resLaside < 0) resLaside *= -1;
            int m = Long.numberOfLeadingZeros(resLaside)-1;
            int n = Long.numberOfLeadingZeros(multiple)-1;
            if (m+n>=63){  //if (!overflowMultiply(res.laside, multiple)){

               res.laside = res.laside *multiple;
               res.cachedScale=scale; // as requested

               //NOTE:  We do not cache precision!
               tempFlags&=0xFFFFFFEF; //clear prec cache bit
               res.flags=tempFlags;
               //clear bi cache
               res.bi=null;
               return res;
            }
            else{

               // convert to BI and perform padding
               res.LLToBI();
               if (padding > (long)Integer.MAX_VALUE) {
                  scaleOverflow();
               }
               res.bi = res.bi.multiply(powerOfTenBI(padding));
               res.cachedScale=scale;

               tempFlags=res.flags; //cause we went from LL to BI

               //NOTE:  We do not cache precision!
               tempFlags&=0xFFFFFFEF; //clear prec cache bit
               res.flags=tempFlags;
               return res;
            }
         }

         /* If we're decreasing the scale:
          *  See if chopped off digits are not all 0s - throw exception
          *  Otherwise, chop em off and continue
          */
         else{

            //figure out how many digits to chop off
            long chopoff =  (long)ourscale - (long)scale;
            int precision=0;

            // have we cached it?
            if ((tempFlags & 0x10) !=0){
               precision = (tempFlags & 0xFFFFFF80 ) >> 7;
            }
            else{
               // cache the precision
               precision = numDigits(res.laside);
               tempFlags|=0x10; //cache on
               tempFlags&=0x7F; //clear pre-existing bits
               tempFlags |= precision << 7;
               res.flags=tempFlags;
            }

            //if we're chopping more digits then we have...
            if (chopoff > precision) {
               // math.23 = Requires rounding: {0}
               throw new ArithmeticException(Messages.getString("math.23", Long.toString(scale))); //$NON-NLS-1$
            }

            // if chopping off more digits then we can possibly
            // store in LL representation, then we automatically
            // get 0, and the remainder is the laside itself
            if (chopoff > 18){

               if (res.laside !=0) {
                  // math.23 = Requires rounding: {0}
                  throw new ArithmeticException(Messages.getString("math.23", Long.toString(scale))); //$NON-NLS-1$
               }

               res.laside=0;
               res.cachedScale=scale;

               //NOTE:  We do not cache precision!
               tempFlags&=0xFFFFFFEF; //clear prec cache bit
               res.flags=tempFlags;
               return res;
            }

            // otherwise we can use LL
            long rem = res.laside % powersOfTenLL[(int)chopoff];
            if (rem!=0) {
               // math.23 = Requires rounding: {0}
               throw new ArithmeticException(Messages.getString("math.23", Long.toString(scale))); //$NON-NLS-1$
            }

            //otherwise we acquire a quotient
            res.laside = res.laside / powersOfTenLL[(int)chopoff];
            res.cachedScale=scale;

            //NOTE:  We do not cache precision!
            tempFlags&=0xFFFFFFEF; //clear prec cache bit
            res.flags=tempFlags;

            //clear bi cache
            res.bi=null;

            return res;
         }
      }
   }

   /**
    * Sets the scale of this BigDecimal.  The unscaled value
    * is determined by the rounding Mode.
    *
    * @param scale the scale used for the result
    * @param round the rounding mode
    *
    * @return  a BigDecimal with the same value, but specified cale.
    * @exception  ArithmeticException rounding mode must be specified if lose of precision due to setting scale.
    * @exception  IllegalArgumentException invalid rounding mode
    */
   public BigDecimal setScale(int scale,java.math.RoundingMode round){
      return this.setScale(scale,round.ordinal());
   }

   // optimized for fast scale manipulation of dollar/cents BigDecimal obhects
   /**
    * Sets the scale of this BigDecimal.  The unscaled value
    * is determined by the rounding Mode.
    *
    * @param scale the scale used for the result
    * @param rm the rounding constant
    *
    * @return  a BigDecimal with the same value, but specified cale.
    * @exception  ArithmeticException rounding mode must be specified if lose of precision due to setting scale.
    * @exception  IllegalArgumentException invalid rounding mode
    */
   public BigDecimal setScale(int scale,int rm){

      // the most commonly occuring operations are
      // chopping off BI's and LL's (more LLs then BIs)

      // will throw an exception if illegal rounding mode
        if (rm < ROUND_UP || rm > ROUND_UNNECESSARY) {
         // math.27 = Bad round value: {0}
         throw new IllegalArgumentException(Messages.getString("math.27", Integer.toString(rm))); //$NON-NLS-1$
        }

        // we need to use scale() for DFP since scale is not
        // kept up to date otherwise. Defect 140836
      long ourscale =(long)this.scale();

      // at present this only checks round if it is used, for speed
      if (ourscale==scale)  // already correct scale
         return this;

      BigDecimal res = new BigDecimal();

      // only do this code if not in DFP
      if (!DFPHWAvailable() || ((this.flags & ~0xFFFFFFFC) != 0x00000000)){
         clone(res,this);

         if (((this.flags & ~0xFFFFFFFC) != 0x00000002))
            res.bi=null;

         // quick check if this value is 0
         if (res.signum()==0){
            res.cachedScale = scale;

            //NOTE:  We do not cache precision!
            res.flags&=0xFFFFFFEF; //clear prec cache bit
            return res;
         }

         //long newlen=0;
         boolean isBI=false;

         if (((this.flags & ~0xFFFFFFFC) == 0x00000002))
            isBI = true;

         if (ourscale > scale){

            res = res.divide(ONE, scale, rm);

            // NOTE:  We do not cache precision!
            res.flags&=0xFFFFFFEF; //clear prec cache bit
            postSetScaleProcessing(res);
            return res;
         }
         else{ //LL padding case, most frequently taken in our Hystersis framework
            if (!isBI){
               long padding=(long)scale - ourscale;

               //see if we can't even pad...
               if (padding > 18 ){
                  // convert to BI and perform padding
                  res.LLToBI();
                  if (padding > (long)Integer.MAX_VALUE) {
                     scaleOverflow();
                  }
                  res.bi = res.bi.multiply(powerOfTenBI(padding));
                  res.cachedScale=scale;
               }
               else{
                  //if we don't overflow, multiply laside
                  long multiple = powersOfTenLL[(int)padding];

                  // manually inline overflowMultiply code here
                  long resLaside = res.laside;
                  if (resLaside < 0) resLaside *= -1;
                  int m = Long.numberOfLeadingZeros(resLaside)-1;
                  int n = Long.numberOfLeadingZeros(multiple)-1;
                  if (m+n>=63){  //if (!overflowMultiply(res.laside, multiple)){

                     res.laside = res.laside *multiple;
                     res.cachedScale=scale; // as requested
                     res.bi=null;
                  }
                  else{
                     // convert to BI and perform padding
                     res.LLToBI();
                     if (padding > (long)Integer.MAX_VALUE) {
                        scaleOverflow();
                     }
                     res.bi = res.bi.multiply(powerOfTenBI(padding));
                     res.cachedScale=scale;
                  }
               }
               // NOTE:  We do not cache precision!
               res.flags&=0xFFFFFFEF; //clear prec cache bit
               postSetScaleProcessing(res);
               return res;
            }
         }
      }

      //if we got here, we still haven't returned...
      return setScale2(scale, rm, ourscale, res);
   }

   private BigDecimal setScale2(int scale,int rm, long ourscale, BigDecimal allocatedRes){

      BigDecimal res = allocatedRes;
      boolean passed = false;
      int ret=0;
      boolean tryBI=false;

      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         if(DFPSetScaleHelper(res, ourscale, scale, rm)) {
            return res;
         }
         tryBI=true;
      }

      // at this point, we have a DFP, LL or BI

      //here we handle the case if DFP failed for whatever reason
      //OR
      // the case of BI being padded (not handled in setScale)
      if (tryBI || ((this.flags & ~0xFFFFFFFC) == 0x00000002)){

         clone(res,this);

         //convert incoming DFP to BI
         if (tryBI || ((res.flags & 0x3) == 0x0)){
            res.DFPToBI();
         }

         //NOTE:  We do not cache precision!
         res.flags&=0xFFFFFFEF; //clear prec cache bit

         //quick check if this value is 0
         if (res.bi.signum()==0){
            res.cachedScale = scale;

            // NOTE:  We do not cache precision!
            res.flags&=0xFFFFFFEF; //clear prec cache bit
            postSetScaleProcessing(res);
            return res;
         }

         // if we came from DFP -> BI, we need to try
         // padding and chopping cases..
         // otherwise, for BI from setScale, just padding
         if (!tryBI){

            // if ourscale is 0 we may have lots of 0s to add
            long padding=(long)scale-ourscale;

            if (padding > Integer.MAX_VALUE) {
               scaleOverflow();
            }
            res.bi = res.bi.multiply(powerOfTenBI(padding));
            res.cachedScale=scale;

            // NOTE:  We do not cache precision!
            res.flags&=0xFFFFFFEF; //clear prec cache bit

            postSetScaleProcessing(res);
            return res;
         }
         else{

            //chopoff
            if (ourscale > scale){

               res = res.divide(ONE,scale,rm);

               // NOTE:  We do not cache precision!
               res.flags&=0xFFFFFFEF; //clear prec cache bit
               postSetScaleProcessing(res);
               return res;
            }
            else{ //padding

               // if ourscale is 0 we may have lots of 0s to add
               long padding=(long)scale-ourscale;

               if (padding > Integer.MAX_VALUE) {
                  scaleOverflow();
               }
               res.bi = res.bi.multiply(powerOfTenBI(padding));
               res.cachedScale=scale;

               // NOTE:  We do not cache precision!
               res.flags&=0xFFFFFFEF; //clear prec cache bit

               postSetScaleProcessing(res);
               return res;
            }
         }
      }
      else if ((this.flags & ~0xFFFFFFFC) == 0x00000001){ // in LL form

         // THE ONLY possibly case here is signum == 0 regardless
         // of scaling up or down since other cases are taken
         // care of the caller...

         clone(res,this);

         //quick check if this value is 0
         int   signum =  ((int)(res.laside >> 63)) | ((int) ((-res.laside) >>> 63));
         if (signum == 0){
            res.cachedScale = scale;

            //NOTE:  We do not cache precision!
            res.flags&=0xFFFFFFEF; //clear prec cache bit
         }
         postSetScaleProcessing(res);
         return res;
      }
      return res;
   }

   private final static void postSetScaleProcessing(BigDecimal res){

      //    NOTE:    The following helps out in exploiting additional
      //       DFP operations.  If the result of the setScale operation
      //       is small enough, put it back into LL form or try DFP.

      // No DFP HW - put into LL form
      if ((!DFPHWAvailable() || (DFPHWAvailable() && !DFPUseDFP()))
            && ((res.flags & ~0xFFFFFFFC) == 0x00000002) &&res.bi.bitLength()<63){
         res.BIToLL();
      }

      if (DFPHWAvailable() && DFPUseDFP()){

         // from BI to DFP
         if ((res.flags & ~0xFFFFFFFC) == 0x00000002 &&
               res.bi.compareTo(BigDecimal.MAXDFP64) <= 0 &&
               res.bi.compareTo(BigDecimal.MINDFP64) >=0 &&
               //res.exp <= 369 && res.exp >= -398){
               res.cachedScale > -369 && res.cachedScale < 398){

            long lint = res.bi.longValue();

            // NOTE: When we don't specify rounding in DFP, we'll never
            // fail, so don't bother checking the return value
            if (res.DFPLongExpConstructor(lint, -res.cachedScale+398,0, 0, 0, false)){

               /* cache: SGP ESRR */

               // store DFP representation
               res.flags&=0xFFFFFFFC; // clear rep bits
               //sets the bits to (00)

               // cache the exponent
               res.flags|=0x8; //cache on
               //res.exp already stores the value

               // cache the sign
               res.flags|=0x4; //cache on
               res.flags&=0xFFFFFF9F; //clear signum
               if (res.bi.signum() < 0)
                  res.flags|=0x60; //isneg
               else if (res.bi.signum() > 0)
                  res.flags|=0x20; //ispos
               else
                  res.flags&=0xFFFFFF9F;

               res.bi = null;
            }
         }

         // from LL to DFP
         else if ((res.flags & ~0xFFFFFFFC) == 0x00000001 &&
               res.laside <= 9999999999999999L &&
               res.laside >= -9999999999999999L &&
               res.cachedScale > -369 && res.cachedScale < 398){

            int signum=res.signum();

            // NOTE: When we don't specify rounding in DFP, we'll never
            // fail, so don't bother checking the return value

            if (res.DFPLongExpConstructor(res.laside, -res.cachedScale+398,0, 0, 0, false)){

               /* cache: SGP ESRR */

               // store DFP representation
               res.flags&=0xFFFFFFFC; // clear rep bits
               //sets the bits to (00)

               // cache the exponent
               res.flags|=0x8; //cache on
               //res.exp already stores the value

               // cache the sign
               res.flags|=0x4; //cache on
               res.flags&=0xFFFFFF9F; //clear signum
               if (signum < 0)
                  res.flags|=0x60; //isneg
               else if (signum > 0)
                  res.flags|=0x20; //ispos
               else
                  res.flags&=0xFFFFFF9F;
            }
         }
      }
   }

   /**
    * Answers a BigDecimal with the value of the receiver but with trailing
    * zeros removed.
    *
    * @return a BigDecimal stripped of trailing zeros
    */
   public BigDecimal stripTrailingZeros(){

      BigDecimal thisClone = this;
      BigDecimal res;

      //if DFP, convert to LL and perform the strip
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){
         thisClone = clone (this);
         thisClone.DFPToLL();
         res=thisClone;
      }
      else
         res = clone(thisClone);

      // Now all are in LL or BI format

      // if nothing to strip
      if (res.signum() == 0) {
          return BigDecimal.ZERO;
      }

        // In LL, repeated divide by 10 until remainder isn't 0
      if ((thisClone.flags & ~0xFFFFFFFC) == 0x00000001){ //LL

         //NOTE:  We do not cache precision!
         res.flags&=0xFFFFFFEF; //clear prec cache bit

         // otherwise, divide by 10 using Integer division
         int scaleDecr = 0; //used to adjust exponent
         long temp = res.laside;
         if (temp<0)temp*=-1;

         if (temp < Integer.MAX_VALUE){
            int intTmp = (int)temp; //integer version of laside
            int rem=0;
            while (true){
               rem = (byte)(intTmp - ((uDivideByTen(intTmp)<<1) + (uDivideByTen(intTmp)<<3)));
               if (rem!=0)break;
               intTmp = uDivideByTen(intTmp);
               scaleDecr++;
            }

            // zeros are all stripped now
            if (res.laside < 0)
               res.laside = -intTmp;
            else
               res.laside = intTmp;
         }
         else{
            int rem=0;
            while (true){
               rem = (byte)(temp - (((temp/10)<<1)+((temp/10)<<3)));
               if (rem!=0) break;
               temp/=10;
               scaleDecr++;

            }

            // zeros are all stripped now
            if (res.laside < 0)
               res.laside=-temp;
            else
               res.laside=temp;
         }

         // adjust the exponent and return
         res.cachedScale -= scaleDecr;
         return res;
      }
      else{ //BI
         if ((res.flags & 0x3) == 0x0) {
            res.DFPToBI();
         }

         //NOTE:  We do not cache precision!
         res.flags&=0xFFFFFFEF; //clear prec cache bit

         // same idea, but with BigInteger objects

         int scaleDecr = 0; //used to adjust exponent
         BigInteger temp = res.bi;

         BigInteger rem=BigInteger.ZERO; //used for remainder

         BigInteger tmpVal=BigInteger.ONE; //used for remainder calc
         while (true){
            tmpVal = temp;
            rem = tmpVal.remainder(BigInteger.TEN);
            if (rem.signum()!=0)break;
            scaleDecr++;
            temp = temp.divide(BigInteger.TEN);
         }
         res.bi=temp;

         // adjust the exponent and return
         res.cachedScale -= scaleDecr;
         return res;
      }
   }

   /**
    * Answers a string representation of the receiver.
    *
    * @return  a printable representation for the receiver.
    */
   public String toString(){

      // This contains all the long lookaside code that gets executed in
      // the case of common dollar/cents BigDecimal objects
      //int tempFlags = this.flags;
      if ((this.flags & 0x00000001) == 0x00000001) {//LL

         // cache these in locals
         long actualExp = -this.cachedScale;
         int precision = this.precision();

         // the case when we are printing BigDecimals of the form XX.XX
         // where XX are non-zero digits:

         if (longTest1(actualExp, precision)){
            long sign= ( this.laside >> 63) | ((-this.laside) >>> 63);
            int length= 1 /*for .*/ + precision;

            if (longTest2(actualExp, precision)){
                    if (sign == -1) length++;
                    char[] charArray = (length <= 22) ? new char[23] : new char[length];
                    longString1(length, (int)sign, charArray);
               return new String(charArray, 0, length);
            }
            // here we deal with the 2nd most called toString
            // in the form of 0.XX
            else{
               // optimization to catch 0.00 quickly
               if (sign == 0 && actualExp == -2)
                  return zeroDec;

               int numZeros = -(int)actualExp - precision;
               length+= 1+ numZeros;
               return longString2(length, precision, (int)sign);
            }
         }
      }
      return toString2();
   }

   private static boolean longTest1(long actualExp, int precision){
      return (actualExp <0 && (actualExp >= -5-precision));
   }
   private static boolean longTest2(long actualExp, int precision){
      return (actualExp > -precision && actualExp >= -19);
   }

   // contains the fast paths for commonly used longs of the form XX.XX
   private void longString1(int length, int sign, char[] charArray){

      long laside = this.laside;
      long actualExp = -this.cachedScale;

      byte rem=0;
      int start=0;
      laside = Math.abs(laside);

      // 2 Place sign
      if (sign == -1)
         charArray[start++]='-';

      // check for the common case of 32bit long lookaside value first
      char onesDig;
      char tensDig;
      long tmpVal;

      int charLoc=(int)(length-1);

      // 32-bit int ONLY
      if (laside <= Integer.MAX_VALUE){

         /* if we only have 2 decimal digits (i.e. cents) then no need for checking
          * the location of the decimal point when laying down the 2 decimal digits
          */
         if (actualExp == -2){
            int intVal = (int)laside;
            int intTmpVal;

            //lay down the decimal digits
            intTmpVal = intVal;
            intVal = intVal/100;//uDivideByHundred(intVal);
            rem = (byte)(intTmpVal - (intVal * 100));
            onesDig = doubleDigitsOnes[rem];
            tensDig = doubleDigitsTens[rem];
            charArray[charLoc--]=onesDig;
            charArray[charLoc--]=tensDig;

            //lay down the decimal point
            charArray[charLoc--]='.';

            //now lay down the whole portion of the number
            while (intVal != 0){
               intTmpVal = intVal;
               intVal = intVal/100;
               rem = (byte)(intTmpVal - (intVal * 100));
               onesDig = doubleDigitsOnes[rem];
               tensDig = doubleDigitsTens[rem];
               charArray[charLoc--] = onesDig;
               if (charLoc < start) break; //will only happen here
               charArray[charLoc--] = tensDig;
            }
         }
         else{ // we do what we've always done

            // dealing with 32-bit int now
            int decimalPointLoc = (int)((length-(-actualExp))-1);
            int intVal = (int)laside;
            int intTmpVal;
            while (intVal != 0){
               intTmpVal = intVal;
               intVal = intVal/100;
               rem = (byte)(intTmpVal - (intVal * 100));
               onesDig = doubleDigitsOnes[rem];
               tensDig = doubleDigitsTens[rem];
               if (charLoc == decimalPointLoc)
                  charArray[charLoc--]= '.';
               charArray[charLoc--] = onesDig;
               if (charLoc == decimalPointLoc)
                  charArray[charLoc--]= '.';
               if (charLoc < start) break; //will only happen here
               charArray[charLoc--] = tensDig;
            }
         }
      }
      else{ // dealing with a long lookaside that doesn't fit in a 32-bit integer
         int decimalPointLoc = (int)((length-(-actualExp))-1);
         while (laside > Integer.MAX_VALUE){
            tmpVal = laside;
            laside = laside/100;
            rem = (byte)(tmpVal - ((laside << 6) + (laside << 5) + (laside << 2)));
            onesDig = doubleDigitsOnes[rem];
            tensDig = doubleDigitsTens[rem];
            if (charLoc == decimalPointLoc)
               charArray[charLoc--]= '.';
            charArray[charLoc--] = onesDig;
            if (charLoc == decimalPointLoc)
               charArray[charLoc--]= '.';
            charArray[charLoc--] = tensDig;
         }

         // dealing with 32-bit int now
         int intVal = (int)laside;
         int intTmpVal;
         while (intVal != 0){
            intTmpVal = intVal;
            intVal = intVal/100;//uDivideByHundred(intVal);
            rem = (byte)(intTmpVal - ((intVal << 6) + (intVal << 5) + (intVal << 2)));
            onesDig = doubleDigitsOnes[rem];
            tensDig = doubleDigitsTens[rem];
            if (charLoc == decimalPointLoc)
               charArray[charLoc--]= '.';
            charArray[charLoc--] = onesDig;
            if (charLoc == decimalPointLoc)
               charArray[charLoc--]= '.';
            if (charLoc < start) break; //will only happen here
            charArray[charLoc--] = tensDig;
         }
      }
   }

   //fast long toString path for 0.XX form
   private String longString2(int length, int numLasideDigits, int sign){

      long laside = this.laside;
      long actualExp = -this.cachedScale;

      int start=0;
      long tmpVal = 0; //used for remainder calculations
      byte rem=0;

      if (sign == -1)
         length++;

      char [] charArray = new char[length];

      //1 Fill with 0
      Arrays.fill(charArray,0,(int)length,'0');

      //2 Place sign
      if (sign == -1)
         charArray[start++]='-';

      //2 skip leading zero
      start++;

      //2 Place decimal point
      charArray[start++]='.';

      //2 skip more leading zeros that occur after decimal point
      start+=(-actualExp - numLasideDigits);

      if (sign != 0){

         // fill up laside bytes (from back to front)
         laside = Math.abs(laside);

         char onesDig;
         char tensDig;
         int charLoc=(int)(length-1);

         // same optimization for as above 32-bit int long lookaside ONLY
         if (laside <= Integer.MAX_VALUE){
            int intVal = (int)laside;
            int intTmpVal;
            while (intVal != 0){
               intTmpVal = intVal;
               intVal = intVal/100;//uDivideByHundred(intVal);
               rem = (byte)(intTmpVal - ((intVal << 6) + (intVal << 5) + (intVal << 2)));
               onesDig = doubleDigitsOnes[rem];
               tensDig = doubleDigitsTens[rem];
               charArray[charLoc--] = onesDig;
               if (charLoc < start) break; //will only happen here
               charArray[charLoc--] = tensDig;
            }
         }
         else{
            while (laside > Integer.MAX_VALUE){
               tmpVal = laside;
               laside = laside/100;
               rem = (byte)(tmpVal - ((laside << 6) + (laside << 5) + (laside << 2)));
               onesDig = doubleDigitsOnes[rem];
               tensDig = doubleDigitsTens[rem];
               charArray[charLoc--] = onesDig;
               charArray[charLoc--] = tensDig;
            }

            // dealing with 32-bit int now
            int intVal = (int)laside;
            int intTmpVal;
            while (intVal != 0){
               intTmpVal = intVal;
               intVal = intVal/100;//uDivideByHundred(intVal);
               rem = (byte)(intTmpVal - ((intVal << 6) + (intVal << 5) + (intVal << 2)));
               onesDig = doubleDigitsOnes[rem];
               tensDig = doubleDigitsTens[rem];
               charArray[charLoc--] = onesDig;
               if (charLoc < start) break; //will only happen here
               charArray[charLoc--] = tensDig;
            }
         }
      }

   return new String(charArray, 0, length);
   }
   private String toString2(){

      // Get the exponent
      // Need to store it as a long since we may hvae
      // stored Long.MIN_VALUE (which needs to be printed
      // to screen as Long.MIN_VALUE
      long actualExp = 0;
      int tempFlags = this.flags;
      int precision = 0;

      /*
       * Check to see if we can fast path printing
       * long to screen when printing strings
       * that look like:
       *       325235.235235
       *
       * These checks are an accumulation of all the checks
       * along the path we used to take
       */
      if ((tempFlags & 0x00000001) == 0x00000001) {//LL
         actualExp = -this.cachedScale;
         long laside = this.laside;

         //have we cached the precision?
         if ((tempFlags & 0x10) !=0)
            precision = (tempFlags & 0xFFFFFF80) >> 7;
         else{
            precision= numDigits(laside);
            tempFlags|=0x10; //cache on
            tempFlags&=0x7F; //clear pre-existing bits
            tempFlags |= precision << 7;
            this.flags=tempFlags;
         }

         if (actualExp <0 && (actualExp >= -5-precision)){
            if (actualExp > -precision && actualExp >= -19){ //decimal digits

               int sign= ((int)( laside >> 63)) | ((int) ((-laside) >>> 63));
               int length= 1 /*for .*/ + precision;
               int start=0;
               laside = Math.abs(laside);
               if (sign == -1){
                  length++;
               }

               char [] str;
               if (actualExp == Integer.MIN_VALUE)
                  actualExp = -actualExp;

               if (length <=22)
                  str = (char [])thLocalToString.get();
               else
                  str = new char [ length ];

               // 2 Place sign
               if (sign == -1){
                  str[start++]='-';
               }

               int decimalPointLoc = (int)((length-(-actualExp))-1);

               byte rem=0;

               /*
                * while (LL_long > Integer.MAX_VALUE) {
                < divide LL_long by 100 >
                < calculate the remainder and use it to index two tables of 100 chars each which contain the ones and tens digits respectively >
                < LL_long = quotient >
                }
                < copy LL_long to an tmp_int >
                while (tmp_int != 0) {
                < divide tmp_int by 100 >
                < calculate the remainder and use it to index tables for the ones and tens digit >
                < tmp_int - quotient >
                }
                */
               char onesDig;
               char tensDig;
               long tmpVal;
               int charLoc=(int)(length-1);
               while (laside > Integer.MAX_VALUE){
                  tmpVal = laside;
                  laside = laside/100;
                  rem = (byte)(tmpVal - ((laside << 6) + (laside << 5) + (laside << 2)));
                  onesDig = doubleDigitsOnes[rem];
                  tensDig = doubleDigitsTens[rem];
                  if (charLoc == decimalPointLoc)
                     str[charLoc--]= '.';
                  str[charLoc--] = onesDig;
                  if (charLoc == decimalPointLoc)
                     str[charLoc--]= '.';
                  str[charLoc--] = tensDig;
               }

               // dealing with 32-bit int now
               int intVal = (int)laside;
               int intTmpVal;
               while (intVal != 0){
                  intTmpVal = intVal;
                  intVal = intVal/100;//uDivideByHundred(intVal);
                  rem = (byte)(intTmpVal - ((intVal << 6) + (intVal << 5) + (intVal << 2)));
                  onesDig = doubleDigitsOnes[rem];
                  tensDig = doubleDigitsTens[rem];
                  if (charLoc == decimalPointLoc)
                     str[charLoc--]= '.';
                  str[charLoc--] = onesDig;
                  if (charLoc == decimalPointLoc)
                     str[charLoc--]= '.';
                  if (charLoc < start) break; //will only happen here
                  str[charLoc--] = tensDig;
               }
               return new String(str,0,length);
            }
            else if (actualExp !=0){

            }
         }
      }

      /*
       * Otherwise, here is the regular set of paths, minus
       * the fast path for longs
       *    325235235.253235
       */

      actualExp = -(long)this.scale();
      if ((tempFlags & 0x00000001) == 0x00000001) {//LL
         // have we cached the precision?
         if ((tempFlags & 0x10) !=0){
            precision = (tempFlags & 0xFFFFFF80 ) >> 7;
         }
         else{
            // cache the precision
            precision = numDigits(this.laside);
            tempFlags|=0x10; //cache on
            tempFlags&=0x7F; //clear pre-existing bits
            tempFlags |= precision << 7;
            this.flags=tempFlags;
         }
      }
      else
         precision = this.precision();

      // Quick case when no exponent is required
      if (-actualExp >=0 && (actualExp >= -5-precision)){
         if (-actualExp == 0){ //no decimal digits

            if (DFPHWAvailable() && ((tempFlags & ~0xFFFFFFFC) == 0x00000000)){ //DFP

               // Get the unscaled value;
               long lVal = DFPUnscaledValue(this.laside);
               if (lVal == Long.MAX_VALUE){
                  lVal = DFPBCDDigits(this.laside);
                  if (lVal == 10)
                     lVal = extractDFPDigitsBCD(this.laside);

                  // now extract each 4 bit BCD digit from left to right
                  long val=0;  //to store the result
                  int i=0;
                  while (lVal!= 0){
                     val += (lVal & 0xF) * powerOfTenLL(i++);
                     lVal >>>= 4;
                  }

                  //is the sign negative?
                  return Long.toString(val*this.signum());
               }
               else
                  return Long.toString(lVal);
            }
            else if ((tempFlags & 0x00000001) == 0x00000001) {//LL
               return Long.toString(this.laside);
            }
            else { //BI
               BigDecimal thisClone = this;
               if ((tempFlags & 0x3) == 0x0) {
                  thisClone = clone(this);
                  thisClone.DFPToBI();
               }
               return thisClone.bi.toString();
            }
         }
         else{ //-actualExp > 0 (decimal digit required)
            int sign= 0;

            if (DFPHWAvailable() && ((tempFlags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
               sign=this.signum();
               if (-actualExp < precision){
                  int length= 1 /*for .*/ + precision;
                  if (sign == -1)
                     length++;
                  return new String(this.prePaddedStringDFP(sign,precision),0,length);
               }
               else{
                  int length= 2 /*for 0 + for .*/ -(int)actualExp; //+ numZeros + precision;
                  if (sign == -1)
                     length++;
                  return new String(this.prePaddedStringDFP(sign,precision),0,(int)length);
               }
            }
            else if ((tempFlags & 0x00000001) == 0x00000001) {//LL

               /* 523453 scale 2 = 5234.53 (nz = 2-6+1 = 0)
                * 123 scale 1 = 12.3  (nz = 1-3+1 =0)
                * 123 scale 2 = 1.23 (nz = 2 - 3 + 1= 0)
                * 123 scale 3 = 0.123
                * 123 scale 4 = 0.0123
                */
                  /*
                * This is the other case where we place a decimal digit
                * in the String representation for a long, but isn't
                * a fast path (starting with leading zeros)
                *    i.e. 0.00052352
                */
               sign = ((int)(this.laside >> 63)) | ((int) ((-this.laside) >>> 63));
               int length= 2 /*for 0 and for .*/ -(int)actualExp; //+numZeros //+ precision;
               if (sign == -1)
                  length++;
                  return new String(this.prePaddedString(sign,precision),0,(int)length);
               //}
            }
            else { //BI
               BigDecimal thisClone = this;
               if ((tempFlags & 0x3) == 0x0) {
                  thisClone = clone(this);
                  thisClone.DFPToBI();
               }

               sign = thisClone.bi.signum();

               /* 523453 scale 2 = 5234.53 (nz = 2-6+1 = 0)
                * 123 scale 1 = 12.3  (nz = 1-3+1 =0)
                * 123 scale 2 = 1.23 (nz = 2 - 3 + 1= 0)
                * 123 scale 3 = 0.123
                * 123 scale 4 = 0.0123
                */
               if (-actualExp < precision){
                  int length= 1 /*for .*/ + precision;
                  if (sign == -1)
                     length++;
                  return new String(thisClone.prePaddedString(sign,precision),0,(int) length);
               }
               else{
                  int length= 2 /*for 0 and  for .*/ -(int)actualExp; //+numZeros + precision;
                  if (sign == -1)
                     length++;
                  return new String(thisClone.prePaddedString(sign,precision),0, (int)length);
               }
            }
         }
      }
      //Exponent required
      else{

         long adjExp= actualExp + precision -1;

         // calculate total length of the array
         /* includes:
          *       -potential negative sign
          *       -first digit
          *       -. (case 1)
          *       -digits (case 1)
          *       -E
          *       -potential +/-
          *       -exponent
          */

         int length=0;
         if (this.signum() == -1) //potential negative
            length++;
         length++; //first digit
         if (precision > 1){
            length++;  //.
            length+=(precision -1); //digits
         }
         length++; //E
         if (actualExp != 0)
            length++; //potential +/-

         int expPrecision = numDigits(adjExp);
         length+=expPrecision; //exponent digits

         if (DFPHWAvailable() && (tempFlags & ~0xFFFFFFFC) == 0x00000000) //DFP
            return new String(DFPToStringExp(length),0,length);
         else if ((tempFlags & 0x00000001) == 0x00000001) //LL
            return new String(toStringExpLL(length),0,length);

         BigDecimal thisClone = this;
         if((tempFlags & ~0xFFFFFFFC) == 0x00000000) {
            thisClone = clone(this);
            thisClone.DFPToBI();
         }
         return new String(thisClone.toStringExpBI(length),0,length);
      }
   }

   private final char [] DFPToStringExp(int length){

      // Get the exponent
      // Need to store it as a long since we may hvae
      // stored Long.MIN_VALUE (which needs to be printed
      // to screen as Long.MIN_VALUE
      long actualExp = -(long)this.scale();

      // Get the precision
      int precision = this.precision();

      // Get the unscaled value;
      long bcd = DFPBCDDigits(this.laside);
      if (bcd == 10)
         bcd = extractDFPDigitsBCD(this.laside);

      long adjExp= actualExp + precision -1;

      // two cases to consider:
      /*
       * case 1: precision > 1
       *       singlenumber.remaining numbersE(+/-adjusted exponent)
       * case 2: else
       *       numberE(+/-adjusted exponent)
       */

      int expPrecision = numDigits(adjExp);

      // the character array to fill up
      char [] str;
      int index=0;

      if (length <=22)
         str = (char [])thLocalToString.get();
      else
         str = new char [ length ];

      // the sign
      if (this.signum() == -1){
         str[index++] = '-';
      }

      // the first digit
      str[index++] = (char)(digitAtBCD(bcd,precision,0)|0x0030);

      if (precision > 1){

         // the decimal dot
         str[index++] = '.';

         // rest of the digits
         for (int i=0;i < precision-1 ;i++)
            str[index++] = (char)(digitAtBCD(bcd,precision,i+1)|0x0030);
      }

      // E
      str[index++] = 'E';

      // the +
      if (actualExp>0)
         str[index++] = '+';
      else if (actualExp<0)
         str[index++] = '-';

      // exponent digits
      for (int i=0; i < expPrecision; i++)
         str[index++] = (char)(digitAt(adjExp,expPrecision,i)|0x0030);
      return str;
   }

   private final char [] toStringExpLL(int length){

      // Get the exponent
      // Need to store it as a long since we may hvae
      // stored Long.MIN_VALUE (which needs to be printed
      // to screen as Long.MIN_VALUE
      long actualExp = -(long)this.scale();
      int precision = this.precision();
      long adjExp= actualExp + precision -1;

      // two cases to consider:
      /*
       * case 1: precision > 1
       *       singlenumber.remaining numbersE(+/-adjusted exponent)
       * case 2: else
       *       numberE(+/-adjusted exponent)
       */

      int expPrecision = numDigits(adjExp);

      // the character array to fill up
      char [] str;
      int index=0;
      if (length <=22)
         str = (char [])thLocalToString.get();
      else
         str = new char [ length ];

      // the sign
      if (this.signum() == -1){
         str[index++] = '-';
      }

      // the first digit
      str[index++] = (char)(digitAt(this.laside,precision,0)|0x0030);

      if (precision > 1){

         // the decimal dot
         str[index++] = '.';

         // rest of the digits
         for (int i=0;i < precision -1;i++)
            str[index++] = (char)(digitAt(this.laside,precision,i+1)|0x0030);
      }

      // E
      str[index++] = 'E';

      // the +
      if (actualExp>0)
         str[index++] = '+';
      else if (actualExp<0)
         str[index++] = '-';

      // exponent digits
      for (int i=0; i < expPrecision; i++)
         str[index++] = (char)(digitAt(adjExp,expPrecision,i)|0x0030);
      return str;
   }

   private final char [] toStringExpBI(int length){

      // Get the exponent
      // Need to store it as a long since we may hvae
      // stored Long.MIN_VALUE (which needs to be printed
      // to screen as Long.MIN_VALUE
      long actualExp = -(long)this.scale();
      int precision = this.precision();
      long adjExp= actualExp + precision -1;

      // two cases to consider:
      /*
       * case 1: precision > 1
       *       singlenumber.remaining numbersE(+/-adjusted exponent)
       * case 2: else
       *       numberE(+/-adjusted exponent)
       */

      int expPrecision = numDigits(adjExp);

      // the character array to fill up
      char [] str;
      int index=0;
      if (length <=22)
         str = (char [])thLocalToString.get();
      else
         str = new char [ length ];

      String strBI = this.bi.toString();

      // the sign
      int start=0;
      if (this.bi.signum() == -1)
         str[index++] = '-';

      //the first digit
      if (this.bi.signum() >=0){
         str[index++] = strBI.charAt(0);
         start = 0;
      }
      else{
         str[index++] = strBI.charAt(1);
         start = 1;
      }

      if (precision > 1){

         // the decimal dot
         str[index++] = '.';

         // rest of the digits
         if (this.bi.signum() < 0) //account for the negative
            precision++;

         for (int i=start;i < precision-1 ;i++)
            str[index++] = strBI.charAt(i+1);
      }

      // E
      str[index++] = 'E';

      // the +
      if (actualExp>0)
         str[index++] = '+';
      else if (actualExp<0)
         str[index++] = '-';

      // exponent digits
      for (int i=0; i < expPrecision; i++)
         str[index++] = (char)(digitAt(adjExp,expPrecision,i)|0x0030);
      return str;
   }

   /**
    * Answers a string representation of the receiver with engineering style
    * exponent.
    *
    * @return  a printable representation for the receiver.
    */
   public String toEngineeringString(){

      BigDecimal thisClone = this;
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         thisClone = clone(this);
         thisClone.DFPToLL();
      }

      int tempFlags = thisClone.flags;

      //handle scale overflow... (i.e. use a long)
      long actualExp=-(long)thisClone.cachedScale;
      if (actualExp == Integer.MIN_VALUE)
         actualExp = -actualExp;
      int precision = thisClone.precision();

      byte sign= (byte)this.signum();

      //No exponent required
      if (-actualExp >=0 && (actualExp >= (long)(-5-precision))){
         if (-actualExp == 0){ //no decimal digits

            if ((tempFlags & 0x00000001) == 0x00000001)
               return Long.toString(thisClone.laside);
            else {//BI
               if ((thisClone.flags & 0x3) == 0x0) {
                  thisClone = clone(this);
                  thisClone.DFPToBI();
               }
               return thisClone.bi.toString();
            }
         }
         else{ //-actualExp > 0 (decimal digit required)

            /* 523453 scale 2 = 5234.53 (nz = 2-6+1 = 0)
             * 123 scale 1 = 12.3  (nz = 1-3+1 =0)
             * 123 scale 2 = 1.23 (nz = 2 - 3 + 1= 0)
             * 123 scale 3 = 0.123
             * 123 scale 4 = 0.0123
             */
            if (-actualExp < precision){
               if ((tempFlags & 0x00000001) == 0x00000001){ //LL
                  long length= 1 /*for .*/ + precision;
                  if (sign == -1)
                     length++;
                  return new String(thisClone.prePaddedString(sign, precision),0,(int)length);
               }
               else{ //BI
                  if ((thisClone.flags & 0x3) == 0x0) {
                     thisClone = clone(this);
                     thisClone.DFPToBI();
                  }
                  long length= 1 /*for .*/ + precision;
                  if (sign == -1)
                     length++;
                  return new String(thisClone.prePaddedString(sign, precision),0,(int)length);
               }
            }
            else{
               long numZeros = -actualExp - precision;
               if ((tempFlags & 0x00000001) == 0x00000001){ //LL
                  long length= 1 /*for 0*/ + 1 /*for .*/+ numZeros + precision;
                  if (sign == -1)
                     length++;
                  return new String(thisClone.prePaddedString(sign, precision),0,(int)length);
               }
               else{ //BI
                  if ((thisClone.flags & 0x3) == 0x0) {
                     thisClone = clone(this);
                     thisClone.DFPToBI();
                  }
                  long length= 1 /*for 0*/ + 1 /*for .*/+ numZeros + precision;
                  if (sign == -1)
                     length++;
                  return new String(thisClone.prePaddedString(sign, precision),0,(int)length);
               }
            }
         }
      }

      //Exponent required
      else{

         //Using engineering notation
         long euse = actualExp+thisClone.precision()-1;
         int sigDigs = (int)euse%3; //might cause overflow

         //special case the 0
         boolean isZero = (this.signum() == 0);
         if (isZero){
            int rem =sigDigs;
            if (rem !=0){
               if (rem <0){
                  sigDigs = -rem+1; //we need sig number of 0s in the final answer
                  euse = euse+(-rem); //push the euse to the next largest multiple of 3
               }
               else{
                  sigDigs = 1+(3-rem);
                  euse = euse+(3-rem);
               }

               //now we need sig 0's.. if more than 1, we need a decimal..
               StringBuilder sb = new StringBuilder();
               sb.append('0');
               if (sigDigs>1)
                  sb.append('.');
               for (int j=sigDigs-1; j>0; j--)
                  sb.append('0');

               if (euse<0){
               //sb.append("E-"+(-euse));
                  sb.append('E');
                  sb.append('-');
                  sb.append(-euse);
               }
               else if (euse>0){
                  //sb.append("E+"+euse);
                  sb.append('E');
                  sb.append('+');
                  sb.append(euse);
               }
               return sb.toString();
            }
            else{
               StringBuilder sb = new StringBuilder();
               if (euse<0){
                  sb.append('0');  sb.append('E');  sb.append('-');
                  sb.append(-euse);
                  return sb.toString();
               }
               else if (euse>0){
                  sb.append('0');  sb.append('E');  sb.append('+');
                  sb.append(euse);
                  return sb.toString();
               }
               else
                  return "0"; //$NON-NLS-1$
            }
         }
         else{
            if (sigDigs<0)
               sigDigs=3+sigDigs; // negative exponent
            euse=euse-sigDigs;
            sigDigs=sigDigs+1;

            //character array to work with...
            char [] cmant;
            int length = precision;
            if (length <=22)
               cmant = (char [])thLocalToString.get();
            else
               cmant = new char [ length ];

            if ((tempFlags & 0x00000001) == 0x00000001){  //LL
               long temp=thisClone.laside;
               if (temp < 0L) temp*=-1L;
               byte rem=0;
               int numDigits = precision;

               //fast path for long lookaside <= Integer.MAX_VALUE
               //thisClone path doesn't contain any divides
               if (temp <= Integer.MAX_VALUE){
                  int tmpVal = 0; //used for remainder calculations
                  int intTmp = (int) temp;
                  for (int j=numDigits-1; j>=0; j-- ){
                     tmpVal = intTmp;
                     intTmp=uDivideByTen(intTmp);
                     rem = (byte)(tmpVal - ((intTmp << 3) + (intTmp << 1)));
                     cmant[j]=(char)(rem|0x0030);
                  }
               }
               else{
                  long tmpVal = 0; //used for remainder calculations
                  for (int j=numDigits-1; j>=0; j-- ){
                     tmpVal = temp;
                     temp/=10;
                     rem = (byte)(tmpVal - (((temp << 3) + (temp << 1))));
                     cmant[j]=(char)(rem|0x0030);
                  }
               }
            }
            else{ // BI
               if ((thisClone.flags & 0x3) == 0x0) {
                  thisClone = clone(this);
                  thisClone.DFPToBI();
               }

               //digits
               int j=0;
               String strBI = thisClone.bi.abs().toString();
               for(j=0; j < strBI.length();j++)
                  cmant[j]=(char)(strBI.charAt(j)|0x0030);
            }

            StringBuilder sb = new StringBuilder();
            if (this.signum() == -1)
               sb.append('-');

            if (sigDigs>=length) { // zero padding may be needed
               sb.append(cmant,0,length);
               int x=sigDigs-length;
               for(;x>0;x--)
                  sb.append('0');
            }
            else // decimal point needed
               sb.append(cmant,0,sigDigs).append('.').append(cmant,sigDigs,length-sigDigs);

            if (euse<0){
               sb.append('E');  sb.append('-');
               sb.append(-euse);
            }
            else if (euse>0){
               sb.append('E');  sb.append('+');
               sb.append(euse);
            }
            return sb.toString();
         }
      }
   }

   /**
    * Answers a string representation of the receiver without exponent.
    *
    * @return  a printable representation for the receiver.
    */
   public String toPlainString(){
      /*
       * Three cases to consider...
       * if scale == 0 just return the number
       * else if scale < 0 (XXX0000) form
       * else //scale > 0 (XX.XXXX) form where X might be
       */

      String toPlStr;
      BigDecimal thisClone = this;
      if (DFPHWAvailable() && ((this.flags & ~0xFFFFFFFC) == 0x00000000)){ //DFP
         thisClone = clone(this);
         thisClone.DFPToLL();
      }

      byte sign= (byte)this.signum();

      //get rid of padding right off the bat
      if (thisClone.cachedScale < 0)
         thisClone = thisClone.setScale(0);

      int tempFlags = thisClone.flags;

      //Quick case...
      if (thisClone.cachedScale == 0){
         if ((tempFlags & 0x00000001) == 0x00000001) //LL
            toPlStr = Long.toString(thisClone.laside);
         else //BI
            toPlStr = thisClone.bi.toString();
      }
      else{//Prepad with 0s case

         int precision = thisClone.precision();

         //handle scale overflow... (i.e. use a long)
         long actualExp=-(long)thisClone.cachedScale;
         if (actualExp == Integer.MIN_VALUE)
            actualExp = -actualExp;

         if (-actualExp < precision){
            if ((tempFlags & 0x00000001) == 0x00000001){ //LL
               long length= 1 /*for .*/ + precision;
               if (sign == -1)
                  length++;
               toPlStr = new String(thisClone.prePaddedString(sign, precision),0,(int)length);
            }
            else{ //BI
               if ((thisClone.flags & 0x3) == 0x0) {
                  thisClone = clone(this);
                  thisClone.DFPToBI();
               }
               long length= 1 /*for .*/ + precision;
               if (sign == -1)
                  length++;
               toPlStr = new String(thisClone.prePaddedString(sign, precision),0,(int)length);
            }
         }
         else{
            long numZeros = -actualExp - precision;
            if ((tempFlags & 0x00000001 ) == 0x00000001){ //LL
               long length= 1 /*for 0*/ + 1 /*for .*/+ numZeros + precision;
               if (sign == -1)
                  length++;
               toPlStr = new String(thisClone.prePaddedString(sign, precision),0,(int)length);
            }
            else{ //BI
               if ((thisClone.flags & 0x3) == 0x0) {
                  thisClone = clone(this);
                  thisClone.DFPToBI();
               }
               long length= 1 /*for 0*/ + 1 /*for .*/ + numZeros + precision;
               if (sign == -1)
                  length++;
               toPlStr = new String(thisClone.prePaddedString(sign, precision),0,(int)length);
            }
         }
      }

      return toPlStr;
   }

   private final char [] prePaddedStringDFP(int sign, int precision){

      //NOTE :  unscaledValue is in BCD form

      char [] str;
      int actualExp = -this.scale();
      int signLen=0;
      if (sign == -1)
         signLen = 1;

      // Get the unscaled value;
      long unscaledValue= DFPBCDDigits(this.laside);
      if (unscaledValue == 10)
         unscaledValue = extractDFPDigitsBCD(this.laside);

      // if scale is less than precision, won't have
      // any leading zeros, and our number will have a decimal
      // point somewhere in the middle...

      /*
       * 1234 scale 1 = 123.4
       * 1234 scale 2 = 12.34
       * 1234 scale 3 = 1.234
       * 123400 scale 3 = 123.400 <-- need to handle trailing zeros for BI rep
       * 123400 scale 5 = 12340.0 <-- need to handle trailing zeros for BI rep
       *
       * NOTE:  don't need to handle scale <= 0 since this is taken care of
       *         in other branches in toStringHelper
       */
      if (-actualExp < precision){
         int i=0;

         // for LL
         // 1 no need to fill with trailing zeros
         // 2 lay down sign
         // 3 lay down all digits after decimal point
         // 4 lay down digits before decimal point

         int length= signLen + 1 /*for .*/ + precision;

         if (length <=22)
            str = (char [])thLocalToString.get();
         else
            str = new char [ length ];
         int start=0;
         int decimalPointLoc = length-(-actualExp)-1;

         // 2 Place sign
         if (signLen !=0){
            str[start++]='-';
         }

         int curBCD = 0;

         //3 lay down all digits after decimal point
         for (i=(length-1); i > decimalPointLoc; i--){
            curBCD = (int)(unscaledValue & 0xF);
            unscaledValue >>>=4;
            str[i] = (char)(curBCD|0x0030);
         }

         // lay down decimal point
         str[i--]='.';

         //4 lay down all digits before decimal point
         for (;i >=start; i--){
            curBCD = (int) (unscaledValue & 0xF);
            unscaledValue >>>=4;
            str[i] = (char)(curBCD|0x0030);
         }
      }
      else{
         // easy case.. where scale >= precision

         /*
          * 1234 scale 4 = 0.1234
          * 1234 scale 5 = 0.01234
          * 1234 scale 6 = 0.001234
          */

         int numZeros = -actualExp - precision;

         // for both LL & BI
         // 1 fill with zeros
         // 2 lay down sign & lay down decimal point
         // 3 lay down all digits

         int length= signLen + 1 /*for 0*/ +
         1 /*for .*/+ numZeros + precision;

         if (length <=22)
            str = (char [])thLocalToString.get();
         else
            str = new char [ length ];

         int start=0;
         int i=0;

         //1 Fill with 0
         Arrays.fill(str,0,length,'0');

         //2 Place sign
         if (signLen !=0)
            str[start++]='-';

         //2 skip leading zero
         start++;

         //2 Place decimal point
         str[start++]='.';

         //2 skip more leading zeros that occur after decimal point
         start+=(-actualExp - precision);

         // fill up laside bytes (from back to front)
         int curBCD=0;
         for (i=length-1; i >= start; i--){
            curBCD = (int) (unscaledValue & 0xF);
            unscaledValue >>>=4;
            str[i] = (char)(curBCD|0x0030);
         }
      }
   return str;
   }

   private final char [] prePaddedString(int sign, int precision){

      char [] str;
      int signLen=0;
      if (sign == -1)
         signLen = 1;

      long actualExp=-(long)this.cachedScale;

      int tempFlags = this.flags;

      // if scale is less than precision, won't have
      // any leading zeros, and our number will have a decimal
      // point somewhere in the middle...

      /*
       * 1234 scale 1 = 123.4
       * 1234 scale 2 = 12.34
       * 1234 scale 3 = 1.234
       * 123400 scale 3 = 123.400
       * 123400 scale 5 = 12340.0
       *
       * NOTE:  don't need to handle scale <= 0 since this is taken care of
       *         in other branches in toStringHelper
       */
      if (-actualExp < precision){

         // for LL
         // 1 no need to fill with trailing zeros
         // 2 lay down sign
         // 3 lay down all digits after decimal point
         // 4 lay down digits before decimal point

         if ((tempFlags & ~0xFFFFFFFC) == 0x00000001){ //LL

            long length= signLen + 1 /*for .*/ + precision;

            if (length <=22)
               str = (char [])thLocalToString.get();
            else
               str = new char [ (int)length ];

            int start=0;
            int decimalPointLoc = (int)((length-(-actualExp))-1);

            // 2 Place sign
            if (signLen !=0)
               str[start++]='-';

            long val = this.laside;
            val = Math.abs(val);
            byte rem=0;
            int i=0;

            // introduce a non-division path for
            //long lookaside <= Integer.MAX_VALUE
            if (val <= Integer.MAX_VALUE){
               int tmpVal = 0; //used for remainder calculations
               int intTmp = (int)val;
               // 3 lay down all digits after decimal point
               for (i=(int)(length-1); i > decimalPointLoc; i--){
                  tmpVal = intTmp;
                  intTmp=uDivideByTen(intTmp);
                  rem = (byte)(tmpVal - ((intTmp << 3) + (intTmp << 1)));
                  str[i] = (char)(rem|0x0030);

               }

               // lay down decimal point
               str[i--]='.';

               //4 lay down all digits after decimal point
               for (;i >=start; i--){
                  tmpVal = intTmp;
                  intTmp=uDivideByTen(intTmp);
                  rem = (byte)(tmpVal - ((intTmp << 3) + (intTmp << 1)));
                  str[i] = (char)(rem|0x0030);
               }
            }
            else{
               long tmpVal = 0; //used for remainder calculations

               //3 lay down all digits after decimal point
               for (i=(int)(length-1); i > decimalPointLoc; i--){
                  tmpVal = val;
                  val/=10;
                  rem = (byte)(tmpVal - ((val << 3) + (val << 1)));
                  str[i] = (char)(rem|0x0030);
               }

               // lay down decimal point
               str[i--]='.';

               //4 lay down all digits before decimal point
               for (;i >=start; i--){
                  tmpVal = val;
                  val/=10;
                  rem = (byte)(tmpVal - ((val << 3) + (val << 1)));
                  str[i] = (char)(rem|0x0030);
               }
            }
         }
         else{ //BI - same idea as with LL

            String strBI = this.bi.toString();
            int numDigits = strBI.length();
            if (sign ==-1) numDigits--;
            long length= signLen + 1 /*for .*/ + numDigits;

            if (length <=22)
               str = (char [])thLocalToString.get();
            else
               str = new char [ (int)length ];
            int start=0;
            int decimalPointLoc = (int)((length-(-actualExp))-1);

            // 2 Place sign
            if (signLen !=0)
               str[start++]='-';

            //3 lay down all digits after decimal point
            int i=0;
            int j=strBI.length()-1;
            for (i=(int)(length-1); i > decimalPointLoc; i--){
               str[i]=(char)(strBI.charAt(j--)|0x0030);
            }

            // lay down decimal point
            str[i--]='.';

            //4 lay down all digits before decimal point
            for (;i >=start; i--){
               str[i]=(char)(strBI.charAt(j--)|0x0030);
            }
         }
      }
      else{
         // easy case.. where scale >= precision

         /*
          * 1234 scale 4 = 0.1234
          * 1234 scale 5 = 0.01234
          * 1234 scale 6 = 0.001234
          */

         long numZeros = -actualExp - precision;
         int i=0;

         // for both LL & BI
         // 1 fill with zeros
         // 2 lay down sign & lay down decimal point
         // 3 lay down all digits
         if ((tempFlags & ~0xFFFFFFFC) == 0x00000001){ //LL

            int numLasideDigits = precision;
            long length= signLen + 1 /*for 0*/ + 1 /*for .*/+ numZeros + numLasideDigits;

            if (length <=22)
               str = (char [])thLocalToString.get();
            else
               str = new char [ (int)length ];

            int start=0;
            long tmpVal = 0; //used for remainder calculations
            byte rem=0;

            //1 Fill with 0
            Arrays.fill(str,0,(int)length,'0');

            //2 Place sign
            if (signLen !=0)
               str[start++]='-';

            //2 skip leading zero
            start++;

            //2 Place decimal point
            str[start++]='.';

            //2 skip more leading zeros that occur after decimal point
            start+=(-actualExp - numLasideDigits);

            if (sign != 0){

               // fill up laside bytes (from back to front)
               long val = this.laside;
               if (val < 0L)val*=-1;

               /*
                * while (LL_long > Integer.MAX_VALUE) {
                < divide LL_long by 100 >
                < calculate the remainder and use it to index two tables of 100 chars each which contain the ones and tens digits respectively >
                < LL_long = quotient >
                }
                < copy LL_long to an tmp_int >
                while (tmp_int != 0) {
                < divide tmp_int by 100 >
                < calculate the remainder and use it to index tables for the ones and tens digit >
                < tmp_int - quotient >
                }
                */

               char onesDig;
               char tensDig;
               int charLoc=(int)(length-1);
               while (val > Integer.MAX_VALUE){
                  tmpVal = val;
                  val = val/100;
                  rem = (byte)(tmpVal - ((val << 6) + (val << 5) + (val << 2)));
                  onesDig = doubleDigitsOnes[rem];
                  tensDig = doubleDigitsTens[rem];
                  str[charLoc--] = onesDig;
                  str[charLoc--] = tensDig;
               }

               // dealing with 32-bit int now
               int intVal = (int)val;
               int intTmpVal;
               while (intVal != 0){
                  intTmpVal = intVal;
                  intVal = intVal/100;//uDivideByHundred(intVal);
                  rem = (byte)(intTmpVal - ((intVal << 6) + (intVal << 5) + (intVal << 2)));
                  onesDig = doubleDigitsOnes[rem];
                  tensDig = doubleDigitsTens[rem];
                  str[charLoc--] = onesDig;
                  if (charLoc < start) break; //will only happen here
                  str[charLoc--] = tensDig;
               }
            }
         }
         else{ //BI

            String strBI = this.bi.toString();
            int numDigits = strBI.length();
            if (sign == -1) numDigits--;
            long length= signLen + 1 /*for 0*/ + 1 /*for .*/+ numZeros + numDigits;

            if (length <=22)
               str = (char [])thLocalToString.get();
            else
               str = new char [ (int)length ];

            int start=0;

            //1 Fill with 0
            Arrays.fill(str,0,(int)length,'0');

            //2 Place sign
            if (signLen !=0)
               str[start++]='-';

            //2 skip leading zero
            start++;

            //2 Place decimal point
            str[start++]='.';

            //2 skip more leading zeros that occur after decimal point
            start+=(-actualExp - numDigits);

            if (sign !=0){

               int charLoc=(int)(length-1);
               i=strBI.length()-1;
               while (charLoc >= start){
                  str[charLoc--] = strBI.charAt(i--);
               }
            }
         }
      }
      return str;
   }

   /**
    * Translate double value into a BigDecimal with scale of zero.
    *
    * @param dub the double value to convert to a BigDecimal
    *
    * @return  a BigDecimal equivalence of a double value.
    */
   public static BigDecimal valueOf(double dub){
      // Reminder: a zero double returns '0.0', so we cannot fastpath to
      // use the constant ZERO.  This might be important enough to justify
      // a factory approach, a cache, or a few private final constants, later.
      return new BigDecimal((new java.lang.Double(dub)).toString());
   }

   /**
    * Translate long value into a BigDecimal with scale of zero.
    *
    * @param lint the long value to convert to a BigDecimal
    *
    * @return  a BigDecimal equivalence of a long value.
    */
   public static BigDecimal valueOf(long lint){
      return valueOf(lint,0);
   }

   /**
    * Translate a long value into a BigDecimal specified by the scale.
    *
    * @param lint the long value to convert to a BigDecimal
    * @param scale the scale of the result
    *
    * @return     BigDecimal     BigDecimal equivalence of a long value.
    * @exception  NumberFormatException if the scale value is < 0;
    */
   public static BigDecimal valueOf(long lint,int scale){
      // ZERO, ONE, TEN
      if (scale == 0){
         if (lint == 0)
            return BigDecimal.ZERO;
         else if (lint == 1)
            return BigDecimal.ONE;
         else if (lint == 5)
            return BigDecimal.FIVE;
         else if (lint == 10)
            return BigDecimal.TEN;
      }

      // catching common values: 0.0 - 10.0, 0.00 - 10.00
      if (scale < 3 && lint >= 0 && lint < CACHE1.length){
         switch (scale){
         case 2: return CACHE2[(int)lint];  //case 2 first (dollars and cents)
         case 1: return CACHE1[(int)lint];
         default: break;
         }
      }

      BigDecimal res=new BigDecimal();

      if (DFPValueOfHelper(lint, scale, res) != null)
         return res;

      valueOf2BI(lint, res);

       res.cachedScale = scale;
      return res;
   }

   private static void valueOf2BI(long lint, BigDecimal res){
      if (lint != Long.MIN_VALUE){
         //set representation to long lookaside
         res.flags |= 0x00000001;
         res.laside = lint;
      } else {
         res.flags |= 0x00000002;
         res.bi = BigInteger.valueOf(Long.MIN_VALUE);
      }
   }

   /* Clone into an already allocated BigDecimal object */
   private final static void clone(BigDecimal copy,BigDecimal dec){
      copy.flags= dec.flags;
      copy.cachedScale=dec.cachedScale;
      copy.laside =dec.laside;
      copy.bi=dec.bi;
   }

   /* Clone this BigDecimal */
   private final static BigDecimal clone(BigDecimal dec){

      BigDecimal copy=new BigDecimal();
      copy.flags = dec.flags;
      copy.cachedScale=dec.cachedScale;
      copy.laside =dec.laside;

      // we should only clone the BI
      // object if we are cloning a BigDecimal to
      // avoid potential stale values in this.bi when
      // this.laside gets updated
      if((dec.flags & 0x02) == 0x02) {
         copy.bi=dec.bi;
      }
      return copy;
   }

   /**
    * Answers a BigDecimal which is the receiver rounded according to the mode
    * given in the Math context.
    *
    * @param set defines the precision and rounding behaviour
    * @return a BigDecimal rounded according to the given mode
    */
   public BigDecimal round(MathContext set){

      BigDecimal res = clone(this);
       if (set.getPrecision()> 0){
         if (DFPHWAvailable() && ((res.flags & ~0xFFFFFFFC) == 0x00000000)) //DFP
            res.DFPRoundHelper(set.getPrecision(), set.getRoundingMode().ordinal());
         else if ((res.flags & 0x00000002) == 0x00000002) //BI
            res.roundBI(set.getPrecision(),set.getRoundingMode().ordinal(),false);
         else { // LL
            if ((res.flags & ~0xFFFFFFFC) == 0x00000000) {
               res.DFPToLL();
            }
            res.roundLL(set.getPrecision(),set.getRoundingMode().ordinal(),false);
         }
      }
      return res;
   }

   /* len = new length of BigDecimal
    * mode = roundingmode
    * ignoreExact - whether we should return right away if prec == 0
    */
   private final void roundBI(long len, int mode, boolean ignoreExact){

      long adjust;
      int sign;
      BigInteger oldBI;
      BigInteger newBI;
      long interm_exp=0;

      // the number of digits being chopped off
      adjust=(long)this.precision()-len;

      // if nothing to be done
      if (adjust<=0)
         return;

      //if we're not ignoring exact precision, don't round if we need exact precision
      if (!ignoreExact)
         if (len == 0)
            return;

      // to check for scale overflow
      interm_exp = -(long)this.cachedScale+(long)adjust;
      sign =this.bi.signum();
      oldBI= this.bi; // save

      BigInteger rem = null;
      BigInteger rest = null;
      byte first = 0;

      // store the first of the discarded digits
      if (len>0){

         /* We want the first discarded digit, and the rest.
          * We also want the first+rest (in remainder form)
          * We also want the final cut-down version prior to rounding it
          */
         BigInteger [] quotRemAdjust=null;
         quotRemAdjust= oldBI.divideAndRemainder(powerOfTenBI(adjust));

         this.bi = quotRemAdjust[0]; //new value prior to incrementing it
         rem = quotRemAdjust[1]; //remainder

         first =  quotRemAdjust[1].divide(powerOfTenBI(adjust-1)).byteValue(); //first discarded digit
         rest =  quotRemAdjust[1].remainder(powerOfTenBI(adjust-1));//remaining discarded digit
      }
      else{// len<=0
         this.bi = BigInteger.ZERO;
         if (len==0){
            rem = oldBI.remainder(powerOfTenBI(adjust));
            first = rem.divide(powerOfTenBI(adjust-1)).byteValue();
            rest = rem.remainder(powerOfTenBI(adjust-1));
         }
         else{
            rem = oldBI;
            first=0; // [virtual digit]
            rest=oldBI;
         }
      }

      if (sign==-1) //need first to be positive
         first*=-1;
      if (rest.signum() < 0) //need rest to be positive
         rest = rest.abs();

      //NOTE:  Clear precision cache
      this.flags&=0xFFFFFFEF; //clear prec cache bit

      int increment=0;  // we start off by NOT incrementing

      // figure out which way we're going to round

      // Check common rounding mode first
      if (mode == BigDecimal.ROUND_HALF_UP){
         if (first>=5)
            increment=sign;
         else if (first == 5)
            if (rest.signum() > 0)
               increment=sign;
         // else behave like Round Down (i.e. do nothing)
      }

      else if (mode == BigDecimal.ROUND_UP){
         if (rem.signum() != 0)
            increment=sign;
      }

      else if (mode == BigDecimal.ROUND_DOWN)
         ; // never increment

      else if (mode == BigDecimal.ROUND_CEILING){ // more positive
         if (sign==1) {
            if (first != 0 || rest.signum() !=0)
               increment = sign;
         }
      }

      else if (mode == BigDecimal.ROUND_FLOOR){ // more negative
         if (sign==-1){
            if (first!=0 || rest.signum() !=0)
               increment=sign;
         }
      }

      else if (mode == BigDecimal.ROUND_HALF_DOWN){
         if (first>5)
            increment=sign;
         else if (first == 5)
            if (rest.signum() > 0)
               increment=sign;
         // else behave like Round Down (i.e. do nothing)
      }

      else if (mode == BigDecimal.ROUND_HALF_EVEN){
         if (first>5) //if not equidistant
            increment=sign;
         else if (first==5){ //if equidistant, round to nearest even neighbor
            if (rest.signum() !=0)
               increment=sign;
            else if ((this.bi.abs().remainder(BigInteger.TEN).byteValue())%2 == 1) //is last digit in chopped BI odd?
               increment=sign;
         }
      }

      else if (mode == BigDecimal.ROUND_UNNECESSARY){ // default for setScale()
         // discarding any non-zero digits is an error
         if (first!=0 || rest.signum() !=0) {
            // math.1B = rounding\ necessary
            throw new ArithmeticException(Messages.getString("math.1B")); //$NON-NLS-1$
         }
      }

      else {
         // math.27 = Bad round value: {0}
         throw new IllegalArgumentException(Messages.getString("math.27", Integer.toString(mode))); //$NON-NLS-1$
      }

      // if we're incrementing
      if (increment != 0) {

         //if originally zero
         if (sign == 0){

            // test resulting exponent
            if (-interm_exp < (long)Integer.MIN_VALUE || -interm_exp > (long)Integer.MAX_VALUE) {
               scaleOutOfRange(-interm_exp);
            }

            // increment to 1
            this.bi = BigInteger.ONE;

            /* cache - SGP ESRR */

            // cache precision of 1
            this.flags|= 0x4; //cache on
            this.flags&= 0x0000007F; //clear precision bits
            this.flags|= 1 << 7; //precision bits

            this.cachedScale = (int)-interm_exp;
         }
         else{

            if (increment == 1)
               newBI = this.bi.add(BigInteger.ONE);
            else
               newBI = this.bi.subtract(BigInteger.ONE);

            // check for a carry...
            int leng = precisionBI(newBI);
            if (this.precision() < leng){

               //NOTE:  Clear precision cache
               this.flags&=0xFFFFFFEF; //clear prec cache bit

               // increment exponent
               interm_exp = interm_exp+1L;

               //drop the rightmost digit
               newBI = newBI.divide(BigInteger.TEN);
            }

            // save the new BI
            this.bi = newBI;

            // check the scale..
            if (-interm_exp < (long)Integer.MIN_VALUE || -interm_exp > (long)Integer.MAX_VALUE) {
               scaleOutOfRange(-interm_exp);
            }

            // store the exponent
            this.cachedScale = (int)-interm_exp;
         }
      }
      else{ //if we're not incrementing

         // check the scale
         if (-interm_exp < (long)Integer.MIN_VALUE || -interm_exp > (long)Integer.MAX_VALUE) {
            scaleOutOfRange(-interm_exp);
         }

         this.cachedScale = (int)-interm_exp;
      }

      if (this.bi.bitLength()<63)
         this.BIToLL(); //place in LL form if we can
   }

   private final void roundLL(long len, int rm, boolean ignoreExact){

      long adjust;
      int sign;
      long oldlaside;
      byte first=0;
      long rest=0;
      int increment=0;
      long newlaside=0;
      long interm_exp=0;
      boolean fallBackToBI=false;
      int precision = 0;
      int tempFlags = this.flags;

      //  have we cached it?
      if ((tempFlags & 0x10) !=0){
         precision = (tempFlags & 0xFFFFFF80 ) >> 7;
      }
      else{
         // cache the precision
         precision = numDigits(this.laside);
         tempFlags|=0x10; //cache on
         tempFlags&=0x7F; //clear pre-existing bits
         tempFlags |= precision << 7;
         this.flags=tempFlags;
      }

      //how many digits must we remove from the mantissa?
      adjust=(long)precision-len;

      //how can we adjust the mantissa to a negative length?
      if (adjust<=0)
         return;

      //if we're not ignoring exact precision, don't round if we need exact precision
      if (!ignoreExact)
         if (len == 0)
            return;

      interm_exp = -(long)this.cachedScale+(long)adjust;
      oldlaside=this.laside; // save
      sign=((int)(oldlaside>> 63)) | ((int) ((-oldlaside) >>> 63));
      if (sign == -1)oldlaside *=-1L;

      if (len>0){
         first=(byte)digitAt(oldlaside,precision,(int)len);
         rest=oldlaside%powerOfTenLL(adjust-1);

         // remove the unwanted digits
         this.laside=oldlaside/powerOfTenLL(adjust); //adjust guaranteed to be below 19

         //set new precision
         this.flags &= 0x7F; //clear prec bits
         this.flags|= numDigits(this.laside) << 7;
      }
      else{// len<=0
         this.laside = 0L;
         if (len==0){  //xyzabc
            first=(byte)digitAt(oldlaside,precision,0); //x
            rest=oldlaside%powerOfTenLL(adjust-1); //yzabc
         }
         else{
            first=(byte)0; // [virtual digit]
            rest=oldlaside;
         }

         //set precision to 1
         this.flags &= 0x7F; //clear prec bits
         this.flags |= 1 << 7;
      }

      //NOTE:  We do not cache precision!
      this.flags&=0xFFFFFFEF; //clear prec cache bit

      // Check common rounding mode first
      if (rm == BigDecimal.ROUND_HALF_UP){ // default first [most common]
         /*if (first>=5)
            increment=sign;*/
         if (first>=5)
            increment=sign;
         else if (first==5){
            if (rest!=0)
               increment=sign;
         }
      }
      else if (rm == BigDecimal.ROUND_HALF_DOWN){ // 0.5000 goes down
         if (first>5)
            increment=sign;
         else if (first==5){
            if (rest!=0)
               increment=sign;
         }
      }
      else if (rm == BigDecimal.ROUND_HALF_EVEN){ // 0.5000 goes down if left digit even
         if (first>5) //if not equidistant
            increment=sign;
         else if (first==5){ //if equidistant, round to nearest even neighbor
            if (rest!=0)
               increment=sign;
            else if (digitAt(laside,numDigits(laside),numDigits(laside)-1) %2 ==1)
               increment=sign;
         }
      }
      else if (rm == BigDecimal.ROUND_DOWN)
         ; // never increment

      else if (rm == BigDecimal.ROUND_UP){ // increment if discarded non-zero
         if (first!=0 || rest!=0)
            increment=sign;
      }
      else if (rm == BigDecimal.ROUND_CEILING){ // more positive
         if (sign==1) {
            if (first!=0 || rest!=0)
               increment=sign;
         }
      }
      else if (rm == BigDecimal.ROUND_FLOOR){ // more negative
         if (sign==-1){
            if (first!=0 || rest!=0)
               increment=sign;
         }
      }
      else if (rm == BigDecimal.ROUND_UNNECESSARY){ // default for setScale()
         // discarding any non-zero digits is an error
         if (first!=0 || rest!=0) {
            // math.1B = rounding necessary
            throw new ArithmeticException(Messages.getString("math.1B")); //$NON-NLS-1$
         }
      }
      else {
         // math.27 = Bad round value: {0}
         throw new IllegalArgumentException(Messages.getString("math.27", Integer.toString(rm))); //$NON-NLS-1$
      }

      //note: increment is going to be iszero, isneg or ispos
      if (increment!=0){
         if (sign==0){

            // we must not subtract from 0, but result is trivial anyway
            this.laside=1L;

            // set precision to 1
            this.flags &= 0x7F; //clear prec bits
            this.flags |= 1 << 7;
         }
         else{
            // mantissa is non-0; we can safely add or subtract 1

            //need to set increment to 1 or -1, right now it's ispos/isneg/iszero
            if (sign==-1 )//flip the increment value
               increment*=-1;
            long sum = this.laside+increment;
            if (overflowAdd(this.laside,increment,sum)==0)
               newlaside=sum;
            else fallBackToBI=true;

            if(fallBackToBI){
               //change to BI rep and repeat...
               this.laside=oldlaside;
               this.LLToBI();
               this.roundBI(len,rm,ignoreExact);
               return;
            }

            //check to see if a 9 rounded up became a 10...
            if (numDigits(newlaside)>this.precision()){
               //drop rightmost digit and raise exponent
               interm_exp = interm_exp+1L;
               if (Math.abs(newlaside) > Integer.MAX_VALUE)
                  this.laside=newlaside/10L; //long divide
               else
                  this.laside = (((int)newlaside)/10); //integer divide
            }else
               this.laside=newlaside;
         }
      }

      if (-interm_exp < (long)Integer.MIN_VALUE || -interm_exp > (long)Integer.MAX_VALUE) {
         scaleOutOfRange(-interm_exp);
      }

      this.cachedScale = (int)-interm_exp;

      // set precision
      this.flags &= 0x7F; //clear prec bits
      this.flags |= numDigits(this.laside) << 7;

      if (sign == -1)
         this.laside*=-1L;
   }

   /* Only returns positive values - a negative only if long-related functionality fails */
   private final static long roundPostLLDivision(long quotient, int quotInd, long divisor,
                     long remainder, int rm){

      // Check common rounding mode first
      if (rm == BigDecimal.ROUND_HALF_UP){
         //no fear of overflowing...
         if (remainder < (Long.MAX_VALUE/2)){
            if (2*remainder >= divisor){ //if remainder is >= 0.5
               long sum = quotient + 1;
               if (overflowAdd(quotient,1,sum)==1)
                  return -1;
               quotient=sum;
            }
         }
         else{ //we're gonna increment if it would have overflowed...
            long sum = quotient + 1;
            if (overflowAdd(quotient,1,sum)==1)
               return -1;
            quotient=sum;
         }
      }

      else if (rm == BigDecimal.ROUND_UP){
         if (remainder > 0){//if (remainder > 0){
            long sum = quotient + 1;
            if (overflowAdd(quotient,1,sum)==1)
               return -1;
            quotient=sum;
         }
      }
      else if (rm == BigDecimal.ROUND_DOWN)
         //never increments
         ;
      else if (rm == BigDecimal.ROUND_CEILING){
         if (remainder > 0){//if (remainder > 0){
            if (quotInd == 1){
               long sum = quotient + 1;
               if (overflowAdd(quotient,1,sum)==1)
                  return -1;
               quotient=sum;
            }
            //don't care about negative case
         }
      }
      else if (rm == BigDecimal.ROUND_FLOOR){
         if (remainder > 0){//if (remainder > 0){
            if (quotInd == -1){
               long sum = quotient + 1;
               if (overflowAdd(quotient,1,sum)==1)
                  return -1;
               quotient=sum;
            }
            //don't care about positive case
         }
      }
      else if (rm == BigDecimal.ROUND_HALF_DOWN){
         //no fear of overflowing...
         if (remainder < (Long.MAX_VALUE/2)){
            if (2*remainder > divisor){ //if remainder is > 0.5
               long sum = quotient + 1;
               if (overflowAdd(quotient,1,sum)==1)
                  return -1;
               quotient=sum;
            }
         }
         else{ //we're gonna increment if it would have overflowed...
            long sum = quotient + 1;
            if (overflowAdd(quotient,1,sum)==1)
               return -1;
            quotient=sum;
         }
      }
      else if (rm == BigDecimal.ROUND_HALF_EVEN){
         //no fear of overflowing...
         if (remainder < (Long.MAX_VALUE/2)){
            if (2*remainder > divisor){ //if remainder is > 0.5
               long sum = quotient + 1;
               if (overflowAdd(quotient,1,sum)==1)
                  return -1;
               quotient=sum;
            }
            else if (2*remainder < divisor)
               ; //don't do anything
            else{ //check to see if last digit is even
               if ((quotient%10)%2 == 1){ //it's odd
                  long sum = quotient + 1;
                  if (overflowAdd(quotient,1,sum)==1)
                     return -1;
                  quotient=sum;
               }
            }
         }
         else{ //we're gonna increment if it would have overflowed...
            long sum = quotient + 1;
            if (overflowAdd(quotient,1,sum)==1)
               return -1;
            quotient=sum;
         }
      }

      else if (rm == BigDecimal.ROUND_UNNECESSARY){
         if (remainder !=0) {
            // math.30 = Rounding unnecessary for inexact result
            throw new ArithmeticException(Messages.getString("math.30")); //$NON-NLS-1$
         }
      }

      return quotient;
   }

   private final static BigInteger roundPostSlowDivision(
         BigInteger quotient, int quotInd, BigInteger divisor,
         BigInteger remainder, int rm){

      // Check common rounding mode first
      if (rm == BigDecimal.ROUND_HALF_UP){
         if (remainder.shiftLeft(1).compareTo(divisor) >= 0) //if remainder is >= 0.5
            quotient = quotient.add(BigInteger.ONE);
      }
      else if (rm == BigDecimal.ROUND_UP){
         if (remainder.signum()> 0)//if (remainder > 0){
            quotient = quotient.add(BigInteger.ONE);
      }
      else if (rm == BigDecimal.ROUND_DOWN)
         //never increments
         ;
      else if (rm == BigDecimal.ROUND_CEILING){
         if (remainder.signum()> 0){//if (remainder > 0){
            if (quotInd == 1)
               quotient = quotient.add(BigInteger.ONE);
            //don't care about negative case
         }
      }
      else if (rm == BigDecimal.ROUND_FLOOR){
         if (remainder.signum() > 0){//if (remainder > 0){
            if (quotInd == -1)
               quotient = quotient.add(BigInteger.ONE);
            //don't care about positive case
         }
      }
      else if (rm == BigDecimal.ROUND_HALF_DOWN){
         if (remainder.shiftLeft(1).compareTo(divisor) > 0) //if remainder is > 0.5
            quotient = quotient.add(BigInteger.ONE);
      }
      else if (rm == BigDecimal.ROUND_HALF_EVEN){
         if (remainder.shiftLeft(1).compareTo(divisor) > 0) //if remainder is > 0.5
            quotient = quotient.add(BigInteger.ONE);
         else if (remainder.shiftLeft(1).compareTo(divisor) < 0)
            ; //don't do anything
         else{ //check to see if last digit is even
            if ((quotient.mod(BigInteger.TEN).mod(
                  new BigInteger("2"))).compareTo( //$NON-NLS-1$
                        BigInteger.ONE)==0) //it's odd
               quotient = quotient.add(BigInteger.ONE);
         }
      }

      else if (rm == BigDecimal.ROUND_UNNECESSARY){
         if (remainder.signum() !=0) {
            // math.30 = Rounding unnecessary for inexact result
            throw new ArithmeticException(Messages.getString("math.30")); //$NON-NLS-1$
         }
      }

      return quotient;
   }

   /* Tests rightmost num.length-start digits to see if all 0.
    * i.e. num = 23456, start = 2 --> 23(456)
    */

   private final static boolean allzeroBCD(long num, int numDigsToCheck){

      // isolate numDigsToCheck rightmost digits...
      long mask = 0xFFFFFFFFFFFFFFFFL;
      mask >>>= (64-numDigsToCheck)*4;
      return ((mask & num) == 0);
   }

   private final void finish(int prec, int rm){

      if (prec > 0 && this.precision() > prec){

         if ((this.flags & ~0xFFFFFFFC) == 0x00000000) // DFP
            this.DFPRoundHelper(prec,rm);
         else if ((this.flags & ~0xFFFFFFFC) == 0x00000002) //BI
            this.roundBI(prec,rm,false);
         else // LL
            this.roundLL(prec,rm,false);
      }
   }

   private static final BigInteger powerOfTenBI(long i){

      if (i > -1 && i <= 18)
         return powersOfTenBI[(int)i];
      else{
         char [] ten = new char[(int)i+1];
         Arrays.fill(ten,'0');
         ten[0] = '1';
         return new BigInteger( new String(ten));
      }
   }

   private static final long powerOfTenLL(long i){
      if (i > -1 && i <= 18)
         return powersOfTenLL[(int)i];
      else
         return -1;
   }

   //prereq - num must be of length 19 or less... and can
   //not have a leading sign.. i.e. it must be a String of 19
   //or less digits between 0 and 9
   private final static long toLongForm(char [] num){

      //iterate from L to R in the String,
      //accumulting the digit, and then multiplying the long by 10
      long laside=0L;
      for (int i=0; i < num.length; i++){
         laside *=10;
         laside += (int)(num[i] - 48);
      }
      return laside;
   }

   /* Convert internal representation from BI to LL
    * Used when taking the BigInteger slow path and we'd like
    * to coerce the result of an operation into LL form */

   private final void BIToLL(){

      // Use the same logic here as we do in the bigIntegerConstructor
 
         // store as LL representation
         this.flags &= 0xFFFFFFFC; // clear bits
         this.flags |= 0x00000001;

         // store the long lookaside
         this.laside=bi.longValue();

         // clear this - very important since
         // use the unscaledValue() trick of
         // caching BigInteger regardless of the
         // internal representation
         this.bi = null;
  }

   /* Convert internal representation from LL to BI */
   private final void LLToBI(){

      // Perform this only for LL representation
      if ((this.flags & ~0xFFFFFFFC) == 0x00000001){

         // change the representation flag bit last
         // since all calls before it depend on it

         // store the BigInteger
         this.bi = BigInteger.valueOf(this.laside);

         // cache the precision
         this.flags&=0x7F; // clear the cached precision
         this.flags|=0x10; //caching the precision
         this.flags |= numDigits(this.laside) << 7;

         // set the BigInteger representation
         this.flags &= 0xFFFFFFFC; // clear bits
         this.flags |= 0x00000002;
      }
   }

   /* Convert from DFP internal representation to LL representation */

   private final void DFPToLL(){

      //quick check for 0
      if (this.laside == dfpZERO){

         // set representation as long lookaside
         this.flags &= 0xFFFFFFFC; //clear bits
         this.flags |= 0x00000001;

         // reset exponent
         this.cachedScale = 0;

         // set long lookaside
         this.laside = 0;

         // cache the precision of 1
         this.flags&=0x7F; // clear the cached precision
         this.flags|=0x10; //caching the precision
         this.flags |= 1 << 7; //precision of 1
      }
      else{

         // need to store representation and new long lookaside last
         // since helper functions that are called
         // depend on the correct internal representation flag bit

         int signum = DFPSignumHelper();

         //store the exponent
         this.cachedScale = this.scale();

         //cache the precision
         int prec = this.DFPPrecisionHelper();
         this.flags&=0x7F; // clear the cached precision
         this.flags &= 0xFFFFFFEF;//clear bits
         this.flags |= 0x10; //cache on
         this.flags |= prec << 7;

         long lVal = DFPUnscaledValue(this.laside);
         if (lVal == Long.MAX_VALUE){
            lVal = DFPBCDDigits(this.laside);
            if (lVal == 10)
               lVal = extractDFPDigitsBCD(this.laside);

            // now extract each 4 bit BCD digit from left to right
            long val=0;  //to store the result
            int i=0;
            while (lVal!= 0){
               val += (lVal & 0xF) * powerOfTenLL(i++);
               lVal >>>= 4;
            }

            //is the sign negative?
            this.laside = val*signum;
         }
         else
            this.laside = lVal;

         // store representation as long lookaside
         this.flags &= 0xFFFFFFFC;
         this.flags |= 0x00000001;
      }

      // need to make sure bi is not cached by previous calls to unscaled value
      this.bi = null;
   }

   /* Convert from DFP internal representation to BI representation */

   private final void DFPToBI(){

      //quick check for 0
      if (this.laside == dfpZERO){

         //store BigInteger representation
         this.flags &= 0xFFFFFFFC; //clear bits
         this.flags |= 0x00000002;

         //clear exponent
         this.cachedScale = 0;

         //store BI
         this.bi = BigInteger.ZERO;

         // cache the precision of 1
         this.flags&=0x7F; // clear the cached precision
         this.flags|=0x10; //caching the precision
         this.flags |= 1 << 7; //precision of 1
      }
      else{

         // need to store representation last
         // since helper functions that are called
         // depend on the correct internal representation flag bit

         //store the exponent
         this.cachedScale = this.scale();

         //store the BigInteger
         this.bi = this.DFPUnscaledValueHelper();

         /* cache - SGP ESRR */

         //cache the precision
         int prec = this.DFPPrecisionHelper();
         this.flags&=0x7F; // clear the cached precision
         this.flags |= 0x10; //cache on
         this.flags |= prec << 7;

         // store representation as BigInteger
         this.flags &= 0xFFFFFFFC; //clear bits
         this.flags |= 0x00000002;
      }
   }

   /* Used on an integer from a BigInteger array */
   private final static int numDigitsBI(int in){

      // if the result is negative, the highest bit is set
      if (in < 0 )
         return 10;
      else{
         if (in < 1000000000 /*powerOfTenLL[9]*/){
            if (in < 100000000 /*powerOfTenLL[8]*/){
               if (in < 10000 /*powerOfTenLL[4]*/){
                  if (in < 100 /* powerOfTenLL[2]*/){
                     if (in < 10 /*powerOfTenLL[1]*/)
                        return 1;
                     else
                        return 2;
                  }
                  else if (in < 1000 /*powerOfTenLL[3]*/)
                     return 3;
                  else
                     return 4;
               }
               else if (in < 1000000 /*powerOfTenLL[6]*/){
                  if ( in < 100000 /*powerOfTenLL[5]*/)
                     return 5;
                  else
                     return 6;
               }
               else if (in < 10000000 /*powerOfTenLL[7]*/)
                  return 7;
               else return 8;
            }
            else
               return 9;
         }
         else
            return 10;
      }
   }

   /* Return number of digits in lon
    * The value '0' has numDigits = 1.
    */
   private final static int numDigits(long lon){
      lon = Math.abs(lon);

      //hardcode powers of ten to avoid LUT lookups

      //rhs of the tree
      if (lon < 1000000000L /*powerOfTenLL[9]*/){
         if (lon < 100000000L /*powerOfTenLL[8]*/){
            if (lon < 10000L /*powerOfTenLL[4]*/){
               if (lon < 100L /* powerOfTenLL[2]*/){
                  if (lon < 10L /*powerOfTenLL[1]*/)
                     return 1;
                  else
                     return 2;
               }
               else if (lon < 1000L /*powerOfTenLL[3]*/)
                  return 3;
               else
                  return 4;
            }
            else if (lon < 1000000L /*powerOfTenLL[6]*/){
               if ( lon < 100000L /*powerOfTenLL[5]*/)
                  return 5;
               else
                  return 6;
            }
            else if (lon < 10000000L /*powerOfTenLL[7]*/)
               return 7;
            else return 8;
         }
         else
            return 9;
      }
      //lhs of the tree
      else{
         if (lon < 10000000000L /*powerOfTenLL[10]*/)
            return 10;
         else if (lon < 100000000000000L /*powerOfTenLL[14]*/){
            if (lon < 1000000000000L /*powerOfTenLL[12]*/){
               if (lon < 100000000000L /*powerOfTenLL[11]*/)
                  return 11;
               else
                  return 12;
            }
            else if (lon < 10000000000000L /*powerOfTenLL[13]*/)
               return 13;
            else
               return 14;
         }
         else if (lon < 10000000000000000L /*powerOfTenLL[16]*/){
            if (lon < 1000000000000000L /*powerOfTenLL[15]*/)
               return 15;
            else
               return 16;
         }
         else if (lon < 100000000000000000L /*powerOfTenLL[17]*/)
            return 17;
         else if (lon < 1000000000000000000L /*powerOfTenLL[18]*/)
            return 18;
         return 19;
      }
   }

   /* Returns the digit in lon, at location loc.
    * Leftmost digit in lon is index value 0
    *
    *i.e. 123456, loc=3 - returns 4
    *i.e. 123456, loc=8 - returns 0
    */
   private final static int digitAt(long lon, int numdigits, int loc){

      lon = Math.abs(lon);
      if (loc > numdigits-1)return -1;
      if (loc < 0) return -1;
      int indexFromRight = numdigits-loc-1;
      if (lon <= Integer.MAX_VALUE){
         int temp=(int)lon;
         switch (indexFromRight){
         case 0:
            break;
         case 1:
            temp /=10;
            break;
         case 2:
            temp /=100;
            break;
         case 3:
            temp /=1000;
            break;
         case 4:
            temp /=10000;
            break;
         case 5:
            temp /=100000;
            break;
         case 6:
            temp /=1000000;
            break;
         case 7:
            temp /=10000000;
            break;
         case 8:
            temp /=100000000;
            break;
         case 9:
            temp /=1000000000;      //unsure whether remaining cases would
            break;               //ever be taken in the Integer case
         case 10:
            temp /=10000000000L;
            break;
         case 11:
            temp /=100000000000L;
            break;
         case 12:
            temp /=1000000000000L;
            break;
         case 13:
            temp /=10000000000000L;
            break;
         case 14:
            temp /=100000000000000L;
            break;
         case 15:
            temp /=1000000000000000L;
            break;
         case 16:
            temp /=10000000000000000L;
            break;
         case 17:
            temp /=100000000000000000L;
            break;
         case 18:
            temp /=1000000000000000000L;
            break;
         }

         // find remainder
         if (temp <= Integer.MAX_VALUE){
            int intTmp = (int)temp;
            int tmpVal = intTmp;
            intTmp = uDivideByTen(intTmp);
            return (tmpVal - ((intTmp << 3) + (intTmp << 1)));
         }
         else{
            long tmpVal = temp;
            temp/=10;
            return (int)(tmpVal - (((temp << 3) + (temp << 1))));
         }
      }
      else{
         long temp=lon;
         switch (indexFromRight){
         case 0:
            break;
         case 1:
            temp /= 10;
            break;
         case 2:
            temp /= 100;
            break;
         case 3:
            temp /= 1000;
            break;
         case 4:
            temp /= 10000;
            break;
         case 5:
            temp /= 100000;
            break;
         case 6:
            temp /= 1000000;
            break;
         case 7:
            temp /= 10000000;
            break;
         case 8:
            temp /= 100000000;
            break;
         case 9:
            temp /= 1000000000;
            break;
         case 10:
            temp /= 10000000000L;
            break;
         case 11:
            temp /= 100000000000L;
            break;
         case 12:
            temp /= 1000000000000L;
            break;
         case 13:
            temp /= 10000000000000L;
            break;
         case 14:
            temp /= 100000000000000L;
            break;
         case 15:
            temp /= 1000000000000000L;
            break;
         case 16:
            temp /= 10000000000000000L;
            break;
         case 17:
            temp /= 100000000000000000L;
            break;
         case 18:
            temp /= 1000000000000000000L;
            break;
         }

         // find remainder
         if (temp <= Integer.MAX_VALUE){
            int intTmp = (int)temp;
            int tmpVal = intTmp;
            intTmp = uDivideByTen(intTmp);
            return (tmpVal - ((intTmp << 3) + (intTmp << 1)));
         }
         else{
            long tmpVal = temp;
            temp/=10;
            return (int)(tmpVal - (((temp << 3) + (temp << 1))));
         }
      }
   }

   /* Same as digitAt, except working on a 64-bit unsigned BCD */

   private final static int digitAtBCD(long bcd, int numDigits, int indexFromLeft){

      int indexFromRight = numDigits-indexFromLeft-1;
      switch (indexFromRight){
      case 0:
         return (int)(bcd &  0x000000000000000FL);
      case 1:
         return (int)((bcd & 0x00000000000000F0L)>>>4);
      case 2:
         return (int)((bcd & 0x0000000000000F00L)>>>8);
      case 3:
         return (int)((bcd & 0x000000000000F000L)>>>12);
      case 4:
         return (int)((bcd & 0x00000000000F0000L)>>>16);
      case 5:
         return (int)((bcd & 0x0000000000F00000L)>>>20);
      case 6:
         return (int)((bcd & 0x000000000F000000L)>>>24);
      case 7:
         return (int)((bcd & 0x00000000F0000000L)>>>28);
      case 8:
         return (int)((bcd & 0xF00000000L)>>>32);
      case 9:
         return (int)((bcd & 0xF000000000L)>>>36);
      case 10:
         return (int)((bcd & 0xF0000000000L)>>>40);
      case 11:
         return (int)((bcd & 0xF00000000000L)>>44);
      case 12:
         return (int)((bcd & 0xF000000000000L)>>>48);
      case 13:
         return (int)((bcd & 0xF0000000000000L)>>>52);
      case 14:
         return (int)((bcd & 0xF00000000000000L)>>>56);
      case 15:
         return (int)((bcd & 0xF000000000000000L)>>>60);
      default:
         return 0;
      }
   }

   /* returns true if lhs + rhs results in overflow
    * prerequirements: none (lhs, rhs are signed 64 bit longs)
    */

   private final static long overflowAdd(long lhs, long rhs, long sum){

      //adapted fr hacker's delight p. 27 (left column, bottom x+y+c)
      //detecting if overflow in addition - only when signs are the same
      /*long z=0;
      z = ~(lhs ^ rhs) & 0x8000000000000000L; //z gets 0x80000..L if sign(rhs)=sign(lhs), or else 0
      z = z & ~(((lhs ^ z)+rhs)^rhs);

      //msb in z will be 1 iff lhs, rhs are both positive or negative and overflow
      //if lhs, rhs are of different signs, msb will not be 1
      return (z < -1); // same as expression above*/
      return (((sum)^lhs) & ((sum)^rhs)) >>> 63;

   }

   /* returns true if lhs * rhs causes an overflow..
    * it returns true for some cases that don't, but holds true for all cases that do
    * prerequisite: lhs, rhs must be unsigned longs (they are therefore treated
    * as 63 bit values since the most significant bit will never be set), so
    * that we can call numLeadingZeros63
    */

   private final static boolean overflowMultiply(long lhs, long rhs){

      if (lhs < 0)lhs *=-1;//throw new ArithmeticException("Invalid argument");
      if (rhs < 0)rhs *=-1;//throw new ArithmeticException("Invalid argument");

      //adapted from hacker's delight, p. 31
      
      /* Idea here is that 63 bit x 63 bit will fit under 126...
       * and we expect the product to have n+m or n+m+1 leading 0s
       * (where n=lz(lhs), m=lz(rhs)
       *
       * We will get overflow, therefore, if the leading number of 0s
       * in the 126 bit product is less than 63...
       */

      if (lhs == 0|| rhs==0)return false;
      if (lhs == 1|| rhs==1)return false;

      int m=0;
      int n=0;
      m = Long.numberOfLeadingZeros(lhs)-1;
      n = Long.numberOfLeadingZeros(rhs)-1;
      if (m+n<63)return true;
      else return false;
   }

   /* Division by 10 using shifts and adds
    * prereq - x is unsigned int
    */

   private final static int uDivideByTen(int x){
      int q = (x >> 1) + (x >> 2);
      q = q + (q >> 4);
      q = q + (q >> 8);
      q = q + (q >> 16);
      q >>=3;
      x -= q*10;
      return q + ((x + 6) >> 4);
   }

   private synchronized void writeObject(java.io.ObjectOutputStream out) throws IOException{
      ObjectOutputStream.PutField fields = out.putFields();
      fields.put("scale", this.scale()); //$NON-NLS-1$
      fields.put("intVal", this.unscaledValue()); //$NON-NLS-1$
      out.writeFields();
   }

   private synchronized void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{

      ObjectInputStream.GetField fields = in.readFields();

      BigInteger bi = (BigInteger)fields.get("intVal", null); //$NON-NLS-1$
      int scale = (int)fields.get("scale",0); //$NON-NLS-1$

      //Now convert to our liking - same as BigInteger constructor
      //without any respect to MathContext
      if (bi == null) {
         // math.37 = Referenced field was null
         throw new IOException(Messages.getString("math.37")); //$NON-NLS-1$
      }

      this.bigIntegerConstructor(bi, scale, MathContext.UNLIMITED);
   }

   /* Initializes the look-aside-table used to lookup the first 2 bits of the exponent
    * and the leftmost digit of the DFP - combo field is used as the lookup
    * and the result is a byte of the form 00xxxxyy where
    *
    * xxxx = left most digit
    * yy = first two bits of the exponent
    *
    * Does not check for infinities, or NaN.
    */
   private final static byte [] comboinit(){
      final byte comboCode[]={
            0, 4, 8, 12, 16, 20, 24, 28,
            1, 5, 9, 13, 17, 21, 25, 29,
            2, 6, 10, 14, 18, 22, 26, 30,
            32, 36, 33, 37, 34, 38
      };
      return comboCode;
   }

   private final static short [] dpd2bcdinit(){
      final short[] dpd2bcd = {
            0,    1,    2,    3,    4,    5,    6,    7,
            8,    9,  128,  129, 2048, 2049, 2176, 2177,   16,   17,   18,   19,   20,
            21,   22,   23,   24,   25,  144,  145, 2064, 2065, 2192, 2193,   32,   33,
            34,   35,   36,   37,   38,   39,   40,   41,  130,  131, 2080, 2081, 2056,
            2057,   48,   49,   50,   51,   52,   53,   54,   55,   56,   57,  146,  147,
            2096, 2097, 2072, 2073,   64,   65,   66,   67,   68,   69,   70,   71,   72,
            73,  132,  133, 2112, 2113,  136,  137,   80,   81,   82,   83,   84,   85,
            86,   87,   88,   89,  148,  149, 2128, 2129,  152,  153,   96,   97,   98,
            99,  100,  101,  102,  103,  104,  105,  134,  135, 2144, 2145, 2184, 2185,
            112,  113,  114,  115,  116,  117,  118,  119,  120,  121,  150,  151, 2160,
            2161, 2200, 2201,  256,  257,  258,  259,  260,  261,  262,  263,  264,  265,
            384,  385, 2304, 2305, 2432, 2433,  272,  273,  274,  275,  276,  277,  278,
            279,  280,  281,  400,  401, 2320, 2321, 2448, 2449,  288,  289,  290,  291,
            292,  293,  294,  295,  296,  297,  386,  387, 2336, 2337, 2312, 2313,  304,
            305,  306,  307,  308,  309,  310,  311,  312,  313,  402,  403, 2352, 2353,
            2328, 2329,  320,  321,  322,  323,  324,  325,  326,  327,  328,  329,  388,
            389, 2368, 2369,  392,  393,  336,  337,  338,  339,  340,  341,  342,  343,
            344,  345,  404,  405, 2384, 2385,  408,  409,  352,  353,  354,  355,  356,
            357,  358,  359,  360,  361,  390,  391, 2400, 2401, 2440, 2441,  368,  369,
            370,  371,  372,  373,  374,  375,  376,  377,  406,  407, 2416, 2417, 2456,
            2457,  512,  513,  514,  515,  516,  517,  518,  519,  520,  521,  640,  641,
            2050, 2051, 2178, 2179,  528,  529,  530,  531,  532,  533,  534,  535,  536,
            537,  656,  657, 2066, 2067, 2194, 2195,  544,  545,  546,  547,  548,  549,
            550,  551,  552,  553,  642,  643, 2082, 2083, 2088, 2089,  560,  561,  562,
            563,  564,  565,  566,  567,  568,  569,  658,  659, 2098, 2099, 2104, 2105,
            576,  577,  578,  579,  580,  581,  582,  583,  584,  585,  644,  645, 2114,
            2115,  648,  649,  592,  593,  594,  595,  596,  597,  598,  599,  600,  601,
            660,  661, 2130, 2131,  664,  665,  608,  609,  610,  611,  612,  613,  614,
            615,  616,  617,  646,  647, 2146, 2147, 2184, 2185,  624,  625,  626,  627,
            628,  629,  630,  631,  632,  633,  662,  663, 2162, 2163, 2200, 2201,  768,
            769,  770,  771,  772,  773,  774,  775,  776,  777,  896,  897, 2306, 2307,
            2434, 2435,  784,  785,  786,  787,  788,  789,  790,  791,  792,  793,  912,
            913, 2322, 2323, 2450, 2451,  800,  801,  802,  803,  804,  805,  806,  807,
            808,  809,  898,  899, 2338, 2339, 2344, 2345,  816,  817,  818,  819,  820,
            821,  822,  823,  824,  825,  914,  915, 2354, 2355, 2360, 2361,  832,  833,
            834,  835,  836,  837,  838,  839,  840,  841,  900,  901, 2370, 2371,  904,
            905,  848,  849,  850,  851,  852,  853,  854,  855,  856,  857,  916,  917,
            2386, 2387,  920,  921,  864,  865,  866,  867,  868,  869,  870,  871,  872,
            873,  902,  903, 2402, 2403, 2440, 2441,  880,  881,  882,  883,  884,  885,
            886,  887,  888,  889,  918,  919, 2418, 2419, 2456, 2457, 1024, 1025, 1026,
            1027, 1028, 1029, 1030, 1031, 1032, 1033, 1152, 1153, 2052, 2053, 2180, 2181,
            1040, 1041, 1042, 1043, 1044, 1045, 1046, 1047, 1048, 1049, 1168, 1169, 2068,
            2069, 2196, 2197, 1056, 1057, 1058, 1059, 1060, 1061, 1062, 1063, 1064, 1065,
            1154, 1155, 2084, 2085, 2120, 2121, 1072, 1073, 1074, 1075, 1076, 1077, 1078,
            1079, 1080, 1081, 1170, 1171, 2100, 2101, 2136, 2137, 1088, 1089, 1090, 1091,
            1092, 1093, 1094, 1095, 1096, 1097, 1156, 1157, 2116, 2117, 1160, 1161, 1104,
            1105, 1106, 1107, 1108, 1109, 1110, 1111, 1112, 1113, 1172, 1173, 2132, 2133,
            1176, 1177, 1120, 1121, 1122, 1123, 1124, 1125, 1126, 1127, 1128, 1129, 1158,
            1159, 2148, 2149, 2184, 2185, 1136, 1137, 1138, 1139, 1140, 1141, 1142, 1143,
            1144, 1145, 1174, 1175, 2164, 2165, 2200, 2201, 1280, 1281, 1282, 1283, 1284,
            1285, 1286, 1287, 1288, 1289, 1408, 1409, 2308, 2309, 2436, 2437, 1296, 1297,
            1298, 1299, 1300, 1301, 1302, 1303, 1304, 1305, 1424, 1425, 2324, 2325, 2452,
            2453, 1312, 1313, 1314, 1315, 1316, 1317, 1318, 1319, 1320, 1321, 1410, 1411,
            2340, 2341, 2376, 2377, 1328, 1329, 1330, 1331, 1332, 1333, 1334, 1335, 1336,
            1337, 1426, 1427, 2356, 2357, 2392, 2393, 1344, 1345, 1346, 1347, 1348, 1349,
            1350, 1351, 1352, 1353, 1412, 1413, 2372, 2373, 1416, 1417, 1360, 1361, 1362,
            1363, 1364, 1365, 1366, 1367, 1368, 1369, 1428, 1429, 2388, 2389, 1432, 1433,
            1376, 1377, 1378, 1379, 1380, 1381, 1382, 1383, 1384, 1385, 1414, 1415, 2404,
            2405, 2440, 2441, 1392, 1393, 1394, 1395, 1396, 1397, 1398, 1399, 1400, 1401,
            1430, 1431, 2420, 2421, 2456, 2457, 1536, 1537, 1538, 1539, 1540, 1541, 1542,
            1543, 1544, 1545, 1664, 1665, 2054, 2055, 2182, 2183, 1552, 1553, 1554, 1555,
            1556, 1557, 1558, 1559, 1560, 1561, 1680, 1681, 2070, 2071, 2198, 2199, 1568,
            1569, 1570, 1571, 1572, 1573, 1574, 1575, 1576, 1577, 1666, 1667, 2086, 2087,
            2152, 2153, 1584, 1585, 1586, 1587, 1588, 1589, 1590, 1591, 1592, 1593, 1682,
            1683, 2102, 2103, 2168, 2169, 1600, 1601, 1602, 1603, 1604, 1605, 1606, 1607,
            1608, 1609, 1668, 1669, 2118, 2119, 1672, 1673, 1616, 1617, 1618, 1619, 1620,
            1621, 1622, 1623, 1624, 1625, 1684, 1685, 2134, 2135, 1688, 1689, 1632, 1633,
            1634, 1635, 1636, 1637, 1638, 1639, 1640, 1641, 1670, 1671, 2150, 2151, 2184,
            2185, 1648, 1649, 1650, 1651, 1652, 1653, 1654, 1655, 1656, 1657, 1686, 1687,
            2166, 2167, 2200, 2201, 1792, 1793, 1794, 1795, 1796, 1797, 1798, 1799, 1800,
            1801, 1920, 1921, 2310, 2311, 2438, 2439, 1808, 1809, 1810, 1811, 1812, 1813,
            1814, 1815, 1816, 1817, 1936, 1937, 2326, 2327, 2454, 2455, 1824, 1825, 1826,
            1827, 1828, 1829, 1830, 1831, 1832, 1833, 1922, 1923, 2342, 2343, 2408, 2409,
            1840, 1841, 1842, 1843, 1844, 1845, 1846, 1847, 1848, 1849, 1938, 1939, 2358,
            2359, 2424, 2425, 1856, 1857, 1858, 1859, 1860, 1861, 1862, 1863, 1864, 1865,
            1924, 1925, 2374, 2375, 1928, 1929, 1872, 1873, 1874, 1875, 1876, 1877, 1878,
            1879, 1880, 1881, 1940, 1941, 2390, 2391, 1944, 1945, 1888, 1889, 1890, 1891,
            1892, 1893, 1894, 1895, 1896, 1897, 1926, 1927, 2406, 2407, 2440, 2441, 1904,
            1905, 1906, 1907, 1908, 1909, 1910, 1911, 1912, 1913, 1942, 1943, 2422, 2423,
            2456, 2457};
      return dpd2bcd;
   }

    private final static long extractDFPDigitsBCD(long dfpNum){

       int combo=0; // 5 bit combo field

       //quick check for 0
       if (dfpNum == dfpZERO)
          return 0;

       // store the combo field bits
       combo = (int)(dfpNum >>> 58); //shift out extraneous bits
       combo &= 0x1F; //and out sign bit

       // MANTISSA

       // store the mantissa continuation field bits 14 to 62 (50 bits in total)
       long ccf = dfpNum & 0x0003FFFFFFFFFFFFL;

       // Convert each set of 10 DPD bits from continuation bits to 12 BCD digits
       long ccBCD=0;
       long bcd=0;
       for (int i=0; i <5; i++){ //5 groups of 10 bits
          bcd = BigDecimal.DPD2BCD[(int)((ccf & 0x3FF0000000000L)>>>40)];
          ccBCD <<=12;
          ccBCD|=bcd;
          ccf <<= 10; //shift out top 10 bits
       }

       //ccBCD contains 15 BCD digits, now need to prepend the first one
       ccBCD|=(((long)(BigDecimal.doubleDFPComboField[combo]>>>2))<<60);
       return ccBCD;
   }

   private final static int extractDFPExponent(long dfpNum){

      byte combo=0; // 5 bit combo field

      // store the combo field bits
      combo = (byte)(dfpNum >>> 58); //shift out extraneous bits
      combo &= 0x1F; //and out sign bit

      // store the biased exponent field bits 6 to 13
      short bxf  = (short)(dfpNum >>> 50); //shift out extraneous bits
      bxf &= 0x00FF; //and out sign and combo field

      // parse the combo field
      byte exp2Digits = (byte)(BigDecimal.doubleDFPComboField[combo] & 0x03);

      // unbias the exponent
      short unbExp = (short)(exp2Digits);
      unbExp <<= 8;
      unbExp|=bxf;
      return unbExp;
   }

   private final void DFPConstructZero(){
      this.laside = dfpZERO; //dfp value for +0e0

      // cache the sign (sign bits already off)
      this.flags|=0x4;  //cache on

      // cache the precision of 1
      this.flags&=0x7F; // clear the cached precision
      this.flags|=0x10; //caching the precision
      this.flags |= 1 << 7; //precision of 1

      // cache the exponent (exp already 0)
      this.flags|=0x8;  //cache on

      // store DFP representation
      // (representation bits already 00)
   }

   private final boolean isDFPZero(){

      // quick check for 0
      if (this.laside == dfpZERO)
         return true;

      // if DPF is ZERO, then the combo field will be XX000 where XX is I don't care
      // values, and then coefficient continuation field will be all 0s
      return ((this.laside & 0x0003FFFFFFFFFFFFL) == 0 &&
            ((this.laside >>> 58) & 0x7) == 0);
   }

   /*
    * IMPORANT:
    *
    * DO NOT MODIFY ANY OF THE FOLLOWING METHODS AS THE JIT COMPILER
    * IS DEPENDENT ON THE SIGNATURES BELOW TO INLINE THEM INTO SEQUENCES
    * OF DFP OPERATIONS.
    *
    * NOTE THAT ALL THE INSTANCE METHODS DEFINED BELOW TYPICALLY
    * STORE THE RESULT OF THE DFP OPERATION INTO "THIS" IMPLICITLY
    * PASSED AS ARG0.
    *
    * ALL OTHER DFP METHODS ARE STATIC.
    */

   /*
    * START OF INLINEABLE METHODS
    *
    */

   // DO NOT MODIFY THIS METHOD
   /*
    *  Return value
    *    true - if underlying platform supports DFP hardwre operations
    *    false - if underying platform does not support DFP hardware operations
    */
   private final static boolean DFPHWAvailable(){
      return DFP_HW_AVAILABLE;
   }

   // DO NOT MODIFY THIS METHOD
   /* The method is only called once to setup the flag DFP_HW_AVAILABLE
    *  Return value
    *    true - when JIT compiled this method, return value reflects whether underlying hardware supports DFP
    *    false - if still interpreting this method or disabled by VM option
    */
   private final static boolean DFPGetHWAvailable() {
      return false;
   }

   /*
    *  Return value
    *    true - when JIT compiled this method, return vallue reflects hysteresis should be performed on current hardware
    *    false - if still interpreting this method or disabled by JIT option
    */
   private final static boolean DFPPerformHysteresis(){
      return false;
   }

   /*
    *  Return value
    *    true - when DFP representation should be used for appropriate values
    *    false - when DFP representation should not be used for any values
    */
   private final static boolean DFPUseDFP(){
      return hys_type;
   }

   // DO NOT MODIFY THIS METHOD

   /* NOTE:  The fact we return a boolean means we kill a register
    *        in the cases we don't want to perform rounding.
    *        i.e. rndFlag = 0 or 64 since we must load and
    *        return a 1 value in the generated JIT code.
    */
   private final boolean DFPIntConstructor(int val, int rndFlag, int prec, int rm){
      return false;
    }

   // DO NOT MODIFY THIS METHOD
   private final boolean DFPLongConstructor(long val, int rndFlag, int prec, int rm){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   /*
    *  Interpreted return value:
    *    true - never
    *    false - always
    *
    *  Jitted return value:
    *    true - if rounding succeeded
    *    false - if rounding failed
    *
    *  Parameters:
    *    val - long to be converted to DFP
    *    biasedExp - biased exponent to be inserted into DFP
    *    rndFlag
    *          =0 - no rounding
    *       =1 - rounding according to prec, rm
    *       =64 - rounding according to MathContext64
    *    prec - precision to round constructed DFP to
    *    rm - rm to use
    *    bcd - whether long is in bcd form
    *
    */
   private final boolean DFPLongExpConstructor(long val, int biasedExp,
         int rndFlag, int prec, int rm, boolean bcd){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   private final boolean DFPScaledAdd( long lhsDFP,long rhsDFP,int biasedExp){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   /*
    *  Parameters:
    *    rndFlag
    *          =0 - no rounding
    *       =1 - rounding according to prec, rm
    *       =64 - rounding according to MathContext64
    */
   private final boolean DFPAdd( long lhsDFP,long rhsDFP,
         int rndFlag, int precision, int rm){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   private final boolean DFPScaledSubtract(long lhsDFP,long rhsDFP,int biasedExp){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   /*
    *  Parameters:
    *    rndFlag
    *          =0 - no rounding
    *       =1 - rounding according to prec, rm
    *       =64 - rounding according to MathContext64
    */
   private final boolean DFPSubtract(long lhsDFP, long rhsDFP,
         int rndFlag, int precision, int rm){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   private final boolean DFPScaledMultiply(long lhsDFP,long rhsDFP,int biasedExp){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   /*
    *  Parameters:
    *    rndFlag
    *          =0 - no rounding
    *       =1 - rounding according to prec, rm
    *       =64 - rounding according to MathContext64
    */
   private final boolean DFPMultiply(long lhsDFP, long rhsDFP,
         int rndFlag, int precision, int rm){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   /*
    * Interpreted return value:
    * Returns -1 if not JIT compiled.
    *
    * JIT compiled return value:
    * 0 - ok, but inexact exception --> check UNNECESSARY
    * 1 - ok, no inexact
    * -1 - not ok --> try slow path
    *
    *  rndFlag
    *    =0 - rounding according to MathContext64
    *  =1 - rounding according to prec, rm
    */
   private final int DFPScaledDivide(long lhsDFP, long rhsDFP, int scale, int rndFlg, int rm){
      return -1;
   }

   // DO NOT MODIFY THIS METHOD
   /*
    * Interpreted return value:
    *       -1 = always
    *
    * JIT compiled return value:
    * Return 1 if JIT compiled and DFP divide is successful.
    * Return 0 if JIT compiled, but DFP divide was inexact
    * Return -1 if JIT compiled, but other exception (i.e. overflow)
    *
    * rndFlag
    *    =0 - no rounding
    *  =1 - rounding according to prec, rm
    *  =64 - rounding according to MathContext64
    */
   private final int DFPDivide(long lhsDFP, long rhsDFP,
         boolean checkForInexact, int rndFlag, int prec, int rm){
      return -1;  //return an invalid result
   }

   // DO NOT MODIFY THIS METHOD
   private final boolean DFPRound(long dfpSrc, int precision, int rm){
      return false;
   }

   // DO NOT MODIFY THIS METHOD
   private final static int DFPCompareTo(long lhsDFP, long rhsDFP){
      return -2; //return an invalid result
   }

   // DO NOT MODIFY THIS METHOD
   private final static long DFPBCDDigits(long dfp){
      return 10; //since each digit in a BCD is from 0 to 9
   }

   // DO NOT MODIFY THIS METHOD
   private final static int DFPSignificance(long dfp){
      return -1; //illegal significance
   }

   // DO NOT MODIFY THIS METHOD
   private final static int DFPExponent(long dfp){
      return 1000; //return a value out of the range of Double DFP
   }

   // DO NOT MODIFY THIS METHOD
   /*
    * Interpreted return value:
    *       -1 = always
    *
    * JIT compiled return value:
    * Return 1 if passed
    * Return 0 if JIT compiled, but inexact result
    * Return -1 if JIT compiled, but other failure
    *
    */
   private final int DFPSetScale(long srcDFP, int biasedExp,
         boolean round, int rm, boolean checkInexact){
      return -1;
   }

   // DO NOT MODIFY THIS METHOD
   private final static long DFPUnscaledValue(long srcDFP){
      return Long.MAX_VALUE;
   }


   // DFP Helper Methods 
   private final boolean DFPIntConstructorHelper(int num, MathContext set) {
      // quick path for 0e0
      if (num == 0){
         DFPConstructZero();
         return true;
      }

      // cache the exp for all DFP paths
      // exp remains 0
      this.flags|=0x8; //cache on

      // cache the sign for DFP, on all paths
      this.flags |= 0x4; //cache on

      if (num < 0)
         this.flags|=0x60; //isneg
      else if (num > 0)
         this.flags|=0x20; //ispos
      //iszero is already 00

      // we're going to take the full blown path to
      // constructing a DFP internal representation
      
      int prec = set.getPrecision();
      int rm = set.getRoundingMode().ordinal();

      // fast path for MathContext64
      if (prec == 16 && rm == BigDecimal.ROUND_HALF_EVEN){
         if (this.DFPIntConstructor(num, 64, 0, 0)){
         
            // We assume in the worst case that the precision and
            // exp remain the same since the number of digits
            // is at most 16.  If we passed in 999999999999999999be767
            // and rounded to +inf, we'd get overflow, fail and take
            // the slow path anyway.

            /* cache: SGP ESRR */

            // cache the precision
            this.flags |= 0x10;
            this.flags |= numDigits(num) << 7;

            // store DFP representation
            // (representation bits already 00)
            return true;
         }
      }

      // fast path for NO ROUNDING, as well as the ArithmeticException
      if ((prec == 0) || (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY) || (prec > 16)){
         /* This case catches:
          *    -when no rounding is required
          *  -if rounding is unecessary and precision !=0, check
          *  to see that result wasn't inexact (via call to Finish)
          *  (the latter satisfies the API description of :
          *  ArithmeticException - if the result is inexact but
          *  the rounding mode is UNNECESSARY.
          */

         if (this.DFPIntConstructor(num, 0, 0, 0)){

            /* cache: SGP ESRR */

            // store DFP representation
            // (representation bits already 00)

            // See if we need to throw an arithmetic exception

            if (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY) {
               this.finish(prec, rm);
            } else {
               // we can cache the precision and exp
               // since no rounding meant that (no rounding)
               // cache the precision
               this.flags |= 0x10; //cache on
               this.flags |= numDigits(num) << 7;
            }
            return true;
         }
      }

      // Otherwise, if a precision to round to is specified
      else if (prec <= 16){

         /* NOTE:  We do the following two if statements
          * since the constants used for HALF_EVEN and ROUND_UP in
          * the classlib do not map correctly to the DFP hardware
          * rounding mode bits.  All other classlib RoundingModes, however, do.
          */

         //the default DFP rounding mode
         if (rm == BigDecimal.ROUND_HALF_EVEN)
            rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
         else if (rm == BigDecimal.ROUND_UP)
            rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

         // now construct in hardware if possible
         if(this.DFPIntConstructor(num, 1, prec, rm)){

            /* cache: SGP ESRR */

            // store DFP representation
            // (representation bits already 00)

            //don't try to cache precision/exp since
            //prec might be different precision(val)

            //so turn cache of exp off...
            this.flags&=0xFFFFFFF7;

            return true;
         }
      }
      return false;
   }
   
   private final boolean DFPLongConstructorHelper(long num, int scale, MathContext set){
      // don't want to send in 0 and then round with hardware
      // cause it might place a crummy exponent value...
      if (num == 0){
         DFPConstructZero();
         return true;
      }

      // otherwise, make sure the long is within 64-bit DFP range
      else if (num <=9999999999999999L && num >= -9999999999999999L &&
            -scale>=-398 && -scale<369){

         /* cache: SGP ESRR */

         // cache the sign for DFP, on all paths
         this.flags|=0x4; //cache on
         if (num < 0)
            this.flags|=0x60; //isneg
         else if (num > 0)
            this.flags|=0x20; //ispos
         //iszero is already 00

         int prec = set.getPrecision();
         int rm = set.getRoundingMode().ordinal();

         // fast path for MathContext64
         if (prec == 16 && rm == BigDecimal.ROUND_HALF_EVEN){
            if(this.DFPLongExpConstructor(num, -scale+398, 64, 0, 0, false)){

               // We assume in the worst case that the precision and
               // exponent remain the same since the number of digits
               // is at most 16.  If we passed in 999999999999999999be767
               // and rounded to +inf, we'd get overflow, fail and take
               // the slow path anyway.

               /* cache: SGP ESRR */

               // cache the precision
               this.flags|=0x10; //cache on
               this.flags |= numDigits(num) << 7;

               // cache the exponent
               this.flags|=0x8; //cache on
               this.cachedScale=scale;

               //store DFP representation
               // (already set to 00)

               return true;
            }
         }

         // fast path for NO ROUNDING, as well as the ArithmeticException
         if ((prec == 0) || (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY)
               || (prec > 16)){

            /* This case catches:
             *    -when no rounding is required
             *  -if rounding is unecessary and precision !=0, check
             *  to see that result wasn't inexact (via call to Finish)
             *  (the latter satisfies the API description of :
             *  ArithmeticException - if the result is inexact but
             *  the rounding mode is UNNECESSARY.
             */

            // NOTE: When we don't specify rounding in DFP, we'll never
            // fail, so don't bother checking the return value
            if (this.DFPLongExpConstructor(num, -scale+398, 0, 0, 0, false)){

               // store DFP representation
               // (already set to 00)

               // See if we need to throw an arithmetic exception
               if (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY)
                  this.finish(prec, rm);
               else{

                  // cache the exponent
                  this.flags|=0x8; //cache on
                  this.cachedScale=scale;

                  // cache the precision
                  this.flags|=0x10; //cache on
                  this.flags |= numDigits(num) << 7;
               }
               return true;
            }
         }

         // Otherwise, if a precision to round to is specified
         else if (prec <=16){

            /* NOTE:  We do the following two if statements
             * since the constants used for HALF_EVEN and ROUND_UP in
             * the classlib do not map correctly to the DFP hardware
             * rounding mode bits.  All other classlib RoundingModes, however, do.
             */

            //the default DFP rounding mode
            if (rm == BigDecimal.ROUND_HALF_EVEN)
               rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
            else if (rm == BigDecimal.ROUND_UP)
               rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

            // now construct in hardware if possible
            if(this.DFPLongExpConstructor(num, -scale+398, 1, prec, rm, false)){

               //store DFP representation
               // (already 00)

               //don't try to cache precision/exponent since
               //prec might be different precision(val)

               // so turn caching of exponent off
               this.flags&=0xFFFFFFF7;

               return true;
            }
         }
      }

      // DFP didn't work, place into BI form
      this.flags |= 0x00000002 /* isBigInteger */;
      this.bi = BigInteger.valueOf(num);

      this.cachedScale= scale;
      return false;
   }

   private final boolean DFPCharConstructorHelper(long bcdVal, int newExp, int newInd, int numDigits, MathContext set) {
      int prec = set.getPrecision();
      int rm = set.getRoundingMode().ordinal();

      // fast path for 0e0
      if (bcdVal == 0 && newExp == 0){
         DFPConstructZero();
         return true;
      }

      /* cache - SGP ESRR */

      // cache the sign for DFP on all paths..
      this.flags|=0x4; //cache on
      if (bcdVal != 0) //if not-zero, then we set the val
         this.flags|=newInd;

      // cache the exponent for DFP on all paths..
      this.flags|=0x8; //cache on
      this.cachedScale= -newExp;

      // fast path for MathContext64
      if (prec == 16 && rm == BigDecimal.ROUND_HALF_EVEN){
         if (newInd == 0x60){ //isneg
            if (this.DFPLongExpConstructor(bcdVal, newExp+398, 64, 0, 0, true)){

               // store DFP representation
               // (already 00)

               // since we use unsigned BCDs to get full 16 digits worth
               // lets flip the DFP's sign bit to indicate the fact...
               if (newInd == 0x60) //inseg
                  this.laside |= 0x8000000000000000l;

               /* cache - SGP ESRR */

               // cache the precision
               this.flags|=0x10;
               this.flags |= numDigits << 7;

               this.finish(prec, rm);
               return true;
            }
         }
         else{
            if(this.DFPLongExpConstructor(bcdVal, newExp+398, 64, 0, 0, true)){

               // We assume in the worst case that the precision and
               // exponent remain the same since the number of digits
               // is at most 16.  If we passed in 999999999999999999be767
               // and rounded to +inf, we'd get overflow, fail and take
               // the slow path anyway.

               // cache the precision
               this.flags|=0x10;
               this.flags |= numDigits << 7;

               //store DFP representation
               //(already 00)
               return true;
            }
         }
      }

      // fast path for NO ROUNDING, as well as the ArithmeticException
      if ((prec == 0) || (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY) ||
            (prec > 16)){

         /* This case catches:
          *    -when no rounding is required
          *  -if rounding is unecessary and precision !=0, check
          *  to see that result wasn't inexact (via call to Finish)
          *  (the latter satisfies the API description of :
          *  ArithmeticException - if the result is inexact but
          *  the rounding mode is UNNECESSARY.
          */
         if (this.DFPLongExpConstructor(bcdVal, newExp+398, 0, 0, 0, true)){

            // store DFP representation
            // (already 00)

            // since we use unsigned BCDs to get full 16 digits worth
            // lets flip the DFP's sign bit to indicate the fact...
            if (newInd == 0x60) //isneg
               this.laside |= 0x8000000000000000l;

            // See if we need to throw an arithmetic exception
            if (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY){

               // since we use unsigned BCDs to get full 16 digits worth
               // lets flip the DFP's sign bit to indicate the fact...
               if (newInd == 0x60) //inseg
                  this.laside |= 0x8000000000000000l;

               this.finish(prec, rm);
            }
            else{
               // we can cache the precision and exponent
               // since no rounding meant that (no rounding)
               // cache the precision
               this.flags|=0x10;
               this.flags |= numDigits << 7;
            }
            return true;
         }
      }

      // Otherwise, if a precision to round to is specified
      else if (prec <=16){

         /* NOTE:  We do the following two if statements
          * since the constants used for HALF_EVEN and ROUND_UP in
          * the classlib do not map correctly to the DFP hardware
          * rounding mode bits.  All other classlib RoundingModes, however, do.
          */

         // for negative BCDs
         if (newInd == 0x60){
            if (this.DFPLongExpConstructor(bcdVal, newExp+398, 0, 0, 0, true)){

               // store DFP representation
               // (already 00)

               // since we use unsigned BCDs to get full 16 digits worth
               // lets flip the DFP's sign bit to indicate the fact...
               this.laside |= 0x8000000000000000l;

               /* cache - SGP ESRR */

               // can't store precision since it's going to be prec,
               // but isn't just yet...

               this.finish(prec, rm);
               return true;
            }
         }
         else{

            // NOTE:  We do the following rm reversal here because
            // doing so in common code above would cause the eventual
            // call to DFPRoundHelper to switch them back.

            // the default DFP rounding mode
            if (rm == BigDecimal.ROUND_HALF_EVEN)
               rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
            else if (rm == BigDecimal.ROUND_UP)
               rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

            // for positive BCDs
            if(this.DFPLongExpConstructor(bcdVal, newExp+398, 1, prec, rm, true)){

               //store DFP representation
               //(already 00)

               //don't try to cache precision/exponent since
               //exp might be diff due to rounding, and precision
               //may be larger than actual

               /* cache - SGP ESRR */

               //reset caching of exponent
               this.flags&=0xFFFFFFF7;

               return true;
            }
         }
      }
      return false;
   }
   

   private final boolean DFPBigIntegerConstructorHelper(BigInteger bi,int scale, MathContext set){
      if (precisionBI(bi)<17 && -scale<=369 && -scale>=-398){

         // get the long value with the appropriate sign
         long val = bi.longValue();

         // quick path for 0e0
         if (val == 0 && scale == 0){
            DFPConstructZero();
            return true;
         }

         // otherwise, we'll need to perform DFP construction
         // using the correct precision/roundingmodes

         int prec = set.getPrecision();
         int rm = set.getRoundingMode().ordinal();

         // fast path for MathContext64
         if (prec == 16 && rm == BigDecimal.ROUND_HALF_EVEN){
            if(this.DFPLongExpConstructor(val, -scale+398, 64, 0, 0, false)){

               // We assume in the worst case that the precision and
               // exponent remain the same since the number of digits
               // is at most 16.  If we passed in 999999999999999999be767
               // and rounded to +inf, we'd get overflow, fail and take
               // the slow path anyway.

               /* cache - SGP ESRR */

               // cache the precision
               this.flags|=0x10; //cache on
               this.flags |= numDigits(val) << 7;

               // cache the sign
               this.flags|=0x4; //cache on
               this.flags|= ((bi.signum() <<5) & 0x60);

               //cache the exponent
               this.flags|=0x8; //cache on
               this.cachedScale=scale;

               // store DFP representation
               // (representation bits already 00)

               return true;
            }
         }

         // fast path for NO ROUNDING, as well as the ArithmeticException
         if ((prec == 0) || (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY)
               || (prec > 16)){

            /* This case catches:
             *    -when no rounding is required
             *  -if rounding is unecessary and precision !=0, check
             *  to see that result wasn't inexact (via call to Finish)
             *  (the latter satisfies the API description of :
             *  ArithmeticException - if the result is inexact but
             *  the rounding mode is UNNECESSARY.
             */

            // NOTE: When we don't specify rounding in DFP, we'll never
            // fail, so don't bother checking the return value
            if (this.DFPLongExpConstructor(val, -scale+398, 0, 0, 0, false)){
               /* cache - SGP ESRR */

               // cache the sign
               this.flags|=0x4; // cache on
               this.flags|=((bi.signum() <<5) & 0x60);

               // store DFP representation
               // (representation bits already 00)

               // See if we need to throw an arithmetic exception

               if (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY)
                  this.finish(prec, rm);
               else{
                  // we can cache the precision and exponent
                  // since no rounding left things as is

                  // cache the precision
                  this.flags|=0x10; // cache on
                  this.flags |= numDigits(val) << 7;

                  //cache the exponent
                  this.flags|=0x8; // cache on
                  this.cachedScale=scale;
               }
               return true;
            }
         }

         // Otherwise, if a precision to round to is specified
         else if (prec <=16){

            /* NOTE:  We do the following two if statements
             * since the constants used for HALF_EVEN and ROUND_UP in
             * the classlib do not map correctly to the DFP hardware
             * rounding mode bits.  All other classlib RoundingModes, however, do.
             */

            //the default DFP rounding mode
            if (rm == BigDecimal.ROUND_HALF_EVEN)
               rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
            else if (rm == BigDecimal.ROUND_UP)
               rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

            // now construct in hardware if possible
            if(this.DFPLongExpConstructor(val,-scale+398, 1, prec, rm, false)){

               /* cache - SGP ESRR */

               // cache the sign
               this.flags|=0x4; //cache on
               this.flags|=((bi.signum() <<5) & 0x60);

               //store DFP representation
               // (representation bits already 00)

               //don't try to cache precision/exponent since
               //prec might be different after rounding
               return true;
            }
         }
      }
      // if we got here, DFP paths were unsuccessful

      // place into BigInteger representation
      this.flags |= 0x00000002; //the representation
      this.bi = bi; //the BigInteger
      this.cachedScale = scale; //the exponent

      // round
      if (set != MathContext.UNLIMITED)
         this.finish(set.getPrecision(), set.getRoundingMode().ordinal());
      return false;

   }

   private final boolean DFPAddHelper(BigDecimal res, BigDecimal rhs){
      if (((this.flags | rhs.flags) & ~0xFFFFFFFC) == 0x00000000){
         int resExp =-(Math.max(this.scale(), rhs.scale()));

         /* Precision = 0, rounding = UNNECESSARY */
         if (resExp >=-398 && resExp<=369){
            if(res.DFPScaledAdd(rhs.laside, this.laside, resExp+398)){
               // we can and out the flags because we don't have
               // a sense of the exponent/precision/sign
               // of this result, nor do we want to use DFP hw
               // to figure it out, so we do not cache anything

               //set res as DFP - (already set to 00)

               // because DFP add takes the exclusive or of the sign bits
               //for the result, need to make sure result of add 0 by -0
               //doesn't store a negative sign...
               if(res.isDFPZero())
                  res.laside &= 0x7FFFFFFFFFFFFFFFL;
               return true;
            }
         }
      }
      return false;
   }
   
   private final boolean DFPAddHelper(BigDecimal res, BigDecimal rhs, MathContext set) {
      boolean passed = false;
      int prec = set.getPrecision();
      int rm = set.getRoundingMode().ordinal();
      // need to special case this since DFP hardware
      // doesn't conform to BigDecimal API when adding
      // to a 0..
      if (prec != 0 &&
            set.getRoundingMode().ordinal() != BigDecimal.ROUND_UNNECESSARY){

         boolean lZero = this.isDFPZero();
         boolean rZero = rhs.isDFPZero();

         // if both zero, return the larger scaled one
         if (lZero && rZero){
            clone(res, BigDecimal.valueOf(0, Math.max(this.scale(),rhs.scale())));

            if (set != MathContext.UNLIMITED)
               res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            return true;
         }

         // if left is zero
         else if (lZero){
            clone(res, rhs);

            // do we need to increase the scale of the rhs?
            // hence pad it with 0s?
            if (this.scale() > rhs.scale()){
               if (set.getPrecision()-rhs.precision() >0)
                  clone(res,res.setScale(Math.abs(-this.scale()),true));  //might return BI
            }

            if (set != MathContext.UNLIMITED)
               res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            return true;
         }
         else if (rZero){
            clone(res, this);

            if (rhs.scale() > this.scale()){
               if (set.getPrecision()-rhs.precision() >0)
                  clone(res, res.setScale(Math.abs(-rhs.scale()), true)); //might return BI
            }
            if (set != MathContext.UNLIMITED)
               res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            return true;
         }
      }

      // at this point, not dealing with 0s

      // we can and out the flags because we don't have
      // a sense of the exponent/precision/sign
      // of this result, nor do we want to use DFP hw
      // to figure it out, so we do not cache anything

      // fast path for MathContext64
      if (prec == 16 && rm == BigDecimal.ROUND_HALF_EVEN){
         if(res.DFPAdd(rhs.laside, this.laside, 64, 0, 0)){
            // set res as DFP - (already set to 00)
            passed =true;
         }
      }

      // same as scaled DFPAddition
      else if (prec == 0){
         int resExp =-(Math.max(this.scale(), rhs.scale()));
         if (resExp >=-398 && resExp<=369){
            if(res.DFPScaledAdd(rhs.laside, this.laside, resExp+398)){

               // we can and out the flags because we don't have
               // a sense of the exponent/precision/sign
               // of this result, nor do we want to use DFP hw
               // to figure it out, so we do not cache anything

               //set res as DFP - (already set to 00)
               passed = true;
            }
         }
      }

      // fast path for NO ROUNDING, as well as the ArithmeticException
      else if (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY){
         if(res.DFPAdd(rhs.laside, this.laside, 0, 0, 0)){
            if (DFPPerformHysteresis()){
               DFPPerformHysteresis(-3);
            }

            // set res as DFP - (already set to 00)

            // See if we need to throw an arithmetic exception
            res.finish(prec, rm);
            passed = true;
         }
      }

      // Otherwise, if a precision to round to is specified
      else if (prec <=16){

         /* NOTE:  We do the following two if statements
          * since the constants used for HALF_EVEN and ROUND_UP in
          * the classlib do not map correctly to the DFP hardware
          * rounding mode bits.  All other classlib RoundingModes, however, do.
          */

         //the default DFP rounding mode
         if (rm == BigDecimal.ROUND_HALF_EVEN)
            rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
         else if (rm == BigDecimal.ROUND_UP)
            rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

         boolean dfpPassed = prec == 16 ?
               res.DFPAdd(rhs.laside, this.laside, 1, 16, rm) :
               res.DFPAdd(rhs.laside, this.laside, 1, prec, rm);
         if(dfpPassed){
            if (DFPPerformHysteresis()){
               DFPPerformHysteresis(-3);
            }

            // set res as DFP - (already set to 00)
            passed=true;
         }
      }

      // because DFP add takes the exclusive or of the sign bits
      //for the result, need to make sure result of add 0 by -0
      //doesn't store a negative sign...
      if (passed){
         if(res.isDFPZero())
            res.laside &= 0x7FFFFFFFFFFFFFFFL;
         return true;
      }
      return false;
   }
 
   private boolean DFPSubtractHelper(BigDecimal res, BigDecimal rhs){

      if (((this.flags | rhs.flags) & ~0xFFFFFFFC) == 0x00000000){

         int resExp =-(Math.max(this.scale(), rhs.scale()));
         // Precision = 0, rounding = UNNECESSARY
         if (resExp >=-398 && resExp<=369){
            if(res.DFPScaledSubtract(rhs.laside, this.laside, resExp+398)){

               // we can and out the flags because we don't have
               // a sense of the exponent/precision/sign
               // of this result, nor do we want to use DFP hw
               // to figure it out, so we do not cache anything

               // set res as DFP - (already set to 00)

               // because DFP subtract takes the exclusive or of the sign bits
               //for the result, need to make sure result of subtract 0 by -0
               //doesn't store a negative sign...
               if(res.isDFPZero())
                  res.laside &= 0x7FFFFFFFFFFFFFFFL;
               return true;
            }
         }
      }
      return false;
   }
   
   private final boolean DFPSubtractHelper(BigDecimal res, BigDecimal rhs, MathContext set) {
      boolean passed = false;
      int prec = set.getPrecision();
      int rm = set.getRoundingMode().ordinal();
      
      // need to special case this since DFP hardware
      // doesn't conform to BigDecimal API when adding
      // to a 0..
      if (prec != 0 &&
            set.getRoundingMode().ordinal() != BigDecimal.ROUND_UNNECESSARY){

         boolean lZero = this.isDFPZero();
         boolean rZero = rhs.isDFPZero();

         // if both zero, return the larger scaled one
         if (lZero && rZero){
            clone(res, BigDecimal.valueOf(0, Math.max(this.scale(),rhs.scale())));
            if (set != MathContext.UNLIMITED)
               res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            return true;
         }

         // if left is zero
         else if (lZero){
            clone(res, rhs.negate());

            // do we need to increase the scale of the rhs?
            // hence pad it with 0s?
            if (this.scale() > rhs.scale()){
               //int pad = -rhs.scale()+this.scale();
               if (set.getPrecision()-rhs.precision() >0)
                  clone(res, res.setScale(Math.abs(-this.scale()),true));  //might return BI
            }
            if (set != MathContext.UNLIMITED)
               res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            return true;
         }
         else if (rZero){
            clone(res, this);

            if (rhs.scale() > this.scale()){
               //int pad = -this.scale()+rhs.scale();
               if (set.getPrecision()-rhs.precision() >0)
                  clone(res, res.setScale(Math.abs(-rhs.scale()), true)); //might return BI
            }
            if (set != MathContext.UNLIMITED)
               res.finish(set.getPrecision(), set.getRoundingMode().ordinal());
            return true;
         }
      }

      // fast path for MathContext64
      if (prec == 16 && rm == BigDecimal.ROUND_HALF_EVEN){
         if(res.DFPSubtract(rhs.laside, this.laside, 64, 0, 0)){
            // set res as DFP - (already set to 00)
            passed=true;
         }
      }

      // same as DFPScaledSubtract
      else if (prec == 0){
         int resExp =-(Math.max(this.scale(), rhs.scale()));
         if (resExp >=-398 && resExp<=369){
            if(res.DFPScaledSubtract(rhs.laside, this.laside, resExp+398)){
               // we can and out the flags because we don't have
               // a sense of the exponent/precision/sign
               // of this result, nor do we want to use DFP hw
               // to figure it out, so we do not cache anything

               // set res as DFP - (already set to 00)
               passed = true;
            }
         }
      }

      // fast path for NO ROUNDING, as well as the ArithmeticException
      else if (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY){
         if(res.DFPSubtract(rhs.laside, this.laside, 0, 0, 0)){
            if (DFPPerformHysteresis()){
               int sum = hys_counter -3;
               hys_counter += -3 & ~(((sum^(-3)) & (sum^hys_counter)) >>> 31);
               if (hys_counter<-hys_threshold){
                  hys_type = false; //nonDFP
                  hys_counter=0;
               }
               else if (hys_counter>hys_threshold){
                  hys_type = true; // DFP
                  hys_counter=0;
               }
            }

            // set res as DFP - (already set to 00)

            // See if we need to throw an arithmetic exception
            res.finish(prec, rm);
            passed=true;
         }
      }

      // Otherwise, if a precision to round to is specified
      else if(prec <=16){
         /* NOTE:  We do the following two if statements
          * since the constants used for HALF_EVEN and ROUND_UP in
          * the classlib do not map correctly to the DFP hardware
          * rounding mode bits.  All other classlib RoundingModes, however, do.
          */

         //the default DFP rounding mode
         if (rm == BigDecimal.ROUND_HALF_EVEN)
            rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
         else if (rm == BigDecimal.ROUND_UP)
            rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

         boolean dfpPassed = prec == 16 ?
               res.DFPSubtract(rhs.laside, this.laside, 1, 16, rm) :
               res.DFPSubtract(rhs.laside, this.laside, 1, prec, rm);
         if(dfpPassed){
            if (DFPPerformHysteresis()){
               int sum = hys_counter -3;
               hys_counter += -3 & ~(((sum^(-3)) & (sum^hys_counter)) >>> 31);
               if (hys_counter<-hys_threshold){
                  hys_type = false; //nonDFP
                  hys_counter=0;
               }
               else if (hys_counter>hys_threshold){
                  hys_type = true; // DFP
                  hys_counter=0;
               }
            }

            // set res as DFP - (already set to 00)
            passed=true;
         }
      }

      // because DFP subtracts takes the exclusive or of the sign bits
      //for the result, need to make sure result of subtract 0 by -0
      //doesn't store a negative sign...
      if (passed){
         if(res.isDFPZero())
            res.laside &= 0x7FFFFFFFFFFFFFFFL;
         return true;
      }
      return false;
   }


   private final boolean DFPMultiplyHelper(BigDecimal res, BigDecimal rhs) {
      int resExp =-(this.scale()+rhs.scale());

      /* Precision = 0, rounding = UNNECESSARY */
      if (resExp >=-398 && resExp<=369){
         if(res.DFPScaledMultiply(rhs.laside, this.laside, resExp+398)){

            // we can and out the flags because we don't have
            // a sense of the exponent/precision/sign
            // of this result, nor do we want to use DFP hw
            // to figure it out, so we do not cache anything

            // set res as DFP - (already set to 00)

            // because DFP multiply takes the exclusive or of the sign bits
            //for the result, need to make sure result of multiply by 0
            //doesn't store a negative sign...
            if(res.isDFPZero())
               res.laside &= 0x7FFFFFFFFFFFFFFFL;
            return true;
         }
      }
      return false;
   }


   private final boolean DFPMultiplyHelper(BigDecimal res, BigDecimal rhs, MathContext set) {
      boolean passed = false;
      int prec = set.getPrecision();
      int rm = set.getRoundingMode().ordinal();

      //  fast path for MathContext64
      if (prec == 16 && rm == BigDecimal.ROUND_HALF_EVEN){
         if(res.DFPMultiply(rhs.laside, this.laside, 64, 0, 0)){
            // set res as DFP - (already set to 00)
            passed = true;
         }
      }

      // same as DFPScaledMultiply
      else if (prec == 0){
         int resExp =-(this.scale()+rhs.scale());
         if (resExp >=-398 && resExp<=369){
            if(res.DFPScaledMultiply(rhs.laside, this.laside, resExp+398)){

               // we can and out the flags because we don't have
               // a sense of the exponent/precision/sign
               // of this result, nor do we want to use DFP hw
               // to figure it out, so we do not cache anything

               // set res as DFP - (already set to 00)

               // because DFP multiply takes the exclusive or of the sign bits
               //for the result, need to make sure result of multiply by 0
               //doesn't store a negative sign...
               passed = true;
            }
         }
      }

      // fast path for NO ROUNDING, as well as the ArithmeticException
      else if (prec > 0 && rm == BigDecimal.ROUND_UNNECESSARY){
         if(res.DFPMultiply(rhs.laside, this.laside, 0, 0, 0)){
            // set res as DFP - (already set to 00)

            // See if we need to throw an arithmetic exception
            res.finish(prec, rm);
            passed=true;
         }
      }

      // Otherwise, if a precision to round to is specified
      else if (prec <= 16){
         /* NOTE:  We do the following two if statements
          * since the constants used for HALF_EVEN and ROUND_UP in
          * the classlib do not map correctly to the DFP hardware
          * rounding mode bits.  All other classlib RoundingModes, however, do.
          */

         //the default DFP rounding mode
         if (rm == BigDecimal.ROUND_HALF_EVEN)
            rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
         else if (rm == BigDecimal.ROUND_UP)
            rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

         boolean dfpPassed = prec == 16 ?
               res.DFPMultiply(rhs.laside, this.laside, 1, 16, rm) :
               res.DFPMultiply(rhs.laside, this.laside, 1, prec, rm);
         if(dfpPassed){
            // set res as DFP - (already set to 00)
            passed =true;
         }
      }

      // because DFP multiply takes the exclusive or of the sign bits
      // for the result, need to make sure result of multiply by 0
      // doesn't store a negative sign...
      if (passed){
         if(res.isDFPZero())
            res.laside &= 0x7FFFFFFFFFFFFFFFL;
      }
      return passed;
   }

 
   private final boolean DFPDivideHelper(BigDecimal res, BigDecimal lhs, BigDecimal rhs) {
      if (rhs.isDFPZero()) {
         badDivideByZero();
      }

      // we can and out the flags because we don't have
      // a sense of the exponent/precision/sign
      // of this result, nor do we want to use DFP hw
      // to figure it out, so we do not cache anything

      /*
       * Interpreted return value:
       * Returns -1 if not JIT compiled.
       *
       * JIT compiled return value:
       * Return 1 if JIT compiled and DFP divide is successful.
       * Return 0 if JIT compiled, but DFP divide was inexact
       * Return -2 if JIT compiled, but other exception (i.e. overflow)
       */

      // we need this in order to throw a "non-terminating decimal expansion" error
      int desiredPrecision=(int)
         Math.min(lhs.precision() + Math.ceil(10*rhs.precision()/3),Integer.MAX_VALUE);
      boolean passed=false;

      int ret = res.DFPDivide(rhs.laside, this.laside, true, 0, 0, 0);

      // we passed, just check for non-terminating decimal expansion
      if (ret == 1){
         if (res.precision() > desiredPrecision) {
            // math.28 = Non-terminating decimal expansion
            throw new ArithmeticException(Messages.getString("math.28")); //$NON-NLS-1$
         }
         // set res as DFP - (already set to 00)
         passed=true;
      }

      //otherwise, we had an inexact, or failure... so we'll continue on slow path

      // because DFP divide takes the exclusive or of the sign bits
      //for the result, need to make sure result of multiply by 0
      //doesn't store a negative sign...
      if (passed){
         if (res.isDFPZero())
            res.laside &= 0x7FFFFFFFFFFFFFFFL;
      }
      return passed;
   }

 
   private final boolean DFPDivideHelper(BigDecimal res, BigDecimal thisClone, BigDecimal rhsClone, MathContext set) {
      if (rhsClone.isDFPZero()) {
         badDivideByZero();
      }

      int rm = set.getRoundingMode().ordinal();
      int prec = set.getPrecision();
      boolean passed=false;

      // fast path for MathContext64
      if (prec == 16 && rm == BigDecimal.ROUND_HALF_EVEN){
         int ret = res.DFPDivide(rhsClone.laside, thisClone.laside, false, 64, 0, 0);
         if (ret == 1){

            // set res as DFP - (already set to 00)
            passed = true;
         }
      }
      else if (prec <= 16){

         //the default DFP rounding mode
         if (rm == BigDecimal.ROUND_HALF_EVEN)
            rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
         else if (rm == BigDecimal.ROUND_UP)
            rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

         int ret = prec == 16 ?
               res.DFPDivide(rhsClone.laside, thisClone.laside, true, 1, 16, rm) :
               res.DFPDivide(rhsClone.laside, thisClone.laside, true, 1, prec, rm);
         if (ret == 0 && rm == BigDecimal.ROUND_UNNECESSARY) {
            // math.29 = Inexact result requires rounding
            throw new ArithmeticException(Messages.getString("math.29")); //$NON-NLS-1$
         }
         //otherwise, we divide perfectly and returned 1, or divided
         //and got inexact (in the absense of checking for ROUND_UNNECESSARY
         if (ret == 1){
            if (DFPPerformHysteresis()){
               DFPPerformHysteresis(10);
            }

            // set res as DFP - (already set to 00)
            passed = true;
         }
      }
      // because DFP divide takes the exclusive or of the sign bits
      //for the result, need to make sure result of multiply by 0
      //doesn't store a negative sign...
      if (passed){
         if (res.isDFPZero())
            res.laside &= 0x7FFFFFFFFFFFFFFFL;
      }
      return passed;
   }

   
   private boolean DFPSetScaleHelper(BigDecimal res, int scale) {
      if (-scale >= -398 && -scale <= 369){
         int ret = res.DFPSetScale(this.laside, -scale+398, false, 0, true);
         if (ret == 1){
            if (DFPPerformHysteresis()){
               DFPPerformHysteresis(5);
            }

            /* cache - SGP ESRR */

            // set representation to DFP
            // (already 00)

            // because DFPSetScale maintains the sign of the DFP
            // -23 might get scaled down to -0
            if(res.isDFPZero()){
               res.laside &= 0x7FFFFFFFFFFFFFFFL;
               res.flags|=0x4;
               res.flags&=0xFFFFFF9F; //clear signum bits for 0
            }
            else{
               // cache the sign of the src
               res.flags|=0x4;
               res.flags |=(this.signum()<<5)&0x60;
            }

            // cache the exponent
            res.flags|=0x8;
            res.cachedScale = scale;

            //NOTE:  We do not cache precision!
            res.flags&=0xFFFFFFEF; //clear prec cache bit
            return true;
         }
         else if (ret == 0) {
            // math.23 = Requires rounding: {0}
            throw new ArithmeticException(Messages.getString("math.23", Long.toString(scale))); //$NON-NLS-1$
         }
         return false;
      }
      return false;
   }

   
   private final boolean DFPSetScaleHelper(BigDecimal res, long ourscale, int scale, int rm) {
      boolean passed = false;
      if (-scale >= -398 && -scale <= 369){

         // If the roundind mode is UNNECESSARY, then we can set
         // the scale as if we were setting in the previous API
         // i.e. with no concern to rounding (the 3rd parameter)
         if (rm == BigDecimal.ROUND_UNNECESSARY){
            if (res.DFPSetScale(this.laside, -scale+398,false, 0, true) == 1){
               if (DFPPerformHysteresis()){
                  DFPPerformHysteresis(5);
               }

               /* cache - SGP ESRR */

               // set representation to DFP
               // (already 00)

               // because DFPSetScale maintains the sign of the DFP
               // -23 might get scaled down to -0
               if(res.isDFPZero()){
                  res.laside &= 0x7FFFFFFFFFFFFFFFL;
                  res.flags|=0x4;
                  res.flags&=0xFFFFFF9F; //clear signum bits for 0
               }
               else{
                  // cache the sign of the src
                  res.flags|=0x4;
                  res.flags |=(this.signum()<<5)&0x60;
               }

               // cache the exponent
               res.flags|=0x8;
               res.cachedScale = scale;

               passed = true;
            }
         }
         else{

            //the default DFP rounding mode
            if (rm == BigDecimal.ROUND_HALF_EVEN)
               rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
            else if (rm == BigDecimal.ROUND_UP)
               rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

            if (res.DFPSetScale(this.laside, -scale+398,true, rm, false) == 1){
               if (DFPPerformHysteresis()){
                  DFPPerformHysteresis(5);
               }

               /* cache - SGP ESRR */

               // set representation to DFP
               // (already 00)

               // because DFPSetScale maintains the sign of the DFP
               // -23 might get scaled down to -0
               if(res.isDFPZero()){
                  res.laside &= 0x7FFFFFFFFFFFFFFFL;
                  res.flags|=0x4;
                  res.flags&=0xFFFFFF9F; //clear signum bits for 0
               }
               else{
                  // cache the sign of the src
                  res.flags|=0x4;
                  res.flags |=(this.signum()<<5)&0x60;
               }

               // cache the exponent
               res.flags|=0x8;
               res.cachedScale =scale;
               passed = true;
            }
         }
      }
      return passed;
   }
   
   private final void DFPNegateHelper(BigDecimal res) {
      int signum = this.signum();

      // we're going to cache a new sign bit...
      res.flags|=0x4;

      // need to flip DFP sign bit and cached sign
      if (signum == -1){
         res.laside&=0x7FFFFFFFFFFFFFFFL; //flip DFP sign bit
         res.flags&=0xFFFFFF9F; //clear the cached sign bits
         res.flags|=( 1 << 5) & 0x60;// ispos
      }
      else if (signum == 1){
         res.laside|=res.laside|=0x8000000000000000L; //flip DFP sign bit
         res.flags&=0xFFFFFF9F; //clear the cached sign bits
         res.flags|=(3 << 5) & 0x60; // isneg
      }
      if (res.bi != null) //118570
         res.bi = res.bi.negate();
   }
   
   
   private final void DFPRoundHelper(int prec, int rm){

      // Only enter here iff prec > 0
      if (this.precision() > prec){ // and only perform if request is shorter then us
         long bcd = DFPBCDDigits(this.laside);
         if (bcd == 10)
            bcd = extractDFPDigitsBCD(this.laside);

         if (/*prec > 0 &&*/ rm==BigDecimal.ROUND_UNNECESSARY){
            if (!allzeroBCD(bcd,this.precision()-prec)) {
               // math.36 = Rounding mode unnecessary, but rounding changes value
               throw new ArithmeticException(Messages.getString("math.36")); //$NON-NLS-1$
            }
         }

         //the default DFP rounding mode
         if (rm == BigDecimal.ROUND_HALF_EVEN)
            rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
         else if (rm == BigDecimal.ROUND_UP)
            rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

         if(!this.DFPRound(this.laside, prec, rm)){
            this.DFPToLL();

            // switch these around if we failed...

            //the default DFP rounding mode
            if (rm == BigDecimal.ROUND_HALF_EVEN)
               rm = BigDecimal.ROUND_UP;  //correct DFP HW rounding mode for HALF_EVEN
            else if (rm == BigDecimal.ROUND_UP)
               rm = BigDecimal.ROUND_HALF_EVEN; //correct DFP HW rounding mode for HALF_UP

            this.roundLL(prec,rm,false);
         }
         else{

            /* cache - SGP ESRR */

            // if successfull, update the precision cache...
            this.flags|=0x10; //cache on
            this.flags&=0x7F;
            this.flags |= prec << 7;

            //exponent may have changed, but we don't know
            //to what...
            this.flags&=0xFFFFFFF7; //clear exponent cache bit
         }

         // if the result is still in DFP and is 0, make
         // sure the sign bit is off...
         if ((this.flags & ~0xFFFFFFFC) == 0x00000000 && this.isDFPZero())
            this.laside &= 0x7FFFFFFFFFFFFFFFL;
      }
   }

   
   private final int DFPPrecisionHelper() {
      int tempFlags = this.flags;

      // we're caching it
      tempFlags|=0x10; // cache on
      tempFlags&=0x7F; //clear pre-existing bits

      int sig = DFPSignificance(this.laside);
      if (sig < 0){
         long digits = DFPBCDDigits(this.laside);
         if (digits == 10){
            digits = extractDFPDigitsBCD(this.laside);
         }
         int nlz = Long.numberOfLeadingZeros(digits);
         nlz>>=2;
         nlz=16-nlz;

         // Preceding algorithm algorithm would return 0 for 0
         // and we need it to return a precision of 1
         if (nlz == 0)
            nlz++;

         tempFlags|= nlz << 7;
         this.flags=tempFlags;
         return nlz;
      }
      else{
         // DFPSignificance would return 0 for 0
         // and we need it to return a precision of 1
         if (sig ==0)
            sig++;

         tempFlags|=sig << 7;
         this.flags=tempFlags;
         return sig;
      }
   }


   private final int DFPSignumHelper() {
      int tempFlags = this.flags;

      // is it cached?
      if ((tempFlags&0x4)!=0)
         return (((tempFlags & 0x00000060) << 25) >>30);

      //we're going to cache it
      tempFlags|=0x4;  //cache on

      //check for negative first
      if ((this.laside & 0x8000000000000000L) == 0x8000000000000000L){
         tempFlags|=0x60; //store negative
         this.flags=tempFlags;
         return -1;
      }

      //now we're checking for positive or zero
      long mask = DFPBCDDigits(this.laside);
      if (mask == 10){
         //still haven't jitted the method
         if (this.isDFPZero()){
            tempFlags&=0xFFFFFF9F; //clear the signum cache (00)
            this.flags=tempFlags;
            return 0;
         }
         else{
            tempFlags&=0xFFFFFF9F; //clear the signum cache
            tempFlags|=0x20; //store positive
            this.flags=tempFlags;
            return 1;
         }
      }
      else if (mask !=0){
         tempFlags&=0xFFFFFF9F; //clear the signum cache
         tempFlags|=0x20; //store positive
         this.flags=tempFlags;
         return 1;
      }
      else
         tempFlags&=0xFFFFFF9F; //clear the signum cache (00)
      this.flags=tempFlags;
      return 0;
   }

   private final BigInteger DFPUnscaledValueHelper() {
      long lVal = DFPUnscaledValue(this.laside);
      if (lVal != Long.MAX_VALUE){
         this.bi = BigInteger.valueOf(lVal);
         return this.bi;
      }
      else{
         lVal = DFPBCDDigits(this.laside);
         if (lVal == 10){
            lVal = extractDFPDigitsBCD(this.laside);
         }

         //check for zero
         if (lVal == 0){
            this.bi = BigInteger.ZERO;
            return this.bi;
         }

         // now extract each 4 bit BCD digit from left to right
         long val=0;  //to store the result
         int i=0;
         while (lVal!= 0){
            val += (lVal & 0xF) * powerOfTenLL(i++);
            lVal >>>= 4;
         }
         this.bi = BigInteger.valueOf(DFPSignumHelper()*val);
         return this.bi;
      }
   }


   private static BigDecimal DFPValueOfHelper(long lint, int scale, BigDecimal res){
      int tempFlags=0;
      if (DFPHWAvailable() && DFPUseDFP()){

         if (lint == 0 && scale==0){
            res.DFPConstructZero();
            return res;
         }

         if (lint<=9999999999999999L && lint>=-9999999999999999L &&
               -scale>= -398 && -scale<= 369){

            // NOTE: When we don't specify rounding in DFP, we'll never
            // fail, so don't bother checking the return value
            if (res.DFPLongExpConstructor(lint, -scale+398,0, 0, 0, false)){

               /* cache: SGP ESRR */

               // store DFP representation
               // (already 00)

               // cache the exponent for all DFP paths
               tempFlags|=0x8; //cache on
               res.cachedScale=scale;

               // cache the sign for DFP, on all paths
               tempFlags|=0x4; //cache on
               if (lint < 0)
                  tempFlags|=0x60; //isneg
               else if (lint > 0)
                  tempFlags|=0x20; //ispos
               //iszero is already 00

               res.flags = tempFlags;
               return res;
            }
         }
      }
      return null;
   }

   private static BigDecimal slowSMSS(
                        BigDecimal valueOfObject,
                        BigDecimal subtractObject,
                        BigDecimal multiplyObject) {

        return multiplyObject.multiply(
                valueOfObject.subtract(subtractObject))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
                }

        private static boolean noLLOverflowAdd(long lhs, long rhs, long sum) {
                return ((((sum)^lhs) & ((sum)^rhs)) >>> 63) == 0;
                }

        private static boolean noLLOverflowMul(long lhs, long rhs, long prod) {
                return (lhs < 2147483647) && (lhs > -2147483648) && (lhs < 2147483647) && (rhs > -2147483648);
                }

     private static BigDecimal slowSMAAMSS(
                BigDecimal valueOfObject,
                BigDecimal subtractObject,
                BigDecimal multiplyObject,
                BigDecimal add1Object,
                BigDecimal add2Object){

        BigDecimal subtotal = multiplyObject.multiply(
                (valueOfObject).subtract(subtractObject))
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        return  subtotal.multiply(
                (valueOfObject).add(add1Object).add(add2Object))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
     }

     private static BigDecimal SMAAMSS(
                BigDecimal valueOfObject,
                BigDecimal subtractObject,
                BigDecimal multiplyObject,
                BigDecimal add1Object,
                BigDecimal add2Object,
                int subtractArgScale,
                int multiplyArgScale,
                int add1ArgScale,
                int add2ArgScale) {

        if ((multiplyObject != null) && (multiplyObject.getClass() == java.math.BigDecimal.class) &&
            (subtractObject.cachedScale == subtractArgScale) &&
            (multiplyObject.cachedScale == multiplyArgScale) &&
            (add1Object.cachedScale     == add1ArgScale)     &&
            (add2Object.cachedScale     == add2ArgScale)     &&
            ((subtractObject.flags & multiplyObject.flags & add1Object.flags & add2Object.flags & 1) == 1)) {
              long powerOfTen = (long) Math.pow(10, subtractArgScale);
              long temp1 = powerOfTen - subtractObject.laside;
              if (noLLOverflowAdd(powerOfTen, -subtractObject.laside, temp1)) {
                 long temp2 = temp1 * multiplyObject.laside;
                 if (noLLOverflowMul(temp1, multiplyObject.laside, temp2)) {
                    long rhs = powerOfTen/2;
                    long temp3 = temp2 + rhs;
                    if (noLLOverflowAdd(temp2, rhs, temp3)) {
                       temp3 = temp3/powerOfTen;
                       long temp4 = powerOfTen + add1Object.laside;
                       if (noLLOverflowAdd(powerOfTen, add1Object.laside, temp4)) {
                          long temp5 = temp4 + add2Object.laside;
                          if (noLLOverflowAdd(temp4, add2Object.laside, temp5)) {
                             long temp6 = temp5 * temp3;
                             if (noLLOverflowMul(temp5, temp3, temp6)) {
                                long temp7 = temp6 + rhs;
                                if (noLLOverflowAdd(temp6, rhs, temp7)) {
                                   return BigDecimal.valueOf(temp7/powerOfTen, 2);
                                   }
                                }
                             }
                          }
                       }
                    }
                 }
              }
        // Fall thru case
        return slowSMAAMSS(valueOfObject, subtractObject, multiplyObject, add1Object, add2Object);
     }

    private static BigDecimal SMSS(
                BigDecimal valueOfObject,
                BigDecimal subtractObject,
                BigDecimal multiplyObject,
                int subtractArgScale,
                int multiplyArgScale) {

        if ((multiplyObject != null) && (multiplyObject.getClass() == java.math.BigDecimal.class) &&
            (subtractObject.cachedScale == subtractArgScale) &&
            (multiplyObject.cachedScale == multiplyArgScale) &&
            ((subtractObject.flags & multiplyObject.flags & 1) == 1)) {
              long powerOfTen = (long) Math.pow(10, subtractArgScale);
              long temp1 = powerOfTen - subtractObject.laside;
              if (noLLOverflowAdd(powerOfTen, -subtractObject.laside, temp1)) {
                  long temp2 = temp1 * multiplyObject.laside;
                  if (noLLOverflowMul(temp1, multiplyObject.laside, temp2)) {
                      long rhs = powerOfTen/2;
                      long temp3 = temp2 + rhs;
                      if (noLLOverflowAdd(temp2, rhs, temp3)) {
                          return BigDecimal.valueOf(temp3/powerOfTen, 2);
                      }
                  }
              }
        }

        // Fall thru case
        return slowSMSS(valueOfObject, subtractObject, multiplyObject);
     }

    private static BigDecimal slowAAMSS(
                BigDecimal valueOfObject,
                BigDecimal add1Object,
                BigDecimal add2Object,
                BigDecimal multiplyObject) {
        return multiplyObject.multiply(
               (valueOfObject).add(add1Object).add(add2Object))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
     }

     private static BigDecimal AAMSS(
                 BigDecimal valueOfObject,
                 BigDecimal add1Object,
                 BigDecimal add2Object,
                 BigDecimal multiplyObject,
                 int add1ArgScale,
                 int add2ArgScale,
                 int multiplyArgScale)  {

        if ((multiplyObject != null) && (multiplyObject.getClass() == java.math.BigDecimal.class) &&
            (add1Object.cachedScale == add1ArgScale) &&
            (add2Object.cachedScale == add2ArgScale) &&
            (multiplyObject.cachedScale == multiplyArgScale) &&
            ((add1Object.flags & add2Object.flags & multiplyObject.flags & 1) == 1)) {
                        long powerOfTen = (long) Math.pow(10, add1ArgScale);
                        long temp1 = powerOfTen + add1Object.laside;
                        if (noLLOverflowAdd(powerOfTen, add1Object.laside, temp1)) {
                                long temp2 = temp1 + add2Object.laside;
                                if (noLLOverflowAdd(temp1, add2Object.laside, temp2)) {
                                        long temp3 = temp2 * multiplyObject.laside;
                                        if (noLLOverflowMul(temp2, multiplyObject.laside, temp3)) {
                                                long rhs = powerOfTen/2;
                                                long temp4 = temp3 + rhs;
                                                if (noLLOverflowAdd(temp3, rhs, temp4)) {
                                                        return BigDecimal.valueOf(temp4/powerOfTen, 2);
                                                }
                                        }
                                }
                        }
                }

        return slowAAMSS(valueOfObject, add1Object, add2Object, multiplyObject);

     }

     private static BigDecimal slowMSS(
                 BigDecimal valueOfObject,
                 BigDecimal multiplyObject) {
         return valueOfObject.multiply(multiplyObject).setScale(2, BigDecimal.ROUND_HALF_UP);
     }

     private static BigDecimal MSS(
                 BigDecimal valueOfObject,
                 BigDecimal multiplyObject,
                 int multiplyArgScale)  {

         if ((multiplyObject.cachedScale == multiplyArgScale) &&
                         ((valueOfObject.flags & multiplyObject.flags & 1) == 1)) {
            long temp1 = valueOfObject.laside * multiplyObject.laside;
            if (noLLOverflowMul(valueOfObject.laside,multiplyObject.laside, temp1)) {
                return BigDecimal.valueOf(temp1, 2);
            }
         }

         return slowMSS(valueOfObject, multiplyObject);
         }

    private static BigDecimal SMSetScale(
                BigDecimal subtractObject,
                BigDecimal multiplyObject,
                int subtractArgScale,
                int multiplyArgScale) {

        if ((multiplyObject != null) && (multiplyObject.getClass() == java.math.BigDecimal.class) &&
            (subtractObject.cachedScale == subtractArgScale) &&
            (multiplyObject.cachedScale == multiplyArgScale) &&
            ((subtractObject.flags & multiplyObject.flags & 1) == 1)) {
              long powerOfTen = (long) Math.pow(10, subtractArgScale);
              long temp1 = powerOfTen - subtractObject.laside;
              if (noLLOverflowAdd(powerOfTen, -subtractObject.laside, temp1)) {
                  long temp2 = temp1 * multiplyObject.laside;
                  if (noLLOverflowMul(temp1, multiplyObject.laside, temp2)) {
                      return BigDecimal.valueOf(temp2/powerOfTen, 2);
                  }
              }
        }

        // Fall thru case
        return slowSMSetScale(subtractObject, multiplyObject);
    }

    private static BigDecimal slowSMSetScale(
                BigDecimal subtractObject, 
                BigDecimal multiplyObject) {
        return multiplyObject.multiply(
                BigDecimal.ONE.subtract(subtractObject))
                .setScale(2, BigDecimal.ROUND_DOWN);
    }

}


