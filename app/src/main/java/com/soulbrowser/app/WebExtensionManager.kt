package com.soulbrowser.app
import org.json.JSONObject
import org.mozilla.geckoview.*
object WebExtensionManager {
 private const val EXT_ID="soul-pro@soulbrowser.app"; private const val EXT_URI="resource://android/assets/extensions/soul_pro/"; private const val PORT="soulNative"
 @Volatile private var installed:WebExtension?=null
 fun ensureInstalled(runtime:GeckoRuntime,onReady:((WebExtension)->Unit)?=null){ installed?.let{onReady?.invoke(it);return}; runtime.webExtensionController.ensureBuiltIn(EXT_URI,EXT_ID).accept({e->if(e!=null){installed=e;onReady?.invoke(e)}},{}) }
 fun sendSessionCommand(session:GeckoSession?,action:String,payload:JSONObject=JSONObject(),onReply:((JSONObject?)->Unit)?=null){
  val e=installed?:return; val s=session?:return; val msg=JSONObject(payload.toString()).put("action",action)
  s.webExtensionController.setMessageDelegate(e,object:WebExtension.MessageDelegate{
   override fun onMessage(nativeApp:String,msg:Any,sender:WebExtension.MessageSender):GeckoResult<Any>? { if(msg is JSONObject)onReply?.invoke(msg); return null }
  },PORT)
  runCatching { e.setMessageDelegate(object:WebExtension.MessageDelegate{ override fun onConnect(port:WebExtension.Port){port.postMessage(msg)} },PORT) }
 }
}