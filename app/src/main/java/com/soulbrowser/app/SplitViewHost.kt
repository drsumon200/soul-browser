package com.soulbrowser.app
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
@Composable fun SplitViewHost(primarySession:GeckoSession,secondarySession:GeckoSession,splitRatio:Float,focusedPane:Int,onFocusPane:(Int)->Unit,onDragRatioDelta:(Float)->Unit,modifier:Modifier=Modifier){
 Column(modifier.fillMaxSize()){
  Box(Modifier.weight(splitRatio.coerceIn(.25f,.75f)).fillMaxWidth().clickable{onFocusPane(0)}){
   AndroidView(Modifier.fillMaxSize(),factory={c->GeckoView(c).apply{coverUntilFirstPaint(0xFF101116.toInt());setSession(primarySession)}},update={v->if(v.session!==primarySession){v.releaseSession();v.setSession(primarySession)}},onRelease={it.releaseSession()})
  }
  Box(Modifier.fillMaxWidth().height(14.dp).background(SoulColors.Panel).pointerInput(Unit){detectDragGestures{change,drag->change.consume();onDragRatioDelta(drag.y/1400f)}},contentAlignment=Alignment.Center){Box(Modifier.width(42.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if(focusedPane==1)SoulColors.Mint else SoulColors.Lavender))}
  Box(Modifier.weight(1f-splitRatio.coerceIn(.25f,.75f)).fillMaxWidth().clickable{onFocusPane(1)}){
   AndroidView(Modifier.fillMaxSize(),factory={c->GeckoView(c).apply{coverUntilFirstPaint(0xFF101116.toInt());setSession(secondarySession)}},update={v->if(v.session!==secondarySession){v.releaseSession();v.setSession(secondarySession)}},onRelease={it.releaseSession()})
  }
 }
}