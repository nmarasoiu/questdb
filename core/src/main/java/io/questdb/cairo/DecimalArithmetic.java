/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.cairo;

/**
 * Arithmetic operations for decimal values with automatic scale handling.
 * Handles operations between decimal types with different scales and
 * determines appropriate result scales and types.
 */
public class DecimalArithmetic {
    
    // Pre-computed powers of 10 for efficient scaling
    private static final long[] POWERS_OF_TEN = {
        1L,                     // 10^0
        10L,                    // 10^1
        100L,                   // 10^2
        1_000L,                 // 10^3
        10_000L,                // 10^4
        100_000L,               // 10^5
        1_000_000L,             // 10^6
        10_000_000L,            // 10^7
        100_000_000L,           // 10^8
        1_000_000_000L,         // 10^9
        10_000_000_000L,        // 10^10
        100_000_000_000L,       // 10^11
        1_000_000_000_000L,     // 10^12
        10_000_000_000_000L,    // 10^13
        100_000_000_000_000L,   // 10^14
        1_000_000_000_000_000L, // 10^15
        10_000_000_000_000_000L,// 10^16
        100_000_000_000_000_000L,// 10^17
        1_000_000_000_000_000_000L// 10^18
    };
    
    private DecimalArithmetic() {
    }
    
    /**
     * Gets the power of 10 for a given exponent.
     * 
     * @param exponent power of 10 to compute
     * @return 10^exponent as long
     */
    public static long powerOfTen(int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("Negative exponent not supported: " + exponent);
        }
        
        if (exponent < POWERS_OF_TEN.length) {
            return POWERS_OF_TEN[exponent];
        }
        
