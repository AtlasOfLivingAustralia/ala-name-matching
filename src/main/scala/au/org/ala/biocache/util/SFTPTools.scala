package au.org.ala.biocache.util

import java.io.File
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import collection.JavaConversions
import com.jcraft.jsch.JSch
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import org.rev6.scf._
import org.apache.commons.lang3.StringUtils
import java.util.Date
import au.org.ala.biocache.Config

object SFTPTools {

  import JavaConversions._

  val logger = LoggerFactory.getLogger("SFTPTools")
  //SFTP items
  val connectionPattern = """sftp://([a-zA-Z]*):([a-zA-Z]*)@([a-zA-z\.]*):([a-zA-Z/]*)""".r
  logger.debug(connectionPattern.toString())

  var channelSftp:ChannelSftp = null
  var session:Session = null
  val sftpPattern = """sftp://([a-zA-z\.]*):([0-9a-zA-Z_/\.\-]*)""".r

  def sftpLatestArchive(url:String, resourceUid:String, tempDir:String, afterDate:Option[Date]):Option[(String,Date)]={

    val (user,password,host,directory) ={
      url match{
      case connectionPattern(user, password, host, directory) => {
        (user, password, host, directory)
      }
      case sftpPattern(host,directory) => {
        val u=Config.getProperty("uploadUser")
        val p =Config.getProperty("uploadPassword")
        if(StringUtils.isEmpty(u) || StringUtils.isEmpty(p))
          logger.error("SCP User or password has not been supplied. Please supply as part of the biocache.properties")
         (u,p,host,directory)
      }
      case _=>logger.error("Unable to connect to " + url);(null,null,null,null)
    }}
    connect(host, user, password)
    val lastFile = getLatestFile(directory, "*.*",afterDate)
    disconnect
    if(lastFile.isDefined){
      val dir = tempDir + resourceUid
      //scp the file is faster than sftp
      scpFile(host,user,password,lastFile.get,new File(dir+ File.separator+lastFile.get))
    } else {
      logger.error("No latest file for " + url); None
    }
  }

  def connect(host:String,  user:String, password:String,port:Int=22){
    val jsch = new JSch()
    session = jsch.getSession(user,host,port)
    val config = new java.util.Properties()
    config.put("StrictHostKeyChecking", "no")
    session.setConfig(config)
    session.setPassword(password)
    session.connect()
    val channel = session.openChannel("sftp")
    channel.connect()
    channelSftp =channel.asInstanceOf[ChannelSftp]
  }

  def disconnect(){
    channelSftp.disconnect()
    session.disconnect()
  }

  def getLatestFile(dir:String, filePattern:String, afterDate:Option[Date]):Option[String]={

    val list = listFiles(dir+File.separator+filePattern)
    if(list.size>0){
      val item=list.reduceLeft((a,b)=>if(a.getAttrs().getMTime() > b.getAttrs().getMTime()) a else b)
      if(afterDate.isEmpty || (afterDate.get.getTime()/1000)<item.getAttrs().getMTime())
        Some(dir + File.separator+item.getFilename)
      else
        None
    } else {
      None
    }
  }

  /**
   * An ordering that sorts a list of SFTP files by the last modified time.
   */
  implicit val o = Ordering.by((p: ChannelSftp#LsEntry) => (p.getAttrs().getMTime()))

  def listFiles(filePattern:String):List[ChannelSftp#LsEntry]={
    val vector = channelSftp.ls(filePattern)
    vector.asInstanceOf[java.util.Vector[ChannelSftp#LsEntry]].toList.sorted(o.reverse)//.sort()
  }

  /**
   * SCP the remote file from the supplied host into localFile
   */
  def scpFile(host:String, user:String, password:String, remoteFile:String, localFile:File):Option[(String,Date)]= {
    if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password))
      logger.error("SCP User or password has not been supplied. Please supply as part of the biocache.properties")
    var ssh:SshConnection = null
    try {
      ssh = new SshConnection(host,user,password)
      ssh.connect()

      FileUtils.forceMkdir(localFile.getParentFile())
      val scpFile = new ScpFile(localFile, remoteFile)
      //command to get the last modified date in number of seconds since epoch
      val outputStream = new java.io.ByteArrayOutputStream()
      val command = new SshCommand("date -r " + remoteFile + " +%s", outputStream)
      val date = {
        try {
          ssh.executeTask(command)
          val stringvalue = outputStream.toString()
          //logger.info("The string value of the modified date :$" + stringvalue+"$")
          new Date(stringvalue.trim.toLong *1000)
        } catch {
          case e:Exception => e.printStackTrace();new Date()
        }
      }

      ssh.executeTask(new ScpDownload(scpFile))
      Some((localFile.getAbsolutePath(),date))
    } catch {
       case e:Exception => logger.error("Unable to SCP " + remoteFile ,e); None
    } finally {
      if(ssh != null)
       ssh.disconnect()
      None
    }
  }
}