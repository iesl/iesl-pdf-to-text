package annotator

import java.io.File
import org.jdom2.Content
import org.jdom2.util.IteratorIterable
import scala.collection.immutable.Queue
import scala.collection.JavaConversions.iterableAsScalaIterable 

import org.jdom2.input.SAXBuilder
import org.jdom2.filter.ElementFilter
import org.jdom2.Element
import org.jdom2.Document
import org.jdom2.util.IteratorIterable

object LineAnnotator {
  import Annotator._


  def main(args: Array[String]) = {

    val filePath = args(0)

    val builder = new SAXBuilder()
    val dom = builder.build(new File(filePath)) 


    val annotator = new Annotator(dom)

    val lineList = annotator.elements.foldLeft(Queue[Queue[Element]]())((queueAcc, e) => {
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

    def firstAndLast(e: Element, ee: Element) = {
      val eeLast = ee.getText().size - 1
      (
        ((1 until e.getText().size).foldLeft(AnnoMap[Char]())((annoMap, i) => {
          annoMap + (i -> '~')
        }) + (0 -> 'l') ),
        ( (0 until eeLast).foldLeft(AnnoMap[Char]())((annoMap, i) => {
          annoMap + (i -> '~') 
        }) + (eeLast -> '$') )
      )
    }

    val annoMapSeq = lineList.toIndexedSeq.flatMap(line => {
      line match {
        case e::Nil => 
          val lastIndex = e.getText().size - 1
          IndexedSeq(
            if (lastIndex == 0) {
              AnnoMap(0 -> 'L') 
            } else {
              (1 until lastIndex).foldLeft(AnnoMap[Char]())((annoMap, i) => {
                annoMap + (i -> '~')
              }) + (lastIndex -> '$') + (0 -> 'l')
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
            (0 until e.getText().size).foldLeft(AnnoMap[Char]())((annoMap, i) => {
              annoMap + (i -> '~')
            })
          }) :+ fl._2
      }
    })

    val rule: Int => Option[Annotation] = i => {
      val annoType = AnnoType("line", Left('l'))
      val annoMap = annoMapSeq(i)
      Some(Annotation(annoMap, List(), annoType))
    }


    annotator.annotate(List(rule)) 
    annotator.write()

  }

}
