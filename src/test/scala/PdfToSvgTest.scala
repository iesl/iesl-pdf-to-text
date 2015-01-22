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

  val resourceInputPath = {
    getClass.getResource("/input").getPath() + "/"
  }

  def resourceOutputPath = {
    getClass.getResource("/svg-output").getPath() + "/"
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

  def mkOutputSvgDoms(inputPath: String): (Document, Document) = {
    val inputName = inputPath.stripPrefix(resourceInputPath)
    val expectedSvgPath = resourceOutputPath + (expected(inputName))
    val actualSvgPath = resourceOutputPath + (actual(inputName))
    assert(0 == Seq("bin/run.js", "--svg", "-i", inputPath, "-o", actualSvgPath).!)
    (mkDom(expectedSvgPath), mkDom(actualSvgPath)) 
  }

  Seq("find", resourceInputPath, "-type", "f").lines.foreach(inputPath => {
    val (expectedDom, actualDom) = mkOutputSvgDoms(inputPath)
    val (expectedTspanSeq, actualTspanSeq) = (getTspanSeq(expectedDom), getTspanSeq(actualDom))

    val hasSpacesInMiddle = (tspan: Element) => (tspan.getText().indexOf(" ") > 0)

    if (expectedTspanSeq.indexWhere(hasSpacesInMiddle) >= 0) {
      ("bin/run.js --svg with input: " + inputPath.stripPrefix(resourceInputPath)) should "produce svg with spaces between words of the same tspan" in {
        expectedTspanSeq.zipWithIndex.foreach(p => {
          val (tspan, i) = p
          if (hasSpacesInMiddle(tspan)) {

            assertResult(tspan.getText()) { 
              actualTspanSeq(i).getText() 
            }

          }
        })
      }
    }

    val startsWithSpaceAndOnSameLine = (pair: (Element, Int)) => {
      val (tspan, i) = pair
      tspan.getText().startsWith(" ") && 
      i > 0 && 
      expectedTspanSeq(i - 1).getAttribute("y").getValue() == tspan.getAttribute("y").getValue()
    }

    if (expectedTspanSeq.zipWithIndex.indexWhere(startsWithSpaceAndOnSameLine) > 0) {
      it should "produce svg with tspan that has space at beginning of text and that is on same line as its preceding tspan" in {
        expectedTspanSeq.zipWithIndex.foreach(p => {
          val (tspan, i) = p
          if (startsWithSpaceAndOnSameLine(tspan -> i)) {
            
            assertResult(tspan.getText()) { 
              actualTspanSeq(i).getText() 
            }
            assertResult(tspan.getAttribute("y").getValue()) { 
              actualTspanSeq(i).getAttribute("y").getValue()
            }
            assertResult(expectedTspanSeq(i - 1).getAttribute("y").getValue()) { 
              actualTspanSeq(i- 1).getAttribute("y").getValue()
            }

          }
        })
      }
    }

    val startsWithoutSpaceAndOnSameLine = (pair: (Element, Int)) => {
      val (tspan, i) = pair
      tspan.getText().startsWith(" ") && 
      i > 0 && 
      expectedTspanSeq(i - 1).getAttribute("y").getValue() == tspan.getAttribute("y").getValue()
    }


    if (expectedTspanSeq.zipWithIndex.indexWhere(startsWithoutSpaceAndOnSameLine) > 0) {
      it should "produce svg with tspan that does not have space at beginning and that is on same line as its preceding tspan" in {
        expectedTspanSeq.zipWithIndex.foreach(p => {
          val (tspan, i) = p
          if ((startsWithoutSpaceAndOnSameLine(tspan -> i))) {

            assertResult(tspan.getText()) { 
              actualTspanSeq(i).getText() 
            }
            assertResult(tspan.getAttribute("y").getValue()) { 
              actualTspanSeq(i).getAttribute("y").getValue()
            }
            assertResult(expectedTspanSeq(i - 1).getAttribute("y").getValue()) { 
              actualTspanSeq(i- 1).getAttribute("y").getValue()
            }

          }
        })
      }
    }

    val hasEndX = (tspan: Element) => tspan.getAttribute("endX") != null

    if (expectedTspanSeq.indexWhere(hasEndX) >= 0) {
      it should "produce an svg containing tspans with attribute endX" in {
        expectedTspanSeq.zipWithIndex.foreach(p => {
          val (tspan, i) = p
          if (hasEndX(tspan)) {

            assertResult(true) { 
              actualTspanSeq(i).getAttribute("endX") != null 
            }
            assertResult(tspan.getAttribute("endX").getValue()) { 
              actualTspanSeq(i).getAttribute("endX").getValue()
            }

          }
        })
      }
    }


  })


}
