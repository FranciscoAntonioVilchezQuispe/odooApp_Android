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

* **`AppConfig.kt`**: Archivo *Singleton* centralizado que actúa como el diccionario de constantes globales de la aplicación. Aquí se define la `BASE_URL` (hacia dónde apunta Retrofit) y el `DATABASE_NAME`. Es vital configurar aquí la IP local de tu PC (ej. `192.168.100.51`) si pruebas en un dispositivo físico o emulador.
* **`MainActivity.kt`**: El **Dashboard principal**. Sus responsabilidades ahora incluyen:
    1. Verificar sesión activa.
    2. Carga inicial optimizada (solo campos ligeros: ID, Nombre, Email) para garantizar estabilidad de red.
    3. Búsqueda avanzada multi-campo y ordenamiento dinámico.
    4. Alternancia entre vistas Kanban, Lista y Pivot.

### 📦 Paquete: `/api/` (Capa de Red)

* **`RetrofitClient.kt`**: Motor de conexión optimizado para Odoo 19.
  * **Estabilidad Ferroviaria**: Se fuerza el protocolo `HTTP/1.1` y se deshabilita el `Connection Pooling` para evitar el error `unexpected end of stream` común en Odoo.
  * **`SessionCookieJar`**: Gestiona el `session_id`. Se ha mejorado para proveer la cookie a componentes externos (como Glide) permitiendo carga de imágenes autenticadas.
* **`OdooApiService.kt`**: Interfaz unificada. Utiliza el método genérico `executeKw` para todas las llamadas ORM (`search_read`, `write`, `create`), reduciendo la complejidad del cliente Retrofit.

### 📦 Paquete: `/model/` (Capa de Dominio)

* **`Models.kt`**:
  * **`Kwargs` Robusto**: Los parámetros de búsqueda (`fields`, `offset`, `limit`) son ahora opcionales (null por defecto). Esto permite reutilizar el mismo modelo para lecturas y para escrituras (`write`), evitando enviar parámetros inesperados a Odoo.
  * **`Contact`**: Modelo optimizado que soporta ID, nombre, email, teléfono y VAT (identificación).

### 📦 Paquete: `/data/` (Capa de Estado)

* **`UserSession.kt`**: Un gestor de sesión ultraligero que vive en la memoria RAM. Almacena temporalmente el objeto `User` mientras la app está abierta. Provee el método `clear()` invocado cuando el usuario hace clic en "Cerrar sesión".

### 📦 Paquete: `/ui/` (Capa de Presentación)

* **`/login/LoginActivity.kt`**: Punto de entrada con validación de red y carga inicial ultra-ligera de datos post-login.
* **`/contact/ContactAdapter.kt`**: Puente visual inteligente. Implementa **Lazy Loading de imágenes**: en lugar de procesar Base64 pesado, solicita cada foto bajo demanda mediante URLs autenticadas de Odoo, mejorando drásticamente el rendimiento.
* **`/contact/ContactFormActivity.kt`**: **Motor de CRUD completo**. Gestiona tanto la creación de nuevos socios como la edición de existentes. Soporta carga de fotos desde la galería y validación de campos obligatorios antes de sincronizar con Odoo via RPC.

---

## 3. Estructura de Directorios de Recursos (XML)

Ruta base: `app/src/main/res/`

La carpeta `res/` maneja los activos visuales. Hemos implementado una **Estructura Modular de Layouts** para evitar la acumulación de archivos en una sola carpeta, utilizando `sourceSets` en Gradle.

### 📐 `/layouts/` (Organización por Módulo)

Cada módulo tiene su propia subcarpeta que contiene una carpeta `layout/` interna (requisito de Android):

* **`/main/`**: Dashboard principal.
  * `activity_main.xml`: Pantalla de inicio con accesos rápidos.
  * `dialog_user_profile.xml`: Menú emergente de perfil.
* **`/contact/`**: Gestión de socios.
  * `activity_contact_list.xml`, `activity_contact_form.xml`, `activity_contact_detail.xml`.
  * `item_contact_kanban.xml`, `item_contact_list.xml` (diseño de celdas).
* **`/invoice/`**: Facturación y Comprobantes.
  * `activity_invoice.xml`: Formulario dinámico de Venta/Compra.
  * `item_invoice_line.xml`: Diseño de fila de producto.
* **`/login/`**: Acceso y Perfil de Usuario.
  * `activity_login.xml`: Pantalla de autenticación.
  * `activity_edit_profile.xml`: Formulario de edición de datos personales.

### 🎨 Otras carpetas de Recursos

* **`/drawable/`**: Contiene `logo_stack_peru.png` y `avatar_placeholder.xml`.
* **`/values/`**: 
  * `colors.xml`: Identidad corporativa (Odoo Primary/Secondary).
  * `strings.xml`: Textos internacionalizados.
  * `themes.xml`: Estilos globales y colores de barra de estado.

### 🌐 `/xml/`

* **`network_security_config.xml`**: Configurado para permitir tráfico **Cleartext (HTTP)** hacia IPs locales específicas (ej. `192.168.100.51`), esencial para desarrollo sin certificados SSL.

---

## 4. Archivos Raíz Críticos

* **`AndroidManifest.xml`**: El núcleo de permisos y registro de actividades. Indica qué Activity lanza primero la aplicación (`LoginActivity`) y exige expresamente permiso de `INTERNET` para que el celular no bloquee a Retrofit.
* **`build.gradle.kts (App Level)`**: El gestor de dependencias (similar al `requirements.txt` en Python o `package.json`). Aquí se declaran librerías externas que el proyecto necesita descargar de internet para compilar:
  * *Retrofit / Gson Converter* (Para las APIs y parseo de JSON)
  * *Glide* (Para la carga y renderizado asíncrono y en caché de imágenes de perfil)
  * *Material Components* (Para usar tarjetas con bordes redondeados y campos de texto modernos)
  * *ViewModel & LiveData* (Agregados como cimientos recomendados por Google).

---

## 5. Flujo Operativo Típico (Actualizado)

1. **Auth**: `LoginActivity` valida credenciales y el `CookieJar` guarda la sesión.
2. **Sincronización Inicial**: Al abrir el Dashboard, se descargan los contactos pidiendo solo campos básicos para asegurar que la conexión no se sature.
3. **Visualización**: El `ContactAdapter` dibuja la lista. Las imágenes se cargan en hilos secundarios por URL, manteniendo la app fluida.
4. **Gestión (CRUD)**: Al tocar un contacto, se abre el formulario. Si editas (ej. cambias el teléfono), la app lanza un `executeKw` con el método `write`. La lógica de `Models.kt` asegura que solo se envíen los datos modificados, respetando el contrato de Odoo 19.
5. **Cierre**: El log-out limpia cookies y redirige al inicio seguro.
