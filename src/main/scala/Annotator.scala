package annotator

import java.io.File
import org.jdom2.Content
import org.jdom2.input.SAXBuilder

import scala.collection.JavaConversions.iterableAsScalaIterable 

import org.jdom2.output.XMLOutputter


object Annotator {


  //takes an .svg filepath as only argument
  def main(args: Array[String])() = {

    val filePath = args(0)

    val builder = new SAXBuilder()
    val xml = builder.build(new File(filePath)) 

    //println("hello annoator: " + xml.getRootElement())
    //println("hello annoator: " + filePath)
    xml.getRootElement().getDescendants().foreach(e => {
      //println("hello annoator: " + e.getValue())
    })


    val outputter = new XMLOutputter()
    outputter.output(xml, System.out)




    
   

  }

}
