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

package io.questdb.griffin.engine.functions.groupby;

import io.questdb.cairo.ArrayColumnTypes;
import io.questdb.cairo.CairoConfiguration;
import io.questdb.cairo.ColumnType;
import io.questdb.cairo.map.MapValue;
import io.questdb.cairo.sql.Function;
import io.questdb.cairo.sql.Record;
import io.questdb.griffin.FunctionFactory;
import io.questdb.griffin.SqlExecutionContext;
import io.questdb.griffin.engine.functions.GroupByFunction;
import io.questdb.griffin.engine.functions.Long128Function;
import io.questdb.griffin.engine.functions.UnaryFunction;
import io.questdb.std.IntList;
import io.questdb.std.Numbers;
import io.questdb.std.ObjList;

public class SumLong128GroupByFunctionFactory implements FunctionFactory {
    @Override
    public String getSignature() {
        return "sum(X)";
    }

    @Override
    public boolean isGroupBy() {
        return true;
    }

    @Override
    public Function newInstance(int position, ObjList<Function> args, IntList argPositions, CairoConfiguration configuration, SqlExecutionContext sqlExecutionContext) {
        return new SumLong128GroupByFunction(args.getQuick(0));
    }

    private static class SumLong128GroupByFunction extends Long128Function implements GroupByFunction, UnaryFunction {
        private final Function arg;
        private int hiValueIndex;
        private int loValueIndex;

        public SumLong128GroupByFunction(Function arg) {
            this.arg = arg;
        }

        @Override
        public void computeFirst(MapValue mapValue, Record record, long rowId) {
            final long hi = arg.getLong128Hi(record);
            final long lo = arg.getLong128Lo(record);
            
            if (hi == Numbers.LONG_NULL && lo == Numbers.LONG_NULL) {
                mapValue.putLong(hiValueIndex, 0L);
                mapValue.putLong(loValueIndex, 0L);
            } else {
                mapValue.putLong(hiValueIndex, hi);
                mapValue.putLong(loValueIndex, lo);
            }
        }

        @Override
        public void computeNext(MapValue mapValue, Record record, long rowId) {
            final long hi = arg.getLong128Hi(record);
            final long lo = arg.getLong128Lo(record);
            
            if (hi != Numbers.LONG_NULL || lo != Numbers.LONG_NULL) {
                final long sumHi = mapValue.getLong(hiValueIndex);
                final long sumLo = mapValue.getLong(loValueIndex);
                
                // Add with carry
                final long newLo = sumLo + lo;
                final long carry = (((sumLo & lo) | ((sumLo | lo) & ~newLo)) >>> 63);
                final long newHi = sumHi + hi + carry;
                
                mapValue.putLong(hiValueIndex, newHi);
                mapValue.putLong(loValueIndex, newLo);
            }
        }

        @Override
        public Function getArg() {
            return arg;
        }

        @Override
        public long getLong128Hi(Record rec) {
            return rec.getLong(hiValueIndex);
        }

        @Override
        public long getLong128Lo(Record rec) {
            return rec.getLong(loValueIndex);
        }

        @Override
        public String getName() {
            return "sum";
        }

        @Override
        public int getValueIndex() {
            return hiValueIndex;
        }

        @Override
        public void initValueIndex(int valueIndex) {
            this.hiValueIndex = valueIndex;
            this.loValueIndex = valueIndex + 1;
        }

        @Override
        public void initValueTypes(ArrayColumnTypes columnTypes) {
            this.hiValueIndex = columnTypes.getColumnCount();
            columnTypes.add(ColumnType.LONG);  // high part
            columnTypes.add(ColumnType.LONG);  // low part
        }

        @Override
        public boolean isConstant() {
            return false;
        }

        @Override
        public void setNull(MapValue mapValue) {
            mapValue.putLong(hiValueIndex, Numbers.LONG_NULL);
            mapValue.putLong(loValueIndex, Numbers.LONG_NULL);
        }

        @Override
        public boolean supportsParallelism() {
            return UnaryFunction.super.supportsParallelism();
        }
    }
}