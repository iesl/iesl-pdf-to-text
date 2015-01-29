package edu.umass.cs.iesl.iesl_pdf_to_text

import java.io.PrintWriter

object Inspector {
  def main(args: Array[String]): Unit = {

    val input = args(0)
    val output = args(1)

    runPdfToSvg(input, output)
    val tspanSeq = getTspanSeq(mkDom(output))

    val writer = new PrintWriter(output)
    tspanSeq.foreach(tspan => {
      writer.println(tspan.getText())
      writer.println()
    })

  }
}
