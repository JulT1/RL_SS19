package mario;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.hswgt.teachingbox.core.rl.datastructures.ActionFilter;
import org.hswgt.teachingbox.core.rl.datastructures.ActionSet;
import org.hswgt.teachingbox.core.rl.datastructures.StateSet;
import org.hswgt.teachingbox.core.rl.env.Action;
import org.hswgt.teachingbox.core.rl.env.Environment;
import org.hswgt.teachingbox.core.rl.env.State;
import org.hswgt.teachingbox.core.rl.gridworldeditor.gui.GridWorldGUI;
import org.hswgt.teachingbox.core.rl.gridworldeditor.model.GridModel;
import org.hswgt.teachingbox.core.rl.valuefunctions.QFunction;

import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;


import org.hswgt.teachingbox.core.rl.datastructures.ActionSet;
import org.hswgt.teachingbox.core.rl.env.Action;

import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.MarioAIOptions;

public class MarioTeachingEnv implements Environment {
	private static final long serialVersionUID = -6102052217492409393L;
    double totalReward = -1;

	// Actions
	public MarioEnvironment environment;
	public static final ActionSet ACTION_SET = new ActionSet();	
	public final static StateSet STATE_SET = new StateSet();
	protected static LinkedHashMap <Action, String> ACTIONS = new LinkedHashMap<Action, String>();
	protected static LinkedHashMap <String, Action> ACTION_MAP = new LinkedHashMap<String,Action>();
	static {
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_LEFT)}), "LEFT");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_RIGHT)}), "RIGHT");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_UP)}), "UP");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_DOWN)}), "DOWN");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_JUMP)}), "JUMP");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_SPEED)}), "SPEED");
		
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_SPEED | 1<< Mario.KEY_JUMP)}), "SPEED_JUMP");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_SPEED | 1<< Mario.KEY_RIGHT)}), "SPEED_RIGHT");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_SPEED | 1<< Mario.KEY_LEFT)}), "SPEED_LEFT");
		
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_JUMP | 1<< Mario.KEY_RIGHT)}), "JUMP_RIGHT");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_JUMP | 1<< Mario.KEY_LEFT)}), "JUMP_LEFT");
		
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_JUMP | 1<< Mario.KEY_SPEED | 1<< Mario.KEY_RIGHT)}), "JUMP_SPEED_RIGHT");
		ACTIONS.put(new Action(new double[]{(double)(1 << Mario.KEY_JUMP | 1<< Mario.KEY_SPEED | 1<< Mario.KEY_LEFT)}), "JUMP_SPEED_LEFT");
		
        for (Entry <Action, String> e : ACTIONS.entrySet()) {
			ACTION_SET.add(e.getKey());
			ACTION_MAP.put(e.getValue(), e.getKey());
        }
	}
	public MarioTeachingEnv() {
	    MarioTeachingEnv.STATE_SET.add(new State (new double[] {0.0, 0.0}));
	    MarioTeachingEnv.STATE_SET.add(new State (new double[] {1.0, 0.0}));
	    MarioTeachingEnv.STATE_SET.add(new State (new double[] {0.0, 1.0}));
	    MarioTeachingEnv.STATE_SET.add(new State (new double[] {1.0, 1.0}));
	}
	private static boolean isObstacle(byte[][] scene, int x, int y) {
		switch(scene[y][x]) {
			case GeneralizerLevelScene.BRICK:
			case GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH:
			case GeneralizerLevelScene.FLOWER_POT_OR_CANNON:
			case GeneralizerLevelScene.LADDER:
			//System.out.println ("OBSTACLE: " + scene[y][x]);
			return true;
		}
		return false;
	}
	
   
    
    boolean isObstacleAhead(byte[][] scene, int posx, int posy, int radius) {
    	boolean obstacle= false;
    	for (int i=0; i<radius; i++) {
    	 obstacle= obstacle || isObstacle(scene,posx,posy); 
     }
    	return obstacle;
    }
    
    
    
    public static double booleanToDouble(boolean b) {
        if (b) {
            return 1;
        }
        return 0;
    }
	

	public double doAction(Action action, MarioEnvironment environment) {
		// TODO Auto-generated method stub
		 double rewardDiff = environment.getIntermediateReward() - totalReward;
	     totalReward = environment.getIntermediateReward();
		return rewardDiff;
	}


	public State getState(MarioEnvironment environment) {
		// TODO Auto-generated method stub
		if(isTerminalState()) {
			return new State(new double[]{0.5, 0.5});
		}
		int[] pos = environment.getMarioEgoPos();
		int marioX = pos[0];
		int marioY = pos[1];
		byte[][] scene = environment.getMergedObservationZZ(1, 1);
		boolean ahead = isObstacleAhead(scene, marioX, marioY, 3);
		boolean canJump = (!environment.isMarioOnGround() || environment.isMarioAbleToJump()) ? true : false;
		double doubleAhead=booleanToDouble(ahead);
		double doubleJump=booleanToDouble(canJump);
		State current_state= new State(new double[]{doubleAhead,doubleJump});
		return current_state;
	}


	public boolean isTerminalState(MarioEnvironment environment) {
		if(environment.isLevelFinished()) {
			return true;
		}
		if(environment.getMarioStatus()==Mario.STATUS_DEAD){
			return true;
		}
		if(environment.getTimeSpent()>100){
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void initRandom() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(State state) {
		// TODO Auto-generated method stub
		
	}

}
