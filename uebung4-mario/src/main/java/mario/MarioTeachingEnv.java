package mario;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import org.hswgt.teachingbox.core.rl.datastructures.ActionSet;
import org.hswgt.teachingbox.core.rl.datastructures.StateSet;
import org.hswgt.teachingbox.core.rl.env.Action;
import org.hswgt.teachingbox.core.rl.env.Environment;
import org.hswgt.teachingbox.core.rl.env.State;

import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;

import java.lang.Math;

import ch.idsia.benchmark.mario.engine.GeneralizerEnemies;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Enemy;

import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.MarioAIOptions;

public class MarioTeachingEnv implements Environment {
	private static final long serialVersionUID = -6102052217492409393L;
    public double totalReward = -1;
    float[] prevFloatPos,currFloatPos;


    int steps=0;
	// Actions
	public MarioEnvironment marioEnv;
	public static final int STATE_SIZE = 4;	
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
		marioEnv = MarioEnvironment.getInstance();
		//The loops here generate all possible binary like combinations of states of size STATE_SIZE, as a double array of course.
		for (int i=0; i<Math.pow(2, STATE_SIZE); i++) {
		    String currState= StringUtils.leftPad(Integer.toBinaryString(i), STATE_SIZE, '0');
			double[] stateArr= new double[STATE_SIZE];
		    for (int j=0; j<STATE_SIZE; j++) {
				stateArr[j]=Character.getNumericValue(currState.charAt(j))+0.0;
			}
			MarioTeachingEnv.STATE_SET.add(new State(stateArr));
		}
		currFloatPos=prevFloatPos= new float[]{(float)0.0, (float)0.0};
	}
	private static boolean isObstacle(byte[][] scene, int x, int y) {
		switch(scene[y][x]) {
			case GeneralizerLevelScene.BRICK:
			case GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH:
			case GeneralizerLevelScene.BORDER_HILL:
			case GeneralizerLevelScene.FLOWER_POT_OR_CANNON:
			case GeneralizerLevelScene.LADDER:
			case GeneralizerLevelScene.UNBREAKABLE_BRICK:
			case Enemy.KIND_RED_KOOPA:
			//System.out.println ("OBSTACLE: " + scene[y][x]);
			return true;
		}
		return false;
	}
	
   
    
    boolean isObstacleAhead(byte[][] scene, int pos[], int radius) {
    	for (int i=0; i<radius; i++) {
    		if (isObstacle(scene,pos[0] +i,pos[1]))
    			return true;
     }
    	return false;
    }
    
    
    
    public static double booleanToDouble(boolean b) {
        if (b) {
            return 1;
        }
        return 0;
    }
	
	@Override
	public double doAction(Action action) {
		boolean[] marioAiAction = new boolean[ch.idsia.benchmark.mario.environments.Environment.numberOfKeys];
		for (int i=0; i<ch.idsia.benchmark.mario.environments.Environment.numberOfKeys; i++) {
			marioAiAction[i] = ((int)action.get(0) & (1<<i)) > 0 ? true : false;
		}
		marioEnv.performAction(marioAiAction);
		State currState= getState();
		if (isTerminalState())
			//rewards winning here
			if (currState.get(0)==100.0)
				//reward mario by how fast he finished the level
				return marioEnv.getTimeLeft()*2;
			else
				return -50;

		 double rewardDiff = marioEnv.getIntermediateReward() - totalReward;
	     totalReward = marioEnv.getIntermediateReward();
	     if (currState.get(2)>0.0)
	    	return 5;
	     else if (action.equals(ACTION_MAP.get("LEFT")))
	    	return -1.5;
	     else if (action.equals(ACTION_MAP.get("SPEEDRIGHT")))
	    	 return 10;
	     else if (action.equals(ACTION_MAP.get("SPEEDLEFT")))
	    	 return -3;
	     else if (currState.get(0) >0.0 && action.equals(ACTION_MAP.get("JUMP_RIGHT")))
	    		 return 2;
	     else if (currState.get(0) >0.0 && action.equals(ACTION_MAP.get("JUMP")))
    		 return 1;
	     else if (action.equals(ACTION_MAP.get("JUMP")) ||action.equals(ACTION_MAP.get("JUMP_BACK")) )
	    	 //dont just jump around
	    	 return -2;
	     return 0;
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		steps++;
		if(isTerminalState()) {
			if (marioEnv.getMarioStatus() == MarioEnvironment.MARIO_STATUS_WIN) 
				return new State(new double[]{100.0});
			else if (marioEnv.getMarioStatus() == MarioEnvironment.MARIO_STATUS_DEAD) 
				return new State(new double[]{101.0});
		}
		currFloatPos = marioEnv.getMarioFloatPos();
		int[] currPos = marioEnv.getMarioEgoPos();
		byte[][] scene = marioEnv.getMergedObservationZZ(1, 1);
		boolean ahead = isObstacleAhead(scene, currPos, 5);
		boolean canJump = (marioEnv.isMarioOnGround() && marioEnv.isMarioAbleToJump()) ? true : false;
		double doubleAhead=booleanToDouble(ahead);
		double doubleJump=booleanToDouble(canJump);
		double dirX = booleanToDouble(currFloatPos[0] > prevFloatPos[0]);
		double dirY = booleanToDouble(currFloatPos[1] > prevFloatPos[1]);
		//check if changed direction from a few steps ago. 
		if (steps%5==0)
			prevFloatPos=currFloatPos.clone();



		State current_state= new State(new double[]{doubleAhead,doubleJump,dirX,dirY});
		return current_state;
	}

	@Override
	public boolean isTerminalState() {
		if(marioEnv.isLevelFinished()) {
			return true;
		}
		if(marioEnv.getMarioStatus()==Mario.STATUS_DEAD){
			return true;
		}
		if(marioEnv.getTimeSpent()>100){
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
