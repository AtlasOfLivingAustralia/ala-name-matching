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
 * This module demonstrates how annotations can be created and displayed without
 * using Browser plug-ins.  The code quality is "prototype" with numerous alerts
 * and very little error checking, but demonstrates the concepts well enough.
 * 
 * Prototype by Ron C_  2008-12-23
 */

// CONSTANTS
var DC_NS              = 'http://purl.org/dc/elements/1.0/';
var ANNOTATION_NS      = 'http://www.w3.org/2000/10/annotation-ns#';
var RDF_SYNTAX_NS      = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#';
var ANNOTATION_TYPE_NS = 'http://www.w3.org/2000/10/annotationType#';
var THREAD_NS          = "http://www.w3.org/2001/03/thread#";
var REPLY_TYPE_NS      = "http://www.w3.org/2001/12/replyType#";
var XHTML_NS           = "http://www.w3.org/1999/xhtml";

var DANNO_USER = 'Guest';

// This service proxies for Danno (as well as managing create/edit forms)
// allowing Danno to reside on a different host and not violate the client
// "same source" rule for JS HTTP connections.
var DANNO_HOST = 'http://test.ala.org.au/';
var DANNO_PROXY = DANNO_HOST + '/dias-b/dannotate5.php';
var DANNO_CB = DANNO_HOST + '/dias-b/dannotate4a.php';

var POPUP_DIMS = 'width=700,height=500,left=40,top=80';

var DELETE_ENABLED = true;
var EDIT_ENABLED = true;
var DELETE_FN = 'delete';
var EDIT_FN = 'edit';

var m_xps = new XPointerService();  // Instance of hacked Mozdev XPointer service
var m_cnt = 0;                      // Stateful counter for footnotes on current page
var m_popup = null;                 // stateful ref to last popup that was unhidden
var m_req = null;                   // XmlHTTPRequest for callback routine
var m_editTarg = null;              // temp hold of edit/delete Id.

var IS_DEBUG = false;


/**
 * Class wrapper for an RDF annotation provides access to values
 * @param rdf Root element of an RDF annotation returned by Danno
 */
function Annotation (rdf)
{
   var tmp;
   var node;
   var attr;
   
   this.rdf = rdf;
   
   try {
     attr = rdf.getAttributeNodeNS(RDF_SYNTAX_NS, 'about');
     this.id = attr.nodeValue;
     
     var isReply = false;
     node = rdf.getElementsByTagNameNS(RDF_SYNTAX_NS, 'type');
     for (var i = 0; i < node.length; i++) {
      attr = node[i].getAttributeNodeNS(RDF_SYNTAX_NS, 'resource');
        tmp = attr.nodeValue;
        if (tmp.indexOf(ANNOTATION_TYPE_NS) == 0) {
           this.type = tmp.substr(ANNOTATION_TYPE_NS.length);
        }
        else if (tmp.indexOf(REPLY_TYPE_NS) == 0) {
            this.type = tmp.substr(REPLY_TYPE_NS.length);
        }
        else if (tmp.indexOf(THREAD_NS) == 0) {
          isReply = true;
        }
     }
     this.isReply = isReply;
     
     if (! this.isReply) {
       node = rdf.getElementsByTagNameNS(ANNOTATION_NS, 'annotates');
       attr = node[0].getAttributeNodeNS(RDF_SYNTAX_NS, 'resource');
       this.resource = attr.nodeValue;
       this.about = null;
     }
     else {
       node = rdf.getElementsByTagNameNS(THREAD_NS, 'root');
       attr = node[0].getAttributeNodeNS(RDF_SYNTAX_NS, 'resource');
       this.resource = attr.nodeValue;
       node = rdf.getElementsByTagNameNS(THREAD_NS, 'inReplyTo');
       attr = node[0].getAttributeNodeNS(RDF_SYNTAX_NS, 'resource');
       this.about = attr.nodeValue;
     }
     
     node = rdf.getElementsByTagNameNS(ANNOTATION_NS, 'body');
     attr = node[0].getAttributeNodeNS(RDF_SYNTAX_NS, 'resource');
     this.bodyURL = attr.nodeValue;
     
     node = rdf.getElementsByTagNameNS(ANNOTATION_NS, 'created');
     this.created = safeGetFirstChildValue(node);
     node = rdf.getElementsByTagNameNS(ANNOTATION_NS, 'modified');
     this.modified = safeGetFirstChildValue(node);
     
     if (this.isReply) {
       this.context = '';
     }
     else {
       node = rdf.getElementsByTagNameNS(ANNOTATION_NS, 'context');
       this.context = safeGetFirstChildValue(node);
     }
     
     node = rdf.getElementsByTagNameNS(DC_NS, 'creator');
     this.creator = safeGetFirstChildValue(node, 'anon');
     
     node = rdf.getElementsByTagNameNS(DC_NS, 'title');
     this.title = safeGetFirstChildValue(node);
     
     node = rdf.getElementsByTagNameNS(DC_NS, 'language');
     this.lang = safeGetFirstChildValue(node);
     
     var req = DANNO_PROXY + '?act=bget&url=' + this.bodyURL;
     this.body = getAjaxRespSync(req);
   }
   catch (ex) {
     var st = "Error parsing RDF" + (this.id ? ' for ' + this.id : '') +
              ':\n' + ex.toString();
     throw new Error(st);
   }
}

