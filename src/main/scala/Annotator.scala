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

import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jdom2.output.LineSeparator
import org.jdom2.output.support.AbstractXMLOutputProcessor

object Annotator {


  sealed trait Label
  case object B extends Label
  case object I extends Label
  case object O extends Label
  case object L extends Label
  case object U extends Label

  type Element = org.jdom2.Element
  type ElementFilter = org.jdom2.filter.ElementFilter
  
  case class AnnoType(name: String, c: Char)


  sealed trait Constraint
  case object BlockCon extends Constraint
  case object CharCon extends Constraint
  case class AnnoTypeCon(annoType: AnnoType) extends Constraint

  case class Annotation(
    labelMap: IntMap[Label], 
    annoType: AnnoType,
    constraint: Constraint 
  )

  case class Block(startIndex: Int, nextIndex: Int, annotationMap: Map[AnnoType, Annotation])

  private def renderAnnotation(a: Annotation, length: Int) = {

    val posi = (0 until length).foldLeft("")((stringAcc, i) => {
      stringAcc + (a.labelMap.get(i) match {
        case Some(label) =>  
           label match {
            case B => a.annoType.c.toLower
            case I => '~'
            case O => '-'
            case L => '$' 
            case U => a.annoType.c.toUpper
          }
        case None => ' '
      })
    })

    val constr =  ", constraint: " + (a.constraint match {
      case BlockCon => "block"
      case CharCon => "char"
      case AnnoTypeCon(atype) => atype.name
    })

    val annot = {
      "type: " + "{" + (a.annoType match {
        case AnnoType(name, c) => name + ": " + c
      }) + "}"
    }

    "| |" + posi + "| " + "{" + annot + constr + "}"

  }

  private def renderBlock(bb: Block): String = {
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

    "\n" + bb.annotationMap.values.map(renderAnnotation(_, (next - bb.startIndex))).mkString("\n") + "\n" + ruler + "\n "
  }


  private def addAnnotation(anno: Annotation, bb: Block) = { 
    //require(anno.labelMap.lastKey < bb.nextIndex)
    bb.copy(annotationMap = bb.annotationMap + (anno.annoType -> anno))
  }

  private def getElementsOf(dom: Document) = dom.getRootElement().getDescendants(new ElementFilter("tspan")).toIterable

}

import Annotator._
class Annotator(private val dom: Document, val bbSeq: IndexedSeq[Block], val bIndexTableMap: Map[AnnoType, IntMap[List[Int]]]) {

  def this(dom: Document) = this(
    dom,
    getElementsOf(dom).foldLeft(IndexedSeq[Block]())( (seqAcc, e) => {
      val startIndex = if (seqAcc.isEmpty) 0 else seqAcc.last.nextIndex
      val nextIndex = startIndex + e.getText().size
      seqAcc :+ Block(startIndex, nextIndex, HashMap())
    } ),
    HashMap()
  )

  private val frozenDom = dom.clone()
  final def getElements() = getElementsOf(frozenDom.clone())
  private val frozenElements = getElements().toIndexedSeq


  final def getBIndexList(annoType: AnnoType): List[(Int,Int)] = {
    bIndexTableMap(annoType).toList.flatMap {
      case (blockBIndex, charBIndexList) =>
        charBIndexList.map(charBIndex => {
          (blockBIndex, charBIndex)
        })
    }
  }


  final def getSegment(annoType: AnnoType)(blockIndex: Int, charIndex: Int): IntMap[IntMap[Label]] = {

    val block = bbSeq(blockIndex)
    block.annotationMap.get(annoType) match {
      case None => getSegment(annoType)(blockIndex + 1, 0)
      case Some(annotation) =>
        val labelMap = annotation.labelMap
        labelMap.keys.find(_ >= charIndex) match {
          case None =>
            getSegment(annoType)(blockIndex + 1, 0)
          case Some(_charIndex) =>
            val label = labelMap(_charIndex)
            label match {
              case L =>
                IntMap(blockIndex -> IntMap(_charIndex -> L))
              case U => 
                IntMap(blockIndex -> IntMap(_charIndex -> U))
              case label => 
                val labelTable = getSegment(annoType)(blockIndex, _charIndex + 1)
                labelTable.get(blockIndex) match {
                  case None => 
                    labelTable + (blockIndex -> IntMap(_charIndex -> label))
                  case Some(rowIntMap) => 
                    labelTable + (blockIndex -> (rowIntMap + (_charIndex -> label)))
                }
            }
        }
      }
  }

