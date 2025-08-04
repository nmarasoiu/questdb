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

import io.questdb.std.Numbers;
import io.questdb.std.str.CharSink;
import io.questdb.std.str.StringSink;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Base class for decimal column operations. Provides common functionality
 * for formatting, parsing, and arithmetic operations on decimal values
 * stored as scaled integers.
 */
public abstract class DecimalColumn {
    
    // Pre-computed powers of 10 for common scales (performance optimization)
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
    
    protected final int scale;
    protected final long scaleFactor;
    protected final short decimalType;
    
    protected DecimalColumn(short decimalType, int scale) {
        this.decimalType = decimalType;
        this.scale = scale;
        this.scaleFactor = getScaleFactor(scale);
        
        DecimalColumnType.validateScale(decimalType, scale);
    }
    
    /**
     * Gets the scale factor (10^scale) for this decimal column.
     * 
     * @param scale decimal scale
     * @return scale factor as long
     */
    protected static long getScaleFactor(int scale) {
        if (scale < POWERS_OF_TEN.length) {
            return POWERS_OF_TEN[scale];
        }
        
        // For scales > 18, compute dynamically
        long factor = POWERS_OF_TEN[POWERS_OF_TEN.length - 1];
        for (int i = POWERS_OF_TEN.length; i <= scale; i++) {
            factor *= 10;
        }
        return factor;
    }
    
    /**
     * Parses a decimal string into scaled integer representation.
     * 
     * @param value decimal string (e.g., "123.45")
     * @return scaled integer value
     */
    protected long parseDecimalString(CharSequence value) {
        if (value == null || value.length() == 0) {
            return 0;
        }
        
        try {
            BigDecimal decimal = new BigDecimal(value.toString());
            decimal = decimal.setScale(scale, RoundingMode.HALF_UP);
            return decimal.unscaledValue().longValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal value: " + value, e);
        }
    }
    
    /**
     * Formats a scaled integer value as decimal string.
     * 
     * @param scaledValue scaled integer value
     * @param sink output sink
     */
    protected void formatDecimalToSink(long scaledValue, CharSink sink) {
        if (scale == 0) {
            sink.put(scaledValue);
            return;
        }
        
        boolean negative = scaledValue < 0;
        if (negative) {
            sink.put('-');
            scaledValue = -scaledValue;
        }
        
        long integerPart = scaledValue / scaleFactor;
        long fractionalPart = scaledValue % scaleFactor;
        
        sink.put(integerPart);
        
        if (scale > 0 && fractionalPart > 0) {
            sink.put('.');
            
            // Pad with leading zeros if needed
            String fracStr = Long.toString(fractionalPart);
            int padding = scale - fracStr.length();
            for (int i = 0; i < padding; i++) {
                sink.put('0');
            }
            
            // Remove trailing zeros for cleaner display
            int lastNonZero = fracStr.length() - 1;
            while (lastNonZero >= 0 && fracStr.charAt(lastNonZero) == '0') {
                lastNonZero--;
            }
            
            if (lastNonZero >= 0) {
                sink.put(fracStr, 0, lastNonZero + 1);
            }
        }
    }
    
    /**
     * Formats a scaled integer value as decimal string.
     * 
     * @param scaledValue scaled integer value
     * @return formatted decimal string
     */
    protected String formatDecimalToString(long scaledValue) {
        StringSink sink = new StringSink();
        formatDecimalToSink(scaledValue, sink);
        return sink.toString();
    }
    
    /**
     * Adds two decimal values with automatic scale alignment.
     * 
     * @param value1 first value
     * @param scale1 scale of first value
     * @param value2 second value  
     * @param scale2 scale of second value
     * @return sum with this column's scale
     */
    protected long addWithScaleAlignment(long value1, int scale1, long value2, int scale2) {
        if (scale1 == scale2 && scale1 == this.scale) {
            return value1 + value2;
        }
        
        // Convert both values to this column's scale
        long normalizedValue1 = scaleValue(value1, scale1, this.scale);
        long normalizedValue2 = scaleValue(value2, scale2, this.scale);
        
        return normalizedValue1 + normalizedValue2;
    }
    
    /**
     * Scales a value from one scale to another.
     * 
     * @param value value to scale
     * @param fromScale current scale
     * @param toScale target scale
     * @return scaled value
     */
    protected long scaleValue(long value, int fromScale, int toScale) {
        if (fromScale == toScale) {
            return value;
        }
        
        if (fromScale < toScale) {
            // Scale up - multiply by 10^(toScale - fromScale)
            int scaleDiff = toScale - fromScale;
            return value * getScaleFactor(scaleDiff);
        } else {
            // Scale down - divide by 10^(fromScale - toScale) with rounding
            int scaleDiff = fromScale - toScale;
            long divisor = getScaleFactor(scaleDiff);
            
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
     * Gets the decimal type of this column.
     * 
     * @return decimal type (DECIMAL9, DECIMAL18, etc.)
     */
    public short getDecimalType() {
        return decimalType;
    }
    
    /**
     * Gets the scale of this column.
     * 
     * @return scale (digits after decimal point)
     */
    public int getScale() {
        return scale;
    }
    
    /**
     * Gets the scale factor for this column.
     * 
     * @return scale factor (10^scale)
     */
    public long getScaleFactor() {
        return scaleFactor;
    }
    
    /**
     * Gets the maximum precision for this decimal type.
     * 
     * @return maximum total digits
     */
    public int getMaxPrecision() {
        return DecimalColumnType.getMaxPrecision(decimalType);
    }
    
    /**
     * Gets the underlying storage type for this decimal type.  
     * 
     * @return storage type (INT, LONG, LONG128, LONG256)
     */
    public short getStorageType() {
        return DecimalColumnType.getStorageType(decimalType);
    }
}