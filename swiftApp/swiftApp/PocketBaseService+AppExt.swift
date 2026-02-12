import Foundation
import Combine
import SwiftUI

private enum AppPocketBase {
    static let todosCollection = "todos"
    static let todoRealtimeKey = "todo-realtime"
}

extension PocketBaseService {
    func signup(email: String, password: String, passwordConfirm: String) async throws {
        let payload = SignupPayload(
            email: email,
            password: password,
            passwordConfirm: passwordConfirm,
            emailVisibility: true
        )

        let json = try encodeJSON(payload)
        _ = try await createJson(
            collection: "users",
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
            usernameOrEmail: email,
            password: password,
            expand: nil,
            fields: nil,
            query: [:],
            headers: [:]
        )

        let auth = try decodeJSON(AuthResponse<UserRecord>.self, from: json)
        let recordJson = try encodeJSON(auth.record)
        updateAuthSession(token: auth.token, authRecordJson: recordJson)
        return auth.record
    }

    func logout() async {
        await stopRealtime()
        await clearAuth()
    }

    func fetchTodos() async throws -> [TodoRecord] {
        let json = try await getListJson(
            collection: AppPocketBase.todosCollection,
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

        return try decodeJSON(ListResponse<TodoRecord>.self, from: json).items
    }

    func getTodo(id: String) async throws -> TodoRecord {
        let json = try await getOneJson(
            collection: AppPocketBase.todosCollection,
            id: id,
            expand: nil,
            fields: nil,
            query: [:],
            headers: authHeaders()
        )

        return try decodeJSON(TodoRecord.self, from: json)
    }

    func createTodo(text: String, attachmentURL: URL?) async throws {
        let userId = try requireCurrentUserId()
        let payload = TodoPayload(text: text, completed: false, attachment: nil, user: userId)
        let bodyJson = try encodeJSON(payload)
        let files = try makeFile(field: "attachment", from: attachmentURL)

        _ = try await createJson(
            collection: AppPocketBase.todosCollection,
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

        let bodyJson = try encodeJSON(payload)
        let files = try makeFile(field: "attachment", from: newAttachmentURL)

        _ = try await updateJson(
            collection: AppPocketBase.todosCollection,
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
        try await deleteRecord(
            collection: AppPocketBase.todosCollection,
            id: id,
            query: [:],
            headers: authHeaders()
        )
    }

    func startRealtime(onEvent: @escaping @Sendable () -> Void) async throws {
        await stopRealtime()
        try await subscribeJson(
            key: AppPocketBase.todoRealtimeKey,
            collection: AppPocketBase.todosCollection,
            topic: "*",
            callback: { _ in onEvent() },
            expand: nil,
            filter: nil,
            fields: nil,
            query: [:],
            headers: authHeaders()
        )
    }

    func stopRealtime() async {
        await unsubscribe(key: AppPocketBase.todoRealtimeKey)
    }

    func attachmentURL(for todo: TodoRecord) -> URL? {
        guard let attachment = todo.attachment, !attachment.isEmpty else { return nil }
        return URL(string: "\(AppConfig.serverURL)/api/files/\(AppPocketBase.todosCollection)/\(todo.id)/\(attachment)")
    }

    private func requireCurrentUserId() throws -> String {
        guard let authRecordJson else { throw ServiceError.missingAuthToken }
        let user = try decodeJSON(UserRecord.self, from: authRecordJson)
        return user.id
    }
}

@MainActor
final class AppContainer: ObservableObject {
    let service = PocketBaseService(serverURL: AppConfig.serverURL)
}