/**
 * Debug assist displays all class member values
 */
Annotation.prototype.dump = function () 
{
  alert('id='+this.id+'\nisReply='+this.isReply+'\ntype='+this.type+
        '\nresource='+this.resource+'\nabout='+this.about+
        '\ncreator='+this.creator+'\ntitle='+this.title+
        '\ncreated='+this.created+'\nmodified='+this.modified+
        '\nbodyURL='+this.bodyURL+'\nlang='+this.lang+
        '\ncontext='+this.context+'\nbody='+this.body);
}

/**
 * This fn depends on a hacked version of nsXpointerService being loaded by the browser
 * before this script is loaded from tags in the page being annotated.
 * @return XPath/XPointer statement for selected text, or '' if no selection.
 */
function getXPathForSelection ()
{
  var xp = '';
  try {
    seln = window.getSelection();
    if (seln != null) {
      select = seln.getRangeAt(0);
      xp = m_xps.createXPointerFromSelection(seln, window.document);
    }
  }
  catch (ex) {
    alert('XPath create failed\n' + ex.toString());
  }
  return xp;
}

/**
 * Extracts the URL of the page being displayed in by the Proxy.
 * @return Actual target page URL including protocol
 */
function getTargetUrl ()
{
  // FIXME: this depends on the 'url' arg (if present) being the last arg of the request
  var arg = 'url=';
  var args = window.location.search;
  var idx = args.indexOf(arg);
  var url = (idx > 0) ? args.substr(idx + arg.length) : window.location.toString();
  if (url.charAt(url.length - 1) == '#') {
    url = url.substring(0, url.length - 1);
  }
  return url;
}

/**
 * Get the Range defined by an XPath/Xpointer (restricted to subset of
 * expressions understood by Anozilla).
 */
function getSelectionForXPath (xp)
{
    return m_xps.parseXPointerToRange(xp, window.document);
}

/**
 * If user has a valid selection in the page, invoke the Annotation form.
 */
function createAnnotation ()
{
  var xpath = getXPathForSelection();
  if (!xpath) {
    // FIXME: we should allow users to select nothing and effectively 
    // annotate the entire page but more thinking required about where
    // the decoration should go and what the "text" should be. 
    alert('Warning! No text selected.');
    return;
  }
  var args = '?url=' + encodeURIComponent(getTargetUrl()) +
             '&xp=' + encodeURIComponent(xpath) +
             '&title=' + encodeURIComponent(document.title);
  callForm(args, '1');
}

/**
 * Invoke the form that creates a reply to an annotation (or another reply)
 * @param root The URI of the reply thread is about
 * @param inreply The URI of the reply this reply is about (or root if first in chain)
 */
function createAnnotationReply (root, inreply)
{
  var args = '?url=' + encodeURIComponent(root) +
             '&ref=' + encodeURIComponent(inreply) +
             '&title=About:%20' + encodeURIComponent(document.title);
  callForm(args, 1);
}

