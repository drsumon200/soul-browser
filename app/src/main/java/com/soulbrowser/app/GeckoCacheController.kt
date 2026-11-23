package com.soulbrowser.app
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.StorageController
object GeckoCacheController {
 fun clearAllCaches(r:GeckoRuntime,onDone:()->Unit={}){r.storageController.clearData(StorageController.ClearFlags.ALL_CACHES).accept({onDone()},{onDone()})}
 fun clearCookiesAndStorageForHost(r:GeckoRuntime,host:String,onDone:()->Unit={}){val h=host.trim().lowercase();if(h.isBlank()||h.contains('/')||h.contains('@')){onDone();return};val f=StorageController.ClearFlags.COOKIES or StorageController.ClearFlags.DOM_STORAGES or StorageController.ClearFlags.ALL_CACHES;r.storageController.clearDataFromHost(h,f).accept({onDone()},{onDone()})}
 fun nukeAllBrowsingData(r:GeckoRuntime,onDone:()->Unit={}){r.storageController.clearData(StorageController.ClearFlags.ALL).accept({onDone()},{onDone()})}
}