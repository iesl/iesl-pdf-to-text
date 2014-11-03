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

object LineAnnotator {
  import Annotator._


  def main(args: Array[String]): Unit = {

    val filePath = args(0)

    val builder = new SAXBuilder()
    val dom = builder.build(new File(filePath)) 


    val annotator = new Annotator(dom)

    val lineList = annotator.elements().foldLeft(Queue[Queue[Element]]())((queueAcc, e) => {
      queueAcc.lastOption match {
        case Some(currentLine) if (
            e.getAttribute("y").getValue() 
            == currentLine.last.getAttribute("y").getValue()
        ) => 
          queueAcc.init.enqueue {
            queueAcc.last.enqueue(e)
          }
        case _ => 
          queueAcc.enqueue(Queue(e))
      }
    }).map(_.toList).toList

    //Line By Char 

    def firstAndLast(e: Element, ee: Element) = {
      val eeLast = ee.getText().size - 1
      (
        ((1 until e.getText().size).foldLeft(IntMap[Label]())((annoMap, i) => {
          annoMap + (i -> I)
        }) + (0 -> B) ),
        ( (0 until eeLast).foldLeft(IntMap[Label]())((annoMap, i) => {
          annoMap + (i -> I) 
        }) + (eeLast -> L) )
      )
    }

    val labelMapSeq = lineList.toIndexedSeq.flatMap(line => {
      line match {
        case e::Nil => 
          val lastIndex = e.getText().size - 1
          IndexedSeq(
            if (lastIndex == 0) {
              IntMap(0 -> U) 
            } else {
              (1 until lastIndex).foldLeft(IntMap[Label]())((annoMap, i) => {
                annoMap + (i -> I)
              }) + (lastIndex -> L) + (0 -> B)
            }
          )
        case e::ee::Nil => 
          val fl = firstAndLast(e, ee)
          IndexedSeq(fl._1, fl._2)
        case es => 
          val first = es.head
          val tail = es.tail
          val middle = tail.init
          val last = tail.last
          val fl = firstAndLast(first, last) 
          fl._1 +: middle.toIndexedSeq.map(e => {
            (0 until e.getText().size).foldLeft(IntMap[Label]())((annoMap, i) => {
              annoMap + (i -> I)
            })
          }) :+ fl._2
      }
    })


    val rule: (Int, Int) => Option[Label] = (blockIndex, charIndex) => {
      labelMapSeq(blockIndex).get(charIndex)
    }

    val annoWithLine = annotator.annotateChar(AnnoType("line", 'l'), rule).write()

    val ruleOnLine: (Int, Int) => Option[Label] = (blockIndex, charIndex) => {
      Some(U)
    }

    annoWithLine.annotateAnnoType(AnnoType("line", 'l'), AnnoType("ref", 'r'), ruleOnLine).write()



    //Line By Block

    /*
    val labelMapSeq2 = lineList.toIndexedSeq.flatMap(line => {
      line match {
        case e::Nil => List(U)
        case e::ee::Nil => List(B, L)
        case es => 
          val first = es.head
          val tail = es.tail
          val middle = tail.init
          val last = tail.last
          val fl = firstAndLast(first, last) 
          B +: middle.toIndexedSeq.map(e => {
             I 
          }) :+ L 
      }
    })


    val rule2: Int => Option[Label] = blockIndex => {
      Some(labelMapSeq2(blockIndex))
    }

    val annoWithLine2 = annotator.annotateBlock(AnnoType("line", 'l'), rule2)

    val ruleOnLine2: (Int, Int) => Option[Label] = (blockIndex, charIndex) => {
      Some(U)
    }

    annoWithLine2.annotateAnnoType(AnnoType("line", 'l'), AnnoType("ref", 'r'), ruleOnLine2).write()
    */

  }

}
