package annotator

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

import org.jdom2.Content
import org.jdom2.input.SAXBuilder
import org.jdom2.filter.ElementFilter
import org.jdom2.Element

import scala.collection.JavaConversions.iterableAsScalaIterable 

import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jdom2.output.LineSeparator

class BioBlock(text: String, startIndex: Int) {

  val next = startIndex + text.size

  val height = (next - 1).toString.size

  val bottomRuler = "| |"+(startIndex until next).map(_ % 10).mkString("")+"|"
  val topRulerList = (2 to height).map(level => {
    "| |"+(startIndex until next).map(i => {
      if (i == startIndex || (i % 10) == 0){
        val divisor = Math.pow(10,level).toInt
        val digit = (i % divisor)/(divisor/10)
        if (digit == 0 && level == height) " " else digit
      } else " "
    }).mkString("")+"|"
  })


}

object Annotator {

  import Boxes._

  //takes an .svg filepath as only argument
  def main(args: Array[String])() = {

    val filePath = args(0)

    val builder = new SAXBuilder()
    val xml = builder.build(new File(filePath)) 

    //find all the elements to annotate
    val elmBioBlockList = xml.getRootElement().getDescendants(new ElementFilter("tspan"))
      .foldLeft(List[(Element, BioBlock)]())((list, e) => {
        val startIndex = list match {
          case Nil => 0
          case x::xs => x._2.next 
        }

        (e, new BioBlock(e.getText(), startIndex))::list
      })

    //modify
    elmBioBlockList.foreach(pair => {
      val element = pair._1
      val bioBlock = pair._2
      val bio = bioBlock.topRulerList.foldLeft(tbox(bioBlock.bottomRuler))((boxAcc, top) => {
        tbox(top).atop(boxAcc)
      })
      element.setAttribute("bio", "\n" + bio.toString + "\n ")
    })

    //format
    val outputter = new XMLOutputter(Format.getPrettyFormat())
    val modifiedXML = outputter.outputString(xml).replaceAll("&#xA;", "\n")

    //write
    val out = new FileOutputStream("/home/thomas/out.svg")
    val writer = new PrintWriter(out)
    writer.print(modifiedXML)

  }

}

object Boxes {
  import scalaz._
  import Scalaz._
  import Lens._

  //newline char
  val nl = "\n"

  // The basic data type.  A box has a specified size and some sort of
  //   contents.
  case class Box(rows:Int, cols:Int, content: Content) {
    // Paste two boxes together horizontally, using a default (top) alignment.
    def + : Box => Box = beside
    def beside : Box => Box = 
      r => hcat(top) (List(this,r))

    // Paste two boxes together horizontally with a single intervening
    //   column of space, using a default (top) alignment.
    def +| : Box => Box = besideS
    def besideS: Box => Box = 
      r => hcat(top)(List(this, emptyBox(0)(1), r))

    // Paste two boxes together vertically, using a default (left)
    //   alignment.
    def % : Box => Box = atop
    def atop(b: Box): Box = 
      vcat(left)(List(this,b))


    // Paste two boxes together vertically with a single intervening row
    //   of space, using a default (left) alignment.
    def %| : Box => Box = atopS
    def atopS : Box => Box = 
      b => vcat(left)(List(this,emptyBox(1)(0), b))

    override def toString = {
      // TODO: catch exceptions and fallback to safer rendering code
      render(this)
    }

    def lines:List[String] = renderBox(this)

    // def clipLeft(cols:Int): Box =
    def dropCols(cols:Int): Box = 
      linesToBox(this.lines.map(_.drop(cols)))

    def takeCols(cols:Int): Box = 
      linesToBox(this.lines.map(_.take(cols)))
  }


  object Box {
    val rows : Box @> Int = lensu((obj, v) => obj copy (rows = v), _.rows)
    val cols : Lens[Box, Int] = lensu((obj, v) => obj copy (cols = v), _.cols)
    val content : Lens[Box, Content] = lensu((obj, v) => obj copy (content = v), _.content)
  }

  // pad box with an empty indentation box
  def indent(n:Int=4)(b:Box): Box = {
    emptyBox(1)(n) + b
  }

