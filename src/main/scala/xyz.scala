
package annotator

import java.io.File
import org.jdom2.{Element, Content, Document}
import org.jdom2.util.IteratorIterable
import scala.collection.immutable.Queue
import scala.collection.JavaConversions.iterableAsScalaIterable 

import scala.collection.immutable.IntMap
import org.jdom2.input.SAXBuilder
import org.jdom2.filter.ElementFilter
import org.jdom2.util.IteratorIterable

object XyzAnnotator {
  import Annotator._

  def main(args: Array[String]): Unit = {
    val filePath = args(0)
    val builder = new SAXBuilder()
    val dom = builder.build(new File(filePath)) 
    val annotator = LineAnnotator.addLineAnnotation(new Annotator(dom))


    //some examples

    val refAnno = annotator.annotate(List("reference" -> 'r'), Single(SegmentCon("line")), (blockIndex, charIndex) => {
      val textMap = annotator.getTextMap(/*LineAnnotator.segmentType*/ "line")(blockIndex, charIndex)
      println("the following textMap1: " + textMap)
        if (blockIndex % 3 == 0) {
          Some(B('r'))
        } else if (blockIndex % 3 == 1) {
          Some(I)
        } else {
          Some(L)
        }
      None
    }).write("/Users/klimzaporojets/out.svg")

//
//    refAnno.annotate(List("group" -> 'g'), Range(SegmentCon("reference"), CharCon), (blockIndex, charIndex) => {
//       Some(U('g'))
//    }).write("/home/thomas/out.svg")
//
//
//
//    val lineBIndexPairSet = refAnno.getAnnotatableIndexPairSet(Single(SegmentCon("line")))
//    //see what other annotations exist at each line
//    lineBIndexPairSet.map {
//      case (blockIndex, charIndex) =>
//
//        val elements = refAnno.getElements("line")(blockIndex, charIndex)
//        val annotationBlock = refAnno.annotationBlockSeq(blockIndex)
//        annotationBlock.annotationMap.map {
//          case (annoType, annoSpan) =>
//            annoSpan.labelMap.get(charIndex) match {
//              case Some(label) =>
//              case None =>
//            }
//        }
//    }


   /* alternative */
    val lineBIndexPairSet = annotator.getAnnotatableIndexPairSet(Single(SegmentCon("line")))


    //see what other annotations exist at each line
    val res = lineBIndexPairSet.toList.map {
      case (blockIndex, charIndex) =>
        val textMap = annotator.getTextMap(/*LineAnnotator.segmentType*/ "line")(blockIndex, charIndex)
//        println("the following textMap: " + textMap)
        textMap.values.mkString("")

    }

    print(res);


    val elements = lineBIndexPairSet.toList.map {
      case (blockIndex, charIndex) =>
        val elements = annotator.getElements("line")(blockIndex, charIndex)
        val parent = elements(elements.firstKey).getParentElement
        println(parent)
    }

//    elements(0)


    val groupedByLineContent:Map[Int, IntMap[Element]] = lineBIndexPairSet.toList.zipWithIndex.map {
      case (blockCharIndex, charIndex) =>
        val elements = annotator.getElements("line")(blockCharIndex._1, blockCharIndex._2)
        println(blockCharIndex._1)
        println(blockCharIndex._2)
        Map(charIndex -> elements)
    }.flatten.toMap

    println(groupedByLineContent)

//    val groupedByLineContent:Map[Int, Iterable[Element]] = lineBIndexPairSet.toList.zipWithIndex.map {
//      case (blockCharIndex, charIndex) =>
//        val elements = annotator.getElements("line")(blockCharIndex._1, blockCharIndex._2)
//        Map(charIndex -> elements.values)
//    }.flatten.toMap

//    println(groupedByLineContent)
    val lines:Set[Int] = groupedByLineContent.keySet

    val keysIterator:Iterator[Int] = groupedByLineContent.keysIterator

    val sortedLines:List[Int] = keysIterator.toList.sorted

    //we are here
    val lastLine:Int  = lines.max
    val firstLine:Int = lines.min
    println("")

    for(
      currentLine <- sortedLines
    ) {
      //      println("current line: " + currentLine)
      val line: IntMap[Element] = groupedByLineContent.get(currentLine).get

      println(line.map{_._2.getContent.map{_.getValue}}.flatten.mkString(""))
    }
//
//    println("")

    val fonts = lineBIndexPairSet.map {
      case (blockIndex, charIndex) =>
        val elements = annotator.getElements("line")(blockIndex, charIndex)
        elements.map{x=>
          x._2.getAttribute("font-family").getValue
        }
    }.flatten


    val sizes = lineBIndexPairSet.toList.map {
      case (blockIndex, charIndex) =>
        val elements = annotator.getElements("line")(blockIndex, charIndex)
        elements.map{x=>
          val currVal = x._2.getAttribute("font-size").getValue

          currVal.substring(0,currVal.indexOf("px"))
        }
    }.flatten.groupBy(_.toString).mapValues(_.count(x=>true))

    val sortsizes = sizes.toList.sortBy(- _._2)

    println("The most common size is: " + sortsizes(0)._1)


    val sizes2 = lineBIndexPairSet.map {
      case (blockIndex, charIndex) =>
        val elements = annotator.getElements("line")(blockIndex, charIndex)
        elements.map{x=>
          val currVal = x._2.getAttribute("font-size").getValue
          currVal.substring(0,currVal.indexOf("px"))
        }
    }.flatten //.groupBy(_.toString).mapValues(_.count(x=>true))

    val sortsizes2 = sizes2.toList.sortBy(- _.toInt)

    println("The largest font size is: " + sortsizes2(0))

//test1-20.svg
//    val res2:List[IntMap[String]] = res.toList.sortBy(x=> x.firstKey)
//    println(res)


//
//    refAnno.annotate(List("ref" -> 'r', "junk" -> 'j', "pro" -> 'p'), Single(SegmentCon("line")), (blockIndex, charIndex) => {
//      Some(U('p'))
//    }).write("/home/thomas/out.svg")


    //some tests

    annotator.annotate(List("ref" -> 'r', "junk" -> 'j', "pro" -> 'p'), Single(SegmentCon("line")), (blockIndex, charIndex) => {

      val textMap = annotator.getTextMap(/*LineAnnotator.segmentType*/ "line")(blockIndex, charIndex)

//      val lineText = textMap.values.mkString("")
//      def getLabel(): Option[Label] = {
//        readLine(s"line: ${lineText}: ") match {
//          case "br" => Some(B('r'))
//          case "bj" => Some(B('r'))
//          case "bp" => Some(B('r'))
//          case "i" => Some(I)
//          case "o" => Some(O)
//          case "l" => Some(L)
//          case "ur" => Some(U('r'))
//          case "uj" => Some(U('j'))
//          case "up" => Some(U('p'))
//          case "n" => None
//          case _ => {
//            println("Please enter either b, i, o, l, u, or n")
//            getLabel()
//          }
//        }
//      }
      println("textMap3: " + textMap)
      Some(U('j'))
//      getLabel()
    }).write("/Users/klimzaporojets/out.svg")



//      (annotator.getBIndexList(LineAnnotator.segmentType).map {
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
//    }).toMap




  }

}
