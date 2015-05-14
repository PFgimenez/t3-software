package container;

import obstacles.Obstacle;
import permissions.ReadOnly;
import planification.MemoryManager;
import planification.astar.*;
import planification.astar.arc.Decision;
import planification.astar.arc.SegmentTrajectoireCourbe;
import planification.astar.arcmanager.PathfindingArcManager;
import planification.astar.arcmanager.StrategyArcManager;
import hook.HookFactory;
import exceptions.ContainerException;
import exceptions.FinMatchException;
import exceptions.PointSortieException;
import exceptions.SerialManagerException;
import exceptions.ThreadException;
import utils.*;
import scripts.ScriptManager;
import serial.SerialConnexion;
import serial.SerialManager;
import strategie.GameState;
import table.GridSpace;
import table.ObstacleManager;
import table.Table;
import threads.RobotThread;
import threads.ThreadTimer;
import robot.RobotReal;
import robot.stm.STMcard;


/**
 * 
 * Gestionnaire de la durée de vie des objets dans le code.
 * Permet à n'importe quelle classe implémentant l'interface "Service" d'appeller d'autres instances de services via son constructeur.
 * Une classe implémentant service n'est instanciée que par la classe "Container"
 * 
 * @author pf
 */
public class Container
{

	// liste des services déjà instanciés. Contient au moins Config et Log. Les autres services appelables seront présents s'ils ont déjà étés appellés au moins une fois
	private Service[] instanciedServices = new Service[ServiceNames.values().length];
	
	//gestion des log
	private Log log;
	
	//gestion de la configuration du robot
	private Config config;

	private static int nbInstances = 0;
	
