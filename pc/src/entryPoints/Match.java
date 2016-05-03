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

public class Match
{
	public static void main(String[] args) throws ContainerException, InterruptedException, PointSortieException
	{
		Container container = new Container();
		Log log = (Log) container.getService(ServiceNames.LOG);
		Config config = (Config) container.getService(ServiceNames.CONFIG);
		container.getService(ServiceNames.ROBOT_REAL); // initialisation de l'odo
		DataForSerialOutput stm = (DataForSerialOutput) container.getService(ServiceNames.SERIAL_OUTPUT_BUFFER);
		ClothoidesComputer clotho = (ClothoidesComputer) container.getService(ServiceNames.CLOTHOIDES_COMPUTER);
		RobotReal robot = (RobotReal) container.getService(ServiceNames.ROBOT_REAL);
/*
		stm.asserOff();
		
		double kpVitesseD = 5; // 11 pour 23V // 5 // 80 // 3
		double kiVitesseD = 0.2; // 1 // 1 // 0 // 1
		double kdVitesseD = 0.17; // 0.9 // 0.2 // 0.7 // 0.2

		double kpVitesseG = 8; // 11 pour 23V // 5 // 80 // 5
		double kiVitesseG = 0.2; // 1 // 1 // 0 // 1
		double kdVitesseG = 0.27; // 0.9 // 0.2 // 0.7 // 0.2

		stm.setPIDconstVitesseDroite(kpVitesseD, kiVitesseD, kdVitesseD);
		stm.setPIDconstVitesseGauche(kpVitesseG, kiVitesseG, kdVitesseG);

		double kpRot = 0.12;
		double kiRot = 0;
		double kdRot = 0.008;
		
		stm.setPIDconstRotation(kpRot, kiRot, kdRot);

		double kpTr = 0.04; // 0.04
		double kiTr = 0.0; // sur les conseils de Sylvain
		double kdTr = 0.006; // 0.006

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
		*/
		while(!config.getBoolean(ConfigInfo.MATCH_DEMARRE))
			Sleep.sleep(1);

		try {
			robot.avancer(1900, false, Speed.STANDARD);
			robot.tourner(Math.PI, Speed.STANDARD);
//			robot.vaAuPoint(new Vec2<ReadOnly>(-200, 1200), Speed.STANDARD);
//			robot.vaAuPoint(new Vec2<ReadOnly>(-200, 1200), Speed.STANDARD);
		} catch (UnableToMoveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
