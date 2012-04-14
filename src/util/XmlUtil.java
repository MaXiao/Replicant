package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * handle config.xml
 * 
 * @author Xiao Ma
 * @update March 26, 2012
 */
public class XmlUtil
{
	private static final String FILE_NAME = "config.xml";
	private static final String FILE_PATH = "src";

	/*
	 *  Create a set of config for the new agent.
	 *  
	 *  @param name : name of the prototype
	 */
	public void createConfig(String name) throws Exception
	{
		SAXBuilder builder = new SAXBuilder();
		File file = new File(FILE_PATH, FILE_NAME);
		Document doc = null;
		// Check if config.xml exist. Create it if not.
		if (!file.exists())
		{
			doc = new Document();
			Element root = new Element("AgentConfigs");
			doc.setRootElement(root);

			String xmlFileData = new XMLOutputter().outputString(doc);
			write2File(xmlFileData);
		}
		
		doc = builder.build(file);

		Element root = doc.getRootElement();
		@SuppressWarnings("unchecked")
		List<Element> eleList = root.getChildren("Config");

		for (Iterator<Element> iter = eleList.iterator(); iter.hasNext();) {
			Element configEle = iter.next();
			String configName = configEle.getChildText("Name");
			if (configName.equals(name)) { 
				throw new Exception("Error: Prototype " + name + " already cloned. Give us something new!");
			}
		}

		Element configEle = new Element("Config");
		// Create first level children nodes
		Element nameEle = new Element("Name");
		nameEle.setText(name);
		Element clsEle = new Element("Classifiers");
		Element attEle = new Element("DatasetAttributes");
		
		// Create children nodes for Classifiers element, then add to parent node
		Element actionTypeCls = new Element("ActionTypeClassifier");
		actionTypeCls.setText("weka.classifiers.rules.JRip");
		Element actionAngleCls = new Element("ActionAngleClassifier");
		actionAngleCls.setText("weka.classifiers.rules.M5Rules");
		Element actionPowerCls = new Element("ActionPowerClassifier");
		actionPowerCls.setText("weka.classifiers.rules.M5Rules");
		clsEle.addContent(actionTypeCls); clsEle.addContent(actionAngleCls); clsEle.addContent(actionPowerCls); 
		
		// Create children nodes for DatasetAttributes
		Element actionTypeSet = new Element("ActionType");
		Element KickPowerSet = new Element("KickPower");
		Element KickAngleSet = new Element("KickAngle");
		Element DashPowerSet = new Element("DashPower");
		Element TurnAngleSet = new Element("TurnAngle");
		Element CatchAngleSet = new Element("CatchAngle");
		
		// Add content to dataset nodes
		actionTypeSet.addContent(getAttSettingNodes()); KickPowerSet.addContent(getAttSettingNodes()); KickAngleSet.addContent(getAttSettingNodes());
		DashPowerSet.addContent(getAttSettingNodes()); TurnAngleSet.addContent(getAttSettingNodes()); CatchAngleSet.addContent(getAttSettingNodes());
		
		// Add nodes to DatasetAttributes
		attEle.addContent(actionTypeSet); attEle.addContent(KickPowerSet); attEle.addContent(KickAngleSet); 
		attEle.addContent(DashPowerSet); attEle.addContent(TurnAngleSet); attEle.addContent(CatchAngleSet); 

		configEle.addContent(nameEle); configEle.addContent(clsEle); configEle.addContent(attEle); 
		root.addContent(configEle);
		XMLOutputter outPutter = new XMLOutputter();
		outPutter.setFormat(Format.getPrettyFormat());
		String xmlFileData = outPutter.outputString(doc);
		
		write2File(xmlFileData);		
	}

	/*
	 * create a default setting for each dataset
	 */
	private List<Element> getAttSettingNodes() {
		List<Element> datasetAtt = new ArrayList<Element>();
		
		Element time = new Element("Time"); time.setText("false");
		Element ball = new Element("Ball"); ball.setText("true");
		Element goal = new Element("Goal"); goal.setText("true");
		Element team = new Element("Teammates"); team.setText("false");
		Element opp = new Element("Opponents"); opp.setText("false");
		Element pos = new Element("Position"); pos.setText("false");
		Element state = new Element("States"); state.setText("false");
		
		datasetAtt.add(time); datasetAtt.add(ball); datasetAtt.add(goal); datasetAtt.add(team);
		datasetAtt.add(opp); datasetAtt.add(pos); datasetAtt.add(state);
		return datasetAtt;
	}

