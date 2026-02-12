import Foundation
import Combine
import PocketBase

@MainActor
final class AuthViewModel: ObservableObject {
    enum Screen {
        case login
        case signup
        case forgotPassword
    }

    @Published var screen: Screen = .login
    @Published var email = ""
    @Published var password = ""
    @Published var passwordConfirm = ""
    @Published var errorMessage: String?
    @Published var infoMessage: String?
    @Published var isLoading = false
    @Published var isLoggedIn = false

    private let service: PocketBaseService

    init(service: PocketBaseService) {
        self.service = service
        Task {
            isLoggedIn = service.isLoggedIn()
        }
    }

    func login() {
        infoMessage = nil
        errorMessage = nil
        isLoading = true

        Task {
            defer { isLoading = false }
            do {
                _ = try await service.login(email: email, password: password)
                isLoggedIn = true
            } catch {
                errorMessage = "Login failed: \(error.localizedDescription)"
            }
        }
    }

    func signup() {
        infoMessage = nil
        errorMessage = nil
        isLoading = true

        Task {
            defer { isLoading = false }
            do {
                try await service.signup(email: email, password: password, passwordConfirm: passwordConfirm)
                isLoggedIn = true
            } catch {
                errorMessage = "Signup failed: \(error.localizedDescription)"
            }
        }
    }

    func forgotPassword() {
        infoMessage = nil
        errorMessage = nil
        isLoading = true

        Task {
            defer { isLoading = false }
            do {
                try await service.requestPasswordResetForApp(email: email)
                infoMessage = "Password reset email sent."
            } catch {
                errorMessage = "Request failed: \(error.localizedDescription)"
            }
        }
    }

    func logout() {
        Task {
            await service.logout()
            isLoggedIn = false
            screen = .login
            password = ""
            passwordConfirm = ""
        }
    }
}
