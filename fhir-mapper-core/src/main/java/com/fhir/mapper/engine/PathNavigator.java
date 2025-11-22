package com.fhir.mapper.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Navigate nested map structures using path notation
 */
public class PathNavigator {
    
    public Object getValue(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current == null) return null;

            if (part.contains("[")) {
                int bracketIdx = part.indexOf('[');
                String key = part.substring(0, bracketIdx);
                int index = Integer.parseInt(part.substring(bracketIdx + 1, part.length() - 1));

                current = ((Map<String, Object>) current).get(key);
                if (current instanceof List) {
                    List list = (List) current;
                    current = index < list.size() ? list.get(index) : null;
                }
            } else {
                current = ((Map<String, Object>) current).get(part);
            }
        }

        return current;
    }

    public void setValue(Map<String, Object> data, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            if (part.contains("[")) {
                int bracketIdx = part.indexOf('[');
                String key = part.substring(0, bracketIdx);
                int index = Integer.parseInt(part.substring(bracketIdx + 1, part.length() - 1));

                current.putIfAbsent(key, new ArrayList<>());
                List list = (List) current.get(key);

                while (list.size() <= index) {
                    list.add(new LinkedHashMap<String, Object>());
                }

                current = (Map<String, Object>) list.get(index);
            } else {
                current.putIfAbsent(part, new LinkedHashMap<String, Object>());
                current = (Map<String, Object>) current.get(part);
            }
        }

        String lastPart = parts[parts.length - 1];
        if (lastPart.contains("[")) {
            int bracketIdx = lastPart.indexOf('[');
            String key = lastPart.substring(0, bracketIdx);
            int index = Integer.parseInt(lastPart.substring(bracketIdx + 1, lastPart.length() - 1));

            current.putIfAbsent(key, new ArrayList<>());
            List list = (List) current.get(key);
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, value);
        } else {
            current.put(lastPart, value);
        }
    }
}
