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

package io.questdb.test.griffin.engine.functions.groupby;

import io.questdb.test.AbstractCairoTest;
import org.junit.Test;

public class SumLong128GroupByFunctionFactoryTest extends AbstractCairoTest {

    @Test
    public void testSumLong128Simple() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "x % 3 as grp, " +
                    "to_long128(0, x) as val " +
                    "from long_sequence(9)" +
                    ")");
            
            assertSql(
                    "grp\tsum\n" +
                    "0\t0x00000000000000012\n" +
                    "1\t0x0000000000000000f\n" +
                    "2\t0x00000000000000014\n",
                    "select grp, sum(val) from tab order by grp"
            );
        });
    }

    @Test
    public void testSumLong128WithCarry() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "1 as grp, " +
                    "to_long128(0, " + Long.MAX_VALUE + ") as val " +
                    "from long_sequence(2)" +
                    ")");
            
            assertSql(
                    "grp\tsum\n" +
                    "1\t0x10000000000000000fffffffffffffffe\n",
                    "select grp, sum(val) from tab"
            );
        });
    }

    @Test
    public void testSumLong128WithNulls() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "x % 2 as grp, " +
                    "case when x % 4 = 0 then null else to_long128(0, x) end as val " +
                    "from long_sequence(8)" +
                    ")");
            
            assertSql(
                    "grp\tsum\n" +
                    "0\t0x00000000000000006\n" +
                    "1\t0x0000000000000000e\n",
                    "select grp, sum(val) from tab order by grp"
            );
        });
    }

    @Test
    public void testSumLong128Empty() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab (grp int, val long128)");
            
            assertSql(
                    "sum\n" +
                    "0x00000000000000000\n",
                    "select sum(val) from tab"
            );
        });
    }

    @Test
    public void testSumLong128AllNulls() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "1 as grp, " +
                    "cast(null as long128) as val " +
                    "from long_sequence(5)" +
                    ")");
            
            assertSql(
                    "grp\tsum\n" +
                    "1\t\n",
                    "select grp, sum(val) from tab"
            );
        });
    }

    @Test
    public void testSumLong128HighPart() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "x % 2 as grp, " +
                    "to_long128(x, 0) as val " +
                    "from long_sequence(4)" +
                    ")");
            
            assertSql(
                    "grp\tsum\n" +
                    "0\t0x60000000000000000\n" +
                    "1\t0x40000000000000000\n",
                    "select grp, sum(val) from tab order by grp"
            );
        });
    }
}