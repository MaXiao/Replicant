package model;

import java.io.File;
import java.util.HashMap;
import java.util.Random;

import util.PlayerConfig;
import util.StringUtil;
import util.XmlUtil;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

/**
 * This Class is used to build datasets from
 * CVS files.
 * 
 * @author Xiao Ma
 * @update March 27, 2012
 */

public class DatasetBuilder {
	private String protoName;
	
	/*
	 * Constructor. 
	 * 
	 * @param protoName : name of the prototype you want to load. Such as Krislet, A1.
	 */
	public DatasetBuilder(String protoName) {
		this.protoName = protoName;
	}
	
	/*
	 * This method will create the dataset by the name
	 * of the prototype and the type of database required,
	 * such as ActionType, KickPower, etc,.
	 * 
	 * @param predictionType : type of database
	 */
	public Instances getDataset(String predictionType) throws Exception {
		PlayerConfig pConfig = XmlUtil.getPlayerConfig(protoName);
		HashMap<String, HashMap<String, Boolean>> attSettings = pConfig.getAttSetting();
		
		File csvFile = new File("csv" + File.separator + protoName + ".csv");
		if (!csvFile.exists()) {
			throw new Exception("Error: We don't have this prototype in the gene pool!");
		}
		
		CSVLoader csvLoader = new CSVLoader();
		Instances dataset = null;
		csvLoader.setFile(csvFile);
		dataset = csvLoader.getDataSet();
		
		if (predictionType.equals(StringUtil.ACTION_TYPE)) {
			HashMap<String, Boolean> setting = attSettings.get(StringUtil.ACTION_TYPE);
			if (setting.get("States")) { // Not like other attributes, state atts needs to be added, not removed. Only last action is considered right now.
				dataset = addStateInfo(dataset);
			}
			// Remove extra attributes according to the config
			dataset = removeExtraAttributes(dataset, setting);
			dataset.deleteAttributeAt(dataset.attribute("ActionPower").index());
			dataset.deleteAttributeAt(dataset.attribute("ActionAngle").index());
		} 
		else if (predictionType.equals(StringUtil.DASH_POWER)) {
			HashMap<String, Boolean> setting = attSettings.get(StringUtil.DASH_POWER);
			if (setting.get("States")) { // Not like other attributes, state atts needs to be added, not removed. Only last action is considered right now.
				dataset = addStateInfo(dataset);
			}
			int dashIndex = dataset.attribute(StringUtil.ACTION_TYPE).indexOfValue("dash");
			// Remove extra info other than dash instances
			dataset = removeExtraInstances(dataset, dashIndex);
			// Remove extra attributes according to the config
			dataset = removeExtraAttributes(dataset, setting);
			// Remove ActionType and ActionAngle
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_TYPE).index());
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_ANGLE).index());
		}
		else if (predictionType.equals(StringUtil.TURN_ANGLE)) {
			HashMap<String, Boolean> setting = attSettings.get(StringUtil.TURN_ANGLE);
			if (setting.get("States")) { // Not like other attributes, state atts needs to be added, not removed. Only last action is considered right now.
				dataset = addStateInfo(dataset);
			}
			int turnIndex = dataset.attribute(StringUtil.ACTION_TYPE).indexOfValue("turn");
			// Remove extra info other than turn instances
			dataset = removeExtraInstances(dataset, turnIndex);
			// Remove extra attributes according to the config
			dataset = removeExtraAttributes(dataset, setting);
			// Remove ActionType and ActionAngle
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_TYPE).index());
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_POWER).index());
		}
		else if (predictionType.equals(StringUtil.KICK_POWER)) {
			HashMap<String, Boolean> setting = attSettings.get(StringUtil.KICK_POWER);
			if (setting.get("States")) { // Not like other attributes, state atts needs to be added, not removed. Only last action is considered right now.
				dataset = addStateInfo(dataset);
			}
			int kickIndex = dataset.attribute(StringUtil.ACTION_TYPE).indexOfValue("kick");
			// Remove extra info other than kick instances
			dataset = removeExtraInstances(dataset, kickIndex);
			// Remove extra attributes according to the config
			dataset = removeExtraAttributes(dataset, setting);
			// Remove ActionType and ActionAngle
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_TYPE).index());
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_ANGLE).index());
		}
		else if (predictionType.equals(StringUtil.KICK_ANGLE)) {
			HashMap<String, Boolean> setting = attSettings.get(StringUtil.KICK_ANGLE);
			if (setting.get("States")) { // Not like other attributes, state atts needs to be added, not removed. Only last action is considered right now.
				dataset = addStateInfo(dataset);
			}
			int kickIndex = dataset.attribute(StringUtil.ACTION_TYPE).indexOfValue("kick");
			// Remove extra info other than kick instances
			dataset = removeExtraInstances(dataset, kickIndex);
			// Remove extra attributes according to the config
			dataset = removeExtraAttributes(dataset, setting);
			// Remove ActionType and ActionAngle
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_TYPE).index());
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_POWER).index());
		} 
		else if (predictionType.equals(StringUtil.CATCH_ANGLE)) {
			HashMap<String, Boolean> setting = attSettings.get(StringUtil.CATCH_ANGLE);
			if (setting.get("States")) { // Not like other attributes, state atts needs to be added, not removed. Only last action is considered right now.
				dataset = addStateInfo(dataset);
			}
			int catchIndex = dataset.attribute(StringUtil.ACTION_TYPE).indexOfValue("catch");
			// Remove extra info other than catch instances
			dataset = removeExtraInstances(dataset, catchIndex);
			// Remove extra attributes according to the config
			dataset = removeExtraAttributes(dataset, setting);
			// Remove ActionType and ActionAngle
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_TYPE).index());
			dataset.deleteAttributeAt(dataset.attribute(StringUtil.ACTION_POWER).index());
		}
		// Set attribute to be classed.
		dataset.setClassIndex(dataset.numAttributes() - 1);

		return dataset;
	}

	/*
	 * This method will add state infos in the table on runtime.
	 * Right now, they are just immediate previous action type, 
	 * power and angle. 
	 * 
	 * TODO: needs to be more configurable, like number
	 * 		 of previous actions should be added.
	 * 
	 * @param dataset : the dataset needs to be handled.
	 */
	private Instances addStateInfo(Instances dataset) {
		Instances temp = new Instances(dataset);
		temp.delete(temp.numInstances() - 1);
		Attribute lastActionType = temp.attribute(StringUtil.ACTION_TYPE);
		Attribute lastActionPower = temp.attribute(StringUtil.ACTION_POWER);
		Attribute lastActionAngle  = temp.attribute(StringUtil.ACTION_ANGLE);
		// rename these attributes.
		temp.renameAttribute(lastActionType, "LastActionType");
		temp.renameAttribute(lastActionPower, "LastActionPower");
		temp.renameAttribute(lastActionAngle, "LastActionAngle");
		// delete all instance except last action infos
		int attNum = temp.numAttributes();
		for (int i = 0; i < attNum - 3; i++) {
			temp.deleteAttributeAt(0);
		}
		// delete the last instance in original dataset
		dataset.delete(0);
		// merge two datasets
		dataset = Instances.mergeInstances(temp, dataset);
		return dataset;
	}
	
	/*
	 * Remove extra attributes according to the config settings
	 * Setting can be changed in config.xml under src folder
	 * 
	 * @param dataset : dataset to be handled
	 * @param setting : config setting 
	 */
	private Instances removeExtraAttributes(Instances dataset, HashMap<String, Boolean> setting) {
		if (!setting.get("Time")) {
			dataset.deleteAttributeAt(dataset.attribute("time").index());
		}
		if (!setting.get("Ball")) {
			dataset.deleteAttributeAt(dataset.attribute("ballDis").index());
			dataset.deleteAttributeAt(dataset.attribute("ballDir").index());
		}
		if (!setting.get("Goal")) {
			dataset.deleteAttributeAt(dataset.attribute("goalDis").index());
			dataset.deleteAttributeAt(dataset.attribute("goalDir").index());
			dataset.deleteAttributeAt(dataset.attribute("goalSide").index());
		}
		if (!setting.get("Teammates")) {
			dataset.deleteAttributeAt(dataset.attribute("teammateOneDis").index());
			dataset.deleteAttributeAt(dataset.attribute("teammateOneDir").index());
			dataset.deleteAttributeAt(dataset.attribute("teammateTwoDis").index());
			dataset.deleteAttributeAt(dataset.attribute("teammateTwoDir").index());
			dataset.deleteAttributeAt(dataset.attribute("teammateThreeDis").index());
			dataset.deleteAttributeAt(dataset.attribute("teammateThreeDir").index());
			dataset.deleteAttributeAt(dataset.attribute("teammateFourDis").index());
			dataset.deleteAttributeAt(dataset.attribute("teammateFourDir").index());
		}
		if (!setting.get("Opponents")) {
			dataset.deleteAttributeAt(dataset.attribute("opponentOneDis").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentOneDir").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentTwoDis").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentTwoDir").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentThreeDis").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentThreeDir").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentFourDis").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentFourDir").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentFiveDis").index());
			dataset.deleteAttributeAt(dataset.attribute("opponentFiveDir").index());
		}
		if (!setting.get("Position")) {
			//TODO: add position part
		}
		
		return dataset;
	}

	/*
	 * Used for action power and angle tables. All instances other than the
	 * predicted action will be removed. For example, for predicting dash 
	 * power, instances with kick and turn will be removed.
	 * 
	 * @param dataset : dataset to be handled
	 * @param index	  : index number of the predicted action in the column
	 */
	private Instances removeExtraInstances(Instances dataset, int index) throws Exception {
		// Check if this action appears in dataset. Mainly for catch.
		if (index == -1) {
			throw new Exception("Error: This prototype haven't tried this action!");
		}
		
		// Get stat of ActionType attribute.
		AttributeStats stat = dataset.attributeStats(dataset.numAttributes() - 3);
		// Get nominal counts for each action
		int[] counts = stat.nominalCounts;
		// Sort the dataset by ActionType
		dataset.sort(dataset.attribute(StringUtil.ACTION_TYPE));
		// Find the index of the first instance of given action
		int beginIndex= 0;
		for (int i = 0; i < index; i++) {
			beginIndex += counts[i];
		}
		dataset = new Instances(dataset, beginIndex, counts[index]);
		return dataset;
	}
	
	/*
	 * Feature selection algorithm. No use for now. It turns out not very good.
	 */
    void selectAttUseFilter(Instances dataset) throws Exception
    {
    	// package weka.filters.supervised.attribute!
        AttributeSelection filter = new AttributeSelection();  
        CfsSubsetEval eval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        filter.setEvaluator(eval);
        filter.setSearch(search);
        filter.setInputFormat(dataset);
       
        System.out.println( "number of instance attribute = " + dataset.numAttributes() );
        Instances selectedIns = Filter.useFilter(dataset, filter);
        System.out.println( "number of selected instance attribute = " + selectedIns.numAttributes() );
    }
    
    /*
     * Dynamic feature selection. No use for now. It turns out not very good.
     */
    void selectAttUseMC(Instances dataset) throws Exception
    {  
         AttributeSelectedClassifier classifier = new AttributeSelectedClassifier();
         CfsSubsetEval eval = new CfsSubsetEval();
         GreedyStepwise search = new GreedyStepwise();
         J48 base = new J48();
         classifier.setClassifier( base );
         classifier.setEvaluator( eval );
         classifier.setSearch( search );
         // 10-fold cross-validation
         Evaluation evaluation = new Evaluation(dataset);
         evaluation.crossValidateModel(classifier, dataset, 10, new Random(1));
         System.out.println( evaluation.toSummaryString() );
    }
}
