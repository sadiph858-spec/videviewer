package com.videviewer.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Converters - Room TypeConverters for complex types
 * Converts List<String> to/from JSON string for storage
 */
public class Converters {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<String> toStringList(String json) {
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }
}
