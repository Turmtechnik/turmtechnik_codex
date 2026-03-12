package de.studio3.huaweimonitor;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NetworkDiscovery {
    public interface Callback {
        void onResult(List<String> hosts);
        void onError(String message);
    }

    public void findModbusHosts(Context context, int port, int timeoutMs, Callback callback) {
        ExecutorService executor = Executors.newFixedThreadPool(24);
        new Thread(() -> {
            try {
                String networkPrefix = getNetworkPrefix(context);
                if (networkPrefix == null) {
                    callback.onError("Kein aktives WLAN mit IPv4-Adresse gefunden");
                    return;
                }

                List<Future<String>> futures = new ArrayList<>();
                for (int host = 1; host <= 254; host++) {
                    final String candidate = networkPrefix + host;
                    futures.add(executor.submit(new ProbeTask(candidate, port, timeoutMs)));
                }

                List<String> matches = new ArrayList<>();
                for (Future<String> future : futures) {
                    try {
                        String match = future.get();
                        if (match != null) {
                            matches.add(match);
                        }
                    } catch (ExecutionException e) {
                        // Einzelne Hosts duerfen fehlschlagen.
                    }
                }
                callback.onResult(matches);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = e.getClass().getSimpleName();
                }
                callback.onError(message);
            } finally {
                executor.shutdownNow();
                try {
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private String getNetworkPrefix(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || wifiManager.getConnectionInfo() == null) {
            return null;
        }
        int ipInt = wifiManager.getConnectionInfo().getIpAddress();
        if (ipInt == 0) {
            return null;
        }
        String ip = Formatter.formatIpAddress(ipInt);
        if (ip == null || ip.trim().isEmpty()) {
            return null;
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot < 0) {
            return null;
        }
        return ip.substring(0, lastDot + 1);
    }

    private static class ProbeTask implements Callable<String> {
        private final String host;
        private final int port;
        private final int timeoutMs;

        ProbeTask(String host, int port, int timeoutMs) {
            this.host = host;
            this.port = port;
            this.timeoutMs = timeoutMs;
        }

        @Override
        public String call() {
            try {
                InetAddress address = InetAddress.getByName(host);
                if (!address.isReachable(timeoutMs)) {
                    return null;
                }
                Socket socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress(host, port), timeoutMs);
                    return host;
                } finally {
                    socket.close();
                }
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