	/* 
	 * get player config of certain prototype
	 * 
	 * @param name : name of the prototype
	 * 
	 */
	public static PlayerConfig getPlayerConfig(String name) throws Exception
	{
		PlayerConfig config = new PlayerConfig(name);
		HashMap<String, String> classifiers = new HashMap<String, String>();
		HashMap<String, HashMap<String, Boolean>> attributes = new HashMap<String, HashMap<String, Boolean>>();
		
		SAXBuilder builder = new SAXBuilder();
		File file = new File(FILE_PATH, FILE_NAME);
		Document doc = null;
		
		if (!file.exists())
		{
			doc = new Document();
			Element root = new Element("AgentConfigs");
			doc.setRootElement(root);

			String xmlFileData = new XMLOutputter().outputString(doc);
			write2File(xmlFileData);
		}
		
		try
		{
			doc = builder.build(file);
		} catch (JDOMException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		Element root = doc.getRootElement();
		@SuppressWarnings("unchecked")
		List<Element> eleList = root.getChildren("Config");

		for (Iterator<Element> iter = eleList.iterator(); iter.hasNext();)
		{
			Element configEle = iter.next();
			String configName = configEle.getChildText("Name");
			if (configName.equals(name)) {
				// Set classifiers 
				Element clsEle = configEle.getChild("Classifiers");
				classifiers.put("ActionTypeClassifier", clsEle.getChildText("ActionTypeClassifier"));
				classifiers.put("ActionPowerClassifier", clsEle.getChildText("ActionPowerClassifier"));
				classifiers.put("ActionAngleClassifier", clsEle.getChildText("ActionAngleClassifier"));
				
				// Set dataset attributes
				Element attEle = configEle.getChild("DatasetAttributes");
				attributes.put("ActionType", getAttributeSetting(attEle, "ActionType"));
				attributes.put("KickPower", getAttributeSetting(attEle, "KickPower"));
				attributes.put("KickAngle", getAttributeSetting(attEle, "KickAngle"));
				attributes.put("DashPower", getAttributeSetting(attEle, "DashPower"));
				attributes.put("TurnAngle", getAttributeSetting(attEle, "TurnAngle"));
				attributes.put("CatchAngle", getAttributeSetting(attEle, "CatchAngle"));
			}
		}
		
		if (classifiers.isEmpty() || attributes.isEmpty()) {
			return null;
		}
		
		config.setClassifiers(classifiers);
		config.setAttSetting(attributes);

		return config;
	}
	
	/*
	 * get attribute setting info
	 */
	private static HashMap<String, Boolean> getAttributeSetting (Element ele, String name) {
		HashMap<String, Boolean> map = new HashMap<String, Boolean>();
		Element datasetEle = ele.getChild(name);
		
		map.put("Time", Boolean.parseBoolean(datasetEle.getChildText("Time")));
		map.put("Ball", Boolean.parseBoolean(datasetEle.getChildText("Ball")));
		map.put("Goal", Boolean.parseBoolean(datasetEle.getChildText("Goal")));
		map.put("Teammates", Boolean.parseBoolean(datasetEle.getChildText("Teammates")));
		map.put("Opponents", Boolean.parseBoolean(datasetEle.getChildText("Opponents")));
		map.put("Position", Boolean.parseBoolean(datasetEle.getChildText("Position")));
		map.put("States", Boolean.parseBoolean(datasetEle.getChildText("States")));
		
		return map;
	}

	private static boolean write2File(String str)
	{

		File file = new File(FILE_PATH, FILE_NAME);
		if (!file.exists())
		{
			try
			{
				file.createNewFile();
			} catch (IOException e)
			{
				return false;
			}
		}

		try
		{
			FileWriter writer = new FileWriter(file);
			writer.write(str);
			writer.close();
		} catch (IOException e)
		{
			return false;
		}

		return true;
	}
}
