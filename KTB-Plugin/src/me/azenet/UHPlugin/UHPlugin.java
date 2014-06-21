package me.azenet.UHPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class UHPlugin extends JavaPlugin implements ConversationAbandonedListener {

	private Logger logger = null;
	private LinkedList<Location> loc = new LinkedList<Location>();
	private Random random = null;
	private ShapelessRecipe goldenMelon = null;
	private ShapedRecipe compass = null;
	private Integer episode = 0;
	private Boolean gameRunning = false;
	private Scoreboard sb = null;
	private Integer minutesLeft = 0;
	private Integer secondsLeft = 0;
	private NumberFormat formatter = new DecimalFormat("00");
	private String sbobjname = "KTP";
	private Boolean damageIsOn = false;
	private ArrayList<UHTeam> teams = new ArrayList<UHTeam>();
	private HashMap<String, ConversationFactory> cfs = new HashMap<String, ConversationFactory>();
	private UHPrompts uhp = null;
	private HashSet<String> deadPlayers = new HashSet<String>();
	private Player[] deadPlayersArray = null;
	private Player[] alivePlayers = null;
	private UHTeam[] deadTeams = null;
	private HashSet<UHTeam> deadTeamsAnnounced = new HashSet<UHTeam>();
        
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		 
		File positions = new File("plugins/UHPlugin/positions.txt");
		if (positions.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(positions));
				String line;
				while ((line = br.readLine()) != null) {
					String[] l = line.split(",");
					getLogger().info("Ajout de la position "+Integer.parseInt(l[0])+","+Integer.parseInt(l[1])+" depuis positions.txt");
					addLocation(Integer.parseInt(l[0]), Integer.parseInt(l[1]));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try { if (br != null) br.close(); }
				catch (Exception e) { e.printStackTrace(); } //c tré l'inline
			}
			
		}
		uhp = new UHPrompts(this);
		logger = Bukkit.getLogger();
		random = new Random();
		
		goldenMelon = new ShapelessRecipe(new ItemStack(Material.SPECKLED_MELON));
		goldenMelon.addIngredient(1, Material.GOLD_BLOCK);
		goldenMelon.addIngredient(1, Material.MELON);
		this.getServer().addRecipe(goldenMelon);
		
		if (getConfig().getBoolean("compass")) {
			compass = new ShapedRecipe(new ItemStack(Material.COMPASS));
			compass.shape(new String[] {"CIE", "IRI", "BIF"});
			compass.setIngredient('I', Material.IRON_INGOT);
			compass.setIngredient('R', Material.REDSTONE);
			compass.setIngredient('C', Material.SULPHUR);
			compass.setIngredient('E', Material.SPIDER_EYE);
			compass.setIngredient('B', Material.BONE);
			compass.setIngredient('F', Material.ROTTEN_FLESH);
			this.getServer().addRecipe(compass);
                    
		}
		
		getServer().getPluginManager().registerEvents(new UHPluginListener(this), this);
		
		sb = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
		Objective obj = sb.registerNewObjective("Vie", "dummy");
		obj.setDisplayName("Vie");
		obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		
		setMatchInfo();
		
		getServer().getWorlds().get(0).setGameRuleValue("doDaylightCycle", "false");
		getServer().getWorlds().get(0).setTime(6000L);
		getServer().getWorlds().get(0).setStorm(false);
		getServer().getWorlds().get(0).setDifficulty(Difficulty.HARD);
		
		cfs.put("teamPrompt", new ConversationFactory(this)
		.withModality(true)
		.withFirstPrompt(uhp.getTNP())
		.withEscapeSequence("/cancel")
		.thatExcludesNonPlayersWithMessage("Vous devez être un joueur.")
		.withLocalEcho(false)
		.addConversationAbandonedListener(this));
		
		cfs.put("playerPrompt", new ConversationFactory(this)
		.withModality(true)
		.withFirstPrompt(uhp.getPP())
		.withEscapeSequence("/cancel")
		.thatExcludesNonPlayersWithMessage("Vous devez être un joueur.")
		.withLocalEcho(false)
		.addConversationAbandonedListener(this));
                
                File equipe = new File("plugins/UHPlugin/teams.txt");
		if (equipe.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(equipe));
				String line;
				while ((line = br.readLine()) != null) {
                                        String[] l = line.split(",");
					getLogger().info("Ajout de l'equipe "+l[0]+", nom complet "+l[1]+" de couleur "+l[2]+" depuis teams.txt");
                                        ChatColor cc = ChatColor.valueOf(l[2].toUpperCase());
                                        getLogger().info(cc.toString()+"   "+l[2]);
                                        teams.add(new UHTeam(l[0], l[1], cc, this));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try { if (br != null) br.close(); }
				catch (Exception e) { e.printStackTrace(); } //c tré l'inline
			}
		}
		logger.info("UHPlugin loaded");
	}
	
	public void addLocation(int x, int z) {
		loc.add(new Location(getServer().getWorlds().get(0), x, getServer().getWorlds().get(0).getHighestBlockYAt(x,z)+120, z));
	}
	
	public void setMatchInfo() {
		Objective obj = null;
		try {
			obj = sb.getObjective(sbobjname);
			obj.setDisplaySlot(null);
			obj.unregister();
		} catch (Exception e) {
			
		}
		
		int alivePlayersLength = getAlivePlayers().length;
		
		deadTeams = new UHTeam[99];
		int i2 = 0;
		for(UHTeam t : teams) {
			boolean thisTeamDead = false;
			if(!getAliveTeams().contains(t)) {
				deadTeams[i2] = t;
				i2++;
				thisTeamDead = true;
			}
			if(thisTeamDead && !deadTeamsAnnounced.contains(t)) {
				if(t.getPlayers().size() != 1)
					Bukkit.broadcastMessage(""+ChatColor.RED+ChatColor.BOLD+"L'équipe "+t.getDisplayName()+" a perdu !");
				else {
					Bukkit.broadcastMessage(""+ChatColor.RED+ChatColor.BOLD+"Le joueur "+t.getPlayers().get(0).getName()+" a perdu !");
				}
				deadTeamsAnnounced.add(t);
			}
		}
		
		Random r = new Random();
		sbobjname = "KTP"+r.nextInt(10000000);
		obj = sb.registerNewObjective(sbobjname, "dummy");
		obj = sb.getObjective(sbobjname);

		obj.setDisplayName(this.getScoreboardName());
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		if(getEpisodeLength() != 0) obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY+"Episode "+ChatColor.WHITE+episode)).setScore(7);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.WHITE+""+alivePlayersLength+ChatColor.GRAY+" joueurs")).setScore(6);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.WHITE+""+getAliveTeams().size()+ChatColor.GRAY+" équipe")).setScore(5);
		obj.getScore(Bukkit.getOfflinePlayer("")).setScore(4);
		if(getEpisodeLength() != 0) obj.getScore(Bukkit.getOfflinePlayer(ChatColor.WHITE+formatter.format(this.minutesLeft)+ChatColor.GRAY+":"+ChatColor.WHITE+formatter.format(this.secondsLeft))).setScore(3);
	}

	private ArrayList<UHTeam> getAliveTeams() {
		ArrayList<UHTeam> aliveTeams = new ArrayList<UHTeam>();
		/*for (UHTeam t : teams) {
			for (Player p : t.getPlayers()) {
				if (!deadPlayers.contains(p.getName()) && !aliveTeams.contains(t))
					aliveTeams.add(t);
			}
		}*/
		for(UHTeam t : teams) {
			int i = 0;
			for(Player p : t.getPlayers()) {
				if(!deadPlayers.contains(p.getName()) && !aliveTeams.contains(t))
					i++;
			}
			if(i != 0 && !aliveTeams.contains(t))
				aliveTeams.add(t);
		}
		return aliveTeams;
	}

	@Override
	public void onDisable() {
		logger.info("UHPlugin unloaded");
	}
	
	public Player[] getAlivePlayers() {
		
		int i = 0;
		
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(getTeamForPlayer(p) != null) {
				i++;
			}
			
		}
		
		alivePlayers = new Player[i];
		i = 0;
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(getTeamForPlayer(p) != null) {
				alivePlayers[i] = p;
			}
			
		}
		
		return alivePlayers;
	}
	
	public HashSet<Player> getAlivePlayersHashMap() {
		
		int i = 0;
		
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(getTeamForPlayer(p) != null) {
				i++;
			}
			
		}
		
		HashSet<Player> alivePlayers2 = new HashSet<Player>();
		i = 0;
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(getTeamForPlayer(p) != null) {
				alivePlayers2.add(p);
			}
			
		}
		
		return alivePlayers2;
	}
        
        public ArrayList<Player> getAllPlayers(){
            ArrayList<Player> onlinePlayers = new ArrayList<Player>();
            for (Player p : Bukkit.getOnlinePlayers()){
                onlinePlayers.add(p);
            }
            return onlinePlayers;
        }
	
        /*
        Not more used (not removed in case of translation)
        **************************************************
        
	public static String ordinal(int i) {
	    String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
	    switch (i % 100) {
	    case 11:
	    case 12:
	    case 13:
	        return i + "th";
	    default:
	        return i + sufixes[i % 10];

	    }
	}
        */
	
	@SuppressWarnings("unused")
	public boolean onCommand(final CommandSender s, Command c, String l, String[] a) {
		if (c.getName().equalsIgnoreCase("uh")) {
			if (/*!(s instanceof Player)*/false) {
				s.sendMessage(ChatColor.RED+"Vous devez être un joueur!");
				return true;
			}
			Player pl = null;
			if(s instanceof Player)
				pl = (Player)s;
			if (!pl.isOp()) {
				pl.sendMessage(ChatColor.RED+"Lolnope.");
				return true;
			}
			if (a.length == 0) {
				pl.sendMessage("Usage : /uh <start|shift|teamsgui|newteam|teams|playertoteam|addspawn>");
				return true;
			}
			if (a[0].equalsIgnoreCase("start")) {
				if (teams.size() == 0) {
					for (Player p : getServer().getOnlinePlayers()) {
                                            UHTeam uht = new UHTeam(p.getName(), p.getName(), ChatColor.WHITE, this);
                                            uht.addPlayer(p);
                                            teams.add(uht);
                                            p.setGameMode(GameMode.CREATIVE);
                                            p.setGameMode(GameMode.SURVIVAL);
					}
				}
				if (loc.size() < teams.size()) {
					s.sendMessage(ChatColor.RED+"Besoin de plus de points de départs pour les joueurs");
					return true;
				}
				LinkedList<Location> unusedTP = loc;
				for (final UHTeam t : teams) {
					final Location lo = unusedTP.get(this.random.nextInt(unusedTP.size()));
					Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {

						@Override
						public void run() {
							t.teleportTo(lo);
							for (Player p : t.getPlayers()) {
								p.setGameMode(GameMode.SURVIVAL);
								p.setHealth(20);
								p.setFoodLevel(20);
								p.setExhaustion(5F);
								p.getInventory().clear();
								p.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR), 
										new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
								p.setExp(0L+0F);
								p.setLevel(0);
								p.closeInventory();
								p.getActivePotionEffects().clear();
								p.setCompassTarget(lo);
								setLife(p, 20);
							}
						}
					}, 10L);
					
					unusedTP.remove(lo);
				}
				Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {

					@Override
					public void run() {
						damageIsOn = true;
					}
				}, 600L);
				World w = Bukkit.getOnlinePlayers()[0].getWorld();
				w.setGameRuleValue("doDaylightCycle", ((Boolean)getConfig().getBoolean("daylightCycle.do")).toString());
				w.setTime(getConfig().getLong("daylightCycle.time"));
				w.setStorm(false);
				w.setDifficulty(Difficulty.HARD);
				this.episode = 1;
                                
                                this.minutesLeft = getEpisodeLength();
                                this.secondsLeft = 0;
                                Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                                setMatchInfo();
                                                if(getEpisodeLength() != 0){
                                                    secondsLeft--;
                                                    if (secondsLeft == -1) {
                                                            minutesLeft--;
                                                            secondsLeft = 59;
                                                    }
                                                    if (minutesLeft == -1) {
                                                            minutesLeft = getEpisodeLength();
                                                            secondsLeft = 0;
                                                            Bukkit.getServer().broadcastMessage(ChatColor.AQUA+"-------- Fin de l'épisode "+episode+" --------");
                                                            shiftEpisode();
                                                    }
                                                }
                                        } 
                                }, 20L, 20L);
                                
				Bukkit.getServer().broadcastMessage(ChatColor.GREEN+"--- GO ---");
				this.gameRunning = true;
				return true;
			} else if (a[0].equalsIgnoreCase("shift")) {
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA+"-------- Fin de l'épisode "+episode+" [forcée] --------");
				shiftEpisode();
				this.minutesLeft = getEpisodeLength();
				this.secondsLeft = 0;
				return true;
			} else if (a[0].equalsIgnoreCase("teamsgui")) {
				Inventory iv = this.getServer().createInventory(pl, 54, "- Equipe -");
				Integer slot = 0;
				ItemStack is = null;
				for (UHTeam t : teams) {
					is = new ItemStack(Material.BEACON, t.getPlayers().size());
					ItemMeta im = is.getItemMeta();
					im.setDisplayName(t.getChatColor()+t.getDisplayName());
					ArrayList<String> lore = new ArrayList<String>();
					for (Player p : t.getPlayers()) {
						lore.add("- "+p.getDisplayName());
					}
					im.setLore(lore);
					is.setItemMeta(im);
					iv.setItem(slot, is);
					slot++;
				}
				
				ItemStack is2 = new ItemStack(Material.DIAMOND);
				ItemMeta im2 = is2.getItemMeta();
				im2.setDisplayName(ChatColor.AQUA+""+ChatColor.ITALIC+"Créer une équipe");
				is2.setItemMeta(im2);
				iv.setItem(53, is2);
				
				pl.openInventory(iv);
				return true;
			} else if (a[0].equalsIgnoreCase("newteam")) {
				if (a.length != 4) {
					pl.sendMessage(ChatColor.RED+"Utilisation: /uh newteam <nomEquipe> <couleur> <nomAffiché>");
					return true;
				}
				if (a[1].length() > 16) {
					pl.sendMessage(ChatColor.RED+"Le nom de l'équipe ne doit pas faire plus de 16 charactères.");
					return true;
				}
				if (a[3].length() > 32) {
					pl.sendMessage(ChatColor.RED+"Le nom de l'équipe ne doit pas faire plus de 16 charactères.");
				}
				ChatColor cc;
				try {
					cc = ChatColor.valueOf(a[2].toUpperCase());
				} catch (IllegalArgumentException e) {
					pl.sendMessage(ChatColor.RED+"Couleur incorrecte.");
					return true;
				}
				teams.add(new UHTeam(a[1], a[3], cc, this));
				pl.sendMessage(ChatColor.GREEN+"Equipe créée. Utiliser /uh playertoteam "+a[1]+" nomDuJoueur pour l'ajouter à cette équipe.");
				return true;
			} else if (a[0].equalsIgnoreCase("playertoteam")) {
				if (a.length != 3) {
					pl.sendMessage(ChatColor.RED+"Utilisation: /uh playertoteam <nomEquipe> <nomJoueur>");
					return true;
				}
				UHTeam t = getTeam(a[1]);
				if (t == null) {
					pl.sendMessage(ChatColor.RED+"Cette équipe n'éxiste pas. Ecrire \"/uh teams\" pour lister les équipes éxistantes.");
					return true;
				}
				if (Bukkit.getPlayerExact(a[2]) == null) {
					pl.sendMessage(ChatColor.RED+"Ce joueur n'éxiste pas. Le joueur doit être connecté pour être inscrit.");
					return true;
				}
				t.addPlayer(Bukkit.getPlayerExact(a[2]));
				pl.sendMessage(ChatColor.GREEN+Bukkit.getPlayerExact(a[2]).getName()+" ajouté à l'équipe "+a[1]+".");
				return true;
			} else if (a[0].equalsIgnoreCase("teams")) {
				for (UHTeam t : teams) {
					pl.sendMessage(ChatColor.DARK_GRAY+"- "+ChatColor.AQUA+t.getName()+ChatColor.DARK_GRAY+" ["+ChatColor.GRAY+t.getDisplayName()+ChatColor.DARK_GRAY+"] - "+ChatColor.GRAY+t.getPlayers().size()+ChatColor.DARK_GRAY+" players");
				}
				return true;
			} else if (a[0].equalsIgnoreCase("addspawn")) {
				addLocation(pl.getLocation().getBlockX(), pl.getLocation().getBlockZ());
				pl.sendMessage(ChatColor.DARK_GRAY+"Position ajoutée: "+ChatColor.GRAY+pl.getLocation().getBlockX()+","+pl.getLocation().getBlockZ());
				return true;
			}
                        
                        /***************************************
                      Utilisation du plugin WorldBorder (dynamique)
                        ***************************************/
			/*else if (a[0].equalsIgnoreCase("generateWalls")) {
				pl.sendMessage(ChatColor.GRAY+"Génération en cours...");
				try {
					Integer halfMapSize = (int) Math.floor(this.getConfig().getInt("map.size")/2);
					Integer wallHeight = this.getConfig().getInt("map.wall.height");
					Material wallBlock = Material.getMaterial(this.getConfig().getInt("map.wall.block"));
					World w = pl.getWorld();
					
					Location spawn = w.getSpawnLocation();
					Integer limitXInf = spawn.add(-halfMapSize, 0, 0).getBlockX();
					
					spawn = w.getSpawnLocation();
					Integer limitXSup = spawn.add(halfMapSize, 0, 0).getBlockX();
					
					spawn = w.getSpawnLocation();
					Integer limitZInf = spawn.add(0, 0, -halfMapSize).getBlockZ();
					
					spawn = w.getSpawnLocation();
					Integer limitZSup = spawn.add(0, 0, halfMapSize).getBlockZ();
					
					for (Integer x = limitXInf; x <= limitXSup; x++) {
						w.getBlockAt(x, 1, limitZInf).setType(Material.BEDROCK);
						w.getBlockAt(x, 1, limitZSup).setType(Material.BEDROCK);
						for (Integer y = 2; y <= wallHeight; y++) {
							w.getBlockAt(x, y, limitZInf).setType(wallBlock);
							w.getBlockAt(x, y, limitZSup).setType(wallBlock);
						}
					} 
					
					for (Integer z = limitZInf; z <= limitZSup; z++) {
						w.getBlockAt(limitXInf, 1, z).setType(Material.BEDROCK);
						w.getBlockAt(limitXSup, 1, z).setType(Material.BEDROCK);
						for (Integer y = 2; y <= wallHeight; y++) {
							w.getBlockAt(limitXInf, y, z).setType(wallBlock);
							w.getBlockAt(limitXSup, y, z).setType(wallBlock);
						}
					} 
				} catch (Exception e) {
					e.printStackTrace();
					pl.sendMessage(ChatColor.RED+"Echec génération. Voir console pour détails.");
					return true;
				}
				pl.sendMessage(ChatColor.GRAY+"Génération terminée.");
				return true;
			}*/
		}
		else if(c.getName().equalsIgnoreCase("life")) {
			if(a.length > 0) {
				int i = 1;
				String pseudo = a[0];
				while(a.length > i) {
					pseudo = pseudo+" "+a[i];
					i++;
				}
				Player p = Bukkit.getPlayer(pseudo);
				if(p == null) {
					s.sendMessage(ChatColor.RED+"Joueur inconnu.");
					return true;
				}
				double life = p.getHealth();
				s.sendMessage(ChatColor.GREEN+"Il reste "+life+" à "+p.getName()+".");
				return true;
			}
			else {
				s.sendMessage(ChatColor.RED+"Utilisation: /life <joueur>");
				return true;
			}
		}
		else if(c.getName().equalsIgnoreCase("cast")) {
			if(s.isOp()) {
                            if(a.length > 0) {
				int i = 1;
				String message = a[0];
				while(a.length > i) {
					message = message+" "+a[i];
					i++;
				}
				Bukkit.broadcastMessage(""+ChatColor.RED+ChatColor.BOLD+message);
                            }
                            else {
				s.sendMessage(ChatColor.RED+"Utilisation: /cast <joueur>");
				return true;
                            }
			}
			else {
				s.sendMessage(ChatColor.RED+"Lolnope.");
			}
			return true;
		}
		else if(c.getName().equalsIgnoreCase("head")) {
			if(s.isOp()) {
                            if(a.length > 0) {
                                Player pl = (Player)s;
                                if(pl != null) {
                                        int i = 1;
                                        String pseudo = a[0];
                                        while(a.length > i) {
                                                pseudo = pseudo+" "+a[i];
                                                i++;
                                        }
                                        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
                                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                                        skullMeta.setOwner(pseudo);
                                        skullMeta.setDisplayName(ChatColor.RESET+"Tête de " + pseudo);
                                        skull.setItemMeta(skullMeta);
                                        pl.getInventory().addItem(skull);
                                        s.sendMessage(ChatColor.GREEN+"Tête de "+pseudo+" donnée.");
                                        return true;
                                }
                                else {
                                        s.sendMessage(ChatColor.RED+"Please.");
                                        return true;
                                }
                            }
                            else {
				s.sendMessage(ChatColor.RED+"Utilisation: /head <joueur>");
				return true;
                            }
			}
			else {
				s.sendMessage(ChatColor.RED+"Lolnope.");
				return true;
			}
		}
		return false;
	}
	
	public void shiftEpisode() {
		this.episode++;
	}
	
	public boolean isGameRunning() {
		return this.gameRunning;
	}

	public void updatePlayerListName(Player p) {
		p.setScoreboard(sb);
		Integer he = (int) Math.round(p.getHealth());
		sb.getObjective("Vie").getScore(p).setScore(he);
	}

	public void addToScoreboard(Player player) {
		player.setScoreboard(sb);
		sb.getObjective("Vie").getScore(player).setScore(0);
		this.updatePlayerListName(player);
	}

	public void setLife(Player entity, int i) {
		entity.setScoreboard(sb);
		sb.getObjective("Vie").getScore(entity).setScore(i);
	}

	public boolean isTakingDamage() {
		return damageIsOn;
	}
	
	public Scoreboard getScoreboard() {
		return sb;
	}
	
	public UHTeam getTeam(String name) {
		for(UHTeam t : teams) {
			if (t.getName().equalsIgnoreCase(name)) return t;
		}
		return null;
	}

        public ArrayList<UHTeam> getAllTeams(){
            return teams;
        }
                
	public UHTeam getTeamForPlayer(Player p) {
		for(UHTeam t : teams) {
			if (t.getPlayers().contains(p)) return t;
		}
		return null;
	}
	
	public Integer getEpisodeLength() {
		return this.getConfig().getInt("episodeLength");
	}
        
        public boolean getSpawnWitch() {
            return this.getConfig().getBoolean("witch");
        }
        
        public boolean getPotion2() {
            return this.getConfig().getBoolean("potion2");
        }

	@Override
	public void conversationAbandoned(ConversationAbandonedEvent abandonedEvent) {
		if (!abandonedEvent.gracefulExit()) {
			abandonedEvent.getContext().getForWhom().sendRawMessage(ChatColor.RED+"Abandonné par "+abandonedEvent.getCanceller().getClass().getName());
		}		
	}
	
	public boolean createTeam(String name, ChatColor color) {
		if (teams.size() <= 50) {
			teams.add(new UHTeam(name, name, color, this));
			return true;
		}
		return false;
	}
	public ConversationFactory getConversationFactory(String string) {
		if (cfs.containsKey(string)) return cfs.get(string);
		return null;
	}
	
	public boolean isPlayerDead(String name) {
		return deadPlayers.contains(name);
	}
	
	public void addDead(String name) {
		deadPlayers.add(name);
		int i = 0;
		if(deadPlayersArray == null) {
			deadPlayersArray = new Player[99];
		}
		else
			i = deadPlayersArray.length;
		deadPlayersArray[i] = Bukkit.getPlayerExact(name);
	}
	
	public Player[] getDeadPlayers() {
		return deadPlayersArray;
	}
	
	public String getScoreboardName() {
		String s = this.getConfig().getString("scoreboard", "Kill the Bite");
		return s.substring(0, Math.min(s.length(), 16));
	}


	public boolean inSameTeam(Player pl, Player pl2) {
		return (getTeamForPlayer(pl).equals(getTeamForPlayer(pl2)));
	}
}
