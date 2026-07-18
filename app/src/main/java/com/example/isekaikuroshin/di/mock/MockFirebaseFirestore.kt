package com.example.isekaikuroshin.di.mock

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

/**
 * Mock Firebase Firestore
 * Firebase bağımlılıkları olmadan test için geçici implementasyon
 */
class MockFirebaseFirestore {

    fun collection(path: String): MockCollectionReference {
        return MockCollectionReference()
    }
}

class MockCollectionReference {

    fun document(documentId: String): MockDocumentReference {
        return MockDocumentReference()
    }

    fun get(): Task<MockQuerySnapshot> {
        return Tasks.forResult(MockQuerySnapshot())
    }
}

class MockDocumentReference {

    fun collection(path: String): MockCollectionReference {
        return MockCollectionReference()
    }

    fun set(data: Map<String, Any>): Task<Void> {
        return Tasks.forResult(null)
    }

    fun get(): Task<MockDocumentSnapshot> {
        return Tasks.forResult(MockDocumentSnapshot())
    }
}

class MockQuerySnapshot {
    val documents: List<MockDocumentSnapshot> = emptyList()
}

class MockDocumentSnapshot {
    fun getString(field: String): String? = null
    fun getLong(field: String): Long? = null
}
