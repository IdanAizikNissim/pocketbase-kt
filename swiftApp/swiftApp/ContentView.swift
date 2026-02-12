import SwiftUI
import UniformTypeIdentifiers

struct ContentView: View {
    @EnvironmentObject private var container: AppContainer

    var body: some View {
        RootContentView(service: container.service)
    }
}

private struct RootContentView: View {
    @StateObject private var authViewModel: AuthViewModel
    @StateObject private var todoViewModel: TodoViewModel

    init(service: PocketBaseService) {
        _authViewModel = StateObject(wrappedValue: AuthViewModel(service: service))
        _todoViewModel = StateObject(wrappedValue: TodoViewModel(service: service))
    }

    var body: some View {
        Group {
            if authViewModel.isLoggedIn {
                TodoRootView(authViewModel: authViewModel, todoViewModel: todoViewModel)
            } else {
                AuthRootView(viewModel: authViewModel)
            }
        }
    }
}

private struct AuthRootView: View {
    @ObservedObject var viewModel: AuthViewModel

    var body: some View {
        VStack(spacing: 16) {
            switch viewModel.screen {
            case .login:
                LoginView(viewModel: viewModel)
            case .signup:
                SignupView(viewModel: viewModel)
            case .forgotPassword:
                ForgotPasswordView(viewModel: viewModel)
            }
        }
        .padding(24)
        .frame(maxWidth: 420)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct LoginView: View {
    @ObservedObject var viewModel: AuthViewModel

    var body: some View {
        VStack(spacing: 12) {
            Text("Login").font(.largeTitle).bold()
            TextField("Email", text: $viewModel.email).textFieldStyle(.roundedBorder)
            SecureField("Password", text: $viewModel.password).textFieldStyle(.roundedBorder)
            statusText
            Button(viewModel.isLoading ? "Loading..." : "Login") { viewModel.login() }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.isLoading)
            Button("Create Account") { viewModel.screen = .signup }
            Button("Forgot Password?") { viewModel.screen = .forgotPassword }
        }
    }

    @ViewBuilder
    private var statusText: some View {
        if let message = viewModel.errorMessage {
            Text(message).foregroundStyle(.red)
        } else if let message = viewModel.infoMessage {
            Text(message).foregroundStyle(.blue)
        }
    }
}

private struct SignupView: View {
    @ObservedObject var viewModel: AuthViewModel

    var body: some View {
        VStack(spacing: 12) {
            Text("Signup").font(.largeTitle).bold()
            TextField("Email", text: $viewModel.email).textFieldStyle(.roundedBorder)
            SecureField("Password", text: $viewModel.password).textFieldStyle(.roundedBorder)
            SecureField("Confirm Password", text: $viewModel.passwordConfirm).textFieldStyle(.roundedBorder)
            if let message = viewModel.errorMessage {
                Text(message).foregroundStyle(.red)
            }
            Button(viewModel.isLoading ? "Loading..." : "Signup") { viewModel.signup() }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.isLoading)
            Button("Back to Login") { viewModel.screen = .login }
        }
    }
}

private struct ForgotPasswordView: View {
    @ObservedObject var viewModel: AuthViewModel

    var body: some View {
        VStack(spacing: 12) {
            Text("Reset Password").font(.largeTitle).bold()
            TextField("Email", text: $viewModel.email).textFieldStyle(.roundedBorder)
            if let message = viewModel.errorMessage {
                Text(message).foregroundStyle(.red)
            } else if let message = viewModel.infoMessage {
                Text(message).foregroundStyle(.blue)
            }
            Button(viewModel.isLoading ? "Loading..." : "Send Reset Link") { viewModel.forgotPassword() }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.isLoading)
            Button("Back to Login") { viewModel.screen = .login }
        }
    }
}

private struct TodoRootView: View {
    @ObservedObject var authViewModel: AuthViewModel
    @ObservedObject var todoViewModel: TodoViewModel
    @State private var showAddDialog = false

