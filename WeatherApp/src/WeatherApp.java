import javax.swing.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.net.URI;

public class WeatherApp extends JFrame {

    private JTextField cityField;
    private JButton searchBtn, autoBtn, refreshBtn, forecastBtn, downloadBtn;

    private JLabel cityLabel;
    private JLabel detailsLabel;
    private JLabel statusLabel;
    private JLabel backgroundLabel;

    private JProgressBar progressBar;

    private WeatherService service = new WeatherService();
    private String lastCity = "";

    private static final String PLACEHOLDER = "Enter city name...";

    public WeatherApp() {

        setTitle("Weather Application");
        setSize(600, 560);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        backgroundLabel = new JLabel();
        backgroundLabel.setBounds(0, 0, 600, 560);
        setContentPane(backgroundLabel);
        backgroundLabel.setLayout(null);
        // TITLE
        JLabel title = new JLabel("Weather Application", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setBounds(120, 15, 350, 40);
        add(title);

        // INPUT
        cityField = new JTextField();
        cityField.setBounds(120, 70, 230, 35);
        add(cityField);
        addPlaceholder(cityField, PLACEHOLDER);

        searchBtn = new JButton("Search");
        searchBtn.setBounds(360, 70, 90, 35);
        add(searchBtn);

        autoBtn = new JButton("Auto Locate");
        autoBtn.setBounds(120, 120, 120, 35);
        add(autoBtn);

        refreshBtn = new JButton("Refresh");
        refreshBtn.setBounds(250, 120, 100, 35);
        add(refreshBtn);

        // CITY + FLAG
        cityLabel = new JLabel("", SwingConstants.CENTER);
        cityLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cityLabel.setBounds(100, 180, 400, 30);
        add(cityLabel);

        // WEATHER DETAILS
        detailsLabel = new JLabel("", SwingConstants.CENTER);
        detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        detailsLabel.setBounds(100, 215, 400, 120);
        add(detailsLabel);

        // STATUS
        statusLabel = new JLabel("Enter a city to check weather", SwingConstants.CENTER);
        statusLabel.setBounds(100, 350, 400, 25);
        add(statusLabel);

        progressBar = new JProgressBar();
        progressBar.setBounds(100, 380, 400, 12);
        progressBar.setVisible(false);
        add(progressBar);

        // FORECAST
        forecastBtn = new JButton("5-Day Forecast");
        forecastBtn.setBounds(100, 420, 160, 35);
        forecastBtn.setEnabled(false);
        add(forecastBtn);

        // DOWNLOAD PDF
        downloadBtn = new JButton("Download PDF");
        downloadBtn.setBounds(300, 420, 160, 35);
        downloadBtn.setEnabled(false);
        add(downloadBtn);

        // ACTIONS
        searchBtn.addActionListener(e -> loadWeather());
        autoBtn.addActionListener(e -> autoLocate());
        refreshBtn.addActionListener(e -> refreshWeather());
        forecastBtn.addActionListener(e -> fetchForecastAndShow());
        downloadBtn.addActionListener(e -> downloadPDF());

        setVisible(true);
    }

    // SOUND CONTROL
    private Clip currentClip = null;

    private void playSoundForWeather(String mainWeather) {
        try {
            String name = mapWeatherToName(mainWeather);
            URL url = getClass().getResource("/sounds/" + name + ".wav");

            AudioInputStream audioIn = null;

            if (url != null) {
                audioIn = AudioSystem.getAudioInputStream(url);
            } else {
                // fallback to file system
                File f = new File("sounds/" + name + ".wav");
                if (f.exists()) audioIn = AudioSystem.getAudioInputStream(f);
            }

            if (audioIn == null) return;

            if (currentClip != null && currentClip.isActive()) {
                currentClip.stop();
                currentClip.close();
            }

            currentClip = AudioSystem.getClip();
            currentClip.open(audioIn);
            currentClip.start();

        } catch (Exception ignored) {
        }
    }

    // BACKGROUND LOADER
    private void loadBackgroundForWeather(String mainWeather, int hour) {
        try {
            String name = mapWeatherToName(mainWeather);

            URL url = getClass().getResource("/backgrounds/" + name + ".jpg");
            BufferedImage img = null;

            if (url != null) {
                img = ImageIO.read(url);
            } else {
                File f = new File("backgrounds/" + name + ".jpg");
                if (f.exists()) img = ImageIO.read(f);
            }

            if (img == null) return;

            Image scaled = img.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
            backgroundLabel.setIcon(new ImageIcon(scaled));
        } catch (Exception ignored) {
        }
    }

    private String mapWeatherToName(String mainWeather) {
        if (mainWeather == null) return "default";
        String w = mainWeather.toLowerCase();
        if (w.contains("clear")) return "clear";
        if (w.contains("cloud")) return "clouds";
        if (w.contains("rain")) return "rain";
        if (w.contains("snow")) return "snow";
        if (w.contains("thunder")) return "thunder";
        return "default";
    }

    // PLACEHOLDER
    private void addPlaceholder(JTextField field, String text) {

        field.setText(text);
        field.setForeground(Color.GRAY);

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(text)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(text);
                }
            }
        });
    }

    // LOAD WEATHER
    private void loadWeather() {

        String city = cityField.getText().trim();

        if (city.isEmpty() || city.equals(PLACEHOLDER)) {
            statusLabel.setText("Enter a valid city name");
            return;
        }

        lastCity = city;
        progressBar.setVisible(true);
        statusLabel.setText("Fetching weather...");
        forecastBtn.setEnabled(false);
        downloadBtn.setEnabled(false);

        new SwingWorker<WeatherData, Void>() {

            protected WeatherData doInBackground() throws Exception {
                return service.getWeather(city);
            }

            protected void done() {
                progressBar.setVisible(false);
                try {
                    WeatherData d = get();

                    cityLabel.setIcon(loadFlag(d.country));
                    cityLabel.setText(" " + city + ", " + d.country);

                    detailsLabel.setText(
                            "<html><div style='text-align:center;'>"
                                    + "Temperature: " + d.temp + " °C<br>"
                                    + "Feels Like: " + d.feels + " °C<br>"
                                    + "Humidity: " + d.humidity + "%<br>"
                                    + "Wind Speed: " + d.wind + " m/s<br>"
                                    + "Condition: " + d.desc
                                    + "</div></html>"
                    );

                        loadBackgroundForWeather(d.mainWeather, d.hour);
                        playSoundForWeather(d.mainWeather);

                        statusLabel.setText("Weather loaded successfully");
                        forecastBtn.setEnabled(true);

                } catch (Exception e) {
                    clearWeatherUI();
                    statusLabel.setText("City not found or API error");
                }
            }
        }.execute();
    }

    // FLAG LOADER
    private ImageIcon loadFlag(String countryCode) {

        try {
            ImageIcon icon =
                    new ImageIcon(getClass().getResource("/flags/" + countryCode + ".png"));
            Image img = icon.getImage().getScaledInstance(28, 18, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            return null;
        }
    }

    // AUTO LOCATE
    private void autoLocate() {

        statusLabel.setText("Detecting location...");
        progressBar.setVisible(true);

        new SwingWorker<String, Void>() {

            protected String doInBackground() throws Exception {
                return LocationService.detectCity();
            }

            protected void done() {
                progressBar.setVisible(false);
                try {
                    cityField.setForeground(Color.BLACK);
                    cityField.setText(get());
                    loadWeather();
                } catch (Exception e) {
                    statusLabel.setText("Auto locate failed");
                }
            }
        }.execute();
    }

    // REFRESH
    private void refreshWeather() {

        cityField.setForeground(Color.GRAY);
        cityField.setText(PLACEHOLDER);
        clearWeatherUI();
        lastCity = "";
        statusLabel.setText("Enter a city to check weather");
        progressBar.setVisible(false);
        forecastBtn.setEnabled(false);
        downloadBtn.setEnabled(false);
    }

    private void clearWeatherUI() {
        cityLabel.setText("");
        cityLabel.setIcon(null);
        detailsLabel.setText("");
    }

    // FORECAST
    private void fetchForecastAndShow() {

        if (lastCity.isEmpty()) return;

        progressBar.setVisible(true);
        statusLabel.setText("Fetching 5-day forecast...");

        new SwingWorker<List<ForecastData>, Void>() {

            protected List<ForecastData> doInBackground() throws Exception {
                List<ForecastData> forecast =
                        service.getFiveDayForecast(lastCity);
                service.saveForecastToDB(lastCity, forecast);
                return forecast;
            }

            protected void done() {
                progressBar.setVisible(false);
                try {
                    showForecastDialog(get());
                    statusLabel.setText("Forecast saved and displayed");
                    downloadBtn.setEnabled(true);
                } catch (Exception e) {
                    statusLabel.setText("Forecast save failed");
                }
            }
        }.execute();
    }

    private void showForecastDialog(List<ForecastData> forecast) {

        StringBuilder sb = new StringBuilder("5-Day Weather Forecast\n\n");

        for (ForecastData f : forecast) {
            sb.append(f.date).append(" | ")
              .append(f.minTemp).append("°C - ")
              .append(f.maxTemp).append("°C | ")
              .append(f.condition).append("\n");
        }

        JOptionPane.showMessageDialog(this, sb.toString(),
                "5-Day Forecast", JOptionPane.INFORMATION_MESSAGE);
    }

    //  PDF DOWNLOAD
    private void downloadPDF() {

        if (lastCity.isEmpty()) return;

        try {
            String url =
                "http://localhost/weather_api/download_forecast_pdf.php?city="
                + lastCity;

            Desktop.getDesktop().browse(new URI(url));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to download PDF");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherApp::new);
    }
}