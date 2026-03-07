# Guía de Arquitectura y Estructura del Proyecto: OdooApp (Stack Perú Partners)

Esta guía documenta exhaustivamente la estructura de carpetas, la arquitectura elegida y el flujo de comunicación de la aplicación nativa Android construida en Kotlin, diseñada para integrarse directamente con el backend de **Odoo 19** mediante JSON-RPC 2.0.

---

## 1. Arquitectura del Proyecto

El proyecto sigue el patrón **MVC (Model-View-Controller)** con principios de **Clean Architecture** adaptados a Android. El objetivo es mantener una estricta separación de responsabilidades, lo que permite que el código sea predecible, escalable y fácil de depurar.

La comunicación de red se gestiona mediante la librería **Retrofit** y la manipulación/parseo de respuestas asíncronas con **Kotlin Coroutines** (`lifecycleScope.launch`).

---

## 2. Estructura de Directorios Lógicos (Kotlin)

Ruta base: `app/src/main/java/com/stackperu/odooapp/`

### 📦 Raíz del Paquete
*   **`AppConfig.kt`**: Archivo *Singleton* centralizado que actúa como el diccionario de constantes globales de la aplicación. Aquí se define la `BASE_URL` (hacia dónde apunta Retrofit) y el `DATABASE_NAME` (nombre de la base de datos de PostgreSQL en Odoo). Es el primer lugar al que debes acudir si cambias de entorno (ej. de Localhost a Producción).
*   **`MainActivity.kt`**: Es el **Controlador principal (Dashboard)** de la aplicación post-login. Su responsabilidad principal es:
    1.  Verificar que la sesión esté activa (si no, devuelve al Login).
    2.  Realizar la petición a Odoo para recuperar los contactos (`res.partner`) utilizando los parámetros definidos para llamadas ORM.
    3.  Gestionar los clicks de la interfaz para alternar dinámicamente entre las vistas: Kanban (Malla/Grid), Lista (Vertical) o Pivot (Resumen de texto).
    4.  Configurar la "Top Bar" solicitando a Glide la descarga del avatar del usuario logueado inyectando la cookie de sesión.

### 📦 Paquete: `/api/` (Capa de Red)
Gestiona todas las comunicaciones HTTP hacia el servidor Odoo.

*   **`RetrofitClient.kt`**: El motor de conexión. Configura el cliente HTTP (usando OkHttp). 
    *   *Lo más importante aquí es la clase interna `SessionCookieJar`*. Odoo 19 no usa Tokens tipo Bearer/JWT convencionales, sino una cookie clásica de navegador llamada `session_id`. El `CookieJar` captura esta cookie durante el Login, la guarda en memoria, y automáticamente la "pega" en los Headers de todas las peticiones futuras para validar que estás autenticado.
*   **`OdooApiService.kt`**: Un contrato (Interfaz) que mapea las rutas exactas del API de Odoo a funciones de Kotlin (Ej: `@POST("/web/session/authenticate")`). Define qué modelo de datos entra y qué modelo sale de cada ruta.

### 📦 Paquete: `/model/` (Capa de Dominio / Entidades)
Define las "estructuras" o "moldes" (Data Classes) de la información. Son vitales para traducir (parsear) de JSON a objetos Kotlin y viceversa.

*   **`Models.kt`**: Agrupa todas las estructuras necesarias.
    *   *Modelos de Red (Petición/Respuesta Odoo)*: `OdooRequest`, `OdooResponse`, `CallKwParams`, `Kwargs`, `AuthParams`. Estas clases arman exactamente el cuerpo JSON que exige el estándar JSON-RPC 2.0 de Odoo.
    *   *Modelos de Negocio*: `User` (usuario que se loguea) y `Contact` (representa un registro de `res.partner`). En `Contact` se agregó una lógica especial (`get() = avatarBase64`) para lidiar con el hecho de que Odoo devuelve la imagen en formato Base64 (texto) o un booleano `false` si no hay foto.