  // Implicit to use bare string literals as boxes.
  implicit def stringToBox(s: String): Box = {
    linesToBox(scala.io.Source.fromString(s).getLines.toList)
  }

  def mstringToList(s: String): List[String] = 
    scala.io.Source.fromString(s).getLines.toList


  // Given a string, split it back into a box
  def unrenderString(s:String): Box = 
    s.split(s"$nl").toList |> linesToBox

  // Given a list of strings, create a box
  def linesToBox(lines: List[String]): Box = {
    vjoin()((lines map (tbox(_))):_*)
  }

  // Data type for specifying the alignment of boxes.
  sealed trait Alignment

  case object AlignFirst extends Alignment
  case object AlignLast extends Alignment
  case object AlignCenter1 extends Alignment
  case object AlignCenter2 extends Alignment

  // Align boxes along their top/bottom/left/right
  def top = AlignFirst
  def bottom = AlignLast
  def left = AlignFirst
  def right = AlignLast

  // Align boxes centered, but biased to the left/top (center1) or 
  //  right/bottom (center2) in the case of unequal parities.
  def center1    = AlignCenter1
  def center2    = AlignCenter2

  // Contents of a box.
  sealed trait Content

  case object Blank extends Content
  case class Text(s:String) extends Content
  case class Row(bs:List[Box]) extends Content
  case class Col(bs:List[Box]) extends Content
  case class SubBox(a1: Alignment, a2: Alignment, b:Box) extends Content
  case class AnnotatedBox(props:Map[String, String], b:Box) extends Content


  // The null box, which has no content and no size.  
  def nullBox = emptyBox(0)(0)

  // @emptyBox r c@ is an empty box with @r@ rows and @c@ columns.
  //   Useful for effecting more fine-grained positioning of other
  //   boxes, by inserting empty boxes of the desired size in between
  //   them.
  def emptyBox: Int => Int => Box = 
    r => c => Box(r, c, Blank)

  // A @1x1@ box containing a single character.
  def char: Char => Box =
    c => Box(1, 1, Text(c.toString))

  // A (@1 x len@) box containing a string of length @len@.
  def tbox: String => Box =
    s => Box(1, s.length, Text(s))


  // Glue a list of boxes together horizontally, with the given alignment.
  def hcat: Alignment => List[Box] => Box =
    a => bs => {
      def h = (0 :: (bs ∘ (_.rows))) max
      def w = (bs ∘ (_.cols)) sum
      val aligned = alignVert(a)(h)
      Box(h, w, Row(bs ∘ aligned))
    }

  // @hsep sep a bs@ lays out @bs@ horizontally with alignment @a@,
  //   with @sep@ amount of space in between each.
  def hsep: Int => Alignment => List[Box] => Box =
    sep => a => bs => punctuateH(a)(emptyBox(0)(sep))(bs)


  // Glue a list of boxes together vertically, with the given alignment.
  def vcat: Alignment => List[Box] => Box =
    a => bs => {
      def h = (bs ∘ (_.rows)).sum
      def w = (0 :: (bs ∘ (_.cols))) max
      val aligned = alignHoriz(a)(w)
      Box(h, w, Col(bs ∘ aligned))
    }



  // @vsep sep a bs@ lays out @bs@ vertically with alignment @a@,
  //   with @sep@ amount of space in between each.
  def vsep: Int => Alignment => List[Box] => Box =
    sep => a => bs => punctuateV(a)(emptyBox(sep)(0))(bs)


  // @punctuateH a p bs@ horizontally lays out the boxes @bs@ with a
  //   copy of @p@ interspersed between each.
  def punctuateH: Alignment => Box => List[Box] => Box =
    a => p => bs => hcat(a)(bs intersperse p)


  // A vertical version of 'punctuateH'.
  def punctuateV: Alignment => Box => List[Box] => Box =
    a => p => bs => vcat(a)(bs intersperse p)

  def vjoin(a:Alignment=left, sep:Box=nullBox)(bs:Box*): Box =
    vcat(a)(bs.toList intersperse sep)

  def hjoin(a:Alignment=top, sep:Box=nullBox)(bs:Box*): Box =
    hcat(a)(bs.toList intersperse sep)

