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

package io.questdb.test.griffin.engine.functions.math;

import io.questdb.test.AbstractCairoTest;
import org.junit.Test;

public class SubLong128FunctionFactoryTest extends AbstractCairoTest {

    @Test
    public void testSubSimple() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(5, 10) as a, to_long128(2, 3) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x50000000000000000a\t0x20000000000000003\t0x30000000000000007\n",
                    "select a, b, a - b as c from tab"
            );
        });
    }

    @Test 
    public void testSubWithBorrow() throws Exception {
        assertMemoryLeak(() -> {
            // Test borrow from high to low part
            execute("create table tab as (select to_long128(1, 0) as a, to_long128(0, 1) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x10000000000000000\t0x10000000000000001\t0x0ffffffffffffffff\n",
                    "select a, b, a - b as c from tab"
            );
        });
    }

    @Test
    public void testSubZero() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(5, 10) as a, to_long128(0, 0) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x50000000000000000a\t0x00000000000000000\t0x50000000000000000a\n",
                    "select a, b, a - b as c from tab"
            );
        });
    }

    @Test
    public void testSubNegativeResult() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(2, 5) as a, to_long128(5, 10) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x20000000000000005\t0x50000000000000000a\t0xfffffffffffffffcfffffffffffffffb\n",
                    "select a, b, a - b as c from tab"
            );
        });
    }

    @Test
    public void testSubSameValues() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(7, 13) as a from long_sequence(1))");
            
            assertSql(
                    "a\tc\n" +
                    "0x70000000000000000d\t0x00000000000000000\n",
                    "select a, a - a as c from tab"
            );
        });
    }

    @Test
    public void testSubMaxValues() throws Exception {
        assertMemoryLeak(() -> {
            // Test subtraction with large values
            execute("create table tab as (select " +
                    "to_long128(" + Long.MAX_VALUE + ", " + Long.MAX_VALUE + ") as a, " +
                    "to_long128(1, 1) as b " +
                    "from long_sequence(1))");
            
            assertSql(
                    "c\n" +
                    "0x7fffffffffffffff7ffffffffffffffe\n",
                    "select a - b as c from tab"
            );
        });
    }
}