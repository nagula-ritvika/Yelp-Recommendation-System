package org.neu.data_transform

import java.io.{BufferedWriter, File, FileWriter}

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author Rashmi Dwaraka
  */
object topic_rating_transform {
  //Write to file
  def writeToFile(output: Array[String], outputPath: String): Unit = {
    val file = new File(outputPath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s"business_id,food_type,stars\n")
    output.foreach(o => bw.write(o))
    bw.close()
  }

  def appendToFile(output: Array[String], outputPath: String): Unit = {
    val file = new File(outputPath)
    val bw = new BufferedWriter(new FileWriter(file,true))
    output.foreach(o => bw.write(o))
    bw.close()
  }

  def getfoodTypeAvg(foodReviews : RDD[(String,Int,Float)], typeTopics : Set[Int],
                     foodType : String, outputPath: String): Unit = {

    val FoodRat = foodReviews.filter { case (bid, top,stars) => typeTopics.contains(top) }.
      map { case (bid, top,stars)  => (bid, stars) }.
      mapValues(x => (x, 1)).
      reduceByKey((x, y) => (x._1 + y._1, x._2 + y._2)).
      mapValues(y => 1.0 * y._1 / y._2).
      map{case(bid,stars) => s"$bid,$foodType,$stars\n"}.collect

    appendToFile(FoodRat,outputPath+"business_ratings_food_type.csv")
  }

  def getAvgRating(reviewsRDD : RDD[(String, Int, Float, String)],
                   catTopics : Set[Int]) : RDD[(String,Double)] = {
    reviewsRDD.filter { case (rev, top, stars, bid) => catTopics.contains(top) }.
      map { case (rev, top, stars, bid) => (bid, stars) }.
      mapValues(x => (x, 1)).
      reduceByKey((x, y) => (x._1 + y._1, x._2 + y._2)).
      mapValues(y => 1.0 * y._1 / y._2)
  }

