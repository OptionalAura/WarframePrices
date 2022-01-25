import java.util.List;

public class Utils {
    public static String arrayToString(Object[] array){
        if(array == null)
            return "null";
        if(array.length == 0){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(array[0].toString());
        for(int i = 1; i < array.length; i++){
            sb.append(", ").append(array[i].toString());
        }
        return sb.toString();
    }
    public static String listToString(List<?> array){
        if(array == null)
            return "null";
        if(array.size() == 0){
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(array.get(0).toString());
        for(int i = 1; i < array.size(); i++){
            sb.append(", ").append(array.get(i).toString());
        }
        sb.append(']');
        return sb.toString();
    }
    public static boolean containsIgnoreCase(String src, String search){
        if(search.length() == 0)
            return true;
        if(search.length() > src.length()){
            return false;
        }
        for(int i = src.length() - search.length(); i >= 0; i--){
            boolean matches = true;
            for(int j = 0; j < search.length(); j++){
                if(Character.toLowerCase(src.charAt(i+j)) != Character.toLowerCase(search.charAt(j))){
                    matches = false;
                    break;
                }
            }
            if(matches){
                return true;
            }
        }
        return false;
    }
    public static <T> T notNull(T obj, T def){
        return obj == null ? def : obj;
    }
}
