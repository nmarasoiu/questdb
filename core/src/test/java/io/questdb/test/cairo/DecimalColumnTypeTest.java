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

package io.questdb.test.cairo;

import io.questdb.cairo.ColumnType;
import io.questdb.cairo.DecimalColumnType;
import org.junit.Assert;
import org.junit.Test;

public class DecimalColumnTypeTest {

    @Test
    public void testCreateType() {
        int decimalType = DecimalColumnType.createType(DecimalColumnType.DECIMAL18, 4);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(decimalType));
        Assert.assertEquals(DecimalColumnType.DECIMAL18, DecimalColumnType.getDecimalType(decimalType));
        Assert.assertEquals(4, DecimalColumnType.getScale(decimalType));
    }
    
    @Test
    public void testDecimalTypeConstants() {
        Assert.assertTrue(DecimalColumnType.isDecimalType(DecimalColumnType.DECIMAL9));
        Assert.assertTrue(DecimalColumnType.isDecimalType(DecimalColumnType.DECIMAL18));
        Assert.assertTrue(DecimalColumnType.isDecimalType(DecimalColumnType.DECIMAL38));
        Assert.assertTrue(DecimalColumnType.isDecimalType(DecimalColumnType.DECIMAL77));
        
        Assert.assertFalse(DecimalColumnType.isDecimalType(ColumnType.INT));
        Assert.assertFalse(DecimalColumnType.isDecimalType(ColumnType.LONG));
    }
    
    @Test
    public void testStorageTypeMapping() {
        Assert.assertEquals(ColumnType.INT, DecimalColumnType.getStorageType(DecimalColumnType.DECIMAL9));
        Assert.assertEquals(ColumnType.LONG, DecimalColumnType.getStorageType(DecimalColumnType.DECIMAL18));
        Assert.assertEquals(ColumnType.LONG128, DecimalColumnType.getStorageType(DecimalColumnType.DECIMAL38));
        Assert.assertEquals(ColumnType.LONG256, DecimalColumnType.getStorageType(DecimalColumnType.DECIMAL77));
    }
    
    @Test
    public void testMaxPrecision() {
        Assert.assertEquals(9, DecimalColumnType.getMaxPrecision(DecimalColumnType.DECIMAL9));
        Assert.assertEquals(18, DecimalColumnType.getMaxPrecision(DecimalColumnType.DECIMAL18));
        Assert.assertEquals(38, DecimalColumnType.getMaxPrecision(DecimalColumnType.DECIMAL38));
        Assert.assertEquals(77, DecimalColumnType.getMaxPrecision(DecimalColumnType.DECIMAL77));
    }
    
    @Test
    public void testScaleValidation() {
        // Valid scales
        DecimalColumnType.validateScale(DecimalColumnType.DECIMAL9, 0);
        DecimalColumnType.validateScale(DecimalColumnType.DECIMAL9, 9);
        DecimalColumnType.validateScale(DecimalColumnType.DECIMAL18, 18);
        DecimalColumnType.validateScale(DecimalColumnType.DECIMAL38, 38);
        DecimalColumnType.validateScale(DecimalColumnType.DECIMAL77, 77);
        
        // Invalid scales
        try {
            DecimalColumnType.validateScale(DecimalColumnType.DECIMAL9, 10);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum precision"));
        }
        
        try {
            DecimalColumnType.validateScale(DecimalColumnType.DECIMAL18, 19);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum precision"));
        }
    }
    
    @Test
    public void testTypeNames() {
        Assert.assertEquals("DECIMAL9", DecimalColumnType.getTypeName(DecimalColumnType.DECIMAL9));
        Assert.assertEquals("DECIMAL18", DecimalColumnType.getTypeName(DecimalColumnType.DECIMAL18));
        Assert.assertEquals("DECIMAL38", DecimalColumnType.getTypeName(DecimalColumnType.DECIMAL38));
        Assert.assertEquals("DECIMAL77", DecimalColumnType.getTypeName(DecimalColumnType.DECIMAL77));
    }
    
    @Test
    public void testFullTypeNames() {
        int decimal18_4 = DecimalColumnType.createType(DecimalColumnType.DECIMAL18, 4);
        Assert.assertEquals("DECIMAL18(4)", DecimalColumnType.getFullTypeName(decimal18_4));
        
        int decimal9_2 = DecimalColumnType.createType(DecimalColumnType.DECIMAL9, 2);
        Assert.assertEquals("DECIMAL9(2)", DecimalColumnType.getFullTypeName(decimal9_2));
        
        int decimal38_10 = DecimalColumnType.createType(DecimalColumnType.DECIMAL38, 10);
        Assert.assertEquals("DECIMAL38(10)", DecimalColumnType.getFullTypeName(decimal38_10));
    }
    
    @Test
    public void testScaleRange() {
        // Test boundary values
        DecimalColumnType.createType(DecimalColumnType.DECIMAL18, 0);
        DecimalColumnType.createType(DecimalColumnType.DECIMAL18, 18);
        
        // Test invalid scale values
        try {
            DecimalColumnType.createType(DecimalColumnType.DECIMAL18, -1);
            Assert.fail("Expected IllegalArgumentException for negative scale");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("must be between 0 and"));
        }
        
        try {
            DecimalColumnType.createType(DecimalColumnType.DECIMAL18, 256);
            Assert.fail("Expected IllegalArgumentException for scale > 255");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("must be between 0 and"));
        }
    }
    
    @Test
    public void testInvalidDecimalType() {
        try {
            DecimalColumnType.createType((short) 999, 4);
            Assert.fail("Expected IllegalArgumentException for invalid decimal type");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid decimal type"));
        }
    }
    
    @Test
    public void testScaleExtraction() {
        // Test various scale values
        for (int scale = 0; scale <= 18; scale++) {
            int columnType = DecimalColumnType.createType(DecimalColumnType.DECIMAL18, scale);
            Assert.assertEquals(scale, DecimalColumnType.getScale(columnType));
        }
    }
    
    @Test
    public void testNonDecimalTypes() {
        Assert.assertFalse(DecimalColumnType.isDecimal(ColumnType.INT));
        Assert.assertFalse(DecimalColumnType.isDecimal(ColumnType.LONG));
        Assert.assertFalse(DecimalColumnType.isDecimal(ColumnType.STRING));
        Assert.assertFalse(DecimalColumnType.isDecimal(ColumnType.SYMBOL));
        
        Assert.assertEquals(0, DecimalColumnType.getScale(ColumnType.INT));
        Assert.assertEquals(0, DecimalColumnType.getScale(ColumnType.LONG));
    }
}