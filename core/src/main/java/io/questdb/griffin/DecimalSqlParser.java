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

package io.questdb.griffin;

import io.questdb.cairo.DecimalColumnType;
import io.questdb.std.Chars;
import io.questdb.std.GenericLexer;
import io.questdb.std.Numbers;
import io.questdb.std.NumericException;

/**
 * SQL parser extensions for decimal type support.
 * Provides utilities for parsing DECIMAL_TYPE(scale) syntax and integrating
 * with existing QuestDB SQL parsing infrastructure.
 */
public class DecimalSqlParser {
    
    private DecimalSqlParser() {
    }
    
    /**
     * Helper method to get next token - in actual integration this would be part of SqlParser.
     */
    private static CharSequence getNextToken(GenericLexer lexer) throws SqlException {
        // This is a placeholder - in actual integration, this would call SqlParser.optTok()
        // For now, we use lexer.next() which may throw if no token available
        return lexer.hasNext() ? lexer.next() : null;
    }
    
    /**
     * Enhanced type parsing that supports decimal types with scale parameters.
     * Extends the existing toColumnType logic to handle DECIMAL9(4) syntax.
     * 
     * @param lexer SQL lexer
     * @param tok type token (e.g., "DECIMAL18")
     * @return encoded column type with decimal flag and scale
     * @throws SqlException if parsing fails
     */
    public static int parseDecimalType(GenericLexer lexer, CharSequence tok) throws SqlException {
        int typePosition = lexer.lastTokenPosition();
        
        // Check if this is a decimal type
        short decimalType = parseDecimalTypeTag(tok);
        if (decimalType == -1) {
            // Not a decimal type, delegate to standard parsing
            throw SqlException.$(typePosition, "unsupported column type: ").put(tok);
        }
        
        // Parse scale parameter: DECIMAL18(4) 
        // Note: This would need to be integrated into SqlParser.optTok() method
        CharSequence next = getNextToken(lexer);
        if (next == null || !Chars.equals(next, '(')) {
            throw SqlException.$(lexer.getPosition(), "scale parameter expected for ").put(tok).put(", e.g., ").put(tok).put("(4)");
        }
        
        CharSequence scaleToken = getNextToken(lexer);
        if (scaleToken == null) {
            throw SqlException.$(lexer.getPosition(), "scale value expected");
        }
        
        int scale;
        try {
            scale = Numbers.parseInt(scaleToken);
        } catch (NumericException e) {
            throw SqlException.$(lexer.lastTokenPosition(), "invalid scale value: ").put(scaleToken);
        }
        
        if (scale < 0) {
            throw SqlException.$(lexer.lastTokenPosition(), "scale cannot be negative: ").put(scale);
        }
        
        // Validate scale for this decimal type
        try {
            DecimalColumnType.validateScale(decimalType, scale);
        } catch (IllegalArgumentException e) {
            throw SqlException.$(lexer.lastTokenPosition(), e.getMessage());
        }
        
        CharSequence closeParen = getNextToken(lexer);
        if (closeParen == null || !Chars.equals(closeParen, ')')) {
            throw SqlException.$(lexer.getPosition(), "')' expected after scale parameter");
        }
        
        // Create encoded decimal type
        return DecimalColumnType.createType(decimalType, scale);
    }
    
    /**
     * Parses a decimal type name to its type tag.
     * 
     * @param tok type token to parse
     * @return decimal type tag or -1 if not a decimal type
     */
    private static short parseDecimalTypeTag(CharSequence tok) {
        if (Chars.equalsIgnoreCase(tok, "DECIMAL9")) {
            return DecimalColumnType.DECIMAL9;
        }
        if (Chars.equalsIgnoreCase(tok, "DECIMAL18")) {
            return DecimalColumnType.DECIMAL18;
        }
        if (Chars.equalsIgnoreCase(tok, "DECIMAL38")) {
            return DecimalColumnType.DECIMAL38;
        }
        if (Chars.equalsIgnoreCase(tok, "DECIMAL77")) {
            return DecimalColumnType.DECIMAL77;
        }
        
        // Also support generic DECIMAL syntax - recommend appropriate type
        if (Chars.equalsIgnoreCase(tok, "DECIMAL")) {
            // Default to DECIMAL18 for generic DECIMAL
            return DecimalColumnType.DECIMAL18;
        }
        
        return -1; // Not a decimal type
    }
    
    /**
     * Integration point for SqlParser.toColumnType() method.
     * This shows how to modify the existing parser to support decimal types.
     */
    public static class SqlParserIntegration {
        
