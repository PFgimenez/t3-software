package tests;

import org.junit.Before;

import container.ServiceNames;
import enums.RobotColor;
import robot.RobotReal;
import strategie.GameState;
import utils.ConfigInfo;
import vec2.ReadOnly;
import vec2.ReadWrite;
import vec2.Vec2;

/**
 * Tests unitaires des scripts.
 * Utilisé pour voir en vrai comment agit le robot et si la table est bien mise à jour.
 * @author pf
 *
 */

public class JUnit_Script_Symetrie extends JUnit_Test {

	private GameState<RobotReal,ReadWrite> gamestate;

    @SuppressWarnings("unchecked")
	@Before
    public void setUp() throws Exception {
        super.setUp();
        config.set(ConfigInfo.COULEUR, RobotColor.getCouleurAvecSymetrie());
        gamestate = (GameState<RobotReal,ReadWrite>) container.getService(ServiceNames.REAL_GAME_STATE);
        GameState.setPosition(gamestate, new Vec2<ReadOnly>(1100, 1000));
    }

}