    var body: some View {
        NavigationStack {
            List(todoViewModel.todos) { todo in
                NavigationLink(value: todo.id) {
                    HStack {
                        Button {
                            todoViewModel.toggleTodo(todo)
                        } label: {
                            Image(systemName: todo.completed ? "checkmark.circle.fill" : "circle")
                        }
                        .buttonStyle(.plain)
                        VStack(alignment: .leading) {
                            Text(todo.text)
                            if let attachment = todo.attachment, !attachment.isEmpty {
                                Text("Attachment: \(attachment)").font(.caption).foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Button("Delete") {
                            todoViewModel.deleteTodo(id: todo.id)
                        }
                        .buttonStyle(.borderless)
                        .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("Todos")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Logout") {
                        authViewModel.logout()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showAddDialog = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .navigationDestination(for: String.self) { todoId in
                TodoDetailView(todoId: todoId, viewModel: todoViewModel)
            }
        }
        .sheet(isPresented: $showAddDialog) {
            AddTodoSheet { text, attachment in
                todoViewModel.createTodo(text: text, attachmentURL: attachment)
            }
        }
        .onAppear { todoViewModel.start() }
        .onDisappear { todoViewModel.stop() }
    }
}

private struct AddTodoSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @State private var selectedFile: URL?
    @State private var showImporter = false
    let onAdd: (String, URL?) -> Void

    var body: some View {
        NavigationStack {
            Form {
                TextField("Task", text: $text)
                Button(selectedFile == nil ? "Attach File" : "File: \(selectedFile!.lastPathComponent)") {
                    showImporter = true
                }
            }
            .navigationTitle("New Todo")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(text, selectedFile)
                        dismiss()
                    }
                    .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .fileImporter(isPresented: $showImporter, allowedContentTypes: [.data], allowsMultipleSelection: false) { result in
                if case let .success(urls) = result {
                    selectedFile = urls.first
                }
            }
        }
    }
}

private struct TodoDetailView: View {
    let todoId: String
    @ObservedObject var viewModel: TodoViewModel

    @State private var todo: TodoRecord?
    @State private var text = ""
    @State private var completed = false
    @State private var selectedFile: URL?
    @State private var deleteAttachment = false
    @State private var showImporter = false
    @State private var attachmentURL: URL?
    @State private var isLoading = true

    var body: some View {
        Form {
            if isLoading {
                ProgressView()
            } else {
                TextField("Task", text: $text)
                Toggle("Completed", isOn: $completed)

                if let todo, let attachment = todo.attachment, !attachment.isEmpty, !deleteAttachment {
                    Section("Attachment") {
                        if let attachmentURL {
                            AsyncImage(url: attachmentURL) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFit().frame(maxHeight: 200)
                                default:
                                    Text(attachment)
                                }
                            }
                        } else {
                            Text(attachment)
                        }

                        Button("Remove Existing Attachment", role: .destructive) {
                            deleteAttachment = true
                        }
                    }
                }

                Section {
                    Button(selectedFile == nil ? "Attach File" : "New: \(selectedFile!.lastPathComponent)") {
                        showImporter = true
                    }
                }
            }
        }
        .navigationTitle("Edit Todo")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Save") { save() }
                    .disabled(todo == nil || isLoading)
            }
            ToolbarItem(placement: .topBarLeading) {
                Button("Delete", role: .destructive) { delete() }
                    .disabled(todo == nil || isLoading)
            }
        }
        .fileImporter(isPresented: $showImporter, allowedContentTypes: [.data], allowsMultipleSelection: false) { result in
            if case let .success(urls) = result {
                selectedFile = urls.first
                if selectedFile != nil {
                    deleteAttachment = false
                }
            }
        }
        .task { await load() }
    }

    private func load() async {
        isLoading = true
        do {
            let loaded = try await viewModel.loadTodo(id: todoId)
            todo = loaded
            text = loaded.text
            completed = loaded.completed
            attachmentURL = viewModel.attachmentURL(for: loaded)
        } catch {
            viewModel.errorMessage = "Could not load todo: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func save() {
        guard let todo else { return }
        viewModel.updateTodo(
            todo: todo,
            text: text,
            completed: completed,
            newAttachmentURL: selectedFile,
            deleteAttachment: deleteAttachment
        )
    }

    private func delete() {
        guard let todo else { return }
        viewModel.deleteTodo(id: todo.id)
    }
}

#Preview {
    ContentView()
}
