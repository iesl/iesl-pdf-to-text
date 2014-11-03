package annotator

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

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

  case class Block(startIndex: Int, nextIndex: Int, annotations: List[Annotation])

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

    "\n" + bb.annotations.map(renderAnnotation(_, (next - bb.startIndex))).mkString("\n") + "\n" + ruler + "\n "
  }


  private def addAnnotation(anno: Annotation, bb: Block) = { 
    //require(anno.labelMap.lastKey < bb.nextIndex)
    bb.copy(annotations = anno +: bb.annotations)
  }

  private def elementsOf(dom: Document) = dom.getRootElement().getDescendants(new ElementFilter("tspan")).toIterable

}

import Annotator._
class Annotator(private val dom: Document, val bbSeq: IndexedSeq[Block], val annoAtomIndexMap: Map[AnnoType, IntMap[List[Int]]]) {

  def this(dom: Document) = this(
    dom,
    elementsOf(dom).foldLeft(IndexedSeq[Block]())( (seqAcc, e) => {
      val startIndex = if (seqAcc.isEmpty) 0 else seqAcc.last.nextIndex
      val nextIndex = startIndex + e.getText().size
      seqAcc :+ Block(startIndex, nextIndex, List())
    } ),
    HashMap()
  ) 

  private val frozenDom = dom.clone()

  def elements() = elementsOf(frozenDom.clone())

  final def annotateBlock(newAnnoType: AnnoType, rule: Int => Option[Label]): Annotator = {

    val startIndexMap = IntMap(bbSeq.zipWithIndex.flatMap { 
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
      annoAtomIndexMap + (newAnnoType -> startIndexMap)
    )
  }

  final def annotateChar(newAnnoType: AnnoType, rule: (Int, Int) => Option[Label]): Annotator = {

    val es = elements().toIndexedSeq

    val startIndexMap = IntMap(bbSeq.zipWithIndex.flatMap { 
      case (block, i) => 
        (0 until es(i).getText().size).flatMap(charIndex => {
          rule(i, charIndex) match {
            case Some(label) if(label == B || label == U) =>
              Some(charIndex)
            case _ => None
          }
        }).toList match {
          case Nil => None
          case xs => Some(i -> xs)
        }
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
      annoAtomIndexMap + (newAnnoType -> startIndexMap)
    )
  }

  final def annotateAnnoType(annoType: AnnoType, newAnnoType: AnnoType, rule: (Int, Int) => Option[Label]): Annotator = {

    val es = elements().toIndexedSeq

    val startIndexMap = annoAtomIndexMap(annoType).flatMap {
      case (blockIndex, charIndexList) =>
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

    new Annotator(
      frozenDom,
      bbSeq.zipWithIndex.map { case (block, blockIndex) => {
        annoAtomIndexMap(annoType).get(blockIndex) match {
          case None => block
          case Some(charIndexList) =>
            val labelMap = IntMap(charIndexList.flatMap(charIndex => {
              rule(blockIndex, charIndex).map((charIndex -> _))
            }): _*)
            val annotation = Annotation(labelMap, newAnnoType, AnnoTypeCon(annoType))
            addAnnotation(annotation, block)
        }
      }},
      annoAtomIndexMap + (newAnnoType -> startIndexMap)
    )

  }


  final def write(): Annotator = {

    val writableDom = frozenDom.clone()
    elementsOf(writableDom).zipWithIndex.foreach { case (e, i) => {
      val block = bbSeq(i)
      e.setAttribute("bio", renderBlock(block))
    }}

    //format
    val outputter = new XMLOutputter(Format.getPrettyFormat())
    val modifiedXML = outputter.outputString(writableDom).replaceAll("&#xA;", "\n").replaceAll("<svg:tspan", "\n<svg:tspan")

    //write
    val out = new FileOutputStream("/home/thomas/out.svg")
    val writer = new PrintWriter(out)
    writer.print(modifiedXML)
    this

  }

}