  def vjoinList(a:Alignment=left, sep:Box=nullBox)(bs:Seq[Box]): Box =
    vcat(a)(bs.toList intersperse sep)

  def hjoinList(a:Alignment=top, sep:Box=nullBox)(bs:Seq[Box]): Box =
    hcat(a)(bs.toList intersperse sep)


  //------------------------------------------------------------------------------
  //  Alignment  -----------------------------------------------------------------
  //------------------------------------------------------------------------------

  // @alignHoriz algn n bx@ creates a box of width @n@, with the
  //   contents and height of @bx@, horizontally aligned according to
  //   @algn@.
  def alignHoriz: Alignment => Int => Box => Box = 
    a => c => b => {
      Box(b.rows, c, SubBox(a, AlignFirst, b))
    }

  // alignVert creates a box of height n, with the contents and width of bx,
  // vertically aligned according to algn
  def alignVert: Alignment => Int => Box => Box = 
    a => r => b => 
      Box(r, (b.cols), SubBox(AlignFirst, a, b))


  // @align ah av r c bx@ creates an @r@ x @c@ box with the contents
  //   of @bx@, aligned horizontally according to @ah@ and vertically
  //   according to @av@.
  def align : (Alignment, Alignment, Int, Int, Box) => Box =
    (ah, av, r, c, bx) => Box(r, c, SubBox(ah, av, bx))

  // Move a box \"up\" by putting it in a larger box with extra rows,
  //   aligned to the top.  See the disclaimer for 'moveLeft'.
  def moveUp : Int => Box => Box = 
    n => b => alignVert(top)(b.rows + n)(b)


  // Move a box down by putting it in a larger box with extra rows,
  //   aligned to the bottom.  See the disclaimer for 'moveLeft'.
  def moveDown : Int => Box => Box = 
    n => b => alignVert(bottom)(b.rows + n)(b)

  // Move a box left by putting it in a larger box with extra columns,
  //   aligned left.  Note that the name of this function is
  //   something of a white lie, as this will only result in the box
  //   being moved left by the specified amount if it is already in a
  //   larger right-aligned context.
  def moveLeft : Int => Box => Box = 
    n => b => alignHoriz(left)(b.cols + n)(b)


  // Move a box right by putting it in a larger box with extra
  //   columns, aligned right.  See the disclaimer for 'moveLeft'.
  def moveRight : Int => Box => Box = 
    n => b => alignHoriz(right)(b.cols + n)(b)


  //------------------------------------------------------------------------------
  //  Implementation  ------------------------------------------------------------
  //------------------------------------------------------------------------------

  // Render a 'Box' as a String, suitable for writing to the screen or
  //   a file.
  def render : Box => String = 
    b => renderBox(b) |> (_.mkString(s"$nl")) 


  /** take n copies from list, padding end with A if necessary */
  def takePad[A](a:A, n:Int): List[A] => List[A] = { aas => 
    val pad = if (n <= aas.length) 0 else n - aas.length
    aas.take(n) ::: Stream.continually(a).take(pad).toList
  }

  // takePA  is like 'takeP', but with alignment.  That is, we
  //   imagine a copy of `xs` extended infinitely on both sides with
  //   copies of `a`, and a window of size `n` placed so that `xs` has
  //   the specified alignment within the window; `takePA algn a n xs`
  //   returns the contents of this window.
  def takePadAlign[A](align:Alignment, pad:A, n:Int): List[A] => List[A] = 
    as => {
      def numFwd(_a:Alignment, i:Int): Int = _a match {
        case AlignFirst    => i
        case AlignLast     => 0
        case AlignCenter1  => i / 2
        case AlignCenter2  => (i+1) / 2
      }

      def numRev(_a:Alignment, i:Int): Int = _a match {
        case AlignFirst    => 0
        case AlignLast     => i
        case AlignCenter1  => (i+1) / 2
        case AlignCenter2  => i / 2
      }

      def split: List[A] => (List[A], List[A]) = 
        as => ((_:List[A]) reverse).first apply as.splitAt(numRev(align, as.length)) 

      def padding = (takePad(pad, numRev(align, n))  *** takePad(pad, numFwd(align,n)))

      padding apply split(as) fold (_.reverse ++ _)
    }


