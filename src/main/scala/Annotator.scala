package annotator

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.Writer

import org.jdom2.Content
import org.jdom2.input.SAXBuilder
import org.jdom2.filter.ElementFilter
import org.jdom2.Element
import org.jdom2.Document
import org.jdom2.util.IteratorIterable

import scala.collection.JavaConversions.iterableAsScalaIterable 
import scala.collection.immutable.IntMap
import scala.collection.immutable.Queue
import scala.collection.immutable.HashMap
import scala.collection.immutable.SortedSet

import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jdom2.output.LineSeparator
import org.jdom2.output.support.AbstractXMLOutputProcessor

object Annotator {

  sealed trait Label
  case class B(c: Char) extends Label
  case object I extends Label
  case object O extends Label
  case object L extends Label
  case class U(c: Char) extends Label

  type Segment = IntMap[IntMap[Label]]

  case class AnnotationType(name: String, c: Char, constraintRange: ConstraintRange)

  type Element = org.jdom2.Element
  type ElementFilter = org.jdom2.filter.ElementFilter
  

  sealed trait Constraint
  case object CharCon extends Constraint
  case class SegmentCon(annotationTypeName: String) extends Constraint

  sealed trait ConstraintRange
  case class Range(from: Constraint, to: Constraint) extends ConstraintRange
  case class Single(constraint: Constraint) extends ConstraintRange

  case class AnnotationSpan(
    labelMap: IntMap[Label], 
    annotationTypeSeq: Seq[AnnotationType]
  )

  case class AnnotationInfo(annotationType: AnnotationType, bIndexPairSortedSet: SortedSet[(Int, Int)])

  case class AnnotationBlock(startIndex: Int, nextIndex: Int, annotationMap: Map[AnnotationType, AnnotationSpan])

  private def getElementsOf(dom: Document) = {
    dom.getRootElement().getDescendants(new ElementFilter("tspan")).toIterable.filter(e => {
      e.getText().size > 0
    })
  }

  def fontSize(e: Element) = {
    e.getAttribute("font-size").getValue().dropRight(2).toDouble
  }

  def y(e: Element) = {
    e.getAttribute("y").getValue().toDouble 
  }

  def xs(e: Element) = {
    e.getAttribute("x").getValue().split(" ").map(_.toDouble) 
  }

  def endX(e: Element) = {
    e.getAttribute("endX").getValue().toDouble 
  }

  def commonAncestor(e1: Element, e2: Element): Element = {
    require(e1 != null && e2 != null, "one of the elements has invalid null value")
    if (e1 == e2) {
      e1
    } else {
      commonAncestor(e1.getParentElement(), e2.getParentElement())
    }
  }

  //svg matrix is defined at http://www.w3.org/TR/SVG/coords.html#EstablishingANewUserSpace
  //and at https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/transform
  private type SvgMatrix = Array[Double]

  private def svgMatrixMultiply(m1: SvgMatrix, m2: SvgMatrix): SvgMatrix = {
    require(m1.size == 6  && m2.size == 6, "one or more SvgMatrix has invalid size instead of size of 6")

    val _0 = m1(0) * m2(0) + m1(2) * m2(1) 
    val _1 = m1(1) * m2(0) + m1(3) * m2(1)
    val _2 = m1(0) * m2(2) + m1(2) * m2(3) 
    val _3 = m1(1) * m2(2) + m1(3) * m2(3)
    val _4 = m1(0) * m2(4) + m1(2) * m2(5) + m1(4) 
    val _5 = m1(1) * m2(4) + m1(3) * m2(5) + m1(5) 
    Array(_0, _1, _2, _3, _4, _5)
  }

