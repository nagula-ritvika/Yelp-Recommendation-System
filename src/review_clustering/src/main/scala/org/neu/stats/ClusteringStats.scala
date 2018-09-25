package org.neu.stats

import java.io._

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.neu.model.{business, docTopic, reviews}

/**
  * @author Rashmi Dwaraka
  */
object ClusteringStats {

  /**
    * Write to file
    * @param output
    * @param outputPath
    */
  def writeToFile(output: Array[String], outputPath: String): Unit = {
    val file = new File(outputPath)
    val bw = new BufferedWriter(new FileWriter(file))
    output.foreach(o => bw.write(o))
    bw.close()
  }

  /**
    * Topic wise count
    * @param topic_cluster
    * @param opPath
    */
  def topicCount(topic_cluster: RDD[(Int, String)], opPath: String): Unit = {
    val topic_count = topic_cluster.map(x => (x._1, 1)).
      reduceByKey(_ + _).map(x => "Topic " + (x._1).toString + ": " + x._2.toString + "\n").collect

    writeToFile(topic_count, opPath + "topic_distribution.txt")
  }

  /**
    * Topic wise count for high and low rating
    * @param reviewsRDD
    * @param topic_reviews
    * @param opPath
    */
  def Rating_topicCount(reviewsRDD: RDD[(String, Float, String)],
                        topic_reviews: RDD[(String, Int)], opPath: String): Unit = {

    val highRating_topic = reviewsRDD.map(r => (r._1, (r._2, r._3))).
      filter(r => r._2._1 >= 4.0).
      join(topic_reviews).persist
    val lowRating_topic = reviewsRDD.map(r => (r._1, (r._2, r._3))).filter(r => r._2._1 <= 2.0).persist

        //high and low rating
    val highRating_topic_count = highRating_topic.
      map { case (rid, ((stars, bid), topic)) => (topic, 1) }.
      reduceByKey(_ + _).map(x => "Topic " + (x._1).toString + ": " + x._2.toString + "\n").collect

    writeToFile(highRating_topic_count, opPath + "high_rating_topic_distribution.txt")

    val lowRating_topic_count = lowRating_topic.join(topic_reviews).
      map { case (rid, ((stars, bid), topic)) => (topic, 1) }.
      reduceByKey(_ + _).map(x => "Topic " + (x._1).toString + ": " + x._2.toString + "\n").collect

    writeToFile(lowRating_topic_count, opPath + "low_rating_topic_distribution.txt")

  }


  /**
    * Topic wise count for high and low rating
    * @param reviewsRDD
    * @param topic_reviews
    * @param businessRDD
    * @param opPath
    */
  def topBusiness(reviewsRDD: RDD[(String, Float, String)], topic_reviews: RDD[(String, Int)],
                  businessRDD: RDD[(String, (String, Float,String))], opPath: String): Unit = {


    val highRating_topic = reviewsRDD.map(r => (r._1, r._2, r._3)).
      filter(r => r._2 >= 4.5).map(r => (r._1, r._3)).
      join(topic_reviews).map(r => r._2).persist
    val lowRating_topic = reviewsRDD.map(r => (r._1, r._2, r._3)).
      filter(r => r._2 <= 2.0).
      map(r => (r._1, r._3)).
      join(topic_reviews).map(r => r._2).persist

    //Top 10 business

    val filtered_businessRDD = businessRDD.filter(r => r._2._2 >= 4.5).distinct.persist

    var best_business_topic = highRating_topic.
      join(filtered_businessRDD).
      map { case (bid, (topic, (name, rating,city))) => s"$topic,$bid,$name,$city,$rating" }.distinct.
      coalesce(1,shuffle = true).saveAsTextFile(opPath+"Best_Restaurants_TopicWise")

    var worst_business_topic = lowRating_topic.
      join(filtered_businessRDD).
      map { case (bid, (topic, (name, rating,city))) => s"$topic,$bid,$name,$city,$rating" }.distinct.
      coalesce(1,shuffle = true).saveAsTextFile(opPath+"Worst_Restaurants_TopicWise")

//    val topBusiness = business_topic.
//      groupByKey.map { case (key, it) =>
//      (key, it.toList.distinct.sortBy(mr => -mr._3).take(20).mkString("\n"))
//    }.persist
//
//    val topBusinessEachTopic = List.tabulate(50)(n => {
//      writeToFile(
//        topBusiness.filter(r => r._1 == n).map { case (topic, it) => it}.collect,
//        opPath + "top_Business/topic_" + n + "_businesses.csv")
//    })

  }


  /**
    * main function
    * @param args
    */
  def main(args: Array[String]): Unit = {

    // create Spark context with Spark configuration
    // val sc = new SparkContext(new SparkConf().setAppName("Spark Count").setMaster("local"))
    val sc = new SparkContext(new SparkConf().setAppName("Clustering"))
    sc.setLogLevel("ERROR")

    // read input file paths and output path
    val reviews = sc.textFile(args(0))
    val doc_topic = sc.textFile(args(1))
    val business = sc.textFile(args(2))
    val opPath = args(3)

    val reviewsRDD = reviews.mapPartitionsWithIndex {
      case (0, iter) => iter.drop(1)
      case (_, iter) => iter
    }.
      map(row => new reviews(row)).
      map(rev => (rev.review_id, rev.stars, rev.business_id)).persist

    val businessRDD = business.mapPartitionsWithIndex {
      case (0, iter) => iter.drop(1)
      case (_, iter) => iter
    }.
      map(row => new business(row)).
      map(b => (b.business_id, (b.name, b.stars,b.city))).persist

    val topic_cluster = doc_topic.map(row => {
      val top_topic = new docTopic(row)
      ((top_topic.topic1, top_topic.review), (top_topic.topic2, top_topic.review))
    }).
      flatMap { x => Set(x._1, x._2) }.persist


    //Topic wise count
    topicCount(topic_cluster, opPath)

    val topic_reviews = topic_cluster.map(_.swap)

    //High and Low Ratings
    Rating_topicCount(reviewsRDD, topic_reviews, opPath)

    //Top and low businesses for each topic
    topBusiness(reviewsRDD, topic_reviews, businessRDD, opPath)

    println("Done!")

  }

}

