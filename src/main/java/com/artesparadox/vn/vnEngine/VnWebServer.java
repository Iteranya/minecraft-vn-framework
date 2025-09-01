package com.artesparadox.vn.vnEngine;

import com.artesparadox.vn.vnEngine.dataclass.Const;
import com.artesparadox.vn.vnEngine.router.SimpleRouter;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map; // Make sure to import Map

public class VnWebServer {

    private final HttpServer server;
    private final int port;
    private int counter = 0; // State for our simple counter

    public VnWebServer(int port, MinecraftServer minecraftServer) throws IOException {
        this.port = port;

        // 1. Create a new router instance.
        SimpleRouter router = new SimpleRouter();

        // 2. Tell the router to handle the entire static file setup and serving process.
        router.serveStaticFilesFrom(minecraftServer);

        // 3. Add our new API routes for the counter.
        // This endpoint returns the current value of the counter.
        router.get("/api/counter", (exchange, params) -> {
            // We'll send the count back in a simple JSON object.
            SimpleRouter.sendJson(exchange, 200, Map.of("count", this.counter));
        });

        // This endpoint increments the counter and returns the new value.
        // We use POST because this action changes the state on the server.
        router.post("/api/counter/increment", (exchange, params) -> {
            this.counter++; // Increment the counter
            SimpleRouter.sendJson(exchange, 200, Map.of("count", this.counter));
        });

        // 4. Create and start the server with the configured router.
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", router);
        this.server.setExecutor(null);
        this.server.start();

        System.out.println(Const.LOG_PREFIX + " SUCCESS: Web Server started on http://localhost:" + this.port + "/");
    }

    public void stop() {
        server.stop(0);
        System.out.println(Const.LOG_PREFIX + " Web Server on port " + this.port + " stopped.");
    }
}
