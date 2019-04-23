package cn.wode490390.nukkit.eventscontroller;

import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.plugin.MethodEventExecutor;
import cn.nukkit.plugin.PluginBase;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EventsController extends PluginBase implements Listener {

    private static final String CONFIG_CANCEL = "cancel";

    private List<String> cancel;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        String node = CONFIG_CANCEL;
        try {
            this.cancel = this.getConfig().getStringList(node);
        } catch (Exception e) {
            this.logLoadException(node);
        }
        new MetricsLite(this);
        if (this.cancel == null || this.cancel.isEmpty()) {
            return;
        }

        List<String> classNames = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String path = "cn/nukkit/event";
        URL url = loader.getResource(path);
        if (url != null) {
            String[] jarInfo = url.getPath().split("!");
            String jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"));
            String packagePath = jarInfo[1].substring(1);
            try {
                JarFile jarFile = new JarFile(jarFilePath);
                Enumeration<JarEntry> entrys = jarFile.entries();
                while (entrys.hasMoreElements()) {
                    JarEntry jarEntry = entrys.nextElement();
                    String entryName = jarEntry.getName();
                    if (entryName.endsWith(".class")) {
                        if (entryName.startsWith(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                            classNames.add(entryName);
                        }
                    }
                }
            } catch (IOException ignore) {

            }
        }

        MethodEventExecutor executor;
        try {
            Method method = EventsController.class.getMethod("handleEvent", Event.class);
            executor = new MethodEventExecutor(method);
        } catch (NoSuchMethodException unreachable) {
            return;
        }

        classNames.forEach((className) -> {
            try {
                Class clazz = Class.forName(className);
                if (Cancellable.class.isAssignableFrom(clazz) && clazz != Cancellable.class) {
                    clazz.getDeclaredMethod("getHandlers"); // Check class
                    this.getServer().getPluginManager().registerEvent(clazz, this, EventPriority.LOWEST, executor, this);
                }
            } catch (ClassNotFoundException | NoSuchMethodException ignore) {

            }
        });
    }

    public <T extends Event> void handleEvent(T event) {
        if (this.cancel.contains(event.getClass().getSimpleName())) {
            event.setCancelled();
        }
    }

    private void logLoadException(String text) {
        this.getLogger().alert("An error occurred while reading the configuration '" + text + "'. Use the default value.");
    }
}
