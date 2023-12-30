package ch.makery.something.AKKA

import com.typesafe.config.{Config, ConfigFactory}

import java.net.{InetAddress, NetworkInterface}
import scala.collection.JavaConverters._
import ch.makery.something.Client

object MyConfiguration {
  //any customice of config must initialize localAddress and runlocalonly property
  var localAddress: Option[InetAddress] = None
  var runLocalOnly: Option[Boolean] = None

  def askDevConfigClient(clientIP: String, clientPort: Int): Config = {
    val ipNumbers = clientIP.split("\\.").map(_.toInt)

    val ipRaw = Array(ipNumbers(0), ipNumbers(1), ipNumbers(2), ipNumbers(3))
    val inetAddress = InetAddress.getByAddress(ipRaw.map(x => x.toByte))

    localAddress = Some(inetAddress)
    runLocalOnly = Some(true)

    MyConfiguration(localAddress.get.getHostAddress(), "", clientPort.toString)
  }

  def askDevConfigServer(): Config = {
    // the IP address inside LAN accessible by others in the same LAN. 0.0.0.0 is just all the NIC in a computer. Change this to ipconfig
    // if want LAN. ONE AS can only have one port.

    println("please enter LAN IP to bind")
    val ipRawInput = scala.io.StdIn.readLine()
    val ipNumbers = ipRawInput.split("\\.").map(_.toInt)

    val ipRaw = Array(ipNumbers(0), ipNumbers(1), ipNumbers(2), ipNumbers(3))
    val inetAddress = InetAddress.getByAddress(ipRaw.map(x => x.toByte))

    localAddress = Some(inetAddress)
    runLocalOnly = Some(true)

    println("please enter port to bind")
    val port = scala.io.StdIn.readLine.toInt
    MyConfiguration(localAddress.get.getHostAddress(), "", port.toString)
  }

  def askForConfig(): Config = {
    var count = -1
    val addresses = (for (inf <- NetworkInterface.getNetworkInterfaces.asScala;
                          add <- inf.getInetAddresses.asScala) yield {
      count = count + 1
      (count -> add)
    }).toMap
    for((i, add) <- addresses){
      println(s"$i = $add")
    }
    println("please select which interface to bind")
    var selection: Int = 0
    do {
      selection = scala.io.StdIn.readInt()
    } while(!(selection >= 0 && selection < addresses.size))

    localAddress = Option(addresses(selection))

    println("please enter port to bind")

    val port = scala.io.StdIn.readLine.toInt

    if (scala.io.StdIn.readLine("Is running locally?y/n").toUpperCase == "Y") {
      runLocalOnly = Some(true)
      MyConfiguration(localAddress.get.getHostAddress(), "", port.toString)
    } else {
      runLocalOnly = Some(false)
      MyConfiguration(scala.io.StdIn.readLine("Enter public address/domain name"),  localAddress.get.getHostAddress(), port.toString)
    }
  }

  def apply(extHostName: String, intHostName: String, port: String): Config = {
    ConfigFactory.parseString(
      s"""
         |akka {
         |  loglevel = "INFO" #INFO, DEBUG
         |  actor {
         |    # provider=remote is possible, but prefer cluster
         |    provider =  cluster
         |    allow-java-serialization=on
         |    serializers {
         |      jackson-json = "akka.serialization.jackson.JacksonJsonSerializer"
         |    }
         |    serialization-bindings {
         |      "com.hep88.protocol.JsonSerializable" = jackson-json
         |    }
         |  }
         |  remote {
         |    artery {
         |      transport = tcp # See Selecting a transport below
         |      canonical.hostname = "${extHostName}"
         |      canonical.port = ${port}
         |      bind.hostname = "${intHostName}" # internal (bind) hostname
         |      bind.port = ${port}              # internal (bind) port
         |
         |      #log-sent-messages = on
         |      #log-received-messages = on
         |    }
         |  }
         |  cluster {
         |    downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
         |  }
         |
         |  discovery {
         |    loglevel = "OFF"
         |    method = akka-dns
         |  }
         |
         |  management {
         |    loglevel = "OFF"
         |    http {
         |      hostname = "${extHostName}"
         |      port = 8558
         |      bind-hostname = "${intHostName}"
         |      bind-port = 8558
         |    }
         |  }
         |}
         """.stripMargin)
  }
}