  private def svgMatrix(e: Element): SvgMatrix = {

    val identity = Array(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    def translate2Matrix(array: Array[Double]) = Array(1.0, 0.0, 0.0, 1.0, array(0), array(1))
    def scale2Matrix(array: Array[Double]) = Array(array(0), 0.0, 0.0, array(1), 0.0, 0.0)

    Option(e.getAttribute("transform")) match {
      case Some(attr) if !attr.getValue().isEmpty  =>
        attr.getValue().split("((?<=\\))[\\s,]+)").map(str => {
          val firstN = str.indexOf("(") + 1
          val lastN = str.size - str.indexOf(")")
          val doubleArray = str.drop(firstN).dropRight(lastN).split("[\\s,]+").map(_.toDouble)
          if (str.startsWith("matrix(")) {
            assert(doubleArray.size == 6, "svg matrix has invalid size")
            doubleArray
          } else if (str.startsWith("translate(")) {
            assert(doubleArray.size == 2, "svg translate has invalid size")
            translate2Matrix(doubleArray)
          } else if (str.startsWith("scale(")) {
            assert(doubleArray.size == 2, "svg scale has invalid size")
            scale2Matrix(doubleArray)
          } else {
            identity
          }
        }).foldLeft(identity) {
          case (mAcc, m) => svgMatrixMultiply(mAcc, m)
        }
      case _ => 
        identity
    }
  }

  def getTransformedCoords(sourceE: Element, ancestorE: Element): (List[Double], Double, List[Double]) = {

    def matrixTotal(e: Element): SvgMatrix = {
      require(e != null)
      val m = svgMatrix(e)
      if (e == ancestorE) {
        m
      } else {
        svgMatrixMultiply(matrixTotal(e.getParentElement()), m)
      }
    }

    val m = matrixTotal(sourceE)
    val sourceXs = xs(sourceE)
    val sourceY = y(sourceE)

    val _xs = sourceXs.map(x => {
      m(0) * x + m(2) * sourceY + m(4)
    })

    val _ys = sourceXs.map(x => {
      m(1) * x + m(3) * sourceY + m(5)
    })

    val _endX = m(0) * endX(sourceE) + m(2) * sourceY + m(4)

    (_xs.toList, _endX, _ys.toList)

  }


}

import Annotator._
class Annotator(private val dom: Document, val annotationBlockSeq: IndexedSeq[AnnotationBlock], val annotationInfoMap: Map[String, AnnotationInfo]) {

  def this(dom: Document) = this(
    dom,
    getElementsOf(dom).foldLeft(IndexedSeq[AnnotationBlock]())( (seqAcc, e) => {
      val startIndex = if (seqAcc.isEmpty) 0 else seqAcc.last.nextIndex
      val nextIndex = startIndex + e.getText().size
      seqAcc :+ AnnotationBlock(startIndex, nextIndex, HashMap())
    } ),
    HashMap()
  )

  private val frozenDom = dom.clone()
  final def getElements() = getElementsOf(frozenDom.clone())
  private val frozenElements = getElements().toIndexedSeq

  private def renderAnnotation(a: AnnotationSpan, length: Int) = {

    val posi = (0 until length).foldLeft("")((stringAcc, i) => {
      stringAcc + (a.labelMap.get(i) match {
        case Some(B(char)) => char.toLower
        case Some(U(char)) => char.toUpper
        case Some(I) => '~'
        case Some(O) => '-'
        case Some(L) => '$'
        case None => ' '
      })
    })

    val constr =  ", constraint: " + {
      val constraintRange = a.annotationTypeSeq(0).constraintRange
      a.annotationTypeSeq.foreach(annoType => {
        assert(annoType.constraintRange == constraintRange, "annotationTypeSeq has inconsistent constraints")
      })
      def loop(cr: ConstraintRange): String = {
        cr match {
          case Single(CharCon) => 
            "char"
          case Single(SegmentCon(annotationTypeName)) => 
            annotationTypeName
          case Range(x, y) if x == y => 
            loop(Single(x))
          case Range(SegmentCon(annotationTypeName), end) => 
            val annotationType = annotationInfoMap(annotationTypeName).annotationType
            val con = annotationType.constraintRange match {
              case Single(c) => c
              case Range(_, c) => c
            }
            annotationTypeName + "." + loop(Range(con, end))
        }
      }
      loop(constraintRange)
    }

    val annot = {
      "type: " + "{" + a.annotationTypeSeq.map(at => {
        at.name + ": " + at.c
      }).mkString(", ") + "}"
    }

    "| |" + posi + "| " + "{" + annot + constr + "}"

  }

