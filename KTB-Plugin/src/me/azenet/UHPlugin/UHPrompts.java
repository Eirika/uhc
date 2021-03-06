package me.azenet.UHPlugin;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.conversations.ValidatingPrompt;

public class UHPrompts {

	private UHPlugin p = null;
	
	public UHPrompts(UHPlugin p) {
		this.p = p;
	}
	
	private class TeamNamePrompt extends StringPrompt {

		@Override
		public String getPromptText(ConversationContext context) {
			return ChatColor.GRAY+"Veuillez entrer un nom pour l'équipe. /cancel pour annuler.";
		}

		@Override
		public Prompt acceptInput(ConversationContext context, String input) {
			if (input.length() > 16) {
				context.getForWhom().sendRawMessage(ChatColor.RED+"Le nom de l'équipe doit faire 16 caractères maximum.");
				return this;
			}
			context.setSessionData("nomTeam", input);
			return new TeamColorPrompt();
		}
		
	}
	
	private class TeamColorPrompt extends ValidatingPrompt {

		private ArrayList<String> colors;
		
		public TeamColorPrompt() {
			super();

			colors = new ArrayList<String>();

			colors.add(ChatColor.BLACK+"Black");
			colors.add(ChatColor.BLUE+"Blue");
                        colors.add(ChatColor.GRAY+"Gray");
                        colors.add(ChatColor.GREEN+"Green");
                        colors.add(ChatColor.RED+"Red");
			colors.add(ChatColor.WHITE+"White");
			colors.add(ChatColor.YELLOW+"Yellow");

		}
		
		@Override
		public String getPromptText(ConversationContext context) {
			String colorsString = "";
			for(String s : colors) {
				colorsString += s+ChatColor.WHITE+", ";
			}
			colorsString = colorsString.substring(0, colorsString.length()-2);
			return ChatColor.GRAY+"Veuillez entrer une couleur pour l'équipe. /cancel pour annuler.\n"+colorsString;
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context,
				String input) {
			context.setSessionData("color", StringToChatColor.getChatColorByName(ChatColor.stripColor(input)));
			p.createTeam((String) context.getSessionData("nomTeam"), (ChatColor) context.getSessionData("color"));
			context.getForWhom().sendRawMessage(ChatColor.GRAY+"Team "+((ChatColor)context.getSessionData("color"))+context.getSessionData("nomTeam")+ChatColor.GRAY+" créée.");
			return Prompt.END_OF_CONVERSATION;
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			for (String s : colors) {
				if (ChatColor.stripColor(s).equalsIgnoreCase(input)) return true;
			}
			return false;
		}
		
	}
	/*
        =========================
        Remplaced by addToTeamGui
        =========================
        
	private class PlayerPrompt extends PlayerNamePrompt {

		public PlayerPrompt(Plugin plugin) {
			super(plugin);
		}

		@Override
		public String getPromptText(ConversationContext context) {
			return ChatColor.GRAY+"Entrez le nom du joueur à ajouter dans l'équipe "+((ChatColor)context.getSessionData("color"))+context.getSessionData("nomTeam")+ChatColor.WHITE+".";
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, Player input) {
			p.getTeam((String) context.getSessionData("nomTeam")).addPlayer(input);
			context.getForWhom().sendRawMessage(ChatColor.GREEN+input.getName()+ChatColor.DARK_GREEN+" a été ajouté à l'équipe "+((ChatColor)context.getSessionData("color"))+context.getSessionData("nomTeam")+".");
			return Prompt.END_OF_CONVERSATION;
		}
		
	}
	*/
	private enum StringToChatColor {
                
		BLACK("Black", ChatColor.BLACK),
		BLUE("Blue", ChatColor.BLUE), 
		GRAY("Gray", ChatColor.GRAY),
		GREEN("Green", ChatColor.GREEN),
		RED("Red", ChatColor.RED), 
		WHITE("White", ChatColor.WHITE),
		YELLOW("Yellow", ChatColor.YELLOW);
		
		private String name;
		private ChatColor color;
		
		StringToChatColor(String name, ChatColor color) {
			this.name = name;
			this.color = color;
		}
		
		public static ChatColor getChatColorByName(String name) {
			for(StringToChatColor stcc : values()) {
				if (stcc.name.equalsIgnoreCase(name)) return stcc.color;
			}
			return null;
		}
	}
	
	public TeamNamePrompt getTNP() {
		return new TeamNamePrompt();
	}
        
	/*
	public TeamColorPrompt getTCoP() {
		return new TeamColorPrompt();
	}
	
	public PlayerPrompt getPP() {
		return new PlayerPrompt(p);
	}
        */
}