/**
 * Test for user permission to delete/edit an entry.
 * @param id ID of annotation/reply
 * @throws Error if id not found. Returns silently otherwise.
 */
function testExists (id)
{
  // FIXME: ultimately, we need to pass what we want to do with this
  // id (read, update, delete) so we get authorization as well.
  
  // id will be a Danno query string which we must redirect thru our proxy.
  doc = getAjaxRespSync(DANNO_PROXY + '?act=bget&url=' + id);
  if ((doc == null) || (typeof doc == 'string')) {
    throw new Error('Unable to validate Annotation still exists!\n' + id);
  } 
  var nodeList = doc.getElementsByTagNameNS(RDF_SYNTAX_NS, 'Description');
  if (nodeList.length == 0) {
    throw new Error('Annotation no longer exists!\n' + id);
  }
}

/**
 * Deletes an annotation. No checks are made for referential integrity,
 * so unless the server takes care of it, deleting annotations and replies
 * may lead to orphaned bodies and replies with no root, etc.
 * @param id Annotation or Reply id to delete
 * @return true if item was deleted
 */
function doDelete (id)
{
  var res = doModify(id, DELETE_FN);
  if (res) {
    // remove the visible tag and all children (hidden div)
    var tmp = window.document.getElementById(id);
    if (tmp) {
      tmp.parentNode.removeChild(tmp);
    }
  }
  return res;
}

/**
 * Call a form that allows the annotation and body to be edited
 * @param id Annotation or Reply unique ID
 */
function editAnnotation (id)
{
  // FIXME: we need to 'remember' the id because the edit depends on an async
  // callback which will insert the new one, or reinsert the old one if the
  // operation was cancelled.
  m_editTarg = id;
  var res = doModify(id, EDIT_FN);
  if (res) {
    // remove the visible tag and all children (hidden div)
    // FIXME: if has chained replies they disappear but can't be re-added
    // because they exist in the dom!
    var tmp = window.document.getElementById(id);
    if (tmp) {
      tmp.parentNode.removeChild(tmp);
    }
  }
}

/**
 * Delete or Edit an annotation or reply.
 * @param id ACTUAL Annotation ID (not body)
 * @param op One of EDIT_FN | DELETE_FN
 * @return True if operation permitted and maybe succeeded (edit is async)
 */
function doModify (id, op)
{
  var res = false;
  try {
    testExists(id);
    if (op == DELETE_FN) {
      var resp = getAjaxRespSync(DANNO_PROXY + '?act=del&url=' + id);
      res = (resp == 'ok');
    }
    else if (op == EDIT_FN) {
      callForm('?url=' + id, '1a');
      res = true;
    }
  }
  catch (ex) {
    // error string should be user friendly in this case (ours!)
    alert(ex.toString());
  }
  return res;
}

/**
 * Invokes a server that will provide a dynamically created annotation creation
 * form in a popup window (Firefox) or the host tab (IE)
 * @param args Unique args
 * @param action Code for servlet FSM (1: create Annotation; 7: Edit Annotation)
 */
function callForm (args, action)
{
  var token = getAjaxRespSync(DANNO_PROXY + '?act=0');
  var form = DANNO_PROXY + args +
            '&act=' + action +
            '&name=' + DANNO_USER +
            '&token=' + token +
            '&jump=';
  openForm = function() {
    // try to open a popup. If that fails, change current tab location
    if (!window.open(form + 'noClose','Dannotate 0.4',
      'location=no,links=no,scrollbars=no,toolbar=no,status=no,' +
      POPUP_DIMS, false)) {
         location.href = form + 'noReturn';
    }
  };
  /Firefox/.test(navigator.userAgent)?setTimeout(openForm, 0) : openForm();
  // Entry for processing now taking place, so setup for an async callback
  // that will eventually get the ID of the completed annotation, or a timeout
  setDannoCallback(token);
}

/**
 * Starts a long-lived asynchronous exchange that may eventually return the id of
 * a new annotation, or a timeout.
 * @param token Unique session id provided at start of create annotation exchange.
 */
