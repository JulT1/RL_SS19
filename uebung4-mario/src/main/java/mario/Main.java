package mario;

import java.util.Random;

import org.hswgt.teachingbox.core.rl.agent.Agent;
import org.hswgt.teachingbox.core.rl.env.Action;
import org.hswgt.teachingbox.core.rl.learner.TabularQLearner;
import org.hswgt.teachingbox.core.rl.policy.EpsilonGreedyPolicy;
import org.hswgt.teachingbox.core.rl.policy.Policy;
import org.hswgt.teachingbox.core.rl.env.State;
import org.hswgt.teachingbox.core.rl.tabular.HashQFunction;

import ch.idsia.tools.MarioAIOptions;



public class Main {

	private static MarioAIOptions marioAIOptions;
	public static MarioTeachingEnv teachingEnv = new MarioTeachingEnv();

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
		//marioAIOptions.setLevelRandSeed(levelRandSeed); // comment out for random levels
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
	    teachingEnv.marioEnv.reset(marioAIOptions);
		
		// reset agent
	    agentMario.setObservationDetails(
	    		teachingEnv.marioEnv.getReceptiveFieldWidth(),
	    		teachingEnv.marioEnv.getReceptiveFieldHeight(),
	    		teachingEnv.marioEnv.getMarioEgoPos()[0],
	    		teachingEnv.marioEnv.getMarioEgoPos()[1]);
	 
		
		
	    
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
		
		//State currentState = teachingEnv.getState();
		//System.out.println(currentState);
		double rewardDiff =0;
		agentTeaching.start(teachingEnv.getState());
		
	    // do some actions
		for (int step=0; step<1000; step++) {
			
			
			/*This Block is supposed to choose the action it seems very messi 
		since we re enter the chosen action into the agent of the Mario API this is
		probably unecessary.
		*/
			State currState = teachingEnv.getState();
			System.out.println(currState);
			Action nextAction = agentTeaching.nextStep(currState, rewardDiff, teachingEnv.isTerminalState());
			rewardDiff = teachingEnv.doAction(nextAction);
			
			boolean[] marioAiAction = new boolean[ch.idsia.benchmark.mario.environments.Environment.numberOfKeys];
			for (int i=0; i<ch.idsia.benchmark.mario.environments.Environment.numberOfKeys; i++) {
				marioAiAction[i] = ((int)nextAction.get(0) & (1<<i)) > 0 ? true : false;
			}

			// perform action
			teachingEnv.marioEnv.performAction(marioAiAction);
			marioAIOptions.setReceptiveFieldVisualized(true);
			
			// update environment
	    	teachingEnv.marioEnv.tick();
	        agentMario.integrateObservation(teachingEnv.marioEnv);	
	        
	        // 1) reward
	        System.out.println("  -> Reward: " + rewardDiff + " - totalReward=" + teachingEnv.totalReward);
	        
	        // 2) position
	        float[] pos = teachingEnv.marioEnv.getMarioFloatPos();
	        float marioX = pos[0];
	        float marioY = pos[1];
	        System.out.println("  -> MarioPos: x=" + marioX + ", y=" + marioY);
	        
	        // 3) investigate scene for enemies/obstacles ...
	        byte[][] scene = teachingEnv.marioEnv.getMergedObservationZZ(1, 1);
	        
	        
	        // 4) is mario able to jump?	   
	        boolean canJump = (!teachingEnv.marioEnv.isMarioOnGround() || teachingEnv.marioEnv.isMarioAbleToJump()) ? true : false;
	        System.out.println("  -> canJump=" + canJump);
	        
	        // 5) Mario mode
			boolean isMarioInvulnerable = teachingEnv.marioEnv.getMarioInvulnerableTime() > 0 ? true : false;
			int marioMode = teachingEnv.marioEnv.getMarioMode();		
			int marioStatus = teachingEnv.marioEnv.getMarioStatus();
			System.out.println("  -> invulnerable=" + isMarioInvulnerable);
			System.out.println("  -> MarioMode=" + marioMode);
			System.out.println("  -> MarioStatus=" + marioStatus);
	    
			// 6) etc ...
}}}
