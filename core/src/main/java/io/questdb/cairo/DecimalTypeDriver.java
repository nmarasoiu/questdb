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
 * Type driver for decimal column types. Since decimal types are stored as 
 * integers, this driver is mainly for decimal-specific metadata and operations.
 * For simplicity, decimal columns don't need a complex type driver since they
 * use fixed-size storage that maps directly to existing integer types.
 */
public class DecimalTypeDriver {
    
    public static final DecimalTypeDriver INSTANCE = new DecimalTypeDriver();
    
    private DecimalTypeDriver() {
    }
    
    /**
     * Checks if this driver handles the given column type.
     * 
     * @param columnType column type to check
     * @return true if this driver handles decimal types
     */
    public boolean handlesType(int columnType) {
        return DecimalColumnType.isDecimal(columnType);
    }
    
    /**
     * Gets the appropriate decimal column instance for the given type.
     * 
     * @param columnType encoded decimal column type
     * @return decimal column instance
     */
    public DecimalColumn getDecimalColumn(int columnType) {
        if (!DecimalColumnType.isDecimal(columnType)) {
            throw new IllegalArgumentException("Not a decimal type: " + columnType);
        }
        
        short decimalType = DecimalColumnType.getDecimalType(columnType);
        int scale = DecimalColumnType.getScale(columnType);
        
        switch (decimalType) {
            case DecimalColumnType.DECIMAL9:
                return new Decimal9Column(scale);
            case DecimalColumnType.DECIMAL18:
                return new Decimal18Column(scale);
            case DecimalColumnType.DECIMAL38:
                // TODO: Implement Decimal38Column for LONG128 storage
                throw new UnsupportedOperationException("DECIMAL38 not yet implemented");
            case DecimalColumnType.DECIMAL77:
                // TODO: Implement Decimal77Column for LONG256 storage  
                throw new UnsupportedOperationException("DECIMAL77 not yet implemented");
            default:
                throw new IllegalArgumentException("Unknown decimal type: " + decimalType);
        }
    }
    
    /**
     * Gets the underlying storage type driver for a decimal type.
     * 
     * @param columnType decimal column type
     * @return underlying storage type driver
     */
    public ColumnTypeDriver getStorageDriver(int columnType) {
        short storageType = DecimalColumnType.getStorageType(DecimalColumnType.getDecimalType(columnType));
        
        // For now, return null as storage operations are handled directly
        // In a full implementation, this would delegate to appropriate type drivers
        return null;
    }
    
    /**
     * Formats a decimal column type for display.
     * 
     * @param columnType encoded decimal column type
     * @return formatted type name (e.g., "DECIMAL18(4)")
     */
    public String formatTypeName(int columnType) {
        return DecimalColumnType.getFullTypeName(columnType);
    }
}