function setDannoCallback (token)
{
   try {
     var url = DANNO_CB + '?act=3&token=' + token;
     m_req = new XMLHttpRequest();
     m_req.open('GET', url, true);
     m_req.setRequestHeader('UserAgent','XMLHttpRequest');
     m_req.setRequestHeader('Content-Type','application/text');
     m_req.onreadystatechange = doCallBack;
     m_req.send(null);
   }
   catch (ex) {
     alert("Error\n" + ex);
   }
}

/**
 * Callback for async AJAX exchange. Reply will either be the URL of our bright, shiny,
 * new annotation, or an error of some kind.
 * @return
 */
function doCallBack ()
{
  try {
    if (m_req.readyState == 4) {
      if (m_req.status != 200) {
        if (m_req.status >= 100) {
          // FIXME: Occasionally seeing a status of zero!  Happens when the
          // repeater has supplied a page with cookies (like GBIF).  Why?
          alert('Server error ' + m_req.status + ' processing call back');
        }
        m_req = null;
        return;
      }

      var resp = m_req.responseText;
      m_req = null;
      var atoms = resp.split(/[: ]/);
      switch (atoms[0]) {
        case 'http':    fetchInsert(resp); break;
        case 'Cancel':
        case 'Timeout': fetchInsert(m_editTarg); break;
        default: 
          alert('Unexpected callback reply: ' + resp);
          return;
      }
    }
  }
  catch (ex) {
    alert('Callback error:\n' + ex);
  }
}

/**
 * Retrieve and insert to the DOM an annotation or reply by danno ID.
 * @param URL of danno annotation or reply request
 */
function fetchInsert (id)
{
  if ((id == null) || (id =='')) {
    return;
  }

  doc = getAjaxRespSync(DANNO_PROXY + '?act=bget&url=' + id);
  if ((doc == null) || (typeof doc == 'string')) {
    alert('Unable to fetch new or updated annotation for id:\n' + id);
    return;
  }
  
  var nodeList = doc.getElementsByTagNameNS(RDF_SYNTAX_NS, 'Description');
  if (nodeList.length == 0) {
    alert('Can\'t get annotation:\n' + id);
  }
  else {
    var ano = new Annotation(nodeList.item(0));
    if (ano.isReply) {
      // Need root annotation for context XPath
      doc = getAjaxRespSync(DANNO_PROXY + '?act=bget&url=' + ano.resource);
      if (doc == null) {
        alert('Can\'t get root annotation:\n' + ano.about + '\nfor Reply ' + id);
        return;
      }
      nodeList = doc.getElementsByTagNameNS(RDF_SYNTAX_NS, 'Description');
      if (nodeList.length == 0) {
        alert('Bad root annotation:\n' + ano.about + '\nfor Reply ' + id);
        return;
      }
      root = new Annotation(nodeList.item(0));
      ano.context = root.context;
    }
    insertAnnotation(ano);
  }
}

/**
 * Finds or creates a node to which our annotation marker may be attached.
 * If the end of selection range lies within a text node, split it at after the
 * end of the selection and insert a zero length span tag which becomes the
 * marker node to which the visible "footnote" is attached.
 * If the XPath selection is an Element node, that becomes the marker node.
 * @param range
 * @param ano
 * @return
 */
function findAnchorNode (range, ano)
{
  var endNode = range.endContainer;
  var parent = endNode.parentNode;
  var node = endNode;
  
  if (endNode.nodeType != node.TEXT_NODE) {
    node = parent;
  }
  else {
    var xpath = ano.context;
    if (xpath.toLowerCase().indexOf('string-range') > 0) {
      var nextTextNode = endNode.splitText(range.endOffset);
      var markerNode = createDomNode('span');
      // In case we need to locate them, the empty snap tags get
      // an ID related to the annotation ID with an 'A-' prefix.
      markerNode.id = 'A-' + ano.id;
      parent.insertBefore(markerNode, nextTextNode);
      node = markerNode;
    }
  }
  return node;
}

/**
 * Simple helper creates a node for the window document marked "ignore"
 * so our XPath creator will not include them in any future user selection
 * XPath statements.
 * ALL INSERTED NODES MUST BE CREATED WITH THIS METHOD.
 * @param type HTML tag 
 * @param id ID name for node (may be null)
 * @param name Class name for node (may be null)
 * @return Unattached node of tag type
 */
