package com.soulbrowser.app
import android.graphics.Bitmap
import android.util.LruCache
import org.mozilla.geckoview.GeckoView
object TabThumbnailCache { private val cache=object:LruCache<String,Bitmap>(16384){override fun sizeOf(k:String,v:Bitmap)=(v.byteCount/1024).coerceAtLeast(1)};fun get(id:String)=cache.get(id);fun put(id:String,b:Bitmap){cache.put(id,b)};fun remove(id:String){cache.remove(id)};fun captureFromView(id:String,v:GeckoView?,done:(Bitmap?)->Unit={}){if(v==null){done(null);return};runCatching{v.capturePixels().accept({b->if(b==null){done(null)}else{val scaled=Bitmap.createScaledBitmap(b,(b.width/3).coerceAtLeast(180),(b.height/3).coerceAtLeast(240),true);if(scaled!==b)b.recycle();put(id,scaled);done(scaled)}},{done(null)})}.onFailure{done(null)}} }