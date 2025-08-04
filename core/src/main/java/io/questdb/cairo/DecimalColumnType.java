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
 * Decimal column type utilities for DECIMAL9, DECIMAL18, DECIMAL38, DECIMAL77 types.
 * These types store decimal values as scaled integers to maintain vectorization while
 * providing decimal semantics through column metadata.
 * 
 * This class extends QuestDB's type system with decimal types that map to existing
 * storage types but provide decimal semantics through scale metadata.
 */
public final class DecimalColumnType {
    
    // Decimal type constants - extend ColumnType with decimal variants
    public static final short DECIMAL9 = ColumnType.NULL + 1;   // = 34, maps to INT storage
    public static final short DECIMAL18 = DECIMAL9 + 1;         // = 35, maps to LONG storage  
    public static final short DECIMAL38 = DECIMAL18 + 1;        // = 36, maps to LONG128 storage
    public static final short DECIMAL77 = DECIMAL38 + 1;        // = 37, maps to LONG256 storage
    
    // Scale encoding in extra type information field (bits 8-15)
    private static final int SCALE_SHIFT = 8;
    private static final int SCALE_MASK = 0xFF;
    private static final int MAX_SCALE = 255;
    
    // Type flag to identify decimal types
    private static final int DECIMAL_TYPE_FLAG = (1 << 20);
    
    // Base type mappings for storage
    private static final short[] BASE_TYPES = {
        ColumnType.INT,     // DECIMAL9  -> INT storage
        ColumnType.LONG,    // DECIMAL18 -> LONG storage
        ColumnType.LONG128, // DECIMAL38 -> LONG128 storage  
        ColumnType.LONG256  // DECIMAL77 -> LONG256 storage
    };
    
    private DecimalColumnType() {
    }
    
    /**
     * Creates a decimal column type with the specified base type and scale.
     * 
     * @param decimalType one of DECIMAL9, DECIMAL18, DECIMAL38, DECIMAL77
     * @param scale number of digits after decimal point (0-255)
     * @return encoded column type with decimal flag and scale
     */
    public static int createType(short decimalType, int scale) {
        if (scale < 0 || scale > MAX_SCALE) {
            throw new IllegalArgumentException("Scale must be between 0 and " + MAX_SCALE + ", got: " + scale);
        }
        if (!isDecimalType(decimalType)) {
            throw new IllegalArgumentException("Invalid decimal type: " + decimalType);
        }
        
        return decimalType | DECIMAL_TYPE_FLAG | (scale << SCALE_SHIFT);
    }
    
    /**
     * Extracts the scale from an encoded decimal column type.
     * 
     * @param columnType encoded decimal column type
     * @return scale (digits after decimal point)
     */
    public static int getScale(int columnType) {
        if (!isDecimal(columnType)) {
            return 0;
        }
        return (columnType >> SCALE_SHIFT) & SCALE_MASK;
    }
    
    /**
     * Gets the base decimal type (DECIMAL9, DECIMAL18, etc.) from encoded type.
     * 
     * @param columnType encoded decimal column type
     * @return base decimal type tag
     */
    public static short getDecimalType(int columnType) {
        return (short) (columnType & 0xFF);
    }
    
    /**
     * Gets the underlying storage type for a decimal type.
     * 
     * @param decimalType one of DECIMAL9, DECIMAL18, DECIMAL38, DECIMAL77
     * @return corresponding storage type (INT, LONG, LONG128, LONG256)
     */
    public static short getStorageType(short decimalType) {
        if (!isDecimalType(decimalType)) {
            throw new IllegalArgumentException("Not a decimal type: " + decimalType);
        }
        
        int index = decimalType - DECIMAL9;
        return BASE_TYPES[index];
    }
    
    /**
     * Checks if a column type is a decimal type.
     * 
     * @param columnType column type to check
     * @return true if decimal type, false otherwise
     */
    public static boolean isDecimal(int columnType) {
        return (columnType & DECIMAL_TYPE_FLAG) != 0 && isDecimalType((short) (columnType & 0xFF));
    }
    
    /**
     * Checks if a type tag is a decimal type.
     * 
     * @param typeTag type tag to check
     * @return true if decimal type tag, false otherwise
     */
    public static boolean isDecimalType(short typeTag) {
        return typeTag >= DECIMAL9 && typeTag <= DECIMAL77;
    }
    
    /**
     * Gets maximum precision (total digits) for a decimal type.
     * 
     * @param decimalType one of DECIMAL9, DECIMAL18, DECIMAL38, DECIMAL77
     * @return maximum precision
     */
    public static int getMaxPrecision(short decimalType) {
        switch (decimalType) {
            case DECIMAL9:  return 9;
            case DECIMAL18: return 18;
            case DECIMAL38: return 38;
            case DECIMAL77: return 77;
            default:
                throw new IllegalArgumentException("Unknown decimal type: " + decimalType);
        }
    }
    
    /**
     * Validates that scale doesn't exceed precision for the decimal type.
     * 
     * @param decimalType decimal type
     * @param scale scale to validate
     * @throws IllegalArgumentException if scale > precision
     */
    public static void validateScale(short decimalType, int scale) {
        int maxPrecision = getMaxPrecision(decimalType);
        if (scale > maxPrecision) {
            throw new IllegalArgumentException(
                "Scale " + scale + " exceeds maximum precision " + maxPrecision + 
                " for type " + getTypeName(decimalType)
            );
        }
    }
    
    /**
     * Gets the display name for a decimal type.
     * 
     * @param decimalType decimal type
     * @return type name (e.g., "DECIMAL9", "DECIMAL18")
     */
    public static String getTypeName(short decimalType) {
        switch (decimalType) {
            case DECIMAL9:  return "DECIMAL9";
            case DECIMAL18: return "DECIMAL18";
            case DECIMAL38: return "DECIMAL38";
            case DECIMAL77: return "DECIMAL77";
            default:
                return "UNKNOWN_DECIMAL_" + decimalType;
        }
    }
    
    /**
     * Gets the display name for an encoded decimal column type including scale.
     * 
     * @param columnType encoded decimal column type
     * @return formatted name (e.g., "DECIMAL18(4)")
     */
    public static String getFullTypeName(int columnType) {
        if (!isDecimal(columnType)) {
            return "NOT_DECIMAL";
        }
        
        short decimalType = getDecimalType(columnType);
        int scale = getScale(columnType);
        return getTypeName(decimalType) + "(" + scale + ")";
    }
}