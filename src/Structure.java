import org.json.JSONObject;

public class Structure {
    public static int getLevel(JSONObject json){
        if(json.has("subtype")){
            return switch (json.getString("subtype")) {
                    case "intact" -> 0;
                    case "exceptional" -> 1;
                    case "flawless" -> 2;
                    case "radiant" -> 3;
                    default -> 5;
                };
        } else if (json.has("mod_rank")){
            return json.getInt("mod_rank");
        }
        return 0;
    }
    public static class Order {
        int price;
        int quantity;
        int level;
        boolean selling;
        boolean visible;
        UserShort user;
        public Order(int price, int quantity, boolean selling, UserShort user, boolean visible){
            this.price = price;
            this.quantity = quantity;
            this.selling = selling;
            this.user = user;
            this.visible = visible;
        }
        public Order(JSONObject json){
            this.price = json.getInt("platinum");
            this.quantity = json.getInt("quantity");
            this.selling = json.getString("order_type").equals("sell");
            this.user = new UserShort(json.getJSONObject("user"));
            this.visible = json.getBoolean("visible");
            this.level = getLevel(json);
        }
        @Override
        public String toString(){
            return "[Price: " + price + ", Quantity: " + quantity + ", Selling: " + selling + ", Visible: " + visible + ", User: " + user + "]";
        }
    }
    public static class UserShort{
        String name;
        int reputation;
        boolean online;
        public UserShort(String name, boolean online, int reputation){
            this.name = name;
            this.reputation = reputation;
        }
        public UserShort(JSONObject json){
            this.name = json.getString("ingame_name");
            this.reputation = json.getInt("reputation");
            this.online = json.getString("status").equals("ingame");
        }
        @Override
        public String toString(){
            return "[" + name + ", Reputation: " + reputation + ", Online: " + online + "]";
        }
    }
}
