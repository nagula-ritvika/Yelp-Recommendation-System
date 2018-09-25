package org.neu.data_transform

import org.apache.spark.{SparkConf, SparkContext}
import java.io._

import org.neu.model.docTopic

/**
  * @author Rashmi Dwaraka
  *
  * transforming the document topic probability distribution to maapping
  * of reviews with its top 2 topics
  */
object doc_topic_transform {

  /**
    * Write to file
    * @param output
    * @param outputPath
    */
  def writeToFile(output: Array[String], outputPath: String): Unit = {
    val file = new File(outputPath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s"reviewId,topic\n")
    output.foreach(o => bw.write(o))
    bw.close()
  }

  /**
    * main function initiate transform the data
    * @param args
    */
  def main(args: Array[String]): Unit = {

    // create Spark context with Spark configuration
    // val sc = new SparkContext(new SparkConf().setAppName("Spark Count").setMaster("local"))
    val sc = new SparkContext(new SparkConf().setAppName("Document Topic Transformation"))
    sc.setLogLevel("ERROR")

    // read input file paths and output path
    val doc_topic = sc.textFile(args(0))
    val opPath = args(1)

    val topic_cluster = doc_topic.map(row => {
      val top_topic = new docTopic(row)
      ((top_topic.topic1, top_topic.review), (top_topic.topic2, top_topic.review))
    }).flatMap { x => Set(x._1, x._2) }.map{case(topic,reviewId) => s"$reviewId,$topic\n"}.persist

    writeToFile(topic_cluster.collect(),opPath+"topic_reviews.csv")

  }

}
