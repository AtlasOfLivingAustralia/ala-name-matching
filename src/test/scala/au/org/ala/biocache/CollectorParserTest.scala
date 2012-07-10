package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Assertions._


@RunWith(classOf[JUnitRunner])
class CollectorParserTest extends FunSuite {

  test("Surname, FirstName combinations"){
    val result = CollectorNameParser.parse("Beauglehole, A.C.")
    expect("Beauglehole, A.C."){result.get}
    expect("Beauglehole, A.C. Atest"){CollectorNameParser.parse("Beauglehole, A.C. Atest").get}
    expect("Beauglehole, A. Atest"){CollectorNameParser.parse("Beauglehole, Atest").get}
    expect("Field, P. Ross"){CollectorNameParser.parse("Field, Ross P.").get}
    expect("Robinson, A.C. Tony"){CollectorNameParser.parse(""""ROBINSON A.C. Tony"""").get}
    expect(List("Graham, K.L. Kate")){CollectorNameParser.parseForList("GRAHAM K.L. Kate").get}
    expect(List("natasha.carter@csiro.au")){CollectorNameParser.parseForList("natasha.carter@csiro.au").get}
    expect(List("Gunness, A.G.")){CollectorNameParser.parseForList("A.G.Gunness et. al.").get}
  }
  
  test("FirstName, Surname"){
    expect("Starr, S. Simon"){CollectorNameParser.parse("Simon Starr").get}
    expect("Starr, S.S. Simon"){CollectorNameParser.parse("Simon S.S Starr").get}
  }
  
  test("Surname initials"){
    expect(List("Wilson, P.J.")){CollectorNameParser.parseForList(""""WILSON P.J. N/A"""").get}
    //expect(List()){CollectorNameParser.parseForList("Eichler, Hj.")}
  }
  
  test("hyphen names"){
    expect(List("Kenny, S.D. Sue","Wallace-Ward, D. Di")){CollectorNameParser.parseForList(""""KENNY S.D. Sue""WALLACE-WARD D. Di"""").get}
    expect(List("Russell-Smith, J.")){CollectorNameParser.parseForList("""Russell-Smith, J.""").get}
    expect(List("Davies, R.J-P. Richard")){CollectorNameParser.parseForList(""""DAVIES R.J-P. Richard"""").get}
  }
  
  test("title test"){
    expect(List("Dittrich")){CollectorNameParser.parseForList("""Dittrich, Lieutenant""").get}
  }
  
  test("preffix surnames"){
    expect(List("van Leeuwen, S.")){CollectorNameParser.parseForList("""van Leeuwen, S.""").get}
    expect(List("van der Leeuwen, S. Simon")){CollectorNameParser.parseForList("""van der Leeuwen, Simon""").get}
    expect(List("von Blandowski, J.W.T.L.")){CollectorNameParser.parseForList("""Blandowski, J.W.T.L. von""").get}
  }
  
  test("ignore brackets"){
    expect(List("Kinnear, A.J.")){CollectorNameParser.parseForList(""""KINNEAR A.J. (Sandy)"""").get}
    expect(Some("Ratkowsky, D. David")){CollectorNameParser.parse("David Ratkowsky (2589)")}
    expect(List("Ratkowsky, D. David","Gates, G.")){CollectorNameParser.parseForList("""David Ratkowsky (2589),G Gates (7518)""").get}
  }
  
  test("Initials then surname"){
    expect("Kirby, N.L."){CollectorNameParser.parse("NL Kirby").get}
    expect(List("Annabell, R. Graeme")){CollectorNameParser.parseForList("Annabell, Mr. Graeme R").get}
    expect(List("Kaspiew, B.")){CollectorNameParser.parseForList("B Kaspiew (Professor)").get}
    expect(List("Hegedus, A. Alexandra","Australian Museum","Science")){CollectorNameParser.parseForList("Hegedus, Ms Alexandra - Australian Museum - Science").get}
    expect(List("Hegedus, A.D. Alexandra","Australian Museum","Science")){CollectorNameParser.parseForList("Hegedus, Ms Alexandra Danica - Australian Museum - Science").get}
    //NL Kirby
    //Field, Ross P.
    //"ROBINSON A.C. Tony"
    //Annabell, Mr. Graeme R
    //B Kaspiew (Professor)
    //A.G. Gunness et al.
    
  }
  
  test("Unknown/Anonymous"){    
    expect(List("UNKNOWN OR ANONYMOUS")){CollectorNameParser.parseForList("No data").get}
    expect(List("UNKNOWN OR ANONYMOUS")){CollectorNameParser.parseForList("[unknown]").get}
    expect(List("UNKNOWN OR ANONYMOUS")){CollectorNameParser.parseForList(""""NOT ENTERED - SEE ORIGINAL DATA  -"""").get}
    expect(List("UNKNOWN OR ANONYMOUS")){CollectorNameParser.parseForList(""""ANON  N/A"""").get}
  }
  
  test("Organisations"){
    //println(CollectorNameParser.parse("Canberra Ornithologists Group"))
    expect(List("Canberra Ornithologists Group")){CollectorNameParser.parseForList("Canberra Ornithologists Group").get}
    expect(List(""""SA ORNITHOLOGICAL ASSOCIATION  SAOA"""")){CollectorNameParser.parseForList(""""SA ORNITHOLOGICAL ASSOCIATION  SAOA"""").get}
    expect(List("Macquarie Island summer and wintering parties")){CollectorNameParser.parseForList("Macquarie Island summer and wintering parties").get}
    expect("test Australian Museum test"){CollectorNameParser.parse("test Australian Museum test").get}
    expect(List(""""NPWS-(SA) N/A"""")){CollectorNameParser.parseForList(""""NPWS-(SA) N/A"""").get}
    expect(List("UNKNOWN OR ANONYMOUS")){CollectorNameParser.parseForList(""""NOT ENTERED - SEE ORIGINAL DATA -"""").get}
  }
  
  test("Mulitple collector tests"){
    expect(List("Spillane, N. Nicole", "Jacobson, P. Paul")){CollectorNameParser.parseForList("Nicole Spillane & Paul Jacobson").get}
    expect(List("Fisher, K. Keith","Fisher, L. Lindsay")){CollectorNameParser.parseForList("Keith & Lindsay Fisher").get}
    expect(List("Spurgeon, P. Pauline","Spurgeon, A. Arthur")){CollectorNameParser.parseForList("Pauline and Arthur Spurgeon").get}
    expect(List("Andrews-Goff, V. Virginia","Spinks, J. Jim")){CollectorNameParser.parseForList("Virginia Andrews-Goff and Jim Spinks").get}
    expect(List("Kemper, C.M. Cath","Carpenter, G.A. Graham")){CollectorNameParser.parseForList(""""KEMPER C.M. Cath""CARPENTER G.A. Graham"""").get}
    expect(List("James, D. David","Scofield, P. Paul")){CollectorNameParser.parseForList("""David James, Paul Scofield""").get}
    expect(List("Simmons, J.G.","Simmons, M.H.")){CollectorNameParser.parseForList("""Simmons, J.G.; Simmons, M.H.""").get}
    expect(List("Hedley, C.","Starkey","Kesteven, H.L.")){CollectorNameParser.parseForList("""C.Hedley, Mrs.Starkey & H.L.Kesteven""").get}
    expect(List("Gomersall, N.","Gomersall, V.")){CollectorNameParser.parseForList("""N.& V.Gomersall""").get}
    expect(List("Kenny, S.D. Sue", "Wallace-Ward, D. Di")){CollectorNameParser.parseForList(""""KENNY S.D. Sue""WALLACE-WARD D. Di"""").get}
    //Nicole Spillane & Paul Jacobson
    //Keith & Lindsay Fisher
    //Pauline and Arthur Spurgeon
    //"GREENWOOD G. Graeme""PITTS B. Brenda"
    //Virginia Andrews-Goff and Jim Spinks
    //Simmons, J.G.; Simmons, M.H.
    //Kim and Geoff Larmour
    //David James, Paul Scofield -- this one is going to be problematic due to possible surname, entries...
    //C.Hedley, Mrs.Starkey & H.L.Kesteven
    //N.& V.Gomersall
    //"KEMPER C.M. Cath""CARPENTER G.A. Graham""BROWN T. Tonia""HOW T.L. Travis""LOUGHLIN C. Chris"
  }
  
}