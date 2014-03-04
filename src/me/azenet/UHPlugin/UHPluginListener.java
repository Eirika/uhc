package me.azenet.UHPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.Sound;
import org.bukkit.conversations.Conversation;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class UHPluginListener implements Listener {

	UHPlugin p = null;
	
	public UHPluginListener(UHPlugin p) {
		this.p = p;
	}
	
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
	    Player player = event.getPlayer();
	    TeleportCause cause = event.getCause();
	    Location to = event.getTo();
	     
	    if(cause == TeleportCause.ENDER_PEARL){
	    	event.setCancelled(true);
	    	player.teleport(to);
	    }
    }
	
	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent ev) {
		Location l = ev.getEntity().getLocation();
		Player[] ps = Bukkit.getServer().getOnlinePlayers();
		for (Player pp : ps) {
			pp.playSound(pp.getLocation(), Sound.WITHER_SPAWN, 1F, 1F);
		}
		this.p.addDead(ev.getEntity().getName());
		Bukkit.getScheduler().runTaskLater(this.p, new BukkitRunnable() {
			
			@Override
			public void run() {
				p.setLife((Player)ev.getEntity(), 0);
			}
		}, 1L);
		if (this.p.getConfig().getBoolean("kick-on-death.kick", true) && p.isGameRunning()) {
			Bukkit.getScheduler().runTaskLater(this.p, new BukkitRunnable() {
				
				@Override
				public void run() {
					int i = Bukkit.getOnlinePlayers().length - 1;
					ev.getEntity().kickPlayer("Congrats! You finished at the "+i+" place!");
				}
			}, 20L*this.p.getConfig().getInt("kick-on-death.time", 30));
		}
		try { 
			ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
			SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
			skullMeta.setOwner(((Player)ev.getEntity()).getName());
			skullMeta.setDisplayName(ChatColor.RESET + ((Player)ev.getEntity()).getName() + "'s head");
			skull.setItemMeta(skullMeta);
			l.getWorld().dropItem(l, skull);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(ev.getEntity().getKiller() != null) {
			Player p = ev.getEntity().getKiller();
			p.giveExpLevels(5);
		}

	}
	
	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent ev) {
		if(!p.isGameRunning() && !ev.getPlayer().isOp())
			ev.setCancelled(true);
		if (ev.getItem().getItemStack().getType() == Material.GHAST_TEAR && ev.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) ev.setCancelled(true);
		p.updatePlayerListName(ev.getPlayer());
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent ev) {
		if (this.p.isPlayerDead(ev.getPlayer().getName()) && !this.p.getConfig().getBoolean("allow-reconnect", true)) {
			ev.setResult(Result.KICK_OTHER);
			ev.setKickMessage("You are died!");
		}
	}
		
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent ev) {
		if (!this.p.isGameRunning()) {
			ev.getPlayer().setGameMode(GameMode.CREATIVE);
			Location l = ev.getPlayer().getWorld().getSpawnLocation();
			ev.getPlayer().teleport(l.add(0,1,0));
		}
		p.addToScoreboard(ev.getPlayer());
		Bukkit.getScheduler().runTaskLater(this.p, new BukkitRunnable() {
			
			@Override
			public void run() {
				p.updatePlayerListName(ev.getPlayer());
			}
		}, 1L);
		ev.getPlayer().setGameMode(GameMode.SURVIVAL);
	}
	
	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent e) {
		if(!p.isGameRunning()) {
			e.setCancelled(true);
			//Bukkit.getLogger().info("Mob spawn cancelled");
			return;
		}
		if(e.getEntityType() == EntityType.WITCH) {
			e.setCancelled(true);
			Bukkit.getLogger().info("Witch spawn cancelled");
			return;
		}
	}
	
	@EventHandler
	public void onBlockBreakEvent(final BlockBreakEvent ev) {
		if (!this.p.isGameRunning() && !ev.getPlayer().isOp())
			ev.setCancelled(true);
	}
	
	@EventHandler
	public void onBlockPlaceEvent(final BlockPlaceEvent ev) {
		if (!this.p.isGameRunning() && !ev.getPlayer().isOp())
			ev.setCancelled(true);
	}
	
	@EventHandler
	public void onFoodUpdate(FoodLevelChangeEvent ev) {
		if(!p.isGameRunning())
			ev.setCancelled(true);
	}
	