  // Generate a string of spaces.
  def blanks : Int => String = 
    n => " " * n


  def merge(sss: List[List[String]]): List[String] = {
    (sss foldLeft List[String]()) { case (acc, ss) =>
        acc.zipAll(ss, "", "") map {case (s1, s2) => s1+s2}
    }
  }


  // Render a box as a list of lines.
  def renderBox(box: Box): List[String] = box match {
    case Box(r, c, Blank)             => resizeBox(r, c, List(""))
    case Box(r, c, Text(t))           => resizeBox(r, c, List(t))
    case Box(r, c, Col(bs))           => (bs >>= renderBoxWithCols(c)) |> (resizeBox(r, c, _))
    case Box(r, c, SubBox(ha, va, b)) => resizeBoxAligned(r, c, ha, va)(renderBox(b))
    case Box(r, c, Row(bs))           => {
      bs ∘ renderBoxWithRows(r) |> merge |> (resizeBox(r, c, _))
    }
    case Box(r, c, AnnotatedBox(props, b))  => {
      renderBox(b)
    }
  }


  def fmtsll(sss: List[List[String]]) = sss.mkString("[$nl  ", s"$nl  ", s"$nl]")
  def fmtsl(ss: List[String]) = ss.mkString("[$nl  ", s"$nl  ", s"$nl]")

  // Render a box as a list of lines, using a given number of rows.
  def renderBoxWithRows : Int => Box => List[String] = 
    r => b => renderBox (Box.rows.set(b, r))

  // Render a box as a list of lines, using a given number of columns.
  def renderBoxWithCols : Int => Box => List[String] = 
    c => b => renderBox (Box.cols.set(b, c))

  // Resize a rendered list of lines.
  def resizeBox : (Int, Int, List[String]) => List[String] =
    (r, c, ss) => {
      val taker = takePad(" "*c, r)
      val takec = ss map (s => (takePad(' ', c)(s.toList)).mkString(""))
      taker(takec)
    }

  // Resize a rendered list of lines, using given alignments.
  def resizeBoxAligned(r: Int, c: Int, ha: Alignment, va : Alignment): List[String] => List[String] = {
    ss => takePadAlign(va, blanks(c), r){
      (ss.map (_.toList)) ∘ (takePadAlign(ha, ' ', c)) ∘ (_.mkString(""))
    }
  }

  // A convenience function for rendering a box to stdout.
  def printBox : Box => Unit = 
    box => println(render(box))


  def repeat(b:Box): Stream[Box] = {
    Stream.continually(b)
  }


  def borderLR(c:String)(b:Box): Box = {
    val b0 = vjoin()( repeat(c).take(b.rows).toList:_* )
    tbox("+") % b0 % tbox("+")
  }


  def borderTB(c:String)(b:Box): Box = {
    hjoin()( repeat(c).take(b.cols).toList:_* )
  }

  def border(b:Box): Box = {
    val lr = borderLR("|")(b)
    val tb = borderTB("-")(b)

    lr + (tb % b % tb) + lr
  }


  def padLine(lc:String, rc:String, fill:String, space:String=" ")(l:String): String = {
    val lpad = l.takeWhile(_==' ')
    val rpad = l.reverse.takeWhile(_==' ')
    val left =  lc + fill*lpad.size + space
    val right = space + fill*rpad.size + rc
    left + l.trim + right
  }

  def borderInlineH(b:Box): Box = {
    if (b.rows==0) b
    else if (b.rows==1)
      tbox("[") + b + tbox("]")
    else {
      val lines:List[String] = renderBox(b)

      linesToBox(
        padLine("┌", "┐", "─")(lines.head) :: 
          lines.drop(1).take(lines.length-2).map(
            str => "│ "+str+" │"
          ) ++ List(padLine("└", "┘", "─")(lines.last)))
    }
  }

  def borderLeftRight(l:String, r:String)(b:Box): Box = {
    linesToBox(
      renderBox(b).map(l+_+r)
    )
  }


