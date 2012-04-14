package util;

import java.util.HashMap;

/**
 * handle player configuration
 * 
 * @author XYuser
 *
 */
public class PlayerConfig {
	private String name;
	private HashMap<String, String> classifiers;
	private HashMap<String, HashMap<String, Boolean>> attSetting;

	// Get settings from config.xml by name
	public PlayerConfig(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public HashMap<String, String> getClassifiers() {
		return classifiers;
	}

	public void setClassifiers(HashMap<String, String> classifiers) {
		this.classifiers = classifiers;
	}

	public HashMap<String, HashMap<String, Boolean>> getAttSetting() {
		return attSetting;
	}

	public void setAttSetting(HashMap<String, HashMap<String, Boolean>> attributes) {
		this.attSetting = attributes;
	}
}
