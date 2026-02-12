import Foundation
import Combine
import SwiftUI
import PocketBase

private enum AppPocketBase {
    static let usersCollection = "users"
    static let todosCollection = "todos"
}

extension PocketBase.ApplePocketBaseService {
    func signup(email: String, password: String, passwordConfirm: String) async throws {
        let payload = SignupPayload(
            email: email,
            password: password,
            passwordConfirm: passwordConfirm,
            emailVisibility: true
        )

        let body = try encodeJSON(payload)
        _ = try await send(
            path: "/api/collections/\(AppPocketBase.usersCollection)/records",
            method: "POST",
            body: Data(body.utf8),
            contentType: "application/json",
            useAuth: false
        )

        _ = try await login(email: email, password: password)
    }

    @discardableResult
    func login(email: String, password: String) async throws -> UserRecord {
        let body = try JSONEncoder().encode(["identity": email, "password": password])
        let data = try await send(
            path: "/api/collections/\(AppPocketBase.usersCollection)/auth-with-password",
            method: "POST",
            body: body,
            contentType: "application/json",
            useAuth: false
        )

        let auth = try JSONDecoder().decode(AuthResponse<UserRecord>.self, from: data)
        let recordJson = try encodeJSON(auth.record)
        updateAuthSession(token: auth.token, authRecordJson: recordJson)
        return auth.record
    }

    func logout() async {
        updateAuthSession(token: "", authRecordJson: nil)
    }

    func requestPasswordResetForApp(email: String) async throws {
        let body = try JSONEncoder().encode(["email": email])
        _ = try await send(
            path: "/api/collections/\(AppPocketBase.usersCollection)/request-password-reset",
            method: "POST",
            body: body,
            contentType: "application/json",
            useAuth: false
        )
    }

    func fetchTodos() async throws -> [TodoRecord] {
        let data = try await send(
            path: "/api/collections/\(AppPocketBase.todosCollection)/records",
            method: "GET",
            query: [
                URLQueryItem(name: "page", value: "1"),
                URLQueryItem(name: "perPage", value: "50"),
                URLQueryItem(name: "sort", value: "-created"),
            ],
            useAuth: true
        )

        return try JSONDecoder().decode(ListResponse<TodoRecord>.self, from: data).items
    }

    func getTodo(id: String) async throws -> TodoRecord {
        let data = try await send(
            path: "/api/collections/\(AppPocketBase.todosCollection)/records/\(id)",
            method: "GET",
            useAuth: true
        )

        return try JSONDecoder().decode(TodoRecord.self, from: data)
    }

    func createTodo(text: String, attachmentURL: URL?) async throws {
        let userId = try requireCurrentUserId()
        let payload = TodoPayload(text: text, completed: false, attachment: nil, user: userId)
        let bodyJson = try encodeJSON(payload)

        if let attachmentURL {
            let attachment = try loadAttachment(from: attachmentURL)
            let multipart = makeMultipartBody(
                jsonPayload: bodyJson,
                fileField: "attachment",
                fileName: attachment.fileName,
                fileData: attachment.data
            )

            _ = try await send(
                path: "/api/collections/\(AppPocketBase.todosCollection)/records",
                method: "POST",
                body: multipart.body,
                contentType: multipart.contentType,
                useAuth: true
            )
        } else {
            _ = try await send(
                path: "/api/collections/\(AppPocketBase.todosCollection)/records",
                method: "POST",
                body: Data(bodyJson.utf8),
                contentType: "application/json",
                useAuth: true
            )
        }
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

        if let newAttachmentURL {
            let attachment = try loadAttachment(from: newAttachmentURL)
            let multipart = makeMultipartBody(
                jsonPayload: bodyJson,
                fileField: "attachment",
                fileName: attachment.fileName,
                fileData: attachment.data
            )

            _ = try await send(
                path: "/api/collections/\(AppPocketBase.todosCollection)/records/\(todo.id)",
                method: "PATCH",
                body: multipart.body,
                contentType: multipart.contentType,
                useAuth: true
            )
        } else {
            _ = try await send(
                path: "/api/collections/\(AppPocketBase.todosCollection)/records/\(todo.id)",
                method: "PATCH",
                body: Data(bodyJson.utf8),
                contentType: "application/json",
                useAuth: true
            )
        }
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
        _ = try await send(
            path: "/api/collections/\(AppPocketBase.todosCollection)/records/\(id)",
            method: "DELETE",
            useAuth: true
        )
    }

    func startRealtime(onEvent: @escaping @Sendable () -> Void) async throws {
        _ = onEvent
    }

    func stopRealtime() async {}

    func attachmentURL(for todo: TodoRecord) -> URL? {
        guard let attachment = todo.attachment, !attachment.isEmpty else { return nil }
        return URL(string: "\(AppConfig.serverURL)/api/files/\(AppPocketBase.todosCollection)/\(todo.id)/\(attachment)")
    }

    private func requireCurrentUserId() throws -> String {
        guard let authRecordJson = authRecordJson() else { throw ServiceError.missingAuthToken }
        let user = try decodeJSON(UserRecord.self, from: authRecordJson)
        return user.id
    }

    private func send(
        path: String,
        method: String,
        query: [URLQueryItem] = [],
        body: Data? = nil,
        contentType: String? = nil,
        useAuth: Bool = true
    ) async throws -> Data {
        guard var components = URLComponents(string: "\(AppConfig.serverURL)\(path)") else {
            throw ServiceError.invalidResponse
        }

        if !query.isEmpty {
            components.queryItems = query
        }

        guard let url = components.url else {
            throw ServiceError.invalidResponse
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.httpBody = body

        if let contentType {
            request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        }

        if useAuth, let auth = authHeaders()["Authorization"], !auth.isEmpty {
            request.setValue(auth, forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200 ... 299).contains(http.statusCode) else {
            throw ServiceError.invalidResponse
        }

        return data
    }

    private func loadAttachment(from fileURL: URL) throws -> (data: Data, fileName: String) {
        let data = try Data(contentsOf: fileURL)
        let fileName = fileURL.lastPathComponent
        guard !fileName.isEmpty else { throw ServiceError.invalidAttachment }
        return (data, fileName)
    }

    private func makeMultipartBody(
        jsonPayload: String,
        fileField: String,
        fileName: String,
        fileData: Data
    ) -> (body: Data, contentType: String) {
        let boundary = "Boundary-\(UUID().uuidString)"
        var data = Data()

        func append(_ string: String) {
            data.append(Data(string.utf8))
        }

        append("--\(boundary)\r\n")
        append("Content-Disposition: form-data; name=\"@jsonPayload\"; filename=\"payload.json\"\r\n")
        append("Content-Type: application/json\r\n\r\n")
        append(jsonPayload)
        append("\r\n")

        append("--\(boundary)\r\n")
        append("Content-Disposition: form-data; name=\"\(fileField)\"; filename=\"\(fileName)\"\r\n")
        append("Content-Type: application/octet-stream\r\n\r\n")
        data.append(fileData)
        append("\r\n")

        append("--\(boundary)--\r\n")

        return (data, "multipart/form-data; boundary=\(boundary)")
    }
}

@MainActor
final class AppContainer: ObservableObject {
    let service = PocketBaseService(
        url: AppConfig.serverURL,
        authCollection: "users",
        sessionKey: "pocketbase.session"
    )
}
