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

import io.questdb.cairo.Decimal9Column;
import io.questdb.cairo.DecimalColumnType;
import org.junit.Assert;
import org.junit.Test;

public class Decimal9ColumnTest {

    @Test
    public void testCreateColumn() {
        Decimal9Column column = new Decimal9Column(4);
        Assert.assertEquals(4, column.getScale());
        Assert.assertEquals(DecimalColumnType.DECIMAL9, column.getDecimalType());
        Assert.assertEquals(10000L, column.getScaleFactor());
    }
    
    @Test
    public void testParseDecimal() {
        Decimal9Column column = new Decimal9Column(4);
        
        Assert.assertEquals(1234567, column.parseDecimal("123.4567"));
        Assert.assertEquals(1000000, column.parseDecimal("100.0000"));
        Assert.assertEquals(1, column.parseDecimal("0.0001"));
        Assert.assertEquals(0, column.parseDecimal("0"));
        Assert.assertEquals(-1234567, column.parseDecimal("-123.4567"));
    }
    
    @Test
    public void testFormatDecimal() {
        Decimal9Column column = new Decimal9Column(4);
        
        Assert.assertEquals("123.4567", column.formatDecimal(1234567));
        Assert.assertEquals("100", column.formatDecimal(1000000));
        Assert.assertEquals("0.0001", column.formatDecimal(1));
        Assert.assertEquals("0", column.formatDecimal(0));
        Assert.assertEquals("-123.4567", column.formatDecimal(-1234567));
    }
    
    @Test
    public void testZeroScale() {
        Decimal9Column column = new Decimal9Column(0);
        
        Assert.assertEquals(123, column.parseDecimal("123"));
        Assert.assertEquals("123", column.formatDecimal(123));
        Assert.assertEquals(1L, column.getScaleFactor());
    }
    
    @Test
    public void testAdd() {
        Decimal9Column column = new Decimal9Column(2);
        
        int value1 = column.parseDecimal("123.45"); // 12345
        int value2 = column.parseDecimal("67.89");  // 6789
        int result = column.add(value1, value2);     // 19134
        
        Assert.assertEquals("191.34", column.formatDecimal(result));
    }
    
    @Test
    public void testSubtract() {
        Decimal9Column column = new Decimal9Column(2);
        
        int value1 = column.parseDecimal("123.45"); // 12345
        int value2 = column.parseDecimal("67.89");  // 6789
        int result = column.subtract(value1, value2); // 5556
        
        Assert.assertEquals("55.56", column.formatDecimal(result));
    }
    
    @Test
    public void testMultiply() {
        Decimal9Column column = new Decimal9Column(2);
        
        int value1 = column.parseDecimal("12.34"); // 1234
        int value2 = column.parseDecimal("5.67");  // 567
        int result = column.multiply(value1, value2);
        
        Assert.assertEquals("69.97", column.formatDecimal(result));
    }
    
    @Test
    public void testDivide() {
        Decimal9Column column = new Decimal9Column(2);
        
        int dividend = column.parseDecimal("100.00"); // 10000
        int divisor = column.parseDecimal("4.00");    // 400
        int result = column.divide(dividend, divisor);
        
        Assert.assertEquals("25", column.formatDecimal(result));
    }
    
    @Test
    public void testDivideByZero() {
        Decimal9Column column = new Decimal9Column(2);
        
        try {
            column.divide(100, 0);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertEquals("Division by zero", e.getMessage());
        }
    }
    
    @Test
    public void testCompare() {
        Decimal9Column column = new Decimal9Column(2);
        
        int value1 = column.parseDecimal("123.45");
        int value2 = column.parseDecimal("67.89");
        int value3 = column.parseDecimal("123.45");
        
        Assert.assertTrue(column.compare(value1, value2) > 0);
        Assert.assertTrue(column.compare(value2, value1) < 0);
        Assert.assertEquals(0, column.compare(value1, value3));
    }
    
