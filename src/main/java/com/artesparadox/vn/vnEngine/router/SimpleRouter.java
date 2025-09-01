package com.artesparadox.vn.vnEngine.router;

import com.artesparadox.vn.vnEngine.dataclass.Const;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lightweight, zero-dependency HTTP router built on Java's built-in HttpServer.
 * It provides clean routing for a REST API, path parameter extraction,
 * static file serving, and simple JSON responses using Minecraft's bundled Gson library.
 */
public class SimpleRouter implements HttpHandler {

    // Gson is provided by Minecraft, so this is a safe dependency.
    private static final Gson GSON = new Gson();

    private final List<Route> routes = new ArrayList<>();
    private Path staticFileRoot; // Instance variable to hold the root for static files

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("webp", "image/webp");
        MIME_TYPES.put("ogg", "audio/ogg");
        MIME_TYPES.put("json", "application/json");
        // Add more MIME types as needed for your assets.
    }

    // --- Public API for Defining Routes ---

    public void get(String path, RouteHandler handler) {
        addRoute("GET", path, handler);
    }

    public void post(String path, RouteHandler handler) {
        addRoute("POST", path, handler);
    }

    /**
     * Initializes static file serving.
     * This method handles finding the web root, creating it if necessary,
     * copying default files from the JAR, and registering the route to serve them.
     *
     * @param minecraftServer The running Minecraft server instance, used to locate the world save directory.
     * @throws IOException if file operations fail.
     */
    public void serveStaticFilesFrom(MinecraftServer minecraftServer) throws IOException {
        this.staticFileRoot = getWebRootPath(minecraftServer);
        System.out.println(Const.LOG_PREFIX + " STEP 1: Determined web root destination is: " + this.staticFileRoot.toAbsolutePath());

        ensureWebRootExists(this.staticFileRoot);

        // Register the catch-all route to serve the files.
        // This handler now calls an instance method that knows about `staticFileRoot`.
        this.get("/.*", (exchange, params) -> {
            serveStaticFile(exchange);
        });
    }

    // --- Core Routing Logic ---

    private void addRoute(String method, String path, RouteHandler handler) {
        String regex = path.replaceAll("\\{([^}]+)}", "(?<$1>[^/]+)").replace(".*", ".*");
        this.routes.add(new Route(method, Pattern.compile("^" + regex + "$"), handler));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        for (Route route : routes) {
            String requestPath = exchange.getRequestURI().getPath();
            if (route.method.equalsIgnoreCase(exchange.getRequestMethod())) {
                Matcher matcher = route.pathPattern.matcher(requestPath);
                if (matcher.matches()) {
                    try {
                        route.handler.handle(exchange, matcher.namedGroups());
                    } catch (Exception e) {
                        System.err.println("Error handling request: " + requestPath);
                        e.printStackTrace();
                        sendJson(exchange, 500, Collections.singletonMap("error", "Internal Server Error"));
                    }
                    return;
                }
            }
        }
        sendJson(exchange, 404, Collections.singletonMap("error", "API endpoint not found."));
    }

    // --- Helper Methods for Responses ---

    public static void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String jsonResponse = GSON.toJson(data);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    // --- Static File Serving Logic (Now part of the Router) ---

    private void serveStaticFile(HttpExchange exchange) throws IOException {
        if (this.staticFileRoot == null) {
            throw new IllegalStateException("Static file serving has not been initialized. Call serveStaticFilesFrom() first.");
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        if (path.contains("..")) {
            sendJson(exchange, 400, Collections.singletonMap("error", "Bad Request: Invalid path."));
            return;
        }

        Path filePath = this.staticFileRoot.resolve(path.substring(1)).toAbsolutePath();
        if (!filePath.startsWith(this.staticFileRoot.toAbsolutePath())) {
            sendJson(exchange, 403, Collections.singletonMap("error", "Forbidden"));
            return;
        }

        File file = filePath.toFile();
        if (file.exists() && !file.isDirectory()) {
            String extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
            String mimeType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");

            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody(); InputStream is = new FileInputStream(file)) {
                is.transferTo(os);
            }
        } else {
            sendJson(exchange, 404, Collections.singletonMap("error", "The requested file was not found on this server."));
        }
    }

    private Path getWebRootPath(MinecraftServer minecraftServer) {
        Path frameworkPath = Paths.get(Const.FRAMEWORK_ID, Const.WEB_SUBDIR);
        if (minecraftServer != null) {
            return minecraftServer.getWorldPath(LevelResource.ROOT).resolve(frameworkPath);
        } else {
            return Paths.get("run").resolve(frameworkPath);
        }
    }

    private void ensureWebRootExists(Path webRoot) throws IOException {
        Files.createDirectories(webRoot);
        System.out.println(Const.LOG_PREFIX + " STEP 2: Ensured web root directory exists at: " + webRoot);
        System.out.println(Const.LOG_PREFIX + " STEP 3: Verifying essential files...");

        for (String fileName : Const.DEFAULT_WEB_FILES) {
            Path destinationFile = webRoot.resolve(fileName);
            System.out.println(Const.LOG_PREFIX + "  -> Checking for: " + destinationFile.getFileName());

            if (Files.notExists(destinationFile) || true) { // Make this true for development
                System.out.println(Const.LOG_PREFIX + "     -> File is MISSING. Attempting to copy from JAR...");
                String internalPath = Const.INTERNAL_ASSETS_PATH + fileName;
                // Use SimpleRouter.class to find the resource within the JAR
                try (InputStream sourceStream = SimpleRouter.class.getResourceAsStream(internalPath)) {
                    if (sourceStream == null) {
                        throw new IOException("FATAL: Default UI file not found in JAR at " + internalPath);
                    }
                    Files.copy(sourceStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println(Const.LOG_PREFIX + "     -> SUCCESS: Copied " + fileName);
                }
            } else {
                System.out.println(Const.LOG_PREFIX + "     -> File already exists. Skipping.");
            }
        }
    }

    // --- Internal Data Structures ---

    @FunctionalInterface
    public interface RouteHandler {
        void handle(HttpExchange exchange, Map<String, Integer> params) throws IOException;
    }

    private record Route(String method, Pattern pathPattern, RouteHandler handler) {}
}
