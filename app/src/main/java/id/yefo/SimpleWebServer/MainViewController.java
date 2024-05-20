/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.yefo.SimpleWebServer;

// Class2 dari lib javafx yg diperlukan untuk elemen2 GUI
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

// Class2 untuk operasi files, networking, formatting tanggal/date
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.Properties;

// Untuk HTTP Server
import com.sun.net.httpserver.HttpServer;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 *
 * @author ACER
 */
public class MainViewController {
    @FXML
    private TextField portField;

    @FXML
    private TextField webDirectoryField;

    @FXML
    private TextField logDirectoryField;

    @FXML
    private Button toggleButton;

    @FXML
    private TextArea logArea;

    private HttpServer server;
    private Path webDirectory = Paths.get("D:/Web"); // default dir
    private Path logDirectory = Paths.get("D:/Web/logs"); // default dir
    private int port = 14687; // default port
    private StringBuilder accessLogs = new StringBuilder();
    private static final String CONFIG_FILE = "config.properties";
    
    @FXML
    private void initialize() {
        // Inisialisasi teks field port, web directory, dan logs directory
        loadConfig();
        portField.setText(String.valueOf(port));
        webDirectoryField.setText(webDirectory.toString());
        logDirectoryField.setText(logDirectory.toString());
    }

    @FXML
    private void handleBrowseWebDirectory(){
        // Membuat objek DirectoryChooser supaya user dapat memilih directory
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Web Directory");
        // Membuka file selection dialog yg akan disimpan di selectedDirectory
        File selectedDirectory = directoryChooser.showDialog(new Stage());
        if (selectedDirectory != null){
            // Convert objek File yg dipilih menjadi objek Path
            webDirectory = Paths.get(selectedDirectory.getAbsolutePath());
            // Update teks pada UI field dengan dir yg dipilih
            webDirectoryField.setText(webDirectory.toString());
            saveConfig();
        }
    }

    @FXML
    private void handleBrowseLogDirectory(){ // Konsep sama dengan web directory
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Log Directory");
        File selectedDirectory = directoryChooser.showDialog(new Stage());
        if (selectedDirectory != null) {
            logDirectory = Paths.get(selectedDirectory.getAbsolutePath());
            logDirectoryField.setText(logDirectory.toString());
            saveConfig();
        }
    }

    @FXML
    private void handleToggleServer(){
        if (server == null){
            // Jika server blm ada, maka server dinyalakan
            startServer();
        }
        else{
            // Jika ada, maka dimatikan
            stopServer();
        }
    }
    
    // Menambahkan aktivitas pada log
    private void appendLog(String message) {
        logArea.appendText(message + "\n");
    }

    private void startServer() {
        try {
            // Mendapatkan port dari field port
            port = Integer.parseInt(portField.getText());
            
            // Membuat instansi HttpServer dari port di atas dengan backlog 0
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Mendefinisikan context handler dari path root '/'
            server.createContext("/", exchange -> {
                // Mendapatkan path yg diminta(requested) dari HTTP request
                String requestedPath = exchange.getRequestURI().getPath();
                appendLog("Requested Path: " + requestedPath);
                
                // Mendapatkan path relatif dari webDirectory
                Path filePath = webDirectory.resolve(requestedPath.substring(1));
                
                // Mengecek apakah metode requestnya GET
                if ("GET".equals(exchange.getRequestMethod())){
                    logAccess(exchange);
                }
                
                // Mengecek apakah 'filepath' directory yang valid dalam server
                if (Files.isDirectory(filePath)) {
                    // Mendapatkan path index.html relatif dari filePath
                    Path indexPath = filePath.resolve("index.html");
                    if (Files.exists(indexPath)) {
                        filePath = indexPath;
                        String redirectURL;
                        // Redirect to index.html
                        if(requestedPath == "/"){
                            redirectURL = "http://localhost:" + port + requestedPath + (requestedPath.endsWith("/") ? "" : "/") + "index.html";
                        }
                        else{
                            redirectURL = requestedPath + (requestedPath.endsWith("/") ? "" : "/") + "index.html";
                        }
                        exchange.getResponseHeaders().set("Location", redirectURL);
                        exchange.sendResponseHeaders(302, -1);
                        return;
                    }
                    else{
                        // Mengirim list directory jika tidak ada index.html
                        appendLog("Directory listing for: " + filePath.toString());
                        StringBuilder directoryListing = new StringBuilder("<html><body><ul>");
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(filePath)){
                            directoryListing.append("<h1>Directory list</h1>")
                                            .append("<h2>")
                                            .append(filePath.toString())
                                            .append("</h2>");
                            for (Path entry : stream) {
                                directoryListing.append("<li><a href=\"")
                                                .append(requestedPath)
                                                .append(requestedPath.endsWith("/") ? "" : "/")
                                                .append(entry.getFileName().toString())
                                                .append("\">")
                                                .append(entry.getFileName().toString())
                                                .append("</a></li>");
                            }
                        }
                        directoryListing.append("</ul></body></html>");
                        byte[] data = directoryListing.toString().getBytes();
                        exchange.sendResponseHeaders(200, data.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(data);
                        }
                        return;
                    }
                }
                
                appendLog("Resolved File Path: " + filePath.toString());
                
                if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    byte[] data = Files.readAllBytes(filePath);
                    exchange.sendResponseHeaders(200, data.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(data);
                    }
                } else {
                    String notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes());
                    }
                }
                logAccess(exchange);
            });
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
            appendLog("Server started on port " + port);
            toggleButton.setText("Stop Server");
            saveConfig();
        } catch (IOException | NumberFormatException e) {
            appendLog("Failed to start server: " + e.getMessage());
        }
    }

    private void stopServer(){
        if (server != null){
            server.stop(0);
            server = null;
            appendLog("Server stopped");
            saveLogToFile(accessLogs.toString());
            toggleButton.setText("Start Server");
        }
    }
    
    private void logAccess(com.sun.net.httpserver.HttpExchange exchange){
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String method = exchange.getRequestMethod();
        String url = exchange.getRequestURI().getPath();
        String ipAddress = exchange.getRemoteAddress().getAddress().toString();
        String logMessage = timestamp + " - " + method + " - " + ipAddress + " - " + url;
        logArea.appendText(logMessage + "\n");
        accessLogs.append(logMessage).append("\n");
    }
    
    private void saveLogToFile(String logMessage) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String fileName = "access_log_" + sdf.format(new Date()) + ".txt";
            Path logFile = logDirectory.resolve(fileName);
            Files.write(logFile, logMessage.getBytes(), Files.exists(logFile) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException e) {
            appendLog("Error saving log to file: " + e.getMessage());
        }
    }
    
    private void saveConfig(){
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(port));
        props.setProperty("webDirectory", webDirectory.toString());
        props.setProperty("logDirectory", logDirectory.toString());
        try(OutputStream output = new FileOutputStream(CONFIG_FILE)){
            props.store(output, null);
            appendLog("Configuration saved");
        }
        catch(IOException e){
            appendLog("Error saving configuration: " + e.getMessage());
        }
    }
    
    private void loadConfig(){
        Properties props = new Properties();
        try(FileInputStream input = new FileInputStream(CONFIG_FILE)){
            props.load(input);
            port = Integer.parseInt(props.getProperty("port", "14687"));
            webDirectory = Paths.get(props.getProperty("webDirectory", "D:/Web"));
            logDirectory = Paths.get(props.getProperty("logDirectory", "D:/Web/logs"));
            appendLog("Configuration loaded");
        }
        catch(IOException | NumberFormatException e){
            appendLog("Error loading configuration: " + e.getMessage());
        }
    }
}
