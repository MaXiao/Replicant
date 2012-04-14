package agent;

//	File:			Brain.java
//	Author:		Krzysztof Langner
//	Date:			1997/04/28
//
//    Modified by:	Paul Marlow

//    Modified by:      Edgar Acosta
//    Date:             March 4, 2008

//	  Modified by:	Xiao Ma,  Omi
//	  Date:			March 29, 2012

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

import model.DatasetBuilder;
import util.PlayerConfig;
import util.StringUtil;
import util.XmlUtil;
import weka.core.Instance;
import weka.core.Instances;

class Brain extends Thread implements SensorInput
{
	private Action action;
    //---------------------------------------------------------------------------
    // This constructor:
    // - stores connection to krislet
    // - starts thread for this object
    public Brain(SendCommand krislet, 
		 String team, 
		 char side, 
		 int number, 
		 String playMode,
		 String prototype)
    {
	m_timeOver = false;
	m_krislet = krislet;
	m_memory = new Memory();
	m_team = team;
	m_side = side;
	// m_number = number;
	m_playMode = playMode;
	m_prototype = prototype;
	start();
    }


    //---------------------------------------------------------------------------
    // This is main brain function used to make decision
    // In each cycle we decide which command to issue based on
    // current situation. the rules are:
    //
    //	1. If you don't know where is ball then turn right and wait for new info
    //
    //	2. If ball is too far to kick it then
    //		2.1. If we are directed towards the ball then go to the ball
    //		2.2. else turn to the ball
    //
    //	3. If we dont know where is opponent goal then turn wait 
    //				and wait for new info
    //
    //	4. Kick ball
    //
    //	To ensure that we don't send commands to often after each cycle
    //	we waits one simulator steps. (This of course should be done better)

    // ***************  Improvements ******************
    // Allways know where the goal is.
    // Move to a place on my side on a kick_off
    // ************************************************

