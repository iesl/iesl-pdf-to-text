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


  val lineAnnoType = AnnoType("line", 'l')


  def addLineAnnotation(annotator: Annotator): Annotator =  {
    val lineList = annotator.getElements().foldLeft(Queue[Queue[Element]]())((queueAcc, e) => {
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
        }) + (0 -> B(0)) ),
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
              IntMap(0 -> U(0)) 
            } else {
              (1 until lastIndex).foldLeft(IntMap[Label]())((annoMap, i) => {
                annoMap + (i -> I)
              }) + (lastIndex -> L) + (0 -> B(0))
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


    annotator.annotateChar(List(lineAnnoType), (blockIndex, charIndex) => {
      labelMapSeq(blockIndex).get(charIndex)
    })

  }


}
