package obstacles.types;

import utils.Vec2;
import utils.permissions.ReadOnly;

/**
 * Cet obstacle a une forme étrange, car c'est la forme du robot quand il tourne.
 * Cet obstacle sert à savoir si on peut tourner en étant près d'un mur.
 * On utilise une approximation de cette forme avec plusieurs rectangles
 * (comme si le robot "sautait" d'angle en angle).
 * @author pf
 *
 */

public class ObstacleRotationRobot extends ObstacleRectanglesCollection
{
	
	public ObstacleRotationRobot(Vec2<ReadOnly> position, double angleDepart, double angleArrivee)
	{
		super(position);
		calculeOmbres(angleDepart, angleArrivee);
	}
	
/*	public static final void update(ObstacleRotationRobot o, Vec2<ReadOnly> position, double angleDepart, double angleArrivee)
	{
		Obstacle.setPosition(o, position);
		o.calculeOmbres(angleDepart, angleArrivee);
	}
	*/

	private void calculeOmbres(double angleDepart, double angleArrivee)
	{
		double angleRelatif = (angleArrivee-angleDepart) % (2*Math.PI);
		if(angleRelatif > Math.PI)
			angleRelatif -= 2*Math.PI;
		else if(angleRelatif < -Math.PI)
			angleRelatif += 2*Math.PI;
//		log.debug("Math.abs(angleRelatif)/anglePas = "+Math.abs(angleRelatif)/anglePas, this);
		nbRectangles = (int)Math.ceil(Math.abs(angleRelatif)/anglePas)+1;
		ombresRobot = (ObstacleRectangular[]) new ObstacleRectangular[nbRectangles]; // le dernier est à part
		for(int i = 0; i < nbRectangles-1; i++)
			ombresRobot[i] = new ObstacleRectangular(position.getReadOnly(), longueurRobot, largeurRobot, angleDepart-i*anglePas*Math.signum(angleRelatif));

		ombresRobot[nbRectangles-1] = new ObstacleRectangular(position.getReadOnly(), longueurRobot, largeurRobot, angleArrivee);
	}
	
}
