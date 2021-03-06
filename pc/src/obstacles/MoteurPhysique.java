package obstacles;

import obstacles.types.Obstacle;
import obstacles.types.ObstacleProximity;
import obstacles.types.ObstacleRectangular;
import pathfinding.ChronoGameState;
import pathfinding.astarCourbe.AStarCourbeNode;
import table.GameElementNames;
import utils.Config;
import utils.ConfigInfo;
import utils.Log;
import utils.Vec2;
import utils.permissions.ReadOnly;
import container.Service;
import enums.Tribool;

/**
 * Service qui permet de calculer, pour un certain GameState, des collisions
 * @author pf
 *
 */

public class MoteurPhysique implements Service {
	
	protected Log log;
	
	private int distanceApproximation;
	
	public MoteurPhysique(Log log)
	{
		this.log = log;
	}

    /**
     * Cette méthode vérifie les obstacles fixes uniquement.
     * Elle est utilisée dans le lissage.
     * @param A
     * @param B
     * @return
     */
    public boolean obstacleFixeDansSegmentPathfinding(Vec2<ReadOnly> A, Vec2<ReadOnly> B)
    {
    	ObstacleRectangular chemin = new ObstacleRectangular(A, B);
    	for(ObstaclesFixes o: ObstaclesFixes.values)
    	{
    		if(chemin.isColliding(o.getObstacle()))
    			return true;
    	}
    	return false;
    }
 
	/**
	 * Y a-t-il un obstacle de table dans ce segment?
	 * @param A
	 * @param B
	 * @return
	 */
    public boolean obstacleTableDansSegment(ChronoGameState state, Vec2<ReadOnly> A, Vec2<ReadOnly> B)
    {
    	ObstacleRectangular chemin = new ObstacleRectangular(A, B);
        for(GameElementNames g: GameElementNames.values())
        	// Si on a interprété que l'ennemi est passé sur un obstacle,
        	// on peut passer dessus par la suite.
            if(state.table.isDone(g) == Tribool.FALSE && obstacle_proximite_dans_segment(g, chemin))
            {
//            	log.debug(o.getName()+" est dans le chemin.", this);
                return true;
            }

        return false;    	
    }

    /**
     * Y a-t-il un obstacle de proximité dans ce segment?
     * Va-t-il disparaître pendant le temps de parcours?
     * @param sommet1
     * @param sommet2
     * @return
     */
    public boolean obstacleProximiteDansSegment(ChronoGameState state, Vec2<ReadOnly> A, Vec2<ReadOnly> B, int date)
    {
    	state.iterator.reinit();
    	while(state.iterator.hasNext())
        	if(state.iterator.next().obstacle_proximite_dans_segment(A, B, date))
        		return true;

        return false;
    }

    /**
     * Utilisé pour savoir si ce qu'on voit est un obstacle fixe.
     * @param position
     * @return
     */
    public boolean isObstacleFixePresentCapteurs(Vec2<ReadOnly> position)
    {
    	for(ObstaclesFixes o: ObstaclesFixes.obstaclesFixesVisibles)
            if(isObstaclePresent(position, o.getObstacle(), distanceApproximation))
                return true;
        return false;
    }
    
    /**
     * Indique si un obstacle fixe de centre proche de la position indiquée existe.
     * Cela permet de ne pas détecter en obstacle mobile des obstacles fixes.
     * De plus, ça allège le nombre d'obstacles.
     * Utilisé pour savoir s'il y a un ennemi devant nous.
     * @param position
     * @return
     */
    public boolean isObstacleMobilePresent(ChronoGameState state, Vec2<ReadOnly> position, int distance) 
    {
    //    if(isThereHypotheticalEnemy && isObstaclePresent(position, hypotheticalEnemy.getReadOnly(), distance))
    //    	return true;
/*    	state.iterator.reinit();
    	while(state.iterator.hasNext())
        	if(isObstaclePresent(position, state.iterator.next(), distance))
        		return true;
 */       return false;
    }

    /**
     * Vérifie pour un seul obstacle.
     * Renvoie true si un obstacle est proche
     * @param position
     * @param o
     * @param distance
     * @return
     */
    private boolean isObstaclePresent(Vec2<ReadOnly> position, Obstacle o, int distance)
    {
    	return o.isProcheObstacle(position, distance);
    }
    
	@Override
	public void updateConfig(Config config)
	{}

	@Override
	public void useConfig(Config config)
	{
		distanceApproximation = config.getInt(ConfigInfo.DISTANCE_MAX_ENTRE_MESURE_ET_OBJET);		
	}
	
	/**
	 * Y a-t-il collision sur le chemin d'une trajectoire courbe ?
	 * @param node
	 * @return
	 */
	public boolean isTraversableCourbe(AStarCourbeNode node)
	{
		for(int i = 0; i < node.came_from_arc.getNbPoints(); i++)
		{
			ObstacleRectangular obs = new ObstacleRectangular(node.came_from_arc.getPoint(i), node.state.robot.isDeploye());
	
			// Collision avec un obstacle fixe?
	    	for(ObstaclesFixes o: ObstaclesFixes.values)
	    		if(obs.isColliding(o.getObstacle()))
	    			return false;
	
	    	node.state.iterator.reinit();
	    	while(node.state.iterator.hasNext())
	           	if(obs.isColliding(node.state.iterator.next()))
	        		return false;
		}
        return true;
	}
	
	/**
	 * g est-il proche de position? (utilisé pour vérifier si on shoot dans un élément de jeu)
	 * @param g
	 * @param position
	 * @param rayon_robot_adverse
	 * @return
	 */
	public boolean didTheEnemyTakeIt(GameElementNames g, ObstacleProximity o)
	{
		return g.getObstacle().isProcheObstacle(o.position, o.radius);
	}

	/**
	 * g est-il dans le segment[a, b]?
	 * @param g
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean obstacle_proximite_dans_segment(GameElementNames g, ObstacleRectangular o)
	{
		return o.isColliding(g.getObstacle());
	}

}
