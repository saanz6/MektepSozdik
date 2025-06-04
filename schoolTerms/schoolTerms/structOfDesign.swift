//
//  structOfDesing.swift
//  schoolTerms
//
//  Created by Sanzhar  Zhabagin  on 25.03.2025.
//

import SwiftUI

struct SearchBar: View {
    @Binding var searchText: String
    var placeholder: String
    @FocusState private var isSearchFocused: Bool
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)

            TextField(placeholder, text: $searchText)
                .focused($isSearchFocused)
                .autocorrectionDisabled(true)
                .textInputAutocapitalization(.none)

            if !searchText.isEmpty {
                Button(action: {
                    searchText = ""
                    isSearchFocused = false
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
        .background(
            colorScheme == .dark ? Color(.systemGray6) : Color(.systemBackground)
        )
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.gray.opacity(0.4), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 2)
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }
}

struct DictionaryCard: View {
    var title: String
    var iconName: String
    var isSelected: Bool
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: iconName)
                .font(.system(size: 24))
                .foregroundColor(colorScheme == .dark ? .white : .blue)

            Text(title)
                .font(.headline)
                .foregroundColor(colorScheme == .dark ? .white : .blue)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(colorScheme == .dark ? Color(.systemGray5) : Color.white)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}

struct DictionaryView: View {
    @EnvironmentObject var languageManager: LanguageManager

    private let subjects = [
        "Математика", "Физика", "Биология",
        "Естествознание", "Информатика", "Химия", "География", "Геометрия", "Алгебра"
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                ForEach(subjects, id: \.self) { subject in
                    NavigationLink(destination: TermsView(subject: subject)) {
                        DictionaryCard(
                            title: subject.localizedSubjectName(language: languageManager.selectedLanguage),
                            iconName: iconName(for: subject),
                            isSelected: false
                        )
                    }
                }
            }
            .padding()
        }
        .navigationTitle(
            languageManager.selectedLanguage == .kazakh ? "Пәндер" :
            languageManager.selectedLanguage == .english ? "Subjects" :
            "Предметы"
        )
    }

    // Сопоставление предметов с иконками
    private func iconName(for subject: String) -> String {
        switch subject {
        case "Математика", "Алгебра":
            return "function"
        case "Геометрия":
            return "triangle"
        case "Физика":
            return "atom"
        case "Биология":
            return "leaf"
        case "Химия":
            return "flask"
        case "Информатика":
            return "desktopcomputer"
        case "География":
            return "globe.europe.africa"
        case "Естествознание":
            return "sparkles"
        default:
            return "book"
        }
    }
}

struct TermsView: View {
    @StateObject private var dataLoader = TermDataLoader()
    @State private var searchText: String = ""
    let subject: String

    var body: some View {
        VStack {
            SearchBar(searchText: $searchText, placeholder: "Cөз іздеу, Поиск слова, Search a word")
            
            List(filteredTerms) { term in
                NavigationLink(destination: TermDetailView(term: term)) {
                    VStack(alignment: .leading) {
                        Text(term.kazakh)
                            .font(.headline)
                        Text(term.russian)
                            .font(.subheadline)
                        Text(term.english)
                            .font(.subheadline)
                    }
                }
            }
        }
        .onAppear {
            dataLoader.fetchTerms(for: subject) { terms in
                dataLoader.terms = terms
            }
        }
        .navigationTitle("")
    }

    var filteredTerms: [Term] {
        if searchText.isEmpty {
            return dataLoader.terms
        } else {
            return dataLoader.terms.filter { term in
                term.kazakh.localizedCaseInsensitiveContains(searchText) ||
                term.russian.localizedCaseInsensitiveContains(searchText) ||
                term.english.localizedCaseInsensitiveContains(searchText)
            }
        }
    }
}
struct TermDetailView: View {
    let term: Term

