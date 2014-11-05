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
        val segment = annotator.getSegment(LineAnnotator.lineAnnoType)(blockIndex, charIndex)
        val blockBIndex = segment.firstKey
        val charBIndex = segment(blockBIndex).firstKey
        val blockLIndex = segment.lastKey
        val charLIndex = segment(segment.lastKey).lastKey

        val textMap = annotator.getTextMapInRange(
            blockBIndex, 
            charBIndex,
            blockLIndex,
            charLIndex
        )

        val lineText = textMap.values.mkString("")

        def loop(): Option[Label] = {
          readLine(s"line: ${lineText}: ") match {
            case "b" => Some(B)
            case "i" => Some(I)
            case "o" => Some(O)
            case "l" => Some(L)
            case "u" => Some(U)
            case "n" => None
            case _ => {
              println("Please enter either b, i, o, l, u, or n")
              loop()
            }
          }
        }

        (blockIndex -> charIndex) -> loop()

    }).toMap

    val refAnnoType = AnnoType("demo", 'd')

    annotator.annotateAnnoType(LineAnnotator.lineAnnoType, refAnnoType, (blockIndex, charIndex) => {
      table(blockIndex -> charIndex)
    }).write("/home/thomas/out.svg")
  }

}
