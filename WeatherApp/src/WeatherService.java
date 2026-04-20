import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherService {

    
    private static final String API_KEY = "137d0df46e535b17c2008db2d001346e";

    // Parth
    public WeatherData getWeather(String city) throws Exception {

        String urlStr =
                "https://api.openweathermap.org/data/2.5/weather?q="
                        + city
                        + "&units=metric&appid="
                        + API_KEY;

        JSONObject json = readJsonFromUrl(urlStr);

        WeatherData data = new WeatherData();
        data.country = json.getJSONObject("sys").getString("country");
        data.temp = json.getJSONObject("main").getDouble("temp");
        data.feels = json.getJSONObject("main").getDouble("feels_like");
        data.humidity = json.getJSONObject("main").getInt("humidity");
        data.wind = json.getJSONObject("wind").getDouble("speed");
        data.mainWeather = json.getJSONArray("weather")
                .getJSONObject(0)
                .getString("main");
        data.desc = json.getJSONArray("weather")
                .getJSONObject(0)
                .getString("description");
        
        data.hour = LocalTime.now().getHour();

        return data;
    }

    // 5-DAY FORECAST
    public List<ForecastData> getFiveDayForecast(String city) throws Exception {

        String urlStr =
                "https://api.openweathermap.org/data/2.5/forecast?q="
                        + city
                        + "&units=metric&appid="
                        + API_KEY;

        JSONObject json = readJsonFromUrl(urlStr);
        JSONArray list = json.getJSONArray("list");

        List<ForecastData> forecastList = new ArrayList<>();

        // Kaivalya
        LocalDate today = LocalDate.now();
        int dayOffset = 1;

        for (int i = 0; i < list.length() && dayOffset <= 5; i += 8) {

            JSONObject obj = list.getJSONObject(i);

            ForecastData f = new ForecastData();
            f.date = today.plusDays(dayOffset).toString();
            f.minTemp = obj.getJSONObject("main").getDouble("temp_min");
            f.maxTemp = obj.getJSONObject("main").getDouble("temp_max");
            f.condition = obj.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main");

            forecastList.add(f);
            dayOffset++;
        }

        return forecastList;
    }

    // SAVE FORECAST TO DATABASE
    public void saveForecastToDB(String city, List<ForecastData> forecast) throws Exception {

        URL url = new URL("http://localhost/weather_api/save_forecast.php");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        JSONObject payload = new JSONObject();
        payload.put("city", city);

        JSONArray forecastArray = new JSONArray();
        for (ForecastData f : forecast) {
            JSONObject obj = new JSONObject();
            obj.put("date", f.date);
            obj.put("min", f.minTemp);
            obj.put("max", f.maxTemp);
            obj.put("condition", f.condition);
            forecastArray.put(obj);
        }

        payload.put("forecast", forecastArray);

        OutputStream os = con.getOutputStream();
        os.write(payload.toString().getBytes());
        os.flush();
        os.close();

        int responseCode = con.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Server error: " + responseCode);
        }
    }

    // HELPER METHOD
    private JSONObject readJsonFromUrl(String urlStr) throws Exception {

        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader br =
                new BufferedReader(new InputStreamReader(con.getInputStream()));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        br.close();
        return new JSONObject(sb.toString());
    }
}