    @SuppressWarnings("unused")
	public void run()
    {
	ObjectInfo object;
	VisualInfo vInfo;
	// Get action
	try {
		action = new Action(m_prototype);
	} catch (Exception e1) {
		System.err.println("We may not have prototype " + m_prototype);
		e1.printStackTrace();
	}
	// Get actionNumMap to check if this action appears in the dataset to avoid errors. Mainly for catch.
	HashMap<String, Double> actionNumMap = action.getActionNumMap();
	boolean hasKick = false, hasDash = false, hasTurn = false, hasCatch = false;
	if (actionNumMap.get("kick") != -1)
		hasKick = true;
	if (actionNumMap.get("dash") != -1)
		hasDash = true;
	if (actionNumMap.get("turn") != -1) 
		hasTurn = true;
	if (actionNumMap.get("catch") != -1)
		hasCatch = true;
	
	// Get attribute setting for each model
	PlayerConfig config = null;
	try {
		config = XmlUtil.getPlayerConfig(m_prototype);
	} catch (Exception e2) {
		e2.printStackTrace();
	}
	HashMap<String, HashMap<String, Boolean>> attSetting = config.getAttSetting();
	HashMap<String, Boolean> actionTypeAttSetting = attSetting.get(StringUtil.ACTION_TYPE);
	HashMap<String, Boolean> kickPowerAttSetting = attSetting.get(StringUtil.KICK_POWER);
	HashMap<String, Boolean> kickAngleAttSetting = attSetting.get(StringUtil.KICK_ANGLE);
	HashMap<String, Boolean> dashPowerAttSetting = attSetting.get(StringUtil.DASH_POWER);
	HashMap<String, Boolean> turnAngleAttSetting = attSetting.get(StringUtil.TURN_ANGLE);
	HashMap<String, Boolean> catchAngleAttSetting = attSetting.get(StringUtil.CATCH_ANGLE);
	
	// init datasets
	DatasetBuilder dBuilder = new DatasetBuilder(m_prototype);
	Instances actionTypeDataset = null;
	Instances kickPowerDataset = null;
	Instances kickAngleDataset = null;
	Instances dashPowerDataset = null;
	Instances turnAngleDataset = null;
	Instances catchAngleDataset = null;
	
	// We need to get datasets, since instance has to be associated with a dataset for classifying.
	if (hasKick || hasDash || hasTurn || hasCatch)
		try {
			actionTypeDataset = dBuilder.getDataset(StringUtil.ACTION_TYPE);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	if (hasKick) {
		try {
			kickPowerDataset = dBuilder.getDataset(StringUtil.KICK_POWER);
			kickAngleDataset = dBuilder.getDataset(StringUtil.KICK_ANGLE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	if (hasDash)
		try {
			dashPowerDataset = dBuilder.getDataset(StringUtil.DASH_POWER);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	if (hasTurn)
		try {
			turnAngleDataset = dBuilder.getDataset(StringUtil.TURN_ANGLE);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	if (hasCatch)
		try {
			catchAngleDataset = dBuilder.getDataset(StringUtil.CATCH_ANGLE);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	
	// create a set of ArrayList for storing visual info
	ArrayList<Double> ballInfo = new ArrayList<Double>(2);
	ArrayList<Double> goalInfo = new ArrayList<Double>(3);
	ArrayList<Double> teammatesInfo = new ArrayList<Double>(8);
	ArrayList<Double> opponentsInfo = new ArrayList<Double>(10);
	ArrayList<Double> locationInfo = new ArrayList<Double>(2);
	ArrayList<Double> stateInfo = new ArrayList<Double>();
	ArrayList<Double> visualInfo = new ArrayList<Double>();
	
	// add default stateInfo. TODO: the index of lastActionType may not be always the same as ActionType.
	stateInfo.add(actionNumMap.get("dash")); stateInfo.add(100.); stateInfo.add(Double.NaN);
	double lastActionType;
	double lastActionPower;
	double lastActionAngle;
	
	// for objects
	Vector<ObjectInfo> objects = new Vector<ObjectInfo>();
	Vector<FlagInfo> flags = new Vector<FlagInfo>();
	Vector<PlayerInfo> players = new Vector<PlayerInfo>();
	Vector<PlayerInfo> teammates = new Vector<PlayerInfo>();
	Vector<PlayerInfo> opponents = new Vector<PlayerInfo>();
	
	// first put it somewhere on my side
	if(Pattern.matches("^before_kick_off.*",m_playMode))
	    m_krislet.move( -Math.random()*52.5 , 34 - Math.random()*68.0 );

	while( !m_timeOver )
	    {
		// Memory usage. TODO: clean
//		long l=Runtime.getRuntime().totalMemory(); 
//		System.out.println("total mem: " + l);
//		long f = Runtime.getRuntime().freeMemory();
//		System.out.println("free mem: " + f);
		
		// set default values to last action
		lastActionType = 0.;
		lastActionPower = 999;
		lastActionAngle = 999;
		
		
		vInfo = m_memory.getM_info();
		objects = vInfo.m_objects;
		
		// Get players list
		players.clear();
		teammates.clear();
		opponents.clear();
		flags.clear();
		for(int c = 0 ; c < objects.size() ; c ++) {
			object = (ObjectInfo)objects.elementAt(c);
			if (object.m_type.compareTo("player") == 0) {
				players.add((PlayerInfo) object);
			}else if (object.m_type.contains("flag")) {
				flags.add((FlagInfo) object);
			}				
		}
		// Get team mates & opponents lists
		for(int c = 0 ; c < players.size() ; c ++)
		{
			int i;
			if(players.elementAt(c).m_teamName.equals(m_team)){
				for(i=0; i <teammates.size(); i++){
					if(players.elementAt(c).m_distance < teammates.elementAt(i).m_distance){	
						teammates.add(i,players.elementAt(c));//insert at this position
						break;
					}
				}
				teammates.add(i,players.elementAt(c));//insert at this position
			}else{//if we cant see that the player is on this team, regard as opponents
				for(i=0; i <opponents.size(); i++){
					if(players.elementAt(c).m_distance < opponents.elementAt(i).m_distance){	
						opponents.add(i,players.elementAt(c));//insert at this position
						break;
					}
				}
				opponents.add(i,players.elementAt(c));//insert at this position
			}
		}
		
		// prepare time info
		double time = vInfo.getTime();
		
		// prepare ball info
		ObjectInfo ball = m_memory.getObject("ball");
		ballInfo.clear();
		if (ball != null) {
			// add ball distance
			ballInfo.add((double) ball.m_distance);
			// add ball direction. Make the direction a little bit fuzzy to avoid extra turning
			double ballDir = ball.m_direction;
			if (ballDir >= -5 && ballDir <= 5)
				ballDir = 0;
			ballInfo.add(ballDir);
		} else {
			ballInfo.add(999.);
			ballInfo.add(999.);
		}
		
		// prepare goal info
		ObjectInfo goal;
		goalInfo.clear();
		if( m_side == 'l' )
		    goal = m_memory.getObject("goal r");
		else
		    goal = m_memory.getObject("goal l");
		if (goal != null) {
			goalInfo.add((double) goal.m_distance);
			goalInfo.add((double) goal.m_direction);
			goalInfo.add(1.);
		} else {
			goalInfo.add(999.);
			goalInfo.add(999.);
			goalInfo.add(0.);
		}
		
		// prepare team mates infos
		int matesNum = teammates.size();
		teammatesInfo.clear();
		// We only consider 4 teammates.
		if (matesNum > 4)
			matesNum = 4;
		if (matesNum > 0) {
			for (int i = 0; i < matesNum; i++) { // fill infos in
				teammatesInfo.add((double) teammates.get(i).m_distance);
				teammatesInfo.add((double) teammates.get(i).m_direction);
			}
		}
		if (matesNum < 4) { // fill blank spaces with 999.
			for (int i = 0; i < 4 - matesNum; i++) {
				teammatesInfo.add(999.);
				teammatesInfo.add(999.);
			}
		}
		
		// prepare opponents infos
		int oppNum = opponents.size();
		opponentsInfo.clear();
		// we only consider 5 opponents.
		if (oppNum > 5)
			oppNum = 5;
		if (oppNum > 0) {
			for (int i = 0; i < oppNum; i++) { // fill infos in
				opponentsInfo.add((double) opponents.get(i).m_distance);
				opponentsInfo.add((double) opponents.get(i).m_direction);
			}
		}
		if (oppNum < 5) { // fill blank spaces with 999.
			for (int i = 0; i < 5 - oppNum; i++) {
				opponentsInfo.add(999.);
				opponentsInfo.add(999.);
			}
		}
		// visual info for debugging. TODO: clean
		//System.out.println("Ball dis: " + ballInfo.get(0) + " Ball dir: " + ballInfo.get(1));
		//System.out.println("Goal dis: " + goalInfo.get(0) + " Goal dir: " + goalInfo.get(1) + " Goal sid: " + goalInfo.get(2));
		//System.out.println("teammateOne dis: " + teammatesInfo.get(0) + " dir: " + teammatesInfo.get(1));
		
		// prepare data for action type prediction
		Instance actionTypeInstance = prepareVisualInstance(actionTypeAttSetting, stateInfo, ballInfo, goalInfo,
															teammatesInfo, opponentsInfo, visualInfo, time);
		actionTypeInstance.setDataset(actionTypeDataset);
		String actionType = null;
		try {
			actionType = action.getActionType(actionTypeInstance);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		// record action
		lastActionType = actionNumMap.get(actionType);
		//System.out.print(actionType); // TODO: clean it.
		
		// get params for predicted action, then send the action command.
		if (actionType == "kick") {
			double power = 100.;
			double angle = 0.;
			Instance kickPowerInstance = prepareVisualInstance(kickPowerAttSetting, stateInfo, ballInfo, goalInfo, 
															   teammatesInfo, opponentsInfo, visualInfo, time);
			Instance kickAngleInstance = prepareVisualInstance(kickAngleAttSetting, stateInfo, ballInfo, goalInfo, 
															   teammatesInfo, opponentsInfo, visualInfo, time);
			kickPowerInstance.setDataset(kickPowerDataset);
			kickAngleInstance.setDataset(kickAngleDataset);
			try {
				power = action.getActionPower(kickPowerInstance, StringUtil.KICK_POWER);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				angle = action.getActionAngle(kickAngleInstance, StringUtil.KICK_ANGLE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			lastActionPower = power;
			lastActionAngle = angle;
			m_krislet.kick(power, angle);
		} else if (actionType == "turn") {
			double angle = 40.;
			Instance turnAngleInstance = prepareVisualInstance(turnAngleAttSetting, stateInfo, ballInfo, goalInfo, 
															   teammatesInfo, opponentsInfo, visualInfo, time);
			turnAngleInstance.setDataset(turnAngleDataset);
			try {
				angle = action.getActionAngle(turnAngleInstance, StringUtil.TURN_ANGLE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			lastActionAngle = angle;
			m_krislet.turn(angle);
			m_memory.waitForNewInfo();
		} else if (actionType == "dash") {
			double power = 100.;
			Instance dashPowerInstance = prepareVisualInstance(dashPowerAttSetting, stateInfo, ballInfo, goalInfo, 
															   teammatesInfo, opponentsInfo, visualInfo, time);
			dashPowerInstance.setDataset(dashPowerDataset);
			//System.out.println("last action power: " + stateInfo.get(1)); //TODO: clean
			try {
				power = action.getActionPower(dashPowerInstance, StringUtil.DASH_POWER);
			} catch (Exception e) {
				e.printStackTrace();
			}
			lastActionPower = power;
			m_krislet.dash(power);
		} else if (actionType == "catch") {
			double angle = 40.;
			Instance catchAngleInstance = prepareVisualInstance(catchAngleAttSetting, stateInfo, ballInfo, goalInfo, 
															    teammatesInfo, opponentsInfo, visualInfo, time);
			catchAngleInstance.setDataset(catchAngleDataset);
			try {
				angle = action.getActionAngle(catchAngleInstance, StringUtil.CATCH_ANGLE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*lastActionPower = power;
			lastActionAngle = angle;*/
			// TODO: add catch in krislet.java.
		}
		
		// process last action info
		stateInfo.clear();
		stateInfo.add(lastActionType);
		if (lastActionPower == 999) {
			stateInfo.add(Double.NaN);
		} else {
			stateInfo.add(lastActionPower);
		}
		if (lastActionAngle == 999) {
			stateInfo.add(Double.NaN);
		} else {
			stateInfo.add(lastActionAngle);
		}
		
		// sleep one step to ensure that we will not send
		// two commands in one cycle.
		try{
		    Thread.sleep(2*SoccerParams.simulator_step);
		}catch(Exception e){}
	    }
	m_krislet.bye();
    }


	private Instance prepareVisualInstance(
			HashMap<String, Boolean> actionTypeAttSetting, ArrayList<Double> lastAction,
			ArrayList<Double> ballInfo, ArrayList<Double> goalInfo,
			ArrayList<Double> teammatesInfo, ArrayList<Double> opponentsInfo,
			ArrayList<Double> visualInfo, double time) {
		visualInfo.clear();
		if (actionTypeAttSetting.get("States"))
			visualInfo.addAll(lastAction);
		if (actionTypeAttSetting.get("Time"))
			visualInfo.add(time);
		if (actionTypeAttSetting.get("Ball"))
			visualInfo.addAll(ballInfo);
		if (actionTypeAttSetting.get("Goal"))
			visualInfo.addAll(goalInfo);
		if (actionTypeAttSetting.get("Teammates"))
			visualInfo.addAll(teammatesInfo);
		if (actionTypeAttSetting.get("Opponents"))
			visualInfo.addAll(opponentsInfo);
		/*if (actionTypeAttSetting.get("Position"))
			// TODO: add pos info
		*/	
		Object[] array = visualInfo.toArray();
		double[] instance = new double[array.length];
		for(int i=0; i<array.length; i++){
			instance[i] = (Double) array[i];
		}
		Instance visualInstance = new Instance(1, instance);
		return visualInstance;
	}


    //===========================================================================
    // Here are suporting functions for implement logic


    //===========================================================================
    // Implementation of SensorInput Interface

    //---------------------------------------------------------------------------
    // This function sends see information
    public void see(VisualInfo info)
    {
	m_memory.store(info);
    }


    //---------------------------------------------------------------------------
    // This function receives hear information from player
    public void hear(int time, int direction, String message)
    {
    }

    //---------------------------------------------------------------------------
    // This function receives hear information from referee
    public void hear(int time, String message)
    {						 
	if(message.compareTo("time_over") == 0)
	    m_timeOver = true;

    }


    //===========================================================================
    // Private members
    private SendCommand	                m_krislet;			// robot which is controled by this brain
    private Memory			m_memory;				// place where all information is stored
    private char			m_side;
    volatile private boolean		m_timeOver;
    private String                      m_playMode;
    private String m_prototype;
    private String m_team;
}