### 📦 Paquete: `/data/` (Capa de Estado)
*   **`UserSession.kt`**: Un gestor de sesión ultraligero que vive en la memoria RAM. Almacena temporalmente el objeto `User` mientras la app está abierta. Provee el método `clear()` invocado cuando el usuario hace clic en "Cerrar sesión".

### 📦 Paquete: `/ui/` (Capa de Presentación / Controladores Hijos)
Controlan partes específicas de la interfaz de usuario.

*   **`/login/LoginActivity.kt`**: El punto de entrada inicial. Recoge los datos del formulario (correo y contraseña), toma la base de datos de `AppConfig`, y dispara la petición de validación a Odoo. Si Odoo responde exitosamente, guarda el usuario en `UserSession` y abre `MainActivity`.
*   **`/contact/ContactAdapter.kt`**: Es el "puente" entre la lista cruda de contactos (Data) y el componente visual (RecyclerView). Su función estrella es evaluar la variable booleana `isKanbanView` y, dependiendo de ella, "inflar" (dibujar) el XML de cuadrícula (`item_contact_kanban.xml`) o el de fila tradicional (`item_contact_list.xml`). También decodifica el String Base64 del avatar y usa Glide para dibujarlo.
*   **`/contact/ContactDetailActivity.kt`**: Representa la "Vista de Formulario" (Form View). Al hacer tap en un contacto de la lista, esta clase recibe el objeto `Contact` empaquetado (Serializado) y vuelca sus propiedades (nombre, email, teléfono, VAT, dirección, foto) en campos de texto visuales bloqueados para su lectura en profundidad.

---

## 3. Estructura de Directorios de Recursos (XML)

Ruta base: `app/src/main/res/`

La carpeta `res/` maneja los activos visuales (colores, traducciones, imágenes y diseños de pantallas). Aquí **no hay código lógico**, solo definiciones visuales.

### 🎨 `/drawable/`
Contiene imágenes e íconos.
*   **`logo_stack_peru.png`** *(Debes colocar tu archivo real aquí)*: Es la imagen que se muestra en la cabecera de la pantalla del Login.
*   **`avatar_placeholder.xml`**: Un círculo gris vectorial simple que se muestra milisegundos antes de que Glide termine de cargar la foto real del contacto, o que queda permanente si el usuario no tiene foto.

### 📐 `/layout/`
Son los "planos arquitectónicos" de las pantallas. Usan el componente `ConstraintLayout` para alinear objetos usando "anclas" o restricciones.
*   **`activity_login.xml`**: Dibuja el logo, título corporativo ("STACK PERÚ"), los campos de texto estilizados por Material Design y el botón de Login.
*   **`activity_main.xml`**: Define la barra superior personalizada (con título y avatar circular), la botonera (Kanban, List, Pivot), el espacio central (`RecyclerView`) donde se dibujarán las tarjetas, y el bloque de texto oculto para la vista Pivot.
*   **`activity_contact_detail.xml`**: Dibuja la vista de un registro completo, listando verticalmente los atributos mediante cajas de texto flotantes tipo "OutlinedBox".
*   **`item_contact_kanban.xml`**: Define visualmente cómo se ve "una sola tarjetita cuadrada" de un contacto (orientación vertical, foto grande centrada, textos centrados abajo).
*   **`item_contact_list.xml`**: Define visualmente cómo se ve "una sola fila" de contacto (orientación horizontal, foto pequeña a la izquierda, información tabulada a la derecha).

### 🏷️ `/values/`
Centraliza "variables" estéticas.
*   **`colors.xml`**: Archivo crítico para la identidad corporativa. Aquí se declararon los colores oficiales extraídos del logo: Azul Marino (`#0A1128`) y el Verde/Cyan (`#007B7F`), entre otros colores base (blanco, gris).
*   **`strings.xml`**: Contiene todo el texto humano de la app (títulos, pistas de botones, alertas). Esto asegura que el código Kotlin no tenga texto "quemado" (hardcodeado), facilitando correcciones ortográficas o futuras traducciones en un solo lugar.
*   **`themes.xml`**: Le dice a Android cómo pintar elementos predeterminados del sistema, por ejemplo, fuerza a que la barra de notificaciones del teléfono (Status Bar) asuma el color Azul Marino corporativo para dar sensación de inmersión total.

