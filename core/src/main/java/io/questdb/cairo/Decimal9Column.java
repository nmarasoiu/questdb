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

import io.questdb.std.str.CharSink;

/**
 * DECIMAL9 column implementation backed by INT storage.
 * Provides up to 9 digits of precision with configurable scale.
 * 
 * Examples:
 * - DECIMAL9(4): Max ±99,999.9999 (5 digits before, 4 after decimal)
 * - DECIMAL9(2): Max ±9,999,999.99 (7 digits before, 2 after decimal)
 * - DECIMAL9(0): Max ±999,999,999 (9 digits, no decimal)
 */
public class Decimal9Column extends DecimalColumn {
    
    // INT storage limits
    public static final int MIN_VALUE = Integer.MIN_VALUE;
    public static final int MAX_VALUE = Integer.MAX_VALUE;
    
    // Common scale limits for DECIMAL9  
    public static final int MAX_SCALE = 9;
    
    public Decimal9Column(int scale) {
        super(DecimalColumnType.DECIMAL9, scale);
        
        if (scale > MAX_SCALE) {
            throw new IllegalArgumentException(
                "Scale " + scale + " exceeds maximum " + MAX_SCALE + " for DECIMAL9"
            );
        }
    }
    
    /**
     * Parses a decimal value from string representation.
     * 
     * @param value decimal string (e.g., "123.45")
     * @return scaled int value for storage
     */
    public int parseDecimal(CharSequence value) {
        long longValue = parseDecimalString(value);
        
        if (longValue < MIN_VALUE || longValue > MAX_VALUE) {
            throw new ArithmeticException("Value exceeds DECIMAL9 range: " + value);
        }
        
        return (int) longValue;
    }
    
    /**
     * Formats a stored int value as decimal string.
     * 
     * @param scaledValue scaled int value from storage
     * @param sink output sink
     */
    public void formatDecimal(int scaledValue, CharSink sink) {
        formatDecimalToSink((long) scaledValue, sink);
    }
    
    /**
     * Formats a stored int value as decimal string.
     * 
     * @param scaledValue scaled int value from storage
     * @return decimal string representation
     */
    public String formatDecimal(int scaledValue) {
        return formatDecimalToString((long) scaledValue);
    }
    
    /**
     * Adds two DECIMAL9 values.
     * 
     * @param value1 first scaled int value
     * @param value2 second scaled int value
     * @return sum as scaled int value
     * @throws ArithmeticException if overflow occurs
     */
    public int add(int value1, int value2) {
        long result = (long) value1 + value2;
        
        if (result < MIN_VALUE || result > MAX_VALUE) {
            throw new ArithmeticException("DECIMAL9 addition overflow");
        }
        
        return (int) result;
    }
    
    /**
     * Subtracts two DECIMAL9 values.
     * 
     * @param value1 first scaled int value (minuend)  
     * @param value2 second scaled int value (subtrahend)
     * @return difference as scaled int value
     * @throws ArithmeticException if overflow occurs
     */
    public int subtract(int value1, int value2) {
        long result = (long) value1 - value2;
        
        if (result < MIN_VALUE || result > MAX_VALUE) {
            throw new ArithmeticException("DECIMAL9 subtraction overflow");
        }
        
        return (int) result;
    }
    
    /**
     * Multiplies two DECIMAL9 values with proper scale handling.
     * Result scale = scale1 + scale2, then scaled down to this column's scale.
     * 
     * @param value1 first scaled int value
     * @param value2 second scaled int value
     * @return product as scaled int value
     * @throws ArithmeticException if overflow occurs
     */
    public int multiply(int value1, int value2) {
        // Perform multiplication in long to detect overflow
        long product = (long) value1 * value2;
        
        // Adjust scale: divide by scaleFactor with rounding
        long scaledProduct = scaleValue(product, scale * 2, scale);
        
        if (scaledProduct < MIN_VALUE || scaledProduct > MAX_VALUE) {
            throw new ArithmeticException("DECIMAL9 multiplication overflow");
        }
        
        return (int) scaledProduct;
    }
    
    /**
     * Divides two DECIMAL9 values with proper scale handling.
     * To maintain precision, multiply dividend by scaleFactor before division.
     * 
     * @param dividend scaled int value (numerator)
     * @param divisor scaled int value (denominator)
     * @return quotient as scaled int value
     * @throws ArithmeticException if divisor is zero or overflow occurs
     */
    public int divide(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        
        // To maintain precision, multiply dividend by scaleFactor
        long scaledDividend = (long) dividend * scaleFactor;
        
        // Perform division with rounding
        long quotient = scaledDividend / divisor;
        long remainder = scaledDividend % divisor;
        
        // Round half up
        if (Math.abs(remainder) >= Math.abs(divisor) / 2) {
            quotient += (scaledDividend >= 0) == (divisor >= 0) ? 1 : -1;
        }
        
        if (quotient < MIN_VALUE || quotient > MAX_VALUE) {
            throw new ArithmeticException("DECIMAL9 division overflow");
        }
        
        return (int) quotient;
    }
    
    /**
     * Compares two DECIMAL9 values.
     * 
     * @param value1 first scaled int value
     * @param value2 second scaled int value
     * @return -1 if value1 < value2, 0 if equal, 1 if value1 > value2
     */
    public int compare(int value1, int value2) {
        return Integer.compare(value1, value2);
    }
    
    /**
     * Checks if a scaled value would cause overflow for this decimal type.
     * 
     * @param scaledValue scaled value to check
     * @return true if value is within INT range, false otherwise
     */
    public boolean isValidValue(int scaledValue) {
        return scaledValue >= MIN_VALUE && scaledValue <= MAX_VALUE;
    }
    
    /**
     * Checks if a long value can be safely stored as DECIMAL9.
     * 
     * @param longValue long value to check
     * @return true if value fits in INT range, false otherwise
     */
    public boolean isValidValue(long longValue) {
        return longValue >= MIN_VALUE && longValue <= MAX_VALUE;
    }
    
    /**
     * Gets the minimum value for this decimal configuration.
     * 
     * @return minimum decimal value as string
     */
    public String getMinValue() {
        return formatDecimal(MIN_VALUE);
    }
    
    /**
     * Gets the maximum value for this decimal configuration.
     * 
     * @return maximum decimal value as string
     */
    public String getMaxValue() {
        return formatDecimal(MAX_VALUE);
    }
    
    /**
     * Creates a DECIMAL9 instance with specified scale.
     * 
     * @param scale number of digits after decimal point
     * @return new DECIMAL9 column instance
     */
    public static Decimal9Column create(int scale) {
        return new Decimal9Column(scale);
    }
    
    @Override
    public String toString() {
        return "DECIMAL9(" + scale + ")";
    }
}