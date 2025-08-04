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
 * DECIMAL18 column implementation backed by LONG storage.
 * Provides up to 18 digits of precision with configurable scale.
 * 
 * Examples:
 * - DECIMAL18(4): Max ±999,999,999,999,999.9999 (14 digits before, 4 after decimal)
 * - DECIMAL18(2): Max ±9,999,999,999,999,999.99 (16 digits before, 2 after decimal)
 * - DECIMAL18(0): Max ±999,999,999,999,999,999 (18 digits, no decimal)
 */
public class Decimal18Column extends DecimalColumn {
    
    // LONG storage limits
    public static final long MIN_VALUE = Long.MIN_VALUE;
    public static final long MAX_VALUE = Long.MAX_VALUE;
    
    // Common scale limits for DECIMAL18
    public static final int MAX_SCALE = 18;
    
    public Decimal18Column(int scale) {
        super(DecimalColumnType.DECIMAL18, scale);
        
        if (scale > MAX_SCALE) {
            throw new IllegalArgumentException(
                "Scale " + scale + " exceeds maximum " + MAX_SCALE + " for DECIMAL18"
            );
        }
    }
    
    /**
     * Parses a decimal value from string representation.
     * 
     * @param value decimal string (e.g., "123.45")
     * @return scaled long value for storage
     */
    public long parseDecimal(CharSequence value) {
        return parseDecimalString(value);
    }
    
    /**
     * Formats a stored long value as decimal string.
     * 
     * @param scaledValue scaled long value from storage
     * @param sink output sink
     */
    public void formatDecimal(long scaledValue, CharSink sink) {
        formatDecimalToSink(scaledValue, sink);
    }
    
    /**
     * Formats a stored long value as decimal string.
     * 
     * @param scaledValue scaled long value from storage
     * @return decimal string representation
     */
    public String formatDecimal(long scaledValue) {
        return formatDecimalToString(scaledValue);
    }
    
    /**
     * Adds two DECIMAL18 values.
     * 
     * @param value1 first scaled long value
     * @param value2 second scaled long value
     * @return sum as scaled long value
     * @throws ArithmeticException if overflow occurs
     */
    public long add(long value1, long value2) {
        long result = value1 + value2;
        
        // Check for overflow
        if (((value1 ^ result) & (value2 ^ result)) < 0) {
            throw new ArithmeticException("DECIMAL18 addition overflow");
        }
        
        return result;
    }
    
    /**
     * Subtracts two DECIMAL18 values.
     * 
     * @param value1 first scaled long value (minuend)
     * @param value2 second scaled long value (subtrahend)
     * @return difference as scaled long value
     * @throws ArithmeticException if overflow occurs
     */
    public long subtract(long value1, long value2) {
        long result = value1 - value2;
        
        // Check for overflow
        if (((value1 ^ value2) & (value1 ^ result)) < 0) {
            throw new ArithmeticException("DECIMAL18 subtraction overflow");
        }
        
        return result;
    }
    
    /**
     * Multiplies two DECIMAL18 values with proper scale handling.
     * Result scale = scale1 + scale2, then scaled down to this column's scale.
     * 
     * @param value1 first scaled long value
     * @param value2 second scaled long value
     * @return product as scaled long value
     * @throws ArithmeticException if overflow occurs
     */
    public long multiply(long value1, long value2) {
        // For multiplication, we need to handle the scale adjustment
        // Since both values have scale digits after decimal, product has 2*scale
        // We need to divide by scaleFactor to get back to single scale
        
        // Use Math.multiplyExact for overflow detection
        long product = Math.multiplyExact(value1, value2);
        
        // Adjust scale: divide by scaleFactor with rounding
        return scaleValue(product, scale * 2, scale);
    }
    
    /**
     * Divides two DECIMAL18 values with proper scale handling.
     * To maintain precision, multiply dividend by scaleFactor before division.
     * 
     * @param dividend scaled long value (numerator)
     * @param divisor scaled long value (denominator)
     * @return quotient as scaled long value
     * @throws ArithmeticException if divisor is zero or overflow occurs
     */
    public long divide(long dividend, long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        
        // To maintain precision, multiply dividend by scaleFactor
        long scaledDividend = Math.multiplyExact(dividend, scaleFactor);
        
        // Perform division with rounding
        long quotient = scaledDividend / divisor;
        long remainder = scaledDividend % divisor;
        
        // Round half up
        if (Math.abs(remainder) >= Math.abs(divisor) / 2) {
            quotient += (scaledDividend >= 0) == (divisor >= 0) ? 1 : -1;
        }
        
        return quotient;
    }
    
    /**
     * Compares two DECIMAL18 values.
     * 
     * @param value1 first scaled long value
     * @param value2 second scaled long value
     * @return -1 if value1 < value2, 0 if equal, 1 if value1 > value2
     */
    public int compare(long value1, long value2) {
        return Long.compare(value1, value2);
    }
    
    /**
     * Checks if a scaled value would cause overflow for this decimal type.
     * 
     * @param scaledValue scaled value to check
     * @return true if value is within LONG range, false otherwise
     */
    public boolean isValidValue(long scaledValue) {
        return scaledValue >= MIN_VALUE && scaledValue <= MAX_VALUE;
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
     * Creates a DECIMAL18 instance with specified scale.
     * 
     * @param scale number of digits after decimal point
     * @return new DECIMAL18 column instance
     */
    public static Decimal18Column create(int scale) {
        return new Decimal18Column(scale);
    }
    
    @Override
    public String toString() {
        return "DECIMAL18(" + scale + ")";
    }
}