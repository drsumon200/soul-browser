package com.soulbrowser.app
import android.content.Context
import java.security.MessageDigest
object AppLockPinStore {
 private const val PREFS="soul_lock_pin";private const val KEY="pin_hash";private const val SALT="soul_v1_salt_"
 fun hasPin(c:Context)=!c.getSharedPreferences(PREFS,0).getString(KEY,null).isNullOrBlank()
 fun setPin(c:Context,pin:String){c.getSharedPreferences(PREFS,0).edit().putString(KEY,hash(SALT+pin)).apply()}
 fun verifyPin(c:Context,candidate:String):Boolean{val saved=c.getSharedPreferences(PREFS,0).getString(KEY,null)?:return false;return MessageDigest.isEqual(saved.toByteArray(),hash(SALT+candidate).toByteArray())}
 fun clearPin(c:Context){c.getSharedPreferences(PREFS,0).edit().remove(KEY).apply()}
 private fun hash(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it)}
}