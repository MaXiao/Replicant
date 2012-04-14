package agent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import model.DatasetBuilder;
import model.ModelBuilder;
import util.StringUtil;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Get action decisions, may including type, power and angle 
 * from visual info and corresponding model.
 * 
 * @author: Xiao Ma
 * @update: March 27, 2012
 */
public class Action {
	private ModelBuilder modelBuilder;
	private String prototype;
	private HashMap<String, Double> actionNumMap;
	private Classifier actionTypeCls;
	private Classifier dashPowerCls;
	private Classifier kickPowerCls;
	private Classifier kickAngleCls;
	private Classifier turnAngleCls;
	private Classifier catchAngleCls;
	
	// Many procedures are included in constructor to avoid start-up cost.
	public Action(String proto) throws Exception {
		this.prototype = proto;
		this.modelBuilder = new ModelBuilder(proto);
		this.actionNumMap = new HashMap<String, Double>();
		
		// We don't really need the dataset for prediction,
		// just use it to get the connection between 
		// action and their values.
		DatasetBuilder builder = new DatasetBuilder(this.prototype);
		Instances dataset = builder.getDataset(StringUtil.ACTION_TYPE);
        Attribute actionType = dataset.attribute(StringUtil.ACTION_TYPE);
        
        actionNumMap.put("dash", (double) actionType.indexOfValue("dash"));
        actionNumMap.put("kick", (double) actionType.indexOfValue("kick"));
        actionNumMap.put("turn", (double) actionType.indexOfValue("turn"));
        actionNumMap.put("catch", (double) actionType.indexOfValue("catch"));
        // Won't consider turn-neck for now
        //map.put("turn-neck", (double) actionType.indexOfValue("turn-neck"));
		
		// Get actionType model.
		actionTypeCls = modelBuilder.getModel(StringUtil.ACTION_TYPE);
		if (actionNumMap.get("dash") != -1)
			dashPowerCls =  modelBuilder.getModel(StringUtil.DASH_POWER);
		if (actionNumMap.get("kick") != -1) {
			kickPowerCls =  modelBuilder.getModel(StringUtil.KICK_POWER);
			kickAngleCls =  modelBuilder.getModel(StringUtil.KICK_ANGLE);
		}
		if (actionNumMap.get("turn") != -1)
			turnAngleCls =  modelBuilder.getModel(StringUtil.TURN_ANGLE);
		if (actionNumMap.get("catch") != -1)
			catchAngleCls =  modelBuilder.getModel(StringUtil.CATCH_ANGLE);
	}

	public HashMap<String, Double> getActionNumMap() {
		return actionNumMap;
	}

	/*
	 * Make action type decision. It will return the action name
	 * as dash, kick, turn, turn-neck, catch. Say and change-view
	 * are ignored by now.
	 * 
	 * @param vision : visual info, what the agent see in this cycle. 
	 */
	public String getActionType(Instance vision) throws Exception {
		
        // Set default action type to Turn.
		double action = actionNumMap.get("turn"); 
		// Make the decision on action type by model.
		try {
			action = actionTypeCls.classifyInstance(vision);
		} catch (Exception e) {
			System.err.println("Cannot make the decision on action type.");
			e.printStackTrace();
		}
		
		String result = null;
		for (Iterator<?> iter = actionNumMap.entrySet().iterator(); iter.hasNext();) {
			@SuppressWarnings("rawtypes")
			Map.Entry entry = (Entry) iter.next();
			if ((Double)entry.getValue() == action)
				result = (String) entry.getKey();
		}
		
		// return action name
        return result; 
	}
	
	/*
	 * Make decision on power parameters.
	 * 
	 * @param vision : visual info
	 * @param powerType : which type of action we want to predict.
	 */
	public double getActionPower(Instance vision, String powerType) throws Exception {
		// Make the decision by model
		if (powerType == StringUtil.DASH_POWER && dashPowerCls != null) {
			return dashPowerCls.classifyInstance(vision);
		} else if (powerType == StringUtil.KICK_POWER && kickPowerCls != null) {
			return kickPowerCls.classifyInstance(vision);
		} else {
			System.err.println("There is no power parameter for this action. Use default value 0.");
			return 0;
		}
	}
	
	/*
	 * Make decision on angle parameters.
	 * 
	 * @param vision : visual info
	 * @param angleType : which type of action we want to predict.
	 */
	public double getActionAngle(Instance vision, String angleType) throws Exception {
		if (angleType == StringUtil.KICK_ANGLE && kickAngleCls != null) {
			return kickAngleCls.classifyInstance(vision);
		} else if (angleType == StringUtil.TURN_ANGLE && turnAngleCls != null) {
			return turnAngleCls.classifyInstance(vision);
		} else if (angleType == StringUtil.CATCH_ANGLE && catchAngleCls != null) {
			return catchAngleCls.classifyInstance(vision);
		}else {
			System.err.println("There is no angle parameter for this action. Use default value 0");
			return 0;
		}
	}
}