  def borderInlineTop(b:Box): Box = {
    if (b.rows==0) b
    else if (b.rows==1)
      tbox("[") + b + tbox("]")
    else {
      val lines:List[String] = renderBox(b)

      linesToBox(
        padLine("┌", "┐", "─")(lines.head) :: 
          lines.drop(1).take(lines.length-1).map(
            str => "│ "+str+" │"
          ) ++ List(padLine("└", "┘", "─", "─")("─"*lines.last.length)))
    }
  }


  //trait BoxChars {
  //  def hline: Char
  //  def vline: Char
  //  def ulCorner: Char
  //  def urCorner: Char
  //  def llCorner: Char
  //  def lrCorner: Char
  //  def lbracket: Char
  //  def rbracket: Char
  //}


  //------------------------------------------------------------------------------
  //  Paragraph flowing  ---------------------------------------------------------
  //------------------------------------------------------------------------------

  // @para algn w t@ is a box of width @w@, containing text @t@,
  //   aligned according to @algn@, flowed to fit within the given
  //   width.
  def para: Alignment => Int => String => Box =
    a => n => t =>
  flow(n)(t) |> (ss => mkParaBox(a, ss.length, ss))


  // @columns w h t@ is a list of boxes, each of width @w@ and height
  //   at most @h@, containing text @t@ flowed into as many columns as
  //   necessary.
  def columns : (Alignment, Int, Int, String) => List[Box] =
    (a, w, h, t) =>  flow(w)(t) ∘ (_.grouped(h).toList) ∘ (mkParaBox(a, h, _))



  // makes a box of height n with the text ss
  //   aligned according to a
  def mkParaBox(a:Alignment, n:Int, ss:List[String]): Box =
    alignVert(top)(n)(vcat(a)(ss.map(stringToBox(_))))
  
  
  def words(s:String): List[String] = {
    val wordSplit = """\s+""".r
    (for {
      l <- scala.io.Source.fromString(s).getLines
      w <- wordSplit.split(l)
    } yield {
      w.trim
    }).toList
      // (s.split(" ") map (_.trim)).toList
  }

  def unwords(ws:List[String]) = ws.mkString(" ")
  
  // Flow the given text into the given width.
  def flow : Int => String => List[String] =
    n => t => {
      val wrds = words(t) ∘ mkWord
      val para = wrds.foldl (emptyPara(n)) { addWordP }
      para |> getLines |> (_.map(_.take(n)))
    }
  
  sealed trait ParaContent

  case class Para(paraWidth : Int, paraContent : ParaContent)


  val paraWidth: Lens[Para, Int] = lensu((obj, v) => obj copy (paraWidth = v), _.paraWidth)
  val paraContent: Lens[Para, ParaContent] = lensu((obj, v) => obj copy (paraContent = v), _.paraContent)
  
  case class Block(fullLines : List[Line], lastLine  : Line) extends ParaContent
  val fullLines: Lens[Block, List[Line]] = lensu((obj, v) => obj copy (fullLines = v), _.fullLines)
  val lastLine: Lens[Block, Line] = lensu((obj, v) => obj copy (lastLine = v), _.lastLine)
  
  def emptyPara(pw: Int) : Para =
    Para(pw, (Block(Nil, (Line(0, Nil)))))

  def getLines : Para => List[String] =
    p => {
      def process =  (l:List[Line]) => l.reverse ∘ Line.getWords ∘ (_.map(Word.getWord)) ∘ (_.reverse) ∘ unwords

      p match {
        case Para(_, (Block(ls, l))) =>
          if (l.len == 0) process(ls)
          else            process(l::ls)
      }
    }

  case class Line(len: Int, words: List[Word])
  object Line {
    val lenL: Lens[Line, Int] = lensu((obj, v) => obj copy (len = v), _.len)
    val wordsL: Lens[Line, List[Word]] = lensu((obj, v) => obj copy (words = v), _.words)
    val getLen = lenL.get(_)
    val getWords = wordsL.get(_)
  }

  //
  def mkLine : List[Word] => Line =
    ws => Line((ws ∘ Word.getLen).sum + ws.length - 1, ws)

  def startLine : Word => Line =
    w => mkLine(w :: Nil)