/*	@EventHandler
	public void onPlayerMove(PlayerMoveEvent ev) {
		Location l = ev.getTo();
		Integer mapSize = p.getConfig().getInt("map.size");
		Integer halfMapSize = (int) Math.floor(mapSize/2);
		Integer x = l.getBlockX();
		Integer z = l.getBlockZ();
		
		Location spawn = ev.getPlayer().getWorld().getSpawnLocation();
		Integer limitXInf = spawn.add(-halfMapSize, 0, 0).getBlockX();
		
		spawn = ev.getPlayer().getWorld().getSpawnLocation();
		Integer limitXSup = spawn.add(halfMapSize, 0, 0).getBlockX();
		
		spawn = ev.getPlayer().getWorld().getSpawnLocation();
		Integer limitZInf = spawn.add(0, 0, -halfMapSize).getBlockZ();
		
		spawn = ev.getPlayer().getWorld().getSpawnLocation();
		Integer limitZSup = spawn.add(0, 0, halfMapSize).getBlockZ();
		
		if (x < limitXInf || x > limitXSup || z < limitZInf || z > limitZSup) {
			ev.setCancelled(true);
		}
	}*/ // WorldBorder do it better!	

	@EventHandler
	public void onInventoryClick(InventoryClickEvent ev) {
		if (ev.getInventory().getName().equals("- Teams -")) {
			Player pl = (Player) ev.getWhoClicked();
			ev.setCancelled(true);
			if (ev.getCurrentItem().getType() == Material.DIAMOND) {
				pl.closeInventory();
				p.getConversationFactory("teamPrompt").buildConversation(pl).begin();
			} else if (ev.getCurrentItem().getType() == Material.BEACON) {
				pl.closeInventory();
				Conversation c = p.getConversationFactory("playerPrompt").buildConversation(pl);
				c.getContext().setSessionData("nomTeam", ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName()));
				c.getContext().setSessionData("color", p.getTeam(ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName())).getChatColor());
				c.begin();
			}
		}
	}
	
	@EventHandler
	public void onCraftItem(CraftItemEvent ev) {
		try {
			if (ev.getRecipe() instanceof ShapedRecipe) {
				ShapedRecipe r = (ShapedRecipe)ev.getRecipe();
				String item = "the compass";
				Boolean isCompassValid = false;
				for (Map.Entry<Character, ItemStack> e : r.getIngredientMap().entrySet()) {
					if (r.getResult().getType() == Material.GOLDEN_APPLE && e != null && e.getValue() != null && e.getValue().getType() == Material.GOLD_NUGGET) { //gotta cancel
						item = "the golden apple";
						ev.setCancelled(true);
					} else if (r.getResult().getType() == Material.COMPASS && e != null && e.getValue() != null && e.getValue().getType() == Material.BONE) {
						isCompassValid = true;
					}
				}
				if (!p.getConfig().getBoolean("compass")) isCompassValid = true;
				if (!isCompassValid && r.getResult().getType() == Material.COMPASS) ev.setCancelled(true);
				if (ev.isCancelled()) ((Player) ev.getWhoClicked()).sendMessage(ChatColor.RED+"You can't craft "+item+" like this!");
			} else if (ev.getRecipe() instanceof ShapelessRecipe) {
				ShapelessRecipe r = (ShapelessRecipe) ev.getRecipe();
				String item = "";
				for (ItemStack i : r.getIngredientList()) {
					if (i.getType() == Material.GOLD_NUGGET && r.getResult().getType() == Material.SPECKLED_MELON) { //gotta cancel
						item = "the glistering melon";
						ev.setCancelled(true);
					}
				}
				if (ev.isCancelled()) ((Player) ev.getWhoClicked()).sendMessage(ChatColor.RED+"You can't craft "+item+" like this!");
			}
		} catch (Exception e) {
			Bukkit.getLogger().warning(ChatColor.RED+"Crafting error");
			e.printStackTrace();
		}
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent ev) {
		if (ev.getEntity() instanceof Ghast) {
			Bukkit.getLogger().info("Modifying drops for Ghast");
			List<ItemStack> drops = new ArrayList<ItemStack>(ev.getDrops());
			ev.getDrops().clear();
			for (ItemStack i : drops) {
				if (i.getType() == Material.GHAST_TEAR) {
					Bukkit.getLogger().info("Added "+i.getAmount()+" ghast tear(s)");
					ev.getDrops().add(new ItemStack(Material.GOLD_INGOT,i.getAmount()));
				} else {
					Bukkit.getLogger().info("Added "+i.getAmount()+" "+i.getType().toString());
					ev.getDrops().add(i);
				}
			}
		}
	}


	@EventHandler
	public void onEntityDamage(final EntityDamageEvent ev) {
		if (ev.getEntity() instanceof Player) {
			if (!p.isTakingDamage()) ev.setCancelled(true);
			Bukkit.getScheduler().runTaskLater(this.p, new BukkitRunnable() {
				
				@Override
				public void run() {
					p.updatePlayerListName((Player)ev.getEntity());
				}
			}, 1L);
		}
	}
	
	@EventHandler
	public void onEntityRegainHealth(final EntityRegainHealthEvent ev) {
		if (ev.getRegainReason() == RegainReason.SATIATED) ev.setCancelled(true);
		if (ev.getEntity() instanceof Player) {
			Bukkit.getScheduler().runTaskLater(this.p, new BukkitRunnable() {
				
				@Override
				public void run() {
					p.updatePlayerListName((Player)ev.getEntity());
				}
			}, 1L);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent ev) {
		if ((ev.getAction() == Action.RIGHT_CLICK_AIR || ev.getAction() == Action.RIGHT_CLICK_BLOCK) && ev.getPlayer().getItemInHand().getType() == Material.COMPASS && p.getConfig().getBoolean("compass")) {
			Player pl = ev.getPlayer();
			Boolean foundRottenFlesh = false;
			for (ItemStack is : pl.getInventory().getContents()) {
				if (is != null && is.getType() == Material.ROTTEN_FLESH) {
					p.getLogger().info(""+is.getAmount());
					if (is.getAmount() != 1) is.setAmount(is.getAmount()-1);
					else { p.getLogger().info("lol"); pl.getInventory().removeItem(is); }
					pl.updateInventory();
					foundRottenFlesh = true;
					break;
				}
			}
			if (!foundRottenFlesh) {
				pl.sendMessage(ChatColor.GRAY+""+ChatColor.ITALIC+"You don't have any rotten flesh.");
				pl.playSound(pl.getLocation(), Sound.STEP_WOOD, 1F, 1F);
				return;
			}
			pl.playSound(pl.getLocation(), Sound.BURP, 1F, 1F);
			Player nearest = null;
			Double distance = 99999D;
			for (Player pl2 : p.getServer().getOnlinePlayers()) {
				try {	
					Double calc = pl.getLocation().distance(pl2.getLocation());
					if (calc > 1 && calc < distance) {
						distance = calc;
						if (pl2 != pl && !this.p.inSameTeam(pl, pl2)) nearest = pl2.getPlayer();
					}
				} catch (Exception e) {}
			}
			if (nearest == null) {
				pl.sendMessage(ChatColor.GRAY+""+ChatColor.ITALIC+"Nobody near.");
				return;
			}
			pl.sendMessage(ChatColor.GRAY+"The compass points to the nearest player.");
			pl.setCompassTarget(nearest.getLocation());
		}
		else if(ev.getAction() == Action.PHYSICAL) {
			if(ev.getClickedBlock().getY() == 42 && ev.getClickedBlock().getX() == 6 && (ev.getClickedBlock().getZ() == -8 || ev.getClickedBlock().getZ() == -9 || ev.getClickedBlock().getZ() == -10))
			ev.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onWeatherChange(WeatherChangeEvent ev) {
		if (!p.getConfig().getBoolean("weather")) {
			ev.setCancelled(true);
		}
	}
}
