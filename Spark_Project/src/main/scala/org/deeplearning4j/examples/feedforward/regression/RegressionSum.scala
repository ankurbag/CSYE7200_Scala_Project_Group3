package org.deeplearning4j.examples.feedforward.regression

import java.util.{Collections, Random}
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.{NeuralNetConfiguration, Updater}
import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions

/**
 * Created by Anwar on 3/15/2016.
 * An example of regression neural network for performing addition
 */
object RegressionSum {
    //Random number generator seed, for reproducability
    val seed = 12345
    //Number of iterations per minibatch
    val iterations = 1
    //Number of epochs (full passes of the data)
    val nEpochs = 200
    //Number of data points
    val nSamples = 1000
    //Batch size: i.e., each epoch has nSamples/batchSize parameter updates
    val batchSize = 100
    //Network learning rate
    val learningRate = 0.01
    // The range of the sample data, data in range (0-1 is sensitive for NN, you can try other ranges and see how it effects the results
    // also try changing the range along with changing the activation function
    val MIN_RANGE = 0
    val MAX_RANGE = 3

    val rng = new Random(seed)

    def main(args: Array[String]): Unit = {

        /*val bufferedSource =scala.io.Source.fromFile("test1.csv")
        for (line <- bufferedSource.getLines) {
            val cols = line.split(",").map(_.trim)
            // do whatever you want with the columns here
            println(s"${cols(0)}|${cols(1)}|${cols(2)}|${cols(3)}")*/
        //Generate the training data
        val iterator = getTrainingData(batchSize,rng)

        //Create the network
        val numInput = 2
        val numOutputs = 1
        val nHidden = 10
        val net: MultiLayerNetwork = new MultiLayerNetwork(new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInput).nOut(nHidden)
                        .activation("tanh")
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation("identity")
                        .nIn(nHidden).nOut(numOutputs).build())
                .pretrain(false).backprop(true).build()
        )
        net.init()
        net.setListeners(new ScoreIterationListener(1))


        //Train the network on the full data set, and evaluate in periodically
        (0 until nEpochs).foreach { _ =>
            iterator.reset()
            net.fit(iterator)
        }
        // Test the addition of 2 numbers (Try different numbers here)
        val input = Nd4j.create(Array[Double](0.4, 0.5), Array[Int](1, 2))
        val out = net.output(input, false)
        System.out.println(out)
        System.out.println(net.error(out))
    }

    def getTrainingData(batchSize: Int, rand: Random): DataSetIterator = {
        val input1Builder = Array.newBuilder[Double]
        val input2Builder = Array.newBuilder[Double]
        val sumBuilder = Array.newBuilder[Double]
        (0 until nSamples).foreach { i =>
            val i1 = MIN_RANGE + (MAX_RANGE - MIN_RANGE) * rand.nextDouble()
            val i2 =  MIN_RANGE + (MAX_RANGE - MIN_RANGE) * rand.nextDouble()
            input1Builder += i1
            input2Builder += i2
            sumBuilder += i1 + i2
        }
        val inputNDArray1 = Nd4j.create(input1Builder.result(), Array[Int](nSamples,1))
        val inputNDArray2 = Nd4j.create(input2Builder.result(), Array[Int](nSamples,1))
        val inputNDArray = Nd4j.hstack(inputNDArray1,inputNDArray2)
        val outPut = Nd4j.create(sumBuilder.result(), Array[Int](nSamples, 1))
        val dataSet = new DataSet(inputNDArray, outPut)
        val listDs: java.util.List[DataSet] = dataSet.asList()
        Collections.shuffle(listDs,rng)
        new ListDataSetIterator(listDs,batchSize)

    }
}
