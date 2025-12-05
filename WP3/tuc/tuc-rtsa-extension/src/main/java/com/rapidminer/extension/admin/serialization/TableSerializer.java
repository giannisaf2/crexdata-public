/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonFactory
 *  com.fasterxml.jackson.core.JsonGenerator
 *  com.rapidminer.belt.column.ColumnType
 *  com.rapidminer.belt.reader.MixedRowReader
 *  com.rapidminer.belt.reader.Readers
 *  com.rapidminer.belt.table.Table
 *  com.rapidminer.operator.OperatorException
 */
package com.rapidminer.extension.admin.serialization;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Table;
import com.rapidminer.extension.admin.AssertUtility;
import com.rapidminer.extension.admin.operator.aihubapi.exceptions.AdminToolsException;
import com.rapidminer.operator.OperatorException;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;

public class TableSerializer {
    private TableSerializer() {
        throw new IllegalStateException("Utility class");
    }

    public static void writeJSON(Table table, OutputStream out) throws IOException, OperatorException, AdminToolsException {
        AssertUtility.notNull(table, "'table' cannot be null");
        AssertUtility.notNull(out, "'out' cannot be null");
        JsonFactory jsonFactory = new JsonFactory();
        try (JsonGenerator generator = jsonFactory.createGenerator(out);){
            MixedRowReader reader = Readers.mixedRowReader((Table)table);
            generator.writeStartObject();
            generator.writeArrayFieldStart("data");
            while (reader.hasRemaining()) {
                reader.move();
                int index = 0;
                generator.writeStartObject();
                for (String label : table.labels()) {
                    ColumnType type = table.column(label).type();
                    if (type.equals((Object)ColumnType.NOMINAL) || type.equals((Object)ColumnType.TEXT)) {
                        generator.writeStringField(label, (String)reader.getObject(index, String.class));
                    } else if (type.equals((Object)ColumnType.REAL) || type.equals((Object)ColumnType.INTEGER_53_BIT)) {
                        generator.writeNumberField(label, reader.getNumeric(index));
                    } else if (type.equals((Object)ColumnType.DATETIME)) {
                        generator.writeNumberField(label, ((Instant)reader.getObject(index, Instant.class)).toEpochMilli());
                    } else {
                        throw new OperatorException(type.id().toString() + " not yet supported");
                    }
                    ++index;
                }
                generator.writeEndObject();
            }
            generator.writeEndArray();
            generator.writeEndObject();
        }
    }
}

