# 🚀 OdooApp - Stack Perú Partners

![Versión](https://img.shields.io/badge/Versi%C3%B3n-1.0-blue)
![Min SDK](https://img.shields.io/badge/Min_SDK-24-green)
![Target SDK](https://img.shields.io/badge/Target_SDK-36-orange)
![Repositorio](https://img.shields.io/badge/Repo-FranciscoAntonioVilchezQuispe/odooApp__Android-blue?logo=github)

Guía profesional de arquitectura y desarrollo de la aplicación nativa para la gestión de **Odoo 19** con localización peruana. Esta aplicación ha sido diseñada bajo estándares senior para garantizar robustez, mantenibilidad y una experiencia de usuario fluida.

---

## 📱 Vista Previa
| Acceso Seguro | Dashboard Corporativo | Gestión de Contactos | Emisión de Facturas |
| :---: | :---: | :---: | :---: |
| ![Pantalla Login](screenshots/login.png) | ![Dashboard](screenshots/dashboard.png) | ![Contactos](screenshots/contactos.png) | ![Facturación](screenshots/facturacion.png) |

---

## 🏗️ Arquitectura del Proyecto
El proyecto implementa un patrón **MVC (Model-View-Controller)** con principios de **Clean Architecture**, asegurando que la lógica de negocio esté desacoplada de la interfaz de usuario.

*   **Capa de Vista**: Implementada con **ViewBinding** para un acceso seguro a los componentes XML (`res/layouts/*`).
*   **Capa de Datos (Data)**: Repositorios centralizados (`ContactRepository`, `ProductRepository`) que abstraen la complejidad de las llamadas JSON-RPC a Odoo 19 usando la clase estructural `CallKwParams`.
*   **Comunicación**: Interfaz de red gestionada por **Retrofit 2** y OkHttp con un envoltorio genérico `RetrofitClient` para los endpoints y sesiones seguras en cookies.
*   **Asincronía**: Uso intensivo de **Kotlin Coroutines** (`lifecycleScope.launch`) para operaciones puramente no bloqueantes en todo el ciclo de UI.

---

## 🛠️ Tecnologías y Dependencias Principales
El ecosistema moderno utilizado (*extraído directamente de `build.gradle.kts`*):

| Categoría | Librería | Versión | Propósito |
| :--- | :--- | :--- | :--- |
| **Material UI** | `com.google.android.material:material` | *Config global* | Componentes Material 3 (Botones modernos, FABs, Diálogos limpios) |
| **Network Client**| `com.squareup.retrofit2:retrofit` | `2.9.0` | Base estable para llamadas HTTP y JSON-RPC a Odoo |
| **Serializer** | `com.squareup.retrofit2:converter-gson` | `2.9.0` | Interpretar el Payload JSON-RPC de Odoo y parsearlo a Modelos Kotlin |
| **Media/Caching** | `com.github.bumptech.glide:glide` | `4.16.0` | Carga súper eficiente de avatares de usuario y catálogo de productos |
| **Lifecycle API** | `androidx.lifecycle:lifecycle-viewmodel-ktx`| `2.8.3` | Sincronización asíncrona atada al ciclo de vida del usuario (`lifecycleScope`) |
| **UI Components** | `androidx.recyclerview:recyclerview` | `1.3.2` | Listado hiper responsivo e indexado de Clientes/Productos en Layouts (Kanban y Listas) |

---

## 🔒 Permisos de Android (`AndroidManifest.xml`)
La aplicación solicita estrictamente lo necesario para funcionar de manera nativa:

*   📱 `android.permission.INTERNET`: **Crítico.** Requerido para conectarse de ida y vuelta al ecosistema Odoo (JSON-RPC), así como consultar RENIEC/SUNAT asíncronamente.
*   📡 `android.permission.ACCESS_NETWORK_STATE`: Requerido para reaccionar ante intermitencias de datos o WiFi y prevenir caídas molestas en la App al mandar facturas.
*   🖼️ `android.permission.READ_EXTERNAL_STORAGE` (API ≤ 32) y `READ_MEDIA_IMAGES` (API 33+): Requeridos para que la aplicación logre seleccionar fotos de la galería y subirlas al editor del perfil maestro.

---

## ⚙️ Configuración y Variables Dinámicas (`AppConfig.kt`)
La aplicación elimina la codificación rígida ("hardcoding") y extrae la operatividad a variables seguras y centralizadas:

| Componente | Descripción | Extraído de |
| :--- | :--- | :--- |
| **Credenciales y Entorno** | Variables base para consumo como `BASE_URL`, `DATABASE_NAME` e `IDENTITY_API_TOKEN` para la SUNAT. | `AppConfig.kt` (Centralizado) |
| **Constantes de Facturación**| El sistema deduce internamente transacciones mediante `INVOICE_TYPE_SALE` (`"out_invoice"`) y aplica porcentajes legales como el impuesto predeterminado al `18.0`. | `AppConfig.kt` (Operatividad Dinámica) |
| **Modelos de Odoo** | Referencias persistentes y consistentes para evitar errores de redacción: `MODEL_USER`, `MODEL_PRODUCT`, `MODEL_INVOICE`, así como unificación estricta del Ubigeo Andino (`res.city`, `res.state`). | `AppConfig.kt` (Llamadas de API seguras) |

---

## 🔄 Flujo Principal de la App (User Journey)
El marco de diseño de experiencia se divide en **5 etapas clave**:

1.  **Ingreso y Autenticación** (`ui.login.Login`): Comprobación estricta en el servidor interno Odoo. Si la identidad es correcta, la sesión entra al `CookieJar`.
2.  **Dashboard Corporativo** (`Dashboard.kt`): Concentrador estático inmersivo para ramificarse velozmente a las tareas maestras del ERP (Contactos, Productos, Facturas).
3.  **Gestor Comercial** (`FormularioCliente`, `ListaContactos`): Alta fluida de Contactos. Si se introduce un RUC y la validación interna no cruza un usuario previo en Odoo, la App procede con el descubrimiento online inteligente con la SUNAT.
4.  **Flujo Eficiente de Venta** (`ui.invoice.Facturacion`): Facturador unificado que soporta Múltiples Monedas (USD, PEN con carga real), cálculo granular de impuestos por producto, validación de retención general (> S/ 700) y subida a Odoo en directo.
5.  **Cierre Inmediato**: Un log geo-dependiente que liquida inmediatamente las sesiones API de la Caché resguardando la entidad privada del negocio.

---

## 🎨 Branding y Experiencia de Usuario (UX)
*   **Rigid Theme Fix**: La aplicación fuerza el modo estricto ("Light.NoActionBar") bloqueando inmersiones no deseadas del sistema operativo y manteniendo con brillantez el dualismo cromático (**Azul Slate** de soportes técnicos y **Teal Dinámico** para interacciones).
*   **Modo Inmersivo Global**: Todas y cada una de las actividades anulan barras del OS y status estáticos en Android (Delegación al comportamiento Swipe-top-bottom) logrando 100% de la densidad de pantalla real para lectura.
*   **Anti-Dynamic Color Blindspots**: Restricciones puestas en `themes.xml` limitando la auto-configuración de marcas como Huawei, Xiaomi o Honor ante su gestión autónoma del contraste, previniendo los infames "fondos quemados".

---

## 📋 Entorno de Compilación
*   **IDE**: Android Studio (Versiones modernas basadas en IGB)
*   **Gradle**: Configuración nativa del repositorio Kotlin DSL (KTS)
*   **Min SDK Target**: 24
*   **Compile SDK Objetivo**: 36

---

## 📄 Licencia Comercial
© 2026 **Stack Perú Partners**.
**Propietario - Todos los derechos reservados.**
La copia, re-venta, descompilación o distribución no autorizada de este código fuente o del archivo APK ensamblado está estrictamente prohibida y regulada bajo las leyes locales e internacionales de propiedad intelectual.
