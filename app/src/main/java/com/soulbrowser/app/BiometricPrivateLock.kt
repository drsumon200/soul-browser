package com.soulbrowser.app
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
object BiometricPrivateLock { fun isDeviceSecured(c:Context)=(c.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure;fun createUnlockIntent(c:Context,title:String="Unlock Private Space",subtitle:String="Verify identity to view private tabs"):Intent?{val k=c.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager;if(!k.isDeviceSecure)return null;@Suppress("DEPRECATION") return k.createConfirmDeviceCredentialIntent(title,subtitle)} }