  private def renderAnnotationBlock(bb: AnnotationBlock): String = {
    val next = bb.nextIndex

    val height = (next - 1).toString.size

    val topRulerList = (height to 2 by -1).map(level => {
      "| |"+(bb.startIndex until next).map(i => {
        if (i == bb.startIndex || (i % 10) == 0){
          val divisor = Math.pow(10,level).toInt
          val digit = (i % divisor)/(divisor/10)
          if (digit == 0 && level == height) " " else digit
        } else " "
      }).mkString("")+"|"
    })

    val bottomRuler = "| |"+(bb.startIndex until next).map(_ % 10).mkString("")+"|"

    val ruler = (topRulerList :+ bottomRuler).mkString("\n")

    "\n" + bb.annotationMap.values.toList.distinct.map(renderAnnotation(_, (next - bb.startIndex))).mkString("\n") + "\n" + ruler + "\n "
  }


  private def addAnnotation(annotationSpan: AnnotationSpan, annotationBlock: AnnotationBlock) = { 
    require(annotationSpan.labelMap.lastKey < annotationBlock.nextIndex, "annotationSpan is too long for annotationBlock")
    annotationSpan.annotationTypeSeq.foldLeft(annotationBlock)((b, annotationType) => {
      b.copy(annotationMap = b.annotationMap + (annotationType -> annotationSpan))
    })

  }

  private val charBIndexPairSet = SortedSet(frozenElements.zipWithIndex.flatMap { 
    case (e, blockIndex) => 
      (0 until e.getText().size).map(charIndex => {
        blockIndex -> charIndex
      })
  }: _*)


  final def getSegment(annotationTypeName: String)(blockIndex: Int, charIndex: Int): Segment = {

    val annotationType = annotationInfoMap(annotationTypeName).annotationType

    def loop(foundFirst: Boolean, blockIndex: Int, charIndex: Int): Segment = {

      if (annotationBlockSeq.size > blockIndex) {
        val block = annotationBlockSeq(blockIndex)
        block.annotationMap.get(annotationType) match {
          case None => loop(foundFirst, blockIndex + 1, 0)
          case Some(annotation) =>
            val labelMap = annotation.labelMap
            labelMap.keys.find(_ >= charIndex) match {
              case None =>
                loop(foundFirst, blockIndex + 1, 0)
              case Some(_charIndex) =>
                val label = labelMap(_charIndex)
                (foundFirst, label) match {
                  case (false, B(char)) if annotationType.c == char => loop(true, blockIndex, _charIndex) 
                  case (false, U(char)) if annotationType.c == char => loop(true, blockIndex, _charIndex) 
                  case (false, _) => loop(false, blockIndex, _charIndex + 1)

                  case (true, L) =>
                    IntMap(blockIndex -> IntMap(_charIndex -> L))
                  case (true, U(char)) if annotationType.c == char => 
                    IntMap(blockIndex -> IntMap(_charIndex -> U(char)))
                  case (true, U(_)) => 
                    loop(foundFirst, blockIndex, _charIndex + 1)
                  case (true, B(char)) if annotationType.c != char => 
                    loop(foundFirst, blockIndex, _charIndex + 1)
                  case (true, label) => 
                    val labelTable = loop(foundFirst, blockIndex, _charIndex + 1)
                    labelTable.get(blockIndex) match {
                      case None => 
                        labelTable + (blockIndex -> IntMap(_charIndex -> label))
                      case Some(rowIntMap) => 
                        labelTable + (blockIndex -> (rowIntMap + (_charIndex -> label)))
                    }
                }
            }
        }
      } else {
        IntMap[IntMap[Label]]()
      }

    }

    loop(false, blockIndex, charIndex)

  }

