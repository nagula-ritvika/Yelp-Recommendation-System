package org.neu.recommend

import java.io.{BufferedWriter, File, FileWriter, Serializable}

import org.apache.spark.{SparkConf, SparkContext}
import org.neu.model.business

/**
  * @author Rashmi Dwaraka
  */
object recommendations {

  /**
    * Schema definition for category rating
    * @param row
    */
  class categoryRating(row: String) extends Serializable {
    val columns = row.split(",") //All the columns from the row passed in the constructor

    //Attributes of the category rating required to perform the queries

    val business_id = columns(0)

    val ambience = try {
      columns(1).toFloat
    }
    catch {
      case e: Throwable => 0.0
    }

    val service = try {
      columns(2).toFloat
    }
    catch {
      case e: Throwable => 0.0
    }

    val price = try {
      columns(3).toFloat
    }
    catch {
      case e: Throwable => 0.0
    }

    val delivery = try {
      columns(4).toFloat
    }
    catch {
      case e: Throwable => 0.0
    }

    val taste = try {
      columns(5).toFloat
    }
    catch {
      case e: Throwable => 0.0
    }

    val food = try {
      columns(6).toFloat
    }
    catch {
      case e: Throwable => 0.0
    }

  }

  /**
    * Schema definition for food type rating
    * @param row
    */
  class foodTypeRating(row: String) extends Serializable {
    val columns = row.split(",") //All the columns from the row passed in the constructor

    //Attributes of the food type rating required to perform the queries

    val business_id = columns(0)
    val food_type = columns(1)

    val stars = try {
      columns(2).toFloat
    }
    catch {
      case e: Throwable => 0.0
    }

  }

  /**
    * Write a sequence a strings to output file
    * @param output
    * @param outputPath
    */
  def writeToFile(output: Array[String], outputPath: String,header: String): Unit = {
    val file = new File(outputPath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(header)
    output.foreach(o => bw.write(o))
    bw.close()
  }

  /**
    * main function to initiate and process recommendations for restaurants
    * @param args
    */
  def main(args: Array[String]): Unit = {

    // create Spark context with Spark configuration
    // val sc = new SparkContext(new SparkConf().setAppName("Spark Count").setMaster("local"))
    val sc = new SparkContext(new SparkConf().setAppName("Recommendation Model"))
    sc.setLogLevel("ERROR")

    // read input file paths and output path
    val catBusinessRat = sc.textFile("input/business_ratings_topic_wise.csv")
    val foodTypeBusinessRat = sc.textFile("input/business_ratings_food_type.csv")
    val business = sc.textFile("input/filtered_business.csv")
    val opPath = "input/"

    val categoryRat = catBusinessRat.mapPartitionsWithIndex {
      case (0, iter) => iter.drop(1)
      case (_, iter) => iter
    }.map(row => new categoryRating(row)).map(rat => (rat.business_id, (rat.ambience, rat.service,
      rat.price, rat.delivery, rat.taste, rat.food))).persist

    val businessRDD = business.mapPartitionsWithIndex {
      case (0, iter) => iter.drop(1)
      case (_, iter) => iter
    }.map(row => new business(row)).map(b => (b.business_id, (b.name, b.stars, b.city))).persist

    val foodTypeRat = foodTypeBusinessRat.mapPartitionsWithIndex {
      case (0, iter) => iter.drop(1)
      case (_, iter) => iter
    }.map(row => new foodTypeRating(row)).map(rat => (rat.business_id, (rat.food_type, rat.stars))).persist


    val categoryRatBusiness = categoryRat.join(businessRDD).
      map { case (bid, ((ambience, service, price, delivery, taste, food), (name, stars, city))) =>
        s"$bid,$name,$stars,$city,$ambience,$service,$price,$delivery,$taste,$food\n"
      }.collect

    writeToFile(categoryRatBusiness, opPath + "category_business_rating.csv",
      "business_id,name,overall_stars,city,ambience,service,price,delivery,taste,food\n")

    val foodTypeRatBusiness = foodTypeRat.join(businessRDD).
      map { case (bid, ((foodType, stars), (name, overall_stars, city))) =>
        s"$bid,$overall_stars,$city,$name,$foodType,$stars\n"
      }.collect

    writeToFile(foodTypeRatBusiness, opPath + "foodType_business_rating.csv",
      "business_id,overall_stars,city,name,foodType,stars\n")

    println("Done!")

  }
}
