// This could be a new class, e.g., AssetExtractor.java, or a static method in your server class.
package com.artesparadox.vn.vnEngine.router;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AssetExtractor {

    /**
     * Ensures that the web assets exist in the target directory. If they don't,
     * it extracts them from the mod's JAR file.
     *
     * @param targetDirectory The destination folder (e.g., .../saves/MyWorld/vn_engine/web)
     * @throws IOException if there's an error creating directories or copying files.
     */
    public static void ensureWebAssetsExist(Path targetDirectory) throws IOException {
        // Create the directory if it doesn't exist
        Files.createDirectories(targetDirectory);

        // List of files to extract from the JAR's resources
        String[] filesToExtract = {"index.html", "style.css", "app.js"}; // Add all your files here

        for (String fileName : filesToExtract) {
            Path destinationFile = targetDirectory.resolve(fileName);

            // Only copy the file if it doesn't already exist.
            // This allows users to customize their files without them being overwritten.
            if (!Files.exists(destinationFile)) {
                System.out.println("Extracting default asset: " + fileName);

                // The path inside the JAR, relative to the 'resources' folder
                String resourcePath = "/assets/vn_engine/web/" + fileName;

                try (InputStream in = AssetExtractor.class.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        throw new IOException("Could not find resource in JAR: " + resourcePath);
                    }
                    Files.copy(in, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
