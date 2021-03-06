package container;

import obstacles.Capteurs;
import obstacles.IncomingDataBuffer;
import obstacles.MoteurPhysique;
import obstacles.memory.ObstaclesMemory;
import obstacles.types.Obstacle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import debug.IncomingDataDebugBuffer;
import pathfinding.CheminPathfinding;
import pathfinding.CheminPlanif;
import pathfinding.RealGameState;
import pathfinding.astarCourbe.AStarCourbe;
import pathfinding.astarCourbe.AStarCourbeDynamique;
import pathfinding.astarCourbe.AStarCourbeMemoryManager;
import pathfinding.astarCourbe.AStarCourbePlanif;
import pathfinding.astarCourbe.HeuristiqueSimple;
import pathfinding.astarCourbe.arcs.AStarCourbeArcManager;
import pathfinding.astarCourbe.arcs.ClothoidesComputer;
import pathfinding.dstarlite.DStarLite;
import pathfinding.dstarlite.GridSpace;
import hook.HookFactory;
import exceptions.ContainerException;
import exceptions.PointSortieException;
import utils.*;
import scripts.ScriptManager;
import serie.DataForSerialOutput;
import serie.SerialInterface;
import serie.SerialSTM;
import serie.SerialSimulation;
import table.Table;
import threads.ThreadCapteurs;
import threads.ThreadConfig;
import threads.ThreadEvitement;
import threads.ThreadPathfinding;
import threads.ThreadPeremption;
import threads.ThreadSerialInput;
import threads.ThreadSerialOutput;
import robot.RobotReal;
import robot.requete.RequeteSTM;


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

	// liste des services déjà instanciés. Contient au moins Config et Log. Les autres services appelables seront présents quand ils auront été appelés
	private Service[] instanciedServices = new Service[ServiceNames.values().length];
	
	//gestion des log
	private Log log;
	private Config config;
	
	private static int nbInstances = 0;
	private boolean threadsStarted = false;
	
	private static final boolean showGraph = false;
	private FileWriter fw;

	/**
	 * Fonction à appeler à la fin du programme.
	 * ferme la connexion serie, termine les différents threads, et ferme le log.
	 */
	@SuppressWarnings("deprecation")
	public void destructor()
	{
		// arrêt des threads
		if(threadsStarted)
			try {
				for(ServiceNames s: ServiceNames.values())
				{
					if(s.isThread())
					{
						log.debug("Arrêt de "+s);
//						((ThreadAvecStop)getServicePrivate(s)).setFinThread();
						// C'est déprécié. Mais ce n'est utilisé qu'en fin de match ;
						// en fait, ce n'est utile que pour les tests
						((Thread)getService(s)).stop();
					}
				}
				threadsStarted = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		log.debug("Fermeture de la série");
		// fermeture de la connexion série
		
		SerialInterface stm = (SerialInterface)instanciedServices[ServiceNames.SERIE_STM.ordinal()];
		if(stm != null)
			stm.close();

		if(showGraph)
		{
			try {
				fw.write("}\n");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// fermeture du log
		log.debug("Fermeture du log");
		log.close();
		nbInstances--;
		System.out.println("Container détruit");
	}
	
	/**
	 * instancie le gestionnaire de dépendances et quelques services critiques
	 * Services instanciés:
	 * 		Config
	 * 		Log
	 * @throws ContainerException si un autre container est déjà instancié
	 */
	public Container() throws ContainerException
	{
		if(nbInstances != 0)
		{
			System.out.println("Un autre container existe déjà! Annulation du constructeur.");
			throw new ContainerException();
		}
		nbInstances++;
		// affiche la configuration avant toute autre chose
		System.out.println("== Container bootstrap ==");
		System.out.println("Loading config from current directory : " +  System.getProperty("user.dir"));

		try {
			log = (Log)getServiceRecursif(ServiceNames.LOG);
			config = (Config)getServiceRecursif(ServiceNames.CONFIG);
		} catch (PointSortieException e) {
			// Impossible
			e.printStackTrace();
		}
		log.updateConfig(config);
		log.useConfig(config);
		config.init(log);
		
		Obstacle.setLog(log);
		Obstacle.useConfig(config);
				
		if(showGraph)
		{
			try {
				fw = new FileWriter(new File("dependances.dot"));
				fw.write("digraph dependancesJava {\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		startAllThreads();
	}
	
	public Service getService(ServiceNames serviceTo) throws ContainerException, PointSortieException
	{
		return getServiceDisplay(null, serviceTo);
	}
	
	@SuppressWarnings("unused")
	private Service getServiceDisplay(ServiceNames serviceFrom, ServiceNames serviceTo) throws ContainerException, PointSortieException
	{
		if(showGraph && !serviceTo.equals(ServiceNames.LOG))
		{
			ArrayList<ServiceNames> ok = new ArrayList<ServiceNames>();
			ok.add(ServiceNames.CAPTEURS);
			ok.add(ServiceNames.CONFIG);
			ok.add(ServiceNames.SERIE_STM);
			ok.add(ServiceNames.TABLE);
			ok.add(ServiceNames.GRID_SPACE);
			ok.add(ServiceNames.INCOMING_DATA_DEBUG_BUFFER);
			ok.add(ServiceNames.INCOMING_DATA_BUFFER);
			ok.add(ServiceNames.SERIAL_OUTPUT_BUFFER);
			ok.add(ServiceNames.REQUETE_STM);
			ok.add(ServiceNames.A_STAR_COURBE_MEMORY_MANAGER_DYN);
			ok.add(ServiceNames.A_STAR_COURBE_MEMORY_MANAGER_PLANIF);
			ok.add(ServiceNames.D_STAR_LITE);
			ok.add(ServiceNames.CLOTHOIDES_COMPUTER);
			ok.add(ServiceNames.OBSTACLES_MEMORY);
			ok.add(ServiceNames.THREAD_SERIAL_INPUT);
			ok.add(ServiceNames.THREAD_SERIAL_OUTPUT);
			ok.add(ServiceNames.THREAD_PEREMPTION);
			ok.add(ServiceNames.THREAD_CAPTEURS);
			ok.add(ServiceNames.THREAD_CONFIG);

			try {
				if(ok.contains(serviceTo))
					fw.write(serviceTo+" [color=grey80, style=filled];\n");
				else
					fw.write(serviceTo+";\n");

				if(serviceFrom != null)
					fw.write(serviceFrom+" -> "+serviceTo+";\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return getServiceRecursif(serviceTo);
	}


	@SuppressWarnings("unused")
	/**
	 * Fournit un service. Deux possibilités: soit il n'est pas encore instancié et on l'instancie.
	 * Soit il est déjà instancié et on le renvoie.
	 * @param serviceRequested
	 * @return l'instance du service demandé
	 * @throws ContainerException
	 * @throws FinMatchException
	 * @throws PointSortieException
	 */
	private Service getServiceRecursif(ServiceNames serviceRequested) throws ContainerException, PointSortieException
	{
    	// instancie le service demandé lors de son premier appel 
    	boolean updateConfig = true;
		
    	// si le service est déja instancié, on ne le réinstancie pas
		if(instanciedServices[serviceRequested.ordinal()] != null)
			updateConfig = false;

		// Si le service n'est pas encore instancié, on l'instancie avant de le retourner à l'utilisateur
		else if(serviceRequested == ServiceNames.LOG)
			instanciedServices[serviceRequested.ordinal()] = (Service)new Log();
		else if(serviceRequested == ServiceNames.CONFIG)
			instanciedServices[serviceRequested.ordinal()] = (Service)new Config();
		else if(serviceRequested == ServiceNames.CAPTEURS)
			instanciedServices[serviceRequested.ordinal()] = (Service)new Capteurs((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																					(GridSpace)getServiceDisplay(serviceRequested, ServiceNames.GRID_SPACE),
																					(MoteurPhysique)getServiceDisplay(serviceRequested, ServiceNames.MOTEUR_PHYSIQUE),
																					(RequeteSTM)getServiceDisplay(serviceRequested, ServiceNames.REQUETE_STM));
		else if(serviceRequested == ServiceNames.TABLE)
			instanciedServices[serviceRequested.ordinal()] = (Service)new Table((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.D_STAR_LITE)
			instanciedServices[serviceRequested.ordinal()] = (Service)new DStarLite((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																				(GridSpace)getServiceDisplay(serviceRequested, ServiceNames.GRID_SPACE));
		else if(serviceRequested == ServiceNames.CHEMIN_PATHFINDING)
			instanciedServices[serviceRequested.ordinal()] = (Service)new CheminPathfinding((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		
		else if(serviceRequested == ServiceNames.OBSTACLES_MEMORY)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ObstaclesMemory((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.GRID_SPACE)
			instanciedServices[serviceRequested.ordinal()] = (Service)new GridSpace((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																					(ObstaclesMemory)getServiceDisplay(serviceRequested, ServiceNames.OBSTACLES_MEMORY),
																					(Table)getServiceDisplay(serviceRequested, ServiceNames.TABLE));
		else if(serviceRequested == ServiceNames.INCOMING_DATA_BUFFER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new IncomingDataBuffer((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.INCOMING_DATA_DEBUG_BUFFER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new IncomingDataDebugBuffer((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.SERIAL_OUTPUT_BUFFER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new DataForSerialOutput((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.SERIE_STM && !Config.simuleSerie)
			instanciedServices[serviceRequested.ordinal()] = (Service)new SerialSTM((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG), config.getInt(ConfigInfo.BAUDRATE_STM));
		else if(serviceRequested == ServiceNames.SERIE_STM && Config.simuleSerie)
			instanciedServices[serviceRequested.ordinal()] = (Service)new SerialSimulation((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
//		else if(serviceRequested == ServiceNames.SERIE_XBEE)
//			instanciedServices[serviceRequested.ordinal()] = (Service)new SerialXBEE((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG), config.getInt(ConfigInfo.BAUDRATE_XBEE));
		else if(serviceRequested == ServiceNames.HOOK_FACTORY)
			instanciedServices[serviceRequested.ordinal()] = (Service)new HookFactory((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.ROBOT_REAL)
			instanciedServices[serviceRequested.ordinal()] = (Service)new RobotReal((DataForSerialOutput)getServiceDisplay(serviceRequested, ServiceNames.SERIAL_OUTPUT_BUFFER),
															 (Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
															 (RequeteSTM)getServiceDisplay(serviceRequested, ServiceNames.REQUETE_STM));
        else if(serviceRequested == ServiceNames.REAL_GAME_STATE)
        	// ici la construction est un petit peu différente car on interdit l'instanciation publique d'un GameSTate<RobotChrono>
            instanciedServices[serviceRequested.ordinal()] = (Service) new RealGameState((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
                                                             (RobotReal)getServiceDisplay(serviceRequested, ServiceNames.ROBOT_REAL),
															 (ObstaclesMemory)getServiceDisplay(serviceRequested, ServiceNames.OBSTACLES_MEMORY),
															 (Table)getServiceDisplay(serviceRequested, ServiceNames.TABLE));
		else if(serviceRequested == ServiceNames.SCRIPT_MANAGER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ScriptManager((HookFactory)getServiceDisplay(serviceRequested, ServiceNames.HOOK_FACTORY),
																					(Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.THREAD_PEREMPTION)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ThreadPeremption((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																		(GridSpace)getServiceDisplay(serviceRequested, ServiceNames.GRID_SPACE));
		else if(serviceRequested == ServiceNames.THREAD_EVITEMENT)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ThreadEvitement((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																		(ThreadPathfinding)getServiceDisplay(serviceRequested, ServiceNames.THREAD_PATHFINDING),
																		(DataForSerialOutput)getServiceDisplay(serviceRequested, ServiceNames.SERIAL_OUTPUT_BUFFER),
																		(CheminPathfinding)getServiceDisplay(serviceRequested, ServiceNames.CHEMIN_PATHFINDING));
		else if(serviceRequested == ServiceNames.THREAD_CAPTEURS)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ThreadCapteurs((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																		(IncomingDataBuffer)getServiceDisplay(serviceRequested, ServiceNames.INCOMING_DATA_BUFFER),
																		(Capteurs)getServiceDisplay(serviceRequested, ServiceNames.CAPTEURS));
		else if(serviceRequested == ServiceNames.THREAD_SERIAL_INPUT)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ThreadSerialInput((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																		(Config)getServiceDisplay(serviceRequested, ServiceNames.CONFIG),
																		(SerialInterface)getServiceDisplay(serviceRequested, ServiceNames.SERIE_STM),
																		(IncomingDataBuffer)getServiceDisplay(serviceRequested, ServiceNames.INCOMING_DATA_BUFFER),
																		(IncomingDataDebugBuffer)getServiceDisplay(serviceRequested, ServiceNames.INCOMING_DATA_DEBUG_BUFFER),
																		(RequeteSTM)getServiceDisplay(serviceRequested, ServiceNames.REQUETE_STM),
			                                                             (RobotReal)getServiceDisplay(serviceRequested, ServiceNames.ROBOT_REAL),
																			(Table)getServiceDisplay(serviceRequested, ServiceNames.TABLE),
																			(HookFactory)getServiceDisplay(serviceRequested, ServiceNames.HOOK_FACTORY),
																			(DataForSerialOutput)getServiceDisplay(serviceRequested, ServiceNames.SERIAL_OUTPUT_BUFFER));
		else if(serviceRequested == ServiceNames.REQUETE_STM)
			instanciedServices[serviceRequested.ordinal()] = (Service)new RequeteSTM((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.THREAD_SERIAL_OUTPUT)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ThreadSerialOutput((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																		(SerialInterface)getServiceDisplay(serviceRequested, ServiceNames.SERIE_STM),
																		(DataForSerialOutput)getServiceDisplay(serviceRequested, ServiceNames.SERIAL_OUTPUT_BUFFER));
		else if(serviceRequested == ServiceNames.THREAD_CONFIG)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ThreadConfig((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																		(Config)getServiceDisplay(serviceRequested, ServiceNames.CONFIG),
																		this);
		else if(serviceRequested == ServiceNames.THREAD_PATHFINDING)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ThreadPathfinding((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																		(AStarCourbe)getServiceDisplay(serviceRequested, ServiceNames.A_STAR_COURBE_DYNAMIQUE),
																		 (GridSpace)getServiceDisplay(serviceRequested, ServiceNames.GRID_SPACE));
		else if(serviceRequested == ServiceNames.MOTEUR_PHYSIQUE)
			instanciedServices[serviceRequested.ordinal()] = (Service)new MoteurPhysique((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG)); 		
		else if(serviceRequested == ServiceNames.A_STAR_COURBE_PLANIFICATION)
			instanciedServices[serviceRequested.ordinal()] = (Service)new AStarCourbePlanif((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																					(AStarCourbeArcManager)getServiceDisplay(serviceRequested, ServiceNames.A_STAR_COURBE_ARC_MANAGER_PLANIF),
																					(AStarCourbeMemoryManager)getServiceDisplay(serviceRequested, ServiceNames.A_STAR_COURBE_MEMORY_MANAGER_PLANIF),
																					(CheminPlanif)getServiceDisplay(serviceRequested, ServiceNames.CHEMIN_PLANIF),
																					(HeuristiqueSimple)getServiceDisplay(serviceRequested, ServiceNames.HEURISTIQUE_SIMPLE));
		else if(serviceRequested == ServiceNames.A_STAR_COURBE_DYNAMIQUE)
			instanciedServices[serviceRequested.ordinal()] = (Service)new AStarCourbeDynamique((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																					(DStarLite)getServiceDisplay(serviceRequested, ServiceNames.D_STAR_LITE),
																					(AStarCourbeArcManager)getServiceDisplay(serviceRequested, ServiceNames.A_STAR_COURBE_ARC_MANAGER_DYN),
																					(RealGameState)getServiceDisplay(serviceRequested, ServiceNames.REAL_GAME_STATE),
																					(CheminPathfinding)getServiceDisplay(serviceRequested, ServiceNames.CHEMIN_PATHFINDING),
																					(AStarCourbeMemoryManager)getServiceDisplay(serviceRequested, ServiceNames.A_STAR_COURBE_MEMORY_MANAGER_DYN));
		else if(serviceRequested == ServiceNames.CHEMIN_PLANIF)
			instanciedServices[serviceRequested.ordinal()] = (Service)new CheminPlanif((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.A_STAR_COURBE_MEMORY_MANAGER_PLANIF)
			instanciedServices[serviceRequested.ordinal()] = (Service)new AStarCourbeMemoryManager((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																								   (RealGameState)getServiceDisplay(serviceRequested, ServiceNames.REAL_GAME_STATE));
		else if(serviceRequested == ServiceNames.A_STAR_COURBE_MEMORY_MANAGER_DYN)
			instanciedServices[serviceRequested.ordinal()] = (Service)new AStarCourbeMemoryManager((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																								   (RealGameState)getServiceDisplay(serviceRequested, ServiceNames.REAL_GAME_STATE));
		else if(serviceRequested == ServiceNames.A_STAR_COURBE_ARC_MANAGER_DYN)
			instanciedServices[serviceRequested.ordinal()] = (Service)new AStarCourbeArcManager((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																								(MoteurPhysique)getServiceDisplay(serviceRequested, ServiceNames.MOTEUR_PHYSIQUE),
																								(DStarLite)getServiceDisplay(serviceRequested, ServiceNames.D_STAR_LITE),
																								(ClothoidesComputer)getServiceDisplay(serviceRequested, ServiceNames.CLOTHOIDES_COMPUTER));
		else if(serviceRequested == ServiceNames.A_STAR_COURBE_ARC_MANAGER_PLANIF)
			instanciedServices[serviceRequested.ordinal()] = (Service)new AStarCourbeArcManager((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG),
																								(MoteurPhysique)getServiceDisplay(serviceRequested, ServiceNames.MOTEUR_PHYSIQUE),
																								(HeuristiqueSimple)getServiceDisplay(serviceRequested, ServiceNames.HEURISTIQUE_SIMPLE),
																								(ClothoidesComputer)getServiceDisplay(serviceRequested, ServiceNames.CLOTHOIDES_COMPUTER));
		else if(serviceRequested == ServiceNames.CLOTHOIDES_COMPUTER)
			instanciedServices[serviceRequested.ordinal()] = (Service)new ClothoidesComputer((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		else if(serviceRequested == ServiceNames.HEURISTIQUE_SIMPLE)
			instanciedServices[serviceRequested.ordinal()] = (Service)new HeuristiqueSimple((Log)getServiceDisplay(serviceRequested, ServiceNames.LOG));
		// si le service demandé n'est pas connu, alors on log une erreur.
		else
		{
			log.critical("Erreur d'instanciation pour le service : "+serviceRequested+" (service inconnu)");
			throw new ContainerException();
		}
		
		if(updateConfig && config != null)
		{
			instanciedServices[serviceRequested.ordinal()].useConfig(config);
			instanciedServices[serviceRequested.ordinal()].updateConfig(config);
		}
		
		// retourne le service en mémoire à l'utilisateur
		return instanciedServices[serviceRequested.ordinal()];
	}	

	/**
	 * Démarrage de tous les threads
	 */
	private void startAllThreads()
	{
		if(threadsStarted)
			return;
		try {
			for(ServiceNames s: ServiceNames.values())
			{
				if(s.isThread())
				{
					log.debug("Démarrage de "+s);
					((Thread)getService(s)).start();
				}
			}
			threadsStarted = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("Démarrage des threads fini");
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
				log.critical("Non instancié : "+s);
			}
		return out;
	}


	/**
	 * Renvoie le service demandé s'il est déjà instancié, null sinon.
	 * Utilisé pour la mise à jour de la config.
	 * @param s
	 * @return
	 */
	public Service getInstanciedService(ServiceNames serviceRequested) {
		return instanciedServices[serviceRequested.ordinal()];
	}
	
}
