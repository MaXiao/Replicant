package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import agent.GoalInfo;
import agent.ObjectInfo;
import agent.PlayerInfo;
import agent.VisualInfo;

/**
 * convert robocup lsf file to csv file
 * 
 * @author Xiao Ma
 * @update March 29, 2012
 *
 */

public class LogParser {    
	private static ArrayList<String> relAttrs = new ArrayList<String>(); // List of attribute titles for relative table.
	
	public LogParser() { // infos of last actions are not recorded in csv. They will be added in dataset at runtime.
		relAttrs.add("time");
		// Infos of ball and goals. Belief: Cannot see two goals at the same time.
		relAttrs.add("ballDis"); relAttrs.add("ballDir"); relAttrs.add("goalDis"); relAttrs.add("goalDir"); relAttrs.add("goalSide");
		// Infos of teammates. They are not recorded as uniform number, but the distance. One is nearest, Four is farthest.
		relAttrs.add("teammateOneDis"); relAttrs.add("teammateOneDir"); relAttrs.add("teammateTwoDis"); relAttrs.add("teammateTwoDir"); relAttrs.add("teammateThreeDis"); relAttrs.add("teammateThreeDir"); relAttrs.add("teammateFourDis"); relAttrs.add("teammateFourDir");
		// Infos of opponents. They are not recorded as uniform number, but the distance. One is nearest, Four is farthest.
		relAttrs.add("opponentOneDis"); relAttrs.add("opponentOneDir"); relAttrs.add("opponentTwoDis"); relAttrs.add("opponentTwoDir"); relAttrs.add("opponentThreeDis"); relAttrs.add("opponentThreeDir"); relAttrs.add("opponentFourDis"); relAttrs.add("opponentFourDir"); relAttrs.add("opponentFiveDis"); relAttrs.add("opponentFiveDir");
		// TODO: Infos of player absolute position. 
		// relAttrs.add("playerPositionX"); relAttrs.add("playerPositionY");
		// Infos of actions
		relAttrs.add("ActionType"); relAttrs.add("ActionPower"); relAttrs.add("ActionAngle");
	}
	
	/*
	 * build new csv from log file and add
	 * prototype in config.xml, then fill 
	 * in default setting. 
	 * 
	 * @param sourceFile : name and path of source log file
	 * @param protoName  : name of the prototype
	 */
	private void buildNewLog(String sourceFile, String protoName) throws Exception {
		buildNewLog(sourceFile, protoName, false);
	}
	
	/*
	 * build new csv from log file and add
	 * prototype in config.xml, then fill 
	 * in default setting. 
	 * 
	 * @param sourceFile : name and path of source log file
	 * @param protoName  : name of the prototype
	 * @param reTrain    : indicate if we want to re-build the log with given name.
	 */
	 
