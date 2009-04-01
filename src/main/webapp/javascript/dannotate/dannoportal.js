/**
 * Copyright (c) 2009 School of Information Technology and Electrical Engineering, 
 * The University of Queensland.  This software is being developed for the "Data 
 * Integration and Annotation Services in Biodiversity" (DIAS-B) project.  DIAS-B
 * is a NeAT project, funded jointly by ARCS and ANDS, and managed by the Atlas 
 * of Living Australia (ALA).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This module over-rides vales and finction in the standard dannotate.js
 * module to implement ALA Portal specific needs related to context 
 * sensitive annotation creation for the 2009 Q1 demo.
 * 
 * This and related code is a prototype. They are unstable, poorly testsd,
 * and subject to change at any time.
 * 
 * Prototype by Ron C_  2009-03-04
 */


// Change the servlet that creates annotation entry forms, etc.
DANNO_PROXY = DANNO_HOST + '/dias-b/dannotate5.php';
DANNO_CB = DANNO_HOST + '/dias-b/dannotate4a.php';

// Make popup window large enough for largest entry form
POPUP_DIMS = 'width=700,height=770,left=100,top=80';

EDIT_ENABLED = false;

// FIXME: bogus
// Name space made up for ALA annotations
ALA_NS = 'http://ala.org.au/2009/annotation/0.1/';

// Lookup for ALA annotation types to full text descr
ALA_ANNOTATION_TYPES = {
    "latLongReversed" : "Latitude and Longitude are reversed",
    "coordsLocality" : "Coordinates do not match locality",
    "coordsState" : "Coordinates do not match supplied state",
    "latInverted" : "Latitude inverted",
    "longInverted" : "Longitude inverted",
    "coordsLocality" : "Coordinates are inaccurate",
    "otherGeospatial" : "Other...",

    "taxonMisinterpreted" : "Scientific classification incorrectly interpreted",
    "commonNameSupplied" : "Supplied Scientific name is a common name",
    "suppliedClassificationIncomplete" : "Supplied Classification is Incomplete",
    "speciesMisidentified" : "Species misidentified",
    "otherTaxonomy" : "Other...",

    "instCodeWrong" : "Institution code incorrect/mispelt",
    "collCodeWrong" : "Collection code incorrect/mispelt",
    "borWrong" : "Basis of record incorrect",
    "borMisinterpreted" : "Basis of record misinterpreted",
    "collectorWrong" : "Collector name incorrect/mispelt",
    "identiferWrong" : "Indentifer name incorrect/mispelt",
    "fieldNumberWrong" : "Field number incorrect/mispelt",

};

/**
 * Special impl for ALA Portal use uses the user selection to determine the
 * id of the section in which the selection was made and dummies an XPointer
 * for that. The actual selection is unused.
 */
getXPathForSelection = function ()
{
  var xp = '';
  try {
    var seln = window.getSelection();
    if (seln != null) {
      select = seln.getRangeAt(0);
      //xp = m_xps.createXPointerFromSelection(seln, window.document);
      var pat1 = /occurrenceRecord/;
      var pat2 = /occurrenceDataset|occurrenceTaxonomy|occurrenceGeospatial/;
      var node = select.startContainer;
      // FIXME: should not have to do this (bug in Dave's demo page?)
      // chase up to the containing DIV
      while (node && (pat2.exec(node.id) == null)) {
        node = node.parentNode;
      }
      if (node != null) {
        node = node.firstChild;
        // chase through DIV's children to the record and section id
        while (node) {
          if (pat1.exec(node.id) != null) {
            // FIXME: this may be a bad idea, but for now encode id as an 
            // xpointer. We are not really going to use it anyway.
            xp = 'xpointer(string-range(id("' + node.id + '"),"",0,0))';
            break;
          }
          node = node.nextSibling;
        }
      }
    }
  }
  catch (ex) {
    //alert('XPath create failed\n' + ex.toString());
  }
  return xp;
}

/**
 * Override of base function sets insertion point to the first header inside
 * div identified by the xpath, regardless of what the xpath says.
 * @param hRange
 * @param ano
 * @return
 */
