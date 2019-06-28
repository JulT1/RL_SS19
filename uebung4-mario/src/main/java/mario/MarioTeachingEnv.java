package mario;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
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
import org.objectweb.asm.tree.IntInsnNode;

import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import java.lang.Math;
import org.hswgt.teachingbox.core.rl.datastructures.ActionSet;
import org.hswgt.teachingbox.core.rl.env.Action;

import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
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
			case GeneralizerLevelScene.FLOWER_POT_OR_CANNON:
			case GeneralizerLevelScene.LADDER:
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
		// TODO Auto-generated method stub
		 double rewardDiff = marioEnv.getIntermediateReward() - totalReward;
	     totalReward = marioEnv.getIntermediateReward();
		return rewardDiff;
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		steps++;
		if(isTerminalState()) {
			return new State(new double[]{-1.0});
		}
		currFloatPos = marioEnv.getMarioFloatPos();
		int[] currPos = marioEnv.getMarioEgoPos();
		byte[][] scene = marioEnv.getMergedObservationZZ(1, 1);
		boolean ahead = isObstacleAhead(scene, currPos, 3);
		boolean canJump = (marioEnv.isMarioOnGround() && marioEnv.isMarioAbleToJump()) ? true : false;
		double doubleAhead=booleanToDouble(ahead);
		double doubleJump=booleanToDouble(canJump);
		double dirX = booleanToDouble(currFloatPos[0] > prevFloatPos[0]);
		double dirY = booleanToDouble(currFloatPos[1] > prevFloatPos[1]);
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
