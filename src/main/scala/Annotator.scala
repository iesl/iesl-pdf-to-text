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

  type Element = org.jdom2.Element
  type ElementFilter = org.jdom2.filter.ElementFilter
  type AnnoMap = IntMap[Char]
  val AnnoMap = IntMap
  
  case class AnnoType(name: String, c: Char)

  sealed trait AnnoTypeBox
  case class AnnoTypeSingle(at: AnnoType) extends AnnoTypeBox
  case class AnnoTypeGroup(name: String, atList: List[AnnoType]) extends AnnoTypeBox

  case class Annotation(
    annoMap: AnnoMap, 
    annoTypeBox: AnnoTypeBox,
    constraintList: List[AnnoType]
  )

  case class BioBlock(startIndex: Int, nextIndex: Int, annotations: List[Annotation])

  private def renderAnnotation(a: Annotation, length: Int) = {

    val posi = (0 until length).foldLeft("")((stringAcc, i) => {
      a.annoMap.get(i) match {
        case Some(c) => stringAcc + c
        case None => stringAcc + " "
      }
    })

    val constr = 
      if (!a.constraintList.isEmpty) {
        ", constraint: " + a.constraintList.map(_.name).mkString("/")
      } else ""

    val annot = {
      "type: " + "{" + (a.annoTypeBox match {
        case AnnoTypeSingle(annoType) => 
          annoType.name + ": " + annoType.c
        case AnnoTypeGroup(name, list) => 
          name + ": " + "{" + list.map(annoType => annoType.name + ": " + annoType.c).mkString(", ") + "}"
      }) + "}"
    }

    "| |" + posi + "| " + "{" + annot + constr + "}"

  }

  private def renderBioBlock(bb: BioBlock): String = {
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


  private def addAnnotation(anno: Annotation, bb: BioBlock) = { 
    //require(anno.annoMap.lastKey < bb.nextIndex)
    bb.copy(annotations = anno +: bb.annotations)
  }

  private def elementsOf(dom: Document) = dom.getRootElement().getDescendants(new ElementFilter("tspan")).toIterable

}

import Annotator._
class Annotator(private val dom: Document, val bbSeq: IndexedSeq[BioBlock]) {

  def this(dom: Document) = this(
    dom,
    elementsOf(dom).foldLeft(IndexedSeq[BioBlock]())( (seqAcc, e) => {
      val startIndex = if (seqAcc.isEmpty) 0 else seqAcc.last.nextIndex
      val nextIndex = startIndex + e.getText().size
      seqAcc :+ BioBlock(startIndex, nextIndex, List())
    } )
  ) 

  private val frozenDom = dom.clone()

  def elements() = elementsOf(frozenDom.clone())

  //TO DO: change rule to be char token (Int, Int, char) => label-char 
  //and change name to annotateOverChar
  final def annotate(annoTypeBox: AnnoTypeBox, rule: Int => AnnoMap): Annotator = {
    
    new Annotator(
      frozenDom,
      bbSeq.zipWithIndex.map { case (block, i) => {
        val annoMap = rule(i)
        //TO DO: check if annoMap contains the B label 
        //if so, add annoMaps containing begin to end
        //to index of annotype -> spanIndex -> charIndex -> list of annoMaps
        val annotation = Annotation(annoMap, annoTypeBox, List())
        addAnnotation(annotation, block)
      }}
    )
  }

  final def write(): Annotator = {

    val writableDom = frozenDom.clone()
    elementsOf(writableDom).zipWithIndex.foreach { case (e, i) => {
      val block = bbSeq(i)
      e.setAttribute("bio", renderBioBlock(block))
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

