package entryPoints;

import org.jfree.ui.RefineryUtilities;

import pathfinding.VitesseCourbure;
import pathfinding.astarCourbe.arcs.ArcCourbe;
import pathfinding.astarCourbe.arcs.ArcCourbeClotho;
import pathfinding.astarCourbe.arcs.ClothoidesComputer;
import robot.RobotReal;
import robot.Speed;
import serie.DataForSerialOutput;
import utils.Config;
import utils.ConfigInfo;
import utils.Log;
import utils.Sleep;
import utils.Vec2;
import utils.permissions.ReadOnly;
import container.Container;
import container.ServiceNames;
import debug.AffichageDebug;
import debug.IncomingDataDebug;
import debug.IncomingDataDebugBuffer;
import exceptions.ContainerException;
import exceptions.PointSortieException;
import exceptions.UnableToMoveException;

/**
 * Debug l'asser en affichant les grandeurs
 * @author pf
 *
 */

public class DebugAsser
{
	public static void main(String[] args) throws ContainerException, InterruptedException, PointSortieException
	{
		Container container = new Container();
		Log log = (Log) container.getService(ServiceNames.LOG);
		container.getService(ServiceNames.ROBOT_REAL); // initialisation de l'odo
		DataForSerialOutput stm = (DataForSerialOutput) container.getService(ServiceNames.SERIAL_OUTPUT_BUFFER);
		ClothoidesComputer clotho = (ClothoidesComputer) container.getService(ServiceNames.CLOTHOIDES_COMPUTER);
		IncomingDataDebugBuffer buffer = (IncomingDataDebugBuffer) container.getService(ServiceNames.INCOMING_DATA_DEBUG_BUFFER);
		AffichageDebug aff = new AffichageDebug();
		RobotReal robot = (RobotReal) container.getService(ServiceNames.ROBOT_REAL);
		Config config = (Config) container.getService(ServiceNames.CONFIG);

		if(Config.debugAsser)
		{
			aff.pack();
			RefineryUtilities.centerFrameOnScreen(aff);
			aff.setVisible(true);
		}
		
//		Config config = (Config) container.getService(ServiceNames.CONFIG);
/*		synchronized(config)
		{
			config.set(ConfigInfo.MATCH_DEMARRE, true);
			config.set(ConfigInfo.DATE_DEBUT_MATCH, System.currentTimeMillis());
		}
	*/	
		if(Config.debugAsser)
			stm.activeDebugMode();

		stm.asserOff();
		
		double kpVitesseD = 14;
		double kiVitesseD = 13;
		double kdVitesseD = 0;

		double kpVitesseG = 11;
		double kiVitesseG = 11;
		double kdVitesseG = 0;

		stm.setPIDconstVitesseDroite(kpVitesseD, kiVitesseD, kdVitesseD);
		stm.setPIDconstVitesseGauche(kpVitesseG, kiVitesseG, kdVitesseG);

		double kpRot = 0.02;
		double kiRot = 0.001;
		double kdRot = 0.001; // TODO à augmenter (x10)
		
		stm.setPIDconstRotation(kpRot, kiRot, kdRot);

		double kpTr = 0.05; // 0.04
		double kiTr = 0.001; // sur les conseils de Sylvain
		double kdTr = 0.001; 

		stm.setPIDconstTranslation(kpTr, kiTr, kdTr);

		double kpCourbure = 0.8; // 0.8
		double kiCourbure = 0;
		double kdCourbure = 0.001; // 0.001
		
		stm.setPIDconstCourbure(kpCourbure, kiCourbure, kdCourbure);
		
		// distance
		double k1 = 0;
		
		// angle
		double k2 = 50;//0.05;
		
		stm.setConstSamson(k1, k2);

		Sleep.sleep(2000);
		
//		while(!config.getBoolean(ConfigInfo.MATCH_DEMARRE))
//			Sleep.sleep(1);

//		stm.turn(Math.PI/2, Speed.SLOW);

		stm.avancer(500, Speed.SLOW);
		
		if(Config.debugAsser)
			while(true)
			{
				synchronized(buffer)
				{
					if(buffer.isEmpty())
						buffer.wait();
					IncomingDataDebug in = buffer.poll();
					log.debug(in);
					aff.add(in);
	//				aff.add(new IncomingDataDebug((new Random()).nextInt(10), (new Random()).nextInt(7), 7, 2, 1, 2, 5, 6));
	//				Sleep.sleep(100);
				}
			}
	}
}
