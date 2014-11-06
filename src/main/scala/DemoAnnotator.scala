package annotator

import java.io.File
import org.jdom2.Content
import org.jdom2.util.IteratorIterable
import scala.collection.immutable.Queue
import scala.collection.JavaConversions.iterableAsScalaIterable 

import scala.collection.immutable.IntMap
import org.jdom2.input.SAXBuilder
import org.jdom2.filter.ElementFilter
import org.jdom2.Element
import org.jdom2.Document
import org.jdom2.util.IteratorIterable

object DemoAnnotator {
  import Annotator._

  def main(args: Array[String]): Unit = {
    val filePath = args(0)
    val builder = new SAXBuilder()
    val dom = builder.build(new File(filePath)) 
    val annotator = LineAnnotator.addLineAnnotation(new Annotator(dom))

    val table = (annotator.getBIndexList(LineAnnotator.lineAnnoType).map {
      case (blockIndex, charIndex) =>

        val textMap = annotator.getTextMap(LineAnnotator.lineAnnoType)(blockIndex, charIndex)

        val lineText = textMap.values.mkString("")


        def getLabel() = {
          if (blockIndex % 3 == 0) {
            Some(B('a'))
          } else if ((blockIndex - 1) % 3 == 0) {
            Some(L)
          } else if (blockIndex % 7 == 0) {
            Some(B('b'))
          } else if ((blockIndex - 1) % 7 == 0) {
            Some(L)
          } else {
            Some(U('b'))
          }
        }

        def getLabel(): Option[Label] = {
          readLine(s"line: ${lineText}: ") match {
            case "b" => Some(B('d'))
            case "i" => Some(I)
            case "o" => Some(O)
            case "l" => Some(L)
            case "u" => Some(U('d'))
            case "n" => None
            case _ => {
              println("Please enter either b, i, o, l, u, or n")
              getLabel()
            }
          }
        }

        (blockIndex -> charIndex) -> getLabel()

    }).toMap

    //val refAnnoType = AnnoType("demo", 'd')
    val typeList = List(AnnoType("aaa", 'a'), AnnoType("bbb", 'b'))

    annotator.annotateAnnoType(LineAnnotator.lineAnnoType, typeList, (blockIndex, charIndex) => {
      table(blockIndex -> charIndex)
    }).write("/Users/klimzaporojets/out.svg")

  }

}
