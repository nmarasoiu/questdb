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
 * Factory for creating decimal column instances and managing decimal type operations.
 * Provides centralized creation and validation logic for decimal columns.
 */
public class DecimalColumnFactory {
    
    private DecimalColumnFactory() {
    }
    
    /**
     * Creates a decimal column instance based on type and scale.
     * 
     * @param decimalType decimal type (DECIMAL9, DECIMAL18, DECIMAL38, DECIMAL77)
     * @param scale number of digits after decimal point
     * @return appropriate decimal column instance
     */
    public static DecimalColumn createColumn(short decimalType, int scale) {
        DecimalColumnType.validateScale(decimalType, scale);
        
        switch (decimalType) {
            case DecimalColumnType.DECIMAL9:
                return new Decimal9Column(scale);
            case DecimalColumnType.DECIMAL18:
                return new Decimal18Column(scale);
            case DecimalColumnType.DECIMAL38:
                // TODO: Implement when LONG128 decimal column is ready
                throw new UnsupportedOperationException("DECIMAL38 not yet implemented");
            case DecimalColumnType.DECIMAL77:
                // TODO: Implement when LONG256 decimal column is ready
                throw new UnsupportedOperationException("DECIMAL77 not yet implemented");
            default:
                throw new IllegalArgumentException("Unknown decimal type: " + decimalType);
        }
    }
    
    /**
     * Creates a decimal column instance from encoded column type.
     * 
     * @param columnType encoded decimal column type (includes scale)
     * @return appropriate decimal column instance
     */
    public static DecimalColumn createColumn(int columnType) {
        if (!DecimalColumnType.isDecimal(columnType)) {
            throw new IllegalArgumentException("Not a decimal column type: " + columnType);
        }
        
        short decimalType = DecimalColumnType.getDecimalType(columnType);
        int scale = DecimalColumnType.getScale(columnType);
        
        return createColumn(decimalType, scale);
    }
    
    /**
     * Determines the appropriate decimal type for a given precision requirement.
     * 
     * @param precision total number of digits required
     * @return recommended decimal type
     */
    public static short recommendDecimalType(int precision) {
        if (precision <= 0) {
            throw new IllegalArgumentException("Precision must be positive, got: " + precision);
        } else if (precision <= 9) {
            return DecimalColumnType.DECIMAL9;
        } else if (precision <= 18) {
            return DecimalColumnType.DECIMAL18;
        } else if (precision <= 38) {
            return DecimalColumnType.DECIMAL38;
        } else if (precision <= 77) {
            return DecimalColumnType.DECIMAL77;
        } else {
            throw new IllegalArgumentException("Precision " + precision + " exceeds maximum supported precision of 77");
        }
    }
    
    /**
     * Creates the most appropriate decimal column for the given precision and scale.
     * 
     * @param precision total number of digits
     * @param scale number of digits after decimal point
     * @return optimal decimal column instance
     */
    public static DecimalColumn createOptimalColumn(int precision, int scale) {
        short decimalType = recommendDecimalType(precision);
        return createColumn(decimalType, scale);
    }
    
    /**
     * Validates decimal type and scale combination.
     * 
     * @param decimalType decimal type to validate
     * @param scale scale to validate
     * @throws IllegalArgumentException if combination is invalid
     */
    public static void validateDecimalSpec(short decimalType, int scale) {
        if (!DecimalColumnType.isDecimalType(decimalType)) {
            throw new IllegalArgumentException("Invalid decimal type: " + decimalType);
        }
        
        if (scale < 0) {
            throw new IllegalArgumentException("Scale cannot be negative: " + scale);
        }
        
        DecimalColumnType.validateScale(decimalType, scale);
    }
    
    /**
     * Gets the storage requirements for a decimal column.
     * 
     * @param decimalType decimal type
     * @param rowCount number of rows
     * @return storage bytes required
     */
    public static long getStorageSize(short decimalType, long rowCount) {
        short storageType = DecimalColumnType.getStorageType(decimalType);
        
        switch (storageType) {
            case ColumnType.INT:
                return rowCount * Integer.BYTES;
            case ColumnType.LONG:
                return rowCount * Long.BYTES;
            case ColumnType.LONG128:
                return rowCount * 16; // 128 bits = 16 bytes
            case ColumnType.LONG256:
                return rowCount * 32; // 256 bits = 32 bytes
            default:
                throw new UnsupportedOperationException("Unknown storage type: " + storageType);
        }
    }
    
    /**
     * Parses decimal type specification from string (e.g., "DECIMAL18(4)").
     * 
     * @param typeSpec type specification string
     * @return array with [decimalType, scale]
     */
    public static int[] parseDecimalSpec(String typeSpec) {
        if (typeSpec == null || typeSpec.isEmpty()) {
            throw new IllegalArgumentException("Empty decimal type specification");
        }
        
        String upperSpec = typeSpec.toUpperCase().trim();
        
        // Handle DECIMAL9(scale) format
        if (upperSpec.startsWith("DECIMAL9(") && upperSpec.endsWith(")")) {
            int scale = parseScale(upperSpec, "DECIMAL9(".length());
            return new int[]{DecimalColumnType.DECIMAL9, scale};
        }
        
        // Handle DECIMAL18(scale) format  
        if (upperSpec.startsWith("DECIMAL18(") && upperSpec.endsWith(")")) {
            int scale = parseScale(upperSpec, "DECIMAL18(".length());
            return new int[]{DecimalColumnType.DECIMAL18, scale};
        }
        
        // Handle DECIMAL38(scale) format
        if (upperSpec.startsWith("DECIMAL38(") && upperSpec.endsWith(")")) {
            int scale = parseScale(upperSpec, "DECIMAL38(".length());
            return new int[]{DecimalColumnType.DECIMAL38, scale};
        }
        
        // Handle DECIMAL77(scale) format
        if (upperSpec.startsWith("DECIMAL77(") && upperSpec.endsWith(")")) {
            int scale = parseScale(upperSpec, "DECIMAL77(".length());
            return new int[]{DecimalColumnType.DECIMAL77, scale};
        }
        
        throw new IllegalArgumentException("Invalid decimal type specification: " + typeSpec);
    }
    
    private static int parseScale(String spec, int startPos) {
        try {
            String scaleStr = spec.substring(startPos, spec.length() - 1).trim();
            return Integer.parseInt(scaleStr);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid scale in decimal specification: " + spec, e);
        }
    }
    
    /**
     * Creates a decimal column from string specification.
     * 
     * @param typeSpec decimal type specification (e.g., "DECIMAL18(4)")
     * @return decimal column instance
     */
    public static DecimalColumn createFromSpec(String typeSpec) {
        int[] spec = parseDecimalSpec(typeSpec);
        return createColumn((short) spec[0], spec[1]);
    }
}