	/**
	 * Fonction à appeler à la fin du programme.
	 * ferme la connexion serie, termine les différents threads, et ferme le log.
	 */
	public void destructor()
	{
		// stoppe les différents threads
		stopAllThreads();
		Sleep.sleep(700); // attends qu'ils soient bien tous arrètés
		
		// coupe les connexions séries
		SerialManager serialmanager;
		try {
			serialmanager = (SerialManager)getService(ServiceNames.SERIAL_MANAGER);
			serialmanager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// ferme le log
		log.close();
		nbInstances--;
	}
	
	
	/**
	 * instancie le gestionnaire de dépendances et quelques services critiques
	 * Services instanciés:
	 * 		Config
	 * 		Log
	 * @throws ContainerException en cas de problème avec le fichier de configuration ou le système de log
	 */
	public Container() throws ContainerException
	{
		if(nbInstances != 0)
		{
			System.out.println("Un autre container existe déjà! Annulation du constructeur.");
			throw new ContainerException();
		}
		nbInstances++;
		try
		{
			// affiche la configuration avant toute autre chose
			System.out.println("== Container bootstrap ==");
			System.out.println("Loading config from current directory : " +  System.getProperty("user.dir"));
			
			//parse le ficher de configuration.
			instanciedServices[ServiceNames.CONFIG.ordinal()] = (Service)new Config("./config/");
			config = (Config)instanciedServices[ServiceNames.CONFIG.ordinal()];
			
			// démarre le système de log
			instanciedServices[ServiceNames.LOG.ordinal()] = (Service)new Log(config);
			log = (Log)instanciedServices[ServiceNames.LOG.ordinal()];
			
			// Cas particulier d'interdépendance
			config.setLog(log);
			config.init();
			log.init();
			
			Obstacle.setLogConfig(log, config);
		}
		catch(Exception e)
		{
			throw new ContainerException();
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Fournit un service. Deux possibilités: soit il n'est pas encore instancié et on l'instancie.
	 * Soit il est déjà instancié et on le renvoie.
	 * @param serviceRequested
	 * @return
	 * @throws ContainerException
	 * @throws ThreadException
	 * @throws SerialManagerException
	 * @throws FinMatchException
	 * @throws PointSortieException
	 */
	public Service getService(ServiceNames serviceRequested) throws ContainerException, ThreadException, SerialManagerException, FinMatchException, PointSortieException
	{
    	// instancie le service demandé lors de son premier appel 
    	
    	// si le service est déja instancié, on ne le réinstancie pas
		if(instanciedServices[serviceRequested.ordinal()] != null)
			;
		
		// Si le service n'est pas encore instancié, on l'instancie avant de le retourner à l'utilisateur
		else if(serviceRequested == ServiceNames.TABLE)
			instanciedServices[serviceRequested.ordinal()] = (Service)new Table((Log)getService(ServiceNames.LOG),
																				(Config)getService(ServiceNames.CONFIG));
		else if(serviceRequested == ServiceNames.OBSTACLE_MANAGER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ObstacleManager((Log)getService(ServiceNames.LOG),
																				(Config)getService(ServiceNames.CONFIG),
																				(Table)getService(ServiceNames.TABLE));
		else if(serviceRequested == ServiceNames.A_STAR_PATHFINDING)
			instanciedServices[serviceRequested.ordinal()] = (Service)new AStar<PathfindingArcManager, SegmentTrajectoireCourbe>((Log)getService(ServiceNames.LOG),
																				(Config)getService(ServiceNames.CONFIG),
																				(PathfindingArcManager)getService(ServiceNames.PATHFINDING_ARC_MANAGER));
		else if(serviceRequested == ServiceNames.A_STAR_STRATEGY)
			instanciedServices[serviceRequested.ordinal()] = (Service)new AStar<StrategyArcManager, Decision>((Log)getService(ServiceNames.LOG),
																				(Config)getService(ServiceNames.CONFIG),
																				(StrategyArcManager)getService(ServiceNames.STRATEGY_ARC_MANAGER));
		else if(serviceRequested == ServiceNames.GRID_SPACE)
			instanciedServices[serviceRequested.ordinal()] = (Service)new GridSpace((Log)getService(ServiceNames.LOG),
																				(Config)getService(ServiceNames.CONFIG),
																				(ObstacleManager)getService(ServiceNames.OBSTACLE_MANAGER));
		else if(serviceRequested == ServiceNames.SERIAL_MANAGER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new SerialManager((Log)getService(ServiceNames.LOG),
																				(Config)getService(ServiceNames.CONFIG));
		
		else if(serviceRequested == ServiceNames.SERIE_STM) // les séries
		{
			try {
				SerialManager serialmanager = (SerialManager)getService(ServiceNames.SERIAL_MANAGER);
				instanciedServices[serviceRequested.ordinal()] = (Service)serialmanager.getSerial();
			}
			catch(Exception e)
			{
				log.critical("Série introuvable!");
				throw new ContainerException();
			}
		}
		else if(serviceRequested == ServiceNames.STM_CARD)
			instanciedServices[serviceRequested.ordinal()] = (Service)new STMcard((Config)getService(ServiceNames.CONFIG),
			                                                 (Log)getService(ServiceNames.LOG),
			                                                 (SerialConnexion)getService(ServiceNames.SERIE_STM));
		else if(serviceRequested == ServiceNames.MEMORY_MANAGER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new MemoryManager((Log)getService(ServiceNames.LOG),
															 (Config)getService(ServiceNames.CONFIG),
        													 (GameState<RobotReal,ReadOnly>)getService(ServiceNames.REAL_GAME_STATE));
		else if(serviceRequested == ServiceNames.HOOK_FACTORY)
			instanciedServices[serviceRequested.ordinal()] = (Service)new HookFactory((Config)getService(ServiceNames.CONFIG),
															 (Log)getService(ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.ROBOT_REAL)
			instanciedServices[serviceRequested.ordinal()] = (Service)new RobotReal((STMcard)getService(ServiceNames.STM_CARD),
															 (Config)getService(ServiceNames.CONFIG),
															 (Log)getService(ServiceNames.LOG));
        else if(serviceRequested == ServiceNames.REAL_GAME_STATE)
        	// ici la construction est un petit peu différente car on interdit l'instanciation publique d'un GameSTate<RobotChrono>
            instanciedServices[serviceRequested.ordinal()] = (Service) GameState.constructRealGameState(  (Config)getService(ServiceNames.CONFIG),
                                                             (Log)getService(ServiceNames.LOG),
                                                             (GridSpace)getService(ServiceNames.GRID_SPACE),                                                             
                                                             (RobotReal)getService(ServiceNames.ROBOT_REAL),
        													 (HookFactory)getService(ServiceNames.HOOK_FACTORY));
		else if(serviceRequested == ServiceNames.SCRIPT_MANAGER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ScriptManager(	(HookFactory)getService(ServiceNames.HOOK_FACTORY),
																					(Config)getService(ServiceNames.CONFIG),
																					(Log)getService(ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.THREAD_TIMER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ThreadTimer((Log)getService(ServiceNames.LOG),
																		(Config)getService(ServiceNames.CONFIG),
																		(ObstacleManager)getService(ServiceNames.OBSTACLE_MANAGER),
																		(STMcard)getService(ServiceNames.STM_CARD),
																		(SerialManager)getService(ServiceNames.SERIAL_MANAGER));
		else if(serviceRequested == ServiceNames.PATHFINDING_ARC_MANAGER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new PathfindingArcManager((Log)getService(ServiceNames.LOG),
																		(Config)getService(ServiceNames.CONFIG),
																		(MemoryManager)getService(ServiceNames.MEMORY_MANAGER),
																		(GameState<RobotReal,ReadOnly>)getService(ServiceNames.REAL_GAME_STATE));
		else if(serviceRequested == ServiceNames.STRATEGY_ARC_MANAGER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new StrategyArcManager((Log)getService(ServiceNames.LOG),
																		(Config)getService(ServiceNames.CONFIG),
																		(ScriptManager)getService(ServiceNames.SCRIPT_MANAGER),
																		(GameState<RobotReal,ReadOnly>)getService(ServiceNames.REAL_GAME_STATE),
																		(HookFactory)getService(ServiceNames.HOOK_FACTORY),
																		(AStar<PathfindingArcManager, SegmentTrajectoireCourbe>)getService(ServiceNames.A_STAR_PATHFINDING),
																		(MemoryManager)getService(ServiceNames.MEMORY_MANAGER));
		
		// si le service demandé n'est pas connu, alors on log une erreur.
		else
		{
			log.critical("Erreur de getService pour le service (service inconnu): "+serviceRequested);
			throw new ContainerException();
		}
		
		// retourne le service en mémoire à l'utilisateur
		return instanciedServices[serviceRequested.ordinal()];
	}	

	/**
	 * Demande au thread manager de démarrer tous les threads
	 */
	public void startAllThreads()
	{
		try {
			getService(ServiceNames.THREAD_TIMER);
			((Thread)instanciedServices[ServiceNames.THREAD_TIMER.ordinal()]).start();

			getService(ServiceNames.THREAD_SERIE);
			((Thread)instanciedServices[ServiceNames.THREAD_SERIE.ordinal()]).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Méthode qui affiche le nom de tous les services non-instanciés.
	 * Renvoie true si cette liste est vide
	 */
	public boolean afficheNonInstancies()
	{
		boolean out = true;
		
		for(ServiceNames s : ServiceNames.values())
			if(instanciedServices[s.ordinal()] == null)
			{
				out = false;
				log.critical(s);
			}
		return out;
	}

	/**
	 * Arrête tous les threads
	 * Le thread principal (appelant cette méthode) continue son exécution
	 */
	public void stopAllThreads()
	{
		RobotThread.stopAllThread();
	}
	
}
