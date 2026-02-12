import Foundation
import PocketBase

enum ServiceError: Error, LocalizedError {
    case invalidResponse
    case missingAuthToken
    case invalidAttachment

    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid server response."
        case .missingAuthToken: return "You must be logged in."
        case .invalidAttachment: return "Could not read selected file."
        }
    }
}

actor PocketBaseService {
    private struct StoredAuthSession: Codable {
        let token: String
        let authRecordJson: String?
    }

    private let client: ApplePocketBaseClient
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()
    private let authCollection: String
    private let sessionKey: String

    private(set) var authToken: String?
    private(set) var authRecordJson: String?
    private var subscriptions: [String: AppleSubscription] = [:]

    init(
        serverURL: String,
        authCollection: String = "users",
        sessionKey: String = "pocketbase.session"
    ) {
        self.authCollection = authCollection
        self.sessionKey = sessionKey
        client = ApplePocketBaseClient(url: serverURL)

        if
            let data = UserDefaults.standard.data(forKey: sessionKey),
            let session = try? decoder.decode(StoredAuthSession.self, from: data)
        {
            authToken = session.token
            authRecordJson = session.authRecordJson
        }
    }

    func isLoggedIn() -> Bool {
        authToken != nil
    }

    func authHeaders() -> [String: String] {
        guard let token = authToken else { return [:] }
        return ["Authorization": "Bearer \(token)"]
    }

    func updateAuthSession(token: String, authRecordJson: String?) {
        self.authToken = token
        self.authRecordJson = authRecordJson
        persistSession(token: token, authRecordJson: authRecordJson)
    }

    func clearAuth() async {
        for (_, subscription) in subscriptions {
            try? await subscription.cancel()
        }
        subscriptions.removeAll()

        client.clearAuth()
        authToken = nil
        authRecordJson = nil
        UserDefaults.standard.removeObject(forKey: sessionKey)
    }

    func authWithPasswordJson(
        usernameOrEmail: String,
        password: String,
        expand: String? = nil,
        fields: String? = nil,
        query: [String: String] = [:],
        headers: [String: String] = [:]
    ) async throws -> String {
        try await bridge(for: authCollection).authWithPasswordJson(
            usernameOrEmail: usernameOrEmail,
            password: password,
            expand: expand,
            fields: fields,
            query: query,
            headers: headers
        )
    }

    func requestPasswordReset(
        email: String,
        query: [String: String] = [:],
        headers: [String: String] = [:]
    ) async throws {
        try await bridge(for: authCollection).requestPasswordReset(
            email: email,
            query: query,
            headers: headers
        )
    }

    func createJson(
        collection: String,
        bodyJson: String?,
        expand: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String],
        files: [PocketBase.File]
    ) async throws -> String {
        try await bridge(for: collection).createJson(
            bodyJson: bodyJson,
            expand: expand,
            fields: fields,
            query: query,
            headers: headers,
            files: files
        )
    }

    func updateJson(
        collection: String,
        id: String,
        bodyJson: String?,
        expand: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String],
        files: [PocketBase.File]
    ) async throws -> String {
        try await bridge(for: collection).updateJson(
            id: id,
            bodyJson: bodyJson,
            expand: expand,
            fields: fields,
            query: query,
            headers: headers,
            files: files
        )
    }

    func getListJson(
        collection: String,
        page: Int32,
        perPage: Int32,
        skipTotal: Bool,
        expand: String?,
        filter: String?,
        sort: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String]
    ) async throws -> String {
        try await bridge(for: collection).getListJson(
            page: page,
            perPage: perPage,
            skipTotal: skipTotal,
            expand: expand,
            filter: filter,
            sort: sort,
            fields: fields,
            query: query,
            headers: headers
        )
    }

    func getOneJson(
        collection: String,
        id: String,
        expand: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String]
    ) async throws -> String {
        try await bridge(for: collection).getOneJson(
            id: id,
            expand: expand,
            fields: fields,
            query: query,
            headers: headers
        )
    }

    func deleteRecord(
        collection: String,
        id: String,
        query: [String: String],
        headers: [String: String]
    ) async throws {
        try await bridge(for: collection).deleteRecord(id: id, query: query, headers: headers)
    }

    func subscribeJson(
        key: String,
        collection: String,
        topic: String,
        callback: @escaping (String) -> Void,
        expand: String?,
        filter: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String]
    ) async throws {
        if let existing = subscriptions.removeValue(forKey: key) {
            try? await existing.cancel()
        }

        let subscription = try await bridge(for: collection).subscribeJson(
            topic: topic,
            callback: callback,
            expand: expand,
            filter: filter,
            fields: fields,
            query: query,
            headers: headers
        )

        subscriptions[key] = subscription
    }

    func unsubscribe(key: String) async {
        guard let existing = subscriptions.removeValue(forKey: key) else { return }
        try? await existing.cancel()
    }

    func decodeJSON<T: Decodable>(_ type: T.Type, from json: String) throws -> T {
        try decoder.decode(type, from: Data(json.utf8))
    }

    func encodeJSON<T: Encodable>(_ value: T) throws -> String {
        let data = try encoder.encode(value)
        return String(decoding: data, as: UTF8.self)
    }

    func makeFile(field: String, from fileURL: URL?) throws -> [PocketBase.File] {
        guard let fileURL else { return [] }
        let data = try Data(contentsOf: fileURL)
        let fileName = fileURL.lastPathComponent
        guard !fileName.isEmpty else { throw ServiceError.invalidAttachment }
        let kotlinData = data.toKotlinByteArray()
        return [PocketBase.File(field: field, data: kotlinData, fileName: fileName)]
    }

    private func bridge(for collection: String) -> AppleRecordBridge {
        client.collection(idOrName: collection)
    }

    private func persistSession(token: String, authRecordJson: String?) {
        let payload = StoredAuthSession(token: token, authRecordJson: authRecordJson)
        if let data = try? encoder.encode(payload) {
            UserDefaults.standard.set(data, forKey: sessionKey)
        }
    }
}

private extension Data {
    nonisolated func toKotlinByteArray() -> KotlinByteArray {
        let kotlinArray = KotlinByteArray(size: Int32(count))
        for (idx, byte) in enumerated() {
            kotlinArray.set(index: Int32(idx), value: Int8(bitPattern: byte))
        }
        return kotlinArray
    }
}