  final def getRange(annotationTypeName: String)(blockIndex: Int, charIndex: Int): Option[(Int, Int, Int, Int)] = {
    val segment = getSegment(annotationTypeName)(blockIndex, charIndex)

    if (segment.isEmpty) {
      None 
    } else {
      def findLastPairIndex(blockIndex: Int, charIndex: Int, constraint: Constraint): (Int, Int) = {
        constraint match {
          case CharCon => 
            (blockIndex -> charIndex)
          case SegmentCon(annoTypeName) =>
            val annoType = annotationInfoMap(annoTypeName).annotationType
            val segment =  getSegment(annoTypeName)(blockIndex, charIndex)
            val blockLIndex = segment.lastKey
            val charLIndex = segment(segment.lastKey).lastKey
            val con = annoType.constraintRange match {
              case Single(c) => c
              case Range(_, c) => c
            }
            findLastPairIndex(blockLIndex, charLIndex, con)
        }
      }

      val blockBIndex = segment.firstKey
      val charBIndex = segment(blockBIndex).firstKey
      val con = annotationInfoMap(annotationTypeName).annotationType.constraintRange match {
        case Single(c) => c
        case Range(_, c) => c
      }
      val (blockLIndex, charLIndex) = findLastPairIndex(segment.lastKey, segment(segment.lastKey).lastKey, con)

      Some(blockBIndex, charBIndex, blockLIndex, charLIndex)

    }
  }

  final def getElementsInRange(blockIndex1: Int, blockIndex2: Int): IntMap[Element] = {
    IntMap((blockIndex1 to blockIndex2).map(blockIndex =>{
      blockIndex -> getElements().toIndexedSeq(blockIndex)
    }): _*)
  }

  final def getElements(annotationTypeName: String)(blockIndex: Int, charIndex: Int): IntMap[Element] = {
    getRange(annotationTypeName)(blockIndex, charIndex) match {
      case None =>
        IntMap[Element]()
      case Some((blockBIndex, _, blockLIndex, _)) =>
        getElementsInRange(blockBIndex, blockLIndex)
    }
  }

  final def getTextMapInRange(blockIndex1: Int, charIndex1: Int, blockIndex2: Int, charIndex2: Int): IntMap[String] = {
    getElementsInRange(blockIndex1, blockIndex2).map {
      case (blockIndex, e) if blockIndex == blockIndex1 =>
        blockIndex -> e.getText().drop(charIndex1)
      case (blockIndex, e) if blockIndex == blockIndex2 =>
        blockIndex -> e.getText().take(charIndex2 + 1)
      case (blockIndex, e) => 
        blockIndex -> e.getText()
    }
  }



  final def getTextMap(annotationTypeName: String)(blockIndex: Int, charIndex: Int): IntMap[String] = {
    getRange(annotationTypeName)(blockIndex, charIndex) match {
      case None =>
        IntMap[String]()
      case Some((blockBIndex, charBIndex, blockLIndex, charLIndex)) =>
        getTextMapInRange(
            blockBIndex, 
            charBIndex,
            blockLIndex,
            charLIndex
        )
    }
  }

