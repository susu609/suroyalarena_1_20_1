package net.susu609.royalarena;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Serves the stats page to phones on the same Wi-Fi
 * ({@code gradlew statsServe}, port 8000 or {@code -Pport=}). Only the page
 * ({@code docs/stats/}) and the card portraits are served, read-only; the
 * rest of the repository is not. Stop it with Ctrl+C.
 */
public final class StatsServer {
    /** Folders under the repository that may be served. */
    private static final List<String> SHARED = List.of(
            "docs/stats/",
            "src/main/resources/assets/royalarena/textures/gui/card/");
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "jsonl", "text/plain; charset=utf-8",
            "png", "image/png",
            "svg", "image/svg+xml");

    private StatsServer() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        Path root = Path.of("").toAbsolutePath().normalize();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> serve(exchange, root));
        server.start();
        System.out.println("Royal Arena stats page. Open on your phone " +
                "(same Wi-Fi):");
        for (String address : lanAddresses())
            System.out.println("  http://" + address + ":" + port +
                    "/docs/stats/");
        System.out.println("On this computer: http://localhost:" + port +
                "/docs/stats/");
        System.out.println("Ctrl+C to stop.");
    }

    private static void serve(HttpExchange exchange, Path root)
            throws IOException
    {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod()) &&
                    !"HEAD".equals(exchange.getRequestMethod())) {
                send(exchange, 405, "text/plain", "Method not allowed");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/docs/stats")) {
                exchange.getResponseHeaders().add("Location", "/docs/stats/");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            String relative = path.substring(1);
            if (relative.endsWith("/"))
                relative += "index.html";
            Path file = root.resolve(relative).normalize();
            String inside = root.relativize(file).toString()
                    .replace('\\', '/');
            boolean shared = file.startsWith(root) &&
                    SHARED.stream().anyMatch(inside::startsWith);
            if (!shared || !Files.isRegularFile(file)) {
                send(exchange, 404, "text/plain", "Not found");
                return;
            }
            String name = file.getFileName().toString();
            String extension = name.substring(name.lastIndexOf('.') + 1);
            byte[] body = Files.readAllBytes(file);
            exchange.getResponseHeaders().add("Content-Type",
                    TYPES.getOrDefault(extension, "application/octet-stream"));
            // Fresh numbers after every statsReport run.
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            boolean head = "HEAD".equals(exchange.getRequestMethod());
            exchange.sendResponseHeaders(200, head ? -1 : body.length);
            if (!head) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        }
    }

    private static void send(HttpExchange exchange, int status, String type,
            String text) throws IOException
    {
        byte[] body = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /** IPv4 addresses of this computer on the local network. */
    private static List<String> lanAddresses() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(
                    NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual())
                    continue;
                for (InetAddress address : Collections.list(
                        nic.getInetAddresses())) {
                    if (address instanceof Inet4Address &&
                            address.isSiteLocalAddress())
                        out.add(address.getHostAddress());
                }
            }
        } catch (IOException e) {
            // Fall back to the localhost line only.
        }
        return out;
    }
}
