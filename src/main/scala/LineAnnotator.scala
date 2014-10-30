package annotator

import org.jdom2.Content
import org.jdom2.util.IteratorIterable
import scala.collection.immutable.Queue

object LineAnnotator {
  import Annotator._


  def main(args: Array[String]) = {

    val filePath = args(0)

    val eType = AnnoType("eee", Right('e'))
    val dType = AnnoType("ddd", Right('d'))

    val cType = AnnoType("ccc", Left(List(dType, eType)))
    val bType = AnnoType("bbb", Right('b'))
    val aType = AnnoType("aaa", Right('a'))
    val tt = AnnoType("line", Left(List(aType, bType, cType)))
    val rule1: Element => Option[Annotation] = e => {
      Some(Annotation(AnnoMap(1->'a', 4->'e', 15->'b', 400->'c'), List(cType,eType), tt))
    }

    val annotator = new Annotator(filePath)

    val lineList = {
      def loop(es: IteratorIterable[Element], queueAcc: Queue[Queue[Element]]): List[List[Element]] = {
        if (!es.hasNext) {
          queueAcc.map(_.toList).toList
        } else {
          val e = es.next()
          queueAcc.lastOption match {
            case Some(currentLine) if (e.getAttribute("y").getValue() == currentLine.last.getAttribute("y").getValue()) => 
              loop(es, queueAcc.init.enqueue {
                queueAcc.last.enqueue(e)
              })
            case _ => 
              loop(es, queueAcc.enqueue(Queue(e)))
          }
        }
      }
      loop(annotator.elements(), Queue())
    }

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

    val rule2: Element => Option[Annotation] = e => {

      val annoType = AnnoType("line", Right('l'))

      elAnnoMap.get(e) match {
        case None => None
        case Some(annoMap) => Some(Annotation(annoMap, List(), annoType))
      }

    }


    annotator.annotate(List(rule1, rule2)) 
    annotator.write()

  }

}
