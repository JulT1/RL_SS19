package mario;



import ch.idsia.agents.Agent;
import ch.idsia.agents.controllers.BasicMarioAIAgent;

import ch.idsia.benchmark.mario.environments.Environment;

public class TeachingBoxAgent extends BasicMarioAIAgent implements Agent {
	
	public TeachingBoxAgent()
	{
	    super("TeachingBoxAgent");
	    reset();
	}

	private boolean[] action = null;

	public void reset()
	{
		action = new boolean[Environment.numberOfKeys];
	}
	
	public void setAction(boolean[] action)
	{
		this.action = action;
	}

	public boolean[] getAction()
	{
		
	    return this.action;
	}
	
}

