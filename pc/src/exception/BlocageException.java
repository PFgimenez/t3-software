package exception;

import robot.RobotVrai;

public class BlocageException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8074280063169359572L;

	public BlocageException()
	{
		super();
	}
	
	public BlocageException(String m)
	{
		super(m);
	}

	public BlocageException(RobotVrai robotvrai)
	{
		super();
		robotvrai.annuleConsigneOrientation();
	}
	
}
