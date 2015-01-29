package edu.umass.cs.iesl.iesl_pdf_to_text

import java.io.PrintWriter
import java.io.File

object Inspector {
  def main(args: Array[String]): Unit = {

    val input = args(0)
    val outputSvg = args(1)
    val outputTxt = args(2)

    runPdfToSvg(input, outputSvg)
    val tspanSeq = getTspanSeq(mkDom(outputSvg))

    val writer = new PrintWriter(new File(outputTxt))
    tspanSeq.foreach(tspan => {
      writer.println(tspan.getText())
      writer.println()
    })

  }
}
