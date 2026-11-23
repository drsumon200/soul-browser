package com.soulbrowser.app
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
object AdblockFilterEngine {
 private val blocked=ConcurrentHashMap.newKeySet<String>().apply { addAll(listOf("doubleclick.net","googlesyndication.com","googleadservices.com","adservice.google.com","pagead2.googlesyndication.com","ads.facebook.com","an.facebook.com","connect.facebook.net","analytics.google.com","google-analytics.com","googletagmanager.com","adnxs.com","taboola.com","outbrain.com","criteo.com","pubmatic.com","rubiconproject.com","openx.net","amazon-adsystem.com","scorecardresearch.com","quantserve.com","hotjar.com","mixpanel.com","segment.io","appsflyer.com","adjust.com","branch.io","moatads.com")) }
 private val allowed=ConcurrentHashMap.newKeySet<String>(); private val count=AtomicInteger()
 fun isBlocked(requestUrl:String,pageHost:String?=null):Boolean {
  val page=pageHost?.lowercase()?.trim('.')
  if(page!=null && allowed.any { page==it || page.endsWith("."+it) }) return false
  var host=runCatching { URI(requestUrl).host?.lowercase() }.getOrNull() ?: return false
  while(host.isNotEmpty()){ if(host in blocked){count.incrementAndGet();return true}; val i=host.indexOf('.'); if(i<0)break; host=host.substring(i+1) }
  return false
 }
 fun addCustomRule(domain:String){ val d=domain.trim().lowercase().removePrefix("||").substringBefore('^').substringBefore('/'); if(d.contains('.') && d.matches(Regex("[a-z0-9.-]+"))) blocked.add(d) }
 fun setSiteAllowlisted(host:String,allowlisted:Boolean){ val h=host.trim().lowercase().removePrefix("www."); if(h.isNotBlank()) if(allowlisted)allowed.add(h) else allowed.remove(h) }
 fun isSiteAllowlisted(host:String)=host.trim().lowercase().removePrefix("www.") in allowed
 fun totalBlocked()=count.get()
 fun importHostsList(rawText:String):Int { var added=0; rawText.lineSequence().forEach { val l=it.trim(); if(l.isEmpty()||l.startsWith("#")||l.startsWith("!"))return@forEach; val h=l.split(Regex("""\s+""")).lastOrNull()?.lowercase().orEmpty(); if(h.contains('.')&&h.matches(Regex("[a-z0-9.-]+"))&&!h.startsWith("127.")&&h!="localhost"&&blocked.add(h))added++ }; return added }
}