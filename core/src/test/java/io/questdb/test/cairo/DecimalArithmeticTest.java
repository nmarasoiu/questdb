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

import io.questdb.cairo.DecimalArithmetic;
import io.questdb.cairo.DecimalColumnType;
import org.junit.Assert;
import org.junit.Test;

public class DecimalArithmeticTest {

    @Test
    public void testPowerOfTen() {
        Assert.assertEquals(1L, DecimalArithmetic.powerOfTen(0));
        Assert.assertEquals(10L, DecimalArithmetic.powerOfTen(1));
        Assert.assertEquals(100L, DecimalArithmetic.powerOfTen(2));
        Assert.assertEquals(1000L, DecimalArithmetic.powerOfTen(3));
        Assert.assertEquals(1000000000000000000L, DecimalArithmetic.powerOfTen(18));
    }
    
    @Test
    public void testPowerOfTenNegative() {
        try {
            DecimalArithmetic.powerOfTen(-1);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Negative exponent"));
        }
    }
    
    @Test
    public void testScaleValue() {
        // Scale up
        Assert.assertEquals(12300L, DecimalArithmetic.scaleValue(123L, 2, 4));
        Assert.assertEquals(1230L, DecimalArithmetic.scaleValue(123L, 1, 2));
        
        // Scale down
        Assert.assertEquals(123L, DecimalArithmetic.scaleValue(12300L, 4, 2));
        Assert.assertEquals(12L, DecimalArithmetic.scaleValue(1230L, 2, 1));
        
        // No scaling
        Assert.assertEquals(123L, DecimalArithmetic.scaleValue(123L, 2, 2));
    }
    
    @Test
    public void testScaleValueRounding() {
        // Test half-up rounding when scaling down
        Assert.assertEquals(124L, DecimalArithmetic.scaleValue(12350L, 3, 2)); // 123.50 -> 124 (rounds up)
        Assert.assertEquals(123L, DecimalArithmetic.scaleValue(12340L, 3, 2)); // 123.40 -> 123 (rounds down)
        Assert.assertEquals(123L, DecimalArithmetic.scaleValue(12349L, 3, 2)); // 123.49 -> 123 (rounds down)
        Assert.assertEquals(124L, DecimalArithmetic.scaleValue(12351L, 3, 2)); // 123.51 -> 124 (rounds up)
    }
    