### 🌐 `/xml/`
*   **`network_security_config.xml`**: Por motivos de seguridad modernos, Android rechaza todas las conexiones web que no sean HTTPS (con certificado SSL). Este archivo crea una excepción temporal para dominios locales como `10.0.2.2` o `localhost` permitiendo hacer pruebas en entorno de desarrollo.

---

## 4. Archivos Raíz Críticos

*   **`AndroidManifest.xml`**: El núcleo de permisos y registro de actividades. Indica qué Activity lanza primero la aplicación (`LoginActivity`) y exige expresamente permiso de `INTERNET` para que el celular no bloquee a Retrofit.
*   **`build.gradle.kts (App Level)`**: El gestor de dependencias (similar al `requirements.txt` en Python o `package.json`). Aquí se declaran librerías externas que el proyecto necesita descargar de internet para compilar:
    *   *Retrofit / Gson Converter* (Para las APIs y parseo de JSON)
    *   *Glide* (Para la carga y renderizado asíncrono y en caché de imágenes de perfil)
    *   *Material Components* (Para usar tarjetas con bordes redondeados y campos de texto modernos)
    *   *ViewModel & LiveData* (Agregados como cimientos recomendados por Google).

---

## 5. Flujo Operativo Típico (Cómo se conectan)

Para comprender el flujo, imagina el momento en que abres la aplicación e intentas listar tus clientes:

1.  **Arranque**: Android lee el `AndroidManifest.xml`, detecta que `LoginActivity` es el inicio y la lanza. `LoginActivity` manda a dibujar `activity_login.xml` en la pantalla y carga la imagen `logo_stack_peru`.
2.  **Interacción de Login**: Escribes tus datos y presionas "INICIAR SESIÓN". `LoginActivity` recolecta tus campos, lee el nombre de la BD desde `AppConfig.kt` y ensambla el objeto `OdooRequest` apuntando al endpoint oficial (`/web/session/authenticate`).
3.  **Gestión de Red**: `RetrofitClient` hace la llamada HTTP. Odoo evalúa y si todo está OK, emite un *Header* `Set-Cookie`. `SessionCookieJar` lo captura silenciosamente. Se mapea la respuesta a `AuthResult`.
4.  **Almacenamiento Temporal**: Se guardan los datos básicos del individuo autenticado en el objeto estático `UserSession`.
5.  **Transición de Pantalla**: La app cambia la vista lanzando `MainActivity`, cerrando la de Login en el historial.
6.  **Pedido de Datos ORM**: `MainActivity` arranca, dibuja `activity_main.xml` y ejecuta `fetchContacts()`. Construye un nuevo `OdooRequest` bajo el estándar `call_kw` buscando el modelo `"res.partner"`. 
7.  **Inyección Automática**: `RetrofitClient` detecta que vas a hacer una petición e inyecta la cookie `session_id` que había guardado. Odoo te permite pasar y devuelve el inmenso bloque de JSON con los contactos.
8.  **Modelado y Adaptación**: El JSON es forzado a encajar dentro de la clase `Contact` de `Models.kt`. La lista resultante se le entrega al `ContactAdapter`.
9.  **Renderizado Visual**: `ContactAdapter` empieza a "clonar" tarjetas basándose en `item_contact_kanban.xml`, decodifica la inmensa cadena Base64 a bytes reales, invoca a Glide para que la redondee y finalmente la lista se despliega al usuario. 
10. **Detalle Final**: Tocar una tarjeta abre `ContactDetailActivity`, cargando `activity_contact_detail.xml` y bloqueando la edición para lectura pura.
11. **Log Out**: Tocar el botón de apagado en la barra superior envía una señal HTTP final de destrucción (`/web/session/destroy`), vacía las variables RAM (`UserSession` y el `CookieJar`) y te empuja bruscamente de vuelta al ciclo número 1.