package org.neu.model

import java.io.Serializable

/**
  * @author Rashmi Dwaraka
  */
class docTopic(row: String) extends Serializable {

  val columns = row.split("\t") //All the columns from the row passed in the constructor

  //Attributes of the document topic distribution required to perform the queries
  val index: Int = try {
    columns(0).toInt
  }
  catch {
    case e: Throwable => -1
  }

  val review: String = columns(1).split("/").last

  val x = List.tabulate(20)(n => columns(n+2).toFloat).
    zipWithIndex.map(_.swap).
    sortBy(_._2)(Ordering[Float].reverse).take(2)

  val topic1 : Int = x(0)._1
  val topic2 : Int = x(1)._1


}
