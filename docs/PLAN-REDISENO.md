# Plan de rediseño de RED-SOCIAL (UI/UX)

> Objetivo: que RED-SOCIAL sea **atractiva, llamativa e intuitiva**, con lógica real en cada
> proceso. No se inventan funciones: todo lo visual se apoya en la API que ya existe
> (usuarios, seguir, publicaciones, likes, comentarios, feed, búsqueda y chat 1:1).

## 1. Análisis comparativo (qué rescatamos de cada referencia)

| Plataforma | Patrón que la hace fuerte | Qué rescatamos para RED-SOCIAL | Objetivo |
|---|---|---|---|
| **Instagram** | Feed vertical, tarjetas con avatar + acciones grandes, perfil con portada | Tarjetas de post con avatar, acción de like prominente, perfil con *cover* y stats | Feed atractivo y de lectura rápida |
| **X / Twitter** | Jerarquía tipográfica clara, navegación lateral con iconos, composer arriba | Sidebar con iconos + estado activo, composer fijo, separadores finos | Navegación obvia e inmediata |
| **Facebook** | Barra superior global, estados vacíos amables, badges de no leídos | Top bar con usuario, estados vacíos/errores claros, badge de no leídos en el chat | Orientación constante |
| **LinkedIn** | Perfil con cabecera y métricas destacadas, botón primario de seguir | Header de perfil con contadores (seguidores/seguidos/posts) y CTA seguir/mensaje | Perfil profesional y claro |
| **TikTok** | Alto contraste, acentos de color vivos, foco en contenido | Paleta oscura con gradiente vibrante y acentos; contenido protagonista | Estética llamativa moderna |
| **Reddit** | Densidad de información, acciones con conteo | Acciones con contadores (♥ likes, comentarios), layout de columna centrada | Feed informativo sin ruido |
| **Mastodon** | Descentralizado, UI limpia, listas de conversación | Lista de conversaciones con avatar + preview + no leídos | Chat ordenado |
| **Discord / WhatsApp** | Burbujas de mensaje con color propio, timestamps, composer anclado | Burbujas "mías"/"suyas", hora, composer fijo abajo | Chat natural |
| **Linear / Vercel (design systems)** | Tokens de diseño, bordes suaves, animaciones sutiles | Design system con CSS variables, radios y sombras, micro-animaciones | Consistencia y calidad percibida |

## 2. Objetivos que debe cumplir

### 2.1 Identidad visual (design system)
- Paleta oscura con **gradiente de marca** (violeta → magenta) y acento para el like (rojo/rosa).
- Tipografía del sistema (Inter/system-ui), escalas de texto y **contraste AA**.
- Tokens: color, radio, sombra, espaciado, borde — definidos como CSS variables (`--rs-*`).
- Componentes: tarjeta, botón, pill/badge, avatar, burbuja de chat, estado vacío, skeleton.

### 2.2 Navegación e información (IA)
- **Sidebar** con iconos + etiquetas, ítem activo resaltado, marca arriba y salir abajo.
- Responsive: sidebar se comprime a solo-iconos en pantallas chicas.
- Estructura fija: `Inicio (feed) · Explorar · Chat · Perfil`.

### 2.3 Feed
- Composer con avatar, foco claro y botón primario.
- Post: avatar, autor + `@usuario` + tiempo relativo, contenido, barra de acciones (like, comentar, eliminar si es propio).
- Estado vacío que explica qué hacer (seguir gente o publicar).
- Like con feedback visual inmediato (estado activo + conteo).

### 2.4 Perfil
- Cabecera con portada (gradiente), avatar superpuesto, nombre, `@usuario`, bio.
- Métricas (seguidores / siguiendo / publicaciones).
- CTA: `Editar perfil` (propio) o `Seguir/Dejar de seguir` + `Mensaje` (ajeno).
- Listado de publicaciones del usuario.

### 2.5 Explorar
- Buscador prominente con acción clara.
- Resultados como **tarjetas de usuario** (avatar, nombre, `@usuario`) con botón seguir/ver perfil.
- Estado vacío / sin resultados.

