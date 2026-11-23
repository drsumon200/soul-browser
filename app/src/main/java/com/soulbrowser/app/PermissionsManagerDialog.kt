package com.soulbrowser.app
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable fun PermissionsManagerDialog(records:List<PermissionAuditRecord>,onRevoke:(PermissionAuditRecord)->Unit,onDismiss:()->Unit){
 AlertDialog(onDismissRequest=onDismiss,containerColor=SoulColors.Panel,title={Text("Site Permissions",color=SoulColors.Text)},text={
  if(records.isEmpty())Text("No saved site permissions.",color=SoulColors.Muted,fontSize=13.sp)
  else LazyColumn(Modifier.heightIn(max=320.dp)){items(records,key={it.host+"|"+it.permissionName}){r->
   Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text(r.host,color=SoulColors.Text,fontSize=13.sp);Text(r.permissionName+" · "+if(r.allowed)"Allowed" else "Denied",color=if(r.allowed)SoulColors.Mint else SoulColors.Rose,fontSize=11.sp)}
    TextButton(onClick={onRevoke(r)}){Text("Revoke",color=SoulColors.Rose)}
   }
  }}
 },confirmButton={TextButton(onClick=onDismiss){Text("Done",color=SoulColors.Lavender)}})
}