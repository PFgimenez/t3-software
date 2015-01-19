package scripts;

import hook.HookFactory;
import scripts.anticipables.ScriptAttente;
import scripts.anticipables.ScriptCherchePlot;
import scripts.anticipables.ScriptClap;
import scripts.anticipables.ScriptTapis;
import scripts.anticipables.SortieZoneDepart;
import scripts.hooks.ScriptDegommePile;
import scripts.hooks.ScriptFunnyAction;
import scripts.hooks.ScriptPrendPlot;
import scripts.hooks.ScriptPrendVerre;
import utils.Log;
import utils.Config;
import container.Service;
import exceptions.UnknownScriptException;

 /**
  * Classe enregistrée comme service qui fournira les scripts anticipables et de hook
  * @author pf, marsu
  */

public class ScriptManager implements Service
{
	
	private Script[] instancesScriptsAnticipables = new Script[ScriptAnticipableNames.values().length];
	private ScriptHook[] instancesScriptsHook = new ScriptHook[ScriptHookNames.values().length];
	
	public ScriptManager(HookFactory hookfactory, Config config, Log log) throws UnknownScriptException
	{
		// DEPENDS_ON_RULES
		instancesScriptsAnticipables[ScriptAnticipableNames.CLAP.ordinal()] = new ScriptClap(hookfactory, config, log);
		instancesScriptsAnticipables[ScriptAnticipableNames.TAPIS.ordinal()] = new ScriptTapis(hookfactory, config, log);
		instancesScriptsAnticipables[ScriptAnticipableNames.SORTIE_ZONE_DEPART.ordinal()] = new SortieZoneDepart(hookfactory, config, log);
		instancesScriptsAnticipables[ScriptAnticipableNames.ATTENTE.ordinal()] = new ScriptAttente(hookfactory, config, log);
		instancesScriptsAnticipables[ScriptAnticipableNames.CHERCHE_PLOT.ordinal()] = new ScriptCherchePlot(hookfactory, config, log);

		instancesScriptsHook[ScriptHookNames.FUNNY_ACTION.ordinal()] = new ScriptFunnyAction(hookfactory, config, log);
		instancesScriptsHook[ScriptHookNames.PREND_PLOT.ordinal()] = new ScriptPrendPlot(hookfactory, config, log);
		instancesScriptsHook[ScriptHookNames.DEGOMME_PILE.ordinal()] = new ScriptDegommePile(hookfactory, config, log);
		instancesScriptsHook[ScriptHookNames.PREND_VERRE.ordinal()] = new ScriptPrendVerre(hookfactory, config, log);
		
		for(int i = 0; i < ScriptAnticipableNames.values().length; i++)
			if(instancesScriptsAnticipables[i] == null)
			{
				log.warning("Script non instancié: "+ScriptAnticipableNames.values()[i], this);
				throw new UnknownScriptException();
			}
		for(int i = 0; i < ScriptHookNames.values().length; i++)
			if(instancesScriptsHook[i] == null)
			{
				log.warning("Script non instancié: "+ScriptHookNames.values()[i], this);
				throw new UnknownScriptException();
			}

		updateConfig();
	}

	/**
	 * Récupère un script anticipable
	 * @param nom
	 * @return
	 */
	public Script getScript(ScriptAnticipableNames nom)
	{
		Script script = instancesScriptsAnticipables[nom.ordinal()];
		return script;
	}

	/**
	 * Récupère un script de hook
	 * @param nom
	 * @return
	 */
	public ScriptHook getScript(ScriptHookNames nom)
	{
		ScriptHook script = instancesScriptsHook[nom.ordinal()];
		return script;
	}

	public void updateConfig()
	{}

}