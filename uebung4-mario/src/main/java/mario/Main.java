package mario;

import java.util.Random;

import org.hswgt.teachingbox.core.rl.agent.Agent;
import org.hswgt.teachingbox.core.rl.env.Action;
import org.hswgt.teachingbox.core.rl.learner.TabularQLearner;
import org.hswgt.teachingbox.core.rl.plot.DataAveragePlotter;
import org.hswgt.teachingbox.core.rl.policy.EpsilonGreedyPolicy;
import org.hswgt.teachingbox.core.rl.policy.Policy;
import org.hswgt.teachingbox.core.rl.env.State;
import org.hswgt.teachingbox.core.rl.experiment.CumulativeRewardAverager;
import org.hswgt.teachingbox.core.rl.experiment.Experiment;
import org.hswgt.teachingbox.core.rl.experiment.RewardAverager;
import org.hswgt.teachingbox.core.rl.tabular.HashQFunction;
import org.hswgt.teachingbox.core.rl.tools.ObjectSerializer;

import ch.idsia.tools.MarioAIOptions;



public class Main {

	private static MarioAIOptions marioAIOptions;
	public static MarioTeachingEnv teachingEnv = new MarioTeachingEnv();

    private static int levelRandSeed = 1000123;
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
		marioAIOptions.setMarioMode(0);
		marioAIOptions.setLevelLength(80);
		marioAIOptions.setCannonsCount(false);
		marioAIOptions.setTimeLimit(100);
		marioAIOptions.setDeadEndsCount(false);
		marioAIOptions.setTubesCount(false);
		marioAIOptions.setLevelDifficulty(0);
		//actually can be stopped after a level is finished as well, depends on what we want.
		final int STEPS_PER_EPISODE=2000;
		
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
		DataAveragePlotter cumRewardPlotter = new DataAveragePlotter("crawler-cumReward.png", "Mario - Cumulative reward");
		cumRewardPlotter.setLabel("Steps", "Cumulative Reward");
		CumulativeRewardAverager cra = new CumulativeRewardAverager (STEPS_PER_EPISODE, "");
		DataAveragePlotter stepRewardPlotter = new DataAveragePlotter("crawler-rewardPerStep.png", "Mario - Rewards per step");
		stepRewardPlotter.setLabel("Steps", "Average reward per step");
		RewardAverager ra = new RewardAverager (STEPS_PER_EPISODE, "");
		stepRewardPlotter.addScalarAverager(ra);
		stepRewardPlotter.setTics(
				new double[]{0, 10, STEPS_PER_EPISODE},  
				new double[]{0.0, 0.1, 1.5}
		);  
		
	    HashQFunction Q;
		// setup engine 
		Q = ObjectSerializer.load("q-func");
		if (Q==null) {
			Q = new HashQFunction (0 ,MarioTeachingEnv.ACTION_SET);
		}

				
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
		State startState = teachingEnv.getState();
		agentTeaching.start(startState);
		
		Experiment experiment = new Experiment (agentTeaching, teachingEnv, 1, 300);
		experiment.setInitState(startState);

		experiment.addObserver(ra);
		experiment.addObserver(cra);
	    // do some actions
		experiment.run();

		for (int step=0; step<STEPS_PER_EPISODE; step++) {

			/*This Block is supposed to choose the action it seems very messi 
		since we re enter the chosen action into the agent of the Mario API this is
		probably unecessary.
		*/
			
			if (teachingEnv.isTerminalState()) {
				//not sure if saving here makes sense
				ObjectSerializer.save("q-func", Q);
				//should the reward be saved at this point or not ?
				marioAIOptions.setLevelRandSeed(levelRandSeed+step); // comment out for random levels

				teachingEnv.marioEnv.reset(marioAIOptions);
				agentMario.setObservationDetails(
			    teachingEnv.marioEnv.getReceptiveFieldWidth(),
			    teachingEnv.marioEnv.getReceptiveFieldHeight(),
			    teachingEnv.marioEnv.getMarioEgoPos()[0],
			    teachingEnv.marioEnv.getMarioEgoPos()[1]);
			 			}

			State currState = teachingEnv.getState();
			System.out.println(currState);
			Action nextAction = agentTeaching.nextStep(currState, rewardDiff, teachingEnv.isTerminalState());
			


			// perform action
			rewardDiff = teachingEnv.doAction(nextAction);

			marioAIOptions.setReceptiveFieldVisualized(true);
			
			// update environment
	    	teachingEnv.marioEnv.tick();
	        agentMario.integrateObservation(teachingEnv.marioEnv);	
	        
	        // 1) reward
	        System.out.println("  -> currReward: " + rewardDiff + ", totalReward=" + teachingEnv.totalReward);
	        
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
}
		ObjectSerializer.save("q-func", Q);

		//ra.setConfigString("e="+EPSILON);
		//stepRewardPlotter.plot();
		//cra.setConfigString("e="+EPSILON);
		//cumRewardPlotter.plot();	
	}}
