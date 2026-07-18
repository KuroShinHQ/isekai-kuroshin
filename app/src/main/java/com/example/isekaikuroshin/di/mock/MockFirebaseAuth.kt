package com.example.isekaikuroshin.di.mock

/**
 * Mock Firebase Authentication
 * Firebase bağımlılıkları olmadan test için geçici implementasyon
 */
class MockFirebaseAuth {
    val currentUser: MockFirebaseUser? = MockFirebaseUser("mock-user-id")
}

class MockFirebaseUser(val uid: String)
