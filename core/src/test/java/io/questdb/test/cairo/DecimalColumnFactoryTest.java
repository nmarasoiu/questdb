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

import io.questdb.cairo.DecimalColumn;
import io.questdb.cairo.DecimalColumnFactory;
import io.questdb.cairo.DecimalColumnType;
import io.questdb.cairo.Decimal9Column;
import io.questdb.cairo.Decimal18Column;
import org.junit.Assert;
import org.junit.Test;

public class DecimalColumnFactoryTest {

    @Test
    public void testCreateColumn() {
        DecimalColumn column9 = DecimalColumnFactory.createColumn(DecimalColumnType.DECIMAL9, 4);
        Assert.assertTrue(column9 instanceof Decimal9Column);
        Assert.assertEquals(4, column9.getScale());
        
        DecimalColumn column18 = DecimalColumnFactory.createColumn(DecimalColumnType.DECIMAL18, 6);
        Assert.assertTrue(column18 instanceof Decimal18Column);
        Assert.assertEquals(6, column18.getScale());
    }
    
    @Test
    public void testCreateColumnFromEncodedType() {
        int encodedType = DecimalColumnType.createType(DecimalColumnType.DECIMAL18, 4);
        DecimalColumn column = DecimalColumnFactory.createColumn(encodedType);
        
        Assert.assertTrue(column instanceof Decimal18Column);
        Assert.assertEquals(4, column.getScale());
        Assert.assertEquals(DecimalColumnType.DECIMAL18, column.getDecimalType());
    }
    
    @Test
    public void testRecommendDecimalType() {
        Assert.assertEquals(DecimalColumnType.DECIMAL9, DecimalColumnFactory.recommendDecimalType(9));
        Assert.assertEquals(DecimalColumnType.DECIMAL9, DecimalColumnFactory.recommendDecimalType(5));
        
        Assert.assertEquals(DecimalColumnType.DECIMAL18, DecimalColumnFactory.recommendDecimalType(18));
        Assert.assertEquals(DecimalColumnType.DECIMAL18, DecimalColumnFactory.recommendDecimalType(15));
        
        Assert.assertEquals(DecimalColumnType.DECIMAL38, DecimalColumnFactory.recommendDecimalType(38));
        Assert.assertEquals(DecimalColumnType.DECIMAL38, DecimalColumnFactory.recommendDecimalType(25));
        
        Assert.assertEquals(DecimalColumnType.DECIMAL77, DecimalColumnFactory.recommendDecimalType(77));
        Assert.assertEquals(DecimalColumnType.DECIMAL77, DecimalColumnFactory.recommendDecimalType(50));
    }
    
    @Test
    public void testRecommendDecimalTypeInvalid() {
        try {
            DecimalColumnFactory.recommendDecimalType(78);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum supported precision"));
        }
        
        try {
            DecimalColumnFactory.recommendDecimalType(0);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum supported precision"));
        }
    }
    
    @Test
    public void testCreateOptimalColumn() {
        DecimalColumn column = DecimalColumnFactory.createOptimalColumn(9, 4);
        Assert.assertTrue(column instanceof Decimal9Column);
        Assert.assertEquals(4, column.getScale());
        
        column = DecimalColumnFactory.createOptimalColumn(15, 6);
        Assert.assertTrue(column instanceof Decimal18Column);
        Assert.assertEquals(6, column.getScale());
    }
    
    @Test
    public void testValidateDecimalSpec() {
        // Valid specifications
        DecimalColumnFactory.validateDecimalSpec(DecimalColumnType.DECIMAL9, 4);
        DecimalColumnFactory.validateDecimalSpec(DecimalColumnType.DECIMAL18, 18);
        DecimalColumnFactory.validateDecimalSpec(DecimalColumnType.DECIMAL38, 0);
        
        // Invalid decimal type
        try {
            DecimalColumnFactory.validateDecimalSpec((short) 999, 4);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid decimal type"));
        }
        
        // Negative scale
        try {
            DecimalColumnFactory.validateDecimalSpec(DecimalColumnType.DECIMAL18, -1);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("cannot be negative"));
        }
        
