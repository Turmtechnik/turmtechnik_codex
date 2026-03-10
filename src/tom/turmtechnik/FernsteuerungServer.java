package tom.turmtechnik;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP-Server auf eigenem Port (z. B. 8081) für die Fernsteuerung.
 * Leitet alle Anfragen an den ConfigWebServer (8080) weiter – dieselben Layout-Seiten, dieselben Tasten und Funktionen.
 * "/" leitet auf die App-Seite 1 (Layout) weiter.
 */
public class FernsteuerungServer {

    private static final String TAG = "FernsteuerungServer";
    public static final int DEFAULT_PORT = 8081;

    private final int port;
    private final android.content.Context context;
    private final String backendHostPort;
    private SimpleHttpServer httpServer;

    public FernsteuerungServer(int port, android.content.Context context, String backendHostPort) {
        this.port = port > 0 ? port : DEFAULT_PORT;
        this.context = context;
        this.backendHostPort = backendHostPort != null && !backendHostPort.isEmpty() ? backendHostPort : "127.0.0.1:8080";
    }

    public void start() throws java.io.IOException {
        if (httpServer != null) {
            Log.w(TAG, "Fernsteuerung-Server läuft bereits auf Port " + port);
            return;
        }
        httpServer = new SimpleHttpServer(port);
        httpServer.setRequestHandler(this::handleRequest);
        httpServer.start();
        Log.i(TAG, "Fernsteuerung-Server gestartet auf Port " + port + " (Backend: " + backendHostPort + ")");
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
            Log.i(TAG, "Fernsteuerung-Server gestoppt");
        }
    }

    public boolean isRunning() {
        return httpServer != null && httpServer.isRunning();
    }

    private SimpleHttpServer.HttpResponse handleRequest(SimpleHttpServer.HttpRequest request) {
        String uri = request.uri != null ? request.uri.trim() : "";
        if (uri.isEmpty()) uri = "/";
        String method = request.method != null ? request.method : "GET";

        if ("OPTIONS".equals(method)) {
            return new SimpleHttpServer.HttpResponse(200, "text/plain", "");
        }

        if (uri.startsWith("/api/")) {
            return proxyToBackend(request);
        }

        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
        if ("/".equals(path) || "/index.html".equals(path)) {
            SimpleHttpServer.HttpResponse redirect = new SimpleHttpServer.HttpResponse(302, "text/plain", "");
            redirect.headers.put("Location", "/app-seite1.html");
            return redirect;
        }
        if ("/layout1".equals(path) || "/layout1.html".equals(path)) {
            SimpleHttpServer.HttpResponse redirect = new SimpleHttpServer.HttpResponse(302, "text/plain", "");
            redirect.headers.put("Location", "/app-seite1.html");
            return redirect;
        }
        if ("/layout2".equals(path) || "/layout2.html".equals(path)) {
            SimpleHttpServer.HttpResponse redirect = new SimpleHttpServer.HttpResponse(302, "text/plain", "");
            redirect.headers.put("Location", "/app-seite2.html");
            return redirect;
        }
        SimpleHttpServer.HttpResponse response = proxyToBackend(request);
        if ("GET".equals(method) && ("/app-seite1.html".equals(path) || "/app-seite2.html".equals(path))
            && response.statusCode == 200 && response.body != null
            && response.contentType != null && response.contentType.toLowerCase().contains("text/html")) {
            String body = response.body;
            String style = "<style id=\"fernsteuerung-hide-header\">.tt-header,.app-header-center,.app-uhr,#uhrWrap,.app-header-left,.app-header-right{display:none !important;height:0 !important;min-height:0 !important;overflow:hidden !important;margin:0 !important;padding:0 !important;border:none !important}.app-seite-wrap{padding-top:0 !important;padding-left:max(env(safe-area-inset-left),0.5rem) !important;padding-right:max(env(safe-area-inset-right),0.5rem) !important}.app-page-main{padding-top:0 !important;padding-left:0.5rem !important;padding-right:0.5rem !important}.app-grid-wrap{padding-top:0 !important}body.app-seite-page{padding-top:0 !important}</style>";
            int headEnd = body.toLowerCase().indexOf("</head>");
            if (headEnd >= 0) {
                body = body.substring(0, headEnd) + style + body.substring(headEnd);
                return new SimpleHttpServer.HttpResponse(200, response.contentType, body);
            }
        }
        return response;
    }

    private SimpleHttpServer.HttpResponse proxyToBackend(SimpleHttpServer.HttpRequest request) {
        String uri = request.uri != null ? request.uri : "";
        String method = request.method != null ? request.method : "GET";
        String backendUrl = "http://" + backendHostPort + uri;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(backendUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            for (Map.Entry<String, String> h : request.headers.entrySet()) {
                String k = h.getKey();
                if (k != null && !k.equalsIgnoreCase("host") && !k.equalsIgnoreCase("connection")) {
                    conn.setRequestProperty(k, h.getValue());
                }
            }
            if (method.equals("POST") || method.equals("PUT")) {
                byte[] bodyBytes = null;
                if (request.body != null && !request.body.isEmpty()) {
                    bodyBytes = request.body.getBytes(StandardCharsets.UTF_8);
                } else if (request.bodyBytes != null && request.bodyBytes.length > 0) {
                    bodyBytes = request.bodyBytes;
                }
                if (bodyBytes != null && bodyBytes.length > 0) {
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
                    if (conn.getRequestProperty("Content-Type") == null || conn.getRequestProperty("Content-Type").isEmpty()) {
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    }
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bodyBytes);
                    }
                } else if (uri.contains("/api/") && (uri.contains("sofort") || uri.contains("verknuepfte") || uri.contains("control"))) {
                    Log.w(TAG, "Proxy POST/PUT ohne Body: " + method + " " + uri + " (Backend erhält evtl. leeren Body)");
                }
            }
            int code = conn.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = readFully(stream != null ? stream : new java.io.ByteArrayInputStream(new byte[0]));
            String contentType = conn.getContentType();
            if (contentType == null || contentType.isEmpty()) contentType = "application/json";
            if (contentType != null && contentType.toLowerCase().contains("application/json") && !contentType.toLowerCase().contains("charset")) contentType = contentType + "; charset=UTF-8";
            conn.disconnect();
            return new SimpleHttpServer.HttpResponse(code, contentType, body);
        } catch (Exception e) {
            Log.e(TAG, "Proxy fehlgeschlagen: " + backendUrl, e);
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
            return new SimpleHttpServer.HttpResponse(502, "application/json",
                "{\"error\":\"Backend nicht erreichbar: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static String readFully(InputStream is) throws java.io.IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

}
