package annotator

import org.jdom2.Content
import org.jdom2.util.IteratorIterable
import scala.collection.immutable.Queue
import scala.collection.JavaConversions.iterableAsScalaIterable 

object LineAnnotator {
  import Annotator._


  def main(args: Array[String]) = {

    val filePath = args(0)

    val annotator = new Annotator(filePath)

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

    def firstAndLast(e: Element, ee: Element) = {
      val eeLast = ee.getText().size - 1
      List(
        e -> ((1 until e.getText().size).foldLeft(AnnoMap[Char]())((annoMap, i) => {
          annoMap + (i -> '~')
        }) + (0 -> 'l') ),
        ee -> ( (0 until eeLast).foldLeft(AnnoMap[Char]())((annoMap, i) => {
          annoMap + (i -> '~') 
        }) + (eeLast -> '$') )
      )
    }

    val elAnnoMap = lineList.flatMap(line => {
      line match {
        case e::Nil => 
          val lastIndex = e.getText().size - 1
          List(
            if (lastIndex == 0) { e -> AnnoMap(0 -> 'L') } 
            else {
              e -> ((1 until lastIndex).foldLeft(AnnoMap[Char]())((annoMap, i) => {
                annoMap + (i -> '~')
              }) + (lastIndex -> '$') + (0 -> 'l'))
            }
          )
        case e::ee::Nil => firstAndLast(e, ee)
        case es => 
          val first = es.head
          val tail = es.tail
          val middle = tail.init
          val last = tail.last
          firstAndLast(first, last) ++ middle.map(e => {
            e -> ( (0 until e.getText().size).foldLeft(AnnoMap[Char]())((annoMap, i) => {
              annoMap + (i -> '~')
            }))
          })
      }
    }).toMap

    val rule: Element => Option[Annotation] = e => {

      val annoType = AnnoType("line", Left('l'))

      elAnnoMap.get(e) match {
        case None => None
        case Some(annoMap) => Some(Annotation(annoMap, List(), annoType))
      }

    }


    annotator.annotate(List(rule)) 
    annotator.write()

  }

}
