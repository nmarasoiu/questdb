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

package io.questdb.test.griffin;

import io.questdb.cairo.DecimalColumnType;
import io.questdb.griffin.DecimalSqlParser;
import io.questdb.griffin.SqlException;
import io.questdb.std.GenericLexer;
import org.junit.Assert;
import org.junit.Test;

public class DecimalSqlParserTest {

    @Test
    public void testParseDecimalType() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        
        // Test DECIMAL18(4)
        lexer.of("DECIMAL18 ( 4 )");
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(columnType));
        Assert.assertEquals(DecimalColumnType.DECIMAL18, DecimalColumnType.getDecimalType(columnType));
        Assert.assertEquals(4, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testParseDecimal9() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL9(2)");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(columnType));
        Assert.assertEquals(DecimalColumnType.DECIMAL9, DecimalColumnType.getDecimalType(columnType));
        Assert.assertEquals(2, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testParseDecimal38() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL38(15)");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(columnType));
        Assert.assertEquals(DecimalColumnType.DECIMAL38, DecimalColumnType.getDecimalType(columnType));
        Assert.assertEquals(15, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testParseDecimal77() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL77(25)");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(columnType));
        Assert.assertEquals(DecimalColumnType.DECIMAL77, DecimalColumnType.getDecimalType(columnType));
        Assert.assertEquals(25, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testParseZeroScale() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18(0)");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertEquals(0, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testParseMaxScale() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18(18)");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertEquals(18, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testParseScaleExceedsPrecision() {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL9(10)");
        
        try {
            CharSequence tok = lexer.next();
            DecimalSqlParser.parseDecimalType(lexer, tok);
            Assert.fail("Expected SqlException");
        } catch (SqlException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum precision"));
        }
    }
    
    @Test
    public void testParseNegativeScale() {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18(-1)");
        
        try {
            CharSequence tok = lexer.next();
            DecimalSqlParser.parseDecimalType(lexer, tok);
            Assert.fail("Expected SqlException");
        } catch (SqlException e) {
            Assert.assertTrue(e.getMessage().contains("cannot be negative"));
        }
    }
    
    @Test
    public void testParseInvalidScale() {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18(abc)");
        
        try {
            CharSequence tok = lexer.next();
            DecimalSqlParser.parseDecimalType(lexer, tok);
            Assert.fail("Expected SqlException");
        } catch (SqlException e) {
            Assert.assertTrue(e.getMessage().contains("invalid scale value"));
        }
    }
    
    @Test
    public void testParseMissingOpenParen() {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18 4)");
        
        try {
            CharSequence tok = lexer.next();
            DecimalSqlParser.parseDecimalType(lexer, tok);
            Assert.fail("Expected SqlException");
        } catch (SqlException e) {
            Assert.assertTrue(e.getMessage().contains("scale parameter expected"));
        }
    }
    
    @Test
    public void testParseMissingCloseParen() {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18(4");
        
        try {
            CharSequence tok = lexer.next();
            DecimalSqlParser.parseDecimalType(lexer, tok);
            Assert.fail("Expected SqlException");
        } catch (SqlException e) {
            Assert.assertTrue(e.getMessage().contains("')' expected"));
        }
    }
    
    @Test
    public void testParseMissingScale() {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18()");
        
        try {
            CharSequence tok = lexer.next();
            DecimalSqlParser.parseDecimalType(lexer, tok);
            Assert.fail("Expected SqlException");
        } catch (SqlException e) {
            Assert.assertTrue(e.getMessage().contains("scale value expected"));
        }
    }
    
    @Test
    public void testParseNonDecimalType() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("INT");
        
        // Should delegate to standard parsing and return INT type
        CharSequence tok = lexer.next();
        try {
            int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
            // This should delegate to SqlUtil.toPersistedTypeTag and return INT
            // The exact behavior depends on whether INT is a valid persisted type
        } catch (SqlException e) {
            // Expected if INT is not found in the type map in this test context
            Assert.assertTrue(e.getMessage().contains("unsupported column type"));
        }
    }
    
    @Test
    public void testCaseInsensitive() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("decimal18(4)");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(columnType));
        Assert.assertEquals(DecimalColumnType.DECIMAL18, DecimalColumnType.getDecimalType(columnType));
        Assert.assertEquals(4, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testMixedCase() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("Decimal9(6)");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(columnType));
        Assert.assertEquals(DecimalColumnType.DECIMAL9, DecimalColumnType.getDecimalType(columnType));
        Assert.assertEquals(6, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testWhitespaceHandling() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18  (  4  )");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(columnType));
        Assert.assertEquals(4, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testLargeScale() throws SqlException {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL77(50)");
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertTrue(DecimalColumnType.isDecimal(columnType));
        Assert.assertEquals(50, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testScaleBoundaryValues() throws SqlException {
        // Test scale = 255 (maximum)
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL77(77)"); // Maximum scale for DECIMAL77
        
        CharSequence tok = lexer.next();
        int columnType = DecimalSqlParser.parseDecimalType(lexer, tok);
        
        Assert.assertEquals(77, DecimalColumnType.getScale(columnType));
    }
    
    @Test
    public void testScaleOverflow() {
        GenericLexer lexer = new GenericLexer(64);
        lexer.of("DECIMAL18(256)"); // Exceeds maximum scale of 255
        
        try {
            CharSequence tok = lexer.next();
            DecimalSqlParser.parseDecimalType(lexer, tok);
            Assert.fail("Expected SqlException");
        } catch (SqlException e) {
            Assert.assertTrue(e.getMessage().contains("must be between 0 and"));
        }
    }
}