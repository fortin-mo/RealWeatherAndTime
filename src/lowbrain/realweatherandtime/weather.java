package lowbrain.realweatherandtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class weather
extends JavaPlugin {
    public void onEnable() {
        this.getLogger().info("RealWeather Plugin uses http://www.openweathermap.org");
        this.getLogger().info("APIKey needs to be set manually in config.yml");
        try {
            File file;
            if (!this.getDataFolder().exists()) {
                this.getDataFolder().mkdirs();
            }
            if (!(file = new File(this.getDataFolder(), "config.yml")).exists()) {
                this.getLogger().info("Config.yml not found! Creating new one ...");
                FileConfiguration config = this.getConfig();
                config.addDefault("cityCode", (Object)2886241);
                config.addDefault("updateInterval", (Object)600);
                config.addDefault("APIKey", (Object)"none");
                config.addDefault("ServerTimeOffsetInHours", (Object)0);
                config.addDefault("SyncTime", (Object)false);
                config.addDefault("debug", (Object)false);
                config.options().copyDefaults(true);
                this.saveConfig();
            } else {
                this.getLogger().info("Config.yml found, loading saved data!");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        String updateInterval = this.getConfig().getString("updateInterval");
        Bukkit.getServer().getScheduler().runTaskTimer((Plugin)this, new Runnable(){

            @Override
            public void run() {
            	if(weather.this.getConfig().getBoolean("debug")){
            		weather.this.getLogger().info("Auto-Sync");
            	}
                if (weather.this.getConfig().getBoolean("SyncTime")) {
                    weather.this.syncTime();
                }
                weather.this.fetchWeather();
            }
        }, 0, Long.parseLong(updateInterval) * 20);
    }

    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
        this.getLogger().info("Weather-Change prevented");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] arg3) {
        if (cmd.getName().equalsIgnoreCase("sync")) {
            sender.sendMessage("Syncing Weather and Time ...");
            if (this.getConfig().getBoolean("SyncTime")) {
                this.syncTime();
            }
            this.fetchWeather();
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("announceTime")) {
            sender.sendMessage("Announcing Current Time:");
            long currentTimeTicks = ((World)this.getServer().getWorlds().get(0)).getTime();
            Bukkit.broadcastMessage((String)("Current Server-Time In Ticks: " + String.valueOf(currentTimeTicks)));
            return true;
        }
        return false;
    }

    public void fetchWeather() {
        FileConfiguration config = this.getConfig();
        String cityCode = config.getString("cityCode");
        String APIKey = config.getString("APIKey");
        try {
            String inputLine;
            URL openweathermap = new URL("http://api.openweathermap.org/data/2.5/weather?id=" + cityCode + "&APPID=" + APIKey);
            BufferedReader inStream = new BufferedReader(new InputStreamReader(openweathermap.openStream()));
            while ((inputLine = inStream.readLine()) != null) {
                String weatherCode = inputLine.substring(inputLine.indexOf("{\"id\":") + 6, inputLine.indexOf(",\"ma"));
                int weatherInteger = Integer.parseInt(weatherCode);
                if(config.getBoolean("debug")){
                	this.getLogger().info("Received Weather Code: " + weatherCode);
                }
                if (weatherInteger > 600) {
                    ((World)this.getServer().getWorlds().get(0)).setThundering(false);
                    ((World)this.getServer().getWorlds().get(0)).setThunderDuration(0);
                    ((World)this.getServer().getWorlds().get(0)).setStorm(false);
                    ((World)this.getServer().getWorlds().get(0)).setWeatherDuration(this.getConfig().getInt("updateInterval") * 20);
                    if(config.getBoolean("debug")){
                    	
                    }
                    if(config.getBoolean("debug")){
                    	this.getLogger().info("Changing to clear weather");
                    }
                    
                    continue;
                }
                if (weatherInteger > 300) {
                    ((World)this.getServer().getWorlds().get(0)).setThundering(false);
                    ((World)this.getServer().getWorlds().get(0)).setThunderDuration(0);
                    ((World)this.getServer().getWorlds().get(0)).setStorm(true);
                    ((World)this.getServer().getWorlds().get(0)).setWeatherDuration(this.getConfig().getInt("updateInterval") * 20);
                    if(config.getBoolean("debug")){
                    	this.getLogger().info("Changing to rainy weather");
                    }
                    
                    continue;
                }
                if (weatherInteger > 200) {
                    ((World)this.getServer().getWorlds().get(0)).setThundering(true);
                    ((World)this.getServer().getWorlds().get(0)).setThunderDuration(this.getConfig().getInt("updateInterval") * 20);
                    ((World)this.getServer().getWorlds().get(0)).setStorm(true);
                    ((World)this.getServer().getWorlds().get(0)).setWeatherDuration(this.getConfig().getInt("updateInterval") * 20);
                    if(config.getBoolean("debug")){
                    	this.getLogger().info("Changing to stormy weather");
                    }
                    
                    continue;
                }
                ((World)this.getServer().getWorlds().get(0)).setThundering(false);
                ((World)this.getServer().getWorlds().get(0)).setThunderDuration(0);
                ((World)this.getServer().getWorlds().get(0)).setStorm(false);
                ((World)this.getServer().getWorlds().get(0)).setWeatherDuration(this.getConfig().getInt("updateInterval") * 20);
                if(config.getBoolean("debug")){
                	this.getLogger().info("Changing to clear weather");
                }
                
            }
            inStream.close();
        }
        catch (IOException e) {
            this.getLogger().info("Could not retrieve weather data, make sure your APIKey and CityCode are set in config.yml");
        }
    }

    public void syncTime() {
        Calendar d = Calendar.getInstance();
        int h = d.get(11) + this.getConfig().getInt("ServerTimeOffsetInHours");
        int m = d.get(12);
        long ticks = 1000 * h + (m *= 16) + 18000;
        ((World)this.getServer().getWorlds().get(0)).setTime(ticks);
    }

    public void onDisable() {
    }

}

