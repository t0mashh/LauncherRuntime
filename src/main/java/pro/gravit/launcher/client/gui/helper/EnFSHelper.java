package pro.gravit.launcher.client.gui.helper;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.utils.RuntimeCryptedFile;
import pro.gravit.utils.enfs.EnFS;
import pro.gravit.utils.enfs.dir.CachedFile;
import pro.gravit.utils.enfs.dir.FileEntry;
import pro.gravit.utils.enfs.dir.URLFile;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EnFSHelper {

    private static final Set<String> themesCached = new HashSet<>(1);
    private static final String BASE_DIRECTORY = "tgui";

    public static boolean checkEnFSUrl() {
        try {
            URL url = new URL(new URL("enfs", null, -1, "aone").toString()); // check URL Handler
            return true;
        } catch (IOException e) {
            return false; // Failed on Java 8
        }
    }

    public static void initEnFS() throws IOException {
        EnFS.main.newDirectory(Paths.get(BASE_DIRECTORY));
        if(LogHelper.isDevEnabled() || JavaFXApplication.getInstance().isDebugMode()) {
            EnFS.DEBUG_OUTPUT = new LauncherEnFsDebugOutput();
        }
    }

    private static class LauncherEnFsDebugOutput implements EnFS.DebugOutput {

        @Override
        public void debug(String str) {
            LogHelper.debug(str);
        }

        @Override
        public void debug(String format, Object... args) {
            LogHelper.debug(format, args);
        }
    }

    public static Path initEnFSDirectory(LauncherConfig config, String theme) throws IOException {
        Path enfsDirectory = Paths.get(BASE_DIRECTORY, theme != null ? theme : "common");
        Set<String> themePaths;
        String startThemePrefix;
        if (theme != null) {
            if (themesCached.contains(theme)) {
                return enfsDirectory;
            }
            startThemePrefix = "themes/%s/".formatted(theme);
            EnFS.main.newDirectory(enfsDirectory);
            themePaths = new HashSet<>();
            // First stage - collect themes path
            config.runtime.forEach((name, digest) -> {
                if (name.startsWith(startThemePrefix)) {
                    themePaths.add(name.substring(startThemePrefix.length()));
                }
            });
            themesCached.add(theme);
        } else {
            startThemePrefix = "themes/common/";
            themePaths = Collections.emptySet();
        }
        // Second stage - put files
        config.runtime.forEach((name, digest) -> {
            String realPath;
            if (name.startsWith(startThemePrefix)) {
                realPath = name.substring(startThemePrefix.length());
            } else {
                if (themePaths.contains(name)) {
                    return;
                }
                realPath = name;
            }
            try {
                Path path = enfsDirectory.resolve(realPath);
                EnFS.main.newDirectories(path.getParent());
                FileEntry entry = makeFile(config, name, digest);
                EnFS.main.addFile(path, entry);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        });
        return enfsDirectory;
    }

    private static FileEntry makeFile(LauncherConfig config, String name, byte[] digest) throws IOException {
        FileEntry entry;
        if (config.runtimeEncryptKey == null) {
            entry = new URLFile(Launcher.getResourceURL(name));
        } else {
            String encodedName = "runtime/" + SecurityHelper.toHex(digest);
            entry = new CachedFile(new RuntimeCryptedFile(() -> {
                try {
                    return IOHelper.newInput(IOHelper.getResourceURL(encodedName));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, SecurityHelper.fromHex(config.runtimeEncryptKey)));
        }
        return entry;
    }

    public static URL getURL(String name) throws IOException {
        try (InputStream stream = EnFS.main.getInputStream(Paths.get(name))) {
            return new URL("enfs", null, -1, name);
        } catch (UnsupportedOperationException ex) {
            throw new FileNotFoundException(name);
        }
    }
}
