import Foundation

struct UserRecord: Codable, Identifiable, Equatable {
    let id: String
    let email: String?
    let name: String?
    let avatar: String?
}

struct SignupPayload: Codable {
    let email: String
    let password: String
    let passwordConfirm: String
    let emailVisibility: Bool
}

struct TodoRecord: Codable, Identifiable, Equatable {
    let id: String
    let created: String?
    let updated: String?
    let collectionId: String?
    let collectionName: String?
    let text: String
    let completed: Bool
    let attachment: String?
    let user: String
}

struct TodoPayload: Codable {
    let text: String
    let completed: Bool
    let attachment: String?
    let user: String
}

struct ListResponse<T: Codable>: Codable {
    let items: [T]
}

struct AuthResponse<T: Codable>: Codable {
    let token: String
    let record: T
}

struct StoredSession: Codable {
    let token: String
    let user: UserRecord
}
