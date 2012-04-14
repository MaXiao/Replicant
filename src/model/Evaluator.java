package model;

import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

/** Evaluate the performance of a given 
 *  classifier on a given dataset.  It
 *  can be used to compare error rates of
 *  different classifier to choose the
 *  best one dynamically.
 * 
 *  @author: Xiao Ma
 *  @since: March 23, 2012
 **/

public class Evaluator {
	
	private Instances trainSet;
	private Instances testSet;
	private Classifier classifier;
	
	/* 
	 * Instantiate evaluator by train set, test set and classifier
	 * 
	 * @param train : training dataset
	 * @param test : test dataset
	 * @param cls : classifier class name, e.g., weka.classifiers.trees.J48
	 */
	public Evaluator(Instances train, Instances test, String cls) {
		this.trainSet = train;
		this.testSet = test;
		
		trainSet.setClassIndex(trainSet.numAttributes() - 1);
		testSet.setClassIndex(testSet.numAttributes() - 1);
		
		try {
			Class<?> classifierClass = Class.forName(cls);
			this.classifier = (Classifier) classifierClass.newInstance();
		} catch (Exception e) {
			System.err.println("Illegal Classifier for the trainset.");
			e.printStackTrace();
		}		
	}
	
	/* 
	 * Create evaluator.
	 * Using the same dataset as training and test set.
	 * 
	 */
	public Evaluator(Instances train, String cls) {
		new Evaluator(train, train, cls);
	}
    
	/*
	 * 10-fold cross validation test.
	 */
    public void crossValidation() throws Exception {
        Evaluation eval = new Evaluation(trainSet);
        eval.crossValidateModel(classifier, trainSet, 10, new Random(1));
        // Generates a breakdown of the accuracy for each class.
        System.out.println(eval.toClassDetailsString());
        // Outputs the performance statistics in summary form.
        System.out.println(eval.toSummaryString());
        // Outputs the performance statistics as a classification confusion matrix.
        System.out.println(eval.toMatrixString());
    }
    
    /*
     * Evaluate classifier by the given test dataset.
     */
    public void evaluateTestData() throws Exception {
        classifier.buildClassifier(trainSet);
       
        Evaluation eval = new Evaluation(trainSet);
        eval.evaluateModel(classifier, testSet);
        System.out.println(eval.toClassDetailsString());
        System.out.println(eval.toSummaryString());
        System.out.println(eval.toMatrixString());
    }
    
    /*
     * Get error rate by 10-fold cross validation.
     */
    public double getCrossValErrorRate() throws Exception {
        Evaluation eval = new Evaluation(trainSet);
        eval.crossValidateModel(classifier, trainSet, 10, new Random(1));
    	
    	return eval.errorRate();
    }
    
    /*
     * Get error rate of given test dataset.
     */
    public double getTestSetErrorRate() throws Exception {
        classifier.buildClassifier(trainSet);
        
        Evaluation eval = new Evaluation(trainSet);
        eval.evaluateModel(classifier, testSet);
        
        return eval.errorRate();
    }
    
    public static void main(String[] args) throws Exception {
    	DatasetBuilder dBuilder;
    	Evaluator eval;
    	
    	try
	    {
			// First look for parameters
			if (args.length == 3) {
				dBuilder = new DatasetBuilder(args[0]);
				Instances dataset = dBuilder.getDataset(args[1]);
				eval = new Evaluator(dataset, dataset, args[2]);
				eval.crossValidation();
			}
			else {
				throw new Exception();
			}
	    }
	catch(Exception e)
	    {
		System.err.println("");
		System.err.println("USAGE: Evaluator prototype model_type classifier");
		System.err.println("");
		System.err.println("Example:");
		System.err.println("       Evaluator Garfield ActionType weka.classifiers.rules.JRip");
		System.err.println("");
		System.err.println("");
		System.err.println("--------------Notes------------------");
		System.err.println("Classifier name should be the full weka class name. ");
		System.err.println("Please make sure these classifiers can be used for");
		System.err.println("the numerical or binominal attribute you want to classify. ");
		System.err.println("");
		System.err.println("Prototype is the name of prototype you used in config.xml");
		System.err.println("");
		System.err.println("Model Type could be ActionType, KickPower, KickAngle");
		System.err.println("DashPower and TurnAngle");
		return;
	    }
	}
}
