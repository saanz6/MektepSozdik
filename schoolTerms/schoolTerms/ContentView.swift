//
//  ContentView.swift
//  schoolTerms
//
//  Created by Sanzhar  Zhabagin  on 25.03.2025.
//

import SwiftUI

struct ContentView: View {
    @State private var searchText = ""
    @State private var allTerms: [Term] = []
    @FocusState private var isSearchFocused: Bool
    @EnvironmentObject var languageManager: LanguageManager

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                // Язык (Picker)
                Picker("", selection: $languageManager.selectedLanguage) {
                    ForEach(AppLanguage.allCases) { lang in
                        Text(lang.displayName).tag(lang)
                    }
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(.horizontal, 16)

                // Поиск
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)

                    TextField(searchPlaceholder, text: $searchText)
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
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.4), lineWidth: 1)
                )
                .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 2)
                .padding(.horizontal, 16)

                if !filteredTerms.isEmpty {
                    // Поисковая выдача
                    List(filteredTerms) { term in
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
                            .padding(.vertical, 4)
                        }
                    }
                    .listStyle(PlainListStyle())
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            HStack(spacing: 16) {
                                NavigationLink(destination: DictionaryView()) {
                                    DictionaryCard(title: localizedTitle("Словарь"), iconName: "book", isSelected: true)
                                }

                                NavigationLink(destination: FavoritesView()) {
                                    DictionaryCard(title: localizedTitle("Избранное"), iconName: "bookmark", isSelected: false)
                                }
                            }
                            .padding(.horizontal, 16)

                            RandomWordView()
                                .frame(height: 300)
                                .padding(.horizontal, 16)

                            Text("Powered by Neo")
                                .font(.footnote)
                                .foregroundColor(.gray)
                                .multilineTextAlignment(.center)
                                .padding(.top, 200)
                                .padding(.bottom, 20)

                            // Spacer снизу ТОЛЬКО если клавиатура активна
                            if isSearchFocused {
                                Spacer().frame(height: 200)
                            }
                        }
                    }
                    .onTapGesture {
                            isSearchFocused = false // Скрыть клавиатуру при тапе вне
                        }
                }
            }
            .padding(.top, 10)
            .background(Color(UIColor.systemGroupedBackground))
            .onAppear {
                TermDataLoader().fetchAllSubjectsTerms { terms in
                    self.allTerms = terms
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    // Фильтр по поисковому запросу
    var filteredTerms: [Term] {
        if searchText.isEmpty {
            return []
        } else {
            return allTerms.filter { term in
                term.kazakh.localizedCaseInsensitiveContains(searchText) ||
                term.russian.localizedCaseInsensitiveContains(searchText) ||
                term.english.localizedCaseInsensitiveContains(searchText)
            }
        }
    }

    // Местный плейсхолдер
    var searchPlaceholder: String {
        switch languageManager.selectedLanguage {
        case .kazakh: return "Сөз іздеу"
        case .russian: return "Поиск слова"
        case .english: return "Search a word"
        }
    }

    func localizedTitle(_ base: String) -> String {
        switch (base, languageManager.selectedLanguage) {
        case ("Словарь", .kazakh): return "Сөздік"
        case ("Словарь", .english): return "Dictionary"
        case ("Избранное", .kazakh): return "Таңдаулылар"
        case ("Избранное", .english): return "Favorites"
        default: return base
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(LanguageManager())
}
