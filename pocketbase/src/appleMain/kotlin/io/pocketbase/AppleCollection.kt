package io.pocketbase

import io.pocketbase.apple.AppleRecordBridge

fun PocketBase.appleCollection(idOrName: String): AppleRecordBridge = AppleRecordBridge(this, idOrName)
