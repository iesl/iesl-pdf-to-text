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
    //annotator.write("/home/thomas/out.svg")


//    val table = (annotator.getBIndexList(LineAnnotator.segmentType).map {
//      case (blockIndex, charIndex) =>
//
//        val textMap = annotator.getTextMap(LineAnnotator.segmentType)(blockIndex, charIndex)
//
//        val lineText = textMap.values.mkString("")
//
//
//
//        def getLabel(): Option[Label] = {
//          readLine(s"line: ${lineText}: ") match {
//            case "b" => Some(B)
//            case "i" => Some(I)
//            case "o" => Some(O)
//            case "l" => Some(L)
//            case "u" => Some(U)
//            case "n" => None
//            case _ => {
//              println("Please enter either b, i, o, l, u, or n")
//              getLabel()
//            }
//          }
//        }
//
//        (blockIndex -> charIndex) -> getLabel()
//
//    }).toMap

    val lineBIndexPairSet = annotator.getAnnotatableIndexPairSet(Single(SegmentCon("line")))
    lineBIndexPairSet.map {
      case (blockIndex, charIndex) =>
        val textMap = annotator.getTextMap("line")(blockIndex, charIndex)


        val es = annotator.getElements("line")(blockIndex, charIndex)
        es.map {
          case (i, e) =>
            val parent = e.getParent()
            println(parent)
        }


    }



    val abc = annotator.annotate(List("aaa" -> 'a', "bbb" -> 'b'), Single(SegmentCon("line")), (blockIndex, charIndex) => {
        if (blockIndex % 3 == 0) {
          Some(B('a'))
        } else if (blockIndex % 3 == 1) {
          Some(B('b'))
        } else {
          None
        }
    }).write("/home/thomas/out.svg")
    val atype = AnnotationType("aaa", 'a', Single(SegmentCon("line")))
    val btype = AnnotationType("bbb", 'b', Single(SegmentCon("line")))

    abc.annotate(List("name" -> 'n'), Range(SegmentCon("aaa"), CharCon), (blockIndex, charIndex) => {
        Some(U('n'))
    }).write("/home/thomas/out.svg")

  }

}
