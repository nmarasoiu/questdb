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

package io.questdb.test.griffin.engine.functions.columns;

import io.questdb.cairo.ColumnType;
import io.questdb.cairo.sql.Function;
import io.questdb.griffin.FunctionFactory;
import io.questdb.griffin.SqlException;
import io.questdb.griffin.SqlExecutionContext;
import io.questdb.griffin.engine.functions.columns.Decimal18Column;
import io.questdb.std.IntList;
import io.questdb.std.ObjList;
import io.questdb.test.griffin.engine.AbstractFunctionFactoryTest;
import org.junit.Test;

public class Decimal18ColumnTest extends AbstractFunctionFactoryTest {
    
    @Test
    public void testDecimal18Column() throws SqlException {
        call(1234567890123L).andAssert(1234567890123L);
    }
    
    @Test
    public void testDecimal18ColumnNegative() throws SqlException {
        call(-1234567890123L).andAssert(-1234567890123L);
    }
    
    @Test
    public void testDecimal18ColumnZero() throws SqlException {
        call(0L).andAssert(0L);
    }
    
    @Test
    public void testDecimal18ColumnMaxValue() throws SqlException {
        call(Long.MAX_VALUE).andAssert(Long.MAX_VALUE);
    }
    
    @Test
    public void testDecimal18ColumnMinValue() throws SqlException {
        call(Long.MIN_VALUE).andAssert(Long.MIN_VALUE);
    }

    @Override
    protected FunctionFactory getFunctionFactory() {
        return new FunctionFactory() {
            @Override
            public String getSignature() {
                return "decimal18(L)";
            }

            @Override
            public Function newInstance(int position, ObjList<Function> args, IntList argPositions, 
                                      io.questdb.cairo.CairoConfiguration configuration, 
                                      SqlExecutionContext sqlExecutionContext) {
                return new Decimal18Column() {
                    @Override
                    public long getLong(io.questdb.cairo.sql.Record rec) {
                        return args.getQuick(0).getLong(rec);
                    }
                };
            }
        };
    }
}