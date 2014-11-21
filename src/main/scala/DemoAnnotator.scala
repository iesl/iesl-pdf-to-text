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

object DemoAnnotator {
  import Annotator._

  def main(args: Array[String]): Unit = {
    val filePath = args(0)
    val builder = new SAXBuilder()
    val dom = builder.build(new File(filePath)) 
    val annotator = LineAnnotator.addLineAnnotation(TokenAnnotator.addAnnotation(new Annotator(dom)))
    annotator.write("/home/thomas/out.svg")


//    val table = (annotator.getBIndexList(LineAnnotator.segmentType).map {
//      case (blockIndex, charIndex) =>
//
//        val textMap = annotator.getTextMap(LineAnnotator.segmentType)(blockIndex, charIndex)
//
//        val lineText = textMap.values.mkString("")
//
//
//
//        def getLabel(): Option[Label] = {
//          readLine(s"line: ${lineText}: ") match {
//            case "b" => Some(B)
//            case "i" => Some(I)
//            case "o" => Some(O)
//            case "l" => Some(L)
//            case "u" => Some(U)
//            case "n" => None
//            case _ => {
//              println("Please enter either b, i, o, l, u, or n")
//              getLabel()
//            }
//          }
//        }
//
//        (blockIndex -> charIndex) -> getLabel()
//
//    }).toMap

//    val lineBIndexPairSet = annotator.getAnnotatableIndexPairSet(Single(SegmentCon("line")))
//    lineBIndexPairSet.map {
//      case (blockIndex, charIndex) =>
//        val textMap = annotator.getTextMap("line")(blockIndex, charIndex)
//
//        println("line: " + textMap.values.mkString(""))
//
//
//        //val es = annotator.getElements("line")(blockIndex, charIndex)
//        //es.map {
//        //  case (i, e) =>
//        //    val parent = e.getParent()
//        //    println(parent)
//        //}
//
//
//    }



//    val abc = annotator.annotate(List("aaa" -> 'a', "bbb" -> 'b'), Single(SegmentCon("line")), (blockIndex, charIndex) => {
//        if (blockIndex % 3 == 0) {
//          Some(B('a'))
//        } else if (blockIndex % 3 == 1) {
//          Some(B('b'))
//        } else {
//          None
//        }
//    }).write("/home/thomas/out.svg")
//    val atype = AnnotationType("aaa", 'a', Single(SegmentCon("line")))
//    val btype = AnnotationType("bbb", 'b', Single(SegmentCon("line")))
//
//    abc.annotate(List("name" -> 'n'), Range(SegmentCon("aaa"), CharCon), (blockIndex, charIndex) => {
//        Some(U('n'))
//    }).write("/home/thomas/out.svg")


/*
    import bibie._  
    import cc.factorie.app.nlp.Document
    import cc.factorie.app.nlp.segment.DeterministicTokenizer
    import cc.factorie.app.nlp.Sentence

    val trainer = TestCitationModel.loadModel(
      "file:///home/thomas/iesl/pdf.extractor.js/citationCRF.factorie",
      "file:///home/thomas/iesl/bibie/src/main/resources/lexicons"
    )

    val predocs = List(

      //new Document("""Mohit Bansal, Kevin Gimpel, and Karen Livescu.
      //2014.Tailoring Continuous Word Representations
      //for Dependency Parsing.Association for Computa-
      //tional Linguistics (ACL)."""),

      //new Document("""Yoshua Bengio, R´ejean Ducharme, Pascal Vincent, and
      //Christian Jauvin. 2003.A neural probabilistic lan-
      //guage model.Journal of Machine Learning Re-
      //search (JMLR)."""),

      //new Document("""Peter F. Brown, Peter V. Desouza, Robert L. Mercer,
      //Vincent J. Della Pietra, and Jenifer C. Lai. 1992.
      //Class-based N-gram models of natural language
      //Computational Linguistics."""),

      //new Document("""Ronan Collobert and Jason Weston. 2008.A Uni-
      //ﬁed Architecture for Natural Language Process-
      //ing: Deep Neural Networks with Multitask Learn-
      //ing.International Conference on Machine learning
      //(ICML)."""),

      //new Document("""Paramveer S. Dhillon, Dean Foster, and Lyle Ungar.
      //2011.Multi-View Learning of Word Embeddings via
      //CCA.Advances in Neural Information Processing
      //Systems (NIPS)."""),

      new Document("""John Duchi, Elad Hazan, and Yoram Singer 2011.
      Adaptive sub- gradient methods for online learn-
      ing and stochastic optimization.Journal of Machine
      Learning Research (JMLR).""")

    )

    val docs = predocs.map(d => {
      DeterministicTokenizer.process(d)
      new Sentence(d.asSection, 0, d.tokens.size)
      d.tokens.foreach(t => {
        t.attr += new CitationLabel("", t)
      })
      d
    })

    TestCitationModel.process(docs, trainer, false)

    docs.foreach(d => {
      d.tokens.foreach(t => {
        println(t + "\t" + t.attr[CitationLabel])
      })
      d
    })
    */

  }

}
