package org.neu.data_transform

import java.io.{BufferedWriter, File, FileWriter}

import org.apache.spark.{SparkConf, SparkContext}
import org.neu.model.reviews

/**
  * @author Rashmi Dwaraka
  *
  * Get the mapping for review topics of each business
  */
object review_topic_transform {

  /**
    * Write to file
    * @param output
    * @param outputPath
    */
  def writeToFile(output: Array[String], outputPath: String): Unit = {
    val file = new File(outputPath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s"review_id,topic,stars,business_id\n")
    output.foreach(o => bw.write(o))
    bw.close()
  }

  /**
    * main function for processing the mapping
    * @param args
    */
  def main(args: Array[String]): Unit = {

    // create Spark context with Spark configuration
    // val sc = new SparkContext(new SparkConf().setAppName("Spark Count").setMaster("local"))
    val sc = new SparkContext(new SparkConf().setAppName("Review Topic Transformation"))
    sc.setLogLevel("ERROR")

    // read input file paths and output path
    val reviews = sc.textFile(args(0))
    val topic_reviews = sc.textFile(args(1))
    val opPath = args(2)

    val reviewsRDD = reviews.mapPartitionsWithIndex {
      case (0, iter) => iter.drop(1)
      case (_, iter) => iter
    }.map(row => new reviews(row)).map(rev => (rev.review_id, (rev.stars, rev.business_id))).persist

    val topicReviewRDD = topic_reviews.mapPartitionsWithIndex {
      case (0, iter) => iter.drop(1)
      case (_, iter) => iter
    }.map(row => {
      val columns = row.split(",")
      val reviewId : String = columns(0)
      val topic : Int =  try {
        columns(1).toInt
      }
      catch {
        case e: Throwable => -1
      }

      (reviewId,topic)
    }).persist

    val review_topic_rating = topicReviewRDD.join(reviewsRDD).map{
      case(reviewId,(topic,(stars,businessId))) => s"$reviewId,$topic,$stars,$businessId\n"
    }.persist

    writeToFile(review_topic_rating.collect(),opPath+"reviews_rating.csv")


    println("Done!")

  }
}
