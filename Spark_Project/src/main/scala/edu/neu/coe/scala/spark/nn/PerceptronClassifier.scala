package edu.neu.coe.scala.spark.nn

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.Row


/**
 * @author scalaprof
 */
object PerceptronClassifier extends App {

	val conf = new SparkConf().setMaster("local").setAppName("spam")
	val sc = new SparkContext(conf)
	val sqlContext = new org.apache.spark.sql.SQLContext(sc)
	val sparkHome = "/Spark_Project/spark-2.0.2-bin-hadoop2.7/"//"/Applications/spark-1.5.1-bin-hadoop2.6/" // D:\Softwares\spark-2.0.2-bin-hadoop2.7\spark-2.0.2-bin-hadoop2.7
	val trainingFile = "sample_multiclass_classification_data.txt"

	// this is used to implicitly convert an RDD to a DataFrame.
	import sqlContext.implicits._

	// Load training data
	val data = MLUtils.loadLibSVMFile(sc, s"$trainingFile").toDF()
	// Split the data into train and test
	val splits = data.randomSplit(Array(0.6, 0.4), seed = 1234L)
	val train = splits(0)
	val test = splits(1)
	// specify layers for the neural network: 
	// input layer of size 4 (features), two intermediate of size 5 and 4 and output of size 3 (classes)
	val layers = Array[Int](4, 5, 4, 3)
	// create the trainer and set its parameters
	val trainer = new MultilayerPerceptronClassifier()
			.setLayers(layers)
			.setBlockSize(128)
			.setSeed(1234L)
			.setMaxIter(100)
	// train the model
	val model = trainer.fit(train)
	// compute precision on the test set
	val result = model.transform(test)
	val predictionAndLabels = result.select("prediction", "label")
  predictionAndLabels.show
	val evaluator = new MulticlassClassificationEvaluator()
			.setMetricName("precision")
	println("Precision:" + evaluator.evaluate(predictionAndLabels))
}