  final def getElementsInRange(blockIndex1: Int, blockIndex2: Int): IntMap[Element] = {
    IntMap((blockIndex1 to blockIndex2).map(blockIndex =>{
      blockIndex -> frozenElements(blockIndex).clone()
    }): _*)
  }

  final def getElements(annoType: AnnoType)(blockIndex: Int, charIndex: Int): IntMap[Element] = {
    val segment = getSegment(annoType)(blockIndex, charIndex)
    val blockBIndex = segment.firstKey
    val blockLIndex = segment.lastKey
    getElementsInRange(blockBIndex, blockLIndex)
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

  final def getTextMap(annoType: AnnoType)(blockIndex: Int, charIndex: Int): IntMap[String] = {
    val segment = getSegment(annoType)(blockIndex, charIndex)

    val blockBIndex = segment.firstKey
    val charBIndex = segment(blockBIndex).firstKey
    val blockLIndex = segment.lastKey
    val charLIndex = segment(segment.lastKey).lastKey

    getTextMapInRange(
        blockBIndex, 
        charBIndex,
        blockLIndex,
        charLIndex
    )
  }

  


  final def annotateBlock(newAnnoType: AnnoType, rule: Int => Option[Label]): Annotator = {

    val bIndexTable = IntMap(bbSeq.zipWithIndex.flatMap { 
      case (block, i) => 
        rule(i) match {
          case Some(label) if(label == B || label == U) =>
            Some(i -> List(0))
          case _ => None
        }
    }: _*)

    new Annotator(
      frozenDom,
      bbSeq.zipWithIndex.map { case (block, i) => {
        val labelMap = IntMap(rule(i).map((0 -> _)).toList: _ *)
        val annotation = Annotation(labelMap, newAnnoType, BlockCon)
        addAnnotation(annotation, block)
      }},
      bIndexTableMap + (newAnnoType -> bIndexTable)
    )
  }


  private def filterStartIndexes(blockIndex: Int, charIndexList: Iterable[Int], rule: (Int, Int) => Option[Label]) = {
    charIndexList.flatMap(charIndex => {
      rule(blockIndex, charIndex) match {
        case Some(label) if(label == B || label == U) =>
          Some(charIndex)
        case _ => None
      }
    }).toList match {
      case Nil => None
      case xs => Some(blockIndex -> xs)
    }
  }


  final def annotateChar(newAnnoType: AnnoType, rule: (Int, Int) => Option[Label]): Annotator = {

    val es = frozenElements 

    val bIndexTable = IntMap(bbSeq.zipWithIndex.flatMap { 
      case (block, i) => filterStartIndexes(i, (0 until es(i).getText().size), rule)
    }: _*)
    
    new Annotator(
      frozenDom,
      bbSeq.zipWithIndex.map { 
        case (block, i) => 
          val labelMap = IntMap((0 until es(i).getText().size).flatMap {charIndex => {
            rule(i, charIndex).map((charIndex -> _))
          } }: _ *)
          val annotation = Annotation(labelMap, newAnnoType, CharCon)
          addAnnotation(annotation, block)
      },
      bIndexTableMap + (newAnnoType -> bIndexTable)
    )
  }


  final def annotateAnnoType(annoType: AnnoType, newAnnoType: AnnoType, rule: (Int, Int) => Option[Label]): Annotator = {

    val es = frozenElements 

    val bIndexTable = bIndexTableMap(annoType).flatMap {
      case (blockIndex, charIndexList) => filterStartIndexes(blockIndex, charIndexList, rule)
    }

    new Annotator(
      frozenDom,
      bbSeq.zipWithIndex.map { case (block, blockIndex) => {
        bIndexTableMap(annoType).get(blockIndex) match {
          case None => block
          case Some(charIndexList) =>
            val labelMap = IntMap(charIndexList.flatMap(charIndex => {
              rule(blockIndex, charIndex).map((charIndex -> _))
            }): _*)
            val annotation = Annotation(labelMap, newAnnoType, AnnoTypeCon(annoType))
            addAnnotation(annotation, block)
        }
      }},
      bIndexTableMap + (newAnnoType -> bIndexTable)
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
      val block = bbSeq(i)
      e.setAttribute("bio", renderBlock(block))
    }}

    //format
    val outputter = new XMLOutputter(Format.getPrettyFormat(), xmlOutputProcessor)

    //write
    val out = new FileOutputStream(filePath)
    outputter.output(writableDom, out)
    this

  }

}

