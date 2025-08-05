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

package io.questdb.test.griffin.engine.functions.cast;

import io.questdb.test.AbstractCairoTest;
import org.junit.Test;

public class CastLong128Test extends AbstractCairoTest {

    @Test
    public void testCastIntToLong128() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select cast(123 as int) as int_val from long_sequence(1))");
            
            assertSql(
                    "int_val\tcast_val\n" +
                    "123\t0x0000000000000007b\n",
                    "select int_val, cast(int_val as long128) as cast_val from tab"
            );
        });
    }

    @Test
    public void testCastNegativeIntToLong128() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select cast(-123 as int) as int_val from long_sequence(1))");
            
            assertSql(
                    "int_val\tcast_val\n" +
                    "-123\t0xffffffffffffffffffffffffffffffff85\n",
                    "select int_val, cast(int_val as long128) as cast_val from tab"
            );
        });
    }

    @Test
    public void testCastLongToLong128() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select 9223372036854775807L as long_val from long_sequence(1))");
            
            assertSql(
                    "long_val\tcast_val\n" +
                    "9223372036854775807\t0x07fffffffffffffff\n",
                    "select long_val, cast(long_val as long128) as cast_val from tab"
            );
        });
    }

    @Test
    public void testCastNegativeLongToLong128() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select -9223372036854775808L as long_val from long_sequence(1))");
            
            assertSql(
                    "long_val\tcast_val\n" +
                    "-9223372036854775808\t0xffffffffffffffff8000000000000000\n",
                    "select long_val, cast(long_val as long128) as cast_val from tab"
            );
        });
    }

    @Test
    public void testCastLong128ToLong() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(0, 123) as long128_val from long_sequence(1))");
            
            assertSql(
                    "long128_val\tcast_val\n" +
                    "0x0000000000000007b\t123\n",
                    "select long128_val, cast(long128_val as long) as cast_val from tab"
            );
        });
    }

    @Test
    public void testCastLong128ToLongTruncation() throws Exception {
        assertMemoryLeak(() -> {
            // High part should be truncated
            execute("create table tab as (select to_long128(123, 456) as long128_val from long_sequence(1))");
            
            assertSql(
                    "long128_val\tcast_val\n" +
                    "0x7b00000000000001c8\t456\n",
                    "select long128_val, cast(long128_val as long) as cast_val from tab"
            );
        });
    }

    @Test
    public void testCastLong128ToStr() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(1, 2) as long128_val from long_sequence(1))");
            
            assertSql(
                    "long128_val\tcast_val\n" +
                    "0x10000000000000002\t0x10000000000000002\n",
                    "select long128_val, cast(long128_val as string) as cast_val from tab"
            );
        });
    }

    @Test
    public void testCastNullLong128() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab (int_val int, long128_val long128)");
            execute("insert into tab values (null, null)");
            
            assertSql(
                    "cast_to_long128\tcast_to_long\tcast_to_str\n" +
                    "\t\t\n",
                    "select " +
                    "cast(int_val as long128) as cast_to_long128, " +
                    "cast(long128_val as long) as cast_to_long, " +
                    "cast(long128_val as string) as cast_to_str " +
                    "from tab"
            );
        });
    }

    @Test
    public void testCastZeroValues() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select " +
                    "0 as int_zero, " +
                    "0L as long_zero, " +
                    "to_long128(0, 0) as long128_zero " +
                    "from long_sequence(1))");
            
            assertSql(
                    "int_to_long128\tlong_to_long128\tlong128_to_long\n" +
                    "0x00000000000000000\t0x00000000000000000\t0\n",
                    "select " +
                    "cast(int_zero as long128) as int_to_long128, " +
                    "cast(long_zero as long128) as long_to_long128, " +
                    "cast(long128_zero as long) as long128_to_long " +
                    "from tab"
            );
        });
    }
}