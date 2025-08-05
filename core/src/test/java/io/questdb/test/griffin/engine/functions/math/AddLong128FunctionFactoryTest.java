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

public class AddLong128FunctionFactoryTest extends AbstractCairoTest {

    @Test
    public void testAddSimple() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(1, 2) as a, to_long128(3, 4) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x20000000000000001\t0x40000000000000003\t0x60000000000000004\n",
                    "select a, b, a + b as c from tab"
            );
        });
    }

    @Test 
    public void testAddWithCarry() throws Exception {
        assertMemoryLeak(() -> {
            // Test carry from low to high part
            execute("create table tab as (select to_long128(0, -1) as a, to_long128(0, 1) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x0ffffffffffffffff\t0x10000000000000001\t0x10000000000000000\n",
                    "select a, b, a + b as c from tab"
            );
        });
    }

    @Test
    public void testAddZero() throws Exception {
        assertMemoryLeak(() -> {
            execute("create table tab as (select to_long128(5, 10) as a, to_long128(0, 0) as b from long_sequence(1))");
            
            assertSql(
                    "a\tb\tc\n" +
                    "0x50000000000000000a\t0x00000000000000000\t0x50000000000000000a\n",
                    "select a, b, a + b as c from tab"
            );
        });
    }
}