
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

object XyzAnnotator {
  import Annotator._

  def main(args: Array[String]): Unit = {
    val filePath = args(0)
    val builder = new SAXBuilder()
    val dom = builder.build(new File(filePath)) 
    val annotator = LineAnnotator.addLineAnnotation(new Annotator(dom))


    //some examples

    val refAnno = annotator.annotate(List("reference" -> 'r'), Single(SegmentCon("line")), (blockIndex, charIndex) => {
        if (blockIndex % 3 == 0) {
          Some(B('r'))
        } else if (blockIndex % 3 == 1) {
          Some(I)
        } else {
          Some(L)
        }
    }).write("/home/thomas/out.svg")

    refAnno.annotate(List("group" -> 'g'), Range(SegmentCon("reference"), CharCon), (blockIndex, charIndex) => {
       Some(U('g'))
    }).write("/home/thomas/out.svg")



    val lineBIndexPairSet = refAnno.getAnnotatableIndexPairSet(Single(SegmentCon("line")))
    //see what other annotations exist at each line
    lineBIndexPairSet.map {
      case (blockIndex, charIndex) =>

        val elements = refAnno.getElements("line")(blockIndex, charIndex)
        val annotationBlock = refAnno.annotationBlockSeq(blockIndex)
        annotationBlock.annotationMap.map {
          case (annoType, annoSpan) =>
            annoSpan.labelMap.get(charIndex) match {
              case Some(label) =>
              case None =>
            }
        }
    }

    refAnno.annotate(List("ref" -> 'r', "junk" -> 'j', "pro" -> 'p'), Single(SegmentCon("line")), (blockIndex, charIndex) => {
      Some(U('p'))
    }).write("/home/thomas/out.svg")


  }

}
