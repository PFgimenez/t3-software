package robot;

import robot.cardsWrappers.ActuatorCardWrapper;
import robot.cardsWrappers.enums.ActuatorOrder;
import robot.cardsWrappers.enums.HauteurBrasClap;
import table.Table;
import utils.Log;
import utils.Config;
import utils.Sleep;
import utils.Vec2;
import hook.Hook;

import java.util.ArrayList;

import astar.arc.PathfindingNodes;
import enums.Side;
import exceptions.FinMatchException;
import exceptions.ScriptHookException;
import exceptions.SerialConnexionException;
import exceptions.UnableToMoveException;
import exceptions.WallCollisionDetectedException;

/**
 * Effectue le lien entre le code et la réalité (permet de parler aux actionneurs, d'interroger les capteurs, etc.)
 * @author pf, marsu
 *
 */

public class RobotReal extends Robot
{
//	private Table table;
	private Locomotion deplacements;
	private ActuatorCardWrapper actionneurs;

	// Constructeur
	public RobotReal(ActuatorCardWrapper actuator, Locomotion deplacements, Table table, Config config, Log log)
 	{
		super(config, log);
		this.actionneurs = actuator;
		this.deplacements = deplacements;
//		this.table = table;
		updateConfig();
	}
	
	/*
	 * MÉTHODES PUBLIQUES
	 */
	
	public void updateConfig()
	{
		super.updateConfig();
	}
	
	
	public void desactiver_asservissement_rotation() throws FinMatchException
	{
		deplacements.disableRotationnalFeedbackLoop();
	}

	public void desactiver_asservissement_translation() throws FinMatchException
	{
		deplacements.disableTranslationalFeedbackLoop();
	}

	public void activer_asservissement_rotation() throws FinMatchException
	{
		deplacements.enableRotationnalFeedbackLoop();
	}

	public void recaler() throws FinMatchException
	{
	    set_vitesse(Speed.READJUSTMENT);
	    deplacements.readjust();
	}
	
	/**
	 * Avance d'une certaine distance donnée en mm (méthode bloquante), gestion des hooks
	 * @throws UnableToMoveException 
	 * @throws FinMatchException 
	 * @throws ScriptHookException 
	 */
	@Override
    public void avancer(int distance, ArrayList<Hook> hooks, boolean mur) throws UnableToMoveException, FinMatchException
	{
		// Il est nécessaire d'ajouter le hookFinMatch avant chaque appel de deplacements qui prenne un peu de temps (avancer, tourner, ...)
		hooks.add(hookFinMatch);
		try {
			deplacements.moveLengthwise(distance, hooks, mur);
		} catch (ScriptHookException e) {
			log.debug("Script de hook ignoré", this);
			e.printStackTrace();
		}
	}	

	/**
	 * Modifie la vitesse de translation
	 * @param Speed : l'une des vitesses indexées dans enums.
	 * @throws FinMatchException 
	 * 
	 */
	@Override
	public void set_vitesse(Speed vitesse) throws FinMatchException
	{
        deplacements.setTranslationnalSpeed(vitesse);
        deplacements.setRotationnalSpeed(vitesse);
		log.debug("Modification de la vitesse: "+vitesse, this);
	}
	
	/*
	 * ACTIONNEURS
	 */
	
	/* 
	 * GETTERS & SETTERS
	 */
	@Override
	public void setPosition(Vec2 position) throws FinMatchException
	{
	    deplacements.setPosition(position);
	}
	
    @Override
	public Vec2 getPosition() throws FinMatchException
	{
	    return deplacements.getPosition();
	}

	@Override
	public void setOrientation(double orientation) throws FinMatchException
	{
	    deplacements.setOrientation(orientation);
	}

    @Override
    public double getOrientation() throws FinMatchException
    {
        return deplacements.getOrientation();
    }