findAnchorNode = function (hRange, ano)
{
  var regex = new RegExp('id\\("(.+?)"\\)');
  var id = regex.exec(ano.context);
  var fieldset = window.document.getElementById(id[1]);
  var info = getUserInput(ano);
  if (info == null) {
    ano.dump();
    throw 'Unable to obtain annotation details';
  }
  var node = getBestLocation(fieldset, info['old']);
  // In case we need to locate them, the empty span tags get
  // an ID related to the annotation ID with an 'A-' prefix.
  var markerNode = createDomNode('span');
  markerNode.id = 'A-' + ano.id;
  node.appendChild(markerNode);
  return markerNode;
}

function getBestLocation (element, st)
{
  // FIXME: this is all kinda kruddy. We should have the node which contains
  // all the data and label nodes for the data context. First, we'll try to
  // match text against the passed value which claims to be the "current"
  // setting. If we can, that will be the best location.
  var node = null;
  if ((st != null) && (st != '')) {
    var cans = element.childNodes;
    for (var i = 0; i < cans.length; i++) {
      var tw = document.createTreeWalker(cans[i],
        XPointerCreator.SHOW_ONLY_TEXT_NODES, null, XPointerCreator.NO_ENTITY_NODE_EXPAND);
      node = tw.nextNode();
      while (node) {
        var tmp = (node.nodeType == 3) ? node.data : node.innerText;
        tmp = tmp.replace(/^\s*/, '');
        tmp = tmp.replace(/\s*$/, '');
        //if (st == 's.n.') {alert('"'+tmp+'"');}
        if (tmp == st) {
          node = cans[i].firstChild;
          while ((node != null) && (node.tagName != 'LABEL')) {
            node = node.nextSibling;
          }
          if (node != null) {
            return node;
          }
          break;
        }
        node = tw.nextNode();
      }
    }
  }
  // If we can't, or the "current" value is empty, we'll get the parent of the
  // field set which should be a DIV and assume it has a H4 child which will
  // be next best chouce.  If that fails too, we'll use the passed node.
  if (node == null) {
    node = element.parentNode.firstChild;
    while ((node != null) && (node.tagName != 'H4')) {
      node = node.nextSibling;
    }
    if (node == null) {
      node = element.parentNode.firstChild;
    }
  }
  return node;
}

/**
 * Creates a node tree for the hidden annotation popup as a div node.
 * All elements are marked ignore so they'll be transparent to the Xpather.
 * @param num Numeric index for this annotation
 * @param ano The Annotation instance
 * @return Node, with children
 */
