package astar;

import container.Service;
import exceptions.FinMatchException;
import exceptions.MemoryManagerException;
import robot.RobotChrono;
import robot.RobotReal;
import strategie.GameState;
import utils.Config;
import utils.Log;

/**
 * Classe qui fournit des objets GameState à AStar.
 * AStar a besoin de beaucoup de gamestate, et l'instanciation d'un objet est long.
 * Du coup on réutilise les mêmes objets sans devoir en créer tout le temps de nouveaux.
 * @author pf
 *
 */

public class MemoryManager implements Service {

	private static final int nb_instances = 100;

	@SuppressWarnings("unchecked")
	private GameState<RobotChrono>[][] gamestates_list = new GameState[2][nb_instances];
	protected Log log;
	private GameState<RobotChrono> model;
	
	// gamestates_list est triés: avant firstAvailable, les gamestate sont indisponibles, après, ils sont disponibles
	private int firstAvailable[] = new int[2];
	
	@Override
	public void updateConfig() {
	}

	public MemoryManager(Log log, Config config, GameState<RobotReal> realstate)
	{	
		this.log = log;

		firstAvailable[0] = 0;
		firstAvailable[1] = 0;
		try {
			this.model = realstate.cloneGameState();
			// on prépare déjà des gamestates
			log.debug("Instanciation de "+2*nb_instances+" GameState<RobotChrono>", this);
		
			for(int j = 0; j < 2; j++)
				for(int i = 0; i < nb_instances; i++)
				{
					gamestates_list[j][i] = model.cloneGameState(i);
				}
		} catch (FinMatchException e) {
			// Impossible
			e.printStackTrace();
		}
		log.debug("Instanciation finie", this);
		updateConfig();
	}
	
	public GameState<RobotChrono> getNewGameState(int id_astar) throws FinMatchException
	{
		// lève une exception s'il n'y a plus de place
		GameState<RobotChrono> out;
		out = gamestates_list[id_astar][firstAvailable[id_astar]];
		firstAvailable[id_astar]++;
		return out;
	}
	
	public void destroyGameState(GameState<RobotChrono> state, int id_astar) throws MemoryManagerException
	{
		int indice_state = state.getIndiceMemoryManager();
		/**
		 * S'il est déjà détruit, on lève une exception
		 */
		if(indice_state >= firstAvailable[id_astar])
			throw new MemoryManagerException();

		// On inverse dans le Vector les deux gamestates,
		// de manière à avoir toujours un Vector trié.
		firstAvailable[id_astar]--;
		
		GameState<RobotChrono> tmp1 = gamestates_list[id_astar][indice_state];
		GameState<RobotChrono> tmp2 = gamestates_list[id_astar][firstAvailable[id_astar]];

		tmp1.setIndiceMemoryManager(firstAvailable[id_astar]);
		tmp2.setIndiceMemoryManager(indice_state);

		gamestates_list[id_astar][firstAvailable[id_astar]] = tmp1;
		gamestates_list[id_astar][indice_state] = tmp2;
	}
	
	public void empty(int id_astar)
	{
		firstAvailable[id_astar] = 0;
	}
	
	/**
	 * Utilisé par les tests
	 */
	public boolean isMemoryManagerEmpty(int id_astar)
	{
		return firstAvailable[id_astar] == 0;
	}
	
}