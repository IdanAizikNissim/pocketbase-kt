import Foundation
import Combine
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
    private let client: ApplePocketBaseClient
    private let usersBridge: AppleRecordBridge
    private let todosBridge: AppleRecordBridge
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()
    private let serverURL: String
    private let sessionKey = "swiftapp.pb.session"

    private(set) var authToken: String?
    private(set) var currentUser: UserRecord?
    private var todoSubscription: AppleSubscription?

    init(serverURL: String) {
        self.serverURL = serverURL
        client = ApplePocketBaseClient(url: serverURL)
        usersBridge = client.collection(idOrName: "users")
        todosBridge = client.collection(idOrName: "todos")
        if
            let data = UserDefaults.standard.data(forKey: sessionKey),
            let session = try? decoder.decode(StoredSession.self, from: data)
        {
            authToken = session.token
            currentUser = session.user
        }
    }

    func isLoggedIn() -> Bool {
        authToken != nil
    }

    func signup(email: String, password: String, passwordConfirm: String) async throws {
        let payload = SignupPayload(
            email: email,
            password: password,
            passwordConfirm: passwordConfirm,
            emailVisibility: true
        )
        let body = try encoder.encode(payload)
        let json = String(decoding: body, as: UTF8.self)
        _ = try await createJson(
            bridge: usersBridge,
            bodyJson: json,
            expand: nil,
            fields: nil,
            query: [:],
            headers: [:],
            files: []
        )
        _ = try await login(email: email, password: password)
    }

    @discardableResult
    func login(email: String, password: String) async throws -> UserRecord {
        let json = try await authWithPasswordJson(
            bridge: usersBridge,
            usernameOrEmail: email,
            password: password,
            expand: nil,
            fields: nil,
            query: [:],
            headers: [:]
        )
        let auth = try decoder.decode(AuthResponse<UserRecord>.self, from: Data(json.utf8))
        authToken = auth.token
        currentUser = auth.record
        persistSession(token: auth.token, user: auth.record)
        return auth.record
    }

    func requestPasswordReset(email: String) async throws {
        try await requestPasswordReset(
            bridge: usersBridge,
            email: email,
            query: [:],
            headers: [:]
        )
    }

    func logout() async {
        await stopRealtime()
        client.clearAuth()
        authToken = nil
        currentUser = nil
        UserDefaults.standard.removeObject(forKey: sessionKey)
    }

    func fetchTodos() async throws -> [TodoRecord] {
        let json = try await getListJson(
            bridge: todosBridge,
            page: 1,
            perPage: 50,
            skipTotal: false,
            expand: nil,
            filter: nil,
            sort: "-created",
            fields: nil,
            query: [:],
            headers: authHeaders()
        )
        return try decoder.decode(ListResponse<TodoRecord>.self, from: Data(json.utf8)).items
    }

    func getTodo(id: String) async throws -> TodoRecord {
        let json = try await getOneJson(
            bridge: todosBridge,
            id: id,
            expand: nil,
            fields: nil,
            query: [:],
            headers: authHeaders()
        )
        return try decoder.decode(TodoRecord.self, from: Data(json.utf8))
    }

    func createTodo(text: String, attachmentURL: URL?) async throws {
        let userId = try requireUserId()
        let payload = TodoPayload(text: text, completed: false, attachment: nil, user: userId)
        let body = try encoder.encode(payload)
        let bodyJson = String(decoding: body, as: UTF8.self)
        let files = try makeFiles(attachmentURL: attachmentURL)
        _ = try await createJson(
            bridge: todosBridge,
            bodyJson: bodyJson,
            expand: nil,
            fields: nil,
            query: [:],
            headers: authHeaders(),
            files: files
        )
    }

    func updateTodo(
        todo: TodoRecord,
        text: String,
        completed: Bool,
        newAttachmentURL: URL?,
        deleteAttachment: Bool
    ) async throws {
        let payload = TodoPayload(
            text: text,
            completed: completed,
            attachment: deleteAttachment ? "" : nil,
            user: todo.user
        )
        let body = try encoder.encode(payload)
        let bodyJson = String(decoding: body, as: UTF8.self)
        let files = try makeFiles(attachmentURL: newAttachmentURL)

        _ = try await updateJson(
            bridge: todosBridge,
            id: todo.id,
            bodyJson: bodyJson,
            expand: nil,
            fields: nil,
            query: [:],
            headers: authHeaders(),
            files: files
        )
    }

    func toggleTodo(_ todo: TodoRecord) async throws {
        try await updateTodo(
            todo: todo,
            text: todo.text,
            completed: !todo.completed,
            newAttachmentURL: nil,
            deleteAttachment: false
        )
    }

    func deleteTodo(id: String) async throws {
        try await deleteRecord(bridge: todosBridge, id: id, query: [:], headers: authHeaders())
    }

    func startRealtime(onEvent: @escaping @Sendable () -> Void) async throws {
        await stopRealtime()
        todoSubscription = try await subscribeJson(
            bridge: todosBridge,
            topic: "*",
            callback: { _ in
                onEvent()
            },
            expand: nil,
            filter: nil,
            fields: nil,
            query: [:],
            headers: authHeaders()
        )
    }

    func stopRealtime() async {
        if let sub = todoSubscription {
            try? await cancel(subscription: sub)
            todoSubscription = nil
        }
    }

    func attachmentURL(for todo: TodoRecord) -> URL? {
        guard let attachment = todo.attachment, !attachment.isEmpty else { return nil }
        return URL(string: "\(serverURL)/api/files/todos/\(todo.id)/\(attachment)")
    }

    private func authHeaders() -> [String: String] {
        guard let token = authToken else { return [:] }
        return ["Authorization": "Bearer \(token)"]
    }

    private func requireUserId() throws -> String {
        guard let userId = currentUser?.id else {
            throw ServiceError.missingAuthToken
        }
        return userId
    }

    private func makeFiles(attachmentURL: URL?) throws -> [PocketBase.File] {
        guard let attachmentURL else { return [] }
        let data = try Data(contentsOf: attachmentURL)
        let fileName = attachmentURL.lastPathComponent
        guard !fileName.isEmpty else { throw ServiceError.invalidAttachment }
        let kotlinData = data.toKotlinByteArray()
        let file = PocketBase.File(field: "attachment", data: kotlinData, fileName: fileName)
        return [file]
    }

    private func persistSession(token: String, user: UserRecord) {
        let payload = StoredSession(token: token, user: user)
        if let data = try? encoder.encode(payload) {
            UserDefaults.standard.set(data, forKey: sessionKey)
        }
    }

    private func authWithPasswordJson(
        bridge: AppleRecordBridge,
        usernameOrEmail: String,
        password: String,
        expand: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String]
    ) async throws -> String {
        try await bridge.authWithPasswordJson(
            usernameOrEmail: usernameOrEmail,
            password: password,
            expand: expand,
            fields: fields,
            query: query,
            headers: headers
        )
    }

    private func createJson(
        bridge: AppleRecordBridge,
        bodyJson: String?,
        expand: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String],
        files: [PocketBase.File]
    ) async throws -> String {
        try await bridge.createJson(
            bodyJson: bodyJson,
            expand: expand,
            fields: fields,
            query: query,
            headers: headers,
            files: files
        )
    }

    private func updateJson(
        bridge: AppleRecordBridge,
        id: String,
        bodyJson: String?,
        expand: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String],
        files: [PocketBase.File]
    ) async throws -> String {
        try await bridge.updateJson(
            id: id,
            bodyJson: bodyJson,
            expand: expand,
            fields: fields,
            query: query,
            headers: headers,
            files: files
        )
    }

    private func getListJson(
        bridge: AppleRecordBridge,
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
        try await bridge.getListJson(
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

    private func getOneJson(
        bridge: AppleRecordBridge,
        id: String,
        expand: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String]
    ) async throws -> String {
        try await bridge.getOneJson(
            id: id,
            expand: expand,
            fields: fields,
            query: query,
            headers: headers
        )
    }

    private func deleteRecord(
        bridge: AppleRecordBridge,
        id: String,
        query: [String: String],
        headers: [String: String]
    ) async throws {
        try await bridge.deleteRecord(id: id, query: query, headers: headers)
    }

    private func requestPasswordReset(
        bridge: AppleRecordBridge,
        email: String,
        query: [String: String],
        headers: [String: String]
    ) async throws {
        try await bridge.requestPasswordReset(email: email, query: query, headers: headers)
    }

    private func subscribeJson(
        bridge: AppleRecordBridge,
        topic: String,
        callback: @escaping (String) -> Void,
        expand: String?,
        filter: String?,
        fields: String?,
        query: [String: String],
        headers: [String: String]
    ) async throws -> AppleSubscription {
        try await bridge.subscribeJson(
            topic: topic,
            callback: callback,
            expand: expand,
            filter: filter,
            fields: fields,
            query: query,
            headers: headers
        )
    }

    private func cancel(subscription: AppleSubscription) async throws {
        try await subscription.cancel()
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

@MainActor
final class AppContainer: ObservableObject {
    let service = PocketBaseService(serverURL: AppConfig.serverURL)
}
