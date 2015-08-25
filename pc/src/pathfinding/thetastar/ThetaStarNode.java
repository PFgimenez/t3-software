package pathfinding.thetastar;

import permissions.ReadWrite;
import robot.RobotChrono;
import strategie.GameState;

/**
 * Un noeud de ThetaStar
 * @author pf
 *
 */

public class ThetaStarNode
{

	public GameState<RobotChrono,ReadWrite> state;
	public ThetaStarNode came_from;
	public LocomotionArc came_from_arc;
	public int g_score;
	public int f_score;
	public final int hash;
	public long nbPF = 0;

	public ThetaStarNode(GameState<RobotChrono, ReadWrite> state, int hash)
	{
		this.state = state;
		this.hash = hash;
	}
	
	public void init()
	{
		came_from = null;
		came_from_arc = null;
		g_score = Integer.MAX_VALUE;
		f_score = Integer.MAX_VALUE;		
	}
	
	@Override
	public int hashCode()
	{
		return hash;
	}
	
	@Override
	public boolean equals(Object o)
	{
		return hashCode() == o.hashCode();
	}
	
	public int toInt()
	{
		return (f_score << 16) - g_score;
	}

}