function createDomNode (type, id, name)
{
  var node = window.document.createElementNS(XHTML_NS, type);
  m_xps.markElement(node);
  m_xps.markElementHide(node);
  if (id != null) {
    node.id = id;
  }
  if (name != null) {
    node.className = name;
  }
  return node;
}

/**
 * Helper method creates a text node for the passed text.
 * @param text CDATA
 * @return text node
 */
function createDomTextNode (text)
{
  // FIXUP. Should check for required escapes and stuff
  var node = window.document.createTextNode(text);
  return node;  
}

/**
 * Inserts a footnote type reference after a selection range with hidden text
 * for an annotation provided it does not already exist.
 * @param hRange W3C Range object for annotation selection
 * @param num Value for "footnote" and related popup 
 * @param text Inner text for hidden div, or blank to create using DOM
 * @param ano Annotation instance
 * @return true if insertion made
 */
function decorate (hRange, num, text, ano)
{
  var inserted = false;
  if (!window.document.getElementById(ano.id)) {
    var targetNode = getTargetNode(hRange, ano);
    
    // Visual marker is an anchor that will un-hide/hide the annotation data
    var nodeToInsert = createDomNode('a', ano.id, 
        'dannotation fonz' + (ano.isReply ? " reply" : ""));
    nodeToInsert.innerHTML = "[" + num + "]";
    nodeToInsert.setAttribute("href", "#");
    nodeToInsert.setAttribute("onmouseover", "popShow(this.id,'popup" + num + "')");
    nodeToInsert.setAttribute("onmouseout", "popHide(event)");
    
    // Annotation is an initially hidden div with text of annotation
    if (text.length == 0) {
      var infonode = createPopUpNode(num, ano, hRange.toString());
    }
    else {
      var infonode = createDomNode('div', 'popup' + num, 'popup');
      infonode.innerHTML = text;
    }

    nodeToInsert.appendChild(infonode);
    targetNode.appendChild(nodeToInsert);
    inserted = true;
  }
  return inserted;
}

/**
 * This function may be overridden for special cases.
 * @param hRange The selection created from an XPath
 * @param ano The annotation which we are goint to insert
 * @return the DOM node to which an annotation is to be attached.
 */
function getTargetNode (hRange, ano)
{
  if (!ano.isReply) {
    return findAnchorNode(hRange, ano);
  }
  
  var targetNode = window.document.getElementById('A-' + ano.resource);
  if (targetNode == null) {
    alert('No node with ID A-' + ano.resource);
    // Must be the parent
    targetNode = hRange.endContainer.parentNode;
  }
  return targetNode;
}

/**
 * Creates a node tree for the hidden annotation popup as a div node.
 * All elements are marked ignore so they'll be transparent to the Xpather.
 * @param num Numeric index for this annotation
 * @param ano The Annotation instance
 * @param st The text of the xPath selection
 * @return Node, with children
 */
