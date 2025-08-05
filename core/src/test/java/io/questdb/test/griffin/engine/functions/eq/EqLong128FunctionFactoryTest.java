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

package io.questdb.test.griffin.engine.functions.eq;

import io.questdb.test.AbstractCairoTest;
import org.junit.Test;

public class EqLong128FunctionFactoryTest extends AbstractCairoTest {

    @Test
    public void testEqLong128Simple() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "to_long128(1, 2) as a, " +
                    "to_long128(1, 2) as b, " +
                    "to_long128(1, 3) as c " +
                    "from long_sequence(1)" +
                    ")");
            
            assertSql(
                    "a\tb\tc\ta_eq_b\ta_eq_c\n" +
                    "0x10000000000000002\t0x10000000000000002\t0x10000000000000003\ttrue\tfalse\n",
                    "select *, a = b as a_eq_b, a = c as a_eq_c from tab"
            );
        });
    }

    @Test
    public void testEqLong128Zero() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "to_long128(0, 0) as zero1, " +
                    "to_long128(0, 0) as zero2, " +
                    "to_long128(0, 1) as one " +
                    "from long_sequence(1)" +
                    ")");
            
            assertSql(
                    "zero_eq_zero\tzero_eq_one\n" +
                    "true\tfalse\n",
                    "select zero1 = zero2 as zero_eq_zero, zero1 = one as zero_eq_one from tab"
            );
        });
    }

    @Test
    public void testEqLong128WithNulls() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "to_long128(1, 2) as a, " +
                    "cast(null as long128) as b " +
                    "from long_sequence(1)" +
                    ")");
            
            assertSql(
                    "a_eq_b\tb_eq_b\n" +
                    "false\tfalse\n",
                    "select a = b as a_eq_b, b = b as b_eq_b from tab"
            );
        });
    }

    @Test
    public void testEqLong128HighPartDifference() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "to_long128(1, 100) as a, " +
                    "to_long128(2, 100) as b " +
                    "from long_sequence(1)" +
                    ")");
            
            assertSql(
                    "a_eq_b\n" +
                    "false\n",
                    "select a = b as a_eq_b from tab"
            );
        });
    }

    @Test
    public void testEqLong128LowPartDifference() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "to_long128(5, 100) as a, " +
                    "to_long128(5, 200) as b " +
                    "from long_sequence(1)" +
                    ")");
            
            assertSql(
                    "a_eq_b\n" +
                    "false\n",
                    "select a = b as a_eq_b from tab"
            );
        });
    }

    @Test
    public void testEqLong128LargeValues() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "to_long128(" + Long.MAX_VALUE + ", " + Long.MIN_VALUE + ") as a, " +
                    "to_long128(" + Long.MAX_VALUE + ", " + Long.MIN_VALUE + ") as b " +
                    "from long_sequence(1)" +
                    ")");
            
            assertSql(
                    "a_eq_b\n" +
                    "true\n",
                    "select a = b as a_eq_b from tab"
            );
        });
    }

    @Test
    public void testEqLong128InFilter() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "x, " +
                    "to_long128(0, x) as val " +
                    "from long_sequence(5)" +
                    ")");
            
            assertSql(
                    "x\tval\n" +
                    "3\t0x00000000000000003\n",
                    "select * from tab where val = to_long128(0, 3)"
            );
        });
    }
}