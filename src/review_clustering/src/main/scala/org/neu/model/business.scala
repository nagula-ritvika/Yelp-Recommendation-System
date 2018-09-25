package org.neu.model

import java.io.Serializable

/**
  * @author Rashmi Dwaraka
  */
class business(row: String) extends Serializable {

  /**
    * Schema mapping for the business data
    */

  val columns = row.split(",") //All the columns from the row passed in the constructor

  /**
    * Attributes of the business required to perform the queries
    */

  val stars: Float = try {
    columns(0).toFloat
  }
  catch {
    case e: Throwable => -1
  }

  val business_id: String = columns(1)
  val name: String = columns(2)

  val city: String = try {
    columns(3)
  }
  catch {
    case e : Throwable => ""
  }

}
