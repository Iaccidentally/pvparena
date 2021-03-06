package net.slipcor.pvparena.goals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.listeners.PlayerListener;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.EndRunnable;

/**
 * <pre>
 * Arena Goal class "PlayerLives"
 * </pre>
 * 
 * The first Arena Goal. Players have lives. When every life is lost, the player
 * is teleported to the spectator spawn to watch the rest of the fight.
 * 
 * @author slipcor
 */

public class GoalPlayerLives extends ArenaGoal {
	public GoalPlayerLives() {
		super("PlayerLives");
		debug = new Debug(102);
	}

	private EndRunnable endRunner = null;

	@Override
	public String version() {
		return PVPArena.instance.getDescription().getVersion();
	}

	private static final int PRIORITY = 2;

	@Override
	public PACheck checkEnd(final PACheck res) {
		arena.getDebugger().i("checkEnd - " + arena.getName());
		if (res.getPriority() > PRIORITY) {
			arena.getDebugger().i(res.getPriority() + ">" + PRIORITY);
			return res;
		}

		if (!arena.isFreeForAll()) {
			arena.getDebugger().i("TEAMS!");
			final int count = TeamManager.countActiveTeams(arena);
			arena.getDebugger().i("count: " + count);

			if (count <= 1) {
				res.setPriority(this, PRIORITY); // yep. only one team left. go!
			}
			return res;
		}

		final int count = getLifeMap().size();

		arena.getDebugger().i("lives: " + StringParser.joinSet(getLifeMap().keySet(), "|"));

		if (count <= 1) {
			res.setPriority(this, PRIORITY); // yep. only one player left. go!
		}
		if (count == 0) {
			res.setError(this, MSG.ERROR_NOPLAYERFOUND.toString());
		}

		return res;
	}

	@Override
	public String checkForMissingSpawns(final Set<String> list) {
		if (!arena.isFreeForAll()) {
			return this.checkForMissingTeamSpawn(list);
		}
		return this.checkForMissingSpawn(list);
	}

	@Override
	public PACheck checkJoin(final CommandSender sender, final PACheck res, final String[] args) {
		if (res.getPriority() >= PRIORITY) {
			return res;
		}

		final int maxPlayers = arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS);
		final int maxTeamPlayers = arena.getArenaConfig().getInt(
				CFG.READY_MAXTEAMPLAYERS);

		if (maxPlayers > 0 && arena.getFighters().size() >= maxPlayers) {
			res.setError(this, Language.parse(arena, MSG.ERROR_JOIN_ARENA_FULL));
			return res;
		}

		if (args == null || args.length < 1) {
			return res;
		}

		if (!arena.isFreeForAll()) {
			final ArenaTeam team = arena.getTeam(args[0]);

			if (team != null && maxTeamPlayers > 0
						&& team.getTeamMembers().size() >= maxTeamPlayers) {
				res.setError(this, Language.parse(arena, MSG.ERROR_JOIN_TEAM_FULL));
				return res;
			}
		}

