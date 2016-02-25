package edu.umass.cs.iesl

import org.jdom2.filter.ElementFilter
import org.jdom2.Element
import org.jdom2.Document
import org.jdom2.util.IteratorIterable
import org.jdom2.input.SAXBuilder
import java.io.File
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.sys.process._

package object iesl_pdf_to_text {


  def build(builder: SAXBuilder)(fullPath: String) = {
    builder.build(new File(fullPath))
  }

  val mkDom = build(new SAXBuilder()) _

  def getTspanSeq(dom: Document): Seq[Element] = {
    dom.getRootElement().getDescendants(new ElementFilter("tspan")).toIterable.filter(e => {
      e.getText().size > 0
    }).toIndexedSeq
  }

  def runPdfToSvg(inputPath: String, outputPath: String): Unit = {
    assert(0 == Seq("bin/run.js", "--svg", "-i", inputPath, "-o", outputPath).!)
  }


}
