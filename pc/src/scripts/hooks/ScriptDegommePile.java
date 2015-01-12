package scripts.hooks;

import java.util.ArrayList;

import astar.arc.PathfindingNodes;
import exceptions.FinMatchException;
import exceptions.ScriptException;
import exceptions.ScriptHookException;
import exceptions.SerialConnexionException;
import exceptions.UnableToMoveException;
import hook.HookFactory;
import robot.RobotChrono;
import scripts.Script;
import strategie.GameState;
import utils.Config;
import utils.Log;

/**
 * Dégomme la pile de l'adversaire si on passe à côté
 * @author pf
 *
 */

public class ScriptDegommePile extends Script
{

	public ScriptDegommePile(HookFactory hookgenerator, Config config, Log log)
	{
		super(hookgenerator, config, log);
	}

	@Override
	public ArrayList<Integer> getVersions(GameState<RobotChrono> state) {
		return null;
	}

	@Override
	protected void termine(GameState<?> gamestate) throws ScriptException,
			FinMatchException, SerialConnexionException, ScriptHookException
	{
		// TODO
	}

	@Override
	public PathfindingNodes point_entree(int id)
	{
		return null;
	}

	@Override
	public PathfindingNodes point_sortie(int id)
	{
		return null;
	}

	@Override
	protected void execute(int id_version, GameState<?> state)
			throws UnableToMoveException, SerialConnexionException,
			FinMatchException, ScriptHookException
	{
		// TODO
	}

}
