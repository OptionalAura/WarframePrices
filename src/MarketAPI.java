import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class MarketAPI {
    //TODO store information about how many orders are available to determine what to check first
    public static final String ApiUrl = "https://api.warframe.market/v1";
    public static final String ItemsUrl = ApiUrl + "/items";
    public static final String OrdersUrl = "/orders?include=item";
    public static final String StatisticsUrl = "/statistics?include=item";
    public static HashMap<String, HashMap<String, ArrayList<Double>>> averagePriceCache = new HashMap<>();
    static HashMap<String, String> itemURLS = new HashMap<>();
    static List<String> itemNames = new ArrayList<>();

    public static Structure.Order getBestSellOffer(String name) throws IOException {
        JSONArray orders = getObject(name).getJSONArray("orders");
        Structure.Order best = null;
        if (orders.length() > 0) {
            best = new Structure.Order(orders.getJSONObject(0));
        }
        for (int i = 1; i < orders.length(); i++) {
            Structure.Order current = new Structure.Order(orders.getJSONObject(i));
            if (best == null || !best.selling && current.selling || !best.visible && current.visible || !best.user.online && current.user.online) {
                best = current;
            }

            if (current.user.online && current.visible) {
                if (current.selling) {
                    if (current.price < best.price) {
                        best = current;
                    } else if (current.price == best.price) {
                        if (current.user.reputation > best.user.reputation) {
                            best = current;
                        }
                    }
                }
            }
        }
        return best;
    }

    public static JSONObject getObject(String name) throws IOException {
        return new JSONObject(MarketAPI.GET(new URL(MarketAPI.ItemUrl(name) +
                OrdersUrl), new Request("accept", "application/json"), new Request("Platform", "pc")));
    }


    public static String GET(URL address, Request... requests) throws IOException {
        StringBuilder sb = new StringBuilder();
        HttpsURLConnection httpsConnection = (HttpsURLConnection) address.openConnection();
        httpsConnection.setRequestMethod("GET");
        for (Request request : requests) {
            httpsConnection.setRequestProperty(request.request, request.data);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream()));
        String line = br.readLine();
        if (line != null) sb.append(line);
        while ((line = br.readLine()) != null) {
            sb.append("\n").append(line);
        }
        return sb.toString();
    }

    public static String ItemUrl(String name) {
        return ItemsUrl + "/" + itemURLS.get(name);
    }

    public static Structure.Order getBestBuyOffer(String name) throws IOException {
        JSONArray orders = getObject(name).getJSONArray("orders");
        Structure.Order best = null;
        if (orders.length() > 0) {
            best = new Structure.Order(orders.getJSONObject(0));
        }
        for (int i = 1; i < orders.length(); i++) {
            Structure.Order current = new Structure.Order(orders.getJSONObject(i));
            if (best == null || best.selling && !current.selling || !best.visible && current.visible || !best.user.online && current.user.online) {
                best = current;
            }

            if (current.user.online && current.visible) {
                if (!current.selling) {
                    if (current.price > best.price) {
                        best = current;
                    } else if (current.price == best.price) {
                        if (current.user.reputation > best.user.reputation) {
                            best = current;
                        }
                    }
                }
            }
        }
        return best;
    }

    /**
     * Gets the average price of an item over the last 90 days. This will use a cached value if it is available to reduce the dependency on API
     * calls.
     *
     * @param name The name of the item
     * @return the average price of an item
     */
    public static double getAveragePrice90Days(String name) throws IOException {
        return getAveragePrice90Days(name, false);
    }

    /**
     * Gets the average price of an item over the last 90 days. This may or may not use a cached value, depending on the <code>forceUpdate</code>
     * parameter and if a value is cached.
     *
     * @param name        The name of the item
     * @param forceUpdate Whether to force an update by reading from the API
     * @return the average price of an item
     */
    private static double getAveragePrice90Days(String name, boolean forceUpdate) throws IOException {
        return Trends.mean(getPrices(name, "90days", forceUpdate));
    }

    public static boolean isCached(String name, String key){
        return averagePriceCache.containsKey(key) && averagePriceCache.get(key).containsKey(name);
    }

    private static ArrayList<Double> getPrices(String name, String key, boolean forceUpdate) throws IOException {
        //if the value is already cached, return it
        if (!forceUpdate && averagePriceCache.containsKey(key) && averagePriceCache.get(key).containsKey(name)) {
            return averagePriceCache.get(key).get(name);
        }
        //cache the value, then return it
        ArrayList<Double> prices = new ArrayList<>();
        JSONObject statistics = getItemStatistics(name);
        JSONArray period = statistics.getJSONArray(key);

        for (int i = 0; i < period.length(); i++) {
            JSONObject obj = period.getJSONObject(i);
            if (Structure.getLevel(obj) != 0) continue;
            prices.add(obj.getDouble("median"));
        }
        averagePriceCache.putIfAbsent(key, new HashMap<>());
        averagePriceCache.get(key).put(name, prices);
        return prices;
    }

    public static JSONObject getItemStatistics(String name) throws IOException {
        JSONObject obj = new JSONObject(MarketAPI.GET(new URL(MarketAPI.ItemUrl(name) +
                StatisticsUrl), new MarketAPI.Request("accept", "application/json"), new MarketAPI.Request("Platform", "pc")));
        return (JSONObject) ((JSONObject) obj.get("payload")).get("statistics_closed");
    }

    public static ArrayList<Double> getPrices90Days(String name) throws IOException {
        return getPrices90Days(name, false);
    }

    private static ArrayList<Double> getPrices90Days(String name, boolean forceUpdate) throws IOException {
        return getPrices(name, "90days", forceUpdate);
    }

    /**
     * Gets the average price of an item over the last 48 hours. This will use a cached value if it is available to reduce the dependency on API
     * calls.
     *
     * @param name The name of the item
     * @return the average price of an item
     */
    public static double getAveragePrice48Hours(String name) throws IOException {
        return getAveragePrice48Hours(name, false);
    }

    //TODO make this contain null values if there are no orders

    /**
     * Gets the average price of an item over the last 48 hours. This may or may not use a cached value, depending on the <code>forceUpdate</code>
     * parameter and if a value is cached.
     *
     * @param name        The name of the item
     * @param forceUpdate Whether to force an update by reading from the API
     * @return the average price of an item
     */
    private static double getAveragePrice48Hours(String name, boolean forceUpdate) throws IOException {
        return Trends.mean(getPrices(name, "48hours", forceUpdate));
    }

    public static ArrayList<Double> getPrices48Hours(String name) throws IOException {
        return getPrices48Hours(name, false);
    }

    private static ArrayList<Double> getPrices48Hours(String name, boolean forceUpdate) throws IOException {
        return getPrices(name, "48hours", forceUpdate);
    }

    /**
     * Gets the best buy and sell orders of an item
     *
     * @param name The name of the item
     * @return A pair containing the most optimal buy and sell order
     * @throws IOException if a connection to the host cannot be established
     */
    public static Pair<Structure.Order> getBestBuyAndSellOrders(String name) throws IOException {
        return getBestBuyAndSellOrders(getObject(name));
    }
    public static Pair<Structure.Order> getBestBuyAndSellOrders(JSONObject json) throws IOException {
        JSONArray orders = json.getJSONArray("orders");;

        //buy
        Structure.Order bestBuy = null;

        for (int i = 0; i < orders.length(); i++) {
            Structure.Order current = new Structure.Order(orders.getJSONObject(i));
            if (current.user.online && current.visible) {
                if (!current.selling && current.level == 0) {
                    if (bestBuy == null) {
                        bestBuy = current;
                    } else if (current.price > bestBuy.price) {
                        bestBuy = current;
                    } else if (current.price == bestBuy.price) {
                        if (current.user.reputation > bestBuy.user.reputation) {
                            bestBuy = current;
                        }
                    }
                }
            }
        }
        //sell
        Structure.Order bestSell = null;
        for (int i = 0; i < orders.length(); i++) {
            Structure.Order current = new Structure.Order(orders.getJSONObject(i));
            if (current.user.online && current.visible) {
                if (current.selling && current.level == 0) {
                    if (bestSell == null) bestSell = current;
                    if (current.price < bestSell.price) {
                        bestSell = current;
                    } else if (current.price == bestSell.price) {
                        if (current.user.reputation > bestSell.user.reputation) {
                            bestSell = current;
                        }
                    }
                }
            }
        }
        return new Pair<>(bestBuy, bestSell);

    }
    public static void loadItems() {
        try {
            String get = GET(new URL("https://api.warframe.market/v1/items"));
            JSONObject obj = (JSONObject) new JSONObject(get).get("payload");
            JSONArray items = obj.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject JSONItem = (JSONObject) items.get(i);
                String name = JSONItem.getString("item_name");
                String url = JSONItem.getString("url_name");
                itemURLS.put(name, url);
                itemNames.add(name);
                Item item = new Item(name, i);
                Item.register(item);
            }
            itemNames.sort(Comparator.naturalOrder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final class Pair<T> {
        public final T left;
        public final T right;

        public Pair(T left, T right) {
            this.left = left;
            this.right = right;
        }
    }

    public record Request(String request, String data) {}
}