    @Test
    public void testAdditionOverflow() {
        Decimal9Column column = new Decimal9Column(0);
        
        try {
            column.add(Integer.MAX_VALUE, 1);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testSubtractionOverflow() {
        Decimal9Column column = new Decimal9Column(0);
        
        try {
            column.subtract(Integer.MIN_VALUE, 1);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testValidValue() {
        Decimal9Column column = new Decimal9Column(4);
        
        Assert.assertTrue(column.isValidValue(0));
        Assert.assertTrue(column.isValidValue(Integer.MAX_VALUE));
        Assert.assertTrue(column.isValidValue(Integer.MIN_VALUE));
        
        // Test long value validation
        Assert.assertTrue(column.isValidValue(0L));
        Assert.assertTrue(column.isValidValue((long) Integer.MAX_VALUE));
        Assert.assertFalse(column.isValidValue(Long.MAX_VALUE));
    }
    
    @Test
    public void testMaxScale() {
        new Decimal9Column(9); // Should work
        
        try {
            new Decimal9Column(10);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum"));
        }
    }
    
    @Test
    public void testParseDecimalOverflow() {
        Decimal9Column column = new Decimal9Column(0);
        
        try {
            column.parseDecimal("2147483648"); // Integer.MAX_VALUE + 1
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds DECIMAL9 range"));
        }
    }
    
    @Test
    public void testRounding() {
        Decimal9Column column = new Decimal9Column(2);
        
        // Test half-up rounding in parsing
        Assert.assertEquals(1235, column.parseDecimal("12.345")); // rounds to 12.35
        Assert.assertEquals(1234, column.parseDecimal("12.344")); // rounds to 12.34
    }
    
    @Test
    public void testMaximumValues() {
        Decimal9Column column = new Decimal9Column(4);
        
        // Test maximum safe values for DECIMAL9(4)
        int maxValue = 214748; // Integer.MAX_VALUE / 10000 (rounded down)
        Assert.assertEquals("21.4748", column.formatDecimal(maxValue));
        
        int minValue = -214748;
        Assert.assertEquals("-21.4748", column.formatDecimal(minValue));
    }
    
    @Test
    public void testMultiplicationOverflow() {
        Decimal9Column column = new Decimal9Column(2);
        
        try {
            int largeValue1 = column.parseDecimal("99999.99");
            int largeValue2 = column.parseDecimal("99999.99");
            column.multiply(largeValue1, largeValue2);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testDivisionOverflow() {
        Decimal9Column column = new Decimal9Column(2);
        
        try {
            int dividend = column.parseDecimal("21474836.47"); // Near max
            int divisor = column.parseDecimal("0.01");        // Small divisor
            column.divide(dividend, divisor);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testToString() {
        Decimal9Column column = new Decimal9Column(4);
        Assert.assertEquals("DECIMAL9(4)", column.toString());
        
        Decimal9Column column2 = new Decimal9Column(0);
        Assert.assertEquals("DECIMAL9(0)", column2.toString());
    }
    
    @Test
    public void testFactoryMethod() {
        Decimal9Column column = Decimal9Column.create(6);
        Assert.assertEquals(6, column.getScale());
        Assert.assertEquals(1000000L, column.getScaleFactor());
    }
    
    @Test
    public void testMinMaxValues() {
        Decimal9Column column = new Decimal9Column(2);
        
        String minValue = column.getMinValue();
        String maxValue = column.getMaxValue();
        
        Assert.assertNotNull(minValue);
        Assert.assertNotNull(maxValue);
        Assert.assertTrue(minValue.startsWith("-"));
        Assert.assertFalse(maxValue.startsWith("-"));
    }
    
    @Test
    public void testPrecisionBoundaries() {
        // Test DECIMAL9 with different scales at precision boundaries
        Decimal9Column column1 = new Decimal9Column(1); // 8 digits before decimal
        Assert.assertEquals(123456789, column1.parseDecimal("12345678.9"));
        
        Decimal9Column column5 = new Decimal9Column(5); // 4 digits before decimal  
        Assert.assertEquals(99999500000L, column5.parseDecimal("9999.95000"));
    }
}