/*
 * Copyright 2022 Daniel Allen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class WFInfo {
    public static final HashMap<String, Boolean> itemMasteredCache = new HashMap<>();
    public static final HashMap<String, MarketAPI.Pair<Integer, Integer>> itemCountCache = new HashMap<>();
    public static final HashMap<String, String> nameMappings = new HashMap<>();
    private static final File wfInfoEquipmentFile = new File(System.getenv("APPDATA") + "\\WFInfo\\eqmt_data.json");

    public static void updateItemCounts() {
        try {
            JSONObject equipment = new JSONObject(readJsonFile(wfInfoEquipmentFile));
            for (String s : equipment.keySet()) {
                Object item = equipment.get(s);
                if (item instanceof JSONObject) {
                    int min = Integer.MAX_VALUE;
                    boolean mastered = ((JSONObject) item).getBoolean("mastered");
                    JSONObject parts = ((JSONObject) item).getJSONObject("parts");
                    for (String partName : parts.keySet()) {
                        JSONObject part = parts.getJSONObject(partName);
                        int partCount = part.getInt("owned");
                        if (partCount < min) min = partCount;
                        int partsNeededForSet = part.getInt("count");
                        itemCountCache.put(partName, new MarketAPI.Pair<>(partCount, partsNeededForSet));
                        itemMasteredCache.put(partName, mastered);
                    }
                    nameMappings.put(s, s + " Set");
                    itemCountCache.put(convertName(s), new MarketAPI.Pair<>(min, 0));
                    if (((JSONObject) item).has("mastered")) {
                        itemMasteredCache.put(convertName(s), mastered);
                    }
                }
            }
        } catch (IOException e) {

        }
    }

    public static String readJsonFile(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        if (line != null) sb.append(line);
        while ((line = br.readLine()) != null) {
            sb.append("\n").append(line);
        }
        br.close();
        return sb.toString();
    }

    public static String convertName(String name) {
        // if(nameMappings.containsKey(name)){
        //    return nameMappings.get(name);
        //}
        return name;
    }

    public static boolean isMastered(String name) {
        boolean b = itemMasteredCache.containsKey(convertName(name));
        if (b) return itemMasteredCache.get(convertName(name));
        return false;
    }

    public static MarketAPI.Pair<Integer, Integer> getItemCount(String name) {
        if (itemCountCache.containsKey(convertName(name))) {
            return itemCountCache.get(convertName(name));
        }
        JSONObject item = getItemFromWFInfoJson(name);
        if (item == null) return new MarketAPI.Pair<>(0, 0);
        int owned = 0;
        int count = 0;
        if (item.has("parts")) {
            //is a set
            int min = Integer.MAX_VALUE;
            JSONObject parts = item.getJSONObject("parts");
            for (String partName : parts.keySet()) {
                JSONObject part = parts.getJSONObject(partName);
                int partCount = part.has("owned") ? part.getInt("owned") : 0;
                if (partCount < min) min = partCount;
                int partsNeededForSet = part.getInt("count");
                itemCountCache.put(partName, new MarketAPI.Pair<>(partCount, partsNeededForSet));
            }
            itemCountCache.put(convertName(name), new MarketAPI.Pair<>(min, 0));
            owned = min;

        } else {
            owned = item.has("owned") ? item.getInt("owned") : 0;
            count = item.has("count") ? item.getInt("count") : 0;
            itemCountCache.put(name, new MarketAPI.Pair<>(owned, count));
        }
        return new MarketAPI.Pair<>(owned, count);
    }

    public static JSONObject getItemFromWFInfoJson(String name) {
        try {
            JSONObject equipment = new JSONObject(readJsonFile(wfInfoEquipmentFile));
            for (String s : equipment.keySet()) {
                if (convertName(s).equals(name) || s.equals(name)) {
                    return equipment.getJSONObject(s);
                }
                Object set = equipment.get(s);
                if (set instanceof JSONObject) {
                    if (((JSONObject) set).has("parts")) {
                        JSONObject parts = ((JSONObject) set).getJSONObject("parts");
                        for (String part : parts.keySet()) {
                            if (part.equals(name)) {
                                return parts.getJSONObject(name);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
