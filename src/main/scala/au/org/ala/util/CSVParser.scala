package au.org.ala.util
import scala.util.matching.Regex
import scala.util.parsing.combinator._
import java.util.regex.Pattern
import scala.util.matching.Regex
import scala.util.parsing.input._
import scala.collection.mutable

object CSVParser extends RegexParsers {
 
  override def skipWhitespace = false

  def whitespace: Parser[String] = """[ \t]*""".r
   
  def csvRecord: Parser[List[String]] = repsep(rawField, ",") ^^ (List() ::: _)
   
  def rawField: Parser[String] = opt(whitespace) ~ field ~ opt(whitespace) ^^ { case a ~ b ~ c => b} 
  
  def field: Parser[String] = quotedField | simpleField 
  
  def simpleField: Parser[String] = """[^\n\r\t ,"]*""".r 

  def quotedField: Parser[String] = "\"" ~ escapedField ~ "\"" ^^ { case "\"" ~ s ~ "\"" => s }
  
  def escapedField: Parser[String] = repsep("""[^"]*""".r, "\"\"") ^^ { x => x mkString("", "\"", "") }

}