package annotator

import java.io.File
import org.jdom2.Content
import org.jdom2.util.IteratorIterable
import scala.collection.immutable.Queue
import scala.collection.JavaConversions.iterableAsScalaIterable 

import scala.collection.immutable.IntMap
import org.jdom2.input.SAXBuilder
import org.jdom2.filter.ElementFilter
import org.jdom2.Element
import org.jdom2.Document
import org.jdom2.util.IteratorIterable

object ReferencePartAnnotator {
  import Annotator._

  def main(args: Array[String]): Unit = {
    val filePath = args(0)
    val builder = new SAXBuilder()
    val dom = builder.build(new File(filePath)) 
    val annotator = LineAnnotator.addLineAnnotation(TokenAnnotator.addAnnotation(new Annotator(dom)))
    annotator.write("/home/thomas/out.svg")

    ///////////////CITATION STUFF
    import bibie._  
    import cc.factorie.app.nlp.Document
    import cc.factorie.app.nlp.segment.DeterministicTokenizer
    import cc.factorie.app.nlp.Sentence

    val trainer = TestCitationModel.loadModel(
      "file:///home/thomas/iesl/pdf.extractor.js/citationCRF.factorie",
      "file:///home/thomas/iesl/bibie/src/main/resources/lexicons"
    )

    val lineBIndexPairSet = annotator.getAnnotatableIndexPairSet(Single(SegmentCon("line")))

    val docAndPairIndexSeqSet = {
      val pairs = lineBIndexPairSet.toSeq.map {
        case (blockIndex, charIndex) =>
          val textMap = annotator.getTextMap("line")(blockIndex, charIndex)
          val pairIndexSeq = textMap.toIndexedSeq.flatMap {
            case (_blockIndex, text) =>
              val _charIndex = if (_blockIndex == blockIndex) charIndex else 0
              (0 until text.size).map(i => _blockIndex -> (_charIndex + i))
          }

          val doc = {
            val text = textMap.values.mkString("")
            val d = new Document(text)
            DeterministicTokenizer.process(d)
            new Sentence(d.asSection, 0, d.tokens.size)
            d.tokens.foreach(t => {
              t.attr += new CitationLabel("", t)
            })
            d
          }

          (doc -> pairIndexSeq)
      }
      TestCitationModel.process(pairs.map(_._1).filter(_.tokens.size > 1), trainer, false)
      pairs
      
    }


    val typeLabelMapMap = docAndPairIndexSeqSet.flatMap {
      case (doc, pairIndexSeq) =>
        doc.tokens.map(token => {
          val labelTypeStringList = token.attr[CitationLabel].categoryValue.split(":")
          val pairIndex = pairIndexSeq(token.stringStart)
          val typeLabelMap = labelTypeStringList.map(labelTypeString => {
            val labelString = labelTypeString.take(1)
            val typeString = labelTypeString.drop(2)
            println(labelString)
            val label: Label = (labelString match {
              case "B" => B(typeString.toCharArray()(0))
              case "I" => I
              case "O" => O
              case l => 
                println("what's this? :" + l + ":" )
                println("typeString :" + typeString )
                O
            })
            typeString -> label
          }).toMap

          pairIndex -> typeLabelMap
        })
    } toMap

    val typeStrings = List("authors", "person", "person-last", "person-first", "date", "year", "title", "venue", "journal")

    typeStrings.foldLeft(annotator) {
      case (anno, typeString) =>
        val c = typeString.toCharArray()(0)
        anno.annotate(List(typeString -> c), Single(SegmentCon("token")), (blockIndex, charIndex) => {
          typeLabelMapMap.get(blockIndex -> charIndex).flatMap(_.get(typeString))
        })
    } write("/home/thomas/out.svg")


  }

}