function createPopUpNode (num, ano, st)
{
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
  td2Node.appendChild(createDomTextNode(st));
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
  
  var p2Node = createDomNode('p', null, 'body');
  // FIXUP this should be done better (regex).  Also, if the body is HTML,
  // it may contain markup which should be assembled with "marked" nodes.
  var aBody = ano.body;
  var idx = aBody.toLowerCase().indexOf('<body');
  if (idx >= 0) {
    idx = aBody.indexOf('>', idx);
    var jdx = aBody.toLowerCase().indexOf('</body');
    aBody = aBody.substring(idx+1, jdx);
  }
  p2Node.appendChild(createDomTextNode(aBody));
  divNode.appendChild(p2Node);
  
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
 * Creates the content of a hover-text window for an annotation and inserts it and the
 * triggering anchor to the window document model.
 * @param ano All the annotation data returned by the Danno server as an Annotation object
 */
function insertAnnotation (ano)
{
  try {
    var num = m_cnt + 1;
    var idx = ano.context.indexOf('#');
    var sel = getSelectionForXPath(ano.context.substring(idx + 1));
    var text = '';
    if (decorate(sel, num, text, ano)) {
      m_cnt = num;
    }
  }
  catch (ex) {
    alert('Failure inserting markup for Annotation ID:\n' + ano.id + '\n\n' + ex);
  }
}

/**
 * Called when pointer enters an annotation decoration to show text of annotation
 * @param markerId The marker element id.
 * @param popupId The corresponding hidden div id.
 */
function popShow (markerId, popupId)
{
  // check transit from one popup to another
  var tmp = document.getElementById(popupId);
  if ((m_popup != null) && (m_popup != tmp)) {
    m_popup.style.visibility = 'hidden';
  }
  m_popup = tmp;

  // Set global for the hide function
  var overlap = 4;
  var trigger = document.getElementById(markerId);
  var xpos = trigger.offsetLeft + overlap;
  var ypos = trigger.offsetTop + overlap;
  // FIXME: this needs lots more work.
  var parent = trigger.offsetParent;
  while (parent != null) {
    if (null != parent.offsetParent) {
      ypos += parent.offsetTop;
    }
    parent = parent.offsetParent;
  }
  parent = trigger.offsetParent;
  while (parent != null) {
    xpos += parent.offsetLeft;
    parent = parent.offsetParent;
  }

  if ((xpos + m_popup.offsetWidth) > document.body.clientWidth) {
    xpos -= (m_popup.offsetWidth - overlap);
  }
  if ((ypos + m_popup.offsetHeight) > document.body.clientHeight) {
    ypos -= (m_popup.offsetHeight - overlap);
  }
  m_popup.style.left = xpos + 'px';
  m_popup.style.top = ypos + 'px';
  m_popup.style.visibility = 'visible';
}

/**
 * Called when pointer exits an annotation marker to hide text of annotation.
 * @param event Associated mouse event
 * @return True if we consumed the event
 */
function popHide (event)
{
  var exit = false;
  if (m_popup && event && event.relatedTarget) {
    // check transit from marker to associated popup
    //var ignore = event.relatedTarget.getAttribute(xps.DOM_IGNORE_ELEMENT_ATTRIBUTE);
    var ignore = event.relatedTarget.getAttribute('_dom_ignore_element_');
    if (ignore == null) {
      m_popup.style.visibility = 'hidden';
      m_popup = null;
      exit = true;
    }
  }
  return exit;
}

/**
 * Gets annotations for a specific URL
 * @param key Value to match [annotates rdf:resource]
 * @return RDF Document (may be empty) or null on error
 */
function getAnnotationsRDF (key, isReply) 
{
  var action = isReply ? 'rget' : 'aget';
  var req = DANNO_PROXY + '?act=' + action + '&url=' + key;
  return getAjaxRespSync(req);
}

/**
 * Get annotation body value.
 * @param uri Fully formed request against Danno annotation server
 * @return Server response as text or XML document.
 */
function getAjaxRespSync (uri) 
{
  if (IS_DEBUG && (uri.indexOf('act=') < 0)) {
    alert('DIRECT CALL TO ' + uri);
  }
  var req = null;
  try {
    req = new XMLHttpRequest();
    req.open('GET', uri, false);
    req.setRequestHeader('User-Agent','XMLHttpRequest');
    req.setRequestHeader('Content-Type','application/text');
    req.send(null);
  }
  catch (ex) {
    alert('Error in synchronous AJAX request:\n  ' + ex + '\n\nURL: ' + uri);
    return null;
  }

  if (req.status != 200) {
    alert('status '+req.status);
    var hst = (uri.length < 65) ? uri : uri.substring(0, 64) + '...';
    throw new Error('Synchronous AJAX request status error.\n  URI: ' + hst + 
                    '\n  Status: ' + req.status);
  } 

  rtype = req.getResponseHeader('Content-Type');
//alert('POST Headers:\n'+req.getAllResponseHeaders());
  if (rtype == null) {
    // FIXME: Safari sometimes does no set the Content-Type, so for now,
    // try to intuit it thru the text.
//alert('null content-type. Headers:\n'+req.getAllResponseHeaders());
     var txt = req.responseText;
     var doc = null;
     if (txt && (txt.indexOf(':RDF') > 0)) {
       doc = req.responseXML;
       if ((doc == null) && (typeof DOMParser != 'undefined')) {
         //alert('parsing:\n'+txt);           
         var parser = new DOMParser();
         doc = parser.parseFromString(txt, 'application/xml');
       }
     }

     if (doc != null) {
       return doc;
     }
     else if (txt != null) {
       return txt;
     }
  }
  if (rtype.indexOf(';') > 0) {
    // strip any charset encoding etc
    // FIXME: What to do with Annozilla bodies of application/xhtml+xml ?
    rtype = rtype.substring(0, rtype.indexOf(';'));
  }
  switch (rtype) {
    case 'application/xml':  return req.responseXML;
    case 'application/html': return req.responseText;
    case 'application/text': return escapeHTML(req.responseText);
  }
  throw new Error('No usable response.\nContent is "' + rtype + '"' +
                  '\nRequest:\n' + uri + '\n' + req.responseText);
}

/**
 * Retrieve annotations and decorate the page DOM with footnote style tool-tips.
 * @param key Page URL
 * @return number of annotations processed
 */
function getAnnotations (key)
{
  var cnt = 0;
  key = decodeURIComponent(key);
  var raw1 = getAnnotationsRDF(key, false);
  if (raw1 && raw1.getElementsByTagNameNS) {
    var nodeList1 = raw1.getElementsByTagNameNS(RDF_SYNTAX_NS, 'Description');
    if (nodeList1.length == 0) {
      alert("No annotations found for " + sanitizeUrl(key));
    }
    else {
      var annotations = orderByDate(nodeList1);
      for (var i = 0; i < annotations.length; i++) {
        insertAnnotation(annotations[i]);
        ++cnt;
        var raw2 = getAnnotationsRDF(annotations[i].id, true);
        var nodeList2 = raw2.getElementsByTagNameNS(RDF_SYNTAX_NS, 'Description');
        if (nodeList2.length > 0) {
          replies = orderByDate(nodeList2)
          for (var j = 0; j < replies.length; j++) {
            replies[j].context = annotations[i].context;
            insertAnnotation(replies[j]);
            ++cnt;
          }
        }
      }
    }
  }
  return cnt;
}

/**
 * Creates an array of Annotations from a list of RDF nodes in ascending date
 * created order
 * @param nodeList Raw RDF list in arbitrary order
 * @return ordered array of Annotations
 */
function orderByDate (nodeList)
{
  var tmp = new Array();
  for (var j = 0; j < nodeList.length; j++) {
    try {
      tmp[j] = new Annotation(nodeList.item(j));
    }
    catch (ex) {
      alert(ex.toString());
    }
  }
  return tmp.length == 1 ? tmp : 
         tmp.sort(function(a,b){return (a.created > b.created ? 1 : -1)});
}

/**
 * Returns value of first child of first node, or default value if provided.
 */
function safeGetFirstChildValue (node, defaultValue)
{
  return ((node.length > 0) && (node[0] != null) && node[0].firstChild) ?
           node[0].firstChild.nodeValue : defaultValue ? defaultValue : '';
}

function sanitizeUrl (key)
{
  var idx = key.indexOf('&ck=');
  if (idx > 0) {
    key = key.substring(0, idx);
  }
  idx = key.indexOf('#');
  if (idx > 0) {
    key = key.substring(0, idx);
  }
  return key;
}

/**
 * Escape HTML special chars in a string
 * @param st Usually application/txt content
 * @return escaped copy of input
 */
function escapeHTML (st)
{
  // Why does the JS regex not support replace?
  var txt = st;
  var atom = st.split('&');
  if (atom.length > 1) {
    txt = atom[0];
    for (var i = 1; i < atom.length; i++) {
      txt += '&amp;' + atom[i];
    }
  }
  atom = txt.split('<');
  if (atom.length > 1) {
    txt = atom[0];
    for (var i = 1; i < atom.length; i++) {
      txt += '&lt;' + atom[i];
    }
  }
  atom = txt.split('>');
  if (atom.length > 1) {
    txt = atom[0];
    for (var i = 1; i < atom.length; i++) {
      txt += '&gt;' + atom[i];
    }
  }
  return txt;
}

//  -------- EOF --------