        // For very large exponents, compute dynamically
        long result = POWERS_OF_TEN[POWERS_OF_TEN.length - 1];
        for (int i = POWERS_OF_TEN.length; i <= exponent; i++) {
            if (result > Long.MAX_VALUE / 10) {
                throw new ArithmeticException("Power of 10 overflow for exponent: " + exponent);
            }
            result *= 10;
        }
        return result;
    }
    
    /**
     * Scales a value from one scale to another.
     * 
     * @param value value to scale
     * @param fromScale current scale
     * @param toScale target scale
     * @return scaled value
     * @throws ArithmeticException if scaling would cause overflow
     */
    public static long scaleValue(long value, int fromScale, int toScale) {
        if (fromScale == toScale) {
            return value;
        }
        
        if (fromScale < toScale) {
            // Scale up - multiply by 10^(toScale - fromScale)
            int scaleDiff = toScale - fromScale;
            long scaleFactor = powerOfTen(scaleDiff);
            
            // Check for overflow before multiplication
            if (value > 0 && value > Long.MAX_VALUE / scaleFactor) {
                throw new ArithmeticException("Scale up overflow");
            }
            if (value < 0 && value < Long.MIN_VALUE / scaleFactor) {
                throw new ArithmeticException("Scale up overflow");
            }
            
            return value * scaleFactor;
        } else {
            // Scale down - divide by 10^(fromScale - toScale) with rounding
            int scaleDiff = fromScale - toScale;
            long divisor = powerOfTen(scaleDiff);
            
            // Round half up
            long remainder = value % divisor;
            long result = value / divisor;
            
            if (Math.abs(remainder) >= divisor / 2) {
                result += (value >= 0) ? 1 : -1;
            }
            
            return result;
        }
    }
    
    /**
     * Adds two decimal values with different scales.
     * Result has the higher of the two scales.
     * 
     * @param value1 first decimal value
     * @param scale1 scale of first value
     * @param value2 second decimal value
     * @param scale2 scale of second value
     * @return AddResult with sum and result scale
     */
    public static AddResult add(long value1, int scale1, long value2, int scale2) {
        int resultScale = Math.max(scale1, scale2);
        
        long scaledValue1 = scaleValue(value1, scale1, resultScale);
        long scaledValue2 = scaleValue(value2, scale2, resultScale);
        
        // Check for overflow
        long result = scaledValue1 + scaledValue2;
        if (((scaledValue1 ^ result) & (scaledValue2 ^ result)) < 0) {
            throw new ArithmeticException("Addition overflow");
        }
        
        return new AddResult(result, resultScale);
    }
    
    /**
     * Subtracts two decimal values with different scales.
     * Result has the higher of the two scales.
     * 
     * @param value1 first decimal value (minuend)
     * @param scale1 scale of first value
     * @param value2 second decimal value (subtrahend)
     * @param scale2 scale of second value
     * @return SubtractResult with difference and result scale
     */
    public static SubtractResult subtract(long value1, int scale1, long value2, int scale2) {
        int resultScale = Math.max(scale1, scale2);
        
        long scaledValue1 = scaleValue(value1, scale1, resultScale);
        long scaledValue2 = scaleValue(value2, scale2, resultScale);
        
        // Check for overflow
        long result = scaledValue1 - scaledValue2;
        if (((scaledValue1 ^ scaledValue2) & (scaledValue1 ^ result)) < 0) {
            throw new ArithmeticException("Subtraction overflow");
        }
        
        return new SubtractResult(result, resultScale);
    }
    
    /**
     * Multiplies two decimal values with different scales.
     * Result scale is the sum of input scales.
     * 
     * @param value1 first decimal value
     * @param scale1 scale of first value
     * @param value2 second decimal value
     * @param scale2 scale of second value
     * @return MultiplyResult with product and result scale
     */
    public static MultiplyResult multiply(long value1, int scale1, long value2, int scale2) {
        int resultScale = scale1 + scale2;
        
        // Use Math.multiplyExact for overflow detection
        long result = Math.multiplyExact(value1, value2);
        
        return new MultiplyResult(result, resultScale);
    }
    
    /**
     * Divides two decimal values with different scales.
     * To maintain precision, result scale is scale1 - scale2 + extraScale.
     * 
     * @param dividend decimal dividend
     * @param scale1 scale of dividend
     * @param divisor decimal divisor
     * @param scale2 scale of divisor
     * @param extraScale additional scale to maintain precision
     * @return DivideResult with quotient and result scale
     */
    public static DivideResult divide(long dividend, int scale1, long divisor, int scale2, int extraScale) {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        
        int resultScale = scale1 - scale2 + extraScale;
        
        // To maintain precision, multiply dividend by 10^extraScale
        long scaledDividend = dividend;
        if (extraScale > 0) {
            long scaleFactor = powerOfTen(extraScale);
            scaledDividend = Math.multiplyExact(dividend, scaleFactor);
        }
        
        // Perform division with rounding
        long quotient = scaledDividend / divisor;
        long remainder = scaledDividend % divisor;
        
        // Round half up
        if (Math.abs(remainder) >= Math.abs(divisor) / 2) {
            quotient += (scaledDividend >= 0) == (divisor >= 0) ? 1 : -1;
        }
        
        return new DivideResult(quotient, resultScale);
    }
    
    /**
     * Determines the appropriate result type for arithmetic operations.
     * 
     * @param type1 first decimal type
     * @param type2 second decimal type
     * @return result type (widest of the two)
     */
    public static short determineResultType(short type1, short type2) {
        // Return the widest type
        return (short) Math.max(type1, type2);
    }
    
    /**
     * Determines if a result would fit in the target decimal type.
     * 
     * @param value result value
     * @param scale result scale
     * @param targetType target decimal type
     * @return true if value fits, false if overflow would occur
     */
    public static boolean fitsInType(long value, int scale, short targetType) {
        switch (targetType) {
            case DecimalColumnType.DECIMAL9:
                return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
            case DecimalColumnType.DECIMAL18:
                return true; // long can always fit in DECIMAL18
            case DecimalColumnType.DECIMAL38:
                // TODO: Implement LONG128 range check
                return true;
            case DecimalColumnType.DECIMAL77:
                // TODO: Implement LONG256 range check  
                return true;
            default:
                return false;
        }
    }
    
    // Result classes for arithmetic operations
    
    public static class AddResult {
        public final long value;
        public final int scale;
        
        public AddResult(long value, int scale) {
            this.value = value;
            this.scale = scale;
        }
    }
    
    public static class SubtractResult {
        public final long value;
        public final int scale;
        
        public SubtractResult(long value, int scale) {
            this.value = value;
            this.scale = scale;
        }
    }
    
    public static class MultiplyResult {
        public final long value;
        public final int scale;
        
        public MultiplyResult(long value, int scale) {
            this.value = value;
            this.scale = scale;
        }
    }
    
    public static class DivideResult {
        public final long value;
        public final int scale;
        
        public DivideResult(long value, int scale) {
            this.value = value;
            this.scale = scale;
        }
    }
}