    /**
	 * Méthode sleep utilisée par les scripts
     * @throws FinMatchException 
	 */
	@Override	
	public void sleep(long duree, ArrayList<Hook> hooks) throws FinMatchException
	{
		Sleep.sleep(duree);
		for(Hook hook: hooks)
			try {
				hook.evaluate();
			} catch (ScriptHookException e) {
				// Impossible d'avoir des scripts hook pendant un sleep
				e.printStackTrace();
			} catch (WallCollisionDetectedException e) {
				// Impossible pendant un sleep
				e.printStackTrace();
			}
	}

    @Override
    public void stopper() throws FinMatchException
    {
        deplacements.immobilise();
    }

    @Override
    public void tourner(double angle) throws UnableToMoveException, FinMatchException
    {
    	ArrayList<Hook> hooks = new ArrayList<Hook>();
		hooks.add(hookFinMatch);
		try {
			deplacements.turn(angle, hooks);
		} catch (ScriptHookException e) {
			log.debug("Script de hook ignoré", this);
			e.printStackTrace();
		}
    }
    
    @Override
    public void suit_chemin(ArrayList<PathfindingNodes> chemin, ArrayList<Hook> hooks) throws UnableToMoveException, FinMatchException, ScriptHookException
    {
		hooks.add(hookFinMatch);
		// DEPENDS ON RULES
        deplacements.followPath(chemin, hooks, DirectionStrategy.FORCE_FORWARD_MOTION);
    }
    
	@Override
    public RobotChrono cloneIntoRobotChrono() throws FinMatchException
    {
    	RobotChrono rc = new RobotChrono(config, log);
    	copy(rc);
    	return rc;
    }
    
    // Cette copie est un peu plus lente que les autres car il y a un appel série
    // Néanmoins, on ne fait cette copie qu'une fois par arbre.
    @Override
    public void copy(RobotChrono rc) throws FinMatchException
    {
        super.copy(rc);
        getPosition().copy(rc.position);
        rc.orientation = getOrientation();
    }

	@Override
    public int getTempsDepuisDebutMatch()
    {
    	return (int)(System.currentTimeMillis() - Config.getDateDebutMatch());
    }
	
	public void leverDeuxTapis(boolean needToSleep) throws FinMatchException
	{
		try {
			actionneurs.useActuator(ActuatorOrder.LEVE_TAPIS_GAUCHE);
			if(needToSleep)
				leverTapisSleep();
			actionneurs.useActuator(ActuatorOrder.LEVE_TAPIS_DROIT);
			if(needToSleep)
				leverTapisSleep();
		} catch (SerialConnexionException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void poserDeuxTapis(boolean needToSleep) throws FinMatchException
	{
		try {
			actionneurs.useActuator(ActuatorOrder.BAISSE_TAPIS_GAUCHE);
			if(needToSleep)
				poserTapisSleep();
			actionneurs.useActuator(ActuatorOrder.BAISSE_TAPIS_DROIT);
			if(needToSleep)
				poserTapisSleep();
	    	tapisPoses = true;
			pointsObtenus = pointsObtenus + 24;
		} catch (SerialConnexionException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void bougeBrasClap(Side cote, HauteurBrasClap hauteur, boolean needToSleep) throws SerialConnexionException, FinMatchException
	{
		/**
		 * Symétrie:
		 * une couleur lève le bras gauche,
		 * une autre le bras droit.
		 */
		if(symetrie)
			cote = cote.getSymmetric();
		ActuatorOrder order = bougeBrasClapOrder(cote, hauteur);
		actionneurs.useActuator(order);
		if(needToSleep)
			bougeBrasClapSleep(order);
	}
	
	@Override
	public void clapTombe()
	{
		pointsObtenus = pointsObtenus + 5;		
	}

	public boolean isEnemyHere() {
		return deplacements.isEnemyHere();
	}
	
	public void closeSerialConnections()
	{
		deplacements.close();
		actionneurs.close();
	}

	public void initActuatorLocomotion()
	{
		// TODO: replier les bras, etc
	}

}
