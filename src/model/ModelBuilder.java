package model;

import java.io.File;
import java.util.HashMap;

import util.PlayerConfig;
import util.StringUtil;
import util.XmlUtil;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Train action models by different classification
 * algorithms. There are three type of models:
 * 
 * 		  Model  			 Default Algorithm
 * 		ActionType			       JRip
 * 		  Power					  M5Rules
 * 		  Angle					  M5Rules
 * 
 * Other algorithms can be assigned through congif.xml 
 * Models are stored in models/<PrototypeName>/.
 * 
 * @author: Xiao Ma
 * @update: March 30, 2012
 */
public class ModelBuilder {	
	private String actionTypeClassifier;
	private String powerClassifier;
	private String angleClassifier;
	
	private String prototypeName;

	/*
	 * constructor
	 * Instantiate model builder and assign default classifiers.
	 * 
	 * @param prototypeName : name of the prototype
	 */
	public ModelBuilder(String prototypeName) throws Exception {
		this.prototypeName = prototypeName;
		
		PlayerConfig config = XmlUtil.getPlayerConfig(prototypeName);
		HashMap<String, String> cls = config.getClassifiers();
		
		this.actionTypeClassifier = cls.get("ActionTypeClassifier");
		this.powerClassifier = cls.get("ActionPowerClassifier");
		this.angleClassifier = cls.get("ActionAngleClassifier");
	}
	
	/*
	 * rebuild all models from csv file,
	 * based on attribute setting in config.xml
	 */
	public void buildAllModels() throws Exception {
		// get training dataset.
		DatasetBuilder builder = new DatasetBuilder(prototypeName);
		// get ActionType attribute from ActionType dataset.
		// Use it to get index number of different action
		Instances dataset = builder.getDataset(StringUtil.ACTION_TYPE);
		Attribute action = dataset.attribute(dataset.numAttributes() - 1);
		// build ActionType model
		buildModel(StringUtil.ACTION_TYPE);
		// build power and/or angle model(s) for each action
		// if action.indexOfValue("action") == -1, means this
		// action doesn't appear in log file.
		if (action.indexOfValue("kick") != -1) {
			buildModel(StringUtil.KICK_ANGLE);
			buildModel(StringUtil.KICK_POWER);
		}
		if (action.indexOfValue("dash") != -1) {
			buildModel(StringUtil.DASH_POWER);
		}
		if (action.indexOfValue("turn") != -1) {
			buildModel(StringUtil.TURN_ANGLE);
		}
		if (action.indexOfValue("catch") != -1) {
			buildModel(StringUtil.CATCH_ANGLE);
		}
	}
	
	/*
	 * Build certain model from corresponding dataset.
	 * 
	 * @param modelName : Name of the model. It's also the 
	 *        			  name of the dataset to be trained.
	 */
	public void buildModel(String modelName) {
		Class<?> classifierClass = null;
		Classifier model = null;
		
		// Set three classifiers by class name dynamically
		try {
			if (modelName == StringUtil.ACTION_TYPE) {
				classifierClass = Class.forName(actionTypeClassifier);
			} else if (modelName == StringUtil.DASH_POWER || modelName == StringUtil.KICK_POWER) {
				classifierClass = Class.forName(powerClassifier);
			} else if (modelName == StringUtil.KICK_ANGLE || modelName == StringUtil.TURN_ANGLE || 
					   modelName == StringUtil.CATCH_ANGLE) {
				classifierClass = Class.forName(angleClassifier);
			} else {
				System.out.println("Unknow Action");
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Classifier class for " + modelName + " cannot be found.");
			e.printStackTrace();
		}
		
		// Instantiate model from classifier
		try {
			model = (Classifier) classifierClass.newInstance();
		} catch (Exception e) {
			System.err.println("Classifier for " + modelName + " cannot be instantiated");
			e.printStackTrace();
		} 
		
		// Get training dataset.
		DatasetBuilder builder = new DatasetBuilder(prototypeName);
		Instances dataset = null;
		try {
			dataset = builder.getDataset(modelName);
		} catch (Exception e1) {
			System.err.println("Cannot get dataset " + modelName + " for prototype " + prototypeName);
			e1.printStackTrace();
		}
		
		// Set attribute needs to be classified.
        dataset.setClassIndex(dataset.numAttributes() - 1); 		
		
        // Build model
        try {
			model.buildClassifier(dataset);
		} catch (Exception e) {
			System.err.println(modelName + " model-building fail.");
			e.printStackTrace();
		}          
        
        //serialize model to a file
        try {
        	File path = new File("models" + File.separator + prototypeName + File.separator);
        	if (!path.exists())
        		path.mkdir();
        	
			SerializationHelper.write("models" + File.separator + prototypeName + File.separator + modelName + ".model", model);
		} catch (Exception e) {
			System.err.println(modelName + " model serialization fail.");
			e.printStackTrace();
		}
	}
	
	/*
	 * Get model by name.
	 * 
	 * @param name : name of the model.
	 */
	public Classifier getModel(String name) {
		Classifier model = null;
		// Set model file name
		File modelFile = new File("models" + File.separator + prototypeName + File.separator + name + ".model");

		// If model doesn't exist, build it.
		if (!modelFile.exists()) {
			buildModel(name);
		}
		
		// Deserialize model from model file
		try {
			model = (Classifier) weka.core.SerializationHelper.read("models" + File.separator + prototypeName + File.separator + name + ".model");
		} catch (Exception e) {
			System.err.println("Model " + name + " cannot be deserialized.");
			e.printStackTrace();
		}
		
		return model;
	}
	
	/*
	 * Get a re-trained model built from csv data 
	 * 
	 * @param name : name of the model.
	 */
	public Classifier getRetrainedModel(String name) {
		buildModel(name);
		return getModel(name);
	}
	
	/*
	 * Set action type classifier algorithm
	 * 
	 * @param actionTypeClassifier : name of weka algorithm class we want to use 
	 */
	public void setActionTypeClassifier(String actionTypeClassifier) {
		this.actionTypeClassifier = actionTypeClassifier;
	}
	
	/*
	 * Set power classifier algorithm
	 * 
	 * @param powerClassifier : name of weka algorithm class we want to use 
	 */
	public void setPowerClassifier(String powerClassifier) {
		this.powerClassifier = powerClassifier;
	}
	
	/*
	 * Set angle classifier algorithm
	 * 
	 * @param angleClassifier : name of weka algorithm class we want to use 
	 */
	public void setAngleClassifier(String angleClassifier) {
		this.angleClassifier = angleClassifier;
	}
	
	// For testing
	public static void main(String[] args) throws Exception {
		String proto;
		ModelBuilder builder;
		try
	    {
			// First look for parameters
			if (args.length == 0) {
				builder = new ModelBuilder("Krislet");
				builder.buildAllModels();
			}
			else {
				for (int c = 0 ; c < args.length ; c += 2) {
					if (args[c].equals("-proto")) 
						{
						proto = args[c+1];
						builder = new ModelBuilder(proto);
						builder.buildAllModels();
						System.out.println("test");
						}
					else
					    {
						throw new Exception();
					    }
				    }
			}
	    }
	catch(Exception e)
	    {
		System.err.println("");
		System.err.println("USAGE: ModelBuilder [-parameter value]");
		System.err.println("");
		System.err.println("    Parameters  value        default");
		System.err.println("   ------------------------------------");
		System.err.println("    proto     prototype      Krislet");
		System.err.println("");
		System.err.println("    Example:");
		System.err.println("      ModelBuilder -proto Garfield");
		System.err.println("    or");
		System.err.println("      ModelBuilder");
		return;
	    }
	}
}