function createPopUpNode (num, ano)
{
  // parse annotation data to associative list
  var info = getUserInput(ano);
  var divNode = createDomNode('div', 'popup' + num, 'popup fonz');
  
  var h2Node = createDomNode('h2', null, 'pophdr');
  var tmp = ano.title;
  if (IS_DEBUG) {
    var atom = ano.id.split('/');
    tmp += ' (' + atom[atom.length - 1] + ')';
  }
  h2Node.appendChild(createDomTextNode(tmp));
  divNode.appendChild(h2Node);
  divNode.appendChild(createDomNode('hr'));
  
  var tabNode = createDomNode('table');
  var trNode = createDomNode('tr');
  var td1Node = createDomNode('td', null, 'popup');
  td1Node.appendChild(createDomTextNode('[' + num + ']'));
  var td2Node = createDomNode('td', null, 'popup');
  td2Node.appendChild(createDomTextNode(info['type']));
  trNode.appendChild(td1Node);
  trNode.appendChild(td2Node);
  tabNode.appendChild(trNode);
  divNode.appendChild(tabNode);
  divNode.appendChild(createDomNode('hr'));
  
  var p1Node = createDomNode('p');
  var emNode = createDomNode('em');
  var prov = ano.type + ' from ' + ano.creator + ', ';
  if (ano.modified != '') {
    prov += 'last modified on ' + ano.modified;
  }
  else {
    prov += 'created on ' + ano.created;
  }
  emNode.appendChild(createDomTextNode(prov));
  p1Node.appendChild(emNode);
  divNode.appendChild(p1Node);
  
  if (info['new']) {
    var p2Node = createDomNode('p', null, 'body');
    p2Node.appendChild(createDomTextNode(info['old'] + ' should be ' + info['new']));
    divNode.appendChild(p2Node);
  }
  if (info['descr']) {
    var p21Node = createDomNode('p', null, 'body');
    p21Node.appendChild(createDomTextNode(info['descr']));
    divNode.appendChild(p21Node);
  }
  divNode.appendChild(createDomNode('hr'));
  
  var btnNode = createDomNode('div', null, 'cont');
  if (DELETE_ENABLED) {
    var delNode = createDomNode('input');
    delNode.setAttribute('type', 'button');
    delNode.setAttribute('value', 'Delete');
    var delAction = 'doDelete(\'' + ano.id + '\')';
    delNode.setAttribute('onclick', delAction);
    var leftspan = createDomNode('div', null, 'port');
    leftspan.appendChild(delNode);
    btnNode.appendChild(leftspan);
  }
  var root = ano.isReply ? ano.resource : ano.id;
  var about = ano.isReply ? ano.about : ano.resource;
  var addLab = ano.isReply ? "Reply to Reply" : "Reply to Annotation";
  var repNode = createDomNode('input');
  repNode.setAttribute('type', 'button');
  repNode.setAttribute('value', addLab);
  var repAction = 'createAnnotationReply(\'' + root + '\',\'' + about + '\')';
  repNode.setAttribute('onclick', repAction);
  var midspan = createDomNode('div', null, 'port');
  midspan.appendChild(repNode);
  btnNode.appendChild(midspan);
  if (EDIT_ENABLED) {
    var editNode = createDomNode('input');
    editNode.setAttribute('type', 'button');
    editNode.setAttribute('value', 'Edit');
    var editAction = 'editAnnotation(\'' + ano.id + '\')';
    editNode.setAttribute('onclick', editAction);
    var rightspan = createDomNode('div', null, 'stbd');
    rightspan.appendChild(editNode);
    btnNode.appendChild(rightspan);
  }
  divNode.appendChild(btnNode);
  
  return divNode;
}

/**
 * Creates an associative list of user entered data from the XML doc
 * returned from the annotation body RDF.
 * @param ano The annotation object
 * @return
 */
function getUserInput (ano)
{
  var array = null;
  if (ano.isReply) {
    var aBody = ano.body;
    var idx = aBody.toLowerCase().indexOf('<body');
    if (idx >= 0) {
      idx = aBody.indexOf('>', idx);
      var jdx = aBody.toLowerCase().indexOf('</body');
      aBody = aBody.substring(idx+1, jdx);
    }
    array = { 'type':ano.type, 'descr':aBody };
  }
  else {
    var doc = ano.body;
    var list = ano.body.getElementsByTagNameNS(ALA_NS, 'comment');
    var cmnt = (list.length > 0) ? list[0].firstChild.nodeValue : '';
    var list = doc.getElementsByTagNameNS(RDF_SYNTAX_NS, 'Description');
    for (var i = 0; i < list.length; i++) {
      if (list[i].hasAttributeNS(RDF_SYNTAX_NS, 'nodeID')) {
        var n1 = list[i].getElementsByTagNameNS(ALA_NS, 'type');
        var n2 = list[i].getElementsByTagNameNS(ALA_NS, 'old');
        var n3 = list[i].getElementsByTagNameNS(ALA_NS, 'new');
        var atype = ALA_ANNOTATION_TYPES[n1[0].firstChild.nodeValue];
        var oldval = n2[0] && n2[0].firstChild ? n2[0].firstChild.nodeValue : '';
        var newval = n3[0] && n3[0].firstChild ? n3[0].firstChild.nodeValue : '';
        st = atype + ': ' + oldval  + ' should be ' + newval;
        array = { 'type':atype, 'old':oldval, 'new':newval, 'descr':cmnt };
        break;
      }
    }
  }
  return array;
}

// EOF

