package me.azenet.UHPlugin;

/**
 *
 * @author Tristan
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
 
public class UHPluginUpdateChecker {
    UHPlugin plugin;
    public UHPluginUpdateChecker(UHPlugin plugin) {
        this.plugin = plugin;
        currentVersion = plugin.getDescription().getVersion();
    }
 
    private String currentVersion;
    private String readurl = "https://raw.githubusercontent.com/Eirika/uhc/master/KTB-Plugin/src/plugin.yml";
 
    
    public boolean startUpdateCheck() {
        if (plugin.getConfig().getBoolean("update-checker")) {
            Logger log = plugin.getLogger();
            try {
                log.info("Checking for a new version...");
                URL url = new URL(readurl);
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                String str;
                while ((str = br.readLine()) != null) {
                    String line = str;
                    if (line.contains("version: ")) {
                        if(!line.substring(9).equals(currentVersion)){
                            log.warning("New update available");
                            return true;
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                log.severe("The UpdateChecker URL is invalid! Please let me know!");
                log.severe(e.getMessage());
            }
        }
        return false;
    }
}