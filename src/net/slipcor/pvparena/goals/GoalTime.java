package net.slipcor.pvparena.goals;

import org.bukkit.command.CommandSender;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.runnables.TimedEndRunnable;

/**
 * <pre>Arena Goal class "Time"</pre>
 * 
 * Time is ticking ^^
 * 
 * @author slipcor
 */

public class GoalTime extends ArenaGoal {
	
	private TimedEndRunnable ter;

	public GoalTime() {
		super("Time");
		debug = new Debug(106);
	}
	
	@Override
	public String version() {
		return PVPArena.instance.getDescription().getVersion();
	}

	@Override
	public boolean allowsJoinInBattle() {
		return arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE);
	}
	
	public void commitEnd() {
		ter.commit();
	}
	
	@Override
	public void displayInfo(final CommandSender sender) {
		sender.sendMessage("timer: " + StringParser.colorVar(arena.getArenaConfig().getInt(CFG.GOAL_TIME_END)));
	}

	@Override
	public boolean isInternal() {
		return true;
	}

	@Override
	public void parseStart() {
		final int timed = arena.getArenaConfig().getInt(CFG.GOAL_TIME_END);
		if (timed > 0) {
			arena.getDebugger().i("arena timing!");
			// initiate autosave timer
			ter = new TimedEndRunnable(arena, timed, this);
		}
	}
	
	@Override
	public void reset(final boolean force) {
		if (ter != null) {
			ter.commit();
			ter = null;
		}
	}
}
