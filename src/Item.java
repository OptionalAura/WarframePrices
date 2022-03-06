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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serial;
import java.io.Serializable;
public class Item implements Serializable, CSVable {
    @Serial
    private static transient final long serialVersionUID = 5;

    //mandatory parameters
    @CSVInclude("Item Name")
    public String name;
    //@CSVInclude("API Name")
    public String url;
    //@CSVInclude("Tags")
    public String[] tags;
    //@CSVInclude("Warframe Wiki")
    public transient String wikiLink;
    public boolean tracked;
    public int trackValue = 0;
    //for prime items
    //@CSVInclude("Relic Sources")
    public String[] relics;
    //keeps track of if this was initialized with JSON or not
    public transient boolean initialized;
    @CSVInclude("90 Day Average")
    Double avg90d;
    @CSVInclude("Ducat Value")
    public Integer ducats;


    //for mods
    public Integer maxRank;
    public Integer orderCount;
    @CSVInclude("48 Hour Average")
    Double avg48h;

    //data for the table
    transient int location;
    transient Structure.Order buyOrder;
    transient Structure.Order sellOrder;
    transient String trendName;
    transient double trendValue;
    transient Integer profit;
    transient Integer buyPrice;
    transient Integer sellPrice;
    transient boolean goodBuy;
    @CSVInclude("Owned")
    transient int owned;
    @CSVInclude("In Set")
    transient int itemsInSet;
    @CSVInclude("Mastered")
    transient boolean mastered;
    //@CSVInclude("Should Sell")
    transient boolean shouldSell;

    public Item(JSONObject obj) {
        JSONArray itemsInSet = obj.getJSONArray("items_in_set");
        String id = obj.getString("id");
        JSONObject thisItem = itemsInSet.getJSONObject(0);
        for (int i = 0; i < itemsInSet.length(); i++) {
            JSONObject cur = itemsInSet.getJSONObject(i);
            String curId = cur.getString("id");
            if (curId.equals(id)) {
                thisItem = cur;
                break;
            }
        }

        JSONObject en = thisItem.getJSONObject("en");
        //base data
        this.name = en.getString("item_name");
        if (en.has("wiki_link")) {
            try {
                Object wikiLink = en.get("wiki_link");
                if (wikiLink instanceof String) this.wikiLink = (String) wikiLink;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.url = thisItem.getString("url_name");

        //define tags
        JSONArray tagsJSON = thisItem.getJSONArray("tags");
        boolean prime = false;
        boolean mod = false;
        if (tagsJSON.length() > 0) {
            tags = new String[tagsJSON.length()];
            for (int i = 0; i < tagsJSON.length(); i++) {
                tags[i] = tagsJSON.getString(i);
                if (!prime && tags[i].equals("prime")) prime = true;
                if (!mod && tags[i].equals("mod")) mod = true;
            }
            if (prime && !mod) {
                //this.owned = WFInfo.getItemCount(name);
                //define ducat price
                this.ducats = thisItem.getInt("ducats");

                //define drop data
                JSONArray dropSources = en.getJSONArray("drop");
                if (dropSources.length() != 0) {
                    this.relics = new String[dropSources.length()];
                    for (int i = 0; i < dropSources.length(); i++) {
                        JSONObject dropSource = dropSources.getJSONObject(i);
                        String name = dropSource.getString("name");
                        this.relics[i] = name.substring(0, name.indexOf("Relic") - 1);
                    }
                }
            }
        }
        initialized = true;
    }

    public Item(String name, int location) {
        this(name, null, location);
    }

    public Item(String name, String url, int location) {
        this.name = name;
        this.url = url;
        this.location = location;
        initialized = false;
    }

    /**
     * Gets the class that should be displayed in the table
     *
     * @param columnIndex
     * @return
     */
    public static Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0, 6, 9, 10 -> String.class;
            case 1, 2, 7, 11, 13, 14 -> Integer.class;
            case 3, 4, 5, 12 -> Double.class;
            case 8 -> Boolean.class;
            default -> Object.class;
        };
    }

    /**
     * Converts the item into a table entry based on the "column" to show
     *
     * @param columnIndex
     * @return
     */
    public Object getValueAt(int columnIndex) {
        Object val = switch (columnIndex) {
            default -> this;
            case 0 -> Utils.notNull(name, "undefined");
            case 1 -> Utils.notNull(buyPrice, 0);
            case 2 -> Utils.notNull(sellPrice, 0);
            case 3 -> Utils.notNull(profit, 0);
            case 4 -> Utils.notNull(avg48h, 0);
            case 5 -> Utils.notNull(avg90d, 0);
            case 6 -> Utils.notNull(trendName, "Even");
            case 7 -> Utils.notNull(orderCount, 0);
            case 8 -> Utils.notNull(goodBuy, false);
            case 9 -> Utils.notNull(Utils.arrayToString(relics), "");
            case 10 -> Utils.notNull(Utils.arrayToString(tags), "");
            case 11 -> ducats;
            case 12 -> calculateDucatsPerPlat();
            case 13 -> owned;
            case 14 -> getSellCount();
        };
        return val;
    }

    private Double calculateDucatsPerPlat() {
        if (ducats == null || ducats == 0) return null;
        if (Utils.notNull(sellPrice, 0) == 0 && Utils.notNull(avg48h, 0d) == 0) return 0d;
        if (Utils.notNull(sellPrice, 0) == 0) return ducats / avg48h;
        return ducats / (double) sellPrice;
    }

    //todo factor sets into account
    @CSVCalculate("Sell Count")
    public int getSellCount() {
        if (mastered) {
            return owned;
        } else if (owned > itemsInSet) {
            return owned - itemsInSet;
        } else {
            return 0;
        }
    }

    public boolean shouldSell() {
        boolean shouldSell = true;
        if (owned < 1) shouldSell = false;
        else if (owned < 2 && !mastered) shouldSell = false;
        else if ((profit != null && profit > 1) || (avg48h != null && avg48h < 7)) shouldSell = false;
        else if (profit == null || avg48h == null) shouldSell = false;
        else {
            Double doucatsPerPlat = calculateDucatsPerPlat();
            if (doucatsPerPlat != null && doucatsPerPlat > 6) {
                shouldSell = false;
            }
        }

        this.shouldSell = shouldSell;
        return shouldSell;
    }

    @Override
    public String toString() {
        return name + ": ";//+ owned;
    }

}
