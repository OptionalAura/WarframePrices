import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class DelayedThreadQueue extends Thread{
    private final ArrayDeque<Item> itemQueue;
    private long delay;
    private volatile boolean shouldRun = true;
    private volatile boolean paused = false;
    private final Application app;
    public DelayedThreadQueue(long delay, final Application application) {
        itemQueue = new ArrayDeque<>();
        this.delay = delay;
        this.app = application;
    }
    public void pushTask(Item r){
        itemQueue.push(r);
    }
    public ArrayDeque<Item> getQueue(){
        return itemQueue;
    }
    public void pushTask(String name, int i){
        Item item = new Item(name, i);
        pushTask(item);
    }
    public void addTask(Item r){
        itemQueue.add(r);
    }
    @Override
    public void run() {
        while(shouldRun) {
            if(!paused) {
                Item item = itemQueue.pollLast();
                if (item != null) {
                    queueTask(item);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            onSpinWait();
        }
    }
    public void purgeQueue(){
        itemQueue.clear();
    }
    public void setPaused(boolean paused){
        this.paused = paused;
    }
    public boolean isPaused(){
        return this.paused;
    }
    public void terminate(){
        shouldRun = false;
    }
    public void queueTask(Item item){
        MarketAPI.Pair<Structure.Order> order = null;
        try {
            String name = item.name;
            int loc = item.location;
            boolean avgCached = MarketAPI.isCached(name, "90days");
            JSONObject json = MarketAPI.getObject(name);
            order = MarketAPI.getBestBuyAndSellOrders(json.getJSONObject("payload"));
            if(!item.initialized){
                item = new Item(json.getJSONObject("include").getJSONObject("item"));
                item.location = loc;
            }

            Thread.sleep(400);
            double avg90d = Math.round(MarketAPI.getAveragePrice90Days(name) * 100) / 100d;
            double avg48h = Math.round(MarketAPI.getAveragePrice48Hours(name) * 100) / 100d;
            ArrayList<Double> orderPrices = MarketAPI.getPrices90Days(name);

            int trend = Trends.getLinearTrend(orderPrices).getDirection();
            if(!avgCached)
                Thread.sleep(400);
            //if the item is new or has too few orders, try to get orders from a shorter time span
            if(orderPrices.size() < 60){
                orderPrices = MarketAPI.getPrices48Hours(name);
            }
            String trendName = "Even";
            if(trend == -1)
                trendName = "Decreasing";
            else if(trend == 1)
                trendName = "Increasing";
            Integer profit = null;
            if (order.left != null && order.right != null) {
                profit = (int) Math.max(order.left.price - order.right.price, Math.min(avg90d, avg48h) - order.right.price);
            } else {
                if(order.right != null){
                    profit = (int) Math.min(avg90d, avg48h) - order.right.price;
                }
            }

            item.buyOrder = order.left;
            item.sellOrder = order.right;
            item.trendName = trendName;
            item.profit = profit;
            item.buyPrice = order.left == null ? null : order.left.price;
            item.sellPrice = order.right == null ? null : order.right.price;
            item.goodBuy = profit != null && profit > 3 && orderPrices.size() > 30 && trend == 1;
            item.avg48h = avg48h;
            item.avg90d = avg90d;
            item.orderCount = orderPrices.size();

            app.getWindow().getTableModel().getDataVector().set(item.location, item);
            app.getWindow().getTableModel().fireTableRowsUpdated(item.location, item.location);
            //});

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        pushTask(item);
    }
}
