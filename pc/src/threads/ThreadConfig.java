package threads;

import container.Container;
import container.Service;
import container.ServiceNames;
import utils.Config;
import utils.Log;

/**
 * S'occupe de la mise a jour de la config. Surveille config
 * @author pf
 *
 */

public class ThreadConfig extends Thread implements Service {

	protected Log log;
	protected Config config;
	private Container container;
	
	public ThreadConfig(Log log, Config config, Container container)
	{
		this.log = log;
		this.container = container;
		this.config = config;
	}

	@Override
	public void run()
	{
		while(true)
		{
			synchronized(config)
			{
				try {
					config.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			for(ServiceNames name: ServiceNames.values())
			{
				Service service = container.getInstanciedService(name);
				if(service != null)
					service.updateConfig(config);
			}
		}
	}

	@Override
	public void updateConfig(Config config)
	{}

	@Override
	public void useConfig(Config config)
	{}

}