### 2.6 Chat
- Layout de dos paneles: conversaciones (izq.) + hilo (der.).
- Ítem de conversación con avatar, nombre y **badge de no leídos**.
- Burbujas propias vs. ajenas, hora por mensaje, composer anclado abajo.
- Nueva conversación con búsqueda de usuarios.
- Refresco casi en tiempo real (sondeo 2 s) reutilizando la lógica existente.

### 2.7 Autenticación
- Pantalla dividida: hero con marca/gradiente + tarjeta de formulario.
- Mensajes de error claros (ya provienen de la API).

### 2.8 Feedback y estados
- Notificaciones de éxito/error ya existentes.
- Estados vacíos y de carga explícitos (sin datos fantasma).
- Confirmaciones en acciones destructivas (eliminar post).

### 2.9 Accesibilidad y consistencia
- Contraste suficiente, foco visible, tamaños de toque adecuados.
- Textos en español, tiempos relativos, mensajes accionables.

## 3. Design tokens (base)

```
--rs-bg        #0b0d12      fondo app
--rs-surface   #141823      tarjetas
--rs-surface-2 #1b2130      elevado/hover
--rs-border    rgba(255,255,255,.08)
--rs-text      #e8eaf0
--rs-text-dim  #9aa3b5
--rs-primary   #7c5cff
--rs-primary-2 #b14bff
--rs-accent    #ff4d8d      like
--rs-success   #2dd4a7
--rs-grad      135deg violeta→magenta
--rs-radius    16px / 22px
--rs-shadow    0 10px 30px rgba(0,0,0,.35)
```

## 4. Fases de implementación

### Fase 1 — Base visual (esta iteración)
1. Activar Lumo en modo oscuro (`frontend/js/theme-dark.js` + `@JsModule`).
2. Reescribir `frontend/styles.css` con tokens, sidebar, tarjetas, avatares, badges, chat, auth y animaciones.
3. `MainLayout` → shell con **sidebar** (iconos VaadinIcon, activo resaltado, usuario + salir).
4. Helper `AvatarUtil` reutilizable en todo el UI.
5. Restyle de `PostCard`, `FeedView`, `ProfileContent`, `ExploreView`, `ChatView`, `LoginView`, `RegisterView`, `CommentDialog`.

### Fase 2 — Pulido UX (siguiente)
6. Skeletons/spinner al cargar; paginación "ver más" en feed y comentarios.
7. Confirmación de borrado y feedback optimista en like.
8. Responsive fino (drawer en móvil) y atajos.

### Fase 3 — Requiere backend nuevo (NO se inventa; queda marcado)
- Subida real de imágenes (avatar/portada de posts) → endpoint de archivos.
- Notificaciones (campana) → entidad + endpoints.
- Chat 100% en tiempo real con `@Push`/cliente STOMP en la UI.
- Refresh tokens, roles admin, grupos/comunidades.

## 5. Archivos afectados (Fase 1)

| Archivo | Cambio |
|---|---|
| `red-social-ui/frontend/js/theme-dark.js` | Nuevo: activa `theme="dark"` |
| `red-social-ui/frontend/styles.css` | Rediseño completo (tokens + componentes) |
| `.../ui/RedSocialUiApplication.java` | `@JsModule` del script de tema |
| `.../ui/view/MainLayout.java` | Shell con sidebar |
| `.../ui/util/AvatarUtil.java` | Nuevo: avatares reutilizables |
| `.../ui/component/PostCard.java` | Tarjeta rediseñada |
| `.../ui/component/CommentDialog.java` | Estilo de comentarios |
| `.../ui/component/ProfileContent.java` | Header con portada + stats |
| `.../ui/view/FeedView.java` | Composer + sección |
| `.../ui/view/ExploreView.java` | Tarjetas de usuario |
| `.../ui/view/ChatView.java` | Burbujas + badge no leídos |
| `.../ui/view/LoginView.java` / `RegisterView.java` | Split-screen con hero |
