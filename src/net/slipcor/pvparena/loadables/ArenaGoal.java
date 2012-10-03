package net.slipcor.pvparena.loadables;

import java.util.HashMap;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.nodinchan.ncbukkit.loader.Loadable;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheckResult;
import net.slipcor.pvparena.core.Debug;

/**
 * <pre>Arena Goal class</pre>
 * 
 * The framework for adding goals to an arena
 * 
 * @author slipcor
 * 
 * @version v0.9.1
 */

public class ArenaGoal extends Loadable implements Cloneable {
	protected static Debug db = new Debug(30);
	protected Arena arena;

	/**
	 * create an arena type instance
	 * 
	 * @param sName
	 *            the arena type name
	 */
	public ArenaGoal(Arena arena, String sName) {
		super(sName);
		this.arena = arena;
	}
	
	

	/**
	 * does the arena type allow joining in battle?
	 */
	public boolean allowsJoinInBattle() {
		return false;
	}

	public PACheckResult checkCommand(PACheckResult res, String string) {
		return res;
	}

	public PACheckResult checkEnd(PACheckResult res) {
		return res;
	}

	/**
	 * check if all necessary spawns are set
	 * 
	 * @param list
	 *            the list of all set spawns
	 * @return null if ready, error message otherwise
	 */
	public String checkForMissingSpawns(Set<String> list) {
		return null;
	}

	/**
	 * hook into an interacting player
	 * @param res 
	 * 
	 * @param player
	 *            the interacting player
	 * @param clickedBlock
	 *            the block being clicked
	 * @return 
	 */
	public PACheckResult checkInteract(PACheckResult res, Player player, Block clickedBlock) {
		return res;
	}

	public PACheckResult checkJoin(CommandSender sender,
			PACheckResult res) {
		return res;
	}

	/**
	 * notify the goal of a player death, return higher priority if goal should handle the death as WIN/LOSE
	 * @param arena the arena
	 * @param player the dying player
	 * @return a PACheckResult instance to hand forth for parsing
	 */
	public PACheckResult checkPlayerDeath(PACheckResult res, Player player) {
		return res;
	}

	/**
	 * 
	 * @param arena
	 * @param res
	 * @param player
	 * @param block
	 * @return
	 */
	public PACheckResult checkSetFlag(PACheckResult res, Player player, Block block) {
		return res;
	}
	
	@Override
	public ArenaGoal clone() {
		return new ArenaGoal(this.arena, this.getName());
	}

	public void commitCommand(CommandSender sender, String[] args) {
	}

	public void commitEnd() {
		throw new IllegalStateException();
	}

	public void commitInteract(Player player, Block clickedBlock) {
		throw new IllegalStateException();
	}
	
	public void commitPlayerDeath(Player player,
			boolean doesRespawn, String error, PlayerDeathEvent event) {
		throw new IllegalStateException();
	}

	public boolean commitSetFlag(Player player, Block block) {
		return false;
	}

	/**
	 * hook into the config parsing
	 * @param config 
	 */
	public void configParse(YamlConfiguration config) {
	}

	public void displayInfo(CommandSender sender) {
	}

	public PACheckResult getLives(PACheckResult res, ArenaPlayer ap) {
		return res;
	}

	/**
	 * guess the spawn name from a given string
	 * 
	 * @param place
	 *            the string to check
	 * @return the proper spawn name
	 */
	public String guessSpawn(String place) {
		return null;
	}

	public boolean hasSpawn(String string) {
		return false;
	}

	public void initate(Player player) {
	}

	public void parseJoin(CommandSender sender, ArenaTeam team) {
	}

	/**
	 * check if the arena is ready
	 * 
	 * @param arena
	 *            the arena to check
	 * @return null if ready, error message otherwise
	 */
	public String ready() {
		return null;
	}

	/**
	 * hook into an arena reset
	 * @param a the arena being reset
	 * @param force
	 *            is the resetting forced?
	 */
	public void reset(boolean force) {
	}

	public void setArena(Arena a) {
		arena = a;
	}

	public void setDefaults(YamlConfiguration config) {
	}
	
	public void setPlayerLives(int value) {
	}

	public void setPlayerLives(ArenaPlayer ap, int value) {
	}

	/**
	 * initiate an arena
	 * @param a 
	 */
	public void teleportAllToSpawn() {
		
	}

	public HashMap<String, Double> timedEnd(HashMap<String, Double> scores) {
		return scores;
	}

	
	/**
	 * hook into arena player unloading
	 * 
	 * @param player
	 *            the player to unload
	 */
	public void unload(Player player) {
	}

	public String version() {
		return "outdated";
	}
}