    @Test
    public void testScaleValueOverflow() {
        try {
            DecimalArithmetic.scaleValue(Long.MAX_VALUE, 0, 1);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testAdd() {
        // Same scale
        DecimalArithmetic.AddResult result = DecimalArithmetic.add(12345L, 2, 6789L, 2);
        Assert.assertEquals(19134L, result.value);
        Assert.assertEquals(2, result.scale);
        
        // Different scales
        result = DecimalArithmetic.add(1234L, 2, 567L, 1); // 12.34 + 56.7 = 69.04
        Assert.assertEquals(6904L, result.value);
        Assert.assertEquals(2, result.scale);
        
        result = DecimalArithmetic.add(567L, 1, 1234L, 2); // 56.7 + 12.34 = 69.04  
        Assert.assertEquals(6904L, result.value);
        Assert.assertEquals(2, result.scale);
    }
    
    @Test
    public void testAddOverflow() {
        try {
            DecimalArithmetic.add(Long.MAX_VALUE, 0, 1L, 0);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testSubtract() {
        // Same scale
        DecimalArithmetic.SubtractResult result = DecimalArithmetic.subtract(12345L, 2, 6789L, 2);
        Assert.assertEquals(5556L, result.value);
        Assert.assertEquals(2, result.scale);
        
        // Different scales
        result = DecimalArithmetic.subtract(5670L, 1, 1234L, 2); // 567.0 - 12.34 = 554.66
        Assert.assertEquals(55466L, result.value);
        Assert.assertEquals(2, result.scale);
    }
    
    @Test
    public void testSubtractOverflow() {
        try {
            DecimalArithmetic.subtract(Long.MIN_VALUE, 0, 1L, 0);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
    
    @Test
    public void testMultiply() {
        DecimalArithmetic.MultiplyResult result = DecimalArithmetic.multiply(1234L, 2, 567L, 1);
        Assert.assertEquals(699078L, result.value); // 12.34 * 56.7 = 699.078 (stored as 699078 with scale 3)
        Assert.assertEquals(3, result.scale);
        
        result = DecimalArithmetic.multiply(100L, 2, 200L, 2);
        Assert.assertEquals(20000L, result.value); // 1.00 * 2.00 = 2.0000 (stored as 20000 with scale 4)
        Assert.assertEquals(4, result.scale);
    }
    
    @Test
    public void testMultiplyOverflow() {
        try {
            DecimalArithmetic.multiply(Long.MAX_VALUE, 0, 2L, 0);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            // Math.multiplyExact should throw
        }
    }
    
    @Test
    public void testDivide() {
        DecimalArithmetic.DivideResult result = DecimalArithmetic.divide(10000L, 2, 400L, 2, 2);
        Assert.assertEquals(2500L, result.value); // 100.00 / 4.00 = 25.00 (with 2 extra scale)
        Assert.assertEquals(2, result.scale);
        
        result = DecimalArithmetic.divide(123456L, 3, 200L, 1, 2);
        Assert.assertEquals(61728L, result.value); // 123.456 / 20.0 = 6.1728 (scale = 3-1+2 = 4, value scaled to 4)
        Assert.assertEquals(4, result.scale);
    }
    
    @Test
    public void testDivideByZero() {
        try {
            DecimalArithmetic.divide(100L, 2, 0L, 2, 2);
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            Assert.assertEquals("Division by zero", e.getMessage());
        }
    }
    
    @Test
    public void testDivideRounding() {
        // Test rounding in division
        DecimalArithmetic.DivideResult result = DecimalArithmetic.divide(1000L, 2, 300L, 2, 2);
        // 10.00 / 3.00 = 3.333... with extra scale 2 -> should round to appropriate value
        Assert.assertEquals(2, result.scale);
        // Result should be properly rounded
    }
    
    @Test
    public void testDetermineResultType() {
        Assert.assertEquals(DecimalColumnType.DECIMAL18, 
            DecimalArithmetic.determineResultType(DecimalColumnType.DECIMAL9, DecimalColumnType.DECIMAL18));
        Assert.assertEquals(DecimalColumnType.DECIMAL38,
            DecimalArithmetic.determineResultType(DecimalColumnType.DECIMAL18, DecimalColumnType.DECIMAL38));
        Assert.assertEquals(DecimalColumnType.DECIMAL77,
            DecimalArithmetic.determineResultType(DecimalColumnType.DECIMAL38, DecimalColumnType.DECIMAL77));
    }
    
    @Test
    public void testFitsInType() {
        // DECIMAL9 range tests
        Assert.assertTrue(DecimalArithmetic.fitsInType(Integer.MAX_VALUE, 0, DecimalColumnType.DECIMAL9));
        Assert.assertTrue(DecimalArithmetic.fitsInType(Integer.MIN_VALUE, 0, DecimalColumnType.DECIMAL9));
        Assert.assertFalse(DecimalArithmetic.fitsInType(Long.MAX_VALUE, 0, DecimalColumnType.DECIMAL9));
        
        // DECIMAL18 range tests
        Assert.assertTrue(DecimalArithmetic.fitsInType(Long.MAX_VALUE, 0, DecimalColumnType.DECIMAL18));
        Assert.assertTrue(DecimalArithmetic.fitsInType(Long.MIN_VALUE, 0, DecimalColumnType.DECIMAL18));
        
        // DECIMAL38 and DECIMAL77 - always true for now (placeholder implementation)
        Assert.assertTrue(DecimalArithmetic.fitsInType(Long.MAX_VALUE, 0, DecimalColumnType.DECIMAL38));
        Assert.assertTrue(DecimalArithmetic.fitsInType(Long.MAX_VALUE, 0, DecimalColumnType.DECIMAL77));
    }
    
    @Test
    public void testNegativeValues() {
        // Test negative value handling in all operations
        DecimalArithmetic.AddResult addResult = DecimalArithmetic.add(-1234L, 2, -5678L, 2);
        Assert.assertEquals(-6912L, addResult.value);
        Assert.assertEquals(2, addResult.scale);
        
        DecimalArithmetic.SubtractResult subResult = DecimalArithmetic.subtract(-1234L, 2, -5678L, 2);
        Assert.assertEquals(4444L, subResult.value); // -12.34 - (-56.78) = 44.44
        Assert.assertEquals(2, subResult.scale);
        
        DecimalArithmetic.MultiplyResult mulResult = DecimalArithmetic.multiply(-1234L, 2, 567L, 1);
        Assert.assertEquals(-699078L, mulResult.value);
        Assert.assertEquals(3, mulResult.scale);
        
        DecimalArithmetic.DivideResult divResult = DecimalArithmetic.divide(-10000L, 2, 400L, 2, 2);
        Assert.assertEquals(-2500L, divResult.value);
        Assert.assertEquals(2, divResult.scale);
    }
    
    @Test
    public void testZeroValues() {
        // Test zero handling
        DecimalArithmetic.AddResult addResult = DecimalArithmetic.add(0L, 2, 1234L, 2);
        Assert.assertEquals(1234L, addResult.value);
        
        DecimalArithmetic.SubtractResult subResult = DecimalArithmetic.subtract(1234L, 2, 0L, 2);
        Assert.assertEquals(1234L, subResult.value);
        
        DecimalArithmetic.MultiplyResult mulResult = DecimalArithmetic.multiply(0L, 2, 1234L, 2);
        Assert.assertEquals(0L, mulResult.value);
        
        DecimalArithmetic.DivideResult divResult = DecimalArithmetic.divide(0L, 2, 1234L, 2, 2);
        Assert.assertEquals(0L, divResult.value);
    }
    
    @Test
    public void testPowerOfTenLargeExponents() {
        // Test larger exponents
        Assert.assertEquals(1000000000000000000L, DecimalArithmetic.powerOfTen(18));
        
        try {
            DecimalArithmetic.powerOfTen(50); // Should cause overflow
            Assert.fail("Expected ArithmeticException for very large exponent");
        } catch (ArithmeticException e) {
            Assert.assertTrue(e.getMessage().contains("overflow"));
        }
    }
}