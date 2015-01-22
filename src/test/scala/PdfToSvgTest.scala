package edu.umass.cs.iesl.iesl_pdf_to_text

import java.io.File

import scala.collection.JavaConversions.iterableAsScalaIterable 

import scala.collection.immutable.IntMap
import scala.collection.immutable.Set
import scala.collection.immutable.Map
import scala.collection.immutable.ListMap

import org.jdom2.filter.ElementFilter
import org.jdom2.Element
import org.jdom2.Document
import org.jdom2.util.IteratorIterable
import org.jdom2.input.SAXBuilder

import org.scalatest._

import scala.sys.process._

class PdfToSvgTest extends FlatSpec {



  def expected(str: String) = {
    str + ".expected.svg"
  }

  def actual(str: String) = {
    str + ".actual.svg"
  }

  def resourceInputPath(str: String) = {
    getClass.getResource("/input/" + str).getPath()
  }

  def resourceOutputPath(str: String) = {
    getClass.getResource("/svg-output").getPath() + "/" + str
  }

  def build(builder: SAXBuilder)(fullPath: String) = {
    builder.build(new File(fullPath))
  }

  val mkDom = build(new SAXBuilder()) _

  private def getTspanSeq(dom: Document): Seq[Element] = {
    dom.getRootElement().getDescendants(new ElementFilter("tspan")).toIterable.filter(e => {
      e.getText().size > 0
    }).toIndexedSeq
  }

  def mkOutputSvgDoms(inputName: String): (Document, Document) = {
    val expectedSvgPath = resourceOutputPath(expected(reidelPaper))
    val actualSvgPath = resourceOutputPath(actual(inputName))
    assert(0 == Seq("bin/run.js", "--svg", "-i", resourceInputPath(inputName), "-o", actualSvgPath).!)
    (mkDom(expectedSvgPath), mkDom(actualSvgPath)) 
  }

  val reidelPaper = "1301.4293.pdf"
  val (reidelExpectedDom, reidelActualDom) = mkOutputSvgDoms(reidelPaper)
  val (reidelExpectedTspanSeq, reidelActualTspanSeq) = (getTspanSeq(reidelExpectedDom), getTspanSeq(reidelActualDom))



  val hasSpacesInMiddle = (tspan: Element) => (tspan.getText().indexOf(" ") > 0)

  if (reidelExpectedTspanSeq.indexWhere(hasSpacesInMiddle) >= 0) {
    "bin/run.js --svg" should "produce svg with spaces between words of the same tspan" in {
      reidelExpectedTspanSeq.zipWithIndex.foreach(p => {
        val (tspan, i) = p
        if (hasSpacesInMiddle(tspan)) {

          assertResult(tspan.getText()) { 
            reidelActualTspanSeq(i).getText() 
          }

        }
      })
    }
  }

  def startsWithSpaceAndOnSameLine(expectedTspanSeq: Seq[Element]) = (pair: (Element, Int)) => {
    val (tspan, i) = pair
    tspan.getText().startsWith(" ") && 
    i > 0 && 
    reidelExpectedTspanSeq(i - 1).getAttribute("y").getValue() == tspan.getAttribute("y").getValue()
  }

  if (reidelExpectedTspanSeq.zipWithIndex.indexWhere(startsWithSpaceAndOnSameLine(reidelExpectedTspanSeq)) > 0) {
    it should "produce svg with tspan that has space at beginning of text and that is on same line as its preceding tspan" in {
      reidelExpectedTspanSeq.zipWithIndex.foreach(p => {
        val (tspan, i) = p
        if (startsWithSpaceAndOnSameLine(reidelExpectedTspanSeq)(tspan -> i)) {
          
          assertResult(tspan.getText()) { 
            reidelActualTspanSeq(i).getText() 
          }
          assertResult(tspan.getAttribute("y").getValue()) { 
            reidelActualTspanSeq(i).getAttribute("y").getValue()
          }
          assertResult(reidelExpectedTspanSeq(i - 1).getAttribute("y").getValue()) { 
            reidelActualTspanSeq(i- 1).getAttribute("y").getValue()
          }

        }
      })
    }
  }

  def startsWithoutSpaceAndOnSameLine(expectedTspanSeq: Seq[Element]) = (pair: (Element, Int)) => {
    val (tspan, i) = pair
    tspan.getText().startsWith(" ") && 
    i > 0 && 
    reidelExpectedTspanSeq(i - 1).getAttribute("y").getValue() == tspan.getAttribute("y").getValue()
  }


  if (reidelExpectedTspanSeq.zipWithIndex.indexWhere(startsWithoutSpaceAndOnSameLine(reidelExpectedTspanSeq)) > 0) {
    it should "produce svg with tspan that does not have space at beginning and that is on same line as its preceding tspan" in {
      reidelExpectedTspanSeq.zipWithIndex.foreach(p => {
        val (tspan, i) = p
        if ((startsWithoutSpaceAndOnSameLine(reidelExpectedTspanSeq)(tspan -> i))) {

          assertResult(tspan.getText()) { 
            reidelActualTspanSeq(i).getText() 
          }
          assertResult(tspan.getAttribute("y").getValue()) { 
            reidelActualTspanSeq(i).getAttribute("y").getValue()
          }
          assertResult(reidelExpectedTspanSeq(i - 1).getAttribute("y").getValue()) { 
            reidelActualTspanSeq(i- 1).getAttribute("y").getValue()
          }

        }
      })
    }
  }

  val hasEndX = (tspan: Element) => tspan.getAttribute("endX") != null

  if (reidelExpectedTspanSeq.indexWhere(hasEndX) >= 0) {
    it should "produce an svg containing tspans with attribute endX" in {
      reidelExpectedTspanSeq.zipWithIndex.foreach(p => {
        val (tspan, i) = p
        if (hasEndX(tspan)) {

          assertResult(true) { 
            reidelActualTspanSeq(i).getAttribute("endX") != null 
          }
          assertResult(tspan.getAttribute("endX").getValue()) { 
            reidelActualTspanSeq(i).getAttribute("endX").getValue()
          }

        }
      })
    }
  }













}