    @AppStorage("favoriteTerms") private var favoriteTermsData: Data = Data()
    @State private var isFavorite: Bool = false
    @State private var isSharing = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text(term.kazakh)
                    .font(.largeTitle)
                    .fontWeight(.bold)
                Spacer()
                // Кнопка избранного
                Button(action: {
                    toggleFavorite()
                }) {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                        .resizable()
                        .frame(width: 24, height: 24)
                        .foregroundColor(isFavorite ? .red : .gray)
                }
                // Кнопка поделиться
                Button(action: {
                    isSharing = true
                }) {
                    Image(systemName: "square.and.arrow.up")
                        .resizable()
                        .frame(width: 22, height: 22)
                        .foregroundColor(.blue)
                }
                .padding(.leading, 12)
            }

            Text("Русский: \(term.russian)")
            Text("English: \(term.english)")
            Text("Сипаттама: \(term.description)")

            Spacer()
        }
        .padding()
        .onAppear {
            loadFavoriteStatus()
        }
        .sheet(isPresented: $isSharing) {
            ActivityView(activityItems: [shareText])
        }
    }

    private var shareText: String {
        """
        \(term.kazakh)
        Русский: \(term.russian)
        English: \(term.english)
        \(term.description)
        """
    }

    private func toggleFavorite() {
        var favorites = loadFavorites()
        if isFavorite {
            favorites.removeAll { $0.kazakh == term.kazakh }
        } else {
            favorites.append(term)
        }
        saveFavorites(favorites)
        isFavorite.toggle()
    }

    private func loadFavoriteStatus() {
        let favorites = loadFavorites()
        isFavorite = favorites.contains { $0.kazakh == term.kazakh }
    }

    private func loadFavorites() -> [Term] {
        if let decoded = try? JSONDecoder().decode([Term].self, from: favoriteTermsData) {
            return decoded
        }
        return []
    }

    private func saveFavorites(_ favorites: [Term]) {
        if let encoded = try? JSONEncoder().encode(favorites) {
            favoriteTermsData = encoded
        }
    }
}
//структура для поделиться
struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]
    let applicationActivities: [UIActivity]? = nil

    func makeUIViewController(context: Context) -> UIActivityViewController {
        return UIActivityViewController(activityItems: activityItems,
                                        applicationActivities: applicationActivities)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

struct FavoritesView: View {
    @AppStorage("favoriteTerms") private var favoriteTermsData: Data = Data()
    @State private var favoriteTerms: [Term] = []
    @EnvironmentObject var languageManager: LanguageManager

    var body: some View {
        Group {
            if favoriteTerms.isEmpty {
                VStack {
                    Spacer()
                    VStack(spacing: 16) {
                        Image(systemName: "star")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 60, height: 60)
                            .foregroundColor(.gray.opacity(0.5))

                        Text(emptyListText)
                            .font(.headline)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                    }
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(UIColor.systemGroupedBackground))
            } else {
                List(favoriteTerms) { term in
                    NavigationLink(destination: TermDetailView(term: term)) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(term.kazakh)
                                .font(.headline)
                            Text(term.russian)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Text(term.english)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 8)
                    }
                }
                .listStyle(.insetGrouped)
            }
        }
        .onAppear {
            loadFavorites()
        }
        .navigationTitle("")
    }

    private func loadFavorites() {
        if let decoded = try? JSONDecoder().decode([Term].self, from: favoriteTermsData) {
            favoriteTerms = decoded
        }
    }

    private var emptyListText: String {
        switch languageManager.selectedLanguage {
        case .kazakh: return "Таңдаулылар тізімі бос"
        case .russian: return "Список избранного пуст"
        case .english: return "Favorites list is empty"
        }
    }
}

struct RandomWordView: View {
    @State private var currentIndex = 0
    @State private var randomTerms: [Term] = []
    @Environment(\.colorScheme) var colorScheme
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack {
            if !randomTerms.isEmpty {
                TabView(selection: $currentIndex) {
                    ForEach(randomTerms.indices, id: \.self) { index in
                        let term = randomTerms[index]
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Күн сөзі, Слово дня, Word of the day")
                                .font(.caption)
                                .foregroundColor(colorScheme == .dark ? .white : .gray)

                            Text(term.kazakh)
                                .font(.title)
                                .bold()
                                .foregroundColor(colorScheme == .dark ? .white : .blue)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)

                            Text("\(term.russian) • \(term.english)")
                                .font(.subheadline)
                                .foregroundColor(colorScheme == .dark ? .white.opacity(0.8) : .gray)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)

                            Text(term.description)
                                .font(.body)
                                .foregroundColor(colorScheme == .dark ? .white.opacity(0.9) : .blue)
                                .padding(.top, 8)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, minHeight: 260)
                        .background(colorScheme == .dark ? Color(.systemGray5) : Color.white)
                        .cornerRadius(20)
                        .shadow(color: colorScheme == .dark ? .clear : .gray.opacity(0.2), radius: 5, x: 0, y: 2)
                        .padding(.horizontal, 16)
                        .tag(index)
                    }
                }
                .frame(height: 300)
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .onReceive(timer) { _ in
                    withAnimation {
                        currentIndex = (currentIndex + 1) % randomTerms.count
                    }
                }
            } else {
                Text("Загрузка случайных слов...")
                    .padding()
            }
        }
        .onAppear {
            if randomTerms.isEmpty {
                TermDataLoader().fetchAllSubjectsTerms { terms in
                    let shuffled = terms.shuffled()
                    DispatchQueue.main.async {
                        self.randomTerms = Array(shuffled.prefix(10))
                    }
                }
            }
        }
    }
}

enum AppLanguage: String, CaseIterable, Identifiable {
    case kazakh = "kk"
    case russian = "ru"
    case english = "en"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .kazakh: return "Қазақша"
        case .russian: return "Русский"
        case .english: return "English"
        }
    }
}
class LanguageManager: ObservableObject {
    @Published var selectedLanguage: AppLanguage {
        didSet {
            UserDefaults.standard.set(selectedLanguage.rawValue, forKey: "AppLanguage")
        }
    }

    init() {
        let saved = UserDefaults.standard.string(forKey: "AppLanguage") ?? "ru"
        self.selectedLanguage = AppLanguage(rawValue: saved) ?? .russian
    }
}
extension String {
    func localizedSubjectName(language: AppLanguage) -> String {
        let translations: [String: [AppLanguage: String]] = [
            "Математика": [.kazakh: "Математика", .russian: "Математика", .english: "Mathematics"],
            "Физика": [.kazakh: "Физика", .russian: "Физика", .english: "Physics"],
            "Биология": [.kazakh: "Биология", .russian: "Биология", .english: "Biology"],
            "Естествознание": [.kazakh: "Жаратылыстану", .russian: "Естествознание", .english: "Science"],
            "Информатика": [.kazakh: "Информатика", .russian: "Информатика", .english: "Computer Science"],
            "Химия": [.kazakh: "Химия", .russian: "Химия", .english: "Chemistry"],
            "География": [.kazakh: "География", .russian: "География", .english: "Geography"],
            "Геометрия": [.kazakh: "Геометрия", .russian: "Геометрия", .english: "Geometry"],
            "Алгебра": [.kazakh: "Алгебра", .russian: "Алгебра", .english: "Algebra"]
        ]
        
        return translations[self]?[language] ?? self
    }
}

