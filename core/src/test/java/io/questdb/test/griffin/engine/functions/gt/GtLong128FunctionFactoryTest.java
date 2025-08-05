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

package io.questdb.test.griffin.engine.functions.gt;

import io.questdb.test.AbstractCairoTest;
import org.junit.Test;

public class GtLong128FunctionFactoryTest extends AbstractCairoTest {

    @Test
    public void testGtSimple() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(1, 3) as a, to_long128(1, 2) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x10000000000000003\t0x10000000000000002\ttrue\n",
                    "select a, b, a > b as c from tab"
            );
        });
    }

    @Test
    public void testGtHighPartDifference() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(2, 50) as a, to_long128(1, 100) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x20000000000000032\t0x10000000000000064\ttrue\n",
                    "select a, b, a > b as c from tab"
            );
        });
    }

    @Test
    public void testGtEqual() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(5, 10) as a, to_long128(5, 10) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x50000000000000000a\t0x50000000000000000a\tfalse\n",
                    "select a, b, a > b as c from tab"
            );
        });
    }

    @Test
    public void testGtUnsignedComparison() throws Exception {
        assertMemoryLeak(() -> {
            // Same high part, but unsigned comparison needed for low part
            execute("create table tab as (select to_long128(0, 1) as a, to_long128(0, -1) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x10000000000000001\t0x0ffffffffffffffff\tfalse\n",
                    "select a, b, a > b as c from tab"
            );
        });
    }

    @Test
    public void testGtNegativeHighPart() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(-1, 100) as a, to_long128(-2, 200) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0xffffffffffffffff0000000000000064\t0xfffffffffffffffe00000000000000c8\ttrue\n",
                    "select a, b, a > b as c from tab"
            );
        });
    }

    @Test
    public void testGtZero() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select " +
                    "to_long128(0, 1) as positive, " +
                    "to_long128(0, 0) as zero, " +
                    "to_long128(-1, -1) as negative " +
                    "from long_sequence(1))");
            
            assertSql(
                    "pos_gt_zero\tzero_gt_neg\tpos_gt_neg\n" +
                    "true\ttrue\ttrue\n",
                    "select " +
                    "positive > zero as pos_gt_zero, " +
                    "zero > negative as zero_gt_neg, " +
                    "positive > negative as pos_gt_neg " +
                    "from tab"
            );
        });
    }

    @Test
    public void testGtWithNulls() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select " +
                    "to_long128(1, 2) as a, " +
                    "cast(null as long128) as b " +
                    "from long_sequence(1))");
            
            assertSql(
                    "a_gt_b\tb_gt_a\n" +
                    "false\tfalse\n",
                    "select a > b as a_gt_b, b > a as b_gt_a from tab"
            );
        });
    }
}