package com.artesparadox.vn.vnEngine.dataclass;

public final class Const {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Const() {}

    /**
     * The unique identifier for your framework. Used for directory names.
     * CHANGING THIS is all you need to do to change the folder names.
     */
    public static final String FRAMEWORK_ID = "visualnovelframework";

    /**
     * The sub-directory path for the web front-end files.
     */
    public static final String WEB_SUBDIR = "frontend/web";
    /**
     * The sub-directory path for the script files.
     */
    public static final String SCRIPTS_SUBDIR = "script";

    /**
     * The prefix used for all console log messages from the web server.
     */
    public static final String LOG_PREFIX = "[VNFEngine]";

    /**
     * The list of default files that the server should ensure exist in the web root.
     * This is where you will add "style.css", "app.js", and any other files
     * from your Vite build's output that are directly referenced by index.html.
     * NOTE: This does NOT need to include hashed files in the 'assets' sub-folder,
     * as those are handled automatically by the static file server.
     */
    public static final String[] DEFAULT_WEB_FILES = {"index.html"};

    /**
     * The internal path within the JAR's resources where the default web files are stored.
     * This is constructed from the constants above for consistency.
     */
    public static final String INTERNAL_ASSETS_PATH = "/assets/" + FRAMEWORK_ID + "/" + WEB_SUBDIR + "/";
}