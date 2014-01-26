package tests;

import org.junit.Test;
import org.junit.Assert;

/**
 * Tests unitaires pour la configuration... juste épique.
 * @author pf
 *
 */

public class JUnit_ReadIniTest extends JUnit_Test {

	@Test
	public void test_get() throws Exception
	{
		log.debug("JUnit_ReadIniTest.test_get()", this);
		Assert.assertTrue(config.get("test1").equals("test2"));
	}

	@Test
	public void test_set() throws Exception
	{
		log.debug("JUnit_ReadIniTest.test_set()", this);
		config.set("test1", "test3");
		Assert.assertTrue(config.get("test1").equals("test3"));
	}

}