        /**
         * Enhanced version of SqlParser.toColumnType() that supports decimal types.
         * This is how the existing method should be modified.
         */
        public static int toColumnTypeWithDecimals(GenericLexer lexer, CharSequence tok) throws SqlException {
            int typePosition = lexer.lastTokenPosition();
            
            // Handle array syntax first (existing logic)
            if (Chars.equalsNc(tok, '[')) {
                // ... existing array handling logic ...
                throw SqlException.position(typePosition).put("column type is expected here");
            }
            
            // Try decimal type parsing first
            try {
                return parseDecimalType(lexer, tok);
            } catch (SqlException e) {
                // If not a decimal type, continue with standard parsing
                if (!isDecimalParsingError(e)) {
                    throw e;
                }
            }
            
            // Standard type parsing (existing logic)
            final short typeTag = SqlUtil.toPersistedTypeTag(tok, typePosition);
            final int typeTagPosition = lexer.lastTokenPosition();
            
            // Handle precision keyword for DOUBLE (existing logic)
            if (typeTag == io.questdb.cairo.ColumnType.DOUBLE) {
                CharSequence next = getNextToken(lexer);
                if (next != null && !isPrecisionKeyword(next)) {
                    // lexer.unparseLast(); // Would need to implement unparse functionality
                }
            }
            
            // Handle array dimensions (existing logic)
            int nDims = SqlUtil.parseArrayDimensionality(lexer, typeTag, typeTagPosition);
            if (nDims > 0) {
                // ... existing array logic ...
                return io.questdb.cairo.ColumnType.encodeArrayType(typeTag, nDims);
            }
            
            return typeTag;
        }
        
        private static boolean isDecimalParsingError(SqlException e) {
            // Check if this is a decimal-specific parsing error vs general error
            String message = e.getMessage();
            return message != null && (
                message.contains("scale parameter expected") ||
                message.contains("scale value expected") ||
                message.contains("invalid scale value")
            );
        }
        
        private static boolean isPrecisionKeyword(CharSequence token) {
            return Chars.equalsIgnoreCase(token, "precision");
        }
    }
    
    /**
     * Utility to add decimal type names to ColumnType name maps.
     * This shows what needs to be added to ColumnType static initialization.
     */
    public static class ColumnTypeIntegration {
        
        /**
         * Additions needed in ColumnType static initialization block.
         */
        public static void initializeDecimalTypes() {
            // Add to typeNameMap
            // typeNameMap.put(DecimalColumnType.DECIMAL9, "DECIMAL9");
            // typeNameMap.put(DecimalColumnType.DECIMAL18, "DECIMAL18");
            // typeNameMap.put(DecimalColumnType.DECIMAL38, "DECIMAL38");
            // typeNameMap.put(DecimalColumnType.DECIMAL77, "DECIMAL77");
            
            // Add to nameTypeMap
            // nameTypeMap.put("decimal9", DecimalColumnType.DECIMAL9);
            // nameTypeMap.put("decimal18", DecimalColumnType.DECIMAL18);
            // nameTypeMap.put("decimal38", DecimalColumnType.DECIMAL38);
            // nameTypeMap.put("decimal77", DecimalColumnType.DECIMAL77);
            // nameTypeMap.put("decimal", DecimalColumnType.DECIMAL18); // Default
            
            // Add to TYPE_SIZE and TYPE_SIZE_POW2 arrays
            // TYPE_SIZE[DecimalColumnType.DECIMAL9] = Integer.BYTES;
            // TYPE_SIZE[DecimalColumnType.DECIMAL18] = Long.BYTES;
            // TYPE_SIZE[DecimalColumnType.DECIMAL38] = 16; // LONG128
            // TYPE_SIZE[DecimalColumnType.DECIMAL77] = 32; // LONG256
            
            // Add power of 2 sizes
            // TYPE_SIZE_POW2[DecimalColumnType.DECIMAL9] = 2;  // 4 bytes = 2^2
            // TYPE_SIZE_POW2[DecimalColumnType.DECIMAL18] = 3; // 8 bytes = 2^3
            // TYPE_SIZE_POW2[DecimalColumnType.DECIMAL38] = 4; // 16 bytes = 2^4
            // TYPE_SIZE_POW2[DecimalColumnType.DECIMAL77] = 5; // 32 bytes = 2^5
        }
    }
    
    /**
     * Example usage in CREATE TABLE statements.
     */
    public static class UsageExamples {
        
        /*
        -- Financial data table with decimal types
        CREATE TABLE trades (
            id LONG,
            symbol SYMBOL,
            price DECIMAL18(4),        -- Max ±999 trillion with 4 decimal places
            volume DECIMAL18(0),       -- Whole number shares
            commission DECIMAL9(6),    -- Small fees with 6 decimal precision
            timestamp TIMESTAMP
        ) TIMESTAMP(timestamp) PARTITION BY DAY;
        
        -- Aggregation queries work with automatic scale handling
        SELECT 
            symbol,
            AVG(price) as avg_price,      -- Result scale preserved
            SUM(volume) as total_volume,  -- Scale = 0
            MAX(commission) as max_fee    -- Scale = 6
        FROM trades 
        WHERE timestamp > '2024-01-01'
        SAMPLE BY 1h;
        
        -- Arithmetic operations with scale alignment
        SELECT 
            symbol,
            price * volume as notional,           -- Scale = 4 + 0 = 4
            price + commission as total_cost,     -- Scale = max(4, 6) = 6
            commission / price as fee_rate        -- Scale = 6 - 4 + extra = variable
        FROM trades;
        */
    }
}