  def main(args: Array[String]): Unit = {

    // create Spark context with Spark configuration
    // val sc = new SparkContext(new SparkConf().setAppName("Spark Count").setMaster("local"))
    val sc = new SparkContext(new SparkConf().setAppName("Review Topic Transformation"))
    sc.setLogLevel("ERROR")

    // read input file paths and output path
    val reviews_topic_rating = sc.textFile(args(0))
    val opPath = args(1)

    val reviewsRDD = reviews_topic_rating.mapPartitionsWithIndex {
      case (0, iter) => iter.drop(1)
      case (_, iter) => iter
    }.map(row => {
      val columns = row.split(",")
      val reviewId: String = columns(0)
      val topic: Int = try {
        columns(1).toInt
      }
      catch {
        case e: Throwable => -1
      }

      val stars: Float = try {
        columns(2).toFloat
      }
      catch {
        case e: Throwable => -1
      }

      val businessId: String = columns(3)
      (reviewId, topic, stars, businessId)
    }).persist

    //Category wise Average Rating

    val ambienceTopics = Set(0, 1, 2, 6, 16, 20, 21, 24, 25, 28, 33, 37, 38, 48)
    val ambienceRating = getAvgRating(reviewsRDD,ambienceTopics).persist

    val serviceTopics = Set(1, 16, 20, 21, 24, 25, 27, 28, 33, 34, 35, 37, 28, 40, 45, 47, 48)
    val serviceRating = getAvgRating(reviewsRDD,serviceTopics).persist

    val priceTopics = Set(1,15,24,36,45,48)
    val priceRating = getAvgRating(reviewsRDD,priceTopics).persist

    val deliveryTopics = Set(22)
    val deliveryRating = getAvgRating(reviewsRDD,deliveryTopics).persist

    val tasteTopics = Set(14,29,35,40,46,49)
    val tasteRating = getAvgRating(reviewsRDD,tasteTopics).persist

    val foodTopics = Set(3,4,5,7,8,9,10,11,12,17,18,19,23,26,29,30,31,32,34,39,40,41,42,43,44,47,49)
    val foodRating = getAvgRating(reviewsRDD,foodTopics).persist


    val combinedRatings1 = ambienceRating.
      fullOuterJoin(serviceRating).
      map{case(bid,(aRat,sRat))=> (bid,(aRat.getOrElse(0.0),sRat.getOrElse(0.0)))}.persist

    val combinedRatings2 = priceRating.
      fullOuterJoin(deliveryRating).
      map{case(bid,(pRat,dRat))=> (bid,(pRat.getOrElse(0.0),dRat.getOrElse(0.0)))}.persist

    val combinedRatings3 = tasteRating.
      fullOuterJoin(foodRating).
      map{case(bid,(tRat,fRat))=> (bid,(tRat.getOrElse(0.0),fRat.getOrElse(0.0)))}.persist

    val combinedRatings = combinedRatings1.
      fullOuterJoin(combinedRatings2).
      map{case(bid,(cR1,cR2)) => (bid,(cR1.getOrElse(0.0,0.0),cR2.getOrElse(0.0,0.0)))}.
      fullOuterJoin(combinedRatings3).
      map{case(bid,(cR,cR3)) => (bid,cR.getOrElse((0.0,0.0),(0.0,0.0)),cR3.getOrElse(0.0,0.0))}.
      map{case(bid,((aRat,sRat),(pRat,dRat)),(tRat,fRat)) =>
          s"$bid,$aRat,$sRat,$pRat,$dRat,$tRat,$fRat\n"
      }.collect()


    //Food Type wise average rating

    val foodReviews = reviewsRDD.filter { case (rev, top, stars, bid) => foodTopics.contains(top) }.
          map { case (rev, top, stars, bid) => (bid, top,stars) }.persist


    val americanFood = Set(4,11,17)
    val americanFoodRat = foodReviews.filter { case (bid, top,stars) => americanFood.contains(top) }.
      map { case (bid, top,stars)  => (bid, stars) }.
      mapValues(x => (x, 1)).
      reduceByKey((x, y) => (x._1 + y._1, x._2 + y._2)).
      mapValues(y => 1.0 * y._1 / y._2).
      map{case(bid,stars) => s"$bid,american,$stars\n"}.collect

    writeToFile(americanFoodRat,opPath+"business_ratings_food_type.csv")

    val greekFood = Set(8)
    getfoodTypeAvg(foodReviews, greekFood, "greek", opPath)

    val koreanFood = Set(19)
    getfoodTypeAvg(foodReviews, koreanFood, "korean", opPath)

    val indianFood = Set(29)
    getfoodTypeAvg(foodReviews, indianFood, "indian", opPath)

    val vietnameseFood = Set(23)
    getfoodTypeAvg(foodReviews, vietnameseFood, "vietnamese", opPath)

    val mexicanFood = Set(30)
    getfoodTypeAvg(foodReviews, mexicanFood, "mexican", opPath)

    val salad = Set(31,39)
    getfoodTypeAvg(foodReviews, salad, "salad", opPath)

    val breakfast = Set(10)
    getfoodTypeAvg(foodReviews, breakfast, "breakfast", opPath)

    val veg = Set(42)
    getfoodTypeAvg(foodReviews, veg, "veg", opPath)

    val dessert = Set(7,39,49)
    getfoodTypeAvg(foodReviews, dessert, "dessert", opPath)

    val spanishFood = Set(5)
    getfoodTypeAvg(foodReviews, spanishFood, "spanishFood", opPath)

    val italianFood = Set(41)
    getfoodTypeAvg(foodReviews, italianFood, "italianFood", opPath)

    val seaFood = Set(12,18,39)
    getfoodTypeAvg(foodReviews, seaFood, "seaFood", opPath)

    val cafe = Set(9)
    getfoodTypeAvg(foodReviews, cafe, "cafe", opPath)

    val bar = Set(34,40)
    getfoodTypeAvg(foodReviews, bar, "bar", opPath)

    val germanFood = Set(32)
    getfoodTypeAvg(foodReviews, germanFood, "german", opPath)


    println("Done!")

  }
}
