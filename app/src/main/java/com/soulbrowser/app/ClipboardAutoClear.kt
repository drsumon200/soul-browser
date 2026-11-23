package com.soulbrowser.app
import android.content.*
import kotlinx.coroutines.*
object ClipboardAutoClear { private var job:Job?=null;@Synchronized fun copySensitive(context:Context,scope:CoroutineScope,text:String,clearAfterMs:Long=60000L){val cm=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;cm.setPrimaryClip(ClipData.newPlainText("Soul Private",text));job?.cancel();job=scope.launch{delay(clearAfterMs.coerceAtLeast(0));if(cm.primaryClip?.getItemAt(0)?.text?.toString()==text)cm.setPrimaryClip(ClipData.newPlainText("",""))}} }