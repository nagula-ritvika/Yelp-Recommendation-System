package org.neu.model

import java.io.Serializable

import scala.io.Source

/**
  * @author Rashmi Dwaraka
  */
class reviews(row: String) extends Serializable {

  val columns = row.split(",") //All the columns from the row passed in the constructor

  /**
    * Attributes of the reviews required to perform the queries
    */
  val stars: Float = try {
    columns(0).toFloat
  }
  catch {
    case e: Throwable => -1
  }

  val review_id: String = columns(1)
  val user_id: String = columns(2)
  val business_id: String = columns(3)

  val stopWords = Array(Source.fromFile("src/main/resources/stopWords.txt").mkString.split("\n"))

  val reviewWords: Array[String] = columns(4).toLowerCase.split(' ')

  val review: String = reviewWords.filter(s => !(stopWords.contains(s))).mkString(" ")


}
