package pawpaw.util;

import pawpaw.config.AppConfig;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves, caches, crops, scales, and persists bundled and uploaded images.
 */
public final class ImageStore {

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private static final String[] FALLBACK_EXTENSIONS = { ".png", ".jpg", ".jpeg", ".jfif" };

    private ImageStore() {
    }

    /**
     * Loads an image from relative filesystem paths, runtime {@code uploads/}, classpath {@code /images/},
     * or {@code src/main/resources/images/}. Returns {@code null} cleanly if missing or unreadable.
     */
    public static synchronized BufferedImage load(String pathOrName) {
        if (pathOrName == null || pathOrName.isBlank()) {
            return null;
        }
        String normalized = normalizePath(pathOrName);
        if (normalized.isEmpty()) {
            return null;
        }
        if (CACHE.containsKey(normalized)) {
            return CACHE.get(normalized);
        }

        BufferedImage img = resolveImage(normalized);
        if (img != null) {
            img = applyCropRule(normalized, img);
            CACHE.put(normalized, img);
        }
        return img;
    }

    public static BufferedImage loadScaledCover(String pathOrName, int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        BufferedImage src = load(pathOrName);
        if (src == null) {
            return null;
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double scale = Math.max((double) width / src.getWidth(), (double) height / src.getHeight());
        int dw = Math.max(1, (int) Math.ceil(src.getWidth() * scale));
        int dh = Math.max(1, (int) Math.ceil(src.getHeight() * scale));
        g.drawImage(src, (width - dw) / 2, (height - dh) / 2, dw, dh, null);
        g.dispose();
        return out;
    }

    public static BufferedImage loadScaledFit(String pathOrName, int width, int height, boolean round) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        BufferedImage src = load(pathOrName);
        if (src == null) {
            return null;
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (round) {
            g.setClip(new Ellipse2D.Double(0, 0, width, height));
        }
        double scale = Math.min((double) width / src.getWidth(), (double) height / src.getHeight());
        int dw = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int dh = Math.max(1, (int) Math.round(src.getHeight() * scale));
        g.drawImage(src, (width - dw) / 2, (height - dh) / 2, dw, dh, null);
        g.dispose();
        return out;
    }

    /**
     * Copies a user-selected image file into {@code uploads/<subfolder>/} and returns a normalized
     * forward-slash relative path {@code uploads/<subfolder>/<filename>}.
     */
    public static String copyUploadedImage(File source, String subfolder, String prefix) throws IOException {
        if (source == null || !source.isFile()) {
            throw new IOException("Berkas gambar sumber tidak ditemukan.");
        }
        Path targetDir = AppConfig.getUploadDir(subfolder);
        Files.createDirectories(targetDir);

        String originalName = source.getName();
        int dotIdx = originalName.lastIndexOf('.');
        String ext = dotIdx >= 0 ? originalName.substring(dotIdx).toLowerCase(Locale.ROOT) : ".png";
        String cleanPrefix = FormatUtil.slugify(prefix);
        if (cleanPrefix.isEmpty()) {
            cleanPrefix = "img";
        }
        String uniqueSuffix = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String fileName = cleanPrefix + "-" + uniqueSuffix + ext;
        Path target = targetDir.resolve(fileName);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

        String cleanSub = AppConfig.getUploadsRoot().relativize(targetDir).toString().replace('\\', '/');
        String relativePath = "uploads/" + cleanSub + "/" + fileName;
        synchronized (ImageStore.class) {
            CACHE.remove(relativePath);
        }
        return relativePath;
    }

    public static synchronized void clearCache() {
        CACHE.clear();
    }

    private static String normalizePath(String raw) {
        String p = raw.trim().replace('\\', '/');
        while (p.startsWith("./")) {
            p = p.substring(2);
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    private static BufferedImage resolveImage(String normalized) {
        File workingDir = new File(System.getProperty("user.dir"));

        // 1. Direct relative file (or absolute path if supplied)
        File direct = new File(workingDir, normalized);
        BufferedImage img = readFileSafe(direct);
        if (img != null) {
            return img;
        }
        File asGiven = new File(normalized);
        if (asGiven.isAbsolute() && (img = readFileSafe(asGiven)) != null) {
            return img;
        }

        // 2. Stripped assets/img/ (or images/ or uploads/) prefix
        String stripped = normalized;
        if (stripped.startsWith("assets/img/")) {
            stripped = stripped.substring("assets/img/".length());
        } else if (stripped.startsWith("images/")) {
            stripped = stripped.substring("images/".length());
        } else if (stripped.startsWith("uploads/")) {
            stripped = stripped.substring("uploads/".length());
        }

        for (String candidate : extensionVariants(stripped)) {
            File uploadFile = new File(AppConfig.getUploadsRoot().toFile(), candidate);
            if ((img = readFileSafe(uploadFile)) != null) {
                return img;
            }
            if ((img = readClasspathSafe("/images/" + candidate)) != null) {
                return img;
            }
            File resourceFile = new File(workingDir, "src/main/resources/images/" + candidate);
            if ((img = readFileSafe(resourceFile)) != null) {
                return img;
            }
        }

        // 3. Basename fallback in /images/ and /images/shop/
        String baseName = stripped.contains("/") ? stripped.substring(stripped.lastIndexOf('/') + 1) : stripped;
        for (String candidate : extensionVariants(baseName)) {
            if ((img = readClasspathSafe("/images/" + candidate)) != null) {
                return img;
            }
            if ((img = readClasspathSafe("/images/shop/" + candidate)) != null) {
                return img;
            }
            if ((img = readFileSafe(new File(workingDir, "src/main/resources/images/" + candidate))) != null) {
                return img;
            }
            if ((img = readFileSafe(new File(workingDir, "src/main/resources/images/shop/" + candidate))) != null) {
                return img;
            }
        }
        return null;
    }

    private static List<String> extensionVariants(String path) {
        List<String> variants = new ArrayList<>();
        variants.add(path);
        int dot = path.lastIndexOf('.');
        int slash = path.lastIndexOf('/');
        String stem = dot > slash ? path.substring(0, dot) : path;
        for (String ext : FALLBACK_EXTENSIONS) {
            String candidate = stem + ext;
            if (!variants.contains(candidate)) {
                variants.add(candidate);
            }
        }
        return variants;
    }

    private static BufferedImage readFileSafe(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                return img;
            }
        } catch (IOException ignored) {
        }
        Image raw = new ImageIcon(file.getPath()).getImage();
        if (raw.getWidth(null) > 0 && raw.getHeight(null) > 0) {
            BufferedImage copy = new BufferedImage(
                raw.getWidth(null),
                raw.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = copy.createGraphics();
            g.drawImage(raw, 0, 0, null);
            g.dispose();
            return copy;
        }
        return null;
    }

    private static BufferedImage readClasspathSafe(String resourcePath) {
        try (InputStream stream = ImageStore.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                return ImageIO.read(stream);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static BufferedImage applyCropRule(String normalizedPath, BufferedImage img) {
        String baseName = normalizedPath.contains("/")
            ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1)
            : normalizedPath;
        int[] crop = switch (baseName.toLowerCase(Locale.ROOT)) {
            case "logo.png" -> new int[] { 409, 160, 576, 563 };
            case "cuking.png" -> new int[] { 396, 332, 2071, 2214 };
            case "meng.png" -> new int[] { 389, 123, 1296, 1717 };
            case "food.png" -> new int[] { 1137, 700, 1879, 2654 };
            default -> null;
        };
        if (crop != null && crop[0] + crop[2] <= img.getWidth() && crop[1] + crop[3] <= img.getHeight()) {
            return img.getSubimage(crop[0], crop[1], crop[2], crop[3]);
        }
        return img;
    }
}
