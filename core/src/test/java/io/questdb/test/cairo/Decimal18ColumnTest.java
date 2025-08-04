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

import io.questdb.cairo.Decimal18Column;
import io.questdb.cairo.DecimalColumnType;
import org.junit.Assert;
import org.junit.Test;

public class Decimal18ColumnTest {

    @Test
    public void testCreateColumn() {
        Decimal18Column column = new Decimal18Column(4);
        Assert.assertEquals(4, column.getScale());
        Assert.assertEquals(DecimalColumnType.DECIMAL18, column.getDecimalType());
        Assert.assertEquals(10000L, column.getScaleFactor());
    }
    
    @Test
    public void testParseDecimal() {
        Decimal18Column column = new Decimal18Column(4);
        
        Assert.assertEquals(1234567L, column.parseDecimal("123.4567"));
        Assert.assertEquals(1000000L, column.parseDecimal("100.0000"));
        Assert.assertEquals(1L, column.parseDecimal("0.0001"));
        Assert.assertEquals(0L, column.parseDecimal("0"));
        Assert.assertEquals(-1234567L, column.parseDecimal("-123.4567"));
    }
    
    @Test
    public void testFormatDecimal() {
        Decimal18Column column = new Decimal18Column(4);
        
        Assert.assertEquals("123.4567", column.formatDecimal(1234567L));
        Assert.assertEquals("100", column.formatDecimal(1000000L));
        Assert.assertEquals("0.0001", column.formatDecimal(1L));
        Assert.assertEquals("0", column.formatDecimal(0L));
        Assert.assertEquals("-123.4567", column.formatDecimal(-1234567L));
    }
    
    @Test
    public void testZeroScale() {
        Decimal18Column column = new Decimal18Column(0);
        
        Assert.assertEquals(123L, column.parseDecimal("123"));
        Assert.assertEquals("123", column.formatDecimal(123L));
        Assert.assertEquals(1L, column.getScaleFactor());
    }
    
    @Test
    public void testAdd() {
        Decimal18Column column = new Decimal18Column(2);
        
        long value1 = column.parseDecimal("123.45"); // 12345
        long value2 = column.parseDecimal("67.89");  // 6789
        long result = column.add(value1, value2);     // 19134
        
        Assert.assertEquals("191.34", column.formatDecimal(result));
    }
    
    @Test
    public void testSubtract() {
        Decimal18Column column = new Decimal18Column(2);
        
        long value1 = column.parseDecimal("123.45"); // 12345
        long value2 = column.parseDecimal("67.89");  // 6789
        long result = column.subtract(value1, value2); // 5556
        
        Assert.assertEquals("55.56", column.formatDecimal(result));
    }
    
    @Test
    public void testMultiply() {
        Decimal18Column column = new Decimal18Column(2);
        
        long value1 = column.parseDecimal("12.34"); // 1234
        long value2 = column.parseDecimal("5.67");  // 567
        long result = column.multiply(value1, value2);
        
        Assert.assertEquals("69.97", column.formatDecimal(result));
    }
    
    @Test
    public void testDivide() {
        Decimal18Column column = new Decimal18Column(2);
        
        long dividend = column.parseDecimal("100.00"); // 10000
        long divisor = column.parseDecimal("4.00");    // 400
        long result = column.divide(dividend, divisor);
        
        Assert.assertEquals("25", column.formatDecimal(result));
    }
    
    @Test
    public void testDivideByZero() {
        Decimal18Column column = new Decimal18Column(2);
        
        try {
            column.divide(100L, 0L);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertEquals("Division by zero", e.getMessage());
        }
    }
    
    @Test
    public void testCompare() {
        Decimal18Column column = new Decimal18Column(2);
        
        long value1 = column.parseDecimal("123.45");
        long value2 = column.parseDecimal("67.89");
        long value3 = column.parseDecimal("123.45");
        
        Assert.assertTrue(column.compare(value1, value2) > 0);
        Assert.assertTrue(column.compare(value2, value1) < 0);
        Assert.assertEquals(0, column.compare(value1, value3));
    }
    
    @Test
    public void testAdditionOverflow() {
        Decimal18Column column = new Decimal18Column(0);
        
        try {
            column.add(Long.MAX_VALUE, 1L);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testSubtractionOverflow() {
        Decimal18Column column = new Decimal18Column(0);
        
        try {
            column.subtract(Long.MIN_VALUE, 1L);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testValidValue() {
        Decimal18Column column = new Decimal18Column(4);
        
        Assert.assertTrue(column.isValidValue(0L));
        Assert.assertTrue(column.isValidValue(Long.MAX_VALUE));
        Assert.assertTrue(column.isValidValue(Long.MIN_VALUE));
    }
    
    @Test
    public void testMaxScale() {
        new Decimal18Column(18); // Should work
        
        try {
            new Decimal18Column(19);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum"));
        }
    }
    
    @Test
    public void testRounding() {
        Decimal18Column column = new Decimal18Column(2);
        
        // Test half-up rounding in parsing
        Assert.assertEquals(1235L, column.parseDecimal("12.345")); // rounds to 12.35
        Assert.assertEquals(1234L, column.parseDecimal("12.344")); // rounds to 12.34
    }
    
    @Test
    public void testLargePrecisionValues() {
        Decimal18Column column = new Decimal18Column(4);
        
        // Test maximum safe values
        long maxSafe = 922337203685477L; // Just under Long.MAX_VALUE / 10000
        String maxSafeStr = column.formatDecimal(maxSafe);
        Assert.assertEquals(maxSafe, column.parseDecimal(maxSafeStr));
        
        long minSafe = -922337203685477L;
        String minSafeStr = column.formatDecimal(minSafe);
        Assert.assertEquals(minSafe, column.parseDecimal(minSafeStr));
    }
    
    @Test
    public void testToString() {
        Decimal18Column column = new Decimal18Column(4);
        Assert.assertEquals("DECIMAL18(4)", column.toString());
        
        Decimal18Column column2 = new Decimal18Column(0);
        Assert.assertEquals("DECIMAL18(0)", column2.toString());
    }
    
    @Test
    public void testFactoryMethod() {
        Decimal18Column column = Decimal18Column.create(6);
        Assert.assertEquals(6, column.getScale());
        Assert.assertEquals(1000000L, column.getScaleFactor());
    }
    
    @Test
    public void testMinMaxValues() {
        Decimal18Column column = new Decimal18Column(2);
        
        String minValue = column.getMinValue();
        String maxValue = column.getMaxValue();
        
        Assert.assertNotNull(minValue);
        Assert.assertNotNull(maxValue);
        Assert.assertTrue(minValue.startsWith("-"));
        Assert.assertFalse(maxValue.startsWith("-"));
    }
}