	private void buildNewLog(String sourceFile, String protoName, boolean reBuild) throws Exception {
		// Create source file by path and name
		File logFile = new File(sourceFile);
		// Create dest file by prototype name, store in "csv/". 
		File csvFile = new File("csv" + File.separator + protoName + ".csv");
		
		if (reBuild) {
			if (!csvFile.exists())
				csvFile.createNewFile();
		} else {
			// Check if there already has an prototype with the given name
			if (!csvFile.exists())
				csvFile.createNewFile();
			else
				throw new Exception("Error: Prototype " + protoName + " already cloned. Give something new for the gene pool!");
		}
		
		// add prototype in config.xml.
		if (XmlUtil.getPlayerConfig(protoName) == null) {
			XmlUtil xml = new XmlUtil();
			xml.createConfig(protoName);
		}
			
		BufferedReader br = new BufferedReader(new FileReader(logFile));
		BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile));
		
		// Write title line to csv
		StringBuffer titleRow = new StringBuffer();
		for (Iterator<String> iter = relAttrs.iterator(); iter.hasNext();) {
			String attr = (String) iter.next();
			titleRow.append(attr + ",");
		}
		bw.write(titleRow.toString());
		bw.newLine();
		// parse log and write to csv
		parseLog(br, bw);
		
		br.close();
		bw.close();
	}
	
	/*
	 * parse log file, append content to existing csv
	 * 
	 * @param sourceFile : name and path of source log file
	 * @param protoName  : name of the prototype
	 */
	private void appendToExistingLog(String sourceFile, String protoName) throws Exception {
		// Create source file by path and name
		File logFile = new File(sourceFile);
		// Create dest file by prototype name, store in "csv/". 
		File csvFile = new File("csv" + File.separator + protoName + ".csv");
		// Check if there already has an prototype with the given name
		if (!csvFile.exists())
			throw new Exception("We don't have this prototype in gene pool yet.");
		
		BufferedReader br = new BufferedReader(new FileReader(logFile));
		BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile, true));
		// parse log and write to csv
		parseLog(br, bw);
		
		br.close();
		bw.close();
	}

	/*
	 * parse log file to csv file
	 */
	private void parseLog(BufferedReader br, BufferedWriter bw)	throws IOException {
		// match action row in log file
		Pattern actionPattern = Pattern.compile("\\((\\w+)\\s(-?\\d+\\.?\\d*)\\s?(-?\\d+\\.?\\d*)?\\)");
		Matcher matcher;
		// create a map to store attribute-value pair.
		HashMap<String, Comparable<?>> attributeMap = new HashMap<String, Comparable<?>>();
		// get team name
		String teamName = null;
		String firstRow = br.readLine();
		if (firstRow.substring(0, 5).equals("(init")) {
			Pattern p = Pattern.compile("\\(init\\s(\\w[^\\(\\)\\s]+)");
			Matcher m = p.matcher(firstRow);
			if (m.find()) {
				teamName = m.group(1);
			}
		}
		
		String playerSide = "l";
		// prepare a StringBuffer to store string for current row 
		StringBuffer rowStr = new StringBuffer();
		
		// process log file
		String str = null;
		while((str=br.readLine())!=null) {
			
			String actionType = "none";
			String actionPower = "";
			String actionAngle = "";
			
			matcher = actionPattern.matcher(str);
			// indicates if there are two consecutive see lines without a action line inbetween.
			boolean skipActionFlag = false;
			// get player side
			if (str.substring(0, 7).equals("(init l") || str.substring(0, 7).equals("(init r")) {
				playerSide = str.substring(6,7);
				attributeMap.put("playerSide", playerSide);
			} else if (str.substring(0, 4).equals("(see")) { //match see line
				// check if there is no action between this see and last see
				if (attributeMap.get("ActionType") == null)
					skipActionFlag = true;
				// refill attribute map
				for (Iterator<String> iter = relAttrs.iterator(); iter.hasNext();) {
					// Fill an impossible value as default value, mainly for distance and direction params.
					attributeMap.put(iter.next(), "999");
				}
				
				// Set 0 as default for goalSide. If cannot see clearly, treat as its own goal.
				attributeMap.put("goalSide", "0");
				// set action info as null
				attributeMap.put("ActionType", null);
				attributeMap.put("ActionPower", null);
				attributeMap.put("ActionAngle", null);
				
				// write last row to csv if no action skipping, then clear the row.
				if (rowStr.length() > 0) {
					if (skipActionFlag) {
						rowStr.delete(0, rowStr.length());
					} else {
					bw.write(rowStr.toString());
					bw.newLine();
					rowStr.delete(0, rowStr.length());
					}
				}
				
				// get next visual info from log
				VisualInfo info = new VisualInfo(str);
				info.parse();
				// get object list
				Vector<ObjectInfo> objectList = info.m_objects;
				
				// set time info
				int time = info.getTime();
				attributeMap.put("time", time+"");
				
				// two treesets for teammates and opponents, sort by player distance
				TreeSet<PlayerInfo> teammates = new TreeSet<PlayerInfo>(new PlayerComparator());
				TreeSet<PlayerInfo> opponents = new TreeSet<PlayerInfo>(new PlayerComparator());
				
				for (Iterator<ObjectInfo> iterator = objectList.iterator(); iterator.hasNext();) {
					ObjectInfo object = (ObjectInfo) iterator.next();
					String objectType = object.getType();
					// get ball info, add to map
					if (objectType.equals("b") || objectType.equals("ball")) {
						attributeMap.put("ballDis", object.m_distance+"");
						attributeMap.put("ballDir", object.m_direction+"");
					} 
					// get goal info, add to map
					else if (objectType.startsWith("g") || objectType.startsWith("goal")) {
						attributeMap.put("goalDis", object.m_distance+"");
						attributeMap.put("goalDir", object.m_direction+"");
						// See if it is the right goal. 
						// 0 : player's goal
						// 1 : opponent's goal
						String goalSide =  ((GoalInfo)object).m_side+"";
						if (playerSide.equals(goalSide))
							attributeMap.put("goalSide", 0+"");
						else
							attributeMap.put("goalSide", 1+"");
					} // extract teammates and opponents, store in two treesets
					else if (objectType.startsWith("p") || objectType.startsWith("player")) {
						PlayerInfo pInfo = (PlayerInfo)object;
						if (pInfo.m_teamName.equals(teamName)) {
							teammates.add(pInfo);
						} else { // If cannot see clearly, treat as opponents
							opponents.add(pInfo);
						}
					}
				}
				
				// add teammates info to map
				if (teammates.size() > 0) {
					Object[] pArray = teammates.toArray();
					
					int len = pArray.length;
					if (len > 4)
						len = 4;
					switch(len) { // Only consider 4 teammates at most.
					case 4:
						attributeMap.put("teammateFourDis", ((PlayerInfo)pArray[3]).m_distance+"");
						attributeMap.put("teammateFourDir", ((PlayerInfo)pArray[3]).m_direction+"");
					case 3:
						attributeMap.put("teammateThreeDis", ((PlayerInfo)pArray[2]).m_distance+"");
						attributeMap.put("teammateThreeDir", ((PlayerInfo)pArray[2]).m_direction+"");
					case 2:
						attributeMap.put("teammateTwoDis", ((PlayerInfo)pArray[1]).m_distance+"");
						attributeMap.put("teammateTwoDir", ((PlayerInfo)pArray[1]).m_direction+"");
					case 1:
						attributeMap.put("teammateOneDis", ((PlayerInfo)pArray[0]).m_distance+"");
						attributeMap.put("teammateOneDir", ((PlayerInfo)pArray[0]).m_direction+"");
						break;
					}
				}
				// add opponents info to map
				if (opponents.size() > 0) {
					Object[] pArray = opponents.toArray();
					
					int len = pArray.length;
					if (len > 5)
						len = 5;
					switch(len) { // Only consider 5 opponents at most.
					case 5:
						attributeMap.put("opponentFiveDis", ((PlayerInfo)pArray[4]).m_distance+"");
						attributeMap.put("opponentFiveDir", ((PlayerInfo) pArray[4]).m_direction+"");
					case 4:
						attributeMap.put("opponentFourDis", ((PlayerInfo) pArray[3]).m_distance+"");
						attributeMap.put("opponentFourDir", ((PlayerInfo) pArray[3]).m_direction+"");
					case 3:
						attributeMap.put("opponentThreeDis", ((PlayerInfo) pArray[2]).m_distance+"");
						attributeMap.put("opponentThreeDir", ((PlayerInfo) pArray[2]).m_direction+"");
					case 2:
						attributeMap.put("opponentTwoDis", ((PlayerInfo) pArray[1]).m_distance+"");
						attributeMap.put("opponentTwoDir", ((PlayerInfo) pArray[1]).m_direction+"");
					case 1:
						attributeMap.put("opponentOneDis", ((PlayerInfo) pArray[0]).m_distance+"");
						attributeMap.put("opponentOneDir", ((PlayerInfo) pArray[0]).m_direction+"");
						break;
					}
				}
				// add visual info to the string
				for (Iterator<String> iter = relAttrs.iterator(); iter.hasNext();) {
					String attr = (String) iter.next();
					if (!attr.equals("ActionType") && !attr.equals("ActionPower") && !attr.equals("ActionAngle")) {
						String value = (String) attributeMap.get(attr);
						if (value != null)
							rowStr.append(value + ",");
						else
							rowStr.append(",");
					}
				}
			} 
			// match action line
			else if (matcher.matches() && attributeMap.get("ActionType") == null) {
				skipActionFlag = false;

				// get action type and params
				actionType = matcher.group(1);
				if (actionType.equals("dash")) {
					actionPower = matcher.group(2);
				} else if (actionType.equals("kick")) {
					actionPower = matcher.group(2);
					actionAngle = matcher.group(3);
				} else if (actionType.equals("turn")) {
					actionAngle = matcher.group(2);
				} /*else if (actionType.equals("turn-neck")) {  // don't consider turn-neck for now
					actionAngle = matcher.group(2);				// since nobody uses it. Besides, it's
				}*/ else if (actionType.equals("catch")) {		// not a normal action. Need to add
					actionAngle = matcher.group(2);				// additional attributes just for it.
				} else {
					System.out.println("We are not considering action " + actionType + ".");
				}
				// append action infos to the table row, and add it to attribute map.
				if(actionType.equals("dash") || actionType.equals("kick") || actionType.equals("turn") || actionType.equals("catch")) {
							rowStr.append(actionType + ","); attributeMap.put("ActionType", actionType);
							rowStr.append(actionPower + ",");
							rowStr.append(actionAngle);
						}
			}
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		LogParser parser = new LogParser();
		
		try
	    {
		// First look for parameters
		if (args.length == 2) {
			parser.buildNewLog(args[0], args[1]);
		}
		else if (args.length == 3) {
			parser.appendToExistingLog(args[1], args[2]);
		}
		else
			throw new Exception();
	    }
	catch(Exception e)
	    {
		System.err.println("");
		System.err.println("USAGE: LogParser [-option] source file address, prototype name]");
		System.err.println("");
		System.err.println("    option  -a,   append new log to existing csv file");
		System.err.println("   ------------------------------------");
		System.err.println("");
		System.err.println("    Example:");
		System.err.println("      LogParser logs/Krislet_1.lsf Krislet");
		System.err.println("    or");
		System.err.println("      LogParser -a logs/Krislet_2.lsf Krislet");
		return;
	    }
	}

}


