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

package io.questdb.griffin.engine.functions.columns;

import io.questdb.cairo.sql.Function;
import io.questdb.cairo.sql.Record;
import io.questdb.griffin.PlanSink;
import io.questdb.griffin.engine.functions.Long256Function;
import io.questdb.std.Long256;
import io.questdb.std.ObjList;

import static io.questdb.griffin.engine.functions.columns.ColumnUtils.STATIC_COLUMN_COUNT;

/**
 * DECIMAL77 column function that reads LONG256 values and interprets them as 
 * scaled decimal values. Follows the same pattern as other column functions
 * like Long128Column, Long256Column, etc.
 */
public class Decimal77Column extends Long256Function implements Function {
    private static final ObjList<Decimal77Column> COLUMNS = new ObjList<>(STATIC_COLUMN_COUNT);
    private final int columnIndex;

    private Decimal77Column(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public static Decimal77Column newInstance(int columnIndex) {
        if (columnIndex < STATIC_COLUMN_COUNT) {
            return COLUMNS.getQuick(columnIndex);
        }
        return new Decimal77Column(columnIndex);
    }

    @Override
    public Long256 getLong256A(Record rec) {
        return rec.getLong256A(columnIndex);
    }

    @Override
    public Long256 getLong256B(Record rec) {
        return rec.getLong256B(columnIndex);
    }

    @Override
    public void toPlan(PlanSink sink) {
        sink.val(columnIndex);
    }

    static {
        for (int i = 0; i < STATIC_COLUMN_COUNT; i++) {
            COLUMNS.extendAndSet(i, new Decimal77Column(i));
        }
    }
}