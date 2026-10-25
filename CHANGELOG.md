# Changelog
Formato: [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versionamento semântico.

## [0.3.2] - em andamento
### Added
- Configurações com rascunho: nada vale até tocar em **Salvar**; **Descartar** volta ao salvo; botão **Fechar** sempre visível; voltar com alterações pendentes pergunta antes; **Restaurar padrões** (no rascunho). Salvar termina mesmo se a tela fechar logo depois.
### Fixed
- Busca da gaveta: a letra digitada aparecia atrás da anterior. O texto agora é mantido localmente em um `TextFieldValue` (texto + cursor) e só enviado ao ViewModel; antes ele voltava do ViewModel de forma assíncrona e sobrescrevia o campo.

## [0.3.1] - 2026-10-24
### Fixed (build do CI)
- `:core:domain`: `runtime-annotation` com versão fixa (1.7.2/1.7.3) não existe. Agora `api(platform(libs.compose.bom))` + `api(libs.compose.runtime)`, versão vinda do BOM. O BOM é `api` para que data/ui/features consigam resolver o runtime.
- `:feature:gestures`: faltavam `implementation(project(":core:domain"))` e `(":core:ui")` (`GestureAction` não resolvia).
- `a11y_lock.xml`: `accessibilityEventTypes="typeNone"` não é um valor válido (erro do AAPT). O atributo foi removido: sem ele o serviço não recebe eventos, só executa ações globais (bloquear tela/notificações).
- `LauncherGestures.kt`: versão simples (deslizar cima/baixo + toque duplo/longo). Pinça e gesto de borda ficam desativados por ora; o código completo está em `feature/gestures/docs/LauncherGesturesAdvanced.kt.txt`.
- `AppTile.kt`: reescrito só com APIs estáveis do Compose (tipos explícitos nas animações).
- `:feature:home`: dependências duplicadas removidas.
### Changed
- CI: `setup-android@v3`, comando global `gradle`, APK enviado como artifact antes dos testes.
- `gradle.properties`: configuration cache desligado (incompatibilidade de plugin derrubava o build) e mais memória para o Gradle/Kotlin.
- `:benchmark` e o plugin Baseline Profile só entram com `-PwithBenchmark` (aparelho real), então não afetam o APK do CI.
- Versão 0.3.1 (versionCode 4).

## [0.3.0] - 2026-10-24
### Added
- Formato dos ícones (sistema, círculo, squircle, arredondado, gota) com máscara antialias (`IconShapes`); entra na chave de cache.
- Gaveta: ordem A–Z sem acento ou "Novos" (recém-instalados); campo de busca em pílula e topo arredondado (estilo One UI).
- "Bloquear layout da tela inicial": impede mover, remover ou adicionar itens por engano.
- Testes: `AppSorterTest`, `SettingsKeysTest` (todas as preferências, incluindo as novas), `AppListJsonTest`, `IconStyleTest`, `HomeViewModelLockTest`, `DrawerViewModelSortTest`.
### Changed
- Otimização: a lista de apps da sessão anterior é lida do disco e mostrada na hora enquanto o PackageManager monta a nova (`AppListCache`).
- Otimização: `AppTile` não recria o `TextStyle` do rótulo a cada recomposição.
- `DrawerViewModel` recebe `UpdateSettingsUseCase` (ordenação). Versão 0.3.0 (versionCode 3).
### Fixed
- Apps com acento ("Ágora") apareciam depois do "Z" na gaveta.
- Preferências novas (arrastar/soltar, ícones temáticos) agora têm teste de gravação/leitura.

## [0.2.1]
### Added
- GitHub Actions (`android.yml`): compila o APK debug e roda os testes na nuvem; permite compilar só pelo celular.
- Gaveta: índice A–Z lateral (toque/arraste, com vibração) quando há mais de 30 apps.
- Perfil de uso filtra a gaveta (`ProfileFilter`): Trabalho = apps do perfil de trabalho, Games = jogos; com fallback para todos.
- Pastas da home podem ser renomeadas (`TextInputDialog`, `ManageHomeLayoutUseCase.renameFolder`).
- Home já nasce com apps comuns (galeria, calendário, relógio, Play Store, Maps…); dock com galeria e notas (até 7).
- Modo de edição: botão "Ajustes" e "Restaurar widgets (N)".
- Drag-and-drop na home (flag `dragAndDrop`, experimental) com `ManageHomeLayoutUseCase.moveItem` e `HomeLayout.canPlace`.
- Ícones temáticos Material You (flag `themedIcons`, Android 13+) via `ThemedIconProvider`.
- Backup inclui os widgets (providers) e a restauração é feita pela UI; backups antigos continuam válidos.
- `AppInfo.iconCacheKey` (extensão); cache de ícones usa a chave.
- Modo Economia reduz pela metade o cache de ícones em memória (`freezeOffscreenIcons` agora tem efeito).
- Acessibilidade: `contentDescription` nos apps (TalkBack no dock) e vibração no toque longo.
- "Definir como padrão" abre direto o diálogo `RoleManager.ROLE_HOME` (Android 10+).
- Testes: `HomeLayoutEditTest`, `ManageHomeLayoutEditTest`, `ProfileFilterTest`, `UniversalSearchUseCaseTest`, `AppInfoExtTest`, `HomeLayoutJsonTest`, `HomeViewModelEditTest`.
### Fixed
- Toque duplo (bloquear) e toque longo (editar) nunca disparavam: o pager capturava os toques e o fundo ficava atrás dele. Agora o fundo fica dentro de cada página.
- Itens da home "sumiam" ao diminuir linhas/colunas: `HomeLayout.reflow` os traz de volta.
- O layout era regravado em disco mesmo sem mudança.
- O dock padrão nunca escolhe a cópia do perfil de trabalho.
### Changed
- `ManageHomeLayoutUseCase.seed(...)` ganhou `rows`/`columns` com default (compatível).
### Merged from the other session (ideas only, reimplemented)
- `ProfileFilter` (com PERSONAL = todos), `TextInputDialog`, ordem/papéis do dock. Não mesclado: `LayoutOps`, `LayoutRepository` e `AppActionsRepository` (arquitetura diferente).
### Fixed
- `:app`: `androidResources.localeFilters` não existe no AGP 8.5.2; trocado por `resourceConfigurations`.
- `:app`: build `release` sem keystore configurado não quebra mais a configuração (sai sem assinatura).
- `:core:data`: `callbackFlow<Boolean>` explícito em `LauncherAppsDataSource.observe()`.
- `:feature:home`, `:feature:drawer`: dependência `activity-compose` (BackHandler) ausente.