        // Scale exceeds precision
        try {
            DecimalColumnFactory.validateDecimalSpec(DecimalColumnType.DECIMAL9, 10);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum precision"));
        }
    }
    
    @Test
    public void testGetStorageSize() {
        Assert.assertEquals(1000L * Integer.BYTES, DecimalColumnFactory.getStorageSize(DecimalColumnType.DECIMAL9, 1000L));
        Assert.assertEquals(500L * Long.BYTES, DecimalColumnFactory.getStorageSize(DecimalColumnType.DECIMAL18, 500L));
        Assert.assertEquals(100L * 16L, DecimalColumnFactory.getStorageSize(DecimalColumnType.DECIMAL38, 100L));
        Assert.assertEquals(50L * 32L, DecimalColumnFactory.getStorageSize(DecimalColumnType.DECIMAL77, 50L));
    }
    
    @Test
    public void testParseDecimalSpec() {
        int[] spec = DecimalColumnFactory.parseDecimalSpec("DECIMAL9(4)");
        Assert.assertEquals(DecimalColumnType.DECIMAL9, spec[0]);
        Assert.assertEquals(4, spec[1]);
        
        spec = DecimalColumnFactory.parseDecimalSpec("DECIMAL18(12)");
        Assert.assertEquals(DecimalColumnType.DECIMAL18, spec[0]);
        Assert.assertEquals(12, spec[1]);
        
        spec = DecimalColumnFactory.parseDecimalSpec("DECIMAL38(0)");
        Assert.assertEquals(DecimalColumnType.DECIMAL38, spec[0]);
        Assert.assertEquals(0, spec[1]);
        
        spec = DecimalColumnFactory.parseDecimalSpec("DECIMAL77(25)");
        Assert.assertEquals(DecimalColumnType.DECIMAL77, spec[0]);
        Assert.assertEquals(25, spec[1]);
    }
    
    @Test
    public void testParseDecimalSpecCaseInsensitive() {
        int[] spec = DecimalColumnFactory.parseDecimalSpec("decimal9(4)");
        Assert.assertEquals(DecimalColumnType.DECIMAL9, spec[0]);
        Assert.assertEquals(4, spec[1]);
        
        spec = DecimalColumnFactory.parseDecimalSpec("Decimal18(8)");
        Assert.assertEquals(DecimalColumnType.DECIMAL18, spec[0]);
        Assert.assertEquals(8, spec[1]);
    }
    
    @Test
    public void testParseDecimalSpecWhitespace() {
        int[] spec = DecimalColumnFactory.parseDecimalSpec("  DECIMAL18(4)  ");
        Assert.assertEquals(DecimalColumnType.DECIMAL18, spec[0]);
        Assert.assertEquals(4, spec[1]);
    }
    
    @Test
    public void testParseDecimalSpecInvalid() {
        // Empty string
        try {
            DecimalColumnFactory.parseDecimalSpec("");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Empty decimal type"));
        }
        
        // Null
        try {
            DecimalColumnFactory.parseDecimalSpec(null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Empty decimal type"));
        }
        
        // Invalid format
        try {
            DecimalColumnFactory.parseDecimalSpec("DECIMAL18");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid decimal type"));
        }
        
        // Missing closing parenthesis
        try {
            DecimalColumnFactory.parseDecimalSpec("DECIMAL18(4");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid decimal type"));
        }
        
        // Invalid scale
        try {
            DecimalColumnFactory.parseDecimalSpec("DECIMAL18(abc)");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid scale"));
        }
        
        // Unknown decimal type
        try {
            DecimalColumnFactory.parseDecimalSpec("DECIMAL99(4)");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid decimal type"));
        }
    }
    
    @Test
    public void testCreateFromSpec() {
        DecimalColumn column = DecimalColumnFactory.createFromSpec("DECIMAL9(4)");
        Assert.assertTrue(column instanceof Decimal9Column);
        Assert.assertEquals(4, column.getScale());
        
        column = DecimalColumnFactory.createFromSpec("DECIMAL18(8)");
        Assert.assertTrue(column instanceof Decimal18Column);
        Assert.assertEquals(8, column.getScale());
    }
    
    @Test
    public void testCreateUnsupportedTypes() {
        // DECIMAL38 and DECIMAL77 should throw UnsupportedOperationException
        try {
            DecimalColumnFactory.createColumn(DecimalColumnType.DECIMAL38, 10);
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            Assert.assertTrue(e.getMessage().contains("DECIMAL38 not yet implemented"));
        }
        
        try {
            DecimalColumnFactory.createColumn(DecimalColumnType.DECIMAL77, 20);
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            Assert.assertTrue(e.getMessage().contains("DECIMAL77 not yet implemented"));
        }
    }
    
    @Test
    public void testCreateFromEncodedNonDecimalType() {
        try {
            DecimalColumnFactory.createColumn(42); // Some non-decimal type
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Not a decimal column type"));
        }
    }
    
    @Test
    public void testBoundaryPrecisions() {
        // Test boundary precision recommendations
        Assert.assertEquals(DecimalColumnType.DECIMAL9, DecimalColumnFactory.recommendDecimalType(1));
        Assert.assertEquals(DecimalColumnType.DECIMAL18, DecimalColumnFactory.recommendDecimalType(10));
        Assert.assertEquals(DecimalColumnType.DECIMAL38, DecimalColumnFactory.recommendDecimalType(19));
        Assert.assertEquals(DecimalColumnType.DECIMAL77, DecimalColumnFactory.recommendDecimalType(39));
    }
}