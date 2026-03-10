package tom.turmtechnik;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Einfacher HTTP-Server ohne externe Bibliotheken.
 * Implementiert mit Java ServerSocket.
 */
public class SimpleHttpServer {
    private static final String TAG = "SimpleHttpServer";
    private static final int DEFAULT_PORT = 8080;
    
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean isRunning = false;
    private HttpRequestHandler requestHandler;
    
    public interface HttpRequestHandler {
        HttpResponse handleRequest(HttpRequest request);
    }
    
    public static class HttpRequest {
        public String method;
        public String uri;
        public Map<String, String> headers = new HashMap<>();
        public String body;
        public byte[] bodyBytes;  // Für binäre Daten (z.B. multipart/form-data)
    }
    
    public static class HttpResponse {
        public int statusCode;
        public String contentType;
        public String body;
        /** Binär-Body (z.B. Datei-Download); wenn gesetzt, wird dies statt body gesendet. */
        public byte[] bodyBytes;
        public Map<String, String> headers = new HashMap<>();
        
        public HttpResponse(int statusCode, String contentType, String body) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
        }
    }
    
    public SimpleHttpServer(int port) {
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    public void setRequestHandler(HttpRequestHandler handler) {
        this.requestHandler = handler;
    }
    
    /** Startet den Server (Bindung an alle Schnittstellen, Port 8080). */
    public void start() throws IOException {
        if (isRunning) {
            Log.w(TAG, "Server läuft bereits");
            return;
        }
        serverSocket = new ServerSocket();
        try {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress("0.0.0.0", port), 50);
        } catch (IOException e) {
            serverSocket.close();
            throw e;
        }
        isRunning = true;
        executorService.execute(() -> {
            Log.i(TAG, "HTTP-Server gestartet auf Port " + port);
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.execute(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        Log.e(TAG, "Fehler beim Akzeptieren von Verbindungen", e);
                    }
                }
            }
        });
    }
    
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Schließen des Servers", e);
        }
        executorService.shutdown();
        Log.i(TAG, "HTTP-Server gestoppt");
    }
    
    private void handleClient(Socket clientSocket) {
        java.io.BufferedInputStream bufferedInput = null;
        BufferedReader reader = null;
        try {
            InputStream rawInputStream = clientSocket.getInputStream();
            bufferedInput = new java.io.BufferedInputStream(rawInputStream, 65536);
            bufferedInput.mark(65536); // Markiere Position für Reset
            reader = new BufferedReader(new InputStreamReader(bufferedInput, "ISO-8859-1"));
            OutputStream outputStream = clientSocket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);
            
            // Lese Request-Line
            String requestLine = reader.readLine();
            if (requestLine == null) {
                clientSocket.close();
                return;
            }
            
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendErrorResponse(writer, outputStream, 400, "Bad Request");
                clientSocket.close();
                return;
            }
            
            String method = parts[0];
            String uri = parts[1];
            
            // Lese Headers
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String key = line.substring(0, colonIndex).trim().toLowerCase();
                    String value = line.substring(colonIndex + 1).trim();
                    headers.put(key, value);
                }
            }
            
            // Lese Body (falls vorhanden)
            String body = null;
            byte[] bodyBytes = null;
            String contentLengthStr = headers.get("content-length");
            String contentType = headers.get("content-type");
            
            if (contentLengthStr != null) {
                try {
                    int contentLength = Integer.parseInt(contentLengthStr);
                    if (contentLength > 0) {
                        // Für multipart/form-data: Lese direkt als Bytes (binäre Daten)
                        if (contentType != null && contentType.startsWith("multipart/form-data")) {
                            // Reset zum Anfang und lese alles als Bytes
                            bufferedInput.reset();
                            
                            // Lese Request-Line und Headers nochmal (um Position zu finden)
                            byte[] allRequestBytes = new byte[65536];
                            int totalBytesRead = bufferedInput.read(allRequestBytes);
                            
                            // Finde Ende der Headers (doppeltes CRLF oder LF-LF)
                            int headerEndIndex = -1;
                            for (int i = 0; i < totalBytesRead - 3; i++) {
                                if (allRequestBytes[i] == '\r' && allRequestBytes[i+1] == '\n' &&
                                    allRequestBytes[i+2] == '\r' && allRequestBytes[i+3] == '\n') {
                                    headerEndIndex = i + 4;
                                    break;
                                }
                            }
                            if (headerEndIndex < 0) {
                                for (int i = 0; i < totalBytesRead - 1; i++) {
                                    if (allRequestBytes[i] == '\n' && allRequestBytes[i+1] == '\n') {
                                        headerEndIndex = i + 2;
                                        break;
                                    }
                                }
                            }
                            if (headerEndIndex < 0) {
                                headerEndIndex = 0;
                            }
                            bodyBytes = new byte[contentLength];
                            int inBuffer = Math.min(contentLength, totalBytesRead - headerEndIndex);
                            System.arraycopy(allRequestBytes, headerEndIndex, bodyBytes, 0, inBuffer);
                            if (inBuffer < contentLength) {
                                int totalRead = inBuffer;
                                while (totalRead < contentLength) {
                                    int n = bufferedInput.read(bodyBytes, totalRead, contentLength - totalRead);
                                    if (n <= 0) break;
                                    totalRead += n;
                                }
                            }
                            body = new String(bodyBytes, "ISO-8859-1");
                        } else {
                            // PUT/POST mit Body: Reset + Byte-Lesung, da BufferedReader bereits Body-Bytes verbraucht hat (sonst "Failed to fetch" beim Speichern)
                            bufferedInput.reset();
                            int needRead = 8192 + contentLength;
                            byte[] allRequestBytes = new byte[Math.min(65536, needRead)];
                            int totalBytesRead = bufferedInput.read(allRequestBytes);
                            int headerEndIndex = -1;
                            for (int i = 0; i < totalBytesRead - 3; i++) {
                                if (allRequestBytes[i] == '\r' && allRequestBytes[i+1] == '\n' &&
                                    allRequestBytes[i+2] == '\r' && allRequestBytes[i+3] == '\n') {
                                    headerEndIndex = i + 4;
                                    break;
                                }
                            }
                            if (headerEndIndex < 0) {
                                for (int i = 0; i < totalBytesRead - 1; i++) {
                                    if (allRequestBytes[i] == '\n' && allRequestBytes[i+1] == '\n') {
                                        headerEndIndex = i + 2;
                                        break;
                                    }
                                }
                            }
                            if (headerEndIndex < 0) headerEndIndex = 0;
                            bodyBytes = new byte[contentLength];
                            int inBuffer = Math.min(contentLength, totalBytesRead - headerEndIndex);
                            System.arraycopy(allRequestBytes, headerEndIndex, bodyBytes, 0, inBuffer);
                            if (inBuffer < contentLength) {
                                int totalRead = inBuffer;
                                while (totalRead < contentLength) {
                                    int n = bufferedInput.read(bodyBytes, totalRead, contentLength - totalRead);
                                    if (n <= 0) break;
                                    totalRead += n;
                                }
                            }
                            String charset = "ISO-8859-1";
                            if (contentType != null && (contentType.toLowerCase().contains("application/json") || contentType.toLowerCase().contains("charset=utf-8"))) {
                                charset = "UTF-8";
                            }
                            try {
                                body = new String(bodyBytes, charset);
                            } catch (java.io.UnsupportedEncodingException e) {
                                body = new String(bodyBytes, "ISO-8859-1");
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Ungültige Content-Length: " + contentLengthStr);
                } catch (Exception e) {
                    Log.e(TAG, "Fehler beim Lesen des Body", e);
                }
            }
            
            // Erstelle Request-Objekt
            HttpRequest request = new HttpRequest();
            request.method = method;
            request.uri = uri;
            request.headers = headers;
            request.body = body;
            request.bodyBytes = bodyBytes;
            
            // Verarbeite Request
            HttpResponse response;
            if (requestHandler != null) {
                response = requestHandler.handleRequest(request);
            } else {
                response = new HttpResponse(404, "text/plain", "404 - Not Found");
            }
            
            // Sende Response
            sendResponse(writer, outputStream, response);
            
            clientSocket.close();
            
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Verarbeiten der Client-Verbindung", e);
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Ignorieren
            }
        }
    }
    
    private void sendResponse(PrintWriter writer, OutputStream outputStream, HttpResponse response) {
        try {
            // Status-Line
            String statusText = getStatusText(response.statusCode);
            writer.println("HTTP/1.1 " + response.statusCode + " " + statusText);
            
            int contentLength;
            if (response.bodyBytes != null) {
                contentLength = response.bodyBytes.length;
            } else {
                contentLength = (response.body != null) ? response.body.getBytes("UTF-8").length : 0;
            }
            writer.println("Content-Type: " + response.contentType);
            writer.println("Content-Length: " + contentLength);
            writer.println("Access-Control-Allow-Origin: *");
            writer.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
            writer.println("Access-Control-Allow-Headers: Content-Type");
            
            // Zusätzliche Headers
            for (Map.Entry<String, String> entry : response.headers.entrySet()) {
                writer.println(entry.getKey() + ": " + entry.getValue());
            }
            
            writer.println(); // Leerzeile zwischen Headers und Body
            writer.flush();
            
            // Body
            if (response.bodyBytes != null) {
                outputStream.write(response.bodyBytes);
                outputStream.flush();
            } else if (response.body != null && !response.body.isEmpty()) {
                outputStream.write(response.body.getBytes("UTF-8"));
                outputStream.flush();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Senden der Response", e);
        }
    }
    
    private void sendErrorResponse(PrintWriter writer, OutputStream outputStream, 
                                  int statusCode, String message) {
        HttpResponse response = new HttpResponse(statusCode, "text/plain", message);
        sendResponse(writer, outputStream, response);
    }
    
    private String getStatusText(int statusCode) {
            switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 302: return "Found";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            default: return "Unknown";
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
}
