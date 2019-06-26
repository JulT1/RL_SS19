package mario;

import java.util.Random;

import org.hswgt.teachingbox.core.rl.agent.Agent;
import org.hswgt.teachingbox.core.rl.env.Action;
import org.hswgt.teachingbox.core.rl.learner.TabularQLearner;
import org.hswgt.teachingbox.core.rl.policy.EpsilonGreedyPolicy;
import org.hswgt.teachingbox.core.rl.policy.Policy;
import org.hswgt.teachingbox.core.rl.env.State;
import org.hswgt.teachingbox.core.rl.tabular.HashQFunction;

import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.MarioAIOptions;



public class Main {

	private static MarioAIOptions marioAIOptions;
	private static MarioEnvironment environment = MarioEnvironment.getInstance();
	
    private static int levelRandSeed = 1000;
	private static Random randGen = new Random();
	
	public static final double EPSILON = 0.1;
	public static final double GAMMA = 0.95;
	public static final double ALPHA = 1.0;
	
	public static void main(String[] args) {
		marioAIOptions = new MarioAIOptions("");
		marioAIOptions.setFlatLevel(false);
		marioAIOptions.setBlocksCount(true);
		marioAIOptions.setCoinsCount(true);
		marioAIOptions.setLevelRandSeed(levelRandSeed); // comment out for random levels
		marioAIOptions.setVisualization(true); // false: no visualization => faster learning
		marioAIOptions.setGapsCount(false);
		marioAIOptions.setMarioMode(2);
		marioAIOptions.setLevelLength(80);
		marioAIOptions.setCannonsCount(false);
		marioAIOptions.setTimeLimit(100);
		marioAIOptions.setDeadEndsCount(false);
		marioAIOptions.setTubesCount(false);
		marioAIOptions.setLevelDifficulty(0);
	
		
		TeachingBoxAgent agentMario = new TeachingBoxAgent();

		marioAIOptions.setAgent(agentMario); 

	
	
		
		
		// reset environment
	    environment = MarioEnvironment.getInstance();
		environment.reset(marioAIOptions);
		
		// reset agent
	    agentMario.setObservationDetails(
	    		environment.getReceptiveFieldWidth(),
	    		environment.getReceptiveFieldHeight(),
	    		environment.getMarioEgoPos()[0],
	    		environment.getMarioEgoPos()[1]);
	 
		
		
		MarioTeachingEnv env = new MarioTeachingEnv();
	    
		// setup engine 
		HashQFunction Q = new HashQFunction (0 ,MarioTeachingEnv.ACTION_SET);
				
		// Policy
		Policy pi = new EpsilonGreedyPolicy(Q, MarioTeachingEnv.ACTION_SET, EPSILON);

		// configure Agent
		Agent agentTeaching = new Agent(pi);
		
		// configure learner
		TabularQLearner learner = new TabularQLearner(Q);
		
		learner.setAlpha(ALPHA);
		learner.setGamma(GAMMA);
		
		agentTeaching.addObserver(learner);
		
		State currentState = env.getState();
		double totalReward = -1;
		
		
	    // do some actions
		for (int step=0; step<1000; step++) {
			
			
			/*This Block is supposed to choose the action it seems very messi 
		since we re enter the chosen action into the agent of the Mario API this is
		probably unecessary.
		*/
			double rewardDiff = environment.getIntermediateReward() - totalReward;
		    totalReward = environment.getIntermediateReward();
			Action nextAction = agentTeaching.nextStep(currentState, rewardDiff, env.isTerminalState());
			boolean[] marioAiAction = new boolean[ch.idsia.benchmark.mario.environments.Environment.numberOfKeys];
			for (int i=0; i<ch.idsia.benchmark.mario.environments.Environment.numberOfKeys; i++) {
				marioAiAction[i] = ((int)nextAction.get(0) & (1<<i)) > 0 ? true : false;
			}
			agentMario.setAction(marioAiAction);
			
			// perform action
			environment.performAction(agentMario.getAction());
			marioAIOptions.setReceptiveFieldVisualized(true);
			
			// update environment
	    	environment.tick();
	        agentMario.integrateObservation(environment);	
	        
	        // 1) reward
	        System.out.println("  -> Reward: " + rewardDiff + " - totalReward=" + totalReward);
	        
	        // 2) position
	        float[] pos = environment.getMarioFloatPos();
	        float marioX = pos[0];
	        float marioY = pos[1];
	        System.out.println("  -> MarioPos: x=" + marioX + ", y=" + marioY);
	        
	        // 3) investigate scene for enemies/obstacles ...
	        byte[][] scene = environment.getMergedObservationZZ(1, 1);
	        
	        
	        // 4) is mario able to jump?	   
	        boolean canJump = (!environment.isMarioOnGround() || environment.isMarioAbleToJump()) ? true : false;
	        System.out.println("  -> canJump=" + canJump);
	        
	        // 5) Mario mode
			boolean isMarioInvulnerable = environment.getMarioInvulnerableTime() > 0 ? true : false;
			int marioMode = environment.getMarioMode();		
			int marioStatus = environment.getMarioStatus();
			System.out.println("  -> invulnerable=" + isMarioInvulnerable);
			System.out.println("  -> MarioMode=" + marioMode);
			System.out.println("  -> MarioStatus=" + marioStatus);
	    
			// 6) etc ...
}}}
