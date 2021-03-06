package hook.methods;

import pathfinding.ChronoGameState;
import table.GameElementNames;

import java.util.ArrayList;

import enums.SerialProtocol;
import enums.Tribool;
import hook.Executable;

/**
 * Modifie l'état des éléments de jeux
 * @author pf
 *
 */

public class GameElementDone implements Executable
{
	private ChronoGameState state;
	private Tribool done;
	private GameElementNames element;
	
	public GameElementDone(ChronoGameState state, GameElementNames element, Tribool done)
	{
		this.state = state;
		this.done = done;
		this.element = element;
	}
	
	@Override
	public void execute()
	{
		// on ne peut faire qu'augmenter l'état d'un élément de jeu.
		// c'est-à-dire qu'on peut passer de FALSE à MAYBE et TRUE
		// et de MAYBE à TRUE.
		// Les autres transitions sont interdites (en particulier passer de TRUE à MAYBE...)
		if(state.table.isDone(element).hash < done.hash)
			state.table.setDone(element, done);
	}

	@Override
	public void updateGameState(ChronoGameState state)
	{
		this.state = state;
	}
	
	@Override
	public ArrayList<Byte> toSerial()
	{
		ArrayList<Byte> out = new ArrayList<Byte>();
		out.add((byte)(SerialProtocol.CALLBACK_ELEMENT.code+element.ordinal()));
		return out;
	}


}
