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

package io.questdb.test.griffin.engine.functions;

import io.questdb.test.AbstractCairoTest;
import org.junit.Test;

public class Long128FunctionTest extends AbstractCairoTest {

    @Test
    public void testLong128Creation() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(1, 2) as val from long_sequence(1))");
            
            assertSql(
                    "val\n" +
                    "0x10000000000000002\n",
                    "select * from tab"
            );
        });
    }

    @Test
    public void testLong128Arithmetic() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(5, 10) as a, to_long128(3, 7) as b from long_sequence(1))");
            
            assertSql(
                    "addition\tsubtraction\n" +
                    "0x80000000000000011\t0x20000000000000003\n",
                    "select a + b as addition, a - b as subtraction from tab"
            );
        });
    }

    @Test
    public void testLong128Comparison() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select " +
                    "to_long128(1, 100) as a, " +
                    "to_long128(1, 200) as b, " +
                    "to_long128(2, 50) as c " +
                    "from long_sequence(1))");
            
            assertSql(
                    "lt_same_hi\tlt_diff_hi\tgt_same_hi\teq\tneq\n" +
                    "true\ttrue\tfalse\tfalse\ttrue\n",
                    "select " +
                    "a < b as lt_same_hi, " +
                    "a < c as lt_diff_hi, " +
                    "a > b as gt_same_hi, " +
                    "a = b as eq, " +
                    "a != b as neq " +
                    "from tab"
            );
        });
    }

    @Test
    public void testLong128Casting() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select " +
                    "100::long as long_val, " +
                    "to_long128(0, 100) as long128_val " +
                    "from long_sequence(1))");
            
            assertSql(
                    "cast_to_long128\tcast_to_long\n" +
                    "0x00000000000000064\t100\n",
                    "select " +
                    "cast(long_val as long128) as cast_to_long128, " +
                    "cast(long128_val as long) as cast_to_long " +
                    "from tab"
            );
        });
    }

    @Test
    public void testLong128Random() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select rnd_long128() as rnd_val from long_sequence(5))");
            
            // Just verify it runs and returns 5 rows
            assertSql("count\n5\n", "select count(*) from tab");
        });
    }

    @Test
    public void testLong128Aggregation() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "x, " +
                    "to_long128(0, x) as val " +
                    "from long_sequence(5)" +
                    ")");
            
            assertSql(
                    "sum_val\n" +
                    "0x0000000000000000f\n",
                    "select sum(val) as sum_val from tab"
            );
        });
    }

    @Test
    public void testLong128WithNulls() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (" +
                    "select " +
                    "case when x % 2 = 0 then to_long128(0, x) else null end as val " +
                    "from long_sequence(6)" +
                    ")");
            
            assertSql(
                    "sum_val\n" +
                    "0x0000000000000000c\n",
                    "select sum(val) as sum_val from tab"
            );
        });
    }
}