package annotator

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
    val rule1: Element => Annotation = e => {
      Annotation(Some('a'), AnnoMap(1->'a', 4->'e', 15->'b', 400->'c'), List(cType,eType), tt)
    }

    val zType = AnnoType("zzz", Right('z'))
    val xType = AnnoType("xxx", Right('x'))
    val yType = AnnoType("yyy", Right('y'))
    val at = AnnoType("whatev", Left(List(zType, xType, yType)))

    val rule2: Element => Annotation = e => {
      Annotation(None, AnnoMap(3->'x', 9->'y', 20->'z', 37->'x'), List(xType), at)
    }

    val annoGroup = mkAnnoGroup(filePath)
    //val ag1 = annotate(List(rule1, rule2), annoGroup) 
    write(annoGroup)
    //val ag2 = annotateOn(List(at), List(rule1, rule2), annoGroup) 

  }

}
