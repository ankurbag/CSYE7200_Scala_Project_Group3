package org.deeplearning4j.examples.dataExamples

import org.datavec.api.records.reader.RecordReader
import org.datavec.api.records.reader.impl.csv.CSVRecordReader
import org.datavec.api.split.FileSplit
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator
import org.deeplearning4j.eval.Evaluation
import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.dataset.{DataSet, SplitTestAndTrain}
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import java.io.File

object CSVExample {

    lazy val log = LoggerFactory.getLogger(CSVExample.getClass)

    def main(args: Array[String]) {

        //First: get the dataset using the record reader. CSVRecordReader handles loading/parsing
        val numLinesToSkip = 0
        val delimiter = ","
        val recordReader: RecordReader = new CSVRecordReader(numLinesToSkip,delimiter)
        recordReader.initialize(new FileSplit(new File(System.getProperty("user.dir")+"/src/main/resources/iris.txt")))

        //recordReader.initialize(new FileSplit("iris.txt"))
        //Second: the RecordReaderDataSetIterator handles conversion to DataSet objects, ready for use in neural network
        val labelIndex = 4     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        val numClasses = 3     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        val batchSize = 150    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
        val iterator: DataSetIterator = new RecordReaderDataSetIterator(recordReader,batchSize,labelIndex,numClasses);


        val next: DataSet = iterator.next()

        val numInputs = 4
        val outputNum = 3
        val iterations = 1000
        val seed = 6L
        val listenerFreq = iterations


        log.info("Build model....")
        val conf: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .activation("tanh")
                .weightInit(WeightInit.XAVIER)
                .learningRate(0.1)
                .regularization(true).l2(1e-4)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(3)
                        .build())
                .layer(1, new DenseLayer.Builder().nIn(3).nOut(3)
                        .build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .activation("softmax")
                        .nIn(3).nOut(outputNum).build())
                .backprop(true).pretrain(false)
                .build()

        //run the model
        val model = new MultiLayerNetwork(conf)
        model.init()
        model.setListeners(new ScoreIterationListener(100))

        //Normalize the full data set. Our DataSet 'next' contains the full 150 examples
        next.normalizeZeroMeanZeroUnitVariance()
        next.shuffle()
        //split test and train
        val testAndTrain: SplitTestAndTrain = next.splitTestAndTrain(0.65)

        val trainingData = testAndTrain.getTrain
        model.fit(trainingData)

        //evaluate the model on the test set
        val eval = new Evaluation(3)
        val test: DataSet = testAndTrain.getTest
        val output: INDArray = model.output(test.getFeatureMatrix)
        eval.eval(test.getLabels, output)
        log.info(eval.stats())
    }

}
