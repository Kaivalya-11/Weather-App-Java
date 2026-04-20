import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class LocationService {

    public static String detectCity() throws Exception {

        URL url = new URI("http://ip-api.com/json").toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream()));

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        String json = sb.toString();

        return extractString(json, "city");
    }

    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        return "";
    }
}