		res.setPriority(this, PRIORITY);
		return res;
	}

	@Override
	public PACheck checkPlayerDeath(final PACheck res, final Player player) {
		if (res.getPriority() <= PRIORITY) {
			res.setPriority(this, PRIORITY);
		}
		return res;
	}
	
	@Override
	public void commitEnd(final boolean force) {
		if (endRunner != null) {
			return;
		}
		PAGoalEvent gEvent = new PAGoalEvent(arena, this, "");
		Bukkit.getPluginManager().callEvent(gEvent);

		for (ArenaTeam team : arena.getTeams()) {
			for (ArenaPlayer ap : team.getTeamMembers()) {
				if (!ap.getStatus().equals(Status.FIGHT)) {
					continue;
				}
				if (arena.isFreeForAll()) {

					ArenaModuleManager.announce(arena,
							Language.parse(arena, MSG.PLAYER_HAS_WON, ap.getName()),
							"WINNER");

					arena.broadcast(Language.parse(arena, MSG.PLAYER_HAS_WON,
							ap.getName()));
				} else {

					ArenaModuleManager.announce(
							arena,
							Language.parse(arena, MSG.TEAM_HAS_WON,
									team.getColoredName()), "WINNER");

					arena.broadcast(Language.parse(arena, MSG.TEAM_HAS_WON,
							team.getColoredName()));
					break;
				}
			}

			if (ArenaModuleManager.commitEnd(arena, team)) {
				return;
			}
		}

		endRunner = new EndRunnable(arena, arena.getArenaConfig().getInt(
				CFG.TIME_ENDCOUNTDOWN));
	}

	@Override
	public void commitPlayerDeath(final Player player, final boolean doesRespawn,
			final String error, final PlayerDeathEvent event) {
		if (!getLifeMap().containsKey(player.getName())) {
			return;
		}
		if (doesRespawn) {
			PAGoalEvent gEvent = new PAGoalEvent(arena, this, "doesRespawn", "playerDeath:"+player.getName());
			Bukkit.getPluginManager().callEvent(gEvent);
		} else {
			PAGoalEvent gEvent = new PAGoalEvent(arena, this, "playerDeath:"+player.getName());
			Bukkit.getPluginManager().callEvent(gEvent);
		}
		int pos = getLifeMap().get(player.getName());
		arena.getDebugger().i("lives before death: " + pos, player);
		if (pos <= 1) {
			getLifeMap().remove(player.getName());
			if (arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
				arena.getDebugger().i("faking player death", player);
				PlayerListener.finallyKillPlayer(arena, player, event);
			}
			// player died => commit death!
			PACheck.handleEnd(arena, false);
		} else {
			pos--;
			getLifeMap().put(player.getName(), pos);

			final ArenaTeam respawnTeam = ArenaPlayer.parsePlayer(player.getName())
					.getArenaTeam();
			if (arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
				arena.broadcast(Language.parse(arena,
						MSG.FIGHT_KILLED_BY_REMAINING,
						respawnTeam.colorizePlayer(player) + ChatColor.YELLOW,
						arena.parseDeathCause(player, event.getEntity()
								.getLastDamageCause().getCause(),
								player.getKiller()), String.valueOf(pos)));
			}

			if (arena.isCustomClassAlive()
					|| arena.getArenaConfig().getBoolean(
							CFG.PLAYER_DROPSINVENTORY)) {
				InventoryManager.drop(player);
				event.getDrops().clear();
			}

			PACheck.handleRespawn(arena,
					ArenaPlayer.parsePlayer(player.getName()), event.getDrops());

		}
	}

	@Override
	public void displayInfo(final CommandSender sender) {
		sender.sendMessage("lives: "
				+ arena.getArenaConfig().getInt(CFG.GOAL_PLIVES_LIVES));
	}

	@Override
	public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
		if (res.getPriority() <= PRIORITY+1000) {
			res.setError(
					this,
					String.valueOf(getLifeMap().containsKey(aPlayer.getName()) ? getLifeMap().get(aPlayer
									.getName()) : 0));
		}
		return res;
	}

	@Override
	public boolean hasSpawn(final String string) {
		if (arena.isFreeForAll()) {

			if (arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
				for (ArenaClass aClass : arena.getClasses()) {
					if (string.toLowerCase().startsWith(
							aClass.getName().toLowerCase() + "spawn")) {
						return true;
					}
				}
			}
			return (string.toLowerCase().startsWith("spawn"));
		}
		for (String teamName : arena.getTeamNames()) {
			if (string.toLowerCase().startsWith(
					teamName.toLowerCase() + "spawn")) {
				return true;
			}
			if (arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
				for (ArenaClass aClass : arena.getClasses()) {
					if (string.toLowerCase().startsWith(teamName.toLowerCase() + 
							aClass.getName().toLowerCase() + "spawn")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void initate(final Player player) {
		updateLives(player, arena.getArenaConfig().getInt(CFG.GOAL_PLIVES_LIVES));
	}

	@Override
	public boolean isInternal() {
		return true;
	}

	@Override
	public void parseLeave(final Player player) {
		if (player == null) {
			PVPArena.instance.getLogger().warning(
					this.getName() + ": player NULL");
			return;
		}
		if (getLifeMap().containsKey(player.getName())) {
			getLifeMap().remove(player.getName());
		}
	}

	@Override
	public void parseStart() {
		for (ArenaTeam team : arena.getTeams()) {
			for (ArenaPlayer ap : team.getTeamMembers()) {
				updateLives(ap.get(), arena.getArenaConfig().getInt(CFG.GOAL_PLIVES_LIVES));
			}
		}
	}

	@Override
	public void reset(final boolean force) {
		endRunner = null;
		getLifeMap().clear();
	}

	@Override
	public void setDefaults(final YamlConfiguration config) {
		if (arena.isFreeForAll()) {
			return;
		}

		if (config.get("teams.free") != null) {
			config.set("teams", null);
		}
		if (config.get("teams") == null) {
			arena.getDebugger().i("no teams defined, adding custom red and blue!");
			config.addDefault("teams.red", ChatColor.RED.name());
			config.addDefault("teams.blue", ChatColor.BLUE.name());
		}
		if (arena.getArenaConfig().getBoolean(CFG.GOAL_FLAGS_WOOLFLAGHEAD)
				&& (config.get("flagColors") == null)) {
			arena.getDebugger().i("no flagheads defined, adding white and black!");
			config.addDefault("flagColors.red", "WHITE");
			config.addDefault("flagColors.blue", "BLACK");
		}
	}

	@Override
	public void setPlayerLives(final int value) {
		final Set<String> plrs = new HashSet<String>();

		for (String name : getLifeMap().keySet()) {
			plrs.add(name);
		}

		for (String s : plrs) {
			getLifeMap().put(s, value);
		}
	}

	@Override
	public void setPlayerLives(final ArenaPlayer aPlayer, final int value) {
		getLifeMap().put(aPlayer.getName(), value);
	}

	@Override
	public Map<String, Double> timedEnd(final Map<String, Double> scores) {
		double score;

		for (ArenaPlayer ap : arena.getFighters()) {
			score = (getLifeMap().containsKey(ap.getName()) ? getLifeMap().get(ap.getName())
					: 0);
			if (arena.isFreeForAll()) {

				if (scores.containsKey(ap.getName())) {
					scores.put(ap.getName(), scores.get(ap.getName()) + score);
				} else {
					scores.put(ap.getName(), score);
				}
			} else {
				if (ap.getArenaTeam() == null) {
					continue;
				}
				if (scores.containsKey(ap.getArenaTeam().getName())) {
					scores.put(ap.getArenaTeam().getName(),
							scores.get(ap.getName()) + score);
				} else {
					scores.put(ap.getArenaTeam().getName(), score);
				}
			}
		}

		return scores;
	}

	@Override
	public void unload(final Player player) {
		getLifeMap().remove(player.getName());
	}
}
