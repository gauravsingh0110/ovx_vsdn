-- MySQL dump 10.13  Distrib 5.5.43, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: vsdn_debugger
-- ------------------------------------------------------
-- Server version	5.5.43-0ubuntu0.14.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `FlowMod_Entries`
--

DROP TABLE IF EXISTS `FlowMod_Entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FlowMod_Entries` (
  `Match_dl_type` varchar(45) DEFAULT NULL,
  `Match_dl_vlan` varchar(45) DEFAULT NULL,
  `Match_dl_vlan_priority` varchar(45) DEFAULT NULL,
  `Match_in_port` varchar(45) DEFAULT NULL,
  `Match_nw_dst` varchar(45) DEFAULT NULL,
  `Match_nw_src` varchar(45) DEFAULT NULL,
  `Match_nw_protocol` varchar(45) DEFAULT NULL,
  `Match_nw_tos` varchar(45) DEFAULT NULL,
  `Match_tp_dst` varchar(45) DEFAULT NULL,
  `Match_tp_src` varchar(45) DEFAULT NULL,
  `Match_dl_dst` varchar(45) DEFAULT NULL,
  `Match_dl_src` varchar(45) DEFAULT NULL,
  `Actions` varchar(1024) DEFAULT NULL,
  `TimeStamp` varchar(128) DEFAULT NULL,
  `vSwtich_ID` varchar(45) DEFAULT NULL,
  `Tenand_ID` varchar(45) DEFAULT NULL,
  `pSwitch_ID` varchar(45) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FlowMod_Entries`
--

LOCK TABLES `FlowMod_Entries` WRITE;
/*!40000 ALTER TABLE `FlowMod_Entries` DISABLE KEYS */;
/*!40000 ALTER TABLE `FlowMod_Entries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FlowRem_Entries`
--

DROP TABLE IF EXISTS `FlowRem_Entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FlowRem_Entries` (
  `Match_dl_type` varchar(45) DEFAULT NULL,
  `Match_dl_vlan` varchar(45) DEFAULT NULL,
  `Match_dl_vlan_priority` varchar(45) DEFAULT NULL,
  `Match_in_port` varchar(45) DEFAULT NULL,
  `Match_nw_dst` varchar(45) DEFAULT NULL,
  `Match_nw_src` varchar(45) DEFAULT NULL,
  `Match_nw_protocol` varchar(45) DEFAULT NULL,
  `Match_nw_tos` varchar(45) DEFAULT NULL,
  `Match_tp_dst` varchar(45) DEFAULT NULL,
  `Match_tp_src` varchar(45) DEFAULT NULL,
  `Match_dl_dst` varchar(45) DEFAULT NULL,
  `Match_dl_src` varchar(45) DEFAULT NULL,
  `Cookie` varchar(45) DEFAULT NULL,
  `Priority` varchar(45) DEFAULT NULL,
  `Duration` varchar(45) DEFAULT NULL,
  `TimeStamp` varchar(128) DEFAULT NULL,
  `vSwitch_ID` varchar(45) DEFAULT NULL,
  `Tenant_ID` varchar(45) DEFAULT NULL,
  `pSwitch_ID` varchar(45) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FlowRem_Entries`
--

LOCK TABLES `FlowRem_Entries` WRITE;
/*!40000 ALTER TABLE `FlowRem_Entries` DISABLE KEYS */;
/*!40000 ALTER TABLE `FlowRem_Entries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Host_Entries`
--

DROP TABLE IF EXISTS `Host_Entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Host_Entries` (
  `Physical_IP` varchar(45) DEFAULT NULL,
  `MAC` varchar(45) DEFAULT NULL,
  `pSwitch_ID` varchar(45) DEFAULT NULL,
  `pSwitch_Port` varchar(45) DEFAULT NULL,
  `vTenant_ID` varchar(45) DEFAULT NULL,
  `vHost_ID` varchar(45) DEFAULT NULL,
  `Virtual_IP` varchar(45) DEFAULT NULL,
  `vSwitch_ID` varchar(45) DEFAULT NULL,
  `vSwitch_Port` varchar(45) DEFAULT NULL,
  `FirstSeen` bigint(20) DEFAULT NULL,
  `LastSeen` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Host_Entries`
--

LOCK TABLES `Host_Entries` WRITE;
/*!40000 ALTER TABLE `Host_Entries` DISABLE KEYS */;
/*!40000 ALTER TABLE `Host_Entries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Link_Entries`
--

DROP TABLE IF EXISTS `Link_Entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Link_Entries` (
  `Edge_Link` varchar(45) DEFAULT NULL,
  `Src_Switch_ID` varchar(45) DEFAULT NULL,
  `Src_Switch_Port` varchar(45) DEFAULT NULL,
  `Dst_Switch_ID` varchar(45) DEFAULT NULL,
  `Dst_Switch_Port` varchar(45) DEFAULT NULL,
  `FirstSeen` bigint(20) DEFAULT NULL,
  `LastSeen` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Link_Entries`
--

LOCK TABLES `Link_Entries` WRITE;
/*!40000 ALTER TABLE `Link_Entries` DISABLE KEYS */;
/*!40000 ALTER TABLE `Link_Entries` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-06-25 15:14:04
