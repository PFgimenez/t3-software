package tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import astar.arc.PathfindingNodes;
import container.ServiceNames;
import table.GameElementNames;
import table.ObstacleManager;
import table.Table;
import utils.ConfigInfo;
import vec2.ReadOnly;
import vec2.Vec2;
import enums.Tribool;

/**
 * Tests unitaires pour ObstacleManager
 * @author pf
 *
 */

public class JUnit_ObstacleManager extends JUnit_Test {

	private ObstacleManager obstaclemanager;
	private Table table;
	
    @Before
    public void setUp() throws Exception {
        super.setUp();
        obstaclemanager = (ObstacleManager) container.getService(ServiceNames.OBSTACLE_MANAGER);
        table = (Table) container.getService(ServiceNames.TABLE);
        obstaclemanager.clearObstaclesMobiles();
    }

    @Test
    public void test_creation() throws Exception
    {
    	Assert.assertTrue(obstaclemanager.nbObstaclesMobiles() == 0);
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(500, 500), 0);
    	Assert.assertTrue(obstaclemanager.nbObstaclesMobiles() == 1);
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(-20, 10), 0);
    	Assert.assertTrue(obstaclemanager.nbObstaclesMobiles() == 2);
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(5000, 200), 0);
    	Assert.assertTrue(obstaclemanager.nbObstaclesMobiles() == 3);
    }

    // DEPENDS_ON_RULES
    @Test
    public void test_present() throws Exception
    {
    	Assert.assertTrue(!obstaclemanager.isObstacleFixePresentCapteurs(new Vec2<ReadOnly>(500, 500)));
    	Assert.assertTrue(obstaclemanager.isObstacleFixePresentCapteurs(new Vec2<ReadOnly>(30, 30)));
    	
    	// Vérification de la présence d'obstacles mobiles
    	Assert.assertTrue(!obstaclemanager.isObstacleMobilePresent(new Vec2<ReadOnly>(500, 500), 1));
    	Assert.assertTrue(!obstaclemanager.isObstacleMobilePresent(new Vec2<ReadOnly>(520, 520), 100));
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(500, 500), 0);
    	Assert.assertTrue(obstaclemanager.isObstacleMobilePresent(new Vec2<ReadOnly>(500, 500), 1));
    	Assert.assertTrue(obstaclemanager.isObstacleMobilePresent(new Vec2<ReadOnly>(520, 520), 100));
    }
    
    @Test
    public void test_peremption() throws Exception
    {
    	config.set(ConfigInfo.DUREE_PEREMPTION_OBSTACLES, 200); // 200 ms de péremption
    	obstaclemanager.updateConfig();
    	
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(0, 0), 0);
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(500, 500), 0);
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(0, 0), 0);
    	Assert.assertTrue(obstaclemanager.isObstacleMobilePresent(new Vec2<ReadOnly>(500, 500), 1));
    	obstaclemanager.supprimerObstaclesPerimes(100); // pas encore périmé
    	Assert.assertTrue(obstaclemanager.isObstacleMobilePresent(new Vec2<ReadOnly>(500, 500), 1));
    	obstaclemanager.supprimerObstaclesPerimes(300); // ce sera périmé à cette date 
    	Assert.assertTrue(!obstaclemanager.isObstacleMobilePresent(new Vec2<ReadOnly>(500, 500), 1));
    }
    
    // DEPENDS_ON_RULES
    @Test
    public void test_collision_obstacle_fixe() throws Exception
    {
    	Assert.assertTrue(obstaclemanager.obstacleFixeDansSegmentPathfinding(new Vec2<ReadOnly>(-1000, 30), new Vec2<ReadOnly>(1000, 30)));
    	Assert.assertTrue(!obstaclemanager.obstacleFixeDansSegmentPathfinding(new Vec2<ReadOnly>(500, 500), new Vec2<ReadOnly>(800, 800)));
    }

    @Test
    public void test_collision_obstacle_mobile() throws Exception
    {
    	config.setDateDebutMatch();
    	Assert.assertTrue(!obstaclemanager.obstacleProximiteDansSegment(new Vec2<ReadOnly>(100, 100), new Vec2<ReadOnly>(900, 900), 0));
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(-300, 500), 0);
    	Assert.assertTrue(!obstaclemanager.obstacleProximiteDansSegment(new Vec2<ReadOnly>(100, 100), new Vec2<ReadOnly>(900, 900), 0));
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(1300, 1300), 0);
    	Assert.assertTrue(!obstaclemanager.obstacleProximiteDansSegment(new Vec2<ReadOnly>(100, 100), new Vec2<ReadOnly>(900, 900), 0));
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(400, 500), 0);
    }
    @Test
    public void test_peremption_en_mouvement() throws Exception
    {
    	config.set(ConfigInfo.DUREE_PEREMPTION_OBSTACLES, 500); // 200 ms de péremption
    	obstaclemanager.updateConfig();
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(1200, 900), 0);
    	// Le temps qu'on y arrive, il devrait être périmé
    	Assert.assertTrue(!obstaclemanager.obstacleProximiteDansSegment(new Vec2<ReadOnly>(-1200, 100), new Vec2<ReadOnly>(1200, 900), 0));
    }

    @Test
    public void test_collision_element_jeu() throws Exception
    {
    	Assert.assertTrue(obstaclemanager.obstacleTableDansSegment(PathfindingNodes.BAS_DROITE.getCoordonnees(), PathfindingNodes.DEVANT_DEPART_GAUCHE.getCoordonnees()));
    }
	
    @Test
    public void test_ennemi_dans_element_jeu() throws Exception
    {
    	Assert.assertTrue(table.isDone(GameElementNames.PLOT_1) == Tribool.FALSE);
    	obstaclemanager.creerObstacle(new Vec2<ReadOnly>(1500, 150), 0);
    	Assert.assertTrue(table.isDone(GameElementNames.PLOT_1) == Tribool.MAYBE);
    }

}