  case class Word(len:Int, word:String)
  object Word {
    val lenL: Lens[Word, Int] = lensu((obj, v) => obj copy (len = v), _.len)
    val wordL: Lens[Word, String] = lensu((obj, v) => obj copy (word = v), _.word)
    val getLen = lenL.get(_)
    val getWord = wordL.get(_)
  }

  def mkWord : String => Word =
    w => Word(w.length, w)

  def addWordP : Para => Word => Para =
    p => w => {
      p match {
        case Para(pw, (Block(fl,l))) =>
          if (wordFits(pw,w,l))
            Para(pw, Block(fl, addWordL(w, l)))
          else
            Para(pw, Block((l::fl), startLine(w)))
      }
    }


  def addWordL : (Word, Line) => Line =
    (w, l) => l match {
      case Line(len, ws) => Line((len + w.len + 1), (w::ws))
    }


  def wordFits : (Int, Word, Line) => Boolean =
    (pw, w, l) =>
  l.len == 0 || l.len + w.len + 1 <= pw

}


object App extends App {
  import Boxes._


  def boxesTest1() {

    val iconicCreator = vcat(right)(List(tbox("creator"),  tbox("de1cecf8-88d3-4580-a15c-6f207627860c"), tbox("AdamIESL Sau{_id: ...}")))
    val iconicTarget = vcat(right)(List(tbox("target"),  tbox("de1cecf8-88d3-4580-a15c-6f207627860c"), tbox("David S{_id: ...}"), tbox("href=...")))
    val sample = hsep(3)(top)(List(iconicTarget, iconicCreator))

    // val sample = text("forward") +| text("f1fb306c-0962-4ba9-acb1-a2dc761e04e9") +| text("creator: de1cecf8-88d3-4580-a15c-6f207627860c") atop text("AdamIESL Sau{_id: ...}")

    println("========")
    printBox(iconicCreator)
    println("========")
    printBox(iconicTarget)
    println("========")
    printBox(sample)
    println("========")
  }


  /// val hline = (c:String) => (n:Int) => (hjoin()( (takePadAlign(left, text(c), n)(List())):_* ))
  // val vline = (c:String) => (n:Int) => (vjoin()( (takePadAlign(center1, text(c), n)(List())):_* ))
  

  def sampleText1 = vjoin(center2)(
    tbox("Lorem ipsum dolor sit amet"),
    tbox("Anyconsectetur adipisicing elit, sed do eiusmod tempor"),
    tbox("incididunt ut labore et dolore magna "),
    tbox("aliqua. Ut enim ad minim veniam, ")
  )
  def sampleText2 = vjoin(center1)(
    tbox("Lorem ipsum dolor sit amet"),
    tbox("Anyconsectetur adipisicing elit, sed do eiusmod tempor"),
    tbox("aliqua. Ut enim ad minim veniam, "),
    tbox("incididunt ut labore et dolore magna "),
    tbox("aliqua. Ut enim ad minim veniam, ")
  )

  def rawText = """|Lorem ipsum dolor sit amet
                   |Anyconsectetur adipisicing elit, sed do eiusmod tempor
                   |aliqua. Ut enim ad minim veniam, 
                   |incididunt ut labore et dolore magna 
                   |aliqua. Ut enim ad minim veniam
                   |""".stripMargin
                   
                   
  def sampleText3 = vjoin()(
    tbox("Lorem ipsum dolor sit amet")
  )

  def sampleBox1 = hjoin(right)(sampleText1, "  <-|||->  ", sampleText2)

  def sampleBox3 = vjoin(right)(sampleText1, "  <-|||-> ", sampleText2)

  def sampleBox2 = vjoin(center1)(hjoin(center1)(sampleText1, "  <-|||->  ", sampleText2), sampleText3)


  override def main(args: Array[String]) {

    val flowed = para(left)(20)(rawText)
    println(borderInlineH(
      "Inline-header label" atop flowed
    ))

    println(s"$nl$nl")

    println(borderInlineTop(
      "Inline-header top header" atop sampleBox1
    ))

    println(s"$nl$nl")

    println(border(
      "simple border" atop sampleBox2
    ))


    println(s"$nl$nl")

    println(borderLeftRight("--> ", " <--")(
      "Left/right border" atop sampleBox3
    ))
    
    println(s"$nl$nl")

    //boxesTest1()
  }
}
