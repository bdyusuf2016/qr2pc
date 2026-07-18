package com.yusuftech.qr2pc.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseManager {
    // Explicit URL for asia-southeast1 region
    private val database = FirebaseDatabase.getInstance("https://qr2pc-72742-default-rtdb.asia-southeast1.firebasedatabase.app/")

    fun sendScanResult(pairingId: String, result: String, onResult: (Boolean) -> Unit) {
        val userScansRef = database.getReference("scans/$pairingId")
        // Generate a unique key for the scan
        val scanId = userScansRef.push().key ?: run {
            onResult(false)
            return
        }
        
        // Data structure matching the Python listener's expectation
        val scanData = mapOf(
            "text" to result,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis(),
            "pairingId" to pairingId
        )

        // Write directly to scans/{pairingId}/{scanId} and check for success
        userScansRef.child(scanId).setValue(scanData)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    fun getServerLastSeen(pairingId: String): Flow<Long?> = callbackFlow {
        val serverRef = database.getReference("servers/$pairingId/lastSeen")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Long::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                // Ignore errors
            }
        }
        serverRef.addValueEventListener(listener)
        awaitClose { serverRef.removeEventListener(listener) }
    }
}
