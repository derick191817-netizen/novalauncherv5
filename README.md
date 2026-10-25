# NovaLauncher (`dev.nova.launcher`)

Launcher nativo para Android 10+ (API 29–34), otimizado para o Galaxy Note 20 Ultra (One UI 5.1, 120Hz, S Pen).
Kotlin 100% · Compose · Hilt · Coroutines/Flow · Baseline Profile · R8 full mode.

> **Estado:** o código foi escrito sem acesso a Android SDK/Gradle, então **ainda não foi compilado**.
> O primeiro `assembleDebug` pode apontar erros pequenos (imports, nomes de API). Veja "Se o build falhar".

## 1. Pré-requisitos
JDK 17 · Android Studio Koala (2024.1.1)+ · Android SDK 34 · depuração USB ligada no aparelho.
O zip não inclui `gradlew`/`gradle-wrapper.jar`: abra a pasta no Android Studio (ele gera o wrapper) ou rode `gradle wrapper --gradle-version 8.9`.

## 1.1 Compilar só com o celular (sem PC)
Suba o projeto para um repositório no GitHub e use `.github/workflows/android.yml`: a nuvem compila, roda os
testes e entrega o APK em Actions -> (execução) -> Artifacts -> `app-debug-apk`. Passo a passo no chat/CHANGELOG.

## 2. Build e testes (nesta ordem)
```bash
./gradlew :core:domain:testDebugUnitTest        # busca fuzzy, layout, use cases
./gradlew :core:data:compileDebugKotlin
./gradlew :core:ui:compileDebugKotlin
./gradlew :feature:widgets:compileDebugKotlin :feature:gestures:compileDebugKotlin
./gradlew :feature:home:testDebugUnitTest :feature:drawer:testDebugUnitTest
./gradlew :feature:settings:compileDebugKotlin
./gradlew assembleDebug
./gradlew detekt ktlintCheck                    # ktlintFormat corrige imports não usados
```

## 3. Assinatura (release)
```bash
keytool -genkeypair -v -keystore nova.jks -alias nova -keyalg RSA -keysize 2048 -validity 10000
```
Em `~/.gradle/gradle.properties` (nunca no repositório):
```
NOVA_STORE_FILE=/caminho/nova.jks
NOVA_STORE_PASSWORD=...
NOVA_KEY_ALIAS=nova
NOVA_KEY_PASSWORD=...
```
`./gradlew assembleRelease` → `app/build/outputs/apk/release/`.

## 4. Instalação no Note 20 Ultra
```bash
./gradlew installDebug
adb shell cmd package set-home-activity dev.nova.launcher/.LauncherActivity   # define como launcher padrão
```
Ou: botão Home → escolher "Nova Launcher" → "Sempre". Voltar ao launcher da Samsung: Configurações → Apps → Aplicativos padrão → App inicial.

## 5. Benchmark e Baseline Profile (aparelho real, Nova como launcher padrão)
O módulo `:benchmark` está fora do build por padrão. Para ligá-lo: `gradle -PwithBenchmark :app:generateBaselineProfile`.
- Gerar perfil: `./gradlew :app:generateBaselineProfile` (ou rodar `BaselineProfileGenerator` pelo Android Studio).
- Medir: rodar `StartupBenchmark` (meta: `timeToInitialDisplayMs` < 400) e `ScrollBenchmark` (meta: 0 frames perdidos).
Os nomes das tasks dependem da versão do plugin `androidx.baselineprofile`; se mudarem, use as run configurations geradas pelo Android Studio.

## 6. Arquitetura
`:app` (Activities, Hilt, ações de sistema) → `:feature:{home,drawer,settings,widgets,gestures}` → `:core:ui` → `:core:domain` (puro: modelos, use cases, busca) ← `:core:data` (LauncherApps, DataStore, JSON, cache de ícones).
Decisões de desempenho (cada uma comentada no código): sem NavHost; ícones HARDWARE em LRU por bytes + disco WebP; `WhileSubscribed(5s)` (listeners só com UI visível); janela transparente sobre o wallpaper (zero overdraw de fundo); `LazyVerticalGrid` com `key`/`contentType`; paralelismo de ícones limitado a 3.

## 7. S Pen e Knox
Hover: o ícone cresce e mostra o rótulo (`stylusHover`, sem consumir eventos). Botão/Air Actions nativas continuam com o sistema: nenhum evento de S Pen é interceptado. O launcher não altera políticas do Knox; o Device Admin usa só `force-lock`.

## 8. Limitações conhecidas
- Arrastar e soltar é experimental (Configurações → Módulos): move dentro da mesma página, só em modo de edição.
- Ícones temáticos (Android 13+) são opcionais e só valem para apps com ícone monocromático; atualizam ao reabrir o launcher.
- Ainda sem selos de notificação (badges) nem lista de "recentes" por uso.
- Backup guarda os widgets pelo provider; ao restaurar, "Restaurar widgets" aparece no modo de edição e cada um é religado (a posição é reatribuída).
- `expandNotifications` tenta o método oculto do sistema e cai na acessibilidade (nem sempre disponível em API 30+).
- Apps ocultos: Android 10 só aceita biometria; 11+ aceita biometria ou senha.
- Fora do MVP (fase 3): root/Shizuku, color engine próprio, context engine, features exclusivas.

## 9. Checklist no Note 20 Ultra
- [ ] `assembleDebug` e `installDebug` sem erro
- [ ] Home aparece como opção e fica como padrão; dock com apps Samsung
- [ ] Swipe up abre a gaveta; busca "camera" acha "Câmera"
- [ ] Toque longo no app: atalhos, adicionar à home/dock, info, desinstalar
- [ ] Adicionar widget (com e sem tela de configuração); remover no modo de edição
- [ ] Pasta: criar, abrir, adicionar e remover app
- [ ] Toque duplo bloqueia (Device Admin ou Acessibilidade); swipe down abre notificações
- [ ] S Pen pairando mostra rótulo; Air Actions seguem funcionando
- [ ] Tema AMOLED preto puro; modo Ultra Performance sem animações/blur; Economia a 60Hz
- [ ] Smart Folders, apps ocultos (biometria), busca de contatos (permissão), icon pack, backup/restore
- [ ] Benchmarks: cold start < 400ms, 0 frames perdidos; RAM idle < 80MB (`adb shell dumpsys meminfo dev.nova.launcher`)
- [ ] `grep -rn '!!' --include=*.kt .` sem resultados
