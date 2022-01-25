import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("SerializableHasSerializationMethods")
public class Item implements Serializable {
    @Serial
    private static final long serialVersionUID = 5;
    public static HashMap<String, Item> items = new HashMap<>();

    //mandatory parameters
    public String name;
    public String url;
    public String[] tags;
    public transient String wikiLink;

    //for prime items
    public String[] relics;
    public Integer ducats;

    //for mods
    public Integer maxRank;
    public Integer orderCount;

    //keeps track of if this was initialized with JSON or not
    public boolean initialized;

    //data for the table
    transient int location;
    transient Structure.Order buyOrder;
    transient Structure.Order sellOrder;
    transient String trendName;
    transient Integer profit;
    transient Integer buyPrice;
    transient Integer sellPrice;
    transient boolean goodBuy;
    Double avg90d;
    Double avg48h;

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
        //this.wikiLink = en.getString("wiki_link");
        this.url = thisItem.getString("url_name");

        //define tags
        JSONArray tagsJSON = thisItem.getJSONArray("tags");
        boolean prime = false;
        boolean mod = false;
        tags = new String[tagsJSON.length()];
        for (int i = 0; i < tagsJSON.length(); i++) {
            tags[i] = tagsJSON.getString(i);
            if (!prime && tags[i].equals("prime")) {
                prime = true;
            }
            if (!mod && tags[i].equals("mod")) {
                mod = true;
            }
        }
        if (prime && !mod) {
            //define ducat price
            this.ducats = thisItem.getInt("ducats");

            //define drop data
            JSONArray dropSources = en.getJSONArray("drop");
            if (dropSources.length() != 0) {
                this.relics = new String[dropSources.length()];
                for (int i = 0; i < dropSources.length(); i++) {
                    JSONObject dropSource = dropSources.getJSONObject(i);
                    String name = dropSource.getString("name");
                    this.relics[i] = name.substring(0, name.indexOf("Relic")-1);
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

    public static void register(Item item) {
        items.put(item.name, item);
    }

    public static Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0, 6, 9, 10 -> String.class;
            case 1, 2, 7, 11 -> Integer.class;
            case 3, 4, 5, 12 -> Double.class;
            case 8 -> Boolean.class;
            default -> Object.class;
        };
    }

    public Object getValueAt(int columnIndex) {
        return switch (columnIndex) {
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
            case 11 -> Utils.notNull(ducats, 0);
            case 12 -> calculateDucatsPerPlat();
        };
    }

    private double calculateDucatsPerPlat() {
        if(ducats == null || ducats == 0)
            return 0;
        if(Utils.notNull(sellPrice, 0) == 0 && Utils.notNull(avg48h, 0d) == 0)
            return 0;
        if(Utils.notNull(sellPrice, 0) == 0)
            return ducats/avg48h;
        return ducats/(double)sellPrice;
    }
}