  final def getAnnotatableIndexPairSet(constraintRange: ConstraintRange): SortedSet[(Int, Int)] = {
    constraintRange match {
      case Single(CharCon) =>
        charBIndexPairSet
      case Single(SegmentCon(annotationTypeName)) =>
        annotationInfoMap(annotationTypeName).bIndexPairSortedSet
      case Range(SegmentCon(annotationTypeName), endCon) =>
        def loop(bIndexPairSortedSetAcc: SortedSet[(Int, Int)], constraint: Constraint): SortedSet[(Int, Int)] = {
          (constraint, endCon) match {
            case (CharCon, SegmentCon(_)) => 
              require(false, "constraintRange's end does not follow from its start")
              SortedSet[(Int, Int)]()
            case (x, y) if (x == y) => 
              bIndexPairSortedSetAcc
            case (SegmentCon(annotationTypeName), _) =>

              val _bIndexPairSortedSetAcc = bIndexPairSortedSetAcc.flatMap(pair => { 
                val (blockIndex, charIndex) = pair
                val segment = getSegment(annotationTypeName)(blockIndex, charIndex)
                segment.keys.flatMap(bI => {
                  segment(bI).keys.map(cI => {
                    bI -> cI
                  })
                })
              })

              val annotationType = annotationInfoMap(annotationTypeName).annotationType
              val _constraint = annotationType.constraintRange match {
                case Single(c) => c
                case Range(_, c) => c
              }

              loop(_bIndexPairSortedSetAcc, _constraint)
          }
        }
        loop(annotationInfoMap(annotationTypeName).bIndexPairSortedSet, SegmentCon(annotationTypeName))
      case _ =>
        require(false, "constraintRange is illformed")
        SortedSet[(Int, Int)]()
    }
  }


  final def annotate(
      nameCharPairSeq: Seq[(String, Char)], 
      constraintRange: ConstraintRange, 
      rule: (Int, Int) => Option[Label]
  ) = {

    val annotatableIndexPairSet = getAnnotatableIndexPairSet(constraintRange)

    val annotationTypeSeq = nameCharPairSeq.map {
      case (name, char) =>
        AnnotationType(name, char, constraintRange)
    }

    val labelTable = annotatableIndexPairSet.foldLeft(IntMap[IntMap[Label]]()) {
      case (tableAcc, (blockIndex, charIndex)) =>
        val labelOp = rule(blockIndex, charIndex)
        labelOp match {
          case Some(label) if tableAcc.contains(blockIndex) =>
            tableAcc + (blockIndex -> (tableAcc(blockIndex) + (charIndex -> label)))
          case Some(label) =>
            tableAcc + (blockIndex -> IntMap(charIndex -> label))
          case None => tableAcc
        }
    }

    val _annotationBlockSeq = annotationBlockSeq.zipWithIndex.map { case (block, blockIndex) => {
      labelTable.get(blockIndex) match {
        case None => block
        case Some(labelMap) =>
          val annotation = AnnotationSpan(labelMap, annotationTypeSeq)
          addAnnotation(annotation, block)
      }
    }}

    val _annotationInfoMap =  {
      val annotationInfoList = annotationTypeSeq.map {
        case _annotationType => 
          val char = _annotationType.c
          val bIndexPairSet = annotatableIndexPairSet.filter {
            case (blockIndex, charIndex) => 
              labelTable.contains(blockIndex) && ({
                val labelMap = labelTable(blockIndex)
                labelMap.contains(charIndex) && ({
                  val label = labelMap(charIndex)
                  label == B(char) || label == U(char)
                })
              })
          }
          _annotationType.name -> AnnotationInfo(_annotationType, bIndexPairSet)
          
      }

      annotationInfoMap ++ annotationInfoList
    }

    new Annotator(
      frozenDom,
      _annotationBlockSeq,
      _annotationInfoMap
    )
    
  }

  private val xmlOutputProcessor = new AbstractXMLOutputProcessor {
    override def write(writer: Writer, str: String) = {
      super.write(
          writer, 
          if (str == null) {
            str
          } else {
            str.replaceAll("&#xA;", "\n").replaceAll("<svg:tspan", "\n<svg:tspan")
          }
      )
    }
  }

  final def write(filePath: String): Annotator = {

    val writableDom = frozenDom.clone()
    getElementsOf(writableDom).zipWithIndex.foreach { case (e, i) => {
      val block = annotationBlockSeq(i)
      e.setAttribute("bio", renderAnnotationBlock(block))
    }}

    //format
    val outputter = new XMLOutputter(Format.getPrettyFormat(), xmlOutputProcessor)

    //write
    val out = new FileOutputStream(filePath)
    outputter.output(writableDom, out